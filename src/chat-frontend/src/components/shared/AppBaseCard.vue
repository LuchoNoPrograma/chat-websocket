<script lang="ts" setup>
import {ref} from 'vue';
import {useDisplay} from 'vuetify';
import {useSettingsStore} from "@/stores/settingsStore";

const {xs, lgAndUp} = useDisplay();
const settingsStore = useSettingsStore();
</script>

<template>
  <v-app-bar id="top" color="primary" elevation="0" height="60">
    <slot name="app-bar"></slot>
  </v-app-bar>

  <v-navigation-drawer width="320">
    <slot name="navigation-drawer"></slot>
  </v-navigation-drawer>

  <v-navigation-drawer temporary v-model="settingsStore.navigationDrawerMobile" width="320" top v-if="!lgAndUp">
    <v-card-text class="pa-0">
      <slot name="mobileLeftContent"></slot>
    </v-card-text>
  </v-navigation-drawer>

  <slot name="main-content"></slot>

  <!---/Left chat list -->
  <div class="d-flex">
    <div v-if="lgAndUp && $.slots.leftpart" class="left-part">
      <!-- <perfect-scrollbar style="height: calc(100vh - 290px)"> -->
      <slot name="leftpart"></slot>
      <!-- </perfect-scrollbar> -->
    </div>

    <!---right chat conversation -->
    <div class="right-part">
      <!---Toggle Button For mobile-->
      <v-btn block class="d-lg-none d-md-flex d-sm-flex" variant="text" @click="settingsStore.toggleNavigationDrawerMobile()">
        <Menu2Icon class="mr-2" size="20"/>
        Menu
      </v-btn>
      <v-divider class="d-lg-none d-block"/>
      <slot name="rightpart"></slot>
    </div>

    <!---right chat conversation -->
  </div>
</template>

<style lang="scss">
.mainbox {
  position: relative;
  overflow: hidden;
}

.left-part {
  width: 320px;
  /*border-right: 1px solid rgb(var(--v-theme-borderColor));*/
  min-height: 500px;
  transition: 0.1s ease-in;
  flex-shrink: 0;
}

.v-theme--light {
  .left-part {
    background: white;
  }
}

.v-theme--dark {
  .left-part {
    background: #2b2b2b;
  }
}

.right-part {
  width: 100%;
  min-height: 500px;
  position: relative;
}
</style>
