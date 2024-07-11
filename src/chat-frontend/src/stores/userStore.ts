import {defineStore} from "pinia";
import {ref} from "vue";
import type {UserType} from "@/types/model/UserTypes";
import {useWebSocketStore} from "@/stores/webSocketStore";
import type {Message} from "webstomp-client";
import axiosServices from "@/utils/axios";

export const useUserStore = defineStore('user', () => {
  const webSocketStore = useWebSocketStore();

  const userConnected = ref<UserType>();
  const userOnlineList = ref<UserType[]>([]);

  const subscribeTopicUser = async () => {
    const response = await axiosServices.get('/api/v1/user-online')
    userOnlineList.value = response.data;

    webSocketStore?.stompClient?.subscribe('/topic/user', (message: Message) => {
      const payload: UserType = JSON.parse(message.body);

      if(!userConnected.value){
        userConnected.value = payload;
      }
      userOnlineList.value = userOnlineList.value.filter(user => user.username !== payload.username);
      if(payload.online){
        userOnlineList.value.unshift(payload);
      }else{
        userConnected.value = undefined;
      }
    })
  }
  return {userConnected, userOnlineList, subscribeTopicUser}
})