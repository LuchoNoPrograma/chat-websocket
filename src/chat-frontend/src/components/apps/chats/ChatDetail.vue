<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { useChatStore } from '@/stores/apps/chat';
import { formatDistanceToNowStrict } from 'date-fns';
import ChatSendMsg from './ChatSendMsg.vue';
import { useDisplay } from 'vuetify';
import { es } from 'date-fns/locale';

import type { ChatType } from '@/types/apps/ChatMessageType';
import { MessageFormat, MessageType } from '@/types/apps/ChatMessageType';

const { lgAndUp } = useDisplay();

const chatStore = useChatStore();

onMounted(() => {});

const chatList = ref<ChatType[]>([]);

const Rpart = ref(lgAndUp ? true : false);

function toggleRpart() {
    Rpart.value = !Rpart.value;
}
</script>
<template>
    <div class="customHeight">
        <div>
            <v-divider />
            <!---Chat History-->
            <perfect-scrollbar class="rightpartHeight">
                <div class="d-flex flex-column">
                    <div v-for="chatMessage in chatStore.getChatUser().chatHistory" :key="chatMessage.id" class="pa-5">
                        <div class="justify-end d-flex text-end mb-1">
                            <div>
                                <small v-if="chatMessage.sendDate" class="text-medium-emphasis text-subtitle-2">
                                    Hace
                                    {{
                                        formatDistanceToNowStrict(new Date(chatMessage.sendDate as any), {
                                            addSuffix: false,
                                            locale: es
                                        })
                                    }}
                                </small>
                                <v-sheet v-if="chatMessage.type === MessageType.JOIN" class="mx-auto">
                                    {{ chatMessage.sender }} se ha unido!
                                </v-sheet>

                                <v-sheet
                                    v-if="chatMessage.messageFormat !== MessageFormat.IMG"
                                    class="bg-grey100 rounded-md px-3 py-2 mb-1"
                                >
                                    <p class="text-body-1">{{ chatMessage.content }}</p>
                                </v-sheet>
                                <v-sheet v-if="chatMessage.messageFormat === MessageFormat.IMG" class="mb-1">
                                    <img :src="chatMessage.content" alt="pro" class="rounded-md" width="250" />
                                </v-sheet>
                            </div>
                        </div>
                        <!--                        <div class="d-flex align-items-start gap-3 mb-1">
                <div>
                    <small v-if="chatMessage.sendDate" class="text-medium-emphasis text-subtitle-2">
                        {{ chatMessage.sender }},
                        {{
                            formatDistanceToNowStrict(new Date(chatMessage.sendDate as any), {
                                addSuffix: false
                            })
                        }}
                        ago
                    </small>

                    <v-sheet
                        v-if="chatMessage.messageFormat === MessageFormat.TEXT"
                        class="bg-grey100 rounded-md px-3 py-2 mb-1"
                    >
                        <p class="text-body-1">{{ chatMessage.content }}</p>
                    </v-sheet>
                    <v-sheet v-if="chatMessage.messageFormat === MessageFormat.IMG" class="mb-1">
                        <img :src="chatMessage.content" alt="pro" class="rounded-md" width="250" />
                    </v-sheet>
                </div>
            </div>-->
                    </div>
                </div>
            </perfect-scrollbar>
        </div>
        <v-divider />
        <!---Chat send-->
        <chat-send-msg></chat-send-msg>
    </div>
</template>
<style lang="scss">
.customHeight {
}

.rightpartHeight {
    height: 530px;
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