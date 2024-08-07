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
    userOnlineList.value = userOnlineList.value.filter(user => user.username !== userConnected.value?.username);
    userOnlineList.value.unshift(userConnected.value as UserType);

    webSocketStore?.stompClient?.subscribe('/topic/user', (message: Message) => {
      const payload: UserType = JSON.parse(message.body);

      userOnlineList.value = userOnlineList.value.filter(user => user.username !== payload.username);
      if (payload.online && payload.username !== userConnected.value?.username) {
        userOnlineList.value.push(payload);
      }
    })
  }
  return {userConnected, userOnlineList, subscribeTopicUser}
})