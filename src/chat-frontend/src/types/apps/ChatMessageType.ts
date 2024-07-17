import type {UserType} from "@/types/model/UserTypes";

export type ChatType = {
    userRecipient?: UserType,
    chatHistory: ChatMessageType[],
}

export enum MessageType  {
    JOIN= "JOIN",
    LEAVE= "LEAVE",
    CHAT= "CHAT"
}

export enum MessageFormat {
    TEXT= "TEXT",
    IMG= "IMG",
    PDF= "PDF"
}

export type ChatMessageType = {
    id?: string,
    body?: string;
    type: MessageType;
    format: MessageFormat;
    createdAt?: Date;

    roomId?: string;
    userId?: string;
};