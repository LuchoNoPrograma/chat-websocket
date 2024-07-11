import {defineStore} from "pinia";
import {ref} from "vue";
import type {Client} from "webstomp-client";
import Stomp, {Frame} from "webstomp-client";
/*import SockJS from "sockjs-client";*/
import SockJS from 'sockjs-client/dist/sockjs.js';
import {useUserStore} from "@/stores/userStore";
import {useRoomStore} from "@/stores/roomStore";

export const useWebSocketStore = defineStore('web-socket', () => {
  const socket = ref<any>();
  const stompClient = ref<Client>();
  const userStore = useUserStore();
  const roomStore = useRoomStore();

  const connect = (username: string) => {
    const URL = import.meta.env.VITE_BACKEND_URL + '/ws-chatapp';
    socket.value = new SockJS(URL);
    stompClient.value = Stomp.over(socket.value);

    stompClient.value.connect(
        {},
        async (frame) => {
          console.log(frame);

          await userStore.subscribeTopicUser();
          await roomStore.subscribeTopicRoom();
          stompClient.value?.send(`/ws/user.connect/${username}`);

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
      stompClient.value?.send(`/ws/user.disconnect/${userStore.userConnected?.username}`)
      stompClient.value?.disconnect();
    }
  };

  return {connect, disconnect, stompClient};
});