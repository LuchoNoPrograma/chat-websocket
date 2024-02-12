import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { ChatMessageType, ChatType } from '@/types/apps/ChatMessageType';
import { MessageFormat, MessageType } from '@/types/apps/ChatMessageType';
import SockJS from 'sockjs-client/dist/sockjs.js';
import Stomp from 'webstomp-client';
import type { Message, Client } from 'webstomp-client';

// project imports

export const useChatStore = defineStore('chat', () => {
    const connected = ref<boolean>(false);
    const socket = ref();
    const stompClient = ref<Client>();
    const chatUser = ref<ChatType>({
        chatHistory: []
    });
    const chatMessageContent = ref<string>();

    const connect = (username: string) => {
        console.log('Iniciando conexion con SockJS');
        const URL = import.meta.env.VITE_BACKEND_URL + '/ws-chatapp';
        console.log(URL);
        socket.value = new SockJS(URL);
        stompClient.value = Stomp.over(socket.value);

        stompClient.value.connect(
            {},
            (frame) => {
                connected.value = true;
                console.log(frame);

                stompClient.value?.subscribe('/topic/chat', onMessageReceived);
                stompClient.value?.send(
                    '/app/chat.add-user',
                    JSON.stringify({
                        sender: username,
                        type: MessageType.JOIN,
                        sendDate: new Date(),
                    })
                );
            },
            () => {
                console.log('Error de conexión');
            }
        );
    };
    const onMessageReceived = (message: Message) => {
        const payload: ChatMessageType = JSON.parse(message.body);
        chatUser.value.chatHistory.push(payload);
    };

    const sendMessage = () => {
        const chatMessage: ChatMessageType = {
            sender: chatUser.value.username,
            content: chatMessageContent.value,
            type: MessageType.CHAT,
            messageFormat: MessageFormat.TEXT
        };

        stompClient.value?.send('/app/chat.send-message', JSON.stringify(chatMessage));
        chatMessageContent.value = '';
        chatUser.value.chatHistory.push(chatMessage);
    };

    const disconnect = () => {
        if (stompClient.value) {
            stompClient.value?.disconnect();
        }
        connected.value = false;
    };

    const getChatUser = () => {
        if (!chatUser.value.username)
            chatUser.value = {
                username: 'User 1',
                chatHistory: chatUser.value.chatHistory
            };

        return chatUser.value;
    };

    return { chatMessageContent, connect, sendMessage, disconnect, getChatUser };
});
