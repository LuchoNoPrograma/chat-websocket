import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import axios from 'axios';
import { defineStore } from 'pinia';
import SockJS from 'sockjs-client/dist/sockjs.js';
import { computed, ref } from 'vue';
import type { ChatMessage, ConnectionState, Room, User } from '../types';

const backendUrl = (import.meta.env.VITE_BACKEND_URL || 'http://localhost:8081').replace(/\/$/, '');
const http = axios.create({ baseURL: backendUrl, timeout: 8_000 });

const messageKey = (message: ChatMessage) => message.id
  ?? [message.roomId, message.type, message.userId, message.createdAt, message.body].join('|');

const mergeMessages = (...groups: ChatMessage[][]) => {
  const unique = new Map<string, ChatMessage>();
  groups.flat().forEach((message) => unique.set(messageKey(message), message));
  return [...unique.values()].sort((left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
    return leftTime - rightTime;
  });
};

export const useRealtimeStore = defineStore('realtime-chat', () => {
  const connection = ref<ConnectionState>('idle');
  const connectionMessage = ref('Listo para conectar');
  const operationError = ref('');
  const currentUser = ref<User>();
  const users = ref<User[]>([]);
  const rooms = ref<Room[]>([]);
  const activeRoom = ref<Room>();
  const messagesByRoom = ref<Record<string, ChatMessage[]>>({});

  let client: Client | null = null;
  let roomSubscription: StompSubscription | null = null;
  let roomOperation = 0;
  let clientGeneration = 0;

  const activeMessages = computed(() => activeRoom.value ? messagesByRoom.value[activeRoom.value.id] ?? [] : []);
  const onlineCount = computed(() => users.value.filter((user) => user.online).length);

  const clearOperationError = () => {
    operationError.value = '';
  };

  const parseMessage = <T>(message: IMessage): T | undefined => {
    try {
      return JSON.parse(message.body) as T;
    } catch {
      operationError.value = 'El servidor envió un evento que no se pudo interpretar.';
      return undefined;
    }
  };

  const upsertUser = (user: User) => {
    users.value = [user, ...users.value.filter((item) => item.username !== user.username)]
      .filter((item) => item.online);
  };

  const upsertRoom = (room: Room) => {
    const previous = rooms.value.find((item) => item.id === room.id);
    const merged = { ...previous, ...room };
    rooms.value = [merged, ...rooms.value.filter((item) => item.id !== room.id)];
    if (activeRoom.value?.id === room.id) activeRoom.value = { ...activeRoom.value, ...room };
  };

  const hydrate = async () => {
    const [userResponse, roomResponse] = await Promise.all([
      http.get<User[]>('/api/v1/user-online'),
      http.get<Room[]>('/api/v1/room')
    ]);
    users.value = userResponse.data;
    rooms.value = roomResponse.data;
    if (currentUser.value) upsertUser(currentUser.value);
  };

  const unsubscribeFromActiveRoom = () => {
    const roomId = activeRoom.value?.id;
    if (roomSubscription && roomId && client?.connected) {
      roomSubscription.unsubscribe({ destination: `/topic/chat/room/${roomId}` });
    }
    roomSubscription = null;
  };

  const subscribeToRoom = async (roomId: string, operation: number) => {
    if (!client?.connected) throw new Error('El canal en tiempo real no está conectado.');

    const subscription = client.subscribe(`/topic/chat/room/${roomId}`, (message: IMessage) => {
      if (operation !== roomOperation || activeRoom.value?.id !== roomId) return;
      const payload = parseMessage<ChatMessage>(message);
      if (!payload) return;
      messagesByRoom.value[roomId] = mergeMessages(messagesByRoom.value[roomId] ?? [], [payload]);
    });
    roomSubscription = subscription;

    try {
      const response = await http.get<Room>(`/api/v1/room/${roomId}`);
      if (operation !== roomOperation) {
        if (client.connected) subscription.unsubscribe({ destination: `/topic/chat/room/${roomId}` });
        return;
      }
      activeRoom.value = response.data;
      messagesByRoom.value[roomId] = mergeMessages(
        response.data.chatMessages ?? [],
        messagesByRoom.value[roomId] ?? []
      );
    } catch (error) {
      if (roomSubscription === subscription) roomSubscription = null;
      if (client.connected) subscription.unsubscribe({ destination: `/topic/chat/room/${roomId}` });
      throw error;
    }
  };

  const subscribeGlobalTopics = () => {
    client?.subscribe('/topic/user', (message: IMessage) => {
      const payload = parseMessage<User>(message);
      if (payload) upsertUser(payload);
    });
    client?.subscribe('/topic/room', (message: IMessage) => {
      const payload = parseMessage<Room>(message);
      if (payload) upsertRoom(payload);
    });
  };

  const stopClient = async () => {
    roomOperation += 1;
    unsubscribeFromActiveRoom();
    const clientToStop = client;
    client = null;
    clientGeneration += 1;
    if (clientToStop) await clientToStop.deactivate();
  };

  const connect = async (username: string) => {
    await stopClient();
    const cleanUsername = username.trim();
    const generation = ++clientGeneration;
    clearOperationError();
    connection.value = 'connecting';
    connectionMessage.value = 'Abriendo el canal en tiempo real…';

    return new Promise<void>((resolve, reject) => {
      let settled = false;
      const finish = (error?: Error) => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timeout);
        if (error) reject(error);
        else resolve();
      };

      const timeout = window.setTimeout(() => {
        if (generation !== clientGeneration || connection.value !== 'connecting') return;
        connection.value = 'error';
        connectionMessage.value = 'El servidor local no respondió. Comprueba que el backend esté iniciado.';
        finish(new Error('Tiempo de conexión agotado'));
        void stopClient();
      }, 9_000);

      client = new Client({
        webSocketFactory: () => new SockJS(`${backendUrl}/ws-chatapp`),
        connectHeaders: { login: cleanUsername },
        reconnectDelay: 4_000,
        debug: () => undefined,
        onConnect: async () => {
          if (generation !== clientGeneration) return;
          try {
            const response = await http.post<User>('/api/v1/auth', null, { params: { username: cleanUsername } });
            if (generation !== clientGeneration) return;
            currentUser.value = response.data;
            subscribeGlobalTopics();
            const roomToRestoreId = activeRoom.value?.id;
            const operation = roomOperation;
            await hydrate();
            if (roomToRestoreId) {
              const roomToRestore = rooms.value.find((room) => room.id === roomToRestoreId);
              if (roomToRestore) {
                activeRoom.value = roomToRestore;
                messagesByRoom.value[roomToRestoreId] = [];
                await subscribeToRoom(roomToRestoreId, operation);
              } else {
                activeRoom.value = undefined;
                delete messagesByRoom.value[roomToRestoreId];
              }
            }
            connection.value = 'connected';
            connectionMessage.value = 'Conectado en tiempo real';
            clearOperationError();
            finish();
          } catch (error) {
            if (generation !== clientGeneration) return;
            connection.value = 'error';
            connectionMessage.value = 'Conexión abierta, pero no se pudo cargar el chat.';
            operationError.value = connectionMessage.value;
            finish(error instanceof Error ? error : new Error(connectionMessage.value));
            void stopClient();
          }
        },
        onStompError: (frame) => {
          if (generation !== clientGeneration) return;
          connection.value = 'error';
          connectionMessage.value = frame.headers.message || 'El servidor rechazó la conexión.';
          operationError.value = connectionMessage.value;
          finish(new Error(connectionMessage.value));
          void stopClient();
        },
        onWebSocketClose: () => {
          if (generation !== clientGeneration) return;
          roomSubscription = null;
          if (connection.value === 'connected') {
            connection.value = 'connecting';
            connectionMessage.value = 'Reconectando y recuperando la sala…';
          }
        }
      });

      client.activate();
    });
  };

  const enterRoom = async (room: Room) => {
    clearOperationError();
    if (activeRoom.value?.id === room.id && roomSubscription) return;

    roomOperation += 1;
    const operation = roomOperation;
    unsubscribeFromActiveRoom();
    activeRoom.value = room;

    try {
      await subscribeToRoom(room.id, operation);
    } catch (error) {
      if (operation === roomOperation) activeRoom.value = undefined;
      operationError.value = axios.isAxiosError(error)
        ? 'No se pudo cargar la sala. Comprueba que siga disponible.'
        : 'Se perdió la conexión antes de entrar a la sala.';
      throw error;
    }
  };

  const leaveRoom = () => {
    roomOperation += 1;
    unsubscribeFromActiveRoom();
    activeRoom.value = undefined;
    clearOperationError();
  };

  const sendMessage = (body: string) => {
    clearOperationError();
    const content = body.trim();
    if (!content || !activeRoom.value || !currentUser.value) return false;
    const roomId = activeRoom.value.id;
    const message: ChatMessage = {
      body: content,
      type: 'CHAT',
      format: 'TEXT',
      roomId,
      userId: currentUser.value.username,
      createdAt: undefined
    };

    if (!client?.connected) {
      operationError.value = 'Espera a que termine la reconexión antes de enviar.';
      return false;
    }
    client.publish({ destination: '/ws/chat.send-message-room', body: JSON.stringify(message) });
    return true;
  };

  const createRoom = (name: string, description: string) => {
    clearOperationError();
    if (!client?.connected) {
      operationError.value = 'Espera a que termine la reconexión antes de crear una sala.';
      return false;
    }
    client.publish({
      destination: '/ws/room.create-room',
      body: JSON.stringify({ name, description, activeUsers: 0, tags: [] })
    });
    return true;
  };

  const disconnect = async () => {
    await stopClient();
    currentUser.value = undefined;
    users.value = [];
    rooms.value = [];
    activeRoom.value = undefined;
    messagesByRoom.value = {};
    connection.value = 'idle';
    connectionMessage.value = 'Listo para conectar';
    clearOperationError();
  };

  return {
    connection,
    connectionMessage,
    operationError,
    currentUser,
    users,
    rooms,
    activeRoom,
    activeMessages,
    onlineCount,
    clearOperationError,
    connect,
    enterRoom,
    leaveRoom,
    sendMessage,
    createRoom,
    disconnect
  };
});
