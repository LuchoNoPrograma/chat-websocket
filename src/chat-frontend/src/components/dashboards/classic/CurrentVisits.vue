<!-- <script setup lang="ts">
import { onMounted, ref } from "vue";
import svgMap from "svgmap";
import "svgmap/dist/svgMap.min.css";
import { getPrimary, getSecondary } from '@/utils/UpdateColors';
const map = ref(null);
onMounted(() => {
    new svgMap({
        targetElementID: "svgMap",
        mouseWheelZoomEnabled: false,
        hideFlag: true,
        initialZoom: 1.23,

        data: {
            data: {
                countresidents: {
                    format: "{0}",
                    thousandSeparator: ",",
                    thresholdMax: 50000,
                    thresholdMin: 1000,
                },
            },
            applyData: "countresidents",
            values: {
                AF: { countresidents: 587, color: "#6352ce" },
                IN: { countresidents: 232, color: getSecondary.value },
                US: { countresidents: 42393, color: getPrimary.value },
            },
        },
    });
});
</script> -->
<template>
    <VCard elevation="10">
        <v-card-text class="position-relative">
            <div>
                <h3 class="text-h5 title mb-1">Current Visits</h3>
                <h5 class="text-subtitle-1">Visit from the countries</h5>
            </div>
            <div id="svgMap" ref="map" class="mt-8"></div>

            <div class="d-flex align-center justify-space-between mt-8 pt-5">
                <div class="d-flex align-center px-2">
                    <span class="text-overline">
                        <i class="mdi mdi-brightness-1 text-secondary mx-1"></i>
                    </span>
                    <span class="text-grey-darken-1 font-weight-medium mr-1">India</span>
                </div>
                <div class="d-flex align-center px-2">
                    <span class="text-overline">
                        <i class="mdi mdi-brightness-1 text-primary mx-1"></i>
                    </span>
                    <span class="text-grey-darken-1 font-weight-medium mr-1">USA</span>
                </div>
                <div class="d-flex align-center px-2">
                    <span class="text-overline">
                        <i class="mdi mdi-brightness-1 mx-1"></i>
                    </span>
                    <span class="text-grey-darken-1 font-weight-medium mr-1">Angola
                    </span>
                </div>
            </div>
        </v-card-text>
    </VCard>
</template>
<script setup>
import svgMap from "svgmap";
import "svgmap/dist/svgMap.min.css";
import { onMounted, ref } from "vue";

const map = ref(null);

onMounted(() => {
  new svgMap({
    targetElementID: "svgMap",
    mouseWheelZoomEnabled: false,
    hideFlag: true,
    initialZoom: 1.23,

    data: {
      data: {
        countresidents: {
          // name: "count residents",
          format: "{0}",
          thousandSeparator: ",",
          thresholdMax: 50000,
          thresholdMin: 1000,
        },

        // change: {
        //   name: "Change to year before",
        //   format: "{0} %",
        // },
      },
      applyData: "countresidents",
      values: {
        AF: { countresidents: 587, color: "#6352ce" },
        //AL: { gdp: 4583, change: 11.09 },
        IN: { countresidents: 232, color: "#0cb9c5" },
        US: { countresidents: 42393, color: "#1e88e5" },
      },
    },
  });
});
</script>

<style lang="scss">
.svgMap-map-wrapper {
  background: transparent;
}
</style>
  