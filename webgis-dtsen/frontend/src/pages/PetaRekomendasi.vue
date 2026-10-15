<template>
  <section class="dashboard-page">
    <div class="map-area">
      <div class="page-heading">
        <div>
          <span class="eyebrow">Peta Rekomendasi</span>

          <h1>Peta Rekomendasi Bantuan Sosial</h1>

        </div>

        <div class="period-box">
          <span>Data</span>
          <strong>Agregat Wilayah</strong>
        </div>
      </div>

      <div class="map-card">
        <div v-if="showMapLoading" class="map-loading-layer">
          <div class="inline-spinner"></div>
        </div>

        <MapView
          map-mode="rekomendasi"
          :map-level="effectiveRecommendationMapLevel"
          :map-rows="visibleRecommendationMapRows"
          :preserve-selected-on-click="recommendationDrilldownActive"
          @rendering-change="handleMapRenderingChange"
        />

        <MapLegend title="Tingkat Kebutuhan" />
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

const lastLoadedLevel = ref(store.wilayahLevel)
const lastRecommendationMapKey = ref('')
const isRestoringPage = ref(false)
const drilldownParentWilayah = ref(null)

const recommendationDrilldownActive = computed(() => {
  return (
    store.recommendationMapFollowReference &&
    drilldownParentWilayah.value &&
    store.recommendationMapReference
  )
})

const effectiveRecommendationMapLevel = computed(() => {
  if (recommendationDrilldownActive.value) {
    return store.recommendationMapReference
  }

  return store.petaRekomendasiLevel
})

const effectiveRecommendationParentWilayah = computed(() => {
  if (recommendationDrilldownActive.value) {
    return drilldownParentWilayah.value
  }

  return null
})

const visibleRecommendationMapRows = computed(() => {
  const rows = store.petaRekomendasiWilayahData || []
  const keyword = store.searchKeyword.trim().toLowerCase()

  if (!keyword) return rows

  return rows.filter((item) => {
    return (
      safeLower(item.nama).includes(keyword) ||
      safeLower(item.namaWilayah).includes(keyword) ||
      safeLower(item.provinsi).includes(keyword) ||
      safeLower(item.kabupaten).includes(keyword) ||
      safeLower(item.kecamatan).includes(keyword) ||
      safeLower(item.desa).includes(keyword) ||
      safeLower(item.kodeWilayah).includes(keyword) ||
      safeLower(item.kodeProvinsi).includes(keyword) ||
      safeLower(item.kodeKabkot).includes(keyword) ||
      safeLower(item.kodeKecamatan).includes(keyword) ||
      safeLower(item.kodeKelurahanDesa).includes(keyword)
    )
  })
})

const showMapLoading = computed(() => {
  return (
    store.isAreaLoading ||
    store.isMapRendering ||
    store.isMapPanelLoading
  )
})

function handleMapRenderingChange(value) {
  mapRendering.value = value
  store.setMapRendering(value)
}

function setDrilldownParentFromSelected() {
  if (!store.selectedPetaRekomendasiWilayah) return

  drilldownParentWilayah.value = store.selectedPetaRekomendasiWilayah
}

function keepPanelOnDrilldownParent() {
  if (!recommendationDrilldownActive.value) return
  if (!drilldownParentWilayah.value) return

  store.selectedMapWilayah = drilldownParentWilayah.value
}

async function loadRecommendationMapData({ force = false } = {}) {
  const currentLevel = effectiveRecommendationMapLevel.value
  const parentWilayah = effectiveRecommendationParentWilayah.value
  const levelChanged = lastLoadedLevel.value !== currentLevel

  const queryKey = buildRecommendationMapKey()

  const previousSelected = store.selectedMapWilayah
  const previousSelectedCode = getSelectedWilayahCode(previousSelected, currentLevel)

  if (levelChanged && !previousSelectedCode && !recommendationDrilldownActive.value) {
    store.setSelectedMapWilayah(null)
  }

  const canUseCachedData =
    !force &&
    store.areaLoaded &&
    store.areaLoadedLevel === currentLevel &&
    store.petaRekomendasiWilayahData.length > 0 &&
    store.petaRekomendasiMapKey === queryKey

  if (canUseCachedData) {
    lastLoadedLevel.value = currentLevel

    if (recommendationDrilldownActive.value) {
      keepPanelOnDrilldownParent()
      return
    }

    syncSelectedWilayahAfterFilter(previousSelectedCode, currentLevel, true)

    return
  }

  await store.fetchAreas({
    force,
    target: 'peta_rekomendasi',
    wilayah: parentWilayah,
    level: currentLevel,
    desil: store.desil,
    prioritas: store.prioritas,
    asesmen: store.asesmen,
    syncLevelState: !recommendationDrilldownActive.value
  })

  lastRecommendationMapKey.value = queryKey
  store.petaRekomendasiMapKey = queryKey
  lastLoadedLevel.value = currentLevel

  if (recommendationDrilldownActive.value) {
    keepPanelOnDrilldownParent()
    return
  }

  syncSelectedWilayahAfterFilter(previousSelectedCode, currentLevel, true)
}

function buildRecommendationMapKey() {
  const parentWilayah = effectiveRecommendationParentWilayah.value

  return JSON.stringify({
    level: effectiveRecommendationMapLevel.value,
    drilldownActive: store.recommendationMapFollowReference,
    drilldownLevel: store.recommendationMapReference,
    parentKode:
      parentWilayah?.kodeWilayah ||
      parentWilayah?.kodeKelurahanDesa ||
      parentWilayah?.kodeKecamatan ||
      parentWilayah?.kodeKabkot ||
      parentWilayah?.kodeProvinsi ||
      '',
    desil: [...store.desil].sort(),
    prioritas: [...store.prioritas].sort(),
    asesmen: [...store.asesmen].sort()
  })
}

function getSelectedWilayahCode(wilayah, level = store.wilayahLevel) {
  if (!wilayah) return ''

  if (level === 'provinsi') {
    return wilayah.kodeProvinsi || wilayah.kodeWilayah || ''
  }

  if (level === 'kabupaten') {
    return wilayah.kodeKabkot || wilayah.kodeWilayah || ''
  }

  if (level === 'kecamatan') {
    return wilayah.kodeKecamatan || wilayah.kodeWilayah || ''
  }

  if (level === 'desa') {
    return wilayah.kodeKelurahanDesa || wilayah.kodeWilayah || ''
  }

  return wilayah.kodeWilayah || ''
}

function syncSelectedWilayahAfterFilter(
  previousSelectedCode,
  level = store.wilayahLevel,
  clearIfNotFound = false
) {
  if (!previousSelectedCode) {
    if (clearIfNotFound) {
      store.setSelectedMapWilayah(null)
    }

    return
  }

  const normalizedPreviousCode = normalizeKode(previousSelectedCode)

  const updatedSelected = store.petaRekomendasiWilayahData.find((wilayah) => {
    return normalizeKode(getSelectedWilayahCode(wilayah, level)) === normalizedPreviousCode
  })

  if (updatedSelected) {
    store.setSelectedMapWilayah(updatedSelected)
    return
  }

  if (clearIfNotFound) {
    store.setSelectedMapWilayah(null)
  }
}

function normalizeKode(value) {
  return String(value || '')
    .trim()
    .replace(/\./g, '')
}

function safeLower(value) {
  return String(value || '').toLowerCase()
}

function getWilayahLevel(wilayah) {
  if (wilayah?.level) return wilayah.level

  if (wilayah?.kodeKelurahanDesa) return 'desa'
  if (wilayah?.kodeKecamatan) return 'kecamatan'
  if (wilayah?.kodeKabkot) return 'kabupaten'
  if (wilayah?.kodeProvinsi) return 'provinsi'

  return ''
}

onMounted(async () => {
  isRestoringPage.value = true

  const savedLevel = localStorage.getItem('webgis_peta_rekomendasi_level')

  if (savedLevel) {
    store.setActiveMapLevel(savedLevel)
  }

  store.selectedMapWilayah = store.selectedPetaRekomendasiWilayah
  lastLoadedLevel.value = store.wilayahLevel

  await loadRecommendationMapData()

  isRestoringPage.value = false
})

watch(
  () => store.recommendationMapFollowReference,
  (active) => {
    if (active) {
      setDrilldownParentFromSelected()
      keepPanelOnDrilldownParent()
    } else {
      drilldownParentWilayah.value = null
    }

    loadRecommendationMapData({ force: true })
  }
)

watch(
  () => store.selectedPetaRekomendasiWilayah,
  (wilayah) => {
    if (
      store.activeMenu !== 'peta_rekomendasi' ||
      !store.recommendationMapFollowReference ||
      !wilayah
    ) {
      return
    }

    const selectedLevel = getWilayahLevel(wilayah)

    if (selectedLevel !== store.petaRekomendasiLevel) {
      keepPanelOnDrilldownParent()
      return
    }

    drilldownParentWilayah.value = wilayah
    keepPanelOnDrilldownParent()
    loadRecommendationMapData({ force: true })
  }
)

watch(
  () => [
    store.activeMenu,
    store.wilayahLevel,
    store.recommendationMapReference,
    store.searchKeyword,
    store.desil.join(','),
    store.prioritas.join(','),
    store.asesmen.join(',')
  ],
  ([menu], [oldMenu] = []) => {
    if (menu !== 'peta_rekomendasi') return
    if (isRestoringPage.value) return

    const justReturnedToPage = oldMenu && oldMenu !== 'peta_rekomendasi'

    if (justReturnedToPage) {
      store.selectedMapWilayah = store.selectedPetaRekomendasiWilayah
      lastLoadedLevel.value = store.petaRekomendasiLevel
      return
    }

    localStorage.setItem('webgis_peta_rekomendasi_level', store.wilayahLevel)

    const levelChanged = lastLoadedLevel.value !== effectiveRecommendationMapLevel.value

    if (levelChanged && !recommendationDrilldownActive.value) {
      store.setSelectedMapWilayah(null)
    }

    loadRecommendationMapData({ force: true })
  }
)
</script>