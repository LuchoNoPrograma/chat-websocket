<script lang="ts" setup>
import {RouterView} from 'vue-router';
import VerticalSidebarVue from './vertical-sidebar/VerticalSidebar.vue';
import VerticalHeaderVue from './vertical-header/VerticalHeader.vue';
import HorizontalHeader from './horizontal-header/HorizontalHeader.vue';
import HorizontalSidebar from './horizontal-sidebar/HorizontalSidebar.vue';
import Customizer from './customizer/Customizer.vue';
import {useCustomizerStore} from '../../stores/customizer';

const customizer = useCustomizerStore();
</script>

<template>
  <!-----LTR LAYOUT------->
  <v-locale-provider>
    <v-app
      :class="[
                customizer.actTheme,
                customizer.mini_sidebar ? 'mini-sidebar' : '',
                customizer.setHorizontalLayout ? 'horizontalLayout' : 'verticalLayout',
                customizer.setBorderCard ? 'cardBordered' : '',
                customizer.inputBg ? 'inputWithbg' : ''
            ]"
      :theme="customizer.actTheme"
    >
      <!---Customizer location right side--->
      <v-navigation-drawer v-model="customizer.Customizer_drawer" app location="right" temporary
                           width="320">
        <Customizer/>
      </v-navigation-drawer>
      <VerticalHeaderVue v-if="!customizer.setHorizontalLayout"/>
      <VerticalSidebarVue v-if="!customizer.setHorizontalLayout"/>
      <HorizontalHeader v-if="customizer.setHorizontalLayout"/>
      <HorizontalSidebar v-if="customizer.setHorizontalLayout"/>
      <v-main>
        <v-container class="page-wrapper pb-sm-15 pb-10" fluid>
          <div :class="customizer.boxed ? 'maxWidth' : ''">
            <router-view></router-view>
          </div>
        </v-container>
      </v-main>
    </v-app>
  </v-locale-provider>
</template>
