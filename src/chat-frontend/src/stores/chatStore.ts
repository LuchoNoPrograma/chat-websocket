import {defineStore} from 'pinia';
import {ref} from 'vue';
import type {ChatMessageType, ChatType} from '@/types/apps/ChatMessageType';
import type {Message, Subscription} from 'webstomp-client';
import type {RoomType} from "@/types/model/RoomTypes";
import {useWebSocketStore} from "@/stores/webSocketStore";
import {useRoomStore} from "@/stores/roomStore";

export const useChatStore = defineStore('chat', () => {
  const webSocketStore = useWebSocketStore();
  const roomStore = useRoomStore();

  const subscribedChatRooms = ref<{ [key: string]: Subscription }>({});

  const chatMessageContent = ref<string>('');

  //Join chat room
  const subscribeChatRoom = (room: RoomType, onChatUpdate: (body: ChatMessageType) => void) => {
    if (subscribedChatRooms.value[room.id]) {
      return
    }

    const subscription = webSocketStore.stompClient?.subscribe(`/topic/chat/room/${room.id}`, (message: Message) => {
      const payload: ChatMessageType = JSON.parse(message.body);
      roomStore.selectedRoom?.chatMessages?.push(payload)
      onChatUpdate(payload);
    });

    if (subscription) {
      subscribedChatRooms.value[room.id] = subscription
    }
  }

  //Leave chat room
  const unsubscribeChatRoom = (room: RoomType) => {
    //custom header to handle unsubscribe event in Spring Boot
    webSocketStore?.stompClient?.unsubscribe(subscribedChatRooms.value[room.id].id, {
      destination: '/topic/chat/room/' + room.id
    });

    roomStore.selectedRoom = null;
    delete subscribedChatRooms.value[room.id];
  }

  const sendMessage = (chatMessage: ChatMessageType, type ?: 'ROOM' | 'PRIVATE') => {
    let destination = (!type || type === 'ROOM') ? '/ws/chat.send-message-room' : '/ws/chat.send-message-user-private';
    webSocketStore.stompClient?.send(destination, JSON.stringify(chatMessage));
    chatMessageContent.value = '';
  };

  return {
    chatMessageContent, subscribeChatRoom, unsubscribeChatRoom,
    sendMessage
  };
});