<script setup lang="ts">
import { motion } from 'motion-v';
import { computed, onMounted, onBeforeUnmount, ref } from 'vue';
import AppIcon from './AppIcon.vue';
import AppDialog from './AppDialog.vue';
import type { AvatarId, ChatMessage } from '../../types/chat';

const props = defineProps<{
  item: ChatMessage;
  currentUsername?: string;
  avatarId: AvatarId;
  grouped?: boolean;
  reduceMotion?: boolean;
}>();

const emit = defineEmits<{
  reply: [message: ChatMessage];
  read: [message: ChatMessage];
}>();

const bubble = ref<HTMLElement>();
const detailsOpen = ref(false);
const expanded = ref(false);
const isLong = computed(() => (props.item.body?.length ?? 0) > 650 || (props.item.body?.split('\n').length ?? 0) > 10);
const displayedBody = computed(() => isLong.value && !expanded.value
  ? `${(props.item.body ?? '').split('\n').slice(0, 10).join('\n').slice(0, 650).trimEnd()}…`
  : props.item.body);
const deliveries = computed(() => Object.entries(props.item.deliveredTo ?? {}).filter(([name]) => name !== props.item.userId));
const recipients = computed(() => [...new Set([...(props.item.recipientId ? [props.item.recipientId] : []), ...deliveries.value.map(([name]) => name), ...readers.value.map(([name]) => name)])]
  .sort((a, b) => a.localeCompare(b)).map(name => ({ name, deliveredAt: props.item.deliveredTo?.[name], readAt: props.item.readBy?.[name] })));

const readers = computed(() => Object.entries(props.item.readBy ?? {}).filter(([name]) => name !== props.item.userId));
const readLabel = computed(() => readers.value.length
  ? (props.item.roomId ? `Leído por ${readers.value.length}` : 'Leído')
  : deliveries.value.length ? (props.item.roomId ? `Entregado a ${deliveries.value.length}` : 'Entregado') : 'Enviado');
let observer: IntersectionObserver | undefined;
let timer: ReturnType<typeof setInterval> | undefined;
let visibleSince = 0;
const checkRead = () => {
  if (document.visibilityState !== 'visible' || !document.hasFocus()) { visibleSince = 0; return; }
  if (visibleSince && Date.now() - visibleSince >= 600) emit('read', props.item);
};
const refreshVisibility = () => {
  if (!bubble.value) return;
  const rect = bubble.value.getBoundingClientRect();
  const feed = bubble.value.closest('.message-feed')?.getBoundingClientRect();
  const top = Math.max(0, feed?.top ?? 0);
  const bottom = Math.min(innerHeight, feed?.bottom ?? innerHeight);
  const visible = Math.min(rect.bottom, bottom) - Math.max(rect.top, top) >= Math.min(60, rect.height);
  if (!visible || document.visibilityState !== 'visible' || !document.hasFocus() || bubble.value.closest('[inert]') || document.querySelector('dialog[open]') || (isLong.value && (!expanded.value || rect.bottom > bottom))) visibleSince = 0;
  else if (!visibleSince) visibleSince = Date.now();
  checkRead();
};
onMounted(() => {
  if (!bubble.value) return;
  observer = new IntersectionObserver(refreshVisibility, { root: bubble.value.closest('.message-feed'), threshold: [0, .5, 1] });
  observer.observe(bubble.value);
  timer = setInterval(refreshVisibility, 1000);
});
onBeforeUnmount(() => { observer?.disconnect(); clearInterval(timer); });

const timeFormatter = new Intl.DateTimeFormat('es-BO', {
  hour: 'numeric',
  minute: '2-digit',
  hour12: false
});
const dateTimeFormatter = new Intl.DateTimeFormat('es-BO', {
  dateStyle: 'medium',
  timeStyle: 'medium',
  hour12: false
});

const author = (username?: string) => username === props.currentUsername ? 'Tú' : username || 'Usuario';
const messageTime = (date?: string) => {
  if (!date) return 'ahora';
  const timestamp = new Date(date).getTime();
  return Number.isFinite(timestamp) ? timeFormatter.format(new Date(timestamp)) : 'ahora';
};
const messageDateTime = (date?: string) => {
  if (!date) return 'Fecha pendiente';
  const timestamp = new Date(date).getTime();
  return Number.isFinite(timestamp) ? dateTimeFormatter.format(new Date(timestamp)) : 'Fecha pendiente';
};
</script>

<template>
  <motion.article
    layout
    class="message"
    :class="{ 'message--mine': item.userId === currentUsername, 'message--continued': grouped }"
    :initial="reduceMotion ? false : { opacity: 0, y: 10, scale: 0.985 }"
    :animate="{ opacity: 1, y: 0, scale: 1 }"
    :exit="{ opacity: 0, scale: 0.985 }"
  >
    <span v-if="!grouped" class="avatar avatar--image" :data-avatar="avatarId" />
    <span v-else class="message__avatar-spacer" aria-hidden="true" />
    <div class="message__body">
      <div v-if="!grouped" class="message__meta"><strong>{{ author(item.userId) }}</strong></div>
      <div ref="bubble" class="message__bubble">
        <div v-if="item.replyToId" class="message__quote">
          <div class="message__quote-meta">
            <strong>{{ author(item.replyToUserId) }}</strong>
            <time :datetime="item.replyToCreatedAt" :title="messageDateTime(item.replyToCreatedAt)">{{ messageTime(item.replyToCreatedAt) }}</time>
          </div>
          <div class="message__quoted-text">{{ item.replyToBody || 'Mensaje no disponible' }}</div>
        </div>
        <div :id="`body-${item.id}`" class="message__text">{{ displayedBody }}</div>
        <button v-if="isLong" class="message__expand" type="button" :aria-expanded="expanded" :aria-controls="`body-${item.id}`" @click="expanded = !expanded">{{ expanded ? 'Ver menos' : 'Leer más' }}</button>
        <time class="message__time" :datetime="item.createdAt" :title="messageDateTime(item.createdAt)">{{ messageTime(item.createdAt) }}<span v-if="item.userId === currentUsername && item.id" class="message__delivery" :class="{ 'is-read': readers.length }" role="img" :aria-label="readLabel" :title="readLabel"><AppIcon :name="readers.length || deliveries.length ? 'checks' : 'check'" :size="16" /></span></time>
      </div>
      <div class="message__actions">
      <button
        v-if="item.id"
        class="message__reply-action"
        type="button"
        :aria-label="`Responder a ${author(item.userId)}`"
        @click="$emit('reply', item)"
      ><AppIcon name="reply" :size="14" /> Responder</button>
      <button v-if="item.id" class="message__details-action" type="button" @click="detailsOpen = true"><AppIcon name="info" :size="14" /> Detalles</button>
      </div>
    </div>
    <AppDialog v-if="detailsOpen" v-model="detailsOpen" title="Información del mensaje" :description="item.userId === currentUsername ? 'Enviado por ti' : `De @${item.userId}`">
      <div class="message-info">
        <details class="message-info__preview"><summary>Ver mensaje <span>{{ item.body?.length ?? 0 }} caracteres</span></summary><p>{{ item.body }}</p></details>
        <div class="message-info__sent"><AppIcon name="check" :size="18" /><div><strong>Enviado</strong><time :datetime="item.createdAt">{{ messageDateTime(item.createdAt) }}</time></div></div>
        <template v-if="item.userId === currentUsername">
          <div class="message-info__summary"><span><AppIcon name="checks" :size="18" /> Entregado a {{ deliveries.length }}</span><span class="is-read"><AppIcon name="checks" :size="18" /> Leído por {{ readers.length }}</span></div>
          <ul v-if="recipients.length" class="message-info__recipients">
            <li v-for="recipient in recipients" :key="recipient.name">
              <strong>@{{ recipient.name }}</strong>
              <dl><dt>Entregado</dt><dd><time :datetime="recipient.deliveredAt">{{ recipient.deliveredAt ? messageDateTime(recipient.deliveredAt) : 'Sin confirmación' }}</time></dd><dt>Leído</dt><dd :class="{ 'is-read': recipient.readAt }"><time :datetime="recipient.readAt">{{ recipient.readAt ? messageDateTime(recipient.readAt) : 'Pendiente' }}</time></dd></dl>
            </li>
          </ul>
          <p v-else class="message-info__empty">Aún no hay confirmaciones de entrega.</p>
          <p class="message-info__note">La entrega se confirma al recibir el mensaje. La lectura, al mostrarlo con el chat activo.{{ item.roomId ? ' En salas públicas se muestran las personas que lo confirmaron.' : '' }}</p>
        </template>
        <p v-else class="message-info__note">Las confirmaciones por persona se muestran al autor del mensaje.</p>
      </div>
    </AppDialog>
  </motion.article>
</template>

<style scoped>
.message__expand { display: block; min-height: 44px; padding: 6px 0; border: 0; background: transparent; color: var(--lime); font-size: 13px; font-weight: 600; }
.message-info { display: grid; gap: 20px; font-size: 13px; }
.message-info__preview { padding: 12px 14px; border-radius: 10px; background: var(--paper); }
.message-info__preview summary { cursor: pointer; line-height: 1.6; }
.message-info__preview summary span { margin-left: 8px; color: var(--muted); font-size: 12px; }
.message-info__preview p { max-height: 180px; overflow-y: auto; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.6; margin: 12px 0 0; }
.message-info__sent { display: flex; gap: 12px; align-items: flex-start; }
.message-info__sent time { display: block; margin-top: 5px; color: var(--muted); }
.message-info__summary { display: flex; flex-wrap: wrap; gap: 12px 24px; padding-top: 18px; border-top: 1px solid var(--line); color: var(--muted); }
.message-info__summary span { display: flex; align-items: center; gap: 6px; }
.message-info__recipients { list-style: none; padding: 0; margin: 0; }
.message-info__recipients li { padding: 16px 0; border-bottom: 1px solid var(--line); }
.message-info__recipients li:first-child { padding-top: 0; }
.message-info__recipients strong { overflow-wrap: anywhere; }
.message-info__recipients dl { display: grid; grid-template-columns: auto minmax(0, 1fr); gap: 8px 16px; margin: 12px 0 0; }
.message-info__recipients dt { color: var(--muted); }
.message-info__recipients dd { margin: 0; text-align: right; overflow-wrap: anywhere; font-size: 12px; }
.message-info__empty, .message-info__note { margin: 0; color: var(--muted); line-height: 1.6; }
.message-info__note { font-size: 12px; }
.is-read { color: var(--lime); }
</style>
