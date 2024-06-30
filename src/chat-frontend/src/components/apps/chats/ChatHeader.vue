<script lang="ts" setup>

import {Menu2Icon} from "vue-tabler-icons";
import NotificationDD from "@/layouts/full/vertical-header/NotificationDD.vue";
import {useChatStore} from "@/stores/apps/chat";
import {useRouter} from "vue-router";
import {useCustomizerStore} from "@/stores/customizer";
import {ref} from "vue";
import {useTheme} from "vuetify";

const theme = useTheme();
const chatStore = useChatStore();
const router = useRouter();

const logout = async () => {
  chatStore.disconnect();
  await router.push('/auth/register')
}

const customizerStore = useCustomizerStore();

const isDarkMode = ref<boolean>(true);
const changeTheme = () => {
  if (isDarkMode.value) {
    customizerStore.SET_THEME("AQUA_THEME")
  } else {
    customizerStore.SET_THEME("DARK_AQUA_THEME")
  }
  isDarkMode.value = !isDarkMode.value
  console.log('Theme changed')
}
</script>

<template>
  <v-app-bar id="top" color="primary" elevation="5" height="64">

    <v-btn class="hidden-md-and-down " color="primary" icon variant="text">
      <menu2-icon size="25"></menu2-icon>
    </v-btn>
    <v-btn class="hidden-lg-and-up" icon size="small" variant="text">
      <menu2-icon size="25"></menu2-icon>
    </v-btn>

    <v-spacer></v-spacer>

    <v-btn @click="changeTheme" icon>
      <v-icon :icon="isDarkMode? 'mdi-weather-sunny' : 'mdi-weather-night'" size="25"></v-icon>
    </v-btn>
    <notification-d-d></notification-d-d>
    <div class="ml-2">
      <v-btn append-icon="mdi-logout" variant="outlined" @click="logout">Cerrar sesión</v-btn>
    </div>
  </v-app-bar>

  <!-- ---------------------------------------------- -->
  <!-- Right Sidebar -->
  <!-- ---------------------------------------------- -->
  <!--  <v-navigation-drawer location="right" temporary >
      <RightMobileSidebar />
    </v-navigation-drawer>-->
</template>

<style lang="scss" scoped>

</style>