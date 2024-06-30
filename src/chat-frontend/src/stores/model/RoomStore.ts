import {defineStore} from "pinia";
import {ref} from "vue";
import type {RoomType} from "@/types/model/RoomTypes";
import axiosServices from "@/utils/axios";

export const useRoomStore = defineStore('room', () => {
  const roomSelected = ref<RoomType>();

  const getRoomById = async (id: string) => {
    const response = await axiosServices<RoomType>(`/api/v1/room/${id}`);
    roomSelected.value = response.data;

    return roomSelected.value;
  }

  return {getRoomById};
})