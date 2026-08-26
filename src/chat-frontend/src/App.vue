<script setup lang="ts">
import { AnimatePresence, motion, useReducedMotion } from 'motion-v';
import { RouterView } from 'vue-router';

const reduceMotion = useReducedMotion();
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <AnimatePresence mode="wait" :initial="false">
      <motion.div
        :key="route.path"
        class="app-route isolate"
        :initial="reduceMotion ? false : { opacity: 0, filter: 'blur(10px)', y: 8 }"
        :animate="{ opacity: 1, filter: 'blur(0px)', y: 0 }"
        :exit="reduceMotion ? { opacity: 1 } : { opacity: 0, filter: 'blur(8px)', y: -6 }"
        :transition="{ duration: reduceMotion ? 0 : 0.32, ease: [0.22, 1, 0.36, 1] }"
      >
        <component :is="Component" />
      </motion.div>
    </AnimatePresence>
  </RouterView>
</template>
