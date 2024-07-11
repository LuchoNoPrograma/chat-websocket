<script lang="ts" setup>
import {useChatStore} from '@/stores/chatStore';
import {nextTick, ref} from "vue";
import {MessageFormat, MessageType} from "@/types/apps/ChatMessageType";
import {useRoomStore} from "@/stores/roomStore";
import {useUserStore} from "@/stores/userStore";

const chatStore = useChatStore();
const roomStore = useRoomStore();
const userStore = useUserStore();

const textareaRef = ref<any>();
const resizeTextarea = () => {
  if (textareaRef.value) {
    textareaRef.value.$el.querySelector('textarea').scrollTop = textareaRef.value.$el.querySelector('textarea').scrollHeight;
  }

}

const sendMessageAndClear = () => {
  if (chatStore.chatMessageContent?.length === 0) {
    return
  }

  chatStore.sendMessage({
    body: chatStore.chatMessageContent,
    type: MessageType.CHAT,
    format: MessageFormat.TEXT,
    roomId: roomStore.roomSelected?.id,
    userId: userStore.userConnected?.username
  });

  chatStore.chatMessageContent = '';

  nextTick(() => {
    resizeTextarea();
  });
}


const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && event.shiftKey) {
    event.preventDefault();
    chatStore.chatMessageContent += '\n';
    nextTick(() => {
      resizeTextarea();
    });
  } else if (event.key === 'Enter') {
    event.preventDefault();
    sendMessageAndClear();
  }
}
</script>

<template>
  <div class="d-flex align-center pa-4">
    <v-btn class="text-medium-emphasis" icon variant="text">
      <mood-smile-icon size="24"></mood-smile-icon>
    </v-btn>

    <v-textarea
        ref="textareaRef"
        v-model="chatStore.chatMessageContent"
        autocomplete="off"
        class="send-message__v-text-area"
        color="primary"
        density="compact"
        hide-details
        placeholder="Escribe un mensaje"
        variant="solo"
        rows="2"
        @keydown="handleKeyDown"
    >
    </v-textarea>
    <v-btn :disabled="chatStore.chatMessageContent?.replace(/\s/g,'') == ''" class="text-medium-emphasis" icon
           type="submit" variant="text"
           @click="sendMessageAndClear"
    >
      <send-icon size="20"></send-icon>
    </v-btn>

    <v-btn class="text-medium-emphasis" icon variant="text">
      <photo-icon size="20"></photo-icon>
    </v-btn>
    <v-btn class="text-medium-emphasis" icon variant="text">
      <paperclip-icon size="20"></paperclip-icon>
    </v-btn>
  </div>
</template>

<style>
.shadow-none .v-field--no-label {
  --v-field-padding-top: -7px;
}
</style>
