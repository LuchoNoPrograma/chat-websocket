<script setup lang="ts">
import { computed, ref } from 'vue';
import { motion, useReducedMotion } from 'motion-v';
import { useRouter } from 'vue-router';
import AppIcon from '../components/AppIcon.vue';
import { useRealtimeStore } from '../stores/realtime';

const router = useRouter();
const store = useRealtimeStore();
const username = ref('');
const error = ref('');
const reduceMotion = useReducedMotion();

const isValid = computed(() => username.value.trim().length >= 3 && username.value.trim().length <= 20);

const enterLive = async () => {
  if (!isValid.value || store.connection === 'connecting') return;
  error.value = '';
  try {
    await store.connect(username.value);
    await router.push('/chat');
  } catch {
    error.value = store.connectionMessage;
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
      <a class="brand" href="/" aria-label="Relay inicio">
        <span class="brand__mark">R/</span>
        <span>Relay</span>
      </a>
      <span class="nav-note">Vue + Spring · Tiempo real</span>
    </motion.nav>

    <section class="welcome__content">
      <motion.div
        class="welcome__copy will-change-transform"
        :initial="reduceMotion ? false : { opacity: 0, y: 24, filter: 'blur(14px)' }"
        :animate="{ opacity: 1, y: 0, filter: 'blur(0px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.72, delay: 0.12, ease: [0.22, 1, 0.36, 1] }"
      >
        <div class="eyebrow"><span class="live-dot" /> Chat en tiempo real</div>
        <h1>Conversaciones<br><em>sin demora.</em></h1>
        <p class="welcome__lead">
          Salas públicas, presencia en vivo y mensajes compartidos en una experiencia construida con WebSockets.
        </p>
        <div class="tech-line" aria-label="Tecnologías principales">
          <span>Vue 3.5</span>
          <span>STOMP</span>
          <span>Spring Boot</span>
          <span>H2 Memory</span>
        </div>
      </motion.div>

      <motion.div
        class="welcome__access will-change-transform"
        aria-labelledby="access-title"
        :initial="reduceMotion ? false : { opacity: 0, x: 22, filter: 'blur(12px)' }"
        :animate="{ opacity: 1, x: 0, filter: 'blur(0px)' }"
        :transition="{ duration: reduceMotion ? 0 : 0.68, delay: 0.24, ease: [0.22, 1, 0.36, 1] }"
      >
        <span class="access-index">01 / ACCESO</span>
        <h2 id="access-title">Elige cómo entrar</h2>
        <p>Usa un alias. No necesitas contraseña ni crear una cuenta.</p>

        <form @submit.prevent="enterLive">
          <label for="username">Tu alias</label>
          <div class="input-line">
            <span>@</span>
            <input
              id="username"
              v-model="username"
              maxlength="20"
              minlength="3"
              placeholder="nombre.dev"
              autocomplete="username"
              autofocus
            >
          </div>
          <p v-if="error" class="form-error" role="alert">{{ error }}</p>
          <motion.button
            class="button button--primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-relay-lime"
            type="submit"
            :disabled="!isValid || store.connection === 'connecting'"
            :while-press="reduceMotion ? undefined : { scale: 0.98 }"
          >
            <span>{{ store.connection === 'connecting' ? 'Conectando…' : 'Entrar al chat' }}</span>
            <AppIcon name="arrow" />
          </motion.button>
        </form>
        <small>La sesión se restaura al estado inicial después de 30 minutos sin actividad.</small>
      </motion.div>
    </section>

    <footer class="welcome__footer">
      <span>Sesiones efímeras · Datos iniciales automáticos</span>
    </footer>
  </main>
</template>
