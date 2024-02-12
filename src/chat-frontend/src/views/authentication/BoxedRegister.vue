<script lang="ts" setup>
import Logo from '@/layouts/full/logo/LogoDark.vue';
import RegisterForm from '@/components/auth/RegisterForm.vue';
</script>
<template>
    <div class="authentication">
        <v-container class="pa-3" fluid>
            <v-row class="h-100vh d-flex justify-center align-center">
                <v-col class="d-flex align-center" cols="12">
                    <v-card class="px-sm-1 px-0 mx-auto" elevation="10" max-width="450" rounded="md">
                        <v-card-item class="pa-sm-8">
                            <div class="d-flex justify-center py-4 mb-5">
                              <logo></logo>
                            </div>
                            <register-form></register-form>
                        </v-card-item>
                    </v-card>
                </v-col>
            </v-row>
        </v-container>
    </div>
</template>