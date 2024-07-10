import {defineStore} from 'pinia';
import {ref} from 'vue';
import type {ChatMessageType, ChatType} from '@/types/apps/ChatMessageType';
import {MessageFormat, MessageType} from '@/types/apps/ChatMessageType';
import type {Message, Subscription} from 'webstomp-client';
import type {UserType} from "@/types/model/UserTypes";
import axiosServices from "@/utils/axios";
import type {RoomType} from "@/types/model/RoomTypes";
import {useWebSocketStore} from "@/stores/webSocketStore";
import {useRoomStore} from "@/stores/roomStore";
import {useUserStore} from "@/stores/userStore";

export const useChatStore = defineStore('chat', () => {
  const webSocketStore = useWebSocketStore();
  const roomStore = useRoomStore();
  const userStore = useUserStore();

  const subscribedChatRooms = ref<{[key: string]: Subscription}>({});

  const chatMessageContent = ref<string>('');
  const chatUser = ref<ChatType>({
    chatHistory: []
  });

  const subscribeChatRoom = (room: RoomType, onChatUpdate: (body: ChatMessageType) => void) => {
    webSocketStore?.stompClient?.send(`/ws/room.join-room/${room.id}`, JSON.stringify(webSocketStore.userConnected));
    roomStore.roomSelected = room;

    const subscription = webSocketStore.stompClient?.subscribe(`/topic/chat/room/${room.id}`, (message: Message) => {
      const payload: ChatMessageType = JSON.parse(message.body);
      roomStore.roomSelected?.chatMessages?.push(payload)
      onChatUpdate(payload);
    });

    if(subscription){
      subscribedChatRooms.value[room.id] = subscription
    }
  }

  const sendMessage = () => {
    const chatMessage: ChatMessageType = {
      body: chatMessageContent.value,
      type: MessageType.CHAT,
      format: MessageFormat.TEXT,
      roomId: roomStore.roomSelected?.id,
      userId: userStore.userConnected?.username
    };

    webSocketStore.stompClient?.send('/ws/chat.send-message-room', JSON.stringify(chatMessage));
    chatMessageContent.value = '';
  };

  const joinRoom = () => {
    const chatMessage: ChatMessageType = {
      type: MessageType.JOIN,
      format: MessageFormat.TEXT,
      roomId: roomStore.roomSelected?.id,
      userId: userStore.userConnected?.username
    }

    webSocketStore.stompClient?.send('/ws/chat.')
  }

  const getChatUser = async () => {
    if (!chatUser.value.username)
      chatUser.value = {
        username: 'User 1',
        chatHistory: chatUser.value.chatHistory
      };

    const responseUser = await axiosServices<UserType>('/api/v1/user');
    chatUser.value = {
      username: responseUser.data.username,
      chatHistory: chatUser.value.chatHistory
    };
    return chatUser.value;
  };


  return {
    chatMessageContent, subscribeChatRoom,
    sendMessage, getChatUser,
  };
});