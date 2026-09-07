<script setup lang="ts">
const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 20 });

const paths: Record<string, string[]> = {
  info: ['M12 11v6', 'M12 7h.01', 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0'],
  checks: ['m2 12 4 4L16 6', 'm12 16 10-10'],
  check: ['m5 12 4 4L19 6'],
  arrow: ['M5 12h14', 'm13 6 6 6-6 6'],
  back: ['m15 18-6-6 6-6'],
  hash: ['M5 9h14M5 15h14M11 4 9 20M17 4l-2 16'],
  users: ['M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2', 'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8', 'M22 21v-2a4 4 0 0 0-3-3.87', 'M16 3.13a4 4 0 0 1 0 7.75'],
  plus: ['M12 5v14M5 12h14'],
  search: ['m21 21-4.35-4.35', 'M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0'],
  logout: ['M10 17l5-5-5-5M15 12H3', 'M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4'],
  send: ['m22 2-7 20-4-9-9-4Z', 'M22 2 11 13'],
  reply: ['m9 17-5-5 5-5', 'M4 12h9a7 7 0 0 1 7 7'],
  message: ['M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4Z'],
  smile: ['M8 14s1.5 2 4 2 4-2 4-2', 'M9 9h.01M15 9h.01', 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0'],
  clock: ['M12 7v5l3 2', 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0'],
  signal: ['M5 12.55a11 11 0 0 1 14.08 0', 'M8.5 16a6 6 0 0 1 7 0', 'M12 20h.01'],
  sparkles: ['m12 3-1.5 4.5L6 9l4.5 1.5L12 15l1.5-4.5L18 9l-4.5-1.5Z', 'm19 16-.75 2.25L16 19l2.25.75L19 22l.75-2.25L22 19l-2.25-.75Z'],
  menu: ['M4 6h16M4 12h16M4 18h16'],
  close: ['M18 6 6 18M6 6l12 12'],
  sun: ['M12 3v2M12 19v2M3 12h2M19 12h2M5.64 5.64l1.42 1.42M16.94 16.94l1.42 1.42M5.64 18.36l1.42-1.42M16.94 7.06l1.42-1.42', 'M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0'],
  moon: ['M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5 8.5 8.5 0 1 0 20.5 14.5Z']
};
</script>

<template>
  <svg
    aria-hidden="true"
    fill="none"
    :height="props.size"
    viewBox="0 0 24 24"
    :width="props.size"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      v-for="path in paths[props.name] ?? []"
      :key="path"
      :d="path"
      stroke="currentColor"
      stroke-linecap="round"
      stroke-linejoin="round"
      stroke-width="1.8"
    />
  </svg>
</template>
