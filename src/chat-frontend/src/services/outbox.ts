import { sessionCredential } from './session';
import { ref, watch } from 'vue';
import type { ChatMessage } from '../types/chat';

export type PendingMessage = { message: ChatMessage; state: 'sending' | 'unconfirmed'; lastAttempt: number };
/** One pending command per conversation; retries retain the original identity and payload. */
export function createOutbox() {
  const pending = ref<Record<string, PendingMessage>>({});
  const storageKey = () => { const session = sessionCredential(); return session ? `chatty-outbox:${session.generation}:${session.user.username}` : undefined; };
  let activeKey: string | undefined;
  const restore = () => {
    activeKey = storageKey();
    for (const key of Object.keys(sessionStorage)) {
      if (key.startsWith('chatty-outbox:') && key !== activeKey) sessionStorage.removeItem(key);
    }
    try { pending.value = JSON.parse((activeKey && sessionStorage.getItem(activeKey)) || '{}'); }
    catch { pending.value = {}; }
  };
  watch(pending, entries => { if (activeKey) sessionStorage.setItem(activeKey, JSON.stringify(entries)); }, { deep: true, flush: 'sync' });
  const enqueue = (key: string, message: ChatMessage) => {
    const previous = pending.value[key];
    if (previous && previous.message.body === message.body && previous.message.replyToId === message.replyToId) return previous;
    if (previous) throw new Error('Resuelve el envío pendiente antes de enviar otro mensaje.');
    const entry: PendingMessage = { message: { ...message, clientMessageId: crypto.randomUUID() }, state: 'sending', lastAttempt: 0 };
    pending.value[key] = entry;
    return entry;
  };
  const acknowledge = (message: Pick<ChatMessage, 'clientMessageId'>) => {
    for (const [key, entry] of Object.entries(pending.value)) {
      if (entry.message.clientMessageId === message.clientMessageId) delete pending.value[key];
    }
  };
  return { pending, enqueue, acknowledge, restore, clear: () => { pending.value = {}; if (activeKey) sessionStorage.removeItem(activeKey); activeKey = undefined; } };
}
