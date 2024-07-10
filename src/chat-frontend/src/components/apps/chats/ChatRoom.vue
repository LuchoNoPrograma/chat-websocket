<script lang="ts" setup>
import {useChatStore} from "@/stores/chatStore";
import {computed, onMounted, ref} from "vue";
import type {RoomType} from "@/types/model/RoomTypes";
import {helpers, required} from "@vuelidate/validators";
import {Cropper} from "vue-advanced-cropper";
import 'vue-advanced-cropper/dist/style.css'
import 'vue-advanced-cropper/dist/theme.bubble.scss';
import axiosServices from "@/utils/axios";
import type {TagType} from "@/types/model/TagTypes";
import useVuelidate from "@vuelidate/core";
import {useRouter} from "vue-router";
import type {Message} from "webstomp-client";
import type {ChatMessageType} from "@/types/apps/ChatMessageType";
import {useRoomStore} from "@/stores/roomStore";


const chatStore = useChatStore();
const roomStore = useRoomStore();
const router = useRouter();

const roomForm = ref<RoomType>({} as RoomType);
const schemaRules = computed(() => ({
  name: {
    rule1: helpers.withMessage("El nombre es requerido", required)
  },
  description: {
    rule1: helpers.withMessage("La descripción es requerida", required),
  },
  tags: {
    rule1: helpers.withMessage("Las etiquetas son requeridas", required)
  },

}))
//@ts-ignore
const rules = useVuelidate(schemaRules, roomForm.value);

const dialogForm = ref<boolean>(false);
const dialogFormImage = ref<boolean>(false);

const onSelectImage = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const reader = new FileReader();
    reader.onload = () => {
      roomForm.value.pathPortrait = reader.result;
      /* console.log(roomForm.value.pathPortrait);*/
    }
    reader.readAsDataURL(input.files[0]);
  }

  dialogFormImage.value = !dialogFormImage.value

}

type CoordinatesType = {
  width: number,
  height: number,
  left: number,
  top: number
}

type CanvaType = {
  coordinates: any,
  canvas: any
}

const coordinates = ref<CoordinatesType>({
  width: 0,
  height: 0,
  left: 0,
  top: 0
});
const onChangeCrop = (data: CanvaType) => {
  coordinates.value = data.coordinates;
  roomForm.value.imgPreview = new Image();
  roomForm.value.imgPreview.src = data.canvas.toDataURL();
}

const processImage = () => {
  const canvas = document.createElement("canvas");
  canvas.width = roomForm.value.imgPreview?.width as number;
  canvas.height = roomForm.value.imgPreview?.height as number;
  const ctx = canvas.getContext("2d");
  //@ts-ignore
  ctx.drawImage(roomForm.value.imgPreview, 0, 0);
  const dataURL = canvas.toDataURL();
}

const tagList = ref<TagType[]>([]);
onMounted(async () => {
  const responseTag = await axiosServices<TagType[]>("/api/v1/tag");
  tagList.value = responseTag.data;
})

const createRoom = () => {
  roomStore.createRoom(roomForm.value);
}

const enterRoom = async (room: RoomType) => {
  await router.push(`/room/${room.id}`)//ChatDetail.vue
}
</script>

<template>
  <div>
    <v-container>
      <h3 class="font-weight-medium mb-2">Salas disponibles</h3>
      <v-btn append-icon="mdi-account-group" variant="tonal" @click="dialogForm = !dialogForm">Crear sala</v-btn>
    </v-container>

    <v-container>
      <v-row>
        <v-col v-for="room in chatStore.getRoomList()" :key="room.id" cols="12" lg="4" md="6" sm="6"
               class="chat-room__item">
          <v-card elevation="2">
            <template v-slot:default>
              <div class="pa-4">
                <h4 class="text-h4 font-weight-semibold">{{ room.name }}</h4>
                <!--                <p class="text-subtitle-1 my-1">{{room.description}}</p>-->
                <div class="mt-2">
                  <v-chip v-for="tag in room.tags" :key="tag.id" density="compact" class="mr-2">
                    <span class="mr-1">{{ tag.name }}</span>
                    <v-icon :icon="tag.pathIcon"></v-icon>
                  </v-chip>
                </div>
              </div>
              <img :src="room.imgPortrait" alt="1">
              <div class="pa-4">
                <v-btn variant="outlined" prepend-icon="mdi-login" @click="enterRoom(room)">Unirse</v-btn>
              </div>
            </template>
          </v-card>
        </v-col>
      </v-row>
    </v-container>


    <v-dialog v-model="dialogForm" max-width="720">
      <template v-slot:default="{isActive}">
        <v-card class="text-center">
          <div class="bg-primary pa-2 text-center">
            Crear nueva sala
          </div>

          <form class="form-room my-4 px-4" @submit.prevent="createRoom">
            <label :class="{'border-dotted--primary': !roomForm.imgPreview?.src}"
                   :style="{background: 'url(' + roomForm?.imgPreview?.src + ') no-repeat center'}"
                   class="mb-4 mx-auto d-flex flex-column justify-center"
                   for="form-room__portrait-preview"
            >
              <div class="text-primary pa-8 ">
                <div v-if="!roomForm.pathPortrait">
                  <v-icon icon="mdi-plus" size="48"></v-icon>
                </div>
                <p v-if="!roomForm.pathPortrait">
                  Portada de la sala
                </p>
                <input id="form-room__portrait-preview" accept="image" name="roomCover" style="display: none;"
                       type="file" @change="onSelectImage">
              </div>
            </label>
            <!--            <v-input :error-messages="rules.imgFile."></v-input>-->

            <v-row class="w-100">
              <v-col cols="12">
                <v-text-field v-model="roomForm.name"
                              :error-messages="rules.name.$errors.map(e => e.$message as string)" class="w-100 mb-4"
                              clearable counter
                              label="Nombre de la sala"
                              maxlength="30"
                              persistent-counter
                              @blur="rules.name.$touch"
                ></v-text-field>
                <v-textarea v-model="roomForm.description"
                            :error-messages="rules.description.$errors.map(e => e.$message as string)" class="mb-4"
                            clearable counter
                            label="Descripción"
                            maxlength="255"
                            persistent-counter
                            rows="2"
                            @blur="rules.description.$touch"
                ></v-textarea>
                <v-autocomplete v-model="roomForm.tags"
                                :error-messages="rules.tags.$errors.map(e => e.$message as string)"
                                :item-title="(item: TagType) => item.name"
                                :items="tagList"
                                class="mb-4"
                                closable-chips
                                hint="Escoge una o mas etiquetas para tu sala"
                                label="Etiquetas"
                                multiple
                                persistent-hint
                                return-object
                                @blur="rules.tags.$touch"
                >
                  <template v-slot:item="{ props , item}">
                    <v-list-item :title="item.raw.name" class="d-flex" v-bind="props">
                      <v-icon :icon="item.raw.pathIcon" class="mr-2"></v-icon>
                      <!--                      <span class="font-weight-medium">{{item.raw.name}}</span>-->
                    </v-list-item>
                  </template>
                  <template v-slot:chip="{ props, item }">
                    <v-chip
                      color="primary"
                      v-bind="props"
                      variant="tonal"
                    >
                      <span class="mr-2">
                      <v-icon :icon="item.raw.pathIcon"></v-icon>
                    </span>
                      <span class="mr-1">{{ item.raw.name }}</span>
                    </v-chip>
                  </template>
                </v-autocomplete>
              </v-col>
            </v-row>

            <v-btn append-icon="mdi-plus" color="primary" type="submit">Crear sala</v-btn>
          </form>
        </v-card>
      </template>
    </v-dialog>
    <v-dialog v-model="dialogFormImage" :eager="true" width="auto">
      <cropper
        id="room-foor__image-preview"
        key="cropper"
        ref="cropper"
        :auto-zoom="true"
        :src="roomForm.pathPortrait"
        :stencil-size="{
		width: 260,
		height: 180
	}"
        class="cropper"
        image-class="image-preview"
        image-restriction="stencil"
        style="width: 360px; height: 480px;"
        @change="onChangeCrop"
      >

      </cropper>
      <div class="mx-auto">
        <v-btn color="primary" prepend-icon="mdi-crop" variant="elevated">Cortar</v-btn>
        <v-btn color="error" variant="elevated" @click="dialogFormImage = !dialogFormImage">Cancelar</v-btn>
      </div>
    </v-dialog>
  </div>
</template>

<style lang="scss" scoped>
@import 'src/scss/variables';

.border-dotted--primary {
  border: 2px dashed $primary !important;
}

.form-room {
  text-align: center;
}

.form-room__portrait {
  cursor: pointer;
}

label[for="form-room__portrait-preview"] {
  display: inline-block;
  background-size: contain !important;
  background-position: center;
  background-repeat: no-repeat;
  width: 260px;
  height: 180px;
  border-radius: 0.5em;
}

.cropper {
  border: 2px dashed #000;
}

.chat-room__item {
  img {
    width: 100%;
    height: 100%;
  }
}
</style>