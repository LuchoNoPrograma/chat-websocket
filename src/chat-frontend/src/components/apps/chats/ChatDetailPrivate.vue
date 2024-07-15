<script lang="ts" setup>
import {nextTick, onBeforeUnmount, onMounted, ref, watchEffect} from 'vue';
import {useChatStore} from '@/stores/chatStore';
import {formatDistanceToNowStrict} from 'date-fns';
import ChatSendMsg from './ChatSendMessage.vue';
import {es} from 'date-fns/locale';

import {type ChatMessageType, MessageFormat, MessageType} from '@/types/apps/ChatMessageType';
import {useRouter} from "vue-router";
import {useUserStore} from "@/stores/userStore";

import maleProfile from "@/assets/images/chat/default-male-profile.png";
const chatStore = useChatStore();
const userStore = useUserStore();
function updateTimes() {
  const chatMessages = chatStore.selectedRoom?.chatMessages;
  if (chatMessages) {
    chatMessages.forEach((message: any) => {
      message.relativeTime = formatDistanceToNowStrict(new Date(message.createdAt), {
        addSuffix: false,
        locale: es
      });
    });
  }
}

const newMessageFromOthers = ref(false);
const newMessageCount = ref(0);
const scrollbarApi = ref<any>(null);

const scrollToBottom = () => {
  const container = scrollbarApi.value?.ps?.element;
  if (container) {
    container.scrollTop = container.scrollHeight;
  }
}

const scrollToBottomIfNearBottom  = () => {
  const container = scrollbarApi.value?.ps?.element;
  console.log(container)
  const distanceFromBottom = container.scrollHeight - container.scrollTop;

  console.log('distanceFromBottom:', distanceFromBottom);
  if (distanceFromBottom <= 1000) {
    container.scrollTop = container.scrollHeight;
  }else{
    newMessageCount.value += 1;
    newMessageFromOthers.value = true;
  }
}

const clearCountNewMessages = () => {
  newMessageCount.value = 0;
  newMessageFromOthers.value = false;
}

onMounted(async () => {
  //Chat history with target user is already laoded in parent component

  updateTimes();
  window.addEventListener('focus', updateTimes);

  scrollToBottom();
});

onBeforeUnmount(() => {
  window.removeEventListener('focus', updateTimes);
});

const sendMessageToUserAndClear = () => {
  chatStore.sendMessage({
    body: chatStore.chatMessageContent,
    type: MessageType.CHAT,
    format: MessageFormat.TEXT,
    userRecipientId: chatStore.chatUserPrivate.userRecipient?.username,
    userId: userStore.userConnected?.username
  }, 'PRIVATE');
}
</script>
<template>
  <div class="customHeight">
    <v-sheet class="mx-4 py-2 px-4 rounded elevation-10 d-flex align-center justify-space-between">
      <div class="d-flex align-center gap-3">
        <img class="d-inline overflow-hidden" style="border-radius: 100%; object-fit: cover" width="48px" height="48px"
             :src="maleProfile" alt="1">
        <h3 class="font-weight-medium d-inline">
          {{ chatStore.chatUserPrivate?.userRecipient?.username }}
          Eres: {{ userStore.userConnected?.username }}
        </h3>
      </div>
      <v-menu location="bottom">
        <template v-slot:activator="{ props }">
          <v-btn icon variant="plain" v-bind="props">
            <v-icon icon="mdi-dots-vertical"></v-icon>
          </v-btn>
        </template>
      </v-menu>
    </v-sheet>
    <v-divider/>
    <!---Chat History-->
    <v-container>
      <perfect-scrollbar class="rightpartHeight" :options="{minScrollbarLength: 20}" ref="scrollbarApi" @ps-y-reach-end="clearCountNewMessages">
        <div class="d-flex flex-column">
          <div v-for="chatMessage in chatStore.chatUserPrivate.chatHistory" :key="chatMessage.id" class="px-4 pt-4"
          >
            <div v-if="chatMessage.userId === userStore.userConnected?.username" class="justify-end d-flex text-end mb-1">
              <div class="d-flex flex-column align-end">
                <v-sheet
                    color="info"
                    v-if="chatMessage.format === MessageFormat.TEXT"
                    class="rounded-md px-2 py-1 mb-1 text-left"
                    style="max-width: 320px; width: fit-content"
                >
                  <p class="text-body-1" style="white-space: pre-line">{{ chatMessage.body }}</p>
                </v-sheet>
                <small v-if="chatMessage" class="text-subtitle-2 ml-1">
                  {{ chatMessage.relativeTime ? `Hace ${chatMessage.relativeTime}` : 'Ahora' }}
                </small>
              </div>
            </div>
            <div v-else class="d-flex align-items-start gap-3 mb-1">
              <div>
                <v-sheet
                    elevation="10"
                    v-if="chatMessage.format === MessageFormat.TEXT"
                    color="surface"
                    class="rounded-md px-3 py-1"
                    style="max-width: 320px;"
                >
                  <p class="text-body-1" style="white-space: pre-line">{{ chatMessage.body }}</p>
                </v-sheet>
                <small v-if="chatMessage.createdAt" class="text-subtitle-2 ml-1">
                  {{ chatMessage.relativeTime ? `Hace ${chatMessage.relativeTime}` : 'Ahora' }}
                </small>
              </div>
            </div>
          </div>
        </div>
      </perfect-scrollbar>
    </v-container>
    <v-divider/>
    <!--    v-if="newMessageFromOthers"-->
    <div class="container__new-message d-flex ga-1 align-center">
      <v-btn @click="scrollToBottom" class="elevation-10"
             color="info" size="x-small" icon variant="flat">
        <v-badge :content="newMessageCount" color="error" :offset-y="-10" :offset-x="-5">
          <v-icon size="18" icon="mdi-chevron-double-down"></v-icon>
        </v-badge>
      </v-btn>
    </div>
    <chat-send-msg @send-message="sendMessageToUserAndClear"></chat-send-msg>
  </div>
</template>
<style lang="scss">
.message__username {
  font-size: 0.825em;
}

.container__new-message {
  position: absolute;
  bottom: 21.5%;
  right: 11.5%;
}

.rightpartHeight {
  height: 60vh;
}

.badg-dotDetail {
  left: -9px;
  position: relative;
  bottom: -10px;
}

.toggleLeft {
  position: absolute;
  right: 15px;
  top: 15px;
}

.right-sidebar {
  width: 320px;
  border-left: 1px solid rgb(var(--v-theme-borderColor));
  transition: 0.1s ease-in;
  flex-shrink: 0;
}

.HideLeftPart {
  display: none;
}

@media (max-width: 960px) {
  .right-sidebar {
    position: absolute;
    right: -320px;

    &.showLeftPart {
      right: 0;
      z-index: 2;
      box-shadow: 2px 1px 20px rgba(0, 0, 0, 0.1);
    }
  }
  .boxoverlay {
    position: absolute;
    height: 100%;
    width: 100%;
    z-index: 1;
    background: rgba(0, 0, 0, 0.2);
  }
}
</style>