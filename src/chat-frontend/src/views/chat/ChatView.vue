<script lang="ts" setup>
// common components
import AppBaseCard from '@/components/shared/AppBaseCard.vue';
import ChatListing from '@/components/apps/chats/ChatListing.vue';
import ChatProfile from '@/components/apps/chats/ChatProfile.vue';
import {useChatStore} from "@/stores/apps/chatStore";
import {useRouter} from "vue-router";
import ChatHeader from "@/components/apps/chats/ChatHeader.vue";

const chatStore = useChatStore();
const router = useRouter();


const logout = async () => {
  chatStore.disconnect();
  await router.push('/auth/register')
}
</script>

<template>
  <app-base-card>
    <template v-slot:top>
      <chat-header></chat-header>
    </template>
    <template v-slot:leftpart>
      <v-card class="d-flex flex-column justify-space-between" style="height: 80vh">
        <v-container>
          <h4 class="font-weight-medium">
            Usuarios en linea ({{ chatStore.getUserOnlineList().length }})
          </h4>

          <div>
            <v-list style="background-color: transparent !important;">
              <perfect-scrollbar class="h-100">
                <v-list-item v-for="(user, index) in chatStore.getUserOnlineList()" :key="index"
                             class="text-white">
                  <div class="py-2 pl-4 w-100 d-flex align-center justify-space-between border border-md">
                    <v-list-item-title>
                      <span class="textPrimary">{{user.username}}</span>
                    </v-list-item-title>

                    <v-list-item :ripple="false">
                      <v-btn icon size="30" varant="outlined" color="primary" elevation="10">
                        <v-icon icon="mdi-email-fast"></v-icon>
                      </v-btn>
                    </v-list-item>
                  </div>
                </v-list-item>
              </perfect-scrollbar>
            </v-list>
          </div>
        </v-container>
      </v-card>
    </template>

    <template v-slot:rightpart>
      <router-view>
      </router-view>
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