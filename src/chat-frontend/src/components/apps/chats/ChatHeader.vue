<script lang="ts" setup>

import {Menu2Icon} from "vue-tabler-icons";
import NotificationDD from "@/layouts/full/vertical-header/NotificationDD.vue";
import {useChatStore} from "@/stores/apps/chatStore";
import {useRouter} from "vue-router";
import {useCustomizerStore} from "@/stores/customizer";
import {ref} from "vue";
import {useTheme} from "vuetify";
import {useSettingsStore} from "@/stores/settingsStore";

const theme = useTheme();
const chatStore = useChatStore();
const router = useRouter();

const emits = defineEmits({
  'toggle-navigation-drawer': () => true
});

const logout = async () => {
  chatStore.disconnect();
  await router.push('/auth/register')
}

const customizerStore = useCustomizerStore();

const isDarkMode = ref<boolean>(false);
const changeTheme = () => {
  if (isDarkMode.value) {
    customizerStore.SET_THEME("BLUE_THEME")
  } else {
    customizerStore.SET_THEME("DARK_AQUA_THEME")
  }
  isDarkMode.value = !isDarkMode.value
  console.log('Theme changed')
}

const settingsStore = useSettingsStore();
</script>

<template>
<!--  <v-btn class="hidden-md-and-down " color="primary" icon variant="text">
    <menu2-icon size="25"></menu2-icon>
  </v-btn>-->
  <v-btn class="hidden-lg-and-up" icon size="small" variant="text" @click="settingsStore.toggleNavigationDrawerMobile()">
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

  <!-- ---------------------------------------------- -->
  <!-- Right Sidebar -->
  <!-- ---------------------------------------------- -->
  <!--  <v-navigation-drawer location="right" temporary >
      <RightMobileSidebar />
    </v-navigation-drawer>-->
</template>

<style lang="scss" scoped>

</style>