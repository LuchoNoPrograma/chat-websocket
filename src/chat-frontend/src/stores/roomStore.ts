import {defineStore} from "pinia";
import type {RoomType} from "@/types/model/RoomTypes";
import {ref} from "vue";
import {useWebSocketStore} from "@/stores/webSocketStore";
import type {Message, Subscription} from "webstomp-client";
import {useChatStore} from "@/stores/chatStore";
import axiosServices from "@/utils/axios";

export const useRoomStore = defineStore('room', () => {
  const webSocketStore = useWebSocketStore();

  const roomList = ref<RoomType[]>([]);
  const roomSelected = ref<RoomType>();

  const subscribeTopicRoom = async () => {
    const response = await axiosServices.get<RoomType[]>('/api/v1/room')
    roomList.value = response.data;

    webSocketStore.stompClient?.subscribe('/topic/room', (message: Message) => {
      const room: RoomType = JSON.parse(message.body);
      const roomExists = roomList.value.find(item => item.id === room.id);
      if(!roomExists){
        const imgPortraitDataURL: string = room.imgPortrait as string;
        room.imgPreview = new Image();
        room.imgPreview.src = imgPortraitDataURL;
        roomList.value = [...roomList.value, room];
      }else{
        roomExists.activeUsers = room.activeUsers;
      }
    });
  }

  const createRoom = (room: RoomType) => {
    room.imgPortrait = room.imgPreview?.src //Sending img as string
    webSocketStore.stompClient?.send('/ws/room.create-room', JSON.stringify(room));
  }


  return {roomList, roomSelected, createRoom, subscribeTopicRoom};
});