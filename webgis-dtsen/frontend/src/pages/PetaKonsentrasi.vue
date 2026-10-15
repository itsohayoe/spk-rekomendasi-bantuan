<template>
  <section class="dashboard-page">
    <div class="map-area">
      <div class="page-heading">
        <div>
          <span class="eyebrow">Peta Konsentrasi</span>

          <h1>Peta Konsentrasi Deprivasi Multidimensi</h1>

        </div>

        <div class="period-box">
          <span>Metode</span>
          <strong>Location Quotient</strong>
        </div>
      </div>

      <div class="map-card">
        <div
          v-if="showMapLoading"
          class="map-loading-layer"
        >
          <div class="inline-spinner"></div>
        </div>

        <MapView
          map-mode="konsentrasi"
          :map-level="store.petaKonsentrasiLevel"
          :map-rows="store.petaKonsentrasiWilayahData"
          :reference-level="store.concentrationReference"
          :clip-to-reference="store.concentrationMapFollowReference"
          :selected-wilayah="store.selectedPetaKonsentrasiWilayah"
          @rendering-change="handleMapRenderingChange"
        />

        <MapLegend title="Konsentrasi Relatif" is-lq />
      </div>
    </div>

    <Panel />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useStore } from '../store/store'
import MapView from '../components/Map.vue'
import MapLegend from '../components/MapLegend.vue'
import Panel from '../components/Panel.vue'

const store = useStore()
const mapRendering = ref(false)
const lastConcentrationLevel = ref(store.wilayahLevel)
const isRestoringPage = ref(false)

const showMapLoading = computed(() => {
  return (
    store.isAreaLoading ||
    store.isConcentrationLoading ||
    store.isMapRendering
  )
})

function handleMapRenderingChange(value) {
  mapRendering.value = value
  store.setMapRendering(value)
}

watch(
  () => store.concentrationType,
  (value) => {
    localStorage.setItem('webgis_concentration_type', value)
  }
)

watch(
  () => store.selectedProgram,
  (value) => {
    localStorage.setItem('webgis_selected_program', value)
  }
)

watch(
  () => store.selectedDimension,
  (value) => {
    localStorage.setItem('webgis_selected_dimension', value)
  }
)

function buildConcentrationMapKey() {
  return JSON.stringify({
    level: store.petaKonsentrasiLevel,
    reference: store.concentrationReference,
    type: store.concentrationType,
    program: store.selectedProgram,
    dimension: store.selectedDimension
  })
}

async function loadConcentrationMapData({ force = false } = {}) {
  const currentLevel = store.petaKonsentrasiLevel
  const queryKey = buildConcentrationMapKey()

  const canUseCachedData =
    !force &&
    store.petaKonsentrasiWilayahData.length > 0 &&
    store.petaKonsentrasiLoadedLevel === currentLevel &&
    store.petaKonsentrasiMapKey === queryKey &&
    store.concentrationData.length > 0

  if (canUseCachedData) {
    lastConcentrationLevel.value = currentLevel
    return
  }

  await store.fetchAreas({
    force,
    target: 'peta_konsentrasi',
    wilayah: null,
    level: currentLevel
  })

  await store.fetchConcentration({
    force,
    level: currentLevel,
    reference: store.concentrationReference,
    program: store.selectedProgram,
    dimension: store.selectedDimension,
    type: store.concentrationType
  })

  store.petaKonsentrasiMapKey = queryKey
  lastConcentrationLevel.value = currentLevel
}

onMounted(async () => {
  isRestoringPage.value = true

  const savedLevel = localStorage.getItem('webgis_peta_konsentrasi_level')

  if (savedLevel) {
    store.setActiveMapLevel(savedLevel)
  }

  store.selectedMapWilayah = store.selectedPetaKonsentrasiWilayah
  lastConcentrationLevel.value = store.petaKonsentrasiLevel

  await loadConcentrationMapData({ force: false })

  isRestoringPage.value = false
})

watch(
  () => [
    store.activeMenu,
    store.wilayahLevel,
    store.concentrationReference,
    store.selectedProgram,
    store.selectedDimension,
    store.concentrationType
  ],
  async ([menu], [oldMenu] = []) => {
    if (menu !== 'peta_konsentrasi') return
    if (isRestoringPage.value) return

    const justReturnedToPage = oldMenu && oldMenu !== 'peta_konsentrasi'

    if (justReturnedToPage) {
      store.selectedMapWilayah = store.selectedPetaKonsentrasiWilayah
      lastConcentrationLevel.value = store.petaKonsentrasiLevel
      return
    }

    localStorage.setItem('webgis_peta_konsentrasi_level', store.petaKonsentrasiLevel)

    const levelChanged = lastConcentrationLevel.value !== store.petaKonsentrasiLevel

    if (levelChanged) {
      store.setSelectedMapWilayah(null)
    }

    await loadConcentrationMapData({ force: true })
  }
)
</script>