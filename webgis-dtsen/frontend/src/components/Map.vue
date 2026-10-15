<template>
  <div ref="mapContainer" class="leaflet-map"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useStore } from '../store/store'

const props = defineProps({
  mapMode: {
    type: String,
    default: 'rekomendasi'
  },
  mapLevel: {
    type: String,
    default: 'kabupaten'
  },
  mapRows: {
    type: Array,
    default: () => []
  },
  referenceLevel: {
    type: String,
    default: 'nasional'
  },
  clipToReference: {
    type: Boolean,
    default: false
  },
  selectedWilayah: {
    type: Object,
    default: null
  },
  preserveSelectedOnClick: {
    type: Boolean,
    default: false
  }
})

const store = useStore()
const mapContainer = ref(null)
const emit = defineEmits(['rendering-change'])

let map = null
let polygonLayer = null
let geoJsonCache = {}
let renderTimer = null
let lastRenderedLevel = ''
let canvasRenderer = null

const GEOJSON_CONFIG = {
  provinsi: {
    url: '/geojson/provinsi.geojson',
    kodeFields: ['kode_prov', 'kode_wilayah']
  },
  kabupaten: {
    url: '/geojson/kabupaten.geojson',
    kodeFields: ['kode_kab', 'kode_kabkot', 'kode_wilayah']
  },
  kecamatan: {
    url: '/geojson/kecamatan.geojson',
    kodeFields: ['kode_kec', 'kode_kecamatan', 'kode_wilayah']
  },
  desa: {
    url: '/geojson/desa.geojson',
    kodeFields: ['kode_desa', 'kode_kelurahan_desa', 'kode_wilayah']
  }
}

onMounted(async () => {
  await nextTick()

  map = L.map(mapContainer.value, {
    zoomControl: true,
    preferCanvas: true
  }).setView([-2.5, 118], 5)

  canvasRenderer = L.canvas({
    padding: 0.5
  })

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map)

  polygonLayer = L.layerGroup().addTo(map)

  await renderPolygons()

  setTimeout(() => {
    map.invalidateSize()
  }, 250)
})

watch(
  [
    () => props.mapMode,
    () => props.mapLevel,
    () => props.mapRows,
    () => props.referenceLevel,
    () => props.clipToReference,
    () => props.selectedWilayah?.kodeWilayah,
    () => props.selectedWilayah?.kodeProvinsi,
    () => props.selectedWilayah?.kodeKabkot,
    () => props.selectedWilayah?.kodeKecamatan,
    () => props.selectedWilayah?.kodeKelurahanDesa,
    () => store.concentrationData,
    () => store.selectedProgram,
    () => store.selectedDimension,
    () => store.concentrationType,
    () => store.isAreaLoading,
    () => store.isConcentrationLoading
  ],
  () => {
    scheduleRenderPolygons()
  },
  { deep: false }
)

watch(
  () => [
    store.desil.join(','),
    store.prioritas.join(','),
    store.asesmen.join(','),
    props.mapLevel
  ],
  () => {
    if (map) {
      map.closePopup()
    }
  }
)

function scheduleRenderPolygons() {
  if (!map || !polygonLayer) return

  if (renderTimer) {
    clearTimeout(renderTimer)
  }

  renderTimer = setTimeout(async () => {
    renderTimer = null
    await renderPolygons()
  }, 250)
}

function waitForAnimationFrame() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(resolve)
    })
  })
}

async function loadGeoJson(level) {
  const config = GEOJSON_CONFIG[level] || GEOJSON_CONFIG.kabupaten

  if (geoJsonCache[level]) {
    return geoJsonCache[level]
  }

  const response = await fetch(config.url)

  if (!response.ok) {
    throw new Error(`Gagal load GeoJSON ${level}`)
  }

  const data = await response.json()
  geoJsonCache[level] = data

  return data
}

function getActiveMapLevel() {
  return props.mapLevel || 'kabupaten'
}

function getActiveMapWilayahData() {
  return props.mapRows || []
}

function shouldClipToReference() {
  return (
    props.clipToReference &&
    props.referenceLevel !== 'nasional' &&
    props.selectedWilayah
  )
}

function getReferencePrefixFromWilayah() {
  const wilayah = props.selectedWilayah

  if (!wilayah) return ''

  if (props.referenceLevel === 'provinsi') {
    return normalizeKodeForMap(wilayah.kodeProvinsi || wilayah.kodeWilayah).slice(0, 2)
  }

  if (props.referenceLevel === 'kabupaten') {
    return normalizeKodeForMap(wilayah.kodeKabkot || wilayah.kodeWilayah).slice(0, 4)
  }

  if (props.referenceLevel === 'kecamatan') {
    return normalizeKodeForMap(wilayah.kodeKecamatan || wilayah.kodeWilayah).slice(0, 7)
  }

  return ''
}

function shouldShowWilayahByReference(wilayah) {
  if (!shouldClipToReference()) return true

  const prefix = getReferencePrefixFromWilayah()

  if (!prefix) return true

  const kode = normalizeKodeForMap(getKodeByLevel(wilayah))

  return kode.startsWith(prefix)
}

function normalizeKodeForMap(value) {
  return String(value || '')
    .trim()
    .replace(/\./g, '')
}

function isConcentrationMap() {
  return props.mapMode === 'konsentrasi'
}

function isRecommendationMap() {
  return props.mapMode === 'rekomendasi'
}

function filterGeoJsonByWilayah(geoJsonData, wilayahMap, kodeFields = []) {
  const features = Array.isArray(geoJsonData?.features)
    ? geoJsonData.features
    : []

  const filteredFeatures = features.filter((feature) => {
    return Boolean(getWilayahFromFeature(feature, wilayahMap, kodeFields))
  })

  return {
    ...geoJsonData,
    features: filteredFeatures
  }
}

async function renderPolygons() {
  if (!map || !polygonLayer) return

  const isConcentrationLoading =
    isConcentrationMap() && store.isConcentrationLoading

  if (store.isAreaLoading || isConcentrationLoading) {
    emit('rendering-change', true)
    map.closePopup()
    polygonLayer.clearLayers()
    return
  }

  emit('rendering-change', true)
  map.closePopup()

  const level = getActiveMapLevel()
  const config = GEOJSON_CONFIG[level] || GEOJSON_CONFIG.kabupaten

  const renderKey = JSON.stringify({
    level,
    referenceLevel: props.referenceLevel,
    clipToReference: props.clipToReference,
    selectedKode:
      props.selectedWilayah?.kodeWilayah ||
      props.selectedWilayah?.kodeKelurahanDesa ||
      props.selectedWilayah?.kodeKecamatan ||
      props.selectedWilayah?.kodeKabkot ||
      props.selectedWilayah?.kodeProvinsi ||
      ''
  })

  const shouldFitBounds = lastRenderedLevel !== renderKey

  try {
    await nextTick()

    const wilayahMap = buildWilayahMap()

    if (wilayahMap.size === 0) {
      emit('rendering-change', false)
      return
    }

    const geoJsonData = await loadGeoJson(level)

    const filteredGeoJsonData = filterGeoJsonByWilayah(
      geoJsonData,
      wilayahMap,
      config.kodeFields
    )

    if (!filteredGeoJsonData.features.length) {
      emit('rendering-change', false)
      return
    }

    const values = Array.from(wilayahMap.values()).map((wilayah) => getMapValue(wilayah))
    const maxValue = Math.max(...values, 0)

    const nextLayer = L.geoJSON(filteredGeoJsonData, {
      renderer: canvasRenderer,

      style: (feature) => {
        const wilayah = getWilayahFromFeature(feature, wilayahMap, config.kodeFields)
        const value = wilayah ? getMapValue(wilayah) : 0

        return {
          color: '#9a3412',
          weight: 0.8,
          fillColor: getColor(value),
          fillOpacity: isConcentrationMap()
            ? 0.75
            : value > 0 ? 0.75 : 0.15
        }
      },

      onEachFeature: (feature, layer) => {
        const wilayah = getWilayahFromFeature(feature, wilayahMap, config.kodeFields)
        const value = wilayah ? getMapValue(wilayah) : 0

        layer.on('click', (event) => {
          if (!wilayah) return

          if (!props.preserveSelectedOnClick) {
            store.setSelectedMapWilayah(wilayah)
          }

          L.popup({
            maxWidth: 280
          })
            .setLatLng(event.latlng)
            .setContent(buildPopupHtml(feature, wilayah, value))
            .openOn(map)
        })

        if (level === 'provinsi' || level === 'kabupaten') {
          layer.on('mouseover', () => {
            layer.setStyle({
              weight: 2,
              color: '#7c2d12'
            })
          })

          layer.on('mouseout', () => {
            layer.setStyle({
              weight: 0.8,
              color: '#9a3412'
            })
          })
        }
      }
    })

    polygonLayer.clearLayers()
    nextLayer.addTo(polygonLayer)

    if (shouldFitBounds) {
      try {
        map.fitBounds(nextLayer.getBounds(), {
          padding: [16, 16]
        })
      } catch (error) {
        console.warn(error)
      }
    }

    await waitForAnimationFrame()

    lastRenderedLevel = renderKey
  } catch (error) {
    console.error(error)
  } finally {
    emit('rendering-change', false)
  }
}

function buildWilayahMap() {
  const mapByKode = new Map()

  ;(getActiveMapWilayahData() || [])
    .filter((wilayah) => shouldShowWilayahByReference(wilayah))
    .forEach((wilayah) => {
      const kodeList = [
        wilayah.kodeWilayah,
        wilayah.kodeProvinsi,
        wilayah.kodeKabkot,
        wilayah.kodeKecamatan,
        wilayah.kodeKelurahanDesa,
        getKodeByLevel(wilayah)
      ]

    kodeList.forEach((kodeValue) => {
      const kode = normalizeKode(kodeValue)

      if (kode) {
        mapByKode.set(kode, wilayah)
      }
    })
  })

  return mapByKode
}

function getWilayahFromFeature(feature, wilayahMap, kodeFields = []) {
  const props = feature?.properties || {}

  for (const field of kodeFields) {
    const kode = normalizeKode(props[field])

    if (kode && wilayahMap.has(kode)) {
      return wilayahMap.get(kode)
    }
  }

  return null
}

function getKodeByLevel(wilayah) {
  const level = getActiveMapLevel()

  if (level === 'provinsi') return wilayah.kodeProvinsi
  if (level === 'kabupaten') return wilayah.kodeKabkot
  if (level === 'kecamatan') return wilayah.kodeKecamatan
  if (level === 'desa') return wilayah.kodeKelurahanDesa

  return wilayah.kodeWilayah
}

function normalizeKode(value) {
  return String(value || '')
    .trim()
    .replace(/\./g, '')
}

function getMapValue(wilayah) {
  if (!wilayah) return 0

  if (isRecommendationMap()) {
    return getRecommendationNeedPercent(wilayah)
  }

  if (isConcentrationMap()) {
    if (store.concentrationType === 'bantuan') {
      return store.lqProgram(wilayah, store.selectedProgram)
    }

    return store.lqDimension(wilayah, store.selectedDimension)
  }

  return Number(wilayah.totalRekomendasi || totalBantuan(wilayah) || 0)
}

function getRecommendationNeedPercent(wilayah) {
  const directValue = Number(wilayah?.persentaseKeluargaMembutuhkan || 0)

  if (directValue > 0) return directValue

  const totalKeluarga = Number(wilayah?.totalKeluarga || 0)
  const totalKeluargaWilayah = Number(wilayah?.totalKeluargaWilayah || 0)

  if (totalKeluargaWilayah === 0) return 0

  return (totalKeluarga / totalKeluargaWilayah) * 100
}

function getColor(value) {
  if (isConcentrationMap()) {
    if (value >= 1.25) return '#9a3412'
    if (value >= 1.05) return '#c2410c'
    if (value >= 0.95) return '#f97316'
    if (value >= 0.75) return '#fdba74'
    return '#ffedd5'
  }

  if (value === 0) return '#ffedd5'

  if (value >= 80) return '#9a3412'
  if (value >= 60) return '#c2410c'
  if (value >= 40) return '#f97316'
  if (value >= 20) return '#fdba74'
  return '#ffedd5'
}

function getRecommendationNeedClass(value) {
  if (value >= 80) return 'Sangat Tinggi'
  if (value >= 60) return 'Tinggi'
  if (value >= 40) return 'Sedang'
  if (value >= 20) return 'Rendah'
  return 'Sangat Rendah'
}

function getLqCategory(value) {
  if (value >= 1.25) return 'Tinggi'
  if (value >= 1.05) return 'Cukup Tinggi'
  if (value >= 0.95) return 'Sedang'
  if (value >= 0.75) return 'Cukup Rendah'
  return 'Rendah'
}

function formatPercent(value) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatNumber(value) {
  return new Intl.NumberFormat('id-ID').format(Number(value || 0))
}

function getConcentrationData(wilayah) {
  const kodeList = [
    wilayah?.kodeWilayah,
    wilayah?.kodeKelurahanDesa,
    wilayah?.kodeKecamatan,
    wilayah?.kodeKabkot,
    wilayah?.kodeProvinsi
  ]

  for (const kodeValue of kodeList) {
    const kode = normalizeKode(kodeValue)
    const match = store.concentrationByKode.get(kode)

    if (match) {
      return match
    }
  }

  return null
}

function getConcentrationCountLabel() {
  if (store.concentrationType === 'bantuan') {
    return `Rekomendasi ${getSelectedProgramLabel()}`
  }

  return `Deprivasi ${getSelectedDimensionLabel()}`
}

function getConcentrationTotalLabel() {
  if (store.concentrationType === 'bantuan') {
    return 'Total Rekomendasi'
  }

  return 'Total Deprivasi'
}

function getSelectedProgramLabel() {
  return store.programList.find((item) => item.key === store.selectedProgram)?.label || ''
}

function getSelectedDimensionLabel() {
  return store.dimensionList.find((item) => item.key === store.selectedDimension)?.label || ''
}

function buildPopupHtml(feature, wilayah, value) {
  const featureProps = feature?.properties || {}

  const nama = getDisplayName(wilayah, featureProps)
  const subLabel = getWilayahSubLabel(wilayah, featureProps)
  const needPercent = getRecommendationNeedPercent(wilayah)

  if (isConcentrationMap()) {
    const concentration = getConcentrationData(wilayah)

    return `
      <div style="min-width:230px">
        <strong>${nama}</strong><br/>
        <span style="color:#64748b">${subLabel}</span>
        <hr/>
        <div style="display:flex;justify-content:space-between;gap:12px">
          <span>Nilai LQ</span>
          <b>${formatMapValue(value)}</b>
        </div>
        <div style="display:flex;justify-content:space-between;gap:12px">
          <span>${getConcentrationCountLabel()}</span>
          <b>${formatNumber(concentration?.jumlahTerpilih || 0)}</b>
        </div>
        <div style="display:flex;justify-content:space-between;gap:12px">
          <span>${getConcentrationTotalLabel()}</span>
          <b>${formatNumber(concentration?.totalPembanding || 0)}</b>
        </div>
      </div>
    `
  }

  return `
    <div style="min-width:230px">
      <strong>${nama}</strong><br/>
      <span style="color:#64748b">${subLabel}</span>
      <hr/>
      <div style="display:flex;justify-content:space-between;gap:12px">
        <span>Keluarga Sasaran</span>
        <b>${formatMapValue(wilayah?.totalKeluarga || 0)}</b>
      </div>
      <div style="display:flex;justify-content:space-between;gap:12px">
        <span>Total Rekomendasi</span>
        <b>${formatMapValue(wilayah?.totalRekomendasi || value || 0)}</b>
      </div>
      <div style="display:flex;justify-content:space-between;gap:12px">
        <span>Proporsi Sasaran</span>
        <b>${formatPercent(needPercent)}</b>
      </div>
    </div>
  `
}

function getDisplayName(wilayah, featureProps = {}) {
  const level = getActiveMapLevel()

  if (level === 'provinsi') {
    return wilayah?.nama || wilayah?.namaWilayah || featureProps.nama_prov || '-'
  }

  if (level === 'kabupaten') {
    return wilayah?.nama || wilayah?.namaWilayah || featureProps.nama_kab || '-'
  }

  if (level === 'kecamatan') {
    return wilayah?.nama || wilayah?.namaWilayah || featureProps.nama_kec || '-'
  }

  if (level === 'desa') {
    return wilayah?.nama || wilayah?.namaWilayah || featureProps.nama_desa || '-'
  }

  return wilayah?.nama || wilayah?.namaWilayah || '-'
}

function getWilayahSubLabel(wilayah, featureProps = {}) {
  const level = getActiveMapLevel()

  const provinsi =
    wilayah?.provinsi ||
    wilayah?.namaProvinsi ||
    featureProps.nama_prov ||
    ''

  const kabupaten =
    wilayah?.kabupaten ||
    wilayah?.namaKabkot ||
    featureProps.nama_kab ||
    ''

  const kecamatan =
    wilayah?.kecamatan ||
    wilayah?.namaKecamatan ||
    featureProps.nama_kec ||
    ''

  if (level === 'provinsi') {
    return ''
  }

  if (level === 'kabupaten') {
    return provinsi
  }

  if (level === 'kecamatan') {
    return [kabupaten, provinsi]
      .filter((item) => item && item !== '-')
      .join(', ')
  }

  if (level === 'desa') {
    return [kecamatan, kabupaten]
      .filter((item) => item && item !== '-')
      .join(', ')
  }

  return ''
}

function formatMapValue(value) {
  if (isConcentrationMap()) {
    return Number(value || 0).toFixed(2)
  }

  return new Intl.NumberFormat('id-ID').format(Number(value || 0))
}

function totalBantuan(wilayah) {
  return Object.values(wilayah?.bantuan || {}).reduce((a, b) => {
    return a + Number(b || 0)
  }, 0)
}

onBeforeUnmount(() => {
  if (renderTimer) {
    clearTimeout(renderTimer)
    renderTimer = null
  }

  if (map) {
    map.remove()
    map = null
  }
})
</script>