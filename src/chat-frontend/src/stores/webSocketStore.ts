import {defineStore} from "pinia";
import {ref} from "vue";
import type {Client} from "webstomp-client";
import Stomp, {Frame} from "webstomp-client";
/*import SockJS from "sockjs-client";*/
import SockJS from 'sockjs-client/dist/sockjs.js';
import {useUserStore} from "@/stores/userStore";
import {useRoomStore} from "@/stores/roomStore";
import {useChatStore} from "@/stores/chatStore";
import axiosServices from "@/utils/axios";
import type {UserType} from "@/types/model/UserTypes";

export const useWebSocketStore = defineStore('web-socket', () => {
  const socket = ref<any>();
  const stompClient = ref<Client>();
  const userStore = useUserStore();
  const roomStore = useRoomStore();
  const chatStore = useChatStore();

  const connect = (username: string) => {
    const URL = import.meta.env.VITE_BACKEND_URL + '/ws-chatapp';
    socket.value = new SockJS(URL);
    stompClient.value = Stomp.over(socket.value);

    stompClient.value.connect(
        {login: username},
        async (frame) => {
          console.log(frame);

          const response = await axiosServices.post<UserType>("/api/v1/auth", null,  {
            params: {username: username}
          })
          userStore.userConnected = response.data;

          await userStore.subscribeTopicUser();
          await roomStore.subscribeTopicRoom();

          window.addEventListener('beforeunload', disconnect);
        },
        (error: Frame | CloseEvent) => {
          console.log(error);
          console.log('Error de conexión');
        }
    );
  }

  const disconnect = () => {
    if (stompClient.value) {
      window.removeEventListener('beforeunload', disconnect);
      chatStore.subscribedChatRooms = {};
      stompClient.value?.disconnect();
    }
  };

  return {connect, disconnect, stompClient};
});