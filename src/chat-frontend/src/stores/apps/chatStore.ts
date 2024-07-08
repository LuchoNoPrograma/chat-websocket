import {defineStore} from 'pinia';
import {ref} from 'vue';
import type {ChatMessageType, ChatType} from '@/types/apps/ChatMessageType';
import {MessageFormat, MessageType} from '@/types/apps/ChatMessageType';
//@ts-ignore
import SockJS from 'sockjs-client/dist/sockjs.js';
import type {Client, Message} from 'webstomp-client';
import Stomp, {Frame} from 'webstomp-client';
import type {UserType} from "@/types/model/UserTypes";
import axiosServices from "@/utils/axios";
import type {RoomType} from "@/types/model/RoomTypes";
/*import SockJS from "sockjs-client";*/

// project imports

export const useChatStore = defineStore('chat', () => {
  const connected = ref<boolean>(false);
  const userConnected = ref<UserType>();
  const socket = ref();
  const stompClient = ref<Client>();

  const roomList = ref<RoomType[]>([]);

  const chatUser = ref<ChatType>({
    chatHistory: []
  });

  const userOnlineList = ref<UserType[]>([]);

  const chatMessageContent = ref<string>('');
  const selectedRoom = ref<RoomType>();

  const connect = (username: string) => {
    const URL = import.meta.env.VITE_BACKEND_URL + '/ws-chatapp';
    socket.value = new SockJS(URL);
    stompClient.value = Stomp.over(socket.value);


    stompClient.value.connect(
        {},
        async (frame) => {
          connected.value = true;
          const responseUser = await axiosServices<UserType[]>('/api/v1/user-online');
          userOnlineList.value = responseUser.data;
          console.log(frame);

          const responseRoom = await axiosServices<RoomType[]>('/api/v1/room');
          roomList.value = responseRoom.data;

          stompClient.value?.subscribe('/topic/user', onUserOnline);
          stompClient.value?.subscribe('/topic/room', onRoomCreated);

          stompClient.value?.send(`/ws/user.connect/${username}`);

          window.addEventListener('beforeunload', disconnect);
        },
        (error: Frame | CloseEvent) => {
          console.log(error);
          console.log('Error de conexión');
        }
    );
  };

  const disconnect = () => {
    if (stompClient.value) {
      stompClient.value?.send(`/ws/user.disconnect/${userConnected.value?.username}`)
      stompClient.value?.disconnect();
    }
    connected.value = false;
  };

  const onUserOnline = (message: Message) => {
    const payload: UserType = JSON.parse(message.body);
    if (!userConnected.value) {
      userConnected.value = payload;
    }

    if (payload.online) {
      userOnlineList.value = userOnlineList.value.filter((user: UserType) => user.username !== payload.username)
      userOnlineList.value.unshift(payload);
    } else {
      userOnlineList.value = userOnlineList.value.filter((user: UserType) => user.username !== payload.username);
      userConnected.value = undefined;
    }
  }

  const onRoomCreated = (message: Message) => {
    console.log("Payload recibido");
    console.log(message);
    const room: RoomType = JSON.parse(message.body);
    const imgPortraitDataURL: string = room.imgPortrait as string;
    room.imgPreview = new Image();
    room.imgPreview.src = imgPortraitDataURL;

    roomList.value = [...roomList.value, room];
  }

  const sendRoom = (room: RoomType) => {
    console.log("Enviando imagen")
    room.imgPortrait = room.imgPreview?.src
    stompClient.value?.send('/ws/room.create-room', JSON.stringify(room));
  }

  const subscribeRoom = (room: RoomType, onChatUpdate: (body: ChatMessageType) => void) => {
    selectedRoom.value = room;
    stompClient.value?.subscribe(`/topic/chat/room/${room.id}`, (message: Message) => {
      const payload: ChatMessageType = JSON.parse(message.body);
      selectedRoom.value?.chatMessages?.push(payload)
      onChatUpdate(payload);
    });
  }

  const sendMessage = () => {
    const chatMessage: ChatMessageType = {
      sender: chatUser.value.username,
      body: chatMessageContent.value,
      type: MessageType.CHAT,
      format: MessageFormat.TEXT,
      roomId: selectedRoom.value?.id,
      userId: userConnected.value?.username
    };

    stompClient.value?.send('/ws/chat.send-message-room', JSON.stringify(chatMessage));
    chatMessageContent.value = '';
  };

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

  const getChatRoom = () => {

  }

  const getUserOnlineList = () => {
    return userOnlineList.value;
  }

  const getRoomList = () : RoomType[] => {
    return roomList.value
  }

  return {chatMessageContent, socket, stompClient, connect, selectedRoom, subscribeRoom,
    sendMessage, disconnect, getChatUser, getUserOnlineList, getRoomList, sendRoom,
    userConnected
  };
});