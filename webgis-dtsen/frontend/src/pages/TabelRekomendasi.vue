<template>
  <section class="page-section">
    <div class="page-heading">
      <div>
        <span class="eyebrow">Tabel Rekomendasi</span>

        <h1>Tabel Rekomendasi Bantuan Sosial</h1>

      </div>

      <div class="period-box">
        <span>Periode Aktif</span>
        <strong>2025 Triwulan III</strong>
      </div>
    </div>

    <div class="table-mode-card">
      <div class="table-mode-title">
        <span class="eyebrow">Jenis Tabel</span>

        <div class="table-title-row">
          <h2>{{ store.tableMode === 'wilayah' ? 'Level Wilayah' : 'Level Keluarga' }}</h2>

          <span
            v-if="store.tableMode === 'wilayah' && tableFilterActive"
            class="active-filter-badge"
          >
            Filter aktif
          </span>

          <span
            v-if="store.tableMode === 'keluarga' && familyFilterActive"
            class="active-filter-badge"
          >
            Filter aktif
          </span>
        </div>
      </div>

      <div class="table-tabs">
        <button
          :class="{ active: store.tableMode === 'wilayah' }"
          @click="showWilayahTable"
        >
          Wilayah
        </button>

        <button
          :class="{ active: store.tableMode === 'keluarga' }"
          @click="showFamilyTable"
        >
          Keluarga
        </button>
      </div>
    </div>

    <template v-if="store.tableMode === 'wilayah'">
      <TabelWilayah
        v-model:wilayah-level="tableWilayahLevel"
        v-model:search="tableSearch"
        v-model:desil-filter="tableDesilFilter"
        v-model:prioritas-filter="tablePrioritasFilter"
        v-model:asesmen-filter="tableAsesmenFilter"
        v-model:dropdown="tableDropdown"
        :rows="pagedWilayahRows"
        :wilayah-column-label="wilayahColumnLabel"
        :prioritas-list="wilayahPrioritasList"
        :asesmen-list="wilayahAsesmenList"
        :is-loading="wilayahTableLoading"
        :is-searching="isSearching"
        :has-active-filter="wilayahResetVisible"
        @reset="resetWilayahTableFilter"
        @open-family-detail="openFamilyDetailFromTable"
      />

      <div class="table-pagination-strip">
        <button
          class="pagination-icon-button pagination-first"
          :disabled="wilayahPage === 1 || filteredWilayahTable.length === 0 || store.isTableAreaLoading"
          title="Halaman pertama"
          @click="firstWilayahPage"
        >
          <span></span><span></span>
        </button>

        <button
          class="pagination-icon-button pagination-prev"
          :disabled="wilayahPage === 1 || filteredWilayahTable.length === 0 || store.isTableAreaLoading"
          title="Sebelumnya"
          @click="previousWilayahPage"
        >
          <span></span>
        </button>

        <span v-if="filteredWilayahTable.length > 0" class="pagination-strip-info">
          {{ formatNumber(wilayahStartIndex + 1) }}
          –
          {{ formatNumber(wilayahEndIndex) }}
          dari
          {{ formatNumber(filteredWilayahTable.length) }}
          baris
        </span>

        <span v-else class="pagination-strip-info">
          Belum ada data
        </span>

        <button
          class="pagination-icon-button pagination-next"
          :disabled="isLastWilayahPage || filteredWilayahTable.length === 0 || store.isTableAreaLoading"
          title="Berikutnya"
          @click="nextWilayahPage"
        >
          <span></span>
        </button>

        <button
          class="pagination-icon-button pagination-last"
          :disabled="isLastWilayahPage || filteredWilayahTable.length === 0 || store.isTableAreaLoading"
          title="Halaman terakhir"
          @click="lastWilayahPage"
        >
          <span></span><span></span>
        </button>
      </div>
    </template>

    <template v-else>
      <TabelKeluarga
        v-model:wilayah-level="familyWilayahLevel"
        v-model:keyword="activeFamilyWilayahKeyword"
        v-model:program-filter="familyProgramFilter"
        v-model:desil-filter="familyDesilFilter"
        v-model:prioritas-filter="familyPrioritasFilter"
        v-model:asesmen-filter="familyAsesmenFilter"
        v-model:status-program-filter="familyStatusProgramFilter"
        v-model:status-data-filter="familyStatusDataFilter"
        v-model:dropdown="tableDropdown"
        :rows="pagedFamilyProgramRows"
        :family-wilayah-column-label="familyWilayahColumnLabel"
        :program-list="store.programList"
        :prioritas-list="prioritasList"
        :asesmen-list="asesmenList"
        :status-program-list="statusProgramList"
        :status-data-list="statusDataList"
        :is-loading="store.isRecommendationLoading || isSearching || !familyHasLoaded"
        :is-searching="isSearching"
        :has-active-filter="familyFilterActive"
        @reset="resetFamilyTableFilter"
      />

      <div class="table-pagination-strip">
        <button
          class="pagination-icon-button pagination-first"
          :disabled="isFirstRecommendationPage || store.isRecommendationLoading || familyProgramRows.length === 0"
          title="Halaman pertama"
          @click="firstRecommendationPage"
        >
          <span></span><span></span>
        </button>

        <button
          class="pagination-icon-button pagination-prev"
          :disabled="isFirstRecommendationPage || store.isRecommendationLoading || familyProgramRows.length === 0"
          title="Sebelumnya"
          @click="previousRecommendationPage"
        >
          <span></span>
        </button>

        <span v-if="pagedFamilyProgramRows.length > 0" class="pagination-strip-info">
          {{ formatNumber(store.recommendationOffset + 1) }}
          –
          {{ formatNumber(store.recommendationOffset + pagedFamilyProgramRows.length) }}
          dari
          {{ formatNumber(store.recommendationTotalRows) }}
          baris
        </span>

        <span v-else class="pagination-strip-info">
          Belum ada data
        </span>

        <button
          class="pagination-icon-button pagination-next"
          :disabled="isLastRecommendationPage || store.isRecommendationLoading || familyProgramRows.length === 0"
          title="Berikutnya"
          @click="nextRecommendationPage"
        >
          <span></span>
        </button>

        <button
          class="pagination-icon-button pagination-last"
          :disabled="isLastRecommendationPage || store.isRecommendationLoading || familyProgramRows.length === 0"
          title="Halaman terakhir"
          @click="lastRecommendationPage"
        >
          <span></span><span></span>
        </button>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useStore } from '../store/store'
import TabelWilayah from '../components/TabelWilayah.vue'
import TabelKeluarga from '../components/TabelKeluarga.vue'

const store = useStore()

const tableDropdown = ref(null)

const tableWilayahLevel = ref(
  localStorage.getItem('webgis_table_wilayah_level') || 'kabupaten'
)

const tableSearch = ref('')
const tableDesilFilter = ref([])
const tablePrioritasFilter = ref([])
const tableAsesmenFilter = ref([])

const activeTableRegionParent = computed(() => {
  if (
    store.tableRegionParentWilayah &&
    store.tableRegionDetailLevel &&
    tableWilayahLevel.value === store.tableRegionDetailLevel
  ) {
    return store.tableRegionParentWilayah
  }

  return null
})

const tableRegionDetailActive = computed(() => {
  return Boolean(activeTableRegionParent.value)
})

const familyWilayahLevel = ref(
  localStorage.getItem('webgis_family_wilayah_level') || 'kabupaten'
)

const familySelectedWilayah = ref(store.selectedWilayah)
const familyWilayahKeyword = ref('')
const familyProgramFilter = ref([])
const familyDesilFilter = ref([])
const familyPrioritasFilter = ref([])
const familyAsesmenFilter = ref([])
const familyStatusProgramFilter = ref([])
const familyStatusDataFilter = ref([])

const wilayahPage = ref(1)
const wilayahPageSize = 20
const familyPage = ref(1)
const familyPageSize = 20
let familySearchTimer = null
const isSearching = ref(false)
const wilayahHasLoaded = ref(false)
const familyHasLoaded = ref(false)
const lastAreaQueryKey = ref('')
const lastFamilyQueryKey = ref('')
const wilayahPrioritasList = ['Sangat Tinggi', 'Tinggi', 'Sedang', 'Rendah', 'Sangat Rendah']
const prioritasList = ['Sangat Tinggi', 'Tinggi', 'Sedang', 'Rendah', 'Sangat Rendah', 'Belum Ditetapkan']

const wilayahAsesmenList = [
  { key: 'perlu', label: 'Perlu Asesmen' },
  { key: 'tidak_perlu', label: 'Tidak Perlu' }
]

const asesmenList = [
  { key: 'perlu', label: 'Perlu Asesmen' },
  { key: 'tidak_perlu', label: 'Tidak Perlu' },
  { key: 'belum_ditetapkan', label: 'Belum Ditetapkan' }
]

const statusProgramList = [
  { key: 'direkomendasikan', label: 'Direkomendasikan' },
  { key: 'dipertimbangkan', label: 'Dipertimbangkan' }
]

const statusDataList = [
  { key: 'lengkap', label: 'Data Lengkap' },
  { key: 'tidak_lengkap', label: 'Tidak Lengkap' }
]

const tableFilterActive = computed(() => {
  return (
    tableSearch.value.trim() !== '' ||
    tableDesilFilter.value.length > 0 ||
    tablePrioritasFilter.value.length > 0 ||
    tableAsesmenFilter.value.length > 0
  )
})

const wilayahResetVisible = computed(() => {
  const openedFromMapRegion =
    store.tableMode === 'wilayah' &&
    store.tableRegionParentWilayah &&
    store.tableRegionDetailLevel

  return tableFilterActive.value || Boolean(openedFromMapRegion)
})

const wilayahTableLoading = computed(() => {
  return store.isTableAreaLoading || !wilayahHasLoaded.value
})

const familyFilterActive = computed(() => {
  return (
    activeFamilyWilayahKeyword.value.trim() !== '' ||
    familyProgramFilter.value.length > 0 ||
    familyDesilFilter.value.length > 0 ||
    familyPrioritasFilter.value.length > 0 ||
    familyAsesmenFilter.value.length > 0 ||
    familyStatusProgramFilter.value.length > 0 ||
    familyStatusDataFilter.value.length > 0
  )
})

const wilayahColumnLabel = computed(() => {
  const labels = {
    provinsi: 'Provinsi',
    kabupaten: 'Kabupaten/Kota',
    kecamatan: 'Kecamatan',
    desa: 'Desa/Kelurahan'
  }

  return labels[tableWilayahLevel.value] || 'Kabupaten/Kota'
})

const familyWilayahColumnLabel = computed(() => {
  const labels = {
    provinsi: 'Provinsi',
    kabupaten: 'Kabupaten/Kota',
    kecamatan: 'Kecamatan',
    desa: 'Desa/Kelurahan'
  }

  return labels[familyWilayahLevel.value] || 'Kabupaten/Kota'
})

const activeFamilyWilayahKeyword = computed({
  get() {
    return store.familyWilayahKeyword || familyWilayahKeyword.value
  },
  set(value) {
    store.familyWilayahKeyword = value
    familyWilayahKeyword.value = value
  }
})

const filteredWilayahTable = computed(() => {
  const keyword = tableSearch.value.toLowerCase().trim()
  const activeLevel = tableWilayahLevel.value

  return store.tableWilayahData
    .filter((wilayah) => {
      return String(wilayah.level || '').toLowerCase() === activeLevel
    })

    // FILTER SESUAI HIERARKI WILAYAH YANG DIPILIH
    .filter((wilayah) => {
      if (activeLevel === 'kabupaten') {
        if (store.selectedProvinceCode) {
          return wilayah.kodeProvinsi === store.selectedProvinceCode
        }
      }

      if (activeLevel === 'kecamatan') {
        if (store.selectedKabkotCode) {
          return wilayah.kodeKabkot === store.selectedKabkotCode
        }
      }

      if (activeLevel === 'desa') {
        if (store.selectedKecamatanCode) {
          return wilayah.kodeKecamatan === store.selectedKecamatanCode
        }
      }

      return true
    })

    // SEARCH
    .filter((wilayah) => {
      if (keyword === '') return true

      const wilayahName = getWilayahNameByLevel(wilayah, activeLevel).toLowerCase()

      return wilayahName.includes(keyword)
    })

    .sort((a, b) => totalBantuan(b) - totalBantuan(a))
})

const wilayahStartIndex = computed(() => (wilayahPage.value - 1) * wilayahPageSize)

const wilayahEndIndex = computed(() => {
  return Math.min(wilayahStartIndex.value + wilayahPageSize, filteredWilayahTable.value.length)
})

const pagedWilayahRows = computed(() => {
  return filteredWilayahTable.value.slice(wilayahStartIndex.value, wilayahEndIndex.value)
})

const isLastWilayahPage = computed(() => {
  return wilayahEndIndex.value >= filteredWilayahTable.value.length
})

const familyProgramRows = computed(() => {
  return store.keluargaData.flatMap((keluarga) => {
    return keluarga.rekomendasi.map((item) => ({
      rowId: `${keluarga.id_keluarga}-${item.program}-${keluarga.wilayahId}`,
      id_keluarga: keluarga.id_keluarga,

      wilayahId: keluarga.wilayahId,
      wilayah: keluarga.wilayah,

      provinsi: keluarga.provinsi,
      kabupaten: keluarga.kabupaten,
      kecamatan: keluarga.kecamatan,
      desa: keluarga.desa,

      kodeProvinsi: keluarga.kodeProvinsi,
      kodeKabkot: keluarga.kodeKabkot,
      kodeKecamatan: keluarga.kodeKecamatan,
      kodeKelurahanDesa: keluarga.kodeKelurahanDesa,

      desil: keluarga.desil,
      desilLabel: keluarga.desilLabel,

      program: item.program,
      statusProgram: item.statusProgram,
      prioritas: item.prioritas,
      catatanAsesmen: item.catatanAsesmen,
      kelengkapanData: item.kelengkapanData,
      dasarRekomendasi: item.dasarRekomendasi
    }))
  })
})

function normalizeSearch(value) {
  return String(value || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
}

function getFamilySearchTargets(row, level) {
  const levelTargets = {
    provinsi: [row.provinsi, row.namaProvinsi],
    kabupaten: [row.kabupaten, row.namaKabkot, row.kabupatenKota, row.namaKabupatenKota],
    kabupaten_kota: [row.kabupaten, row.namaKabkot, row.kabupatenKota, row.namaKabupatenKota],
    kecamatan: [row.kecamatan, row.namaKecamatan],
    desa: [row.desa, row.kelurahan, row.namaKelurahanDesa]
  }

  return [
    ...(levelTargets[level] || [])
  ]
}

const filteredFamilyProgramRows = computed(() => {
  return familyProgramRows.value
})

const familyStartIndex = computed(() => (familyPage.value - 1) * familyPageSize)

const familyEndIndex = computed(() => {
  return Math.min(familyStartIndex.value + familyPageSize, filteredFamilyProgramRows.value.length)
})

const pagedFamilyProgramRows = computed(() => {
  return filteredFamilyProgramRows.value.slice(familyStartIndex.value, familyEndIndex.value)
})

const isLastRecommendationPage = computed(() => {
  return (
    store.recommendationOffset + familyProgramRows.value.length >=
    store.recommendationTotalRows
  )
})

const isFirstRecommendationPage = computed(() => {
  return store.recommendationOffset <= 0
})

function closeTableDropdown(event) {
  const clickedInsideDropdown =
    event.target.closest('.filter-dropdown') ||
    event.target.closest('.th-filter-wrap') ||
    event.target.closest('.floating-filter-menu')

  if (!clickedInsideDropdown) {
    tableDropdown.value = null
  }
}

function totalBantuan(wilayah) {
  return Object.values(wilayah.bantuan || {}).reduce((a, b) => a + Number(b || 0), 0)
}

function getWilayahNameByLevel(row, level) {
  if (level === 'provinsi') return row.namaWilayah || row.provinsi || '-'
  if (level === 'kabupaten') return row.namaWilayah || row.kabupaten || '-'
  if (level === 'kecamatan') return row.namaWilayah || row.kecamatan || '-'
  if (level === 'desa') return row.namaWilayah || row.desa || '-'

  return row.namaWilayah || row.nama || '-'
}

function getFamilyWilayahByLevel(row, level) {
  if (level === 'provinsi') return row.provinsi || '-'
  if (level === 'kabupaten') return row.kabupaten || '-'
  if (level === 'kecamatan') return row.kecamatan || '-'
  if (level === 'desa') return row.desa || '-'

  return row.wilayah || '-'
}

function getWilayahSubLabel(row, level) {
  if (level === 'provinsi') {
    return ''
  }

  if (level === 'kabupaten') {
    return row.provinsi || ''
  }

  if (level === 'kecamatan') {
    return [row.kabupaten, row.provinsi]
      .filter((value) => value && value !== '-')
      .join(', ')
  }

  if (level === 'desa') {
    return [row.kecamatan, row.kabupaten]
      .filter((value) => value && value !== '-')
      .join(', ')
  }

  return ''
}

function buildStableKey(params) {
  return JSON.stringify(params, (_, value) => {
    return Array.isArray(value) ? [...value].sort() : value
  })
}

async function loadAreasIfNeeded({ force = false } = {}) {
  const params = {
    level: tableWilayahLevel.value,
    wilayah: activeTableRegionParent.value,
    desil: tableDesilFilter.value,
    prioritas: tablePrioritasFilter.value,
    asesmen: tableAsesmenFilter.value
  }

  const queryKey = buildStableKey(params)

  if (
    !force &&
    lastAreaQueryKey.value === queryKey &&
    store.tableWilayahData.length > 0
  ) {
    wilayahHasLoaded.value = true
    return
  }

  lastAreaQueryKey.value = queryKey
  wilayahHasLoaded.value = false

  try {
    await store.fetchTableAreas({
      force,
      ...params
    })
  } finally {
    wilayahHasLoaded.value = true
  }
}

async function resetWilayahTableFilter() {
  tableSearch.value = ''
  tableDesilFilter.value = []
  tablePrioritasFilter.value = []
  tableAsesmenFilter.value = []
  wilayahPage.value = 1
  tableDropdown.value = null

  store.selectedWilayah = null

  store.tableRegionParentWilayah = null
  store.tableRegionDetailLevel = ''
  store.tableRegionOpenRequestId = 0

  lastAreaQueryKey.value = ''

  await loadAreasIfNeeded({ force: true })
}

async function resetFamilyTableFilter() {
  familySelectedWilayah.value = null
  familyWilayahKeyword.value = ''
  store.familyWilayahKeyword = ''
  store.selectedWilayah = null

  familyProgramFilter.value = []
  familyDesilFilter.value = []
  familyPrioritasFilter.value = []
  familyAsesmenFilter.value = []
  familyStatusProgramFilter.value = []
  familyStatusDataFilter.value = []

  tableDropdown.value = null

  familyPage.value = 1
  await loadFamilyRecommendations(0)
}

async function showWilayahTable() {
  store.setTableMode('wilayah')
  tableDropdown.value = null

  await loadAreasIfNeeded()
}

async function showFamilyTable() {
  store.setTableMode('keluarga')
  tableDropdown.value = null

  if (store.keluargaInitialLoaded && store.keluargaData.length > 0) {
    familyHasLoaded.value = true
    return
  }

  await loadFamilyRecommendations(0)
}

function getFamilyKeywordFromWilayah(wilayah, level) {
  if (!wilayah) return ''

  if (level === 'provinsi') {
    return wilayah.namaProvinsi || wilayah.provinsi || wilayah.namaWilayah || wilayah.nama || ''
  }

  if (level === 'kabupaten') {
    return wilayah.namaKabkot || wilayah.kabupaten || wilayah.namaWilayah || wilayah.nama || ''
  }

  if (level === 'kecamatan') {
    return wilayah.namaKecamatan || wilayah.kecamatan || wilayah.namaWilayah || wilayah.nama || ''
  }

  if (level === 'desa') {
    return wilayah.namaKelurahanDesa || wilayah.desa || wilayah.namaWilayah || wilayah.nama || ''
  }

  return wilayah.namaWilayah || wilayah.nama || ''
}

async function openFamilyDetailFromTable(wilayah) {
  familySelectedWilayah.value = wilayah
  familyWilayahLevel.value = tableWilayahLevel.value
  localStorage.setItem('webgis_family_wilayah_level', familyWilayahLevel.value)

  familyWilayahKeyword.value = getFamilyKeywordFromWilayah(
    wilayah,
    familyWilayahLevel.value
  )

  store.familyWilayahKeyword = familyWilayahKeyword.value

  familyDesilFilter.value = [...tableDesilFilter.value]
  familyPrioritasFilter.value = [...tablePrioritasFilter.value]
  familyAsesmenFilter.value = [...tableAsesmenFilter.value]

  familyProgramFilter.value = []
  familyStatusProgramFilter.value = ['direkomendasikan']
  familyStatusDataFilter.value = []
  tableDropdown.value = null

  store.openFamilyTableForWilayah(wilayah)
  familyHasLoaded.value = false
  await loadFamilyRecommendations(0, { force: true })
}

async function applyPanelFiltersToFamilyTable() {
  familySelectedWilayah.value = store.selectedMapWilayah
  familyWilayahLevel.value = store.wilayahLevel || familyWilayahLevel.value
  localStorage.setItem('webgis_family_wilayah_level', familyWilayahLevel.value)

  familyWilayahKeyword.value = getFamilyKeywordFromWilayah(
    familySelectedWilayah.value,
    familyWilayahLevel.value
  )

  store.familyWilayahKeyword = familyWilayahKeyword.value

  familyDesilFilter.value = [...store.desil]
  familyPrioritasFilter.value = [...store.prioritas]
  familyAsesmenFilter.value = [...store.asesmen]

  familyProgramFilter.value = []
  familyStatusProgramFilter.value = ['direkomendasikan']
  familyStatusDataFilter.value = []

  familyPage.value = 1
  store.recommendationOffset = 0
  familyHasLoaded.value = false
  lastFamilyQueryKey.value = ''

  await nextTick()
  await loadFamilyRecommendations(0, { force: true })
}

async function loadFamilyRecommendations(offset = 0, { force = false } = {}) {
  const selected = familySelectedWilayah.value

  const params = {
    offset,
    limit: familyPageSize,

    wilayahLevel: familyWilayahLevel.value,

    kodeProvinsi: selected?.kodeProvinsi || null,
    kodeKabkot: selected?.kodeKabkot || null,
    kodeKecamatan: selected?.kodeKecamatan || null,
    kodeKelurahanDesa: selected?.kodeKelurahanDesa || null,

    keyword: activeFamilyWilayahKeyword.value.trim(),

    programFilter: familyProgramFilter.value,
    desilFilter: familyDesilFilter.value,
    prioritasFilter: familyPrioritasFilter.value,
    asesmenFilter: familyAsesmenFilter.value,
    statusProgramFilter: familyStatusProgramFilter.value,
    statusDataFilter: familyStatusDataFilter.value
  }

  const queryKey = buildStableKey(params)

  if (
    !force &&
    lastFamilyQueryKey.value === queryKey &&
    store.keluargaData.length > 0
  ) {
    familyHasLoaded.value = true
    return
  }

  lastFamilyQueryKey.value = queryKey
  isSearching.value = true

  try {
    await store.fetchRecommendations(params)
    familyHasLoaded.value = true
  } finally {
    isSearching.value = false
  }
}

function firstWilayahPage() {
  wilayahPage.value = 1
}

function previousWilayahPage() {
  if (wilayahPage.value > 1) {
    wilayahPage.value -= 1
  }
}

function nextWilayahPage() {
  if (!isLastWilayahPage.value && filteredWilayahTable.value.length > 0) {
    wilayahPage.value += 1
  }
}

function lastWilayahPage() {
  const totalPage = Math.ceil(filteredWilayahTable.value.length / wilayahPageSize)
  wilayahPage.value = Math.max(1, totalPage)
}

function syncFamilyPageFromOffset() {
  familyPage.value =
    Math.floor(store.recommendationOffset / familyPageSize) + 1
}

async function firstRecommendationPage() {
  await loadFamilyRecommendations(0, { force: true })
  syncFamilyPageFromOffset()
}

async function previousRecommendationPage() {
  const nextOffset = Math.max(
    0,
    store.recommendationOffset - familyPageSize
  )

  await loadFamilyRecommendations(nextOffset, { force: true })
  syncFamilyPageFromOffset()
}

async function nextRecommendationPage() {
  if (isLastRecommendationPage.value) return

  const nextOffset =
    store.recommendationOffset + familyPageSize

  await loadFamilyRecommendations(nextOffset, { force: true })
  syncFamilyPageFromOffset()
}

async function lastRecommendationPage() {
  const totalRows = store.recommendationTotalRows
  const limit = familyPageSize

  const lastOffset =
    Math.max(0, Math.floor((totalRows - 1) / limit) * limit)

  await loadFamilyRecommendations(lastOffset, { force: true })
  syncFamilyPageFromOffset()
}

function formatNumber(value) {
  return new Intl.NumberFormat('id-ID').format(value || 0)
}

watch(
  [
    tableWilayahLevel,
    tableDesilFilter,
    tablePrioritasFilter,
    tableAsesmenFilter
  ],
  async () => {
    if (store.tableMode !== 'wilayah') return

    wilayahPage.value = 1
    await loadAreasIfNeeded({ force: true })
  },
  { deep: true }
)

watch(
  tableSearch,
  () => {
    wilayahPage.value = 1
  }
)

watch(
  tableWilayahLevel,
  (value) => {
    localStorage.setItem('webgis_table_wilayah_level', value)
  }
)

watch(
  [
    familyProgramFilter,
    familyDesilFilter,
    familyPrioritasFilter,
    familyAsesmenFilter,
    familyStatusProgramFilter,
    familyStatusDataFilter
  ],
  async () => {
    if (store.tableMode !== 'keluarga') return

    familyPage.value = 1
    await loadFamilyRecommendations(0)
  },
  { deep: true }
)

watch(
  familyWilayahLevel,
  async (value) => {
    localStorage.setItem('webgis_family_wilayah_level', value)

    if (store.tableMode !== 'keluarga') return

    familyPage.value = 1
    await loadFamilyRecommendations(0)
  }
)

watch(
  activeFamilyWilayahKeyword,
  () => {
    if (store.tableMode !== 'keluarga') return

    clearTimeout(familySearchTimer)

    familySearchTimer = setTimeout(async () => {
      familyPage.value = 1
      await loadFamilyRecommendations(0, { force: true })
    }, 500)
  }
)

watch(
  filteredFamilyProgramRows,
  () => {
    const totalPage = Math.max(1, Math.ceil(filteredFamilyProgramRows.value.length / familyPageSize))

    if (familyPage.value > totalPage) {
      familyPage.value = totalPage
    }
  }
)

watch(
  () => store.familyOpenRequestId,
  async () => {
    if (
      store.activeMenu !== 'tabel_rekomendasi' ||
      store.tableMode !== 'keluarga' ||
      !store.selectedWilayah
    ) {
      return
    }

    if (
      store.familyOpenSource === 'peta_rekomendasi' ||
      store.familyOpenSource === 'peta_konsentrasi'
    ) {
      await applyPanelFiltersToFamilyTable()
    }
  }
)

watch(
  () => store.tableRegionOpenRequestId,
  async () => {
    if (
      store.activeMenu !== 'tabel_rekomendasi' ||
      store.tableMode !== 'wilayah' ||
      !store.tableRegionParentWilayah ||
      !store.tableRegionDetailLevel
    ) {
      return
    }

    tableWilayahLevel.value = store.tableRegionDetailLevel
    tableSearch.value = ''
    wilayahPage.value = 1
    tableDropdown.value = null

    lastAreaQueryKey.value = ''

    await loadAreasIfNeeded({ force: true })
  }
)

onMounted(async () => {
  document.addEventListener('click', closeTableDropdown)

  if (
    store.tableMode === 'wilayah' &&
    store.tableRegionParentWilayah &&
    store.tableRegionDetailLevel
  ) {
    tableWilayahLevel.value = store.tableRegionDetailLevel
    tableSearch.value = ''
    wilayahPage.value = 1
    tableDropdown.value = null
    lastAreaQueryKey.value = ''

    await loadAreasIfNeeded({ force: true })
    return
  }

  if (store.tableMode === 'wilayah') {
    await loadAreasIfNeeded()
    return
  }

  if (store.tableMode === 'keluarga') {
    if (store.keluargaInitialLoaded && store.keluargaData.length > 0) {
      familyHasLoaded.value = true
      return
    }

    await loadFamilyRecommendations(0)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeTableDropdown)

  if (familySearchTimer) {
    clearTimeout(familySearchTimer)
  }
})
</script>