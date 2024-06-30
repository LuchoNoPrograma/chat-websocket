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
    id?: string,
    username?: string,
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
    sender?: string;
    content?: string;
    type: MessageType;
    messageFormat: MessageFormat;
    sendDate?: Date;
};
