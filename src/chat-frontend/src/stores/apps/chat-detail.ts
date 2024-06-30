import {defineStore} from "pinia";
import {useChatStore} from "@/stores/apps/chat";

export const useChatDetailStore = defineStore('chatDetail', () => {
  const chatStore = useChatStore();

  const enterChatRoom = () => {
  }
})