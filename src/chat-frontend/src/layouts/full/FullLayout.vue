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
  <!-----RTL LAYOUT------->
  <v-locale-provider v-if="customizer.setRTLLayout" rtl>
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
      <!---Customizer location left side--->
      <v-navigation-drawer v-model="customizer.Customizer_drawer" app class="left-customizer" elevation="10" location="left"
                           temporary width="320">
      </v-navigation-drawer>
      <VerticalHeaderVue v-if="!customizer.setHorizontalLayout"/>
      <VerticalSidebarVue v-if="!customizer.setHorizontalLayout"/>
      <HorizontalHeader v-if="customizer.setHorizontalLayout"/>
      <HorizontalSidebar v-if="customizer.setHorizontalLayout"/>

      <v-main>
        <v-container class="page-wrapper pb-sm-15 pb-10" fluid>
          <div :class="customizer.boxed ? 'maxWidth' : ''">
            <RouterView/>
            <v-btn
              class="customizer-btn"
              color="primary"
              icon
              size="large"
              variant="flat"
              @click.stop="customizer.SET_CUSTOMIZER_DRAWER(!customizer.Customizer_drawer)"
            >
              <SettingsIcon/>
            </v-btn>
          </div>
        </v-container>
      </v-main>
    </v-app>
  </v-locale-provider>

  <!-----LTR LAYOUT------->
  <v-locale-provider v-else>
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
      <v-navigation-drawer v-model="customizer.Customizer_drawer" app elevation="10" location="right" temporary
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
            <RouterView/>
            <v-btn
              class="customizer-btn"
              color="primary"
              icon
              size="large"
              variant="flat"
              @click.stop="customizer.SET_CUSTOMIZER_DRAWER(!customizer.Customizer_drawer)"
            >
              <SettingsIcon/>
            </v-btn>
          </div>
        </v-container>
      </v-main>
    </v-app>
  </v-locale-provider>
</template>
