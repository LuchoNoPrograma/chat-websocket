// src/socket.js
import { reactive } from 'vue';
import SockJS from 'sockjs-client';
import Stomp from 'webstomp-client';
import axiosServices from '@/utils/axios';
import type { UserType, RoomType } from '@/types/model';

const socketState = reactive({
  socket: null as any,
  stompClient: null as any,
  connected: false,
  userConnected: null as UserType | null,

  roomList: [] as RoomType[],
  userOnlineList: [] as UserType[],
  chatHistory: [] as any[],
});

const connect = async (username: string) => {
  const URL = import.meta.env.VITE_BACKEND_URL + '/ws-chatapp';
  socketState.socket = new SockJS(URL);
  socketState.stompClient = Stomp.over(socketState.socket);

  socketState.stompClient.connect({}, async (frame: any) => {
    socketState.connected = true;
    const responseUser = await axiosServices<UserType[]>('/api/v1/user-online');
    socketState.userOnlineList = responseUser.data;

    const responseRoom = await axiosServices<RoomType[]>('/api/v1/room');
    socketState.roomList = responseRoom.data;

    socketState.stompClient.subscribe('/topic/user', onUserOnline);
    socketState.stompClient.subscribe('/topic/room', onRoomCreated);

    socketState.stompClient.send(`/ws/user.connect/${username}`);

    window.addEventListener('beforeunload', disconnect);
  }, (error: any) => {
    console.log('Error de conexión', error);
  });
};

const disconnect = () => {
  if (socketState.stompClient) {
    socketState.stompClient.send(`/ws/user.disconnect/${socketState.userConnected?.username}`);
    socketState.stompClient.disconnect();
  }
  socketState.connected = false;
};

const onUserOnline = (message: any) => {
  const payload: UserType = JSON.parse(message.body);
  if (!socketState.userConnected) {
    socketState.userConnected = payload;
  }

  if (payload.online) {
    socketState.userOnlineList = socketState.userOnlineList.filter(user => user.username !== payload.username);
    socketState.userOnlineList.unshift(payload);
  } else {
    socketState.userOnlineList = socketState.userOnlineList.filter(user => user.username !== payload.username);
  }
};

const onRoomCreated = (message: any) => {
  const room: RoomType = JSON.parse(message.body);
  room.imgPreview = new Image();
  room.imgPreview.src = room.imgPortrait as string;
  socketState.roomList.push(room);
};

const sendRoom = (room: RoomType) => {
  room.imgPortrait = room.imgPreview?.src;
  socketState.stompClient.send(`/ws/room.create-room`, JSON.stringify(room));
};

const sendMessage = (chatMessage: any) => {
  socketState.stompClient.send('/ws/chat.send-message', JSON.stringify(chatMessage));
  socketState.chatHistory.push(chatMessage);
};

export {
  socketState,
  connect,
  disconnect,
  sendRoom,
  sendMessage,
};
