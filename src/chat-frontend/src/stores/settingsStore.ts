import {defineStore} from "pinia";
import {ref} from "vue";

export const useSettingsStore = defineStore("settings", () => {
  const navigationDrawerMobile = ref<boolean>(false);

  const toggleNavigationDrawerMobile = () => {
    navigationDrawerMobile.value = !navigationDrawerMobile.value;
  }

  return {
    navigationDrawerMobile,
    toggleNavigationDrawerMobile
  }
})