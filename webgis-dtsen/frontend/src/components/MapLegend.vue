<template>
  <div class="legend-card">
    <h4>{{ title }}</h4>

    <template v-if="isLq">
      <div class="legend-row">
        <span class="legend-color color-lq-1"></span>
        <p>Rendah</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-lq-2"></span>
        <p>Cukup Rendah</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-lq-3"></span>
        <p>Sedang</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-lq-4"></span>
        <p>Cukup Tinggi</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-lq-5"></span>
        <p>Tinggi</p>
      </div>
    </template>

    <template v-else>
      <div class="legend-row">
        <span class="legend-color color-1"></span>
        <p>Sangat Rendah</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-2"></span>
        <p>Rendah</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-3"></span>
        <p>Sedang</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-4"></span>
        <p>Tinggi</p>
      </div>

      <div class="legend-row">
        <span class="legend-color color-5"></span>
        <p>Sangat Tinggi</p>
      </div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: 'Legenda'
  },
  isLq: {
    type: Boolean,
    default: false
  }
})
</script>