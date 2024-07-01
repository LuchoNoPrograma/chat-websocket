<script lang="ts" setup>
import { ref, watch } from "vue";
import { useChatStore } from "@/stores/apps/chatStore";
import { useRouter } from "vue-router";

const chatStore = useChatStore();
const router = useRouter();
const valid = ref(false);
const username = ref('');
const usernameRules = ref([
    (v: string) => !!v || 'El nombre de usuario es obligatorio.',
    (v: string) => (v && v.length >= 3) || 'El nombre debe tener minimo 3 caracteres.',
    (v: string) => (v.length <= 15 || 'El limite de caracteres permitido es de 15.')
]);

const sendData = async () => {
  console.log("Validando")
  if(!valid.value) return;
  chatStore.connect(username.value)
  await router.push("/chat");
}

watch(username, (newValue) => {
  valid.value = usernameRules.value.every(rule => rule(newValue) === true);
});
</script>
<template>
    <div class="text-center mb-6">
        <div class="text-h6 w-100 px-5 font-weight-regular auth-divider position-relative">
            <span class="bg-surface px-5 py-3 position-relative">Inicia sesión</span>
        </div>
    </div>
    <v-form ref="form" v-model="valid" class="mt-5" lazy-validation @submit.prevent="sendData">
        <v-label class="text-subtitle-1 font-weight-medium pb-2">Nombre de usuario</v-label>
        <v-text-field v-model="username" :rules="usernameRules" required></v-text-field>

        <div class="d-flex justify-center mt-4">
          <v-btn variant="elevated" color="primary" append-icon="mdi-comment-arrow-right" type="submit">Unirse al chat</v-btn>
        </div>
    </v-form>
</template>
