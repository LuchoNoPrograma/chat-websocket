<script setup lang="ts">
import type { RoomActivity } from '../../types/chat';

defineProps<{
  activity?: RoomActivity;
  compact?: boolean;
}>();
</script>

<template>
  <span
    v-if="activity?.unread"
    class="activity-badge"
    :class="{ 'activity-badge--reply': activity.replies > 0, 'activity-badge--compact': compact }"
    :aria-label="activity.replies > 0
      ? `${activity.replies} respuesta${activity.replies === 1 ? '' : 's'} nueva${activity.replies === 1 ? '' : 's'}`
      : `${activity.unread} mensaje${activity.unread === 1 ? '' : 's'} nuevo${activity.unread === 1 ? '' : 's'}`"
  >
    <span class="activity-badge__pulse" />
    <span v-if="!compact" class="activity-badge__label">{{ activity.replies > 0 ? 'Respuesta' : 'Nuevo' }}</span>
    <strong>{{ activity.replies || activity.unread }}</strong>
  </span>
</template>
