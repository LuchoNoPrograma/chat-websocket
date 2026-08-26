export type ConnectionState = 'idle' | 'connecting' | 'connected' | 'error';

export type User = {
  username: string;
  online: boolean;
};

export type Tag = {
  id: string;
  name: string;
  pathIcon?: string;
};

export type Room = {
  id: string;
  name: string;
  description: string;
  activeUsers?: number;
  imgPortrait?: string;
  createdAt?: string;
  tags?: Tag[];
  chatMessages?: ChatMessage[];
};

export type MessageType = 'JOIN' | 'LEAVE' | 'CHAT';
export type MessageFormat = 'TEXT' | 'IMG' | 'PDF';

export type ChatMessage = {
  id?: string;
  body?: string;
  type: MessageType;
  format: MessageFormat;
  createdAt?: string;
  roomId?: string;
  userId?: string;
};
