<script lang="ts" setup>
import {nextTick, onBeforeUnmount, onMounted, ref} from 'vue';
import {useChatStore} from '@/stores/apps/chatStore';
import {formatDistanceToNowStrict} from 'date-fns';
import ChatSendMsg from './ChatSendMessage.vue';
import {useDisplay} from 'vuetify';
import {es} from 'date-fns/locale';

import {type ChatMessageType, MessageFormat, MessageType} from '@/types/apps/ChatMessageType';
import {useRoomStore} from "@/stores/model/RoomStore";
import {useRouter} from "vue-router";

const {lgAndUp} = useDisplay();

const roomStore = useRoomStore();
const chatStore = useChatStore();
const router = useRouter();

const Rpart = ref(lgAndUp ? true : false);

function toggleRpart() {
  Rpart.value = !Rpart.value;
}

const colors = [
  '#e71c10', '#ff2683', '#bc07dc', '#314ad0',
  '#2196F3', '#129f18', '#d78f0d', '#ea4715'
];

const userColors = new Map();

function getRandomColor() {
  if (colors.length === 0) return '#000000'; // Default color if colors run out
  const randomIndex = Math.floor(Math.random() * colors.length);
  return colors.splice(randomIndex, 1)[0];
}

function getUserColor(userId) {
  if (!userColors.has(userId)) {
    const color = getRandomColor();
    userColors.set(userId, color);
  }
  return userColors.get(userId);
}

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

const handleArrowClickToBottom = () => {
  scrollToBottom();
  newMessageCount.value = 0;
  /*newMessageFromOthers.value = false;*/
}

onMounted(async () => {
  chatStore.selectedRoom = await roomStore.getRoomById(router.currentRoute.value.params.roomId as string)
  chatStore.subscribeRoom(chatStore.selectedRoom, (chatMessage: ChatMessageType) => {
    nextTick(() => {
      const isCurrentUser = chatMessage.userId === chatStore.userConnected.username;
      if (isCurrentUser) {
        scrollToBottom();
      } else {
        newMessageCount.value += 1;
        newMessageFromOthers.value = true;
      }
    });
  });

  updateTimes();
  window.addEventListener('focus', updateTimes);

  scrollToBottom();
});

onBeforeUnmount(() => {
  window.removeEventListener('focus', updateTimes);
});
</script>
<template>
  <div class="customHeight">
    <v-sheet class="mx-4 py-2 px-4 rounded elevation-10 d-flex align-center gap-3">
      <div class="d-inline">
        <img class="overflow-hidden" style="border-radius: 100%; object-fit: cover" width="48px" height="48px" :src="chatStore?.selectedRoom?.imgPortrait" alt="1">
      </div>
      <h3 class="font-weight-medium d-inline">
        {{ chatStore.selectedRoom?.name }}
        Eres: {{ chatStore.userConnected }}
      </h3>
    </v-sheet>
    <v-divider/>
    <!---Chat History-->
    <v-container>
      <div class="container__new-message d-flex ga-1 align-center">
<!--        <span class="bg-info elevation-10 py-1 px-2 text-subtitle-2" style="border-radius: 100%">{{newMessageCount}}</span>-->
        <v-btn @click="handleArrowClickToBottom" class="elevation-10"
               color="info" size="x-small" icon variant="flat">
          <v-badge :content="newMessageCount" color="error" :offset-y="-10" :offset-x="-5">
            <v-icon size="18" icon="mdi-chevron-double-down"></v-icon>
          </v-badge>
        </v-btn>
      </div>
      <perfect-scrollbar class="rightpartHeight" :options="{minScrollbarLength: 20}" ref="scrollbarApi">
        <div class="d-flex flex-column">
          <div v-for="chatMessage in chatStore.selectedRoom?.chatMessages" :key="chatMessage.id" class="px-4 pt-4"
               >
            <div v-if="chatMessage.userId === chatStore.userConnected.username"
                 class="justify-end d-flex text-end mb-1">
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
                <v-sheet v-if="chatMessage.type === MessageType.JOIN" class="mx-auto">
                  {{ chatMessage.userId }} se ha unido!
                </v-sheet>

                <!--                <v-sheet
                                    v-if="chatMessage.messageFormat !== MessageFormat.IMG"
                                    class="bg-grey100 rounded-md px-3 py-2 mb-1"
                                >
                                  <p class="text-body-1">{{ chatMessage.content }}</p>
                                </v-sheet>
                                <v-sheet v-if="chatMessage.messageFormat === MessageFormat.IMG" class="mb-1">
                                  <img :src="chatMessage.content" alt="pro" class="rounded-md" width="250"/>
                                </v-sheet>-->
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
                <span class="message__username" :style="{ color: getUserColor(chatMessage.userId) }">
                  ~ {{ chatMessage.userId }}
                </span>
                  <p class="text-body-1" style="white-space: pre-line">{{ chatMessage.body }}</p>
                </v-sheet>
                <small v-if="chatMessage.createdAt" class="text-subtitle-2 ml-1">
                  {{ chatMessage.relativeTime ? `Hace ${chatMessage.relativeTime}` : 'Ahora' }}
                </small>
                <!--                <v-sheet v-if="chatMessage.format === MessageFormat.IMG" class="mb-1">
                                  <img :src="chatMessage.body" alt="pro" class="rounded-md" width="250"/>
                                </v-sheet>-->
              </div>
            </div>
          </div>
        </div>
      </perfect-scrollbar>
    </v-container>
    <v-divider/>
    <!---Chat send-->
    <v-container>
      <chat-send-msg></chat-send-msg>
    </v-container>
  </div>
</template>
<style lang="scss">
.message__username {
  font-size: 0.825em;
}

.container__new-message {
  position: absolute;
  bottom: 80px;
  right: 20px;
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