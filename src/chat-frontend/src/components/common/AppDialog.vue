<script setup lang="ts">
import { ref, useId, watchEffect } from 'vue';
import AppIcon from './AppIcon.vue';

defineProps<{ title: string; description?: string }>();
const open = defineModel<boolean>({ required: true });
const dialog = ref<HTMLDialogElement>();
const titleId = useId();
watchEffect(() => {
  if (!dialog.value) return;
  if (open.value && !dialog.value.open) dialog.value.showModal();
  else if (!open.value && dialog.value.open) dialog.value.close();
});
</script>

<template>
  <Teleport to="body">
    <dialog ref="dialog" class="app-dialog" :aria-labelledby="titleId" @close="open = false" @cancel="open = false" @click="$event.target === dialog && (open = false)">
      <header class="app-dialog__head">
        <div><h2 :id="titleId">{{ title }}</h2><p v-if="description">{{ description }}</p></div>
        <button type="button" class="app-dialog__close" aria-label="Cerrar diálogo" autofocus @click="open = false"><AppIcon name="close" /></button>
      </header>
      <div class="app-dialog__body"><slot /></div>
    </dialog>
  </Teleport>
</template>

<style>
.app-dialog { width: min(480px, calc(100vw - 32px)); max-height: calc(100dvh - 32px); margin: auto; padding: 0; overflow-y: auto; overscroll-behavior: contain; border: 1px solid var(--line); border-radius: 16px; background: var(--surface); color: var(--text); box-shadow: 0 20px 60px #0003; }
.app-dialog::backdrop { background: #10141c80; }
.app-dialog__head { position: sticky; top: 0; z-index: 1; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 22px 24px 18px; border-bottom: 1px solid var(--line); background: var(--surface); }
.app-dialog__head h2 { margin: 0; font-size: 20px; line-height: 1.4; letter-spacing: -0.025em; }
.app-dialog__head p { margin: 6px 0 0; color: var(--muted); font-size: 13px; line-height: 1.5; }
.app-dialog__close { display: grid; place-items: center; flex-shrink: 0; width: 44px; height: 44px; margin: -8px -10px -8px 0; border: 0; border-radius: 8px; background: transparent; color: var(--muted); }
.app-dialog__close:hover { background: var(--paper-muted); color: var(--text); }
.app-dialog__body { padding: 24px; }
.dialog-form { display: grid; gap: 20px; }
.dialog-form label { display: grid; gap: 8px; font-size: 13px; font-weight: 600; }
.dialog-form input, .dialog-form textarea { width: 100%; padding: 12px; border: 1px solid var(--line); border-radius: 8px; background: var(--paper); color: var(--text); font-size: 16px; font-weight: 400; }
.dialog-form textarea { resize: vertical; min-height: 100px; max-height: 30dvh; }
.app-dialog :focus-visible { outline: 2px solid var(--lime); outline-offset: 2px; }
.dialog-form__actions { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 10px; padding-top: 4px; }
.dialog-form__actions .button { width: auto; border-radius: 8px; min-height: 44px; padding: 10px 16px; font-size: 13px; }
@media(max-width:480px) { .app-dialog__head { padding: 18px 20px; } .app-dialog__body { padding: 20px; } }
</style>
