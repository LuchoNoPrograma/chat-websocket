<script setup lang="ts">
import { formatDistanceToNow } from 'date-fns';
import { es } from 'date-fns/locale';
import { AnimatePresence, motion, useReducedMotion } from 'motion-v';
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import AppIcon from '../components/AppIcon.vue';
import { useRealtimeStore } from '../stores/realtime';
import type { ChatMessage, Room } from '../types';

const store = useRealtimeStore();
const router = useRouter();
const search = ref('');
const message = ref('');
const showCreateRoom = ref(false);
const showMobileAside = ref(false);
const roomName = ref('');
const roomDescription = ref('');
const messagesElement = ref<HTMLElement>();
const reduceMotion = useReducedMotion();

if (!store.currentUser) void router.replace('/');

const filteredRooms = computed(() => {
  const query = search.value.trim().toLocaleLowerCase();
  if (!query) return store.rooms;
  return store.rooms.filter((room) => `${room.name} ${room.description}`.toLocaleLowerCase().includes(query));
});

const initials = (value?: string) => (value || '?')
  .split(/[._\s-]/)
  .filter(Boolean)
  .slice(0, 2)
  .map((part) => part[0]?.toUpperCase())
  .join('');

const roomTone = (room: Room) => {
  const tones = ['violet', 'cyan', 'amber', 'rose'];
  return tones[room.name.length % tones.length];
};

const relativeTime = (date?: string) => date
  ? formatDistanceToNow(new Date(date), { addSuffix: true, locale: es })
  : 'ahora';

const isMine = (item: ChatMessage) => item.userId === store.currentUser?.username;
const messageKey = (item: ChatMessage) => item.id
  ?? [item.roomId, item.type, item.userId, item.createdAt, item.body].join('|');

const openRoom = async (room: Room) => {
  try {
    await store.enterRoom(room);
    showMobileAside.value = false;
  } catch {
    // The store exposes a user-facing error and restores the directory state.
  }
};

const submitMessage = () => {
  if (store.sendMessage(message.value)) message.value = '';
};

const createRoom = () => {
  const name = roomName.value.trim();
  const description = roomDescription.value.trim();
  if (name.length < 3 || description.length < 8) return;
  if (!store.createRoom(name, description)) return;
  roomName.value = '';
  roomDescription.value = '';
  showCreateRoom.value = false;
};

const exit = async () => {
  await store.disconnect();
  await router.replace('/');
};

const onKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') showCreateRoom.value = false;
};

watch(() => store.activeMessages.length, async () => {
  await nextTick();
  messagesElement.value?.scrollTo({
    top: messagesElement.value.scrollHeight,
    behavior: reduceMotion.value ? 'auto' : 'smooth'
  });
});

window.addEventListener('keydown', onKeydown);
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown));
</script>

<template>
  <main class="workspace selection:bg-relay-lime selection:text-relay-ink">
    <AnimatePresence>
      <motion.div
        v-if="store.operationError"
        key="operation-error"
        role="alert"
        class="fixed top-4 right-4 z-[120] flex max-w-[min(420px,calc(100vw-2rem))] items-center gap-3 rounded-xl bg-relay-ink px-4 py-3 text-sm text-white shadow-2xl"
        :initial="reduceMotion ? false : { opacity: 0, y: -12, filter: 'blur(8px)' }"
        :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
        :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, y: -8, filter: 'blur(6px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.24 }"
      >
        <span class="h-2 w-2 shrink-0 rounded-full bg-[#ff7191]" />
        <span class="leading-5">{{ store.operationError }}</span>
        <button
          class="ml-auto grid h-8 w-8 shrink-0 place-items-center rounded-full text-white/60 transition hover:bg-white/10 hover:text-white"
          type="button"
          aria-label="Cerrar aviso"
          @click="store.clearOperationError"
        >
          <AppIcon name="close" :size="16" />
        </button>
      </motion.div>
    </AnimatePresence>
    <aside class="workspace__rail" aria-label="Navegación">
      <a class="brand brand--compact" href="/" aria-label="Relay inicio"><span class="brand__mark">R/</span></a>
      <div class="rail-actions">
        <button class="icon-button is-active" aria-label="Salas"><AppIcon name="hash" /></button>
        <button class="icon-button" aria-label="Personas"><AppIcon name="users" /></button>
      </div>
      <button class="avatar avatar--me" :aria-label="`Perfil de ${store.currentUser?.username}`">
        {{ initials(store.currentUser?.username) }}
        <span class="presence-dot" />
      </button>
    </aside>

    <aside class="workspace__aside" :class="{ 'is-mobile-open': showMobileAside }">
      <div class="aside-head">
        <div>
          <span class="section-kicker">ESPACIOS</span>
          <h2>Directorio</h2>
        </div>
        <button class="icon-button icon-button--light mobile-only" aria-label="Cerrar panel" @click="showMobileAside = false">
          <AppIcon name="close" />
        </button>
      </div>

      <label class="search-field">
        <AppIcon name="search" :size="18" />
        <input v-model="search" placeholder="Buscar una sala" aria-label="Buscar una sala">
      </label>

      <button class="create-room" type="button" @click="showCreateRoom = true">
        <span><AppIcon name="plus" :size="18" /></span>
        Crear nueva sala
      </button>

      <div class="aside-label">
        <span>Salas públicas</span>
        <span>{{ filteredRooms.length }}</span>
      </div>
      <div class="room-list">
        <motion.button
          v-for="room in filteredRooms"
          :key="room.id"
          layout
          class="room-row"
          :class="{ 'is-active': room.id === store.activeRoom?.id }"
          type="button"
          @click="openRoom(room)"
        >
          <span class="room-glyph" :data-tone="roomTone(room)">#</span>
          <span class="room-row__copy">
            <strong>{{ room.name }}</strong>
            <small>{{ room.activeUsers || 0 }} conectados</small>
          </span>
          <span class="room-row__arrow">→</span>
        </motion.button>
        <p v-if="filteredRooms.length === 0" class="empty-copy">No hay salas que coincidan.</p>
      </div>

      <div class="people-block">
        <div class="aside-label">
          <span>En línea</span>
          <span>{{ store.onlineCount }}</span>
        </div>
        <div class="avatar-stack" aria-label="Personas conectadas">
          <span v-for="user in store.users.slice(0, 5)" :key="user.username" class="avatar" :title="user.username">
            {{ initials(user.username) }}
          </span>
          <span v-if="store.users.length > 5" class="avatar avatar--more">+{{ store.users.length - 5 }}</span>
        </div>
      </div>
    </aside>

    <section class="workspace__main">
      <header class="workspace__header">
        <button class="icon-button icon-button--light mobile-only" aria-label="Abrir directorio" @click="showMobileAside = true">
          <AppIcon name="menu" />
        </button>
        <div class="connection-badge" :data-state="store.connection">
          <span class="live-dot" />
          {{ store.connectionMessage }}
        </div>
        <div class="header-user">
          <span>@{{ store.currentUser?.username }}</span>
          <button class="icon-button icon-button--light" aria-label="Salir" @click="exit"><AppIcon name="logout" /></button>
        </div>
      </header>

      <AnimatePresence mode="wait" :initial="false">
        <motion.div
          v-if="!store.activeRoom"
          key="directory"
          class="directory-view"
          :initial="reduceMotion ? false : { opacity: 0, x: -14, filter: 'blur(8px)' }"
          :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
          :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, x: -10, filter: 'blur(6px)' }"
          :transition="{ duration: reduceMotion ? 0 : 0.28, ease: [0.22, 1, 0.36, 1] }"
        >
          <div class="directory-intro">
            <span class="section-kicker">DESCUBRE</span>
            <h1>¿De qué hablamos hoy?</h1>
            <p>Entra a una sala pública o crea un espacio nuevo para empezar la conversación.</p>
          </div>

          <div class="room-grid">
            <motion.button
              v-for="(room, index) in filteredRooms"
              :key="room.id"
              layout
              class="room-card"
              :data-tone="roomTone(room)"
              type="button"
              :initial="reduceMotion ? false : { opacity: 0, y: 14, filter: 'blur(6px)' }"
              :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
              :transition="{ duration: reduceMotion ? 0 : 0.36, delay: reduceMotion ? 0 : index * 0.055 }"
              :while-hover="reduceMotion ? undefined : { x: 5 }"
              :while-press="reduceMotion ? undefined : { scale: 0.995 }"
              @click="openRoom(room)"
            >
              <span class="room-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="room-card__icon">#</span>
              <span class="room-card__copy">
                <strong>{{ room.name }}</strong>
                <span>{{ room.description || 'Una conversación abierta para la comunidad.' }}</span>
              </span>
              <span class="room-card__meta">
                <span>{{ room.activeUsers || 0 }} online</span>
                <AppIcon name="arrow" />
              </span>
            </motion.button>
          </div>
        </motion.div>

        <motion.div
          v-else
          key="conversation"
          class="conversation-view"
          :initial="reduceMotion ? false : { opacity: 0, x: 18, filter: 'blur(8px)' }"
          :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
          :exit="reduceMotion ? { opacity: 0 } : { opacity: 0, x: 12, filter: 'blur(6px)' }"
          :transition="{ duration: reduceMotion ? 0 : 0.3, ease: [0.22, 1, 0.36, 1] }"
        >
          <header class="conversation-head">
            <button class="icon-button icon-button--light" aria-label="Volver al directorio" @click="store.leaveRoom">
              <AppIcon name="back" />
            </button>
            <span class="room-glyph room-glyph--large" :data-tone="roomTone(store.activeRoom)">#</span>
            <div>
              <h1>{{ store.activeRoom.name }}</h1>
              <p>{{ store.activeRoom.description }}</p>
            </div>
            <span class="conversation-count"><span class="live-dot" /> {{ store.activeRoom.activeUsers || 0 }} aquí</span>
          </header>

          <div ref="messagesElement" class="message-feed" aria-live="polite">
            <div class="date-divider"><span>Conversación reciente</span></div>
            <AnimatePresence :initial="false">
              <template v-for="item in store.activeMessages" :key="messageKey(item)">
                <motion.div
                  v-if="item.type !== 'CHAT'"
                  :key="messageKey(item)"
                  layout
                  class="system-message"
                  :initial="reduceMotion ? false : { opacity: 0, scale: 0.96, filter: 'blur(5px)' }"
                  :animate="{ opacity: 1, scale: 1, filter: 'blur(0px)' }"
                  :exit="{ opacity: 0 }"
                >
                  <span>{{ item.userId }}</span> {{ item.type === 'JOIN' ? 'se unió a la sala' : 'salió de la sala' }}
                </motion.div>
                <motion.article
                  v-else
                  :key="messageKey(item)"
                  layout
                  class="message"
                  :class="{ 'message--mine': isMine(item) }"
                  :initial="reduceMotion ? false : { opacity: 0, y: 10, scale: 0.985, filter: 'blur(7px)' }"
                  :animate="{ opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' }"
                  :exit="{ opacity: 0, scale: 0.985 }"
                  :transition="{ duration: reduceMotion ? 0 : 0.28, ease: [0.22, 1, 0.36, 1] }"
                >
                  <span class="avatar">{{ initials(item.userId) }}</span>
                  <div class="message__body">
                    <div class="message__meta">
                      <strong>{{ isMine(item) ? 'Tú' : item.userId }}</strong>
                      <time>{{ relativeTime(item.createdAt) }}</time>
                    </div>
                    <p>{{ item.body }}</p>
                  </div>
                </motion.article>
              </template>
            </AnimatePresence>
            <div v-if="store.activeMessages.length === 0" class="conversation-empty">
              <AppIcon name="sparkles" :size="28" />
              <strong>Abre la conversación.</strong>
              <span>Sé la primera persona en escribir en esta sala.</span>
            </div>
          </div>

          <form class="composer" @submit.prevent="submitMessage">
            <div class="composer__identity">{{ initials(store.currentUser?.username) }}</div>
            <textarea
              v-model="message"
              rows="1"
              maxlength="1000"
              placeholder="Escribe un mensaje…"
              aria-label="Mensaje"
              @keydown.enter.exact.prevent="submitMessage"
            />
            <span class="composer__hint">Shift + Enter para una línea nueva</span>
            <button class="send-button" type="submit" :disabled="!message.trim()" aria-label="Enviar mensaje">
              <AppIcon name="send" />
            </button>
          </form>
        </motion.div>
      </AnimatePresence>
    </section>

    <AnimatePresence>
      <motion.div
        v-if="showCreateRoom"
        key="create-room-modal"
        class="modal-backdrop"
        role="presentation"
        :initial="reduceMotion ? false : { opacity: 0, backdropFilter: 'blur(0px)' }"
        :animate="{ opacity: 1, backdropFilter: 'blur(8px)' }"
        :exit="{ opacity: 0, backdropFilter: 'blur(0px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.22 }"
        @click.self="showCreateRoom = false"
      >
        <motion.form
          class="modal"
          aria-labelledby="new-room-title"
          :initial="reduceMotion ? false : { opacity: 0, y: 20, scale: 0.98, filter: 'blur(8px)' }"
          :animate="{ opacity: 1, y: 0, scale: 1, filter: 'blur(0px)' }"
          :exit="{ opacity: 0, y: 10, scale: 0.99, filter: 'blur(5px)' }"
          :transition="{ duration: reduceMotion ? 0 : 0.28, ease: [0.22, 1, 0.36, 1] }"
          @submit.prevent="createRoom"
        >
          <div class="modal__head">
            <div>
              <span class="section-kicker">NUEVO ESPACIO</span>
              <h2 id="new-room-title">Crear una sala</h2>
            </div>
            <button class="icon-button icon-button--light" type="button" aria-label="Cerrar" @click="showCreateRoom = false">
              <AppIcon name="close" />
            </button>
          </div>
          <label>Nombre <input v-model="roomName" minlength="3" maxlength="30" required placeholder="Ej. Diseño de producto"></label>
          <label>Descripción <textarea v-model="roomDescription" minlength="8" maxlength="255" required rows="3" placeholder="¿De qué se conversa aquí?" /></label>
          <div class="modal__actions">
            <button class="button button--ghost-dark" type="button" @click="showCreateRoom = false">Cancelar</button>
            <button class="button button--primary" type="submit">Crear sala <AppIcon name="arrow" /></button>
          </div>
        </motion.form>
      </motion.div>
    </AnimatePresence>
  </main>
</template>
