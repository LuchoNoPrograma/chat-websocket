<script setup lang="ts">
import { computed, ref } from 'vue';
import { motion, useReducedMotion } from 'motion-v';
import { useRoute, useRouter } from 'vue-router';
import AppIcon from '../components/common/AppIcon.vue';
import { useRealtimeStore } from '../stores/realtime';
import { useThemeStore } from '../stores/theme';
import { AVATAR_OPTIONS, type AvatarId } from '../types/chat';

const router = useRouter();
const route = useRoute();
const store = useRealtimeStore();
const themeStore = useThemeStore();
const username = ref('');
const selectedAvatar = ref<AvatarId>('claudia');
const error = ref('');
const entering = ref(false);
const reduceMotion = useReducedMotion();

const isValid = computed(() => username.value.trim().length >= 3 && username.value.trim().length <= 20);
const accessLabel = computed(() => {
  if (entering.value) return 'Conectando…';
  if (isValid.value) return `Entrar como @${username.value.trim()}`;
  return 'Escribe tu alias para entrar';
});

const enterLive = async () => {
  if (!isValid.value || entering.value) return;
  error.value = '';
  entering.value = true;
  try {
    await store.connect(username.value, selectedAvatar.value);
    await router.push('/chat');
  } catch {
    error.value = store.connectionMessage;
  } finally {
    entering.value = false;
  }
};

</script>

<template>
  <main class="welcome selection:bg-relay-lime selection:text-relay-ink">
    <motion.nav
      class="welcome__nav will-change-transform"
      aria-label="Navegación principal"
      :initial="reduceMotion ? false : { opacity: 0, y: -12 }"
      :animate="{ opacity: 1, y: 0 }"
      :transition="{ duration: reduceMotion ? 0 : 0.45, delay: 0.08 }"
    >
      <a class="brand" href="/" aria-label="Chatty inicio">
        <span class="brand__mark"><span class="brand__logo" aria-hidden="true" /></span>
        <span>Chatty</span>
      </a>
      <span class="nav-note">¿También venías solo a leer?</span>
      <button
        class="theme-toggle"
        type="button"
        :aria-label="themeStore.isDark ? 'Activar modo día' : 'Activar modo noche'"
        :aria-pressed="themeStore.isDark"
        @click="themeStore.toggle"
      >
        <AppIcon :name="themeStore.isDark ? 'sun' : 'moon'" :size="16" />
        <span>{{ themeStore.isDark ? 'Día' : 'Noche' }}</span>
      </button>
    </motion.nav>

    <section class="welcome__content">
      <motion.div
        class="welcome__copy will-change-transform"
        :initial="reduceMotion ? false : { opacity: 0, y: 24, filter: 'blur(14px)' }"
        :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.72, delay: 0.12, ease: [0.22, 1, 0.36, 1] }"
      >
        <div class="eyebrow">SIN REGISTRO. SIN VUELTAS.</div>
        <h1>¿De qué<br><em> hablamos?</em></h1>
        <p class="welcome__lead">Una canción en bucle, un bug que no sale o el plan del finde. Elige una sala y suelta el tema.</p>
        <div class="welcome-guide">
          <div><span>1</span><p><strong>Ponte un alias</strong><small>El alias y la foto los eliges tú.</small></p></div>
          <div><span>2</span><p><strong>Busca una sala</strong><small>Entra por el tema o crea la tuya.</small></p></div>
          <div><span>3</span><p><strong>Ya estás dentro</strong><small>Puedes escribir o leer un rato.</small></p></div>
        </div>
      </motion.div>

      <motion.div
        class="welcome__access will-change-transform"
        aria-labelledby="access-title"
        :initial="reduceMotion ? false : { opacity: 0, x: 22, filter: 'blur(12px)' }"
        :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.68, delay: 0.24, ease: [0.22, 1, 0.36, 1] }"
      >
        <p v-if="route.query.salida" class="logout-feedback" role="status">Saliste del chat. Vuelve cuando quieras.</p>
        <span class="access-index">TU PERFIL</span>
        <h2 id="access-title">Entra con un alias</h2>
        <p>Así te verán en las salas. No necesitas crear una cuenta.</p>

        <form :aria-busy="entering" @submit.prevent="enterLive">
          <label for="username">Tu alias</label>
          <div class="input-line">
            <span>@</span>
            <input
              id="username"
              v-model="username"
              maxlength="20"
              minlength="3"
              placeholder="Ej. alex"
              :disabled="entering"
              aria-describedby="alias-hint"
              :aria-invalid="Boolean(error)"
              autocomplete="username"
              enterkeyhint="go"
            >
          </div>
          <p id="alias-hint" class="input-hint">Entre 3 y 20 caracteres. Será visible para los demás.</p>
          <fieldset class="photo-picker" :disabled="entering">
            <legend>Elige tu foto</legend>
            <div class="photo-options">
              <button
                v-for="(avatar, index) in AVATAR_OPTIONS"
                :key="avatar.id"
                class="photo-choice"
                :class="{ 'is-selected': selectedAvatar === avatar.id }"
                type="button"
                :aria-label="`Seleccionar foto ${index + 1}`"
                :aria-pressed="selectedAvatar === avatar.id"
                @click="selectedAvatar = avatar.id"
              >
                <span class="avatar avatar--image" :data-avatar="avatar.id" aria-hidden="true" />
                <span v-if="selectedAvatar === avatar.id" class="photo-choice__check" aria-hidden="true">✓</span>
              </button>
            </div>
          </fieldset>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <p v-else role="status" class="input-hint" :class="{ 'is-ready': isValid }">
            {{ entering ? 'Conectando con el chat…' : isValid ? `Todo listo, @${username.trim()}.` : 'Elige un alias para continuar.' }}
          </p>
          <motion.button
            class="button button--primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-relay-lime"
            :class="{ 'is-ready': isValid, 'is-loading': entering }"
            type="submit"
            :disabled="!isValid || entering"
            :while-press="reduceMotion ? undefined : { scale: 0.98 }"
          >
            <span class="button__label">{{ accessLabel }}</span>
            <span class="button__icon"><span v-if="entering" class="loading-spinner" /><AppIcon v-else name="arrow" /></span>
          </motion.button>
        </form>
        <small>El chat se reinicia después de 30 minutos sin actividad de nadie. Los mensajes no se guardan para siempre.</small>
      </motion.div>
    </section>

    <footer class="welcome__footer">
      <span>Detrás de cada alias hay alguien. Trata bien a la gente.</span>
    </footer>
  </main>
</template>
