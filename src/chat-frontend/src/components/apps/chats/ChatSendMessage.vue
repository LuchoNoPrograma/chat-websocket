<script lang="ts" setup>
import { useChatStore } from '@/stores/apps/chatStore';
import type { ChatMessageType } from '@/types/apps/ChatMessageType';

const chatStore = useChatStore();

const addMessageTextAndClear = () => {
    if(chatStore.chatMessageContent?.length === 0){
      return
    }
    chatStore.sendMessage();
}


const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' && event.shiftKey) {
    event.preventDefault();
    chatStore.chatMessageContent += '\n'; // Agregar una nueva línea al contenido del mensaje
  } else if (event.key === 'Enter') {
    event.preventDefault();
    addMessageTextAndClear();
  }
}
</script>

<template>
    <div class="d-flex align-center pa-4">
        <v-btn class="text-medium-emphasis" icon variant="text">
            <mood-smile-icon size="24"></mood-smile-icon>
        </v-btn>

        <v-textarea
            v-model="chatStore.chatMessageContent"
            color="primary"
            density="compact"
            hide-details
            placeholder="Escribe un mensaje"
            variant="solo"
            autocomplete="off"
            rows="1"
            @keydown="handleKeyDown"
        >
        </v-textarea>
        <v-btn :disabled="chatStore.chatMessageContent == ''" class="text-medium-emphasis" icon type="submit" variant="text"
               @click="chatStore.sendMessage()"
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
