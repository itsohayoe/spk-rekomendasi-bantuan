<template>
  <aside
    class="panel"
    :class="{ 
      'concentration-panel': store.activeMenu === 'peta_konsentrasi', 
      'recommendation-panel': store.activeMenu === 'peta_rekomendasi'
    }"
  >
    <div class="panel-card">
      <div class="panel-title-row">
        <div>
          <span class="eyebrow">Pengaturan</span>
          <h2>{{ panelSettingTitle }}</h2>
        </div>
      </div>

      <div class="form-group">
        <label>Tingkat Wilayah</label>
        <select v-model="activeWilayahLevel" class="input">
          <option value="provinsi">Provinsi</option>
          <option value="kabupaten">Kabupaten/Kota</option>
          <option value="kecamatan">Kecamatan</option>
          <option value="desa">Desa/Kelurahan</option>
        </select>
      </div>

      <div v-if="store.activeMenu === 'peta_konsentrasi'" class="form-group">
        <div class="field-label-row">
          <label>Wilayah Acuan</label>

          <button
            type="button"
            class="switch-button"
            :class="{ active: store.concentrationMapFollowReference }"
            @click="store.setConcentrationMapFollowReference(!store.concentrationMapFollowReference)"
          >
            <span class="switch-track">
              <span class="switch-thumb"></span>
            </span>

            <span class="switch-text">
              {{ store.concentrationMapFollowReference ? 'Aktif' : 'Nonaktif' }}
            </span>
          </button>
        </div>

        <select v-model="activeConcentrationReference" class="input">
          <option
            v-for="item in concentrationReferenceOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </option>
        </select>
      </div>

      <div
        v-if="store.activeMenu === 'peta_rekomendasi' && activeWilayahLevel !== 'desa'"
        class="form-group"
      >
        <div class="field-label-row">
          <label>Rincian Wilayah</label>

          <button
            type="button"
            class="switch-button"
            :class="{ active: store.recommendationMapFollowReference }"
            @click="store.setRecommendationMapFollowReference(!store.recommendationMapFollowReference)"
          >
            <span class="switch-track">
              <span class="switch-thumb"></span>
            </span>

            <span class="switch-text">
              {{ store.recommendationMapFollowReference ? 'Aktif' : 'Nonaktif' }}
            </span>
          </button>
        </div>

        <select v-model="activeRecommendationMapReference" class="input">
          <option
            v-for="item in recommendationMapReferenceOptions"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </option>
        </select>
      </div>

      <template v-if="store.activeMenu === 'peta_konsentrasi'">
        <div class="form-group">
          <label>Jenis Konsentrasi</label>

          <div class="panel-toggle">
            <button
              :class="{ active: store.concentrationType === 'bantuan' }"
              @click="store.concentrationType = 'bantuan'"
            >
              Bantuan
            </button>

            <button
              :class="{ active: store.concentrationType === 'dimensi' }"
              @click="store.concentrationType = 'dimensi'"
            >
              Dimensi
            </button>
          </div>
        </div>

        <div v-if="store.concentrationType === 'bantuan'" class="form-group">
          <label>Program Bantuan</label>
          <select v-model="store.selectedProgram" class="input">
            <option
              v-for="program in store.programList"
              :key="program.key"
              :value="program.key"
            >
              {{ program.label }}
            </option>
          </select>
        </div>

        <div v-else class="form-group">
          <label>Dimensi Kemiskinan</label>
          <select v-model="store.selectedDimension" class="input">
            <option
              v-for="dimension in store.dimensionList"
              :key="dimension.key"
              :value="dimension.key"
            >
              {{ dimension.label }}
            </option>
          </select>
        </div>
      </template>

      <template v-if="store.activeMenu === 'peta_rekomendasi'">
        <div class="form-group">
          <input
            v-model="store.searchKeyword"
            class="input"
            type="text"
            :placeholder="wilayahSearchPlaceholder"
            @keyup.enter="selectFirstSearchResult"
          />
        </div>

        <div class="filter-row recommendation-filter-row">
          <div class="dropdown">
            <button
              class="btn-filter"
              :class="{ active: store.desil.length > 0 }"
              @click.stop="toggleDropdown('desil')"
            >
              <span>Desil</span>
            </button>

            <div v-if="openDropdown === 'desil'" class="dropdown-menu" @click.stop>
              <label v-for="i in 10" :key="i">
                <input type="checkbox" v-model="store.desil" :value="i" />
                Desil {{ i }}
              </label>
            </div>
          </div>

          <div class="dropdown">
            <button
              class="btn-filter"
              :class="{ active: store.prioritas.length > 0 }"
              @click.stop="toggleDropdown('prioritas')"
            >
              <span>Prioritas</span>
            </button>

            <div v-if="openDropdown === 'prioritas'" class="dropdown-menu" @click.stop>
              <label v-for="item in panelPrioritasList" :key="item">
                <input type="checkbox" v-model="store.prioritas" :value="item" />
                {{ item }}
              </label>
            </div>
          </div>

          <div class="dropdown">
            <button
              class="btn-filter"
              :class="{ active: store.asesmen.length > 0 }"
              @click.stop="toggleDropdown('asesmen')"
            >
              <span>Asesmen</span>
            </button>

            <div v-if="openDropdown === 'asesmen'" class="dropdown-menu" @click.stop>
              <label v-for="item in panelAsesmenList" :key="item.key">
                <input type="checkbox" v-model="store.asesmen" :value="item.key" />
                {{ item.label }}
              </label>
            </div>
          </div>

          <button
            v-if="panelFilterActive"
            class="btn-reset"
            @click="resetRecommendationFilter"
          >
            Reset
          </button>
        </div>
      </template>
    </div>

    <div v-if="!selected" class="panel-card empty-panel">
      <span class="eyebrow">Detail Wilayah</span>

      <h2 v-if="mapSearchNotFound">Data tidak ditemukan</h2>
      <h2 v-else>Belum ada wilayah yang dipilih</h2>
    </div>

    <template v-else>
      <div class="panel-card selected-card">
        <span class="eyebrow">Wilayah Terpilih</span>
        <h2>{{ selectedTitle }}</h2>
        <p v-if="selectedSubtitle">{{ selectedSubtitle }}</p>
      </div>

      <div v-if="store.activeMenu === 'peta_konsentrasi'" class="panel-card">
        <span class="eyebrow">Ringkasan Konsentrasi</span>

        <div v-if="concentrationPanelLoading" class="summary-loading-box">
          <div class="inline-spinner"></div>
        </div>

        <template v-else>
          <div class="selected-grid concentration-summary-grid">
            <div class="summary-box">
              <span>Nilai LQ</span>
              <strong>{{ formatLq(activeConcentrationData?.nilai) }}</strong>
            </div>

            <div class="summary-box">
              <span>Kategori</span>
              <strong>{{ activeConcentrationData?.kategori || '-' }}</strong>
            </div>

            <div class="summary-box">
              <span class="two-line-card-label">{{ selectedFocusLabel }}</span>
              <strong>{{ formatNumber(activeConcentrationData?.jumlahTerpilih) }}</strong>
            </div>

            <div class="summary-box">
              <span class="two-line-card-label">{{ selectedTotalLabel }}</span>
              <strong>{{ formatNumber(activeConcentrationData?.totalPembanding) }}</strong>
            </div>

            <div class="summary-box">
              <span class="two-line-card-label">{{ acuanFocusLabel }}</span>
              <strong>{{ formatNumber(acuanJumlahTerpilih) }}</strong>
            </div>

            <div class="summary-box">
              <span class="two-line-card-label">{{ acuanTotalLabel }}</span>
              <strong>{{ formatNumber(acuanTotalPembanding) }}</strong>
            </div>
          </div>

          <div class="info-card concentration-interpretation-card">
            <span class="eyebrow">Interpretasi</span>
            <p class="interpretation-text">{{ lqInterpretation }}</p>
          </div>

          <div class="indicator-section compact-indicator-section">
            <span class="eyebrow">Rincian Indikator</span>

            <div
              v-if="activeIndicatorRows.length > 0"
              class="indicator-list compact-indicator-list"
            >
              <div
                v-for="item in activeIndicatorRows"
                :key="item.key"
                class="indicator-row compact-indicator-row"
              >
                <span>{{ item.label }}</span>
                <strong>{{ formatNumber(item.value) }}</strong>
              </div>
            </div>

            <div v-else class="empty-indicator compact-empty-indicator">
              {{ indicatorEmptyText }}
            </div>
          </div>
        </template>
      </div>

      <div v-if="store.activeMenu === 'peta_rekomendasi'" class="panel-card">
        <div class="panel-section-title">
          <span class="eyebrow">Ringkasan Rekomendasi</span>
        </div>

        <div v-if="recommendationSummaryLoading" class="summary-loading-box">
          <div class="inline-spinner"></div>
        </div>

        <template v-else>
          <div
            v-if="showSixSummaryCards"
            class="selected-grid filter-summary-grid"
          >
            <div>
              <span>Total Keluarga</span>
              <strong>{{ formatNumber(selectedSixSummary.totalKeluarga) }}</strong>
            </div>

            <div>
              <span>Total Rekomendasi</span>
              <strong>{{ formatNumber(selectedSixSummary.totalRekomendasi) }}</strong>
            </div>

            <div>
              <span>Keluarga D1–D5</span>
              <strong>{{ formatNumber(selectedSixSummary.keluargaDesil15) }}</strong>
            </div>

            <div>
              <span>Rekomendasi D1–D5</span>
              <strong>{{ formatNumber(selectedSixSummary.rekomendasiDesil15) }}</strong>
            </div>

            <div>
              <span>Keluarga D6–D10</span>
              <strong>{{ formatNumber(selectedSixSummary.keluargaDesil610) }}</strong>
            </div>

            <div>
              <span>Rekomendasi D6–D10</span>
              <strong>{{ formatNumber(selectedSixSummary.rekomendasiDesil610) }}</strong>
            </div>
          </div>

          <div
            v-else
            class="selected-grid filter-summary-grid"
          >
            <div>
              <span>Total Keluarga</span>
              <strong>{{ formatNumber(selectedSummary.totalKeluarga) }}</strong>
            </div>

            <div>
              <span>Total Rekomendasi</span>
              <strong>{{ formatNumber(selectedSummary.totalRekomendasi) }}</strong>
            </div>
          </div>

          <div class="program-list with-top-border">
            <div
              v-for="program in programRows"
              :key="program.key"
              class="program-item"
            >
              <div>
                <span>{{ program.label }}</span>
                <div class="tiny-bar">
                  <div :style="{ width: `${program.percent}%` }"></div>
                </div>
              </div>

              <strong>{{ formatNumber(program.value) }}</strong>
            </div>
          </div>

          <div
            class="panel-action-row"
            :class="{ 'only-family': !showRegionDetailButton }"
          >
            <button
              v-if="showRegionDetailButton"
              class="btn-outline-panel"
              @click="openRegionDetailTableFromPanel"
            >
              Lihat Daftar Wilayah
            </button>

            <button class="btn-outline-panel" @click="openFamilyTableFromPanel">
              Lihat Daftar Keluarga
            </button>
          </div>
        </template>
      </div>
    </template>
  </aside>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useStore } from '../store/store'

const store = useStore()
const openDropdown = ref(null)

const API_BASE = 'https://backend-nasional-production.up.railway.app/api'
const selectedRegionSummary = ref(null)
const selectedRegionSummaryLoading = ref(false)
let selectedSummaryRequestId = 0

const panelPrioritasList = [
  'Sangat Tinggi',
  'Tinggi',
  'Sedang',
  'Rendah',
  'Sangat Rendah'
]

const panelAsesmenList = [
  { key: 'perlu', label: 'Perlu Asesmen' },
  { key: 'tidak_perlu', label: 'Tidak Perlu' }
]

const selected = computed(() => store.selectedMapWilayah)

const activeWilayahLevel = computed({
  get() {
    return store.wilayahLevel
  },
  set(value) {
    const oldValue = store.wilayahLevel

    if (
      store.activeMenu === 'peta_rekomendasi' &&
      value !== oldValue
    ) {
      store.setRecommendationMapFollowReference(false)
      store.setSelectedMapWilayah(null)
    }

    store.setActiveMapLevel(value)
  }
})

const activeConcentrationReference = computed({
  get() {
    return store.concentrationReference
  },
  set(value) {
    store.setConcentrationReference(value)
  }
})

const concentrationReferenceOptions = computed(() => {
  if (activeWilayahLevel.value === 'provinsi') {
    return [
      { value: 'nasional', label: 'Nasional' }
    ]
  }

  if (activeWilayahLevel.value === 'kabupaten') {
    return [
      { value: 'provinsi', label: 'Provinsi' },
      { value: 'nasional', label: 'Nasional' }
    ]
  }

  if (activeWilayahLevel.value === 'kecamatan') {
    return [
      { value: 'kabupaten', label: 'Kabupaten/Kota' },
      { value: 'provinsi', label: 'Provinsi' },
      { value: 'nasional', label: 'Nasional' }
    ]
  }

  if (activeWilayahLevel.value === 'desa') {
    return [
      { value: 'kecamatan', label: 'Kecamatan' },
      { value: 'kabupaten', label: 'Kabupaten/Kota' },
      { value: 'provinsi', label: 'Provinsi' },
      { value: 'nasional', label: 'Nasional' }
    ]
  }

  return [
    { value: 'nasional', label: 'Nasional' }
  ]
})

const activeRecommendationMapReference = computed({
  get() {
    return store.recommendationMapReference
  },
  set(value) {
    store.setRecommendationMapReference(value)
  }
})

const recommendationMapReferenceOptions = computed(() => {
  if (activeWilayahLevel.value === 'provinsi') {
    return [
      { value: 'kabupaten', label: 'Kabupaten/Kota' },
      { value: 'kecamatan', label: 'Kecamatan' },
      { value: 'desa', label: 'Desa/Kelurahan' }
    ]
  }

  if (activeWilayahLevel.value === 'kabupaten') {
    return [
      { value: 'kecamatan', label: 'Kecamatan' },
      { value: 'desa', label: 'Desa/Kelurahan' }
    ]
  }

  if (activeWilayahLevel.value === 'kecamatan') {
    return [
      { value: 'desa', label: 'Desa/Kelurahan' }
    ]
  }

  return []
})

watch(
  () => activeWilayahLevel.value,
  () => {
    const allowedValues = concentrationReferenceOptions.value.map((item) => item.value)

    if (!allowedValues.includes(store.concentrationReference)) {
      store.setConcentrationReference('nasional')
    }
  }
)

watch(
  () => activeWilayahLevel.value,
  () => {
    const allowedValues = recommendationMapReferenceOptions.value.map((item) => item.value)

    if (allowedValues.length === 0) {
      store.setRecommendationMapFollowReference(false)
      return
    }

    if (!allowedValues.includes(store.recommendationMapReference)) {
      store.setRecommendationMapReference(allowedValues[0])
    }
  }
)

const concentrationPanelLoading = computed(() => {
  return (
    store.activeMenu === 'peta_konsentrasi' &&
    (
      store.isAreaLoading ||
      store.isConcentrationLoading ||
      store.isMapRendering
    )
  )
})

const wilayahSearchPlaceholder = computed(() => {
  const map = {
    provinsi: 'Cari nama provinsi...',
    kabupaten: 'Cari nama kabupaten/kota...',
    kecamatan: 'Cari nama kecamatan...',
    desa: 'Cari nama desa/kelurahan...'
  }

  return map[store.wilayahLevel] || 'Cari nama wilayah...'
})

const panelFilterActive = computed(() => {
  return (
    store.searchKeyword.trim() !== '' ||
    store.desil.length > 0 ||
    store.prioritas.length > 0 ||
    store.asesmen.length > 0 ||
    (
      store.activeMenu === 'peta_rekomendasi' &&
      store.recommendationMapFollowReference
    )
  )
})

const mapSearchNotFound = computed(() => {
  return (
    store.activeMenu === 'peta_rekomendasi' &&
    store.searchKeyword.trim() !== '' &&
    !store.isAreaLoading &&
    store.filteredMapWilayah.length === 0
  )
})

const recommendationDataFilterActive = computed(() => {
  return (
    store.desil.length > 0 ||
    store.prioritas.length > 0 ||
    store.asesmen.length > 0
  )
})

const showSixSummaryCards = computed(() => {
  return (
    store.activeMenu === 'peta_rekomendasi' &&
    selected.value &&
    !recommendationDataFilterActive.value
  )
})

const showRegionDetailButton = computed(() => {
  return (
    store.activeMenu === 'peta_rekomendasi' &&
    selected.value &&
    activeWilayahLevel.value !== 'desa'
  )
})

const recommendationSummaryLoading = computed(() => {
  if (store.activeMenu !== 'peta_rekomendasi') return false
  if (!selected.value) return false

  return (
    store.isAreaLoading ||
    store.isMapRendering ||
    store.isMapPanelLoading ||
    (showSixSummaryCards.value && selectedRegionSummaryLoading.value)
  )
})

const panelSettingTitle = computed(() => {
  if (store.activeMenu === 'peta_konsentrasi') return 'Parameter Konsentrasi'
  return 'Filter Rekomendasi'
})

const selectedDisplayLevel = computed(() => {
  return selected.value?.level || store.wilayahLevel
})

const selectedTitle = computed(() => {
  const wilayah = selected.value
  if (!wilayah) return ''

  if (selectedDisplayLevel.value === 'provinsi') {
    return wilayah.nama || wilayah.namaWilayah || wilayah.provinsi || wilayah.namaProvinsi || '-'
  }

  if (selectedDisplayLevel.value === 'kabupaten') {
    return wilayah.nama || wilayah.namaWilayah || wilayah.kabupaten || wilayah.namaKabkot || '-'
  }

  if (selectedDisplayLevel.value === 'kecamatan') {
    return wilayah.nama || wilayah.namaWilayah || wilayah.kecamatan || wilayah.namaKecamatan || '-'
  }

  if (selectedDisplayLevel.value === 'desa') {
    return wilayah.nama || wilayah.namaWilayah || wilayah.desa || wilayah.namaKelurahanDesa || '-'
  }

  return wilayah.nama || wilayah.namaWilayah || '-'
})

const selectedShortName = computed(() => {
  return selectedTitle.value || 'Wilayah Ini'
})

const selectedSubtitle = computed(() => {
  const wilayah = selected.value
  if (!wilayah) return ''

  const provinsi =
    wilayah.provinsi ||
    wilayah.namaProvinsi ||
    ''

  const kabupaten =
    wilayah.kabupaten ||
    wilayah.namaKabkot ||
    ''

  const kecamatan =
    wilayah.kecamatan ||
    wilayah.namaKecamatan ||
    ''

  if (selectedDisplayLevel.value === 'provinsi') {
    return ''
  }

  if (selectedDisplayLevel.value === 'kabupaten') {
    return provinsi
  }

  if (selectedDisplayLevel.value === 'kecamatan') {
    return [kabupaten, provinsi].filter(Boolean).join(', ')
  }

  if (selectedDisplayLevel.value === 'desa') {
    return [kecamatan, kabupaten].filter(Boolean).join(', ')
  }

  return ''
})

const selectedSummary = computed(() => {
  const wilayah = selected.value

  const bantuanTotal = Object.values(wilayah?.bantuan || {}).reduce((a, b) => {
    return a + Number(b || 0)
  }, 0)

  return {
    totalKeluarga: Number(wilayah?.totalKeluarga || 0),
    totalRekomendasi: Number(wilayah?.totalRekomendasi || bantuanTotal || 0)
  }
})

const selectedSixSummary = computed(() => {
  return {
    totalKeluarga: Number(selectedSummary.value.totalKeluarga || 0),
    totalRekomendasi: Number(selectedSummary.value.totalRekomendasi || 0),

    keluargaDesil15: Number(selectedRegionSummary.value?.keluargaDesil15 || 0),
    rekomendasiDesil15: Number(selectedRegionSummary.value?.rekomendasiDesil15 || 0),

    keluargaDesil610: Number(selectedRegionSummary.value?.keluargaDesil610 || 0),
    rekomendasiDesil610: Number(selectedRegionSummary.value?.rekomendasiDesil610 || 0)
  }
})

const selectedProgramLabel = computed(() => {
  return store.programList.find((item) => item.key === store.selectedProgram)?.label || ''
})

const selectedDimensionLabel = computed(() => {
  return store.dimensionList.find((item) => item.key === store.selectedDimension)?.label || ''
})

const programRows = computed(() => {
  if (!selected.value) return []

  const bantuan = selected.value.bantuan || {}

  const maxValue = Math.max(
    ...Object.values(bantuan).map((value) => Number(value || 0)),
    0
  )

  return store.programList.map((program) => {
    const value = Number(bantuan[program.key] || 0)

    return {
      ...program,
      value,
      percent: maxValue > 0 ? (value / maxValue) * 100 : 0
    }
  })
})

const programLq = computed(() => {
  if (!selected.value) return 0
  return store.lqProgram(selected.value, store.selectedProgram)
})

const dimensionLq = computed(() => {
  if (!selected.value) return 0
  return store.lqDimension(selected.value, store.selectedDimension)
})

const activeLq = computed(() => {
  return store.concentrationType === 'bantuan'
    ? programLq.value
    : dimensionLq.value
})

const activeConcentrationData = computed(() => {
  if (!selected.value) return null

  const kodeList = [
    selected.value.kodeWilayah,
    selected.value.kodeKelurahanDesa,
    selected.value.kodeKecamatan,
    selected.value.kodeKabkot,
    selected.value.kodeProvinsi
  ]

  for (const kodeValue of kodeList) {
    const kode = normalizeKode(kodeValue)
    const match = store.concentrationByKode.get(kode)

    if (match) {
      return match
    }
  }

  return null
})

const selectedFocusLabel = computed(() => {
  if (store.concentrationType === 'bantuan') {
    return `Rekomendasi ${selectedProgramLabel.value} ${selectedShortName.value}`
  }

  return `Deprivasi ${selectedDimensionLabel.value} ${selectedShortName.value}`
})

const selectedTotalLabel = computed(() => {
  if (store.concentrationType === 'bantuan') {
    return `Total Rekomendasi ${selectedShortName.value}`
  }

  return `Total Deprivasi ${selectedShortName.value}`
})

const levelAcuanLabel = computed(() => {
  if (store.wilayahLevel === 'provinsi') return 'provinsi'
  if (store.wilayahLevel === 'kabupaten') return 'kab/kota'
  if (store.wilayahLevel === 'kecamatan') return 'kecamatan'
  if (store.wilayahLevel === 'desa') return 'desa/kelurahan'

  return 'wilayah'
})

const wilayahAcuanText = computed(() => {
  const levelLabel = levelAcuanLabel.value

  if (store.concentrationReference === 'nasional') {
    return `seluruh ${levelLabel} nasional`
  }

  if (store.concentrationReference === 'provinsi') {
    return `seluruh ${levelLabel} di provinsi yang sama`
  }

  if (store.concentrationReference === 'kabupaten') {
    return `seluruh ${levelLabel} di kabupaten/kota yang sama`
  }

  if (store.concentrationReference === 'kecamatan') {
    return `seluruh ${levelLabel} di kecamatan yang sama`
  }

  return `seluruh ${levelLabel}`
})

const acuanFocusLabel = computed(() => {
  if (store.concentrationType === 'bantuan') {
    return `Rekomendasi ${selectedProgramLabel.value} ${wilayahAcuanText.value}`
  }

  return `Deprivasi ${selectedDimensionLabel.value} ${wilayahAcuanText.value}`
})

const acuanTotalLabel = computed(() => {
  if (store.concentrationType === 'bantuan') {
    return `Total Rekomendasi ${wilayahAcuanText.value}`
  }

  return `Total Deprivasi ${wilayahAcuanText.value}`
})

const acuanJumlahTerpilih = computed(() => {
  return Number(activeConcentrationData.value?.jumlahAcuanTerpilih || 0)
})

const acuanTotalPembanding = computed(() => {
  return Number(activeConcentrationData.value?.totalAcuanPembanding || 0)
})

const lqInterpretation = computed(() => {
  const namaWilayah = selectedTitle.value || 'Wilayah terpilih'

  const fokus =
    store.concentrationType === 'bantuan'
      ? selectedProgramLabel.value
      : selectedDimensionLabel.value

  const objek =
    store.concentrationType === 'bantuan'
      ? `rekomendasi ${fokus}`
      : `deprivasi ${fokus}`

  const pembanding = `pola umum ${wilayahAcuanText.value}`

  if (activeLq.value >= 1.25) {
    return `${namaWilayah} memiliki kecenderungan ${objek} yang jauh lebih tinggi dibandingkan ${pembanding}.`
  }

  if (activeLq.value >= 1.05) {
    return `${namaWilayah} memiliki kecenderungan ${objek} yang lebih tinggi dibandingkan ${pembanding}.`
  }

  if (activeLq.value >= 0.95) {
    return `${namaWilayah} memiliki kecenderungan ${objek} yang relatif setara dengan ${pembanding}.`
  }

  if (activeLq.value >= 0.75) {
    return `${namaWilayah} memiliki kecenderungan ${objek} yang lebih rendah dibandingkan ${pembanding}.`
  }

  return `${namaWilayah} memiliki kecenderungan ${objek} yang jauh lebih rendah dibandingkan ${pembanding}.`
})

const activeDimensionCount = computed(() => {
  if (!selected.value) {
    return { total: 0, indikator: {} }
  }

  return selected.value.deprivasiCount?.[store.selectedDimension] || {
    total: 0,
    indikator: {}
  }
})

const activeDimensionIndicators = computed(() => {
  return Object.entries(activeDimensionCount.value.indikator || {})
    .map(([key, value]) => ({
      key,
      label: formatIndicatorLabel(key),
      value: Number(value || 0)
    }))
    .sort((a, b) => b.value - a.value)
})

const activeProgramIndicators = computed(() => {
  if (!selected.value) return []

  const indicatorData = selected.value.bantuanIndikator?.[store.selectedProgram] || {}

  return Object.entries(indicatorData)
    .map(([key, value]) => ({
      key,
      label: formatIndicatorLabel(key),
      value: Number(value || 0)
    }))
    .sort((a, b) => b.value - a.value)
})

const activeIndicatorRows = computed(() => {
  if (store.concentrationType === 'bantuan') {
    return activeProgramIndicators.value
  }

  return activeDimensionIndicators.value
})

const indicatorEmptyText = computed(() => {
  const wilayah = selectedTitle.value || 'wilayah ini'

  if (store.concentrationType === 'bantuan') {
    const bantuan = selectedProgramLabel.value || 'bantuan ini'
    return `Tidak ada rekomendasi ${bantuan} di ${wilayah}.`
  }

  const dimensi = selectedDimensionLabel.value || 'dimensi ini'
  return `Tidak ada deprivasi ${dimensi} di ${wilayah}.`
})

function normalizeKode(value) {
  return String(value || '')
    .trim()
    .replace(/\./g, '')
}

function toggleDropdown(type) {
  openDropdown.value = openDropdown.value === type ? null : type
}

function selectFirstSearchResult() {
  const firstResult = store.filteredMapWilayah[0]

  if (!firstResult) return

  store.setSelectedMapWilayah(firstResult)

  if (store.activeMenu === 'peta_rekomendasi') {
    const allowedValues = recommendationMapReferenceOptions.value.map((item) => item.value)

    if (allowedValues.length === 0) {
      store.setRecommendationMapFollowReference(false)
      return
    }

    if (!allowedValues.includes(store.recommendationMapReference)) {
      store.setRecommendationMapReference(allowedValues[0])
    }

    store.setRecommendationMapFollowReference(true)
  }
}

function resetRecommendationFilter() {
  store.resetRecommendationFilter()

  if (store.activeMenu === 'peta_rekomendasi') {
    store.setRecommendationMapFollowReference(false)
    store.setSelectedMapWilayah(null)
  }

  openDropdown.value = null
}

function getDefaultRecommendationDetailLevel(level) {
  if (level === 'provinsi') return 'kabupaten'
  if (level === 'kabupaten') return 'kecamatan'
  if (level === 'kecamatan') return 'desa'

  return ''
}

function openRegionDetailTableFromPanel() {
  const wilayah = selected.value
  if (!wilayah) return

  const allowedValues = recommendationMapReferenceOptions.value.map((item) => item.value)

  const detailLevel = allowedValues.includes(store.recommendationMapReference)
    ? store.recommendationMapReference
    : getDefaultRecommendationDetailLevel(activeWilayahLevel.value)

  if (!detailLevel) return

  store.openRegionDetailTableForWilayah(wilayah, detailLevel)
}

function openFamilyTableFromPanel() {
  store.openFamilyTableForWilayah(selected.value)
}

async function loadSelectedRegionSummary() {
  const wilayah = selected.value

  selectedRegionSummary.value = null
  selectedRegionSummaryLoading.value = false
  store.setMapPanelLoading(false)

  if (!wilayah || store.activeMenu !== 'peta_rekomendasi') {
    return
  }

  if (recommendationDataFilterActive.value) {
    return
  }

  const requestId = selectedSummaryRequestId + 1
  selectedSummaryRequestId = requestId
  selectedRegionSummaryLoading.value = true
  store.setMapPanelLoading(true)

  const params = new URLSearchParams()

  if (wilayah.kodeProvinsi) {
    params.append('kode_provinsi', wilayah.kodeProvinsi)
  }

  if (wilayah.kodeKabkot) {
    params.append('kode_kabkot', wilayah.kodeKabkot)
  }

  if (wilayah.kodeKecamatan) {
    params.append('kode_kecamatan', wilayah.kodeKecamatan)
  }

  if (wilayah.kodeKelurahanDesa) {
    params.append('kode_kelurahan_desa', wilayah.kodeKelurahanDesa)
  }

  try {
    const url = `${API_BASE}/summary${params.toString() ? `?${params.toString()}` : ''}`
    const response = await fetch(url)

    if (!response.ok) {
      throw new Error('Gagal mengambil ringkasan wilayah')
    }

    const data = await response.json()

    if (requestId !== selectedSummaryRequestId) return

    selectedRegionSummary.value = {
      totalKeluarga: Number(data.totalKeluarga || 0),
      totalRekomendasi: Number(data.totalRekomendasi || 0),

      keluargaDesil15: Number(data.keluargaDesil15 || 0),
      rekomendasiDesil15: Number(data.rekomendasiDesil15 || 0),

      keluargaDesil610: Number(data.keluargaDesil610 || 0),
      rekomendasiDesil610: Number(data.rekomendasiDesil610 || 0)
    }
  } catch (error) {
    console.error(error)
    selectedRegionSummary.value = null
  } finally {
    if (requestId === selectedSummaryRequestId) {
      selectedRegionSummaryLoading.value = false
      store.setMapPanelLoading(false)
    }
  }
}

function closeDropdown(e) {
  const target = e.target

  if (!(target instanceof Element)) {
    return
  }

  if (!target.closest('.dropdown')) {
    openDropdown.value = null
  }
}

function formatNumber(value) {
  return new Intl.NumberFormat('id-ID').format(value || 0)
}

function formatLq(value) {
  return Number(value || 0).toFixed(2)
}

function formatIndicatorLabel(value) {
  return String(value)
    .replaceAll('_', ' ')
    .replace(/\b\w/g, (char) => char.toUpperCase())
}

function getLqClass(value) {
  if (value >= 1.25) return 'lq-high'
  if (value >= 1.05) return 'lq-mid-high'
  if (value >= 0.95) return 'lq-mid'
  if (value >= 0.75) return 'lq-mid-low'
  return 'lq-low'
}

watch(
  () => [
    store.activeMenu,
    store.wilayahLevel,
    selected.value?.kodeWilayah,
    selected.value?.kodeProvinsi,
    selected.value?.kodeKabkot,
    selected.value?.kodeKecamatan,
    selected.value?.kodeKelurahanDesa,
    store.desil.join(','),
    store.prioritas.join(','),
    store.asesmen.join(',')
  ],
  () => {
    loadSelectedRegionSummary()
  },
  { immediate: true }
)

onMounted(() => {
  document.addEventListener('click', closeDropdown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeDropdown)
})
</script>
