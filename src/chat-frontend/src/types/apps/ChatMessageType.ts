import type {UserType} from "@/types/model/UserTypes";

type attachType = {
    icon?: string;
    file?: string;
    fileSize?: string;
};

type chatHistoryType = {
    createdAt?: any;
    msg: string;
    senderId: number | string;
    type: string;
    attachment: attachType[];
    id: string;
};

/*
export type ChatType = {
    id: number;
    name: string;
    status: string;
    thumb: string;
    recent: boolean;
    chatHistory: chatHistoryType[];
};
*/
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
    IMG= "IMG"
}

export type ChatMessageType = {
    id?: string,
    body?: string;
    type: MessageType;
    format: MessageFormat;
    createdAt?: Date;

    roomId?: string;
    userId?: string;
    userRecipientId?: string;
};

/*
private String userId;
private String roomId;

private String body;
private MessageType type;
private MessageFormat format;
private ZonedDateTime createdAt;*/
