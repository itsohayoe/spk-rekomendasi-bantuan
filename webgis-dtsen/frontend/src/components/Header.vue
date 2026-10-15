<template>
  <header class="app-header">
    <div class="brand">
      <img
        src="../assets/logo-bps.png"
        alt="Logo BPS"
        class="brand-logo"
      />

      <div class="brand-text">
        <h2>WebGIS DTSEN</h2>
        <p>Dashboard rekomendasi bantuan</p>
      </div>
    </div>

    <nav class="header-nav">
      <button
        type="button"
        :class="{ active: store.activeMenu === 'beranda' }"
        @click="goToMenu('beranda')"
      >
        Beranda
      </button>

      <button
        type="button"
        :class="{ active: store.activeMenu === 'peta_konsentrasi' }"
        @click="goToMenu('peta_konsentrasi')"
      >
        Peta Konsentrasi
      </button>

      <button
        type="button"
        :class="{ active: store.activeMenu === 'peta_rekomendasi' }"
        @click="goToMenu('peta_rekomendasi')"
      >
        Peta Rekomendasi
      </button>

      <button
        type="button"
        :class="{ active: store.activeMenu === 'tabel_rekomendasi' }"
        @click="goToMenu('tabel_rekomendasi')"
      >
        Tabel Rekomendasi
      </button>
    </nav>
  </header>
</template>

<script setup>
import { useStore } from '../store/store'

const store = useStore()

function goToMenu(menu) {
  store.setMenu(menu)
}
</script>