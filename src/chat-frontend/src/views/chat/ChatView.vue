<script lang="ts" setup>
// common components
import AppBaseCard from '@/components/shared/AppBaseCard.vue';
import {useRouter} from "vue-router";
import ChatHeader from "@/components/apps/chats/ChatHeader.vue";
import {ref} from "vue";

import maleProfile from "@/assets/images/chat/default-male-profile.png";
import type {RoomType} from "@/types/model/RoomTypes";
import {useUserStore} from "@/stores/userStore";
import {useRoomStore} from "@/stores/roomStore";
import type {UserType} from "@/types/model/UserTypes";

const roomStore= useRoomStore();
const userStore= useUserStore();
const router = useRouter();

const window = ref<number>(0);

const isCurrentUser = (user: UserType) => user.username === userStore.userConnected?.username

const enterRoom = async (room: RoomType) => {
  await router.push({path: `/room/${room.id}`, replace: true})//ChatDetail.vue
}
</script>

<template>
  <app-base-card>
    <template v-slot:app-bar>
      <chat-header></chat-header>
    </template>
    <template v-slot:navigation-drawer>
      <v-tabs v-model="window" color="primary">
        <v-tab value="0">Privados</v-tab>
        <v-tab value="1">Salas</v-tab>
      </v-tabs>
      <v-divider class="border border-sm"></v-divider>
      <v-window v-model="window">
        <v-window-item>
          <h4 class="font-weight-medium ml-4 mt-2">
            Usuarios en linea ({{ userStore.userOnlineList.length }})
          </h4>

          <v-list>
            <perfect-scrollbar class="h-100">
              <v-list-item v-for="(user, index) in userStore.userOnlineList" :key="index" class="mx-0 pa-0">
                <div :class="{'border-b-0': index < userStore.userOnlineList.length - 1, 'bg-lightsuccess': isCurrentUser(user)}"
                     class="py-2 pl-4 w-100 d-flex align-center justify-space-between border border-sm border-s-0 border-e-0">
                  <div class="d-flex align-center gap-2">
                    <img :src="maleProfile" alt="male-profile" class="obj-cover rounded-circle" height="48px"
                         width="48px">
                    <v-list-item-title>
                      <span class="textPrimary text-h6">{{ isCurrentUser(user) ? 'Tú' : user.username }}</span>
                    </v-list-item-title>
                  </div>
                </div>
              </v-list-item>
            </perfect-scrollbar>
          </v-list>
        </v-window-item>
        <v-window-item>
          <h4 class="font-weight-medium ml-4 mt-2">
            Salas disponibles ({{ roomStore.roomList.length }})
          </h4>
          <v-list>
            <perfect-scrollbar>
              <v-list-item v-for="(room, index) in roomStore.roomList" :key="index" class="mx-0 pa-0">
                <div class="py-2 pl-4 w-100 d-flex align-center justify-space-between border border-sm border-s-0 border-e-0"
                     :class="{'border-b-0': index < roomStore.roomList.length - 1}"
                >
                  <v-list-item-title>
                    <div class="textPrimary">{{ room.name }}</div>
                    <span class="font-weight-bold text-13 text-secondary">
                      {{room?.activeUsers}} Online
                    </span>
                  </v-list-item-title>

                  <v-list-item :ripple="false">
                    <v-btn color="primary" elevation="10" icon size="30" varant="outlined" @click="enterRoom(room)">
                      <v-icon icon="mdi-login"></v-icon>
                    </v-btn>
                  </v-list-item>
                </div>
              </v-list-item>
            </perfect-scrollbar>
          </v-list>
        </v-window-item>
      </v-window>
    </template>

    <template v-slot:main-content>
      <v-container>
        <router-view v-slot="{Component, route}">
          <component :is="Component" :key="route.path"></component>
        </router-view>
      </v-container>
    </template>
  </app-base-card>
</template>

<style lang="scss" scoped>
@media (max-width: 1279px) {
  .v-card {
    position: unset;
  }
}

</style>