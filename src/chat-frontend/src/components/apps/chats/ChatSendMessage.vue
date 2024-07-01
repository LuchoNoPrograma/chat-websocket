<script lang="ts" setup>
import { useChatStore } from '@/stores/apps/chatStore';
import type { ChatMessageType } from '@/types/apps/ChatMessageType';

const chatStore = useChatStore();

const addMessageTextAndClear = (item: ChatMessageType) => {
    if (item.body?.length === 0) {
        return;
    }
    chatStore.sendMessage();
    chatStore.chatMessageContent = '';
}

</script>

<template>
    <form class="d-flex align-center pa-4" @submit.prevent="addMessageTextAndClear">
        <v-btn class="text-medium-emphasis" icon variant="text">
            <mood-smile-icon size="24"></mood-smile-icon>
        </v-btn>

        <v-text-field
            v-model="chatStore.chatMessageContent"
            class="shadow-none"
            color="primary"
            density="compact"
            hide-details
            placeholder="Type a Message"
            variant="solo"
        ></v-text-field>
        <v-btn :disabled="chatStore.chatMessageContent == undefined" class="text-medium-emphasis" icon type="submit" variant="text">
            <send-icon size="20"></send-icon>
        </v-btn>

        <v-btn class="text-medium-emphasis" icon variant="text">
            <photo-icon size="20"></photo-icon>
        </v-btn>
        <v-btn class="text-medium-emphasis" icon variant="text">
            <paperclip-icon size="20"></paperclip-icon>.
        </v-btn>
    </form>
</template>

<style>
.shadow-none .v-field--no-label {
    --v-field-padding-top: -7px;
}
</style>
