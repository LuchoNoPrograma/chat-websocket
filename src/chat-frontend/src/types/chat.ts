export type ConnectionState = 'idle' | 'connecting' | 'connected' | 'error';

export type AvatarId = 'claudia' | 'sol' | 'luna' | 'jimenez' | 'dickson' | 'kimi';

export const AVATAR_OPTIONS: { id: AvatarId; label: string }[] = [
  { id: 'claudia', label: 'Claudia' },
  { id: 'sol', label: 'Sol' },
  { id: 'luna', label: 'Luna' },
  { id: 'jimenez', label: 'Jimenez' },
  { id: 'dickson', label: 'Dickson' },
  { id: 'kimi', label: 'Kimi' }
];

export type User = {
  username: string;
  online: boolean;
  avatarId?: AvatarId;
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
};

export type MessageType = 'JOIN' | 'LEAVE' | 'CHAT';
export type MessageFormat = 'TEXT' | 'IMG' | 'PDF';

export type ReceiptEvent = { type: 'MESSAGE_DELIVERED' | 'MESSAGE_READ'; messageId: string; roomId?: string; userId: string; recipientId?: string; username: string; deliveredAt?: string; readAt?: string };

export type ChatMessage = {
  clientMessageId?: string;
  deliveredTo?: Record<string, string>;
  readBy?: Record<string, string>;
  id?: string;
  body?: string;
  type: MessageType;
  format: MessageFormat;
  createdAt?: string;
  roomId?: string;
  userId?: string;
  recipientId?: string;
  replyToId?: string;
  replyToUserId?: string;
  replyToBody?: string;
  replyToCreatedAt?: string;
};

export type MessagePage = {
  messages: ChatMessage[];
  nextCursor?: string;
  hasMore: boolean;
};

export type RoomActivity = {
  unread: number;
  replies: number;
};

export type SessionPolicy = {
  idleTimeoutSeconds: number;
  warningBeforeSeconds: number;
};
