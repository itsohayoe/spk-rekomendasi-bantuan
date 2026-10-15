<template>
  <div class="app-shell">
    <Header />

    <main class="page-content">
      <KeepAlive>
        <Beranda v-if="store.activeMenu === 'beranda'" />

        <PetaKonsentrasi v-else-if="store.activeMenu === 'peta_konsentrasi'" />

        <PetaRekomendasi v-else-if="store.activeMenu === 'peta_rekomendasi'" />

        <TabelRekomendasi v-else-if="store.activeMenu === 'tabel_rekomendasi'" />
      </KeepAlive>
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useStore } from './store/store'
import Header from './components/Header.vue'
import Beranda from './pages/Beranda.vue'
import PetaKonsentrasi from './pages/PetaKonsentrasi.vue'
import PetaRekomendasi from './pages/PetaRekomendasi.vue'
import TabelRekomendasi from './pages/TabelRekomendasi.vue'

const store = useStore()

onMounted(async () => {
  await store.fetchSummary()
})
</script>