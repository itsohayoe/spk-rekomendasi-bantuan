<template>
  <section class="page-section">
    <div class="home-hero">
      <div class="home-hero-main">
        <span class="eyebrow light">Dashboard Spasial DTSEN</span>

        <h1>Sistem Rekomendasi Bantuan Sosial Berbasis Deprivasi Multidimensi</h1>

        <div class="home-hero-actions">
          <button class="hero-button primary" @click="store.setMenu('peta_konsentrasi')">
            Peta Konsentrasi
          </button>

          <button class="hero-button secondary" @click="store.setMenu('peta_rekomendasi')">
            Peta Rekomendasi
          </button>
        </div>
      </div>

      <div class="home-scope-card">
        <h3>Ruang Lingkup Dashboard</h3>

        <div class="scope-row">
          <span>Program bantuan</span>
          <p>PIP, PKH, PBI JK, ATENSI, SEMBAKO, BLT, dan BSPS</p>
        </div>

        <div class="scope-row">
          <span>Dimensi deprivasi</span>
          <p>Pendidikan, kesehatan, perumahan, dan ekonomi</p>
        </div>

      </div>
    </div>

    <div class="home-summary-grid">
      <div class="summary-card">
        <div class="summary-header">
          <div>
            <h2>Kelompok Desil 1–5</h2>
            <p>Kelompok rekomendasi yang dapat diprioritaskan langsung.</p>
          </div>

          <div class="summary-filter-actions">
            <button
              v-if="isD15FilterActive || isD15PrioritasActive"
              class="summary-filter-reset"
              @click="resetD15Filter"
            >
              Reset
            </button>

            <div class="summary-filter-dropdown">
              <button
                class="summary-filter-button"
                :class="{ active: isD15FilterActive }"
                @click.stop="toggleSummaryDropdown('d15-desil')"
              >
                Desil
              </button>

              <div
                v-if="summaryDropdown === 'd15-desil'"
                class="summary-filter-menu"
                @click.stop
              >
                <label v-for="item in desil15Options" :key="item">
                  <input type="checkbox" v-model="d15DesilFilter" :value="item" />
                  Desil {{ item }}
                </label>
              </div>
            </div>

            <div class="summary-filter-dropdown">
              <button
                class="summary-filter-button"
                :class="{ active: isD15PrioritasActive }"
                @click.stop="toggleSummaryDropdown('d15-prioritas')"
              >
                Prioritas
              </button>

              <div
                v-if="summaryDropdown === 'd15-prioritas'"
                class="summary-filter-menu"
                @click.stop
              >
                <label v-for="item in prioritasOptions" :key="item">
                  <input type="checkbox" v-model="d15PrioritasFilter" :value="item" />
                  {{ item }}
                </label>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="isD15Loading || dashboardLoading"
          class="inline-spinner-wrap"
        >
          <div class="inline-spinner"></div>
        </div>

        <template v-else>
          <div class="group-stat-grid">
            <div class="group-stat-card">
              <span>Total Keluarga</span>
              <strong>{{ formatNumber(summaryD15.keluarga) }}</strong>
            </div>

            <div class="group-stat-card">
              <span>Total Rekomendasi</span>
              <strong>{{ formatNumber(summaryD15.rekomendasi) }}</strong>
            </div>
          </div>

          <div class="program-summary">
            <div
              v-for="program in programSummaryD15"
              :key="program.key"
              class="program-summary-row"
            >
              <div class="program-summary-top">
                <span>{{ program.label }}</span>
                <strong>{{ formatNumber(program.value) }}</strong>
              </div>

              <div class="program-summary-bar">
                <div :style="{ width: `${program.percent}%` }"></div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div class="summary-card">
        <div class="summary-header">
          <div>
            <h2>Kelompok Desil 6–10</h2>
            <p>Kelompok rekomendasi yang memerlukan asesmen lanjutan.</p>
          </div>

          <div class="summary-filter-actions">
            <button
              v-if="isD610FilterActive || isD610PrioritasActive"
              class="summary-filter-reset"
              @click="resetD610Filter"
            >
              Reset
            </button>

            <div class="summary-filter-dropdown">
              <button
                class="summary-filter-button"
                :class="{ active: isD610FilterActive }"
                @click.stop="toggleSummaryDropdown('d610-desil')"
              >
                Desil
              </button>

              <div
                v-if="summaryDropdown === 'd610-desil'"
                class="summary-filter-menu"
                @click.stop
              >
                <label v-for="item in desil610Options" :key="item">
                  <input type="checkbox" v-model="d610DesilFilter" :value="item" />
                  Desil {{ item }}
                </label>
              </div>
            </div>

            <div class="summary-filter-dropdown">
              <button
                class="summary-filter-button"
                :class="{ active: isD610PrioritasActive }"
                @click.stop="toggleSummaryDropdown('d610-prioritas')"
              >
                Prioritas
              </button>

              <div
                v-if="summaryDropdown === 'd610-prioritas'"
                class="summary-filter-menu"
                @click.stop
              >
                <label v-for="item in prioritasOptions" :key="item">
                  <input type="checkbox" v-model="d610PrioritasFilter" :value="item" />
                  {{ item }}
                </label>
              </div>
            </div>
          </div>
        </div>

        <div
          v-if="isD610Loading || dashboardLoading"
          class="inline-spinner-wrap"
        >
          <div class="inline-spinner"></div>
        </div>

        <template v-else>
          <div class="group-stat-grid">
            <div class="group-stat-card">
              <span>Total Keluarga</span>
              <strong>{{ formatNumber(summaryD610.keluarga) }}</strong>
            </div>

            <div class="group-stat-card">
              <span>Total Rekomendasi</span>
              <strong>{{ formatNumber(summaryD610.rekomendasi) }}</strong>
            </div>
          </div>

          <div class="program-summary">
            <div
              v-for="program in programSummaryD610"
              :key="program.key"
              class="program-summary-row"
            >
              <div class="program-summary-top">
                <span>{{ program.label }}</span>
                <strong>{{ formatNumber(program.value) }}</strong>
              </div>

              <div class="program-summary-bar">
                <div :style="{ width: `${program.percent}%` }"></div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useStore } from '../store/store'

const store = useStore()

const API_BASE_URL = 'https://backend-nasional-production.up.railway.app/api'

const summaryDropdown = ref(null)

const desil15Options = [1, 2, 3, 4, 5]
const desil610Options = [6, 7, 8, 9, 10]
const prioritasOptions = [
  'Sangat Tinggi',
  'Tinggi',
  'Sedang',
  'Rendah',
  'Sangat Rendah'
]

const d15DesilFilter = ref([])
const d15PrioritasFilter = ref([])

const d610DesilFilter = ref([])
const d610PrioritasFilter = ref([])

const isD15Loading = ref(false)
const isD610Loading = ref(false)
const dashboardCardsReady = ref(false)

const dashboardLoading = computed(() => {
  return (
    store.isSummaryLoading ||
    !store.isDashboardReady ||
    !dashboardCardsReady.value
  )
})

const summaryD15 = ref({
  keluarga: 0,
  rekomendasi: 0,
  programTotals: {}
})

const summaryD610 = ref({
  keluarga: 0,
  rekomendasi: 0,
  programTotals: {}
})

const isD15FilterActive = computed(() => d15DesilFilter.value.length > 0)
const isD15PrioritasActive = computed(() => d15PrioritasFilter.value.length > 0)

const isD610FilterActive = computed(() => d610DesilFilter.value.length > 0)
const isD610PrioritasActive = computed(() => d610PrioritasFilter.value.length > 0)

const programSummaryD15 = computed(() => {
  const values = summaryD15.value.programTotals || {}
  const maxValue = Math.max(...store.programList.map((program) => Number(values[program.key] || 0)), 0)

  return store.programList.map((program) => ({
    ...program,
    value: Number(values[program.key] || 0),
    percent: maxValue > 0 ? (Number(values[program.key] || 0) / maxValue) * 100 : 0
  }))
})

const programSummaryD610 = computed(() => {
  const values = summaryD610.value.programTotals || {}
  const maxValue = Math.max(...store.programList.map((program) => Number(values[program.key] || 0)), 0)

  return store.programList.map((program) => ({
    ...program,
    value: Number(values[program.key] || 0),
    percent: maxValue > 0 ? (Number(values[program.key] || 0) / maxValue) * 100 : 0
  }))
})

onMounted(async () => {
  document.addEventListener('click', closeSummaryDropdown)

  dashboardCardsReady.value = false

  if (!store.summaryLoaded) {
    await store.fetchSummary()
  }

  setDefaultSummaryFromStore()
  dashboardCardsReady.value = true
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeSummaryDropdown)
})

watch(
  [d15DesilFilter, d15PrioritasFilter],
  () => {
    fetchSummaryCard('d15')
  },
  { deep: true }
)

watch(
  [d610DesilFilter, d610PrioritasFilter],
  () => {
    fetchSummaryCard('d610')
  },
  { deep: true }
)

function setDefaultSummaryFromStore() {
  summaryD15.value = {
    keluarga: store.keluargaDesil15,
    rekomendasi: store.rekomendasiDesil15,
    programTotals: { ...store.programTotalsD15 }
  }

  summaryD610.value = {
    keluarga: store.keluargaDesil610,
    rekomendasi: store.rekomendasiDesil610,
    programTotals: { ...store.programTotalsD610 }
  }
}

async function fetchSummaryCard(group) {
  const isD15 = group === 'd15'
  const defaultDesil = isD15 ? desil15Options : desil610Options
  const selectedDesil = isD15 ? d15DesilFilter.value : d610DesilFilter.value
  const selectedPrioritas = isD15 ? d15PrioritasFilter.value : d610PrioritasFilter.value

  const hasCustomFilter = selectedDesil.length > 0 || selectedPrioritas.length > 0

  if (!hasCustomFilter) {
    dashboardCardsReady.value = false

    if (!store.summaryLoaded) {
      await store.fetchSummary()
    }

    if (isD15) {
      summaryD15.value = {
        keluarga: store.keluargaDesil15,
        rekomendasi: store.rekomendasiDesil15,
        programTotals: { ...store.programTotalsD15 }
      }
    } else {
      summaryD610.value = {
        keluarga: store.keluargaDesil610,
        rekomendasi: store.rekomendasiDesil610,
        programTotals: { ...store.programTotalsD610 }
      }
    }

    dashboardCardsReady.value = true
    return
  }

  const desil = selectedDesil.length > 0 ? selectedDesil : defaultDesil

  const params = new URLSearchParams()
  
  desil.forEach((item) => {
    params.append('desil', item)
  })

  selectedPrioritas.forEach((item) => {
    params.append('prioritas', item)
  })

  try {
    if (isD15) {
      isD15Loading.value = true
    } else {
      isD610Loading.value = true
    }

    const response = await fetch(`${API_BASE_URL}/summary?${params.toString()}`)

    if (!response.ok) {
      throw new Error('Gagal mengambil ringkasan')
    }

    const data = await response.json()
    const mapped = mapSummaryResponse(data, isD15)

    if (isD15) {
      summaryD15.value = mapped
    } else {
      summaryD610.value = mapped
    }
  } catch (error) {
    console.error(error)
    setDefaultSummaryFromStore()
  } finally {
    if (isD15) {
      isD15Loading.value = false
    } else {
      isD610Loading.value = false
    }
  }
}

function mapSummaryResponse(data, isD15) {

  const programTotals =
    data.programTotals ||
    (
      isD15
        ? data.programTotalsD15
        : data.programTotalsD610
    ) ||
    data.program ||
    {}

  return {

    keluarga:
      (
        isD15
          ? data.keluargaDesil15
          : data.keluargaDesil610
      ) ??
      data.keluarga ??
      data.totalKeluarga ??
      0,

    rekomendasi:
      (
        isD15
          ? data.rekomendasiDesil15
          : data.rekomendasiDesil610
      ) ??
      data.rekomendasi ??
      data.totalRekomendasi ??
      0,

    programTotals: normalizeProgramTotals(programTotals)
  }
}

function normalizeProgramTotals(programTotals) {
  return {
    pip: Number(programTotals.pip || programTotals.PIP || 0),
    pkh: Number(programTotals.pkh || programTotals.PKH || 0),
    pbi_jk: Number(programTotals.pbi_jk || programTotals.pbiJk || programTotals['PBI JK'] || 0),
    atensi: Number(programTotals.atensi || programTotals.ATENSI || 0),
    sembako: Number(programTotals.sembako || programTotals.SEMBAKO || 0),
    blt: Number(programTotals.blt || programTotals.BLT || 0),
    bsps: Number(programTotals.bsps || programTotals.BSPS || 0)
  }
}

function toggleSummaryDropdown(name) {
  summaryDropdown.value = summaryDropdown.value === name ? null : name
}

function closeSummaryDropdown(event) {
  if (!event.target.closest('.summary-filter-dropdown')) {
    summaryDropdown.value = null
  }
}

function resetD15Filter() {
  d15DesilFilter.value = []
  d15PrioritasFilter.value = []
  summaryDropdown.value = null
}

function resetD610Filter() {
  d610DesilFilter.value = []
  d610PrioritasFilter.value = []
  summaryDropdown.value = null
}

function formatNumber(value) {
  return new Intl.NumberFormat('id-ID').format(Number(value || 0))
}
</script>
