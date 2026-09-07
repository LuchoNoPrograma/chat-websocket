import { messageKey, mergeMessages } from '../services/messages';
import { createOutbox } from '../services/outbox';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import axios from 'axios';
import { defineStore } from 'pinia';
import { backendUrl, http, acquireSession, validateSession, sessionCredential, clearSession, revokeSession, sessionError } from '../services/session';
import SockJS from 'sockjs-client/dist/sockjs.js';
import { computed, onScopeDispose, ref } from 'vue';
import type {
  ReceiptEvent,
  AvatarId,
  ChatMessage,
  ConnectionState,
  MessagePage,
  Room,
  RoomActivity,
  SessionPolicy,
  User
} from '../types/chat';

const defaultSessionPolicy: SessionPolicy = { idleTimeoutSeconds: 15 * 60, warningBeforeSeconds: 2 * 60 };

type HistoryState = {
  nextCursor?: string;
  hasMore: boolean;
  loading: boolean;
  initialized: boolean;
};

const emptyHistory = (): HistoryState => ({ hasMore: false, loading: false, initialized: false });

export const useRealtimeStore = defineStore('realtime-chat', () => {
  const outbox = createOutbox();
  const connection = ref<ConnectionState>('idle');
  const connectionMessage = ref('Listo para conectar');
  const operationError = ref('');
  const currentUser = ref<User>();
  const users = ref<User[]>([]);
  const rooms = ref<Room[]>([]);
  const activeRoom = ref<Room>();
  const activeDirectUser = ref<User>();
  const messagesByRoom = ref<Record<string, ChatMessage[]>>({});
  const directMessagesByUser = ref<Record<string, ChatMessage[]>>({});
  const unreadDirectByUser = ref<Record<string, number>>({});
  const roomActivityById = ref<Record<string, RoomActivity>>({});
  const incomingDirectMessage = ref<ChatMessage>();
  const roomHistory = ref<Record<string, HistoryState>>({});
  const directHistory = ref<Record<string, HistoryState>>({});
  const sessionPolicy = ref<SessionPolicy>(defaultSessionPolicy);
  const idleWarningSeconds = ref<number>();
  const idleExpired = ref(false);
  const sessionEndReason = ref('Te desconectamos por inactividad');

  let client: Client | null = null;
  let roomSubscription: StompSubscription | null = null;
  let conversationOperation = 0;
  let clientGeneration = 0;
  let idleWarningTimer: number | undefined;
  let idleDisconnectTimer: number | undefined;
  let idleCountdownTimer: number | undefined;
  let idleDeadline = 0;
  let idleListenersAttached = false;
  let presenceRevision = 0;
  let roomRevision = 0;
  const presenceEvents = new Map<string, { revision: number; user: User }>();
  const roomEvents = new Map<string, { revision: number; room: Room }>();
  const seenRoomActivity = new Set<string>();

  const activeConversationKey = computed(() => {
    if (activeRoom.value) return `room:${activeRoom.value.id}`;
    if (activeDirectUser.value) return `dm:${activeDirectUser.value.username}`;
    return undefined;
  });
  const activeMessages = computed(() => {
    if (activeRoom.value) return messagesByRoom.value[activeRoom.value.id] ?? [];
    if (activeDirectUser.value) return directMessagesByUser.value[activeDirectUser.value.username] ?? [];
    return [];
  });
  const activeHistory = computed(() => {
    if (activeRoom.value) return roomHistory.value[activeRoom.value.id] ?? emptyHistory();
    if (activeDirectUser.value) return directHistory.value[activeDirectUser.value.username] ?? emptyHistory();
    return emptyHistory();
  });
  const canLoadMore = computed(() => activeHistory.value.hasMore);
  const isLoadingHistory = computed(() => activeHistory.value.loading);
  const onlineCount = computed(() => users.value.filter((user) => user.online).length);
  const totalUnreadDirect = computed(() => Object.values(unreadDirectByUser.value)
    .reduce((total, count) => total + count, 0));
  const totalUnreadRooms = computed(() => Object.values(roomActivityById.value)
    .reduce((total, activity) => total + activity.unread, 0));

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
    if (activeDirectUser.value?.username === user.username) {
      activeDirectUser.value = { ...activeDirectUser.value, ...user };
    }
    users.value = [user, ...users.value.filter((item) => item.username !== user.username)]
      .filter((item) => item.online);
  };

  const upsertRoom = (room: Room) => {
    const previous = rooms.value.find((item) => item.id === room.id);
    const merged = { ...previous, ...room };
    rooms.value = [merged, ...rooms.value.filter((item) => item.id !== room.id)];
    if (activeRoom.value?.id === room.id) activeRoom.value = { ...activeRoom.value, ...room };
  };

  const loadSessionPolicy = async () => {
    try {
      const response = await http.get<SessionPolicy>('/api/v1/session-policy');
      sessionPolicy.value = response.data;
    } catch {
      sessionPolicy.value = defaultSessionPolicy;
    }
  };

  const hydrate = async () => {
    const presenceAtStart = presenceRevision;
    const roomsAtStart = roomRevision;
    const [userResponse, roomResponse] = await Promise.all([
      http.get<User[]>('/api/v1/user-online'),
      http.get<Room[]>('/api/v1/room'),
      loadSessionPolicy()
    ]);
    users.value = userResponse.data;
    rooms.value = roomResponse.data;
    presenceEvents.forEach((event) => {
      if (event.revision > presenceAtStart) upsertUser(event.user);
    });
    roomEvents.forEach((event) => {
      if (event.revision > roomsAtStart) upsertRoom(event.room);
    });
    if (currentUser.value) upsertUser(currentUser.value);
  };

  const deliveryRequests = new Map<string, number>();
  const markMessageDelivered = (message: ChatMessage) => {
    const username = currentUser.value?.username;
    if (!client?.connected || !username || !message.id || message.type !== 'CHAT'
      || message.userId === username || message.deliveredTo?.[username]) return;
    if (message.roomId ? activeRoom.value?.id !== message.roomId : message.recipientId !== username) return;
    const now = Date.now();
    if (now - (deliveryRequests.get(message.id) ?? 0) < 3000) return;
    deliveryRequests.set(message.id, now);
    client.publish({ destination: '/ws/chat.delivered-message', body: JSON.stringify({ messageId: message.id }) });
  };

  const fetchRoomHistory = async (roomId: string, reset: boolean) => {
    const previous = roomHistory.value[roomId] ?? emptyHistory();
    if (previous.loading || (!reset && previous.initialized && !previous.hasMore)) return 0;
    roomHistory.value[roomId] = { ...previous, loading: true };
    try {
      const response = await http.get<MessagePage>(`/api/v1/room/${roomId}/messages`, {
        params: { before: reset ? undefined : previous.nextCursor, size: 30 }
      });
      // Preserve events received by STOMP while the REST history request is in flight.
      const existing = messagesByRoom.value[roomId] ?? [];
      messagesByRoom.value[roomId] = mergeMessages(response.data.messages, existing).map(applyReceipts);
      roomHistory.value[roomId] = {
        nextCursor: response.data.nextCursor,
        hasMore: response.data.hasMore,
        loading: false,
        initialized: true
      };
      response.data.messages.forEach(message => { markMessageDelivered(message); outbox.acknowledge(message); });
      return response.data.messages.length;
    } catch (error) {
      roomHistory.value[roomId] = { ...previous, loading: false };
      throw error;
    }
  };

  const fetchDirectHistory = async (peerUsername: string, reset: boolean) => {
    if (!currentUser.value) return 0;
    const previous = directHistory.value[peerUsername] ?? emptyHistory();
    if (previous.loading || (!reset && previous.initialized && !previous.hasMore)) return 0;
    directHistory.value[peerUsername] = { ...previous, loading: true };
    try {
      const response = await http.get<MessagePage>(`/api/v1/dm/${encodeURIComponent(peerUsername)}/messages`, {
        params: {
          before: reset ? undefined : previous.nextCursor,
          size: 30
        }
      });
      // Preserve events received by STOMP while the REST history request is in flight.
      const existing = directMessagesByUser.value[peerUsername] ?? [];
      directMessagesByUser.value[peerUsername] = mergeMessages(response.data.messages, existing).map(applyReceipts);
      directHistory.value[peerUsername] = {
        nextCursor: response.data.nextCursor,
        hasMore: response.data.hasMore,
        loading: false,
        initialized: true
      };
      response.data.messages.forEach(message => { markMessageDelivered(message); outbox.acknowledge(message); });
      return response.data.messages.length;
    } catch (error) {
      directHistory.value[peerUsername] = { ...previous, loading: false };
      throw error;
    }
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
      if (operation !== conversationOperation || activeRoom.value?.id !== roomId) return;
      const payload = parseMessage<ChatMessage>(message);
      if (!payload) return;
      messagesByRoom.value[roomId] = mergeMessages(messagesByRoom.value[roomId] ?? [], [payload]);
      markMessageDelivered(payload);
      outbox.acknowledge(payload);
      applyReceipts(payload);
    });
    roomSubscription = subscription;

    try {
      const [roomResponse] = await Promise.all([
        http.get<Room>(`/api/v1/room/${roomId}`),
        fetchRoomHistory(roomId, true)
      ]);
      if (operation !== conversationOperation) {
        if (client.connected) subscription.unsubscribe({ destination: `/topic/chat/room/${roomId}` });
        return;
      }
      activeRoom.value = roomResponse.data;
    } catch (error) {
      if (roomSubscription === subscription) roomSubscription = null;
      if (client.connected) subscription.unsubscribe({ destination: `/topic/chat/room/${roomId}` });
      throw error;
    }
  };

  const receiptUpdates = new Map<string, ReceiptEvent[]>();
  const applyReceipts = (message: ChatMessage): ChatMessage => {
    for (const receipt of receiptUpdates.get(message.id ?? '') ?? []) {
      if (receipt.deliveredAt) message.deliveredTo = { ...message.deliveredTo, [receipt.username]: receipt.deliveredAt };
      if (receipt.readAt) message.readBy = { ...message.readBy, [receipt.username]: receipt.readAt };
    }
    return message;
  };
  const acceptMessage = (payload: ChatMessage) => {
    outbox.acknowledge(payload);
    if (payload.roomId) {
      messagesByRoom.value[payload.roomId] = mergeMessages(messagesByRoom.value[payload.roomId] ?? [], [applyReceipts(payload)]);
    } else {
      const peer = payload.userId === currentUser.value?.username ? payload.recipientId : payload.userId;
      if (peer) directMessagesByUser.value[peer] = mergeMessages(directMessagesByUser.value[peer] ?? [], [applyReceipts(payload)]);
    }
  };

  const subscribeGlobalTopics = () => {
    client?.subscribe('/user/queue/errors', frame => {
      const error = parseMessage<{ message: string; clientMessageId?: string }>(frame);
      if (error) {
        operationError.value = error.message;
        if (error.clientMessageId) outbox.acknowledge({ clientMessageId: error.clientMessageId });
      }
    });
    client?.subscribe('/user/queue/accepted', frame => {
      const payload = parseMessage<ChatMessage>(frame);
      if (payload) acceptMessage(payload);
    });
    client?.subscribe('/user/queue/receipts', frame => {
      const receipt = parseMessage<ReceiptEvent>(frame);
      if (!receipt) return;
      const existing = receiptUpdates.get(receipt.messageId) ?? [];
            const previous = existing.find(item => item.username === receipt.username);
      const merged = { ...receipt, readAt: receipt.readAt ?? previous?.readAt, deliveredAt: previous?.deliveredAt ?? receipt.deliveredAt };
      receiptUpdates.set(receipt.messageId, [...existing.filter(item => item.username !== receipt.username), merged]);
      const list = receipt.roomId ? messagesByRoom.value[receipt.roomId]
        : directMessagesByUser.value[receipt.userId === currentUser.value?.username ? receipt.recipientId ?? '' : receipt.userId];
      list?.forEach(message => { if (message.id === receipt.messageId) applyReceipts(message); });
    });
    client?.subscribe('/topic/user', (message: IMessage) => {
      const payload = parseMessage<User>(message);
      if (payload) {
        presenceEvents.set(payload.username, { revision: ++presenceRevision, user: payload });
        upsertUser(payload);
      }
    });
    client?.subscribe('/topic/room', (message: IMessage) => {
      const payload = parseMessage<Room>(message);
      if (payload) {
        roomEvents.set(payload.id, { revision: ++roomRevision, room: payload });
        upsertRoom(payload);
      }
    });
    client?.subscribe('/topic/chat/activity', (message: IMessage) => {
      const payload = parseMessage<ChatMessage>(message);
      const username = currentUser.value?.username;
      if (!payload?.roomId || payload.type !== 'CHAT' || !username || payload.userId === username) return;
      const activityKey = messageKey(payload);
      if (seenRoomActivity.has(activityKey)) return;
      seenRoomActivity.add(activityKey);
      if (activeRoom.value?.id === payload.roomId) return;

      const previous = roomActivityById.value[payload.roomId] ?? { unread: 0, replies: 0 };
      roomActivityById.value = {
        ...roomActivityById.value,
        [payload.roomId]: {
          unread: previous.unread + 1,
          replies: previous.replies + (payload.replyToUserId === username ? 1 : 0)
        }
      };
    });
    client?.subscribe('/user/queue/direct', (message: IMessage) => {
      const payload = parseMessage<ChatMessage>(message);
      const username = currentUser.value?.username;
      if (!payload || !username) return;
      const peerUsername = payload.userId === username ? payload.recipientId : payload.userId;
      if (!peerUsername) return;
      const existing = directMessagesByUser.value[peerUsername] ?? [];
      const isNewMessage = !existing.some((item) => messageKey(item) === messageKey(payload));
      directMessagesByUser.value[peerUsername] = mergeMessages(existing, [payload]);
      markMessageDelivered(payload);
      outbox.acknowledge(payload);
      applyReceipts(payload);

      const isIncoming = payload.userId !== username && payload.recipientId === username;
      const isConversationOpen = activeDirectUser.value?.username === peerUsername;
      if (isIncoming && isNewMessage && !isConversationOpen) {
        unreadDirectByUser.value = {
          ...unreadDirectByUser.value,
          [peerUsername]: (unreadDirectByUser.value[peerUsername] ?? 0) + 1
        };
        incomingDirectMessage.value = payload;
      }
    });
  };

  const dismissDirectNotification = () => {
    incomingDirectMessage.value = undefined;
  };

  const markDirectRead = (username: string) => {
    if (unreadDirectByUser.value[username]) {
      const nextUnread = { ...unreadDirectByUser.value };
      delete nextUnread[username];
      unreadDirectByUser.value = nextUnread;
    }
    if (incomingDirectMessage.value?.userId === username) dismissDirectNotification();
  };

  const markRoomRead = (roomId: string) => {
    if (!roomActivityById.value[roomId]) return;
    const nextActivity = { ...roomActivityById.value };
    delete nextActivity[roomId];
    roomActivityById.value = nextActivity;
  };

  const clearIdleTimers = () => {
    window.clearTimeout(idleWarningTimer);
    window.clearTimeout(idleDisconnectTimer);
    window.clearInterval(idleCountdownTimer);
    idleWarningTimer = undefined;
    idleDisconnectTimer = undefined;
    idleCountdownTimer = undefined;
    idleWarningSeconds.value = undefined;
  };

  const clearChatState = () => {
    outbox.clear();
    receiptUpdates.clear();
    currentUser.value = undefined;
    users.value = [];
    rooms.value = [];
    activeRoom.value = undefined;
    activeDirectUser.value = undefined;
    messagesByRoom.value = {};
    directMessagesByUser.value = {};
    unreadDirectByUser.value = {};
    roomActivityById.value = {};
    seenRoomActivity.clear();
    incomingDirectMessage.value = undefined;
    roomHistory.value = {};
    directHistory.value = {};
  };

  function stopIdleTracking() {
    clearIdleTimers();
    if (idleListenersAttached) {
      activityEvents.forEach((eventName) => window.removeEventListener(eventName, resetIdleActivity));
      idleListenersAttached = false;
    }
  }

  const stopClient = async () => {
    conversationOperation += 1;
    unsubscribeFromActiveRoom();
    stopIdleTracking();
    const clientToStop = client;
    client = null;
    clientGeneration += 1;
    if (clientToStop) await clientToStop.deactivate();
  };

  const expireIdleSession = async () => {
    sessionEndReason.value = 'Te desconectamos por inactividad';
    idleExpired.value = true;
    await stopClient();
    clearChatState();
    connection.value = 'idle';
    connectionMessage.value = 'Desconectado por inactividad';
  };

  const updateIdleCountdown = () => {
    idleWarningSeconds.value = Math.max(0, Math.ceil((idleDeadline - Date.now()) / 1000));
  };

  const resetIdleActivity = () => {
    if (connection.value !== 'connected') return;
    clearIdleTimers();
    const timeoutMs = sessionPolicy.value.idleTimeoutSeconds * 1000;
    const warningMs = sessionPolicy.value.warningBeforeSeconds * 1000;
    idleDeadline = Date.now() + timeoutMs;
    idleWarningTimer = window.setTimeout(() => {
      updateIdleCountdown();
      idleCountdownTimer = window.setInterval(updateIdleCountdown, 1000);
    }, Math.max(0, timeoutMs - warningMs));
    idleDisconnectTimer = window.setTimeout(() => void expireIdleSession(), timeoutMs);
  };

  const activityEvents: (keyof WindowEventMap)[] = ['pointerdown', 'keydown', 'scroll', 'touchstart'];

  function startIdleTracking() {
    if (!idleListenersAttached) {
      activityEvents.forEach((eventName) => window.addEventListener(eventName, resetIdleActivity, { passive: true }));
      idleListenersAttached = true;
    }
    resetIdleActivity();
  }

  const connect = async (username: string, avatarId: AvatarId = 'claudia') => {
    await stopClient();
    const cleanUsername = username.trim();
    try {
      await acquireSession(cleanUsername, avatarId);
      outbox.restore();
    } catch (error) {
      connection.value = 'error';
      connectionMessage.value = sessionError(error);
      if (axios.isAxiosError(error) && error.response?.status === 401) clearSession();
      throw error;
    }
    const generation = ++clientGeneration;
    idleExpired.value = false;
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
        connectionMessage.value = 'El chat no responde. Vuelve a intentar en unos segundos.';
        finish(new Error('Tiempo de conexión agotado'));
        void stopClient();
      }, 9_000);

      client = new Client({
        webSocketFactory: () => new SockJS(`${backendUrl}/ws-chatapp`),
        connectHeaders: { Authorization: `Bearer ${sessionCredential()?.token}` },
        beforeConnect: async () => {
          try { await validateSession(); } catch (error) {
            if (axios.isAxiosError(error) && !error.response) return;
            connectionMessage.value = sessionError(error);
            sessionEndReason.value = 'La sesión del servidor terminó';
            idleExpired.value = true;
            connection.value = 'idle';
            outbox.clear();
            clearSession();
            void stopClient().then(clearChatState);
            finish(error instanceof Error ? error : new Error(connectionMessage.value));
          }
        },
        reconnectDelay: 4_000,
        debug: () => undefined,
        onConnect: async () => {
          if (generation !== clientGeneration) return;
          try {
            const roomToRestore = activeRoom.value;
            const directToRestore = activeDirectUser.value;
            const session = await validateSession();
            if (generation !== clientGeneration) return;
            currentUser.value = session.user;
            messagesByRoom.value = {};
            directMessagesByUser.value = {};
            unreadDirectByUser.value = {};
            roomActivityById.value = {};
            seenRoomActivity.clear();
            deliveryRequests.clear();
            readRequests.clear();
            incomingDirectMessage.value = undefined;
            roomHistory.value = {};
            directHistory.value = {};
            subscribeGlobalTopics();
            await hydrate();
            if (roomToRestore) {
              const room = rooms.value.find((item) => item.id === roomToRestore.id);
              if (room) {
                activeRoom.value = room;
                activeDirectUser.value = undefined;
                const operation = ++conversationOperation;
                await subscribeToRoom(room.id, operation);
              } else {
                activeRoom.value = undefined;
              }
            } else if (directToRestore) {
              activeDirectUser.value = directToRestore;
              activeRoom.value = undefined;
              await fetchDirectHistory(directToRestore.username, true);
            }
            connection.value = 'connected';
            connectionMessage.value = 'Conectado en tiempo real';
            replayPending();
            startIdleTracking();
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
          stopIdleTracking();
          if (currentUser.value) {
            connection.value = 'connecting';
            connectionMessage.value = 'Servidor no disponible · Reintentando…';
          }
        }
      });

      client.activate();
    });
  };

  const onOffline = () => {
    if (!currentUser.value || !client) return;
    connection.value = 'connecting';
    connectionMessage.value = 'Sin conexión · Conservamos los mensajes pendientes';
    client.forceDisconnect();
  };
  window.addEventListener('offline', onOffline);
  onScopeDispose(() => window.removeEventListener('offline', onOffline));

  const enterRoom = async (room: Room) => {
    clearOperationError();
    if (activeRoom.value?.id === room.id && roomSubscription) return;
    conversationOperation += 1;
    const operation = conversationOperation;
    unsubscribeFromActiveRoom();
    activeDirectUser.value = undefined;
    activeRoom.value = room;
    markRoomRead(room.id);
    if (!client?.connected) return;
    messagesByRoom.value[room.id] = [];
    roomHistory.value[room.id] = emptyHistory();
    try {
      await subscribeToRoom(room.id, operation);
      replayPending();
    } catch (error) {
      if (connection.value !== 'connected') return;
      if (operation === conversationOperation) activeRoom.value = undefined;
      operationError.value = axios.isAxiosError(error)
        ? 'No se pudo cargar la sala. Comprueba que siga disponible.'
        : 'Se perdió la conexión antes de entrar a la sala.';
      throw error;
    }
  };

  const enterDirect = async (user: User) => {
    clearOperationError();
    if (!currentUser.value || user.username === currentUser.value.username) return;
    conversationOperation += 1;
    unsubscribeFromActiveRoom();
    activeRoom.value = undefined;
    activeDirectUser.value = user;
    markDirectRead(user.username);
    if (!client?.connected) return;
    directMessagesByUser.value[user.username] = [];
    directHistory.value[user.username] = emptyHistory();
    try {
      await fetchDirectHistory(user.username, true);
      markDirectRead(user.username);
    } catch (error) {
      if (connection.value !== 'connected') return;
      activeDirectUser.value = undefined;
      operationError.value = axios.isAxiosError(error)
        ? 'No se pudo cargar el mensaje directo.'
        : 'Se perdió la conexión antes de abrir el mensaje directo.';
      throw error;
    }
  };

  const leaveConversation = () => {
    conversationOperation += 1;
    unsubscribeFromActiveRoom();
    activeRoom.value = undefined;
    activeDirectUser.value = undefined;
    clearOperationError();
  };

  const loadOlderMessages = async () => {
    try {
      if (activeRoom.value) return await fetchRoomHistory(activeRoom.value.id, false);
      if (activeDirectUser.value) return await fetchDirectHistory(activeDirectUser.value.username, false);
      return 0;
    } catch {
      operationError.value = 'No se pudieron cargar mensajes anteriores.';
      return 0;
    }
  };

  const sendMessage = (body: string, replyToId?: string) => {
    clearOperationError();
    const content = body.trim();
    if (!content || !currentUser.value || (!activeRoom.value && !activeDirectUser.value)) return false;
    if (!client?.connected) {
      operationError.value = 'Espera a que termine la reconexión antes de enviar.';
      return false;
    }
    if (content.length > 10000) {
      operationError.value = 'El mensaje admite hasta 10 000 caracteres.';
      return false;
    }
    const message: ChatMessage = {
      body: content,
      type: 'CHAT',
      format: 'TEXT',
      userId: currentUser.value.username
    };
    if (replyToId) message.replyToId = replyToId;
    if (activeRoom.value) message.roomId = activeRoom.value.id;
    else if (activeDirectUser.value) message.recipientId = activeDirectUser.value.username;
    try {
      const entry = outbox.enqueue(activeConversationKey.value!, message);
      publishPending(entry);
      resetIdleActivity();
      return entry.message.clientMessageId;
    } catch (error) {
      operationError.value = error instanceof Error ? error.message : 'No se pudo preparar el envío.';
      return false;
    }
  };

  const publishPending = (entry: import('../services/outbox').PendingMessage) => {
    if (!client?.connected) return;
    entry.state = 'sending';
    entry.lastAttempt = Date.now();
    client.publish({ destination: entry.message.roomId ? '/ws/chat.send-message-room' : '/ws/chat.send-direct-message', body: JSON.stringify(entry.message) });
  };
  const replayPending = () => {
    Object.values(outbox.pending.value).forEach(entry => {
      if (!entry.message.roomId || entry.message.roomId === activeRoom.value?.id) publishPending(entry);
    });
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
    resetIdleActivity();
    return true;
  };

  const disconnect = async () => {
    idleExpired.value = false;
    await stopClient();
    try { await revokeSession(); } catch { /* Local logout still completes offline. */ }
    clearChatState();
    connection.value = 'idle';
    connectionMessage.value = 'Listo para conectar';
    clearOperationError();
  };

  const retryConnection = async () => {
    const user = currentUser.value;
    if (!user || connection.value === 'connected') return;
    await connect(user.username, user.avatarId);
  };

  const acknowledgeIdleExpiration = () => {
    idleExpired.value = false;
  };

  const readRequests = new Map<string, number>();
  const markMessageRead = (message: ChatMessage) => {
    const username = currentUser.value?.username;
    if (!client?.connected || !username || !message.id || message.type !== 'CHAT'
      || message.userId === username || message.readBy?.[username]) return;
    const belongs = activeRoom.value ? message.roomId === activeRoom.value.id
      : activeDirectUser.value && message.userId === activeDirectUser.value.username && message.recipientId === username;
    if (!belongs || document.visibilityState !== 'visible' || !document.hasFocus()) return;
    const now = Date.now();
    if (now - (readRequests.get(message.id) ?? 0) < 3000) return;
    readRequests.set(message.id, now);
    client.publish({ destination: '/ws/chat.read-message', body: JSON.stringify({ messageId: message.id }) });
  };

  return {
    markMessageRead,
    pendingMessages: outbox.pending,
    retryPendingMessage: (key: string) => {
      const entry = outbox.pending.value[key];
      if (!entry || !client?.connected) return false;
      publishPending(entry);
      return true;
    },
    connection,
    connectionMessage,
    operationError,
    currentUser,
    users,
    rooms,
    activeRoom,
    activeDirectUser,
    activeConversationKey,
    activeMessages,
    canLoadMore,
    isLoadingHistory,
    onlineCount,
    unreadDirectByUser,
    roomActivityById,
    incomingDirectMessage,
    totalUnreadDirect,
    totalUnreadRooms,
    idleWarningSeconds,
    idleExpired,
    sessionEndReason,
    clearOperationError,
    dismissDirectNotification,
    connect,
    enterRoom,
    enterDirect,
    leaveConversation,
    loadOlderMessages,
    sendMessage,
    createRoom,
    resetIdleActivity,
    acknowledgeIdleExpiration,
    retryConnection,
    disconnect
  };
});
