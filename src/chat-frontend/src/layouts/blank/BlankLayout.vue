<script lang="ts" setup>
import {RouterView} from 'vue-router';
import {useCustomizerStore} from "@/stores/customizer";

const customizerStore = useCustomizerStore();
</script>
<template>
  <v-locale-provider>
    <v-app :class="[
             customizerStore.actTheme,
                customizerStore.mini_sidebar ? 'mini-sidebar' : '',
                customizerStore.setHorizontalLayout ? 'horizontalLayout' : 'verticalLayout',
                customizerStore.setBorderCard ? 'cardBordered' : '',
                customizerStore.inputBg ? 'inputWithbg' : ''
           ]" :theme="customizerStore.actTheme"
           class="pa-4"
    >
      <v-main>
        <v-container class="page-wrapper py-0" fluid>
          <div class="maxWidth">
            <router-view></router-view>
          </div>
        </v-container>
      </v-main>
    </v-app>
  </v-locale-provider>
</template>