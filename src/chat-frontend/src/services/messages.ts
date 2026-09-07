import type { ChatMessage } from '../types/chat';

export const messageKey = (message: ChatMessage) => message.id
  ?? [message.roomId, message.recipientId, message.type, message.userId, message.createdAt, message.replyToId, message.body].join('|');

export const mergeMessages = (...groups: ChatMessage[][]) => {
  const unique = new Map<string, ChatMessage>();
  groups.flat().forEach((message) => {
    const previous = unique.get(messageKey(message));
    unique.set(messageKey(message), { ...message, readBy: { ...previous?.readBy, ...message.readBy }, deliveredTo: { ...previous?.deliveredTo, ...message.deliveredTo } });
  });
  return [...unique.values()].sort((left, right) => {
    const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
    const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
    if (leftTime !== rightTime) return leftTime - rightTime;
    return (left.id ?? '').localeCompare(right.id ?? '');
  });
};
