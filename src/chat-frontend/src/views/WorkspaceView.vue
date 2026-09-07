<script setup lang="ts">
import { AnimatePresence, motion, useReducedMotion } from 'motion-v';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppIcon from '../components/common/AppIcon.vue';
import AppDialog from '../components/common/AppDialog.vue';
import ChatMessageItem from '../components/common/ChatMessageItem.vue';
import RoomActivityBadge from '../components/common/RoomActivityBadge.vue';
import { useRealtimeStore } from '../stores/realtime';
import { useThemeStore } from '../stores/theme';
import { AVATAR_OPTIONS, type AvatarId, type ChatMessage, type Room, type User } from '../types/chat';

const store = useRealtimeStore();
const themeStore = useThemeStore();
const router = useRouter();
const search = ref('');
const drafts = ref<Record<string, string>>({});
const message = computed({
  get: () => drafts.value[store.activeConversationKey ?? ''] ?? '',
  set: (value: string) => { drafts.value[store.activeConversationKey ?? ''] = value; }
});
type SendAttempt = { body: string; clientMessageId: string; state: 'sending' | 'unconfirmed' | 'sent'; timer?: number };
const attempts = ref<Record<string, SendAttempt>>({});
watch(() => store.pendingMessages, pending => {
  for (const [key, entry] of Object.entries(pending)) {
    if (!attempts.value[key] && entry.message.clientMessageId) {
      attempts.value[key] = { body: entry.message.body ?? '', clientMessageId: entry.message.clientMessageId, state: 'unconfirmed' };
      if (!drafts.value[key]) drafts.value[key] = entry.message.body ?? '';
    }
  }
}, { deep: true, immediate: true });
const sendAttempt = computed(() => attempts.value[store.activeConversationKey ?? '']);
const opening = ref('');
const creating = ref(false);
const createFeedback = ref('');
const feedback = ref('');
let feedbackTimer: number | undefined;
const notify = (message: string) => {
  window.clearTimeout(feedbackTimer);
  feedback.value = message;
  feedbackTimer = window.setTimeout(() => { feedback.value = ''; }, 4000);
};
let createTimer: number | undefined;

const directoryMode = ref<'rooms' | 'people'>('rooms');
const showCreateRoom = ref(false);
const showMobileAside = ref(false);
const mobileQuery = window.matchMedia('(max-width: 980px)');
const isMobile = ref(mobileQuery.matches);
const viewportHeight = ref(window.visualViewport?.height ?? window.innerHeight);
const viewportTop = ref(window.visualViewport?.offsetTop ?? 0);
const drawerElement = ref<HTMLElement>();
const menuElement = ref<HTMLButtonElement>();
const mobileViewportStyle = computed(() => isMobile.value ? {
  '--chat-height': `${viewportHeight.value}px`,
  '--chat-top': `${viewportTop.value}px`
} : undefined);
const syncViewport = () => {
  isMobile.value = mobileQuery.matches;
  if (!isMobile.value) showMobileAside.value = false;
  // Let pinch zoom behave normally; keyboard and browser chrome resize at scale 1.
  if (window.visualViewport && window.visualViewport.scale !== 1) return;
  viewportHeight.value = window.visualViewport?.height ?? window.innerHeight;
  viewportTop.value = window.visualViewport?.offsetTop ?? 0;
};
watch(showMobileAside, async (visible) => {
  if (!isMobile.value) return;
  await nextTick();
  if (visible) drawerElement.value?.querySelector<HTMLButtonElement>('button')?.focus();
  else if (!showCreateRoom.value) menuElement.value?.focus();
});
const showEmojiPicker = ref(false);
const replyingTo = ref<ChatMessage>();
const roomName = ref('');
const roomDescription = ref('');
const messagesElement = ref<HTMLElement>();
const composerElement = ref<HTMLTextAreaElement>();
const preservingScroll = ref(false);
const stickToBottom = ref(true);
const reduceMotion = useReducedMotion();

const emojis = ['😀', '😂', '🥹', '😍', '😎', '🤔', '😭', '😤', '🥳', '🙌', '👏', '👋', '👍', '🔥', '✨', '💜', '💯', '🎉', '🚀', '👀', '✅', '❤️', '🤝', '☕'];

if (!store.currentUser && !store.idleExpired) void router.replace('/');

const filteredRooms = computed(() => {
  const query = search.value.trim().toLocaleLowerCase();
  if (!query) return store.rooms;
  return store.rooms.filter((room) => `${room.name} ${room.description}`.toLocaleLowerCase().includes(query));
});

const filteredUsers = computed(() => {
  const query = search.value.trim().toLocaleLowerCase();
  return store.users
    .filter((user) => user.username !== store.currentUser?.username)
    .filter((user) => !query || user.username.toLocaleLowerCase().includes(query));
});

const conversationTitle = computed(() => store.activeRoom?.name ?? `@${store.activeDirectUser?.username ?? ''}`);
const conversationSubtitle = computed(() => store.activeRoom?.description
  ?? (store.activeDirectUser?.online ? 'Mensaje directo · En línea' : 'Mensaje directo · Fuera de línea'));
const composerPlaceholder = computed(() => store.activeDirectUser
  ? `Mensaje para @${store.activeDirectUser.username}…`
  : 'Escribe un mensaje…');
const idleCountdown = computed(() => {
  const seconds = store.idleWarningSeconds ?? 0;
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;
});

const roomTone = (room: Room) => {
  const tones = ['violet', 'cyan', 'amber', 'rose'];
  return tones[room.name.length % tones.length];
};

const timeFormatter = new Intl.DateTimeFormat('es-BO', {
  hour: 'numeric',
  minute: '2-digit',
  hour12: true
});

const messageTime = (date?: string) => {
  if (!date) return 'ahora';
  const timestamp = new Date(date).getTime();
  return Number.isFinite(timestamp) ? timeFormatter.format(new Date(timestamp)) : 'ahora';
};

const messageAuthor = (username?: string) => username === store.currentUser?.username
  ? 'Tú'
  : username || 'Usuario';
const messageKey = (item: ChatMessage) => item.id
  ?? [item.roomId, item.recipientId, item.type, item.userId, item.createdAt, item.replyToId, item.body].join('|');

const avatarIdFor = (username?: string): AvatarId => {
  const user = store.users.find((item) => item.username === username);
  const currentUser = store.currentUser;
  if (user?.avatarId) return user.avatarId;
  if (currentUser && username === currentUser.username && currentUser.avatarId) return currentUser.avatarId;
  const index = [...(username || '?')].reduce((total, character) => total + character.charCodeAt(0), 0) % AVATAR_OPTIONS.length;
  return AVATAR_OPTIONS[index].id;
};

const isGroupedMessage = (index: number) => {
  const current = store.activeMessages[index];
  if (!current || current.type !== 'CHAT') return false;
  for (let previousIndex = index - 1; previousIndex >= 0; previousIndex -= 1) {
    const previous = store.activeMessages[previousIndex];
    if (previous?.type === 'CHAT') return previous.userId === current.userId;
  }
  return false;
};

const selectDirectoryMode = (mode: 'rooms' | 'people') => {
  directoryMode.value = mode;
  search.value = '';
  showMobileAside.value = true;
};

const openRoom = async (room: Room) => {
  if (opening.value || store.connection !== 'connected') return;
  opening.value = room.name;
  try {
    await store.enterRoom(room);
    notify(`Entraste a ${room.name}.`);
    showMobileAside.value = false;
  } catch {
    // The store exposes the loading error.
  } finally {
    opening.value = '';
  }
};

const openDirect = async (user: User) => {
  if (opening.value || store.connection !== 'connected') return;
  opening.value = `@${user.username}`;
  directoryMode.value = 'people';
  try {
    await store.enterDirect(user);
    notify(`Conversación con @${user.username} abierta.`);
    showMobileAside.value = false;
  } catch {
    // The store exposes the loading error.
  } finally {
    opening.value = '';
  }
};

const openIncomingDirect = async () => {
  const sender = store.incomingDirectMessage?.userId;
  if (!sender) return;
  const user = store.users.find((item) => item.username === sender)
    ?? { username: sender, online: false };
  await openDirect(user);
};

const retryConnection = () => {
  void store.retryConnection().catch(() => {
    // The connection badge keeps the recovery state visible without a duplicate toast.
  });
};

const submitMessage = () => {
  const key = store.activeConversationKey;
  if (!key || !message.value.trim() || opening.value || sendAttempt.value?.state === 'sending') return;
  const body = message.value.trim();
  const clientMessageId = store.sendMessage(body, replyingTo.value?.id);
  if (!clientMessageId) return;
  window.clearTimeout(attempts.value[key]?.timer);
  attempts.value[key] = { body, clientMessageId, state: 'sending' };
  attempts.value[key].timer = window.setTimeout(() => {
    const attempt = attempts.value[key];
    if (attempt?.state === 'sending') attempt.state = 'unconfirmed';
  }, 8000);
  showEmojiPicker.value = false;
  stickToBottom.value = true;
};

const retryPendingMessage = () => {
  const key = store.activeConversationKey;
  if (!key || !store.retryPendingMessage(key)) return;
  const attempt = attempts.value[key];
  if (!attempt) return;
  window.clearTimeout(attempt.timer);
  attempt.state = 'sending';
  attempt.timer = window.setTimeout(() => { if (attempt.state === 'sending') attempt.state = 'unconfirmed'; }, 8000);
};

watch(() => store.activeMessages, (messages) => {
  const key = store.activeConversationKey;
  const attempt = key ? attempts.value[key] : undefined;
  if (!key || !attempt || attempt.state === 'sent') return;
  const confirmed = messages.some((item) => item.type === 'CHAT'
    && item.userId === store.currentUser?.username && item.clientMessageId === attempt.clientMessageId);
  if (!confirmed) return;
  window.clearTimeout(attempt.timer);
  attempt.state = 'sent';
  if (drafts.value[key]?.trim() === attempt.body) drafts.value[key] = '';
  replyingTo.value = undefined;
}, { deep: true });

const jumpToLatest = async () => {
  stickToBottom.value = true;
  await nextTick();
  messagesElement.value?.scrollTo({ top: messagesElement.value.scrollHeight, behavior: reduceMotion.value ? 'auto' : 'smooth' });
};

const startReply = async (item: ChatMessage) => {
  if (!item.id || item.type !== 'CHAT') return;
  replyingTo.value = item;
  showEmojiPicker.value = false;
  await nextTick();
  composerElement.value?.focus();
};

const cancelReply = () => {
  replyingTo.value = undefined;
};

const insertEmoji = async (emoji: string) => {
  const textarea = composerElement.value;
  const start = textarea?.selectionStart ?? message.value.length;
  const end = textarea?.selectionEnd ?? start;
  message.value = `${message.value.slice(0, start)}${emoji}${message.value.slice(end)}`;
  await nextTick();
  composerElement.value?.focus();
  composerElement.value?.setSelectionRange(start + emoji.length, start + emoji.length);
};

const createRoom = () => {
  const name = roomName.value.trim();
  const description = roomDescription.value.trim();
  if (creating.value || name.length < 3 || description.length < 8) return;
  if (!store.createRoom(name, description)) return;
  creating.value = true;
  createFeedback.value = 'Creando tu sala…';
  createTimer = window.setTimeout(() => {
    creating.value = false;
    createFeedback.value = 'Aún no recibimos confirmación. Revisa la lista de salas antes de volver a intentarlo.';
  }, 8000);
};


watch(() => store.rooms, (rooms, previous) => {
  if (!creating.value) return;
  const created = rooms.find((room) => room.name === roomName.value.trim() && !previous.some((item) => item.id === room.id));
  if (!created) return;
  window.clearTimeout(createTimer);
  creating.value = false;
  createFeedback.value = '';
  roomName.value = '';
  roomDescription.value = '';
  showCreateRoom.value = false;
  showMobileAside.value = false;
  notify(`Sala ${created.name} creada.`);
});

const onMessageScroll = async (force = false) => {
  const element = messagesElement.value;
  if (!element) return;
  stickToBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 100;
  if ((!force && element.scrollTop > 72) || !store.canLoadMore || store.isLoadingHistory) return;
  preservingScroll.value = true;
  const previousHeight = element.scrollHeight;
  const loaded = await store.loadOlderMessages();
  await nextTick();
  if (loaded > 0) element.scrollTop += element.scrollHeight - previousHeight;
  preservingScroll.value = false;
};

const exit = async () => {
  await store.disconnect();
  await router.replace({ path: '/', query: { salida: '1' } });
};

const returnAfterIdle = async () => {
  store.acknowledgeIdleExpiration();
  await router.replace('/');
};

const onKeydown = (event: KeyboardEvent) => {
  if (document.querySelector('dialog[open]')) return;
  if (isMobile.value && showMobileAside.value && !showCreateRoom.value && event.key === 'Tab') {
    const controls = [...(drawerElement.value?.querySelectorAll<HTMLElement>('button:not(:disabled), input') ?? [])];
    const first = controls[0];
    const last = controls[controls.length - 1];
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
  }
  if (event.key !== 'Escape') return;
  showMobileAside.value = false;
  showCreateRoom.value = false;
  showEmojiPicker.value = false;
  cancelReply();
};

const onDocumentPointerDown = (event: PointerEvent) => {
  const target = event.target;
  if (target instanceof Element && !target.closest('.emoji-shell')) showEmojiPicker.value = false;
};

watch(() => store.activeConversationKey, async () => {
  showEmojiPicker.value = false;
  cancelReply();
  stickToBottom.value = true;
  await nextTick();
  const element = messagesElement.value;
  if (element) element.scrollTop = element.scrollHeight;
});

watch(() => store.activeMessages.length, async (length, previousLength) => {
  if (preservingScroll.value || length <= previousLength || !stickToBottom.value) return;
  await nextTick();
  messagesElement.value?.scrollTo({
    top: messagesElement.value.scrollHeight,
    behavior: reduceMotion.value ? 'auto' : 'smooth'
  });
});

const resizeComposer = async () => {
  await nextTick();
  const element = composerElement.value;
  if (!element) return;
  element.style.height = 'auto';
  element.style.height = `${Math.min(element.scrollHeight, Math.min(120, viewportHeight.value * 0.25))}px`;
};
watch([message, () => store.activeConversationKey], resizeComposer, { flush: 'post' });
watch(viewportHeight, async () => {
  await resizeComposer();
  if (stickToBottom.value) await jumpToLatest();
});
onMounted(resizeComposer);
window.addEventListener('resize', syncViewport);
window.visualViewport?.addEventListener('resize', syncViewport);
window.visualViewport?.addEventListener('scroll', syncViewport);
window.addEventListener('keydown', onKeydown);
document.addEventListener('pointerdown', onDocumentPointerDown);
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer);
  Object.values(attempts.value).forEach((attempt) => window.clearTimeout(attempt.timer));
  window.clearTimeout(createTimer);
  window.removeEventListener('resize', syncViewport);
  window.visualViewport?.removeEventListener('resize', syncViewport);
  window.visualViewport?.removeEventListener('scroll', syncViewport);
  window.removeEventListener('keydown', onKeydown);
  document.removeEventListener('pointerdown', onDocumentPointerDown);
});
</script>

<template>
  <main class="workspace selection:bg-relay-lime selection:text-relay-ink" :style="mobileViewportStyle" :class="{ 'workspace--short': isMobile && viewportHeight < 500 }">
    <AnimatePresence>
      <motion.div
        v-if="store.operationError"
        key="operation-error"
        role="alert"
        class="fixed top-4 right-4 z-[120] flex max-w-[min(420px,calc(100vw-2rem))] items-center gap-3 rounded-xl bg-relay-ink px-4 py-3 text-sm text-white shadow-2xl"
        :initial="reduceMotion ? false : { opacity: 0, y: -12, filter: 'blur(8px)' }"
        :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
        :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, y: -8, filter: 'blur(6px)' }"
      >
        <span class="h-2 w-2 shrink-0 rounded-full bg-[#ff7191]" />
        <span class="leading-5">{{ store.operationError }}</span>
        <button class="ml-auto grid h-8 w-8 shrink-0 place-items-center rounded-full text-white/60 transition hover:bg-white/10 hover:text-white" type="button" aria-label="Cerrar aviso" @click="store.clearOperationError">
          <AppIcon name="close" :size="16" />
        </button>
      </motion.div>

      <motion.div
        v-if="store.idleWarningSeconds !== undefined"
        key="idle-warning"
        class="idle-warning"
        role="alert"
        :initial="reduceMotion ? false : { opacity: 0, y: 16 }"
        :animate="{ opacity: 1, y: 0 }"
        :exit="{ opacity: 0, y: 10 }"
      >
        <AppIcon name="clock" :size="18" />
        <span>Te desconectaremos por inactividad en <strong>{{ idleCountdown }}</strong>.</span>
        <button type="button" @click="store.resetIdleActivity">Seguir conectado</button>
      </motion.div>

      <motion.aside
        v-if="store.incomingDirectMessage"
        key="incoming-message"
        class="direct-notification"
        :class="{ 'direct-notification--with-error': store.operationError }"
        role="status"
        aria-live="polite"
        :initial="reduceMotion ? false : { opacity: 0, x: 20, scale: 0.97 }"
        :animate="{ opacity: 1, x: 0, scale: 1 }"
        :exit="{ opacity: 0, x: 12, scale: 0.98 }"
      >
        <button class="direct-notification__open" type="button" @click="openIncomingDirect">
          <span class="avatar avatar--image" :data-avatar="avatarIdFor(store.incomingDirectMessage.userId)" />
          <span class="direct-notification__copy">
            <small>Nuevo mensaje</small>
            <strong>@{{ store.incomingDirectMessage.userId }}</strong>
            <span>{{ store.incomingDirectMessage.body }}</span>
          </span>
          <span class="direct-notification__action">Ver mensaje <AppIcon name="arrow" :size="16" /></span>
        </button>
        <button class="direct-notification__close" type="button" aria-label="Cerrar notificación" @click="store.dismissDirectNotification"><AppIcon name="close" :size="15" /></button>
      </motion.aside>
    </AnimatePresence>

    <aside class="workspace__rail" aria-label="Navegación">
      <a class="brand brand--compact" href="/" aria-label="Chatty inicio"><span class="brand__mark"><span class="brand__logo" aria-hidden="true" /></span></a>
      <div class="rail-actions">
        <button class="icon-button icon-button--badged" :class="{ 'is-active': directoryMode === 'rooms' }" aria-label="Salas" @click="selectDirectoryMode('rooms')">
          <AppIcon name="hash" />
          <span v-if="store.totalUnreadRooms" class="icon-button__badge">{{ store.totalUnreadRooms > 9 ? '9+' : store.totalUnreadRooms }}</span>
        </button>
        <button class="icon-button icon-button--badged" :class="{ 'is-active': directoryMode === 'people' }" aria-label="Personas" @click="selectDirectoryMode('people')">
          <AppIcon name="users" />
          <span v-if="store.totalUnreadDirect" class="icon-button__badge">{{ store.totalUnreadDirect > 9 ? '9+' : store.totalUnreadDirect }}</span>
        </button>
      </div>
      <span class="avatar avatar--me avatar--image" role="img" :data-avatar="avatarIdFor(store.currentUser?.username)" :aria-label="store.currentUser ? `Perfil de ${store.currentUser.username}` : 'Sesión desconectada'">
        <span class="presence-dot" />
      </span>
    </aside>

    <button v-if="isMobile && showMobileAside" class="drawer-backdrop" type="button" tabindex="-1" aria-label="Cerrar directorio" @click="showMobileAside = false" />
    <aside id="mobile-directory" ref="drawerElement" class="workspace__aside" :role="isMobile ? 'dialog' : undefined" :aria-modal="isMobile && showMobileAside ? true : undefined" aria-label="Salas y personas" :inert="showCreateRoom || (isMobile && !showMobileAside)" :class="{ 'is-mobile-open': showMobileAside }">
      <div class="aside-head">
        <div>

          <h2>{{ directoryMode === 'rooms' ? 'Salas' : 'Personas' }}</h2>
        </div>
        <button class="icon-button icon-button--light mobile-only" aria-label="Cerrar panel" @click="showMobileAside = false"><AppIcon name="close" /></button>
      </div>

      <nav class="mobile-directory-tabs" aria-label="Tipo de conversación">
        <button type="button" :aria-pressed="directoryMode === 'rooms'" @click="selectDirectoryMode('rooms')">Salas <span v-if="store.totalUnreadRooms">{{ store.totalUnreadRooms }}</span></button>
        <button type="button" :aria-pressed="directoryMode === 'people'" @click="selectDirectoryMode('people')">Personas <span v-if="store.totalUnreadDirect">{{ store.totalUnreadDirect }}</span></button>
      </nav>
      <label class="search-field">
        <AppIcon name="search" :size="18" />
        <input v-model="search" :placeholder="directoryMode === 'rooms' ? 'Buscar una sala' : 'Buscar una persona'" :aria-label="directoryMode === 'rooms' ? 'Buscar una sala' : 'Buscar una persona'">
      </label>

      <template v-if="directoryMode === 'rooms'">
        <button class="create-room" type="button" aria-label="Crear nueva sala" @click="showCreateRoom = true"><span><AppIcon name="plus" :size="18" /></span>Crear nueva sala</button>
        <div class="aside-label"><span>Salas públicas</span><span>{{ filteredRooms.length }}</span></div>
        <div class="room-list">
          <motion.button
            v-for="room in filteredRooms"
            :key="room.id"
            layout
            class="room-row"
            :class="{ 'is-active': room.id === store.activeRoom?.id }"
            :aria-current="room.id === store.activeRoom?.id ? 'true' : undefined"
            type="button"
            :disabled="Boolean(opening) || store.connection !== 'connected'"
            @click="openRoom(room)"
          >
            <span class="room-glyph" :data-tone="roomTone(room)">#</span>
            <span class="room-row__copy">
              <strong>{{ room.name }}</strong>
              <small>{{ store.roomActivityById[room.id]?.replies ? 'Te respondieron en esta sala' : `${room.activeUsers || 0} en línea` }}</small>
            </span>
            <RoomActivityBadge :activity="store.roomActivityById[room.id]" compact />
            <span v-if="opening === room.name" class="loading-spinner" aria-label="Entrando" />
          </motion.button>
          <p v-if="filteredRooms.length === 0" class="empty-copy">No hay salas que coincidan.</p>
        </div>
        <button class="people-block" type="button" @click="selectDirectoryMode('people')">
          <span class="aside-label"><span>En línea</span><span>{{ store.onlineCount }}</span></span>
          <span class="avatar-stack" aria-label="Personas conectadas">
            <span v-for="user in filteredUsers.slice(0, 5)" :key="user.username" class="avatar avatar--image" :data-avatar="avatarIdFor(user.username)" :title="user.username" />
            <span v-if="filteredUsers.length > 5" class="avatar avatar--more">+{{ filteredUsers.length - 5 }}</span>
          </span>
        </button>
      </template>

      <template v-else>
        <div class="aside-label"><span>Personas disponibles</span><span>{{ filteredUsers.length }}</span></div>
        <div class="people-list">
          <button
            v-for="user in filteredUsers"
            :key="user.username"
            class="person-row"
            :class="{ 'is-active': user.username === store.activeDirectUser?.username }"
            type="button"
            :disabled="Boolean(opening) || store.connection !== 'connected'"
            @click="openDirect(user)"
          >
            <span class="avatar avatar--image" :data-avatar="avatarIdFor(user.username)"><span class="presence-dot" /></span>
            <span class="person-row__copy">
              <strong>@{{ user.username }}</strong>
              <small>{{ store.unreadDirectByUser[user.username] ? `${store.unreadDirectByUser[user.username]} mensaje${store.unreadDirectByUser[user.username] === 1 ? '' : 's'} nuevo${store.unreadDirectByUser[user.username] === 1 ? '' : 's'}` : 'En línea ahora' }}</small>
            </span>
            <span class="person-row__action"><AppIcon name="message" :size="14" /> Mensaje <b v-if="store.unreadDirectByUser[user.username]">{{ store.unreadDirectByUser[user.username] }}</b></span>
          </button>
          <p v-if="filteredUsers.length === 0" class="empty-copy">No hay otras personas conectadas.</p>
        </div>
      </template>
    </aside>

    <section class="workspace__main" :inert="(isMobile && showMobileAside) || showCreateRoom">
      <header class="workspace__header">
        <button class="icon-button icon-button--light mobile-only" ref="menuElement" aria-label="Abrir directorio" aria-controls="mobile-directory" :aria-expanded="showMobileAside" @click="showMobileAside = true"><AppIcon name="menu" /></button>
        <div class="connection-badge" role="status" :data-state="store.connection"><span class="live-dot" />{{ store.connection === 'connected' ? 'En línea' : store.connectionMessage }}</div>
        <div class="header-user">
          <div class="header-profile" :aria-label="store.currentUser ? `Tu sesión: @${store.currentUser.username}` : 'Sesión cerrada'" :title="store.currentUser?.username"><span class="avatar avatar--image" :data-avatar="avatarIdFor(store.currentUser?.username)" aria-hidden="true" /><span>{{ store.currentUser ? `@${store.currentUser.username}` : 'Sesión cerrada' }}</span></div>
          <button class="theme-toggle theme-toggle--compact" type="button" :aria-label="themeStore.isDark ? 'Activar modo día' : 'Activar modo noche'" :aria-pressed="themeStore.isDark" @click="themeStore.toggle"><AppIcon :name="themeStore.isDark ? 'sun' : 'moon'" :size="16" /></button>
          <button class="icon-button icon-button--light" aria-label="Salir" @click="exit"><AppIcon name="logout" /></button>
        </div>
      </header>

      <Transition name="snackbar">
        <div v-if="feedback" class="workspace-snackbar" role="status" aria-live="polite" aria-atomic="true">
          <AppIcon name="message" :size="18" />
          <span>{{ feedback }}</span>
          <button type="button" aria-label="Cerrar confirmación" @click="feedback = ''"><AppIcon name="close" :size="16" /></button>
        </div>
      </Transition>
      <div v-if="store.connection !== 'connected'" class="workspace-recovery" role="status">
        <span>Se perdió la conexión. Tu borrador sigue aquí.</span>
        <button type="button" @click="retryConnection">Reconectar</button>
      </div>
      <AnimatePresence mode="wait" :initial="false">
        <motion.div
          v-if="!store.activeConversationKey"
          :key="`directory-${directoryMode}`"
          class="directory-view"
          :initial="reduceMotion ? false : { opacity: 0, x: -14, filter: 'blur(8px)' }"
          :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
          :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, x: -10, filter: 'blur(6px)' }"
        >
          <div class="directory-intro">
            <span class="section-kicker">{{ directoryMode === 'rooms' ? 'SALAS PÚBLICAS' : 'MENSAJES DIRECTOS' }}</span>
            <h1>{{ directoryMode === 'rooms' ? '¿De qué hablamos hoy?' : '¿Con quién quieres hablar?' }}</h1>
            <p>{{ directoryMode === 'rooms' ? 'Entra a una sala pública o crea un espacio nuevo para empezar la conversación.' : 'Inicia una conversación directa con cualquiera que esté conectado ahora.' }}</p>
          </div>

          <div v-if="directoryMode === 'rooms'" class="room-grid">
            <motion.button
              v-for="(room, index) in filteredRooms"
              :key="room.id"
              layout
              class="room-card"
              :data-tone="roomTone(room)"
              type="button"
              :initial="reduceMotion ? false : { opacity: 0, y: 14 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ duration: reduceMotion ? 0 : 0.36, delay: reduceMotion ? 0 : index * 0.055 }"
              :while-hover="reduceMotion ? undefined : { x: 5 }"
              :disabled="Boolean(opening) || store.connection !== 'connected'"
            @click="openRoom(room)"
            >

              <span class="room-card__icon">#</span>
              <span class="room-card__copy"><strong>{{ room.name }}</strong><span>{{ room.description || 'Una conversación abierta para la comunidad.' }}</span></span>
              <span class="room-card__meta"><RoomActivityBadge :activity="store.roomActivityById[room.id]" /><span>{{ room.activeUsers || 0 }} en línea</span><AppIcon name="arrow" /></span>
            </motion.button>
          </div>

          <div v-else class="people-directory">
            <motion.button
              v-for="(user, index) in filteredUsers"
              :key="user.username"
              class="person-card"
              type="button"
              :initial="reduceMotion ? false : { opacity: 0, y: 12 }"
              :animate="{ opacity: 1, y: 0 }"
              :transition="{ delay: reduceMotion ? 0 : index * 0.045 }"
              :disabled="Boolean(opening) || store.connection !== 'connected'"
            @click="openDirect(user)"
            >
              <span class="avatar avatar--image" :data-avatar="avatarIdFor(user.username)"><span class="presence-dot" /></span>
              <span><strong>@{{ user.username }}</strong><small>Disponible para conversar</small></span>
              <span class="person-card__cta">Enviar mensaje <b v-if="store.unreadDirectByUser[user.username]">{{ store.unreadDirectByUser[user.username] }}</b><AppIcon name="arrow" :size="18" /></span>
            </motion.button>
            <p v-if="filteredUsers.length === 0" class="empty-copy">Aún no hay otras personas conectadas.</p>
          </div>
        </motion.div>

        <motion.div
          v-else
          :key="store.activeConversationKey"
          class="conversation-view"
          :initial="reduceMotion ? false : { opacity: 0, x: 18, filter: 'blur(8px)' }"
          :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
          :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, x: 12, filter: 'blur(6px)' }"
        >
          <header class="conversation-head">
            <button class="icon-button icon-button--light" aria-label="Volver al directorio" @click="store.leaveConversation"><AppIcon name="back" /></button>
            <span v-if="store.activeRoom" class="room-glyph room-glyph--large" :data-tone="roomTone(store.activeRoom)">#</span>
            <span v-else class="avatar avatar--image dm-avatar" :data-avatar="avatarIdFor(store.activeDirectUser?.username)"><span class="presence-dot" /></span>
            <div><h1>{{ conversationTitle }}</h1><p>{{ conversationSubtitle }}</p></div>
            <span v-if="store.activeRoom" class="conversation-count"><span class="live-dot" /> {{ store.activeRoom.activeUsers || 0 }} aquí</span>
            <span v-else class="conversation-count"><span class="live-dot" /> Privado</span>
          </header>

          <div ref="messagesElement" class="message-feed" aria-live="polite" @scroll.passive="onMessageScroll()">
            <div v-if="store.connection !== 'connected'" class="connection-recovery" role="status" aria-live="polite">
              <span class="connection-recovery__signal"><span /></span>
              <div>
                <strong>La conexión se interrumpió</strong>
                <p>Conservamos esta conversación y la recuperaremos cuando el servidor vuelva.</p>
              </div>
              <button type="button" @click="retryConnection">Reintentar ahora</button>
            </div>
            <div class="history-loader" role="status">
              <span v-if="store.isLoadingHistory"><span class="loading-spinner" /> Cargando mensajes…</span>
              <button v-else-if="store.canLoadMore" type="button" :disabled="store.isLoadingHistory" @click="onMessageScroll(true)">
                {{ store.isLoadingHistory ? 'Cargando…' : 'Cargar mensajes anteriores' }}
              </button>
              <span v-else-if="store.activeMessages.length">Inicio de la conversación</span>
            </div>
            <div class="date-divider"><span>Mensajes recientes</span></div>
            <AnimatePresence :initial="false">
              <template v-for="(item, index) in store.activeMessages" :key="messageKey(item)">
                <motion.div
                  v-if="item.type !== 'CHAT'"
                  :key="messageKey(item)"
                  layout
                  class="system-message"
                  :initial="reduceMotion ? false : { opacity: 0, scale: 0.96 }"
                  :animate="{ opacity: 1, scale: 1 }"
                  :exit="{ opacity: 0 }"
                ><span>{{ item.userId }}</span> {{ item.type === 'JOIN' ? 'se unió a la sala' : 'salió de la sala' }}</motion.div>
                <ChatMessageItem
                  v-else
                  :key="messageKey(item)"
                  :item="item"
                  :current-username="store.currentUser?.username"
                  :avatar-id="avatarIdFor(item.userId)"
                  :grouped="isGroupedMessage(index)"
                  :reduce-motion="Boolean(reduceMotion)"
                  @reply="startReply"
                  @read="store.markMessageRead"
                />
              </template>
            </AnimatePresence>
            <div v-if="store.activeMessages.length === 0 && !store.isLoadingHistory" class="conversation-empty">
              <AppIcon :name="store.activeDirectUser ? 'message' : 'sparkles'" :size="28" />
              <strong>Abre la conversación.</strong>
              <span>{{ store.activeDirectUser ? `Envía el primer mensaje a @${store.activeDirectUser.username}.` : 'Sé la primera persona en escribir en esta sala.' }}</span>
            </div>
          </div>

          <button v-if="!stickToBottom" class="jump-latest" type="button" @click="jumpToLatest">↓ Ir a los últimos mensajes</button>
          <div v-if="sendAttempt?.state === 'sending' || sendAttempt?.state === 'unconfirmed'" class="send-feedback" :class="{ 'send-feedback--warning': sendAttempt?.state === 'unconfirmed' }" role="status" aria-live="polite">
            <template v-if="sendAttempt?.state === 'sending'"><span class="loading-spinner" /> Enviando mensaje…</template>
            <template v-else-if="sendAttempt?.state === 'unconfirmed'">Sin confirmación del servidor. Tu mensaje sigue pendiente. <button type="button" :disabled="store.connection !== 'connected'" @click="retryPendingMessage">Reintentar</button></template>
          </div>
          <form class="composer" @submit.prevent="submitMessage">
            <div v-if="replyingTo" class="composer__reply" aria-live="polite">
              <span class="composer__reply-accent" />
              <div>
                <span>Respondiendo a <strong>{{ messageAuthor(replyingTo.userId) }}</strong> · {{ messageTime(replyingTo.createdAt) }}</span>
                <p>{{ replyingTo.body }}</p>
              </div>
              <button type="button" aria-label="Cancelar respuesta" title="Cancelar respuesta" @click="cancelReply"><AppIcon name="close" :size="16" /></button>
            </div>
            <textarea ref="composerElement" v-model="message" rows="1" maxlength="10000" :placeholder="store.connection === 'connected' ? composerPlaceholder : 'Esperando al servidor…'" :disabled="store.connection !== 'connected' || Boolean(opening)" aria-label="Mensaje" enterkeyhint="enter" @keydown.enter.exact="!isMobile && !$event.isComposing && ($event.preventDefault(), submitMessage())" />
            <span class="composer__hint">Shift + Enter para una línea nueva</span>
            <div class="emoji-shell">
              <button class="emoji-button" type="button" aria-label="Elegir emoji" :aria-expanded="showEmojiPicker" @click.stop="showEmojiPicker = !showEmojiPicker"><AppIcon name="smile" /></button>
              <AnimatePresence>
                <motion.div
                  v-if="showEmojiPicker"
                  class="emoji-picker"
                  role="dialog"
                  aria-label="Selector de emojis"
                  :initial="reduceMotion ? false : { opacity: 0, y: 8, scale: 0.97 }"
                  :animate="{ opacity: 1, y: 0, scale: 1 }"
                  :exit="{ opacity: 0, y: 5, scale: 0.98 }"
                >
                  <span>Emojis</span>
                  <div><button v-for="emoji in emojis" :key="emoji" type="button" :aria-label="`Insertar ${emoji}`" @click="insertEmoji(emoji)">{{ emoji }}</button></div>
                </motion.div>
              </AnimatePresence>
            </div>
            <button class="send-button" type="submit" :disabled="!message.trim() || store.connection !== 'connected' || Boolean(opening) || sendAttempt?.state === 'sending' || sendAttempt?.state === 'unconfirmed'" aria-label="Enviar mensaje"><span v-if="sendAttempt?.state === 'sending'" class="loading-spinner" /><AppIcon v-else name="send" /><span>Enviar</span></button>
          </form>
        </motion.div>
      </AnimatePresence>
    </section>

    <AppDialog v-model="showCreateRoom" title="Crear una sala" description="Elige un nombre y cuenta de qué se conversa aquí.">
      <form class="dialog-form" :aria-busy="creating" @submit.prevent="createRoom">
        <label>Nombre <input :disabled="creating" v-model="roomName" minlength="3" maxlength="30" required placeholder="Ej. Diseño de producto"></label>
        <label>Descripción <textarea :disabled="creating" v-model="roomDescription" minlength="8" maxlength="255" required rows="3" placeholder="¿De qué se conversa aquí?" /></label>
        <p v-if="createFeedback" role="status" class="input-hint">{{ createFeedback }}</p>
        <div class="dialog-form__actions"><button class="button button--ghost-dark" type="button" @click="showCreateRoom = false">Cancelar</button><button class="button button--primary" type="submit" :disabled="creating || roomName.trim().length < 3 || roomDescription.trim().length < 8">{{ creating ? 'Creando…' : 'Crear sala' }}</button></div>
      </form>
    </AppDialog>

    <AnimatePresence>
      <motion.div v-if="store.idleExpired" key="idle-expired" class="modal-backdrop" role="alertdialog" aria-modal="true" aria-labelledby="idle-expired-title" :initial="{ opacity: 0 }" :animate="{ opacity: 1 }" :exit="{ opacity: 0 }">
        <div class="modal idle-expired-modal">
          <span class="idle-expired-modal__icon"><AppIcon name="clock" :size="24" /></span>
          <div><span class="section-kicker">SESIÓN FINALIZADA</span><h2 id="idle-expired-title">{{ store.sessionEndReason }}</h2><p>Vuelve al inicio para abrir una sesión y continuar.</p></div>
          <button class="button button--primary" type="button" @click="returnAfterIdle">Volver al inicio <AppIcon name="arrow" /></button>
        </div>
      </motion.div>
    </AnimatePresence>
  </main>
</template>
