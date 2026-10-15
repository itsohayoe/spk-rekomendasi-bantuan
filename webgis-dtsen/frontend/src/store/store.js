import { defineStore } from 'pinia'

const API_BASE = 'https://backend-nasional-production.up.railway.app/api'

const VALID_MENUS = [
  'beranda',
  'peta_konsentrasi',
  'peta_rekomendasi',
  'tabel_rekomendasi'
]

const VALID_TABLE_MODES = ['wilayah', 'keluarga']
const VALID_WILAYAH_LEVELS = ['provinsi', 'kabupaten', 'kecamatan', 'desa']

function getSavedMenu() {
  const saved = localStorage.getItem('webgis_active_menu')
  return VALID_MENUS.includes(saved) ? saved : 'beranda'
}

function getSavedTableMode() {
  const saved = localStorage.getItem('webgis_table_mode')
  return VALID_TABLE_MODES.includes(saved) ? saved : 'wilayah'
}

function getSavedWilayahLevel() {
  const saved = localStorage.getItem('webgis_wilayah_level')
  return VALID_WILAYAH_LEVELS.includes(saved) ? saved : 'kabupaten'
}

function getSavedMapLevel(menu) {
  const key =
    menu === 'peta_konsentrasi'
      ? 'webgis_peta_konsentrasi_level'
      : menu === 'peta_rekomendasi'
        ? 'webgis_peta_rekomendasi_level'
        : 'webgis_wilayah_level'

  const saved = localStorage.getItem(key)
  return VALID_WILAYAH_LEVELS.includes(saved) ? saved : 'kabupaten'
}

function getSavedConcentrationReference() {
  const saved = localStorage.getItem('webgis_concentration_reference')
  return ['nasional', 'provinsi', 'kabupaten', 'kecamatan'].includes(saved)
    ? saved
    : 'nasional'
}

function getSavedConcentrationMapFollowReference() {
  return localStorage.getItem('webgis_concentration_map_follow_reference') === 'true'
}

function getSavedRecommendationMapFollowReference() {
  return localStorage.getItem('webgis_recommendation_map_follow_reference') === 'true'
}

function getSavedRecommendationMapReference() {
  const saved = localStorage.getItem('webgis_recommendation_map_reference')

  return ['kabupaten', 'kecamatan', 'desa'].includes(saved)
    ? saved
    : 'kabupaten'
}

function getSavedConcentrationType() {
  const saved = localStorage.getItem('webgis_concentration_type')
  return ['bantuan', 'dimensi'].includes(saved) ? saved : 'bantuan'
}


function getSavedSelectedProgram() {
  return localStorage.getItem('webgis_selected_program') || 'pbi_jk'
}

function getSavedSelectedDimension() {
  return localStorage.getItem('webgis_selected_dimension') || 'kesehatan'
}

export const useStore = defineStore('main', {
  state: () => ({
    activeMenu: getSavedMenu(),

    wilayahLevel: getSavedMapLevel(getSavedMenu()),
    petaRekomendasiLevel: getSavedMapLevel('peta_rekomendasi'),
    petaKonsentrasiLevel: getSavedMapLevel('peta_konsentrasi'),

    petaRekomendasiMapKey: '',
    petaKonsentrasiMapKey: '',

    petaRekomendasiLoadedLevel: '',
    petaKonsentrasiLoadedLevel: '',

    selectedWilayah: null,
    selectedMapWilayah: null,
    selectedPetaKonsentrasiWilayah: null,
    selectedPetaRekomendasiWilayah: null,

    selectedProgram: getSavedSelectedProgram(),
    selectedDimension: getSavedSelectedDimension(),
    concentrationType: getSavedConcentrationType(),
    concentrationReference: getSavedConcentrationReference(),
    concentrationMapFollowReference: getSavedConcentrationMapFollowReference(),

    recommendationMapFollowReference: getSavedRecommendationMapFollowReference(),
    recommendationMapReference: getSavedRecommendationMapReference(),

    searchKeyword: '',
    desil: [],
    prioritas: [],
    asesmen: [],

    tableMode: getSavedTableMode(),
    familyWilayahKeyword: '',
    familyOpenRequestId: 0,
    familyOpenSource: '',

    tableRegionParentWilayah: null,
    tableRegionDetailLevel: '',
    tableRegionOpenRequestId: 0,

    isLoading: false,
    isSummaryLoading: false,
    isAreaLoading: false,
    isTableAreaLoading: false,
    isRecommendationLoading: false,
    isConcentrationLoading: false,
    isMapRendering: false,
    isMapPanelLoading: false,
    isDashboardReady: false,
    summaryLoaded: false,
    errorMessage: '',

    areaLoaded: false,
    areaLoadedLevel: '',
    areaRequestId: 0,
    petaRekomendasiAreaRequestId: 0,
    petaKonsentrasiAreaRequestId: 0,
    tableAreaRequestId: 0,
    concentrationLoaded: false,

    recommendationLimit: 25,
    recommendationOffset: 0,
    recommendationTotalRows: 0,
    recommendationRequestId: 0,

    summaryTotals: {
      totalKeluarga: 0,
      keluargaDesil15: 0,
      keluargaDesil610: 0,
      rekomendasiDesil15: 0,
      rekomendasiDesil610: 0
    },

    programTotalsD15: {
      pip: 0,
      pkh: 0,
      pbi_jk: 0,
      atensi: 0,
      sembako: 0,
      blt: 0,
      bsps: 0
    },

    programTotalsD610: {
      pip: 0,
      pkh: 0,
      pbi_jk: 0,
      atensi: 0,
      sembako: 0,
      blt: 0,
      bsps: 0
    },

    dimensionList: [
      { key: 'pendidikan', label: 'Pendidikan' },
      { key: 'kesehatan', label: 'Kesehatan' },
      { key: 'perumahan', label: 'Perumahan' },
      { key: 'ekonomi', label: 'Ekonomi' }
    ],

    programList: [
      { key: 'pip', label: 'PIP' },
      { key: 'pkh', label: 'PKH' },
      { key: 'pbi_jk', label: 'PBI JK' },
      { key: 'atensi', label: 'ATENSI' },
      { key: 'sembako', label: 'SEMBAKO' },
      { key: 'blt', label: 'BLT' },
      { key: 'bsps', label: 'BSPS' }
    ],

    wilayahData: [],

    petaRekomendasiWilayahData: [],
    petaKonsentrasiWilayahData: [],

    tableWilayahData: [],
    keluargaData: [],
    concentrationData: []
  }),

  getters: {
    filteredWilayah(state) {
      let result = state.wilayahData

      if (state.searchKeyword.trim() !== '') {
        const keyword = state.searchKeyword.toLowerCase()

        result = result.filter((item) => {
          return (
            safeLower(item.nama).includes(keyword) ||
            safeLower(item.namaWilayah).includes(keyword) ||
            safeLower(item.kabupaten).includes(keyword) ||
            safeLower(item.provinsi).includes(keyword) ||
            safeLower(item.kecamatan).includes(keyword) ||
            safeLower(item.desa).includes(keyword) ||
            safeLower(item.kodeWilayah).includes(keyword) ||
            safeLower(item.kodeProvinsi).includes(keyword) ||
            safeLower(item.kodeKabkot).includes(keyword) ||
            safeLower(item.kodeKecamatan).includes(keyword) ||
            safeLower(item.kodeKelurahanDesa).includes(keyword)
          )
        })
      }

      return result
    },

    activeMapLevel(state) {
      if (state.activeMenu === 'peta_konsentrasi') {
        return state.petaKonsentrasiLevel
      }

      if (state.activeMenu === 'peta_rekomendasi') {
        return state.petaRekomendasiLevel
      }

      return state.wilayahLevel
    },

    activeMapWilayahData(state) {
      if (state.activeMenu === 'peta_konsentrasi') {
        return state.petaKonsentrasiWilayahData
      }

      if (state.activeMenu === 'peta_rekomendasi') {
        return state.petaRekomendasiWilayahData
      }

      return state.wilayahData
    },

    filteredMapWilayah(state) {
      let result =
        state.activeMenu === 'peta_konsentrasi'
          ? state.petaKonsentrasiWilayahData
          : state.activeMenu === 'peta_rekomendasi'
            ? state.petaRekomendasiWilayahData
            : state.wilayahData

      if (state.searchKeyword.trim() !== '') {
        const keyword = state.searchKeyword.toLowerCase()

        result = result.filter((item) => {
          return (
            safeLower(item.nama).includes(keyword) ||
            safeLower(item.namaWilayah).includes(keyword) ||
            safeLower(item.kabupaten).includes(keyword) ||
            safeLower(item.provinsi).includes(keyword) ||
            safeLower(item.kecamatan).includes(keyword) ||
            safeLower(item.desa).includes(keyword) ||
            safeLower(item.kodeWilayah).includes(keyword) ||
            safeLower(item.kodeProvinsi).includes(keyword) ||
            safeLower(item.kodeKabkot).includes(keyword) ||
            safeLower(item.kodeKecamatan).includes(keyword) ||
            safeLower(item.kodeKelurahanDesa).includes(keyword)
          )
        })
      }

      return result
    },

    concentrationByKode(state) {
      const result = new Map()

      ;(state.concentrationData || []).forEach((item) => {
        const kodeList = [
          item.kodeWilayah,
          item.kodeKelurahanDesa,
          item.kodeKecamatan,
          item.kodeKabkot,
          item.kodeProvinsi
        ]

        kodeList.forEach((kodeValue) => {
          const kode = normalizeKode(kodeValue)

          if (kode) {
            result.set(kode, item)
          }
        })
      })

      return result
    },

    totalKeluarga(state) {
      return Number(state.summaryTotals.totalKeluarga || 0)
    },

    keluargaDesil15(state) {
      return Number(state.summaryTotals.keluargaDesil15 || 0)
    },

    keluargaDesil610(state) {
      return Number(state.summaryTotals.keluargaDesil610 || 0)
    },

    rekomendasiDesil15(state) {
      return Number(state.summaryTotals.rekomendasiDesil15 || 0)
    },

    rekomendasiDesil610(state) {
      return Number(state.summaryTotals.rekomendasiDesil610 || 0)
    },

    totalRekomendasi(state) {
      return (
        Number(state.summaryTotals.rekomendasiDesil15 || 0) +
        Number(state.summaryTotals.rekomendasiDesil610 || 0)
      )
    },

    lqProgram(state) {
      return (wilayah, programKey) => {
        const kodeList = [
          wilayah.kodeWilayah,
          wilayah.kodeKelurahanDesa,
          wilayah.kodeKecamatan,
          wilayah.kodeKabkot,
          wilayah.kodeProvinsi
        ]

        for (const kodeValue of kodeList) {
          const kode = normalizeKode(kodeValue)
          const match = this.concentrationByKode.get(kode)

          if (match) {
            return Number(match.nilai || 0)
          }
        }

        return 0
      }
    },

    lqDimension(state) {
      return (wilayah, dimensionKey) => {
        const kodeList = [
          wilayah.kodeWilayah,
          wilayah.kodeKelurahanDesa,
          wilayah.kodeKecamatan,
          wilayah.kodeKabkot,
          wilayah.kodeProvinsi
        ]

        for (const kodeValue of kodeList) {
          const kode = normalizeKode(kodeValue)
          const match = this.concentrationByKode.get(kode)

          if (match) {
            return Number(match.nilai || 0)
          }
        }

        return 0
      }
    }
  },

  actions: {
    setMenu(menu) {
      if (!VALID_MENUS.includes(menu)) return

      this.saveCurrentMapLevel()

      this.activeMenu = menu
      localStorage.setItem('webgis_active_menu', menu)

      if (menu === 'peta_rekomendasi') {
        this.wilayahLevel = this.petaRekomendasiLevel
        localStorage.setItem('webgis_wilayah_level', this.wilayahLevel)
        this.selectedMapWilayah = this.selectedPetaRekomendasiWilayah
      }

      if (menu === 'peta_konsentrasi') {
        this.wilayahLevel = this.petaKonsentrasiLevel
        localStorage.setItem('webgis_wilayah_level', this.wilayahLevel)
        this.selectedMapWilayah = this.selectedPetaKonsentrasiWilayah
      }
    },

    saveCurrentMapLevel() {
      if (this.activeMenu === 'peta_rekomendasi') {
        localStorage.setItem('webgis_peta_rekomendasi_level', this.wilayahLevel)
      }

      if (this.activeMenu === 'peta_konsentrasi') {
        localStorage.setItem('webgis_peta_konsentrasi_level', this.wilayahLevel)
      }
    },

    setActiveMapLevel(level) {
      if (!VALID_WILAYAH_LEVELS.includes(level)) return

      this.wilayahLevel = level
      localStorage.setItem('webgis_wilayah_level', level)

      if (this.activeMenu === 'peta_rekomendasi') {
        this.petaRekomendasiLevel = level
        localStorage.setItem('webgis_peta_rekomendasi_level', level)
      }

      if (this.activeMenu === 'peta_konsentrasi') {
        this.petaKonsentrasiLevel = level
        localStorage.setItem('webgis_peta_konsentrasi_level', level)
      }
    },

    setConcentrationReference(value) {
      const validValues = ['nasional', 'provinsi', 'kabupaten', 'kecamatan']

      if (!validValues.includes(value)) return

      this.concentrationReference = value
      localStorage.setItem('webgis_concentration_reference', value)
    },

    setConcentrationMapFollowReference(value) {
      this.concentrationMapFollowReference = Boolean(value)

      localStorage.setItem(
        'webgis_concentration_map_follow_reference',
        String(this.concentrationMapFollowReference)
      )
    },

    setRecommendationMapFollowReference(value) {
      this.recommendationMapFollowReference = Boolean(value)

      localStorage.setItem(
        'webgis_recommendation_map_follow_reference',
        String(this.recommendationMapFollowReference)
      )
    },

    setRecommendationMapReference(value) {
      const validValues = ['kabupaten', 'kecamatan', 'desa']

      if (!validValues.includes(value)) return

      this.recommendationMapReference = value
      localStorage.setItem('webgis_recommendation_map_reference', value)
    },

    setTableMode(mode) {
      if (!VALID_TABLE_MODES.includes(mode)) return

      this.tableMode = mode
      localStorage.setItem('webgis_table_mode', mode)
    },

    setSelectedWilayah(wilayah) {
      this.selectedWilayah = wilayah
    },

    setSelectedMapWilayah(wilayah) {
      this.selectedMapWilayah = wilayah

      if (this.activeMenu === 'peta_konsentrasi') {
        this.selectedPetaKonsentrasiWilayah = wilayah
      }

      if (this.activeMenu === 'peta_rekomendasi') {
        this.selectedPetaRekomendasiWilayah = wilayah
      }
    },

    setMapRendering(value) {
      this.isMapRendering = Boolean(value)
    },

    setMapPanelLoading(value) {
      this.isMapPanelLoading = Boolean(value)
    },

    resetRecommendationFilter() {
      this.searchKeyword = ''
      this.desil = []
      this.prioritas = []
      this.asesmen = []
    },

    openFamilyTableForWilayah(wilayah) {
      const sourceMenu = this.activeMenu

      this.saveCurrentMapLevel()

      this.activeMenu = 'tabel_rekomendasi'
      this.tableMode = 'keluarga'
      this.familyWilayahKeyword =
        wilayah?.nama || wilayah?.namaWilayah || wilayah?.kabupaten || wilayah?.kodeKabkot || ''

      this.selectedWilayah = wilayah
      this.recommendationOffset = 0

      this.familyOpenSource = sourceMenu
      this.familyOpenRequestId += 1

      localStorage.setItem('webgis_active_menu', 'tabel_rekomendasi')
      localStorage.setItem('webgis_table_mode', 'keluarga')
    },

    openRegionDetailTableForWilayah(wilayah, detailLevel) {
      if (!wilayah || !detailLevel) return

      this.saveCurrentMapLevel()

      this.activeMenu = 'tabel_rekomendasi'
      this.tableMode = 'wilayah'

      this.tableRegionParentWilayah = wilayah
      this.tableRegionDetailLevel = detailLevel
      this.tableRegionOpenRequestId += 1

      localStorage.setItem('webgis_active_menu', 'tabel_rekomendasi')
      localStorage.setItem('webgis_table_mode', 'wilayah')
    },

    async fetchSummary(options = {}) {
      try {
        this.isDashboardReady = false
        this.isSummaryLoading = true
        this.isLoading = true
        this.errorMessage = ''

        const params = new URLSearchParams()

        const wilayah =
          Object.prototype.hasOwnProperty.call(options, 'wilayah')
            ? options.wilayah
            : this.selectedWilayah

        if (wilayah?.kodeProvinsi) {
          params.append('kode_provinsi', wilayah.kodeProvinsi)
        }

        if (wilayah?.kodeKabkot) {
          params.append('kode_kabkot', wilayah.kodeKabkot)
        }

        if (wilayah?.kodeKecamatan) {
          params.append('kode_kecamatan', wilayah.kodeKecamatan)
        }

        if (wilayah?.kodeKelurahanDesa) {
          params.append('kode_kelurahan_desa', wilayah.kodeKelurahanDesa)
        }

        ;(options.prioritas ?? this.prioritas)
          .forEach((item) => {
            params.append('prioritas', item)
          })

        ;(options.desil ?? this.desil)
          .forEach((item) => {
            params.append('desil', item)
          })

        const url =
          `${API_BASE}/summary` +
          (params.toString() ? `?${params.toString()}` : '')

        const response = await fetch(url)

        if (!response.ok) {
          throw new Error('Gagal mengambil ringkasan rekomendasi')
        }

        const data = await response.json()

        this.summaryTotals = {
          totalKeluarga: Number(data.totalKeluarga || 0),

          keluargaDesil15: Number(data.keluargaDesil15 || 0),
          keluargaDesil610: Number(data.keluargaDesil610 || 0),

          rekomendasiDesil15: Number(data.rekomendasiDesil15 || 0),
          rekomendasiDesil610: Number(data.rekomendasiDesil610 || 0)
        }

        this.programTotalsD15 = normalizeProgramTotals(
          data.programTotalsD15 || {}
        )

        this.programTotalsD610 = normalizeProgramTotals(
          data.programTotalsD610 || {}
        )

        this.isDashboardReady = true
        this.summaryLoaded = true

      } catch (error) {

        this.isDashboardReady = false
        this.summaryLoaded = false
        this.errorMessage = error.message
        console.error(error)

      } finally {

        this.isSummaryLoading = false
        this.isLoading = false
      }
    },

    getAreaRequestIdKey(target) {
      if (target === 'peta_rekomendasi') return 'petaRekomendasiAreaRequestId'
      if (target === 'peta_konsentrasi') return 'petaKonsentrasiAreaRequestId'

      return 'areaRequestId'
    },

    getAreaLoadingKey(target) {
      if (target === 'peta_rekomendasi') return 'isAreaLoading'
      if (target === 'peta_konsentrasi') return 'isAreaLoading'

      return 'isAreaLoading'
    },

    async fetchAreas(options = {}) {
      const requestedLevel = options.level || this.wilayahLevel || getSavedWilayahLevel()
      const target = options.target || this.activeMenu

      const targetData =
        target === 'peta_rekomendasi'
          ? this.petaRekomendasiWilayahData
          : target === 'peta_konsentrasi'
            ? this.petaKonsentrasiWilayahData
            : this.wilayahData

      const targetLoadedLevel =
        target === 'peta_rekomendasi'
          ? this.petaRekomendasiLoadedLevel
          : target === 'peta_konsentrasi'
            ? this.petaKonsentrasiLoadedLevel
            : this.areaLoadedLevel

      if (
        targetData.length > 0 &&
        targetLoadedLevel === requestedLevel &&
        !options.force
      ) {
        return
      }

      const requestIdKey = this.getAreaRequestIdKey(target)
      const requestId = this[requestIdKey] + 1
      this[requestIdKey] = requestId

      try {
        this.isAreaLoading = true
        this.isLoading = true
        this.errorMessage = ''

        const params = new URLSearchParams()

        const wilayah =
          Object.prototype.hasOwnProperty.call(options, 'wilayah')
            ? options.wilayah
            : this.selectedWilayah

        if (wilayah?.kodeProvinsi) {
          params.append('kode_provinsi', wilayah.kodeProvinsi)
        }

        if (wilayah?.kodeKabkot) {
          params.append('kode_kabkot', wilayah.kodeKabkot)
        }

        if (wilayah?.kodeKecamatan) {
          params.append('kode_kecamatan', wilayah.kodeKecamatan)
        }

        if (wilayah?.kodeKelurahanDesa) {
          params.append('kode_kelurahan_desa', wilayah.kodeKelurahanDesa)
        }

        const desilFilter = options.desil ?? this.desil
        const prioritasFilter = options.prioritas ?? this.prioritas
        const asesmenFilter = options.asesmen ?? this.asesmen

        desilFilter.forEach((item) => params.append('desil', item))
        prioritasFilter.forEach((item) => params.append('prioritas', item))
        asesmenFilter.forEach((item) => params.append('asesmen', item))

        params.append('level', requestedLevel)

        const syncLevelState = options.syncLevelState !== false

        if (syncLevelState) {
          this.wilayahLevel = requestedLevel
          localStorage.setItem('webgis_wilayah_level', requestedLevel)

          if (this.activeMenu === 'peta_rekomendasi') {
            this.petaRekomendasiLevel = requestedLevel
            localStorage.setItem('webgis_peta_rekomendasi_level', requestedLevel)
          }

          if (this.activeMenu === 'peta_konsentrasi') {
            this.petaKonsentrasiLevel = requestedLevel
            localStorage.setItem('webgis_peta_konsentrasi_level', requestedLevel)
          }
        }

        const url = `${API_BASE}/areas${params.toString() ? `?${params.toString()}` : ''}`
        const response = await fetch(url)

        if (!response.ok) {
          throw new Error('Gagal mengambil data agregasi wilayah')
        }

        const data = await response.json()
        const rows = normalizeArrayResponse(data)

        if (requestId !== this[requestIdKey]) return

        const mappedRows = rows.map((item, index) => {
          const bantuan = normalizeBantuan(item.bantuan || item)

          return {
            id: item.kodeWilayah || item.kodeKabkot || item.kodeKabupatenKota || index + 1,

            wilayahId:
              item.kodeWilayah ||
              item.kodeKelurahanDesa ||
              item.kodeKecamatan ||
              item.kodeKabkot ||
              item.kodeKabupatenKota ||
              item.kodeProvinsi ||
              String(index + 1),

            namaWilayah:
              item.namaWilayah ||
              item.namaKelurahanDesa ||
              item.namaKecamatan ||
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.namaProvinsi ||
              item.provinsi ||
              item.kodeWilayah ||
              '-',

            nama:
              item.namaWilayah ||
              item.namaKelurahanDesa ||
              item.namaKecamatan ||
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.namaProvinsi ||
              item.provinsi ||
              item.kelurahanDesa ||
              item.kecamatan ||
              item.kabupaten ||
              item.desa ||
              item.kodeWilayah ||
              '-',

            level: item.level || this.wilayahLevel || requestedLevel,

            kabupaten:
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.kabupaten ||
              item.kodeKabkot ||
              item.kodeKabupatenKota ||
              '-',

            provinsi:
              item.namaProvinsi ||
              item.provinsi ||
              item.kodeProvinsi ||
              '-',

            kecamatan:
              item.namaKecamatan ||
              item.kecamatan ||
              item.kodeKecamatan ||
              '-',

            desa:
              item.namaKelurahanDesa ||
              item.kelurahanDesa ||
              item.namaDesa ||
              item.desa ||
              item.kodeKelurahanDesa ||
              '-',

            kodeWilayah: item.kodeWilayah || '',
            kodeProvinsi: item.kodeProvinsi || '',
            kodeKabkot: item.kodeKabkot || item.kodeKabupatenKota || '',
            kodeKecamatan: item.kodeKecamatan || '',
            kodeKelurahanDesa: item.kodeKelurahanDesa || '',

            lat: Number(item.lat || item.latitude || getFallbackLat(index)),
            lng: Number(item.lng || item.longitude || getFallbackLng(index)),

            totalKeluarga: Number(item.totalKeluarga ?? item.total_keluarga ?? 0),

            totalKeluargaWilayah: Number(
              item.totalKeluargaWilayah ??
              item.total_keluarga_wilayah ??
              0
            ),

            totalRekomendasi: Number(item.totalRekomendasi ?? item.total_rekomendasi ?? 0),

            persentaseKeluargaMembutuhkan: Number(
              item.persentaseKeluargaMembutuhkan ??
              item.persentase_keluarga_membutuhkan ??
              0
            ),

            keluargaDesil15: Number(item.keluargaDesil15 || 0),
            keluargaDesil610: Number(item.keluargaDesil610 || 0),
            rekomendasiDesil15: Number(item.rekomendasiDesil15 || 0),
            rekomendasiDesil610: Number(item.rekomendasiDesil610 || 0),

            desilList: normalizeList(item.desilList),
            prioritasList: normalizeList(item.prioritasList),
            asesmenList: normalizeList(item.asesmenList),

            prioritasDominan: item.prioritasDominan || '-',
            desilDominan: item.desilDominan || '-',

            deprivasi: normalizeDeprivasi(item.deprivasi),

            deprivasiCount: {
              pendidikan: item.deprivasiCount?.pendidikan || { total: 0, indikator: {} },
              kesehatan: item.deprivasiCount?.kesehatan || { total: 0, indikator: {} },
              perumahan: item.deprivasiCount?.perumahan || { total: 0, indikator: {} },
              ekonomi: item.deprivasiCount?.ekonomi || { total: 0, indikator: {} }
            },

            bantuanIndikator: {
              pip: item.bantuanIndikator?.pip || {},
              pkh: item.bantuanIndikator?.pkh || {},
              pbi_jk: item.bantuanIndikator?.pbi_jk || {},
              atensi: item.bantuanIndikator?.atensi || {},
              sembako: item.bantuanIndikator?.sembako || {},
              blt: item.bantuanIndikator?.blt || {},
              bsps: item.bantuanIndikator?.bsps || {}
            },

            bantuan
          }
                })

        this.wilayahData = mappedRows

        if (target === 'peta_rekomendasi') {
          this.petaRekomendasiWilayahData = mappedRows
          this.petaRekomendasiLoadedLevel = requestedLevel
        }

        if (target === 'peta_konsentrasi') {
          this.petaKonsentrasiWilayahData = mappedRows
          this.petaKonsentrasiLoadedLevel = requestedLevel
        }

        this.areaLoaded = true
        this.areaLoadedLevel = requestedLevel
      } catch (error) {
        this.errorMessage = error.message
        console.error(error)
      } finally {
        if (requestId === this[requestIdKey]) {
          this.isAreaLoading = false
          this.isLoading = false
        }
      }
    },

    async fetchTableAreas(options = {}) {
      const requestedLevel = options.level || 'kabupaten'

      const requestId = this.tableAreaRequestId + 1
      this.tableAreaRequestId = requestId

      try {
        this.isTableAreaLoading = true
        this.isLoading = true
        this.errorMessage = ''

        const params = new URLSearchParams()

        const wilayah =
          Object.prototype.hasOwnProperty.call(options, 'wilayah')
            ? options.wilayah
            : null

        if (wilayah?.kodeProvinsi) {
          params.append('kode_provinsi', wilayah.kodeProvinsi)
        }

        if (wilayah?.kodeKabkot) {
          params.append('kode_kabkot', wilayah.kodeKabkot)
        }

        if (wilayah?.kodeKecamatan) {
          params.append('kode_kecamatan', wilayah.kodeKecamatan)
        }

        if (wilayah?.kodeKelurahanDesa) {
          params.append('kode_kelurahan_desa', wilayah.kodeKelurahanDesa)
        }

        const desilFilter = options.desil ?? []
        const prioritasFilter = options.prioritas ?? []
        const asesmenFilter = options.asesmen ?? []

        desilFilter.forEach((item) => params.append('desil', item))
        prioritasFilter.forEach((item) => params.append('prioritas', item))
        asesmenFilter.forEach((item) => params.append('asesmen', item))

        params.append('level', requestedLevel)

        const url = `${API_BASE}/areas${params.toString() ? `?${params.toString()}` : ''}`
        const response = await fetch(url)

        if (!response.ok) {
          throw new Error('Gagal mengambil data agregasi wilayah')
        }

        const data = await response.json()
        const rows = normalizeArrayResponse(data)

        if (requestId !== this.tableAreaRequestId) return

        this.tableWilayahData = rows.map((item, index) => {
          const bantuan = normalizeBantuan(item.bantuan || item)

          return {
            id: item.kodeWilayah || item.kodeKabkot || item.kodeKabupatenKota || index + 1,

            wilayahId:
              item.kodeWilayah ||
              item.kodeKelurahanDesa ||
              item.kodeKecamatan ||
              item.kodeKabkot ||
              item.kodeKabupatenKota ||
              item.kodeProvinsi ||
              String(index + 1),

            namaWilayah:
              item.namaWilayah ||
              item.namaKelurahanDesa ||
              item.namaKecamatan ||
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.namaProvinsi ||
              item.provinsi ||
              item.kodeWilayah ||
              '-',

            nama:
              item.namaWilayah ||
              item.namaKelurahanDesa ||
              item.namaKecamatan ||
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.namaProvinsi ||
              item.provinsi ||
              item.kelurahanDesa ||
              item.kecamatan ||
              item.kabupaten ||
              item.desa ||
              item.kodeWilayah ||
              '-',

            level: item.level || requestedLevel,

            kabupaten:
              item.namaKabkot ||
              item.namaKabupatenKota ||
              item.kabupaten ||
              item.kodeKabkot ||
              item.kodeKabupatenKota ||
              '-',

            provinsi:
              item.namaProvinsi ||
              item.provinsi ||
              item.kodeProvinsi ||
              '-',

            kecamatan:
              item.namaKecamatan ||
              item.kecamatan ||
              item.kodeKecamatan ||
              '-',

            desa:
              item.namaKelurahanDesa ||
              item.kelurahanDesa ||
              item.namaDesa ||
              item.desa ||
              item.kodeKelurahanDesa ||
              '-',

            kodeWilayah: item.kodeWilayah || '',
            kodeProvinsi: item.kodeProvinsi || '',
            kodeKabkot: item.kodeKabkot || item.kodeKabupatenKota || '',
            kodeKecamatan: item.kodeKecamatan || '',
            kodeKelurahanDesa: item.kodeKelurahanDesa || '',

            totalKeluarga: Number(item.totalKeluarga ?? item.total_keluarga ?? 0),

            totalKeluargaWilayah: Number(
              item.totalKeluargaWilayah ??
              item.total_keluarga_wilayah ??
              0
            ),

            totalRekomendasi: Number(item.totalRekomendasi ?? item.total_rekomendasi ?? 0),

            persentaseKeluargaMembutuhkan: Number(
              item.persentaseKeluargaMembutuhkan ??
              item.persentase_keluarga_membutuhkan ??
              0
            ),

            keluargaDesil15: Number(item.keluargaDesil15 || 0),
            keluargaDesil610: Number(item.keluargaDesil610 || 0),
            rekomendasiDesil15: Number(item.rekomendasiDesil15 || 0),
            rekomendasiDesil610: Number(item.rekomendasiDesil610 || 0),

            bantuan
          }
        })
      } catch (error) {
        this.errorMessage = error.message
        console.error(error)
      } finally {
        if (requestId === this.tableAreaRequestId) {
          this.isTableAreaLoading = false
          this.isLoading = false
        }
      }
    },

    async fetchConcentration(options = {}) {
      if (this.isConcentrationLoading) return
      if (this.concentrationLoaded && !options.force) return

      try {
        this.isConcentrationLoading = true
        this.isLoading = true
        this.errorMessage = ''

        const params = new URLSearchParams()

        params.append('type', options.type || this.concentrationType)
        params.append('program', options.program || this.selectedProgram)
        params.append('dimension', options.dimension || this.selectedDimension)
        params.append('level', options.level || this.wilayahLevel)
        params.append('reference', options.reference || this.concentrationReference)

        const response = await fetch(`${API_BASE}/concentration?${params.toString()}`)

        if (!response.ok) {
          throw new Error('Gagal mengambil data konsentrasi')
        }

        const data = await response.json()
        this.concentrationData = normalizeArrayResponse(data)
        this.concentrationLoaded = true
      } catch (error) {
        this.errorMessage = error.message
        console.error(error)
      } finally {
        this.isConcentrationLoading = false
        this.isLoading = false
      }
    },

    async fetchRecommendations(options = {}) {
      const requestId = this.recommendationRequestId + 1
      this.recommendationRequestId = requestId

      try {
        this.isRecommendationLoading = true
        this.isLoading = true
        this.errorMessage = ''

        const params = new URLSearchParams()

        const offset = Number(options.offset ?? this.recommendationOffset ?? 0)
        const limit = Number(options.limit ?? this.recommendationLimit ?? 25)

        params.append('page', Math.floor(offset / limit) + 1)
        params.append('size', limit)

        if (options.kodeProvinsi) params.append('kode_provinsi', options.kodeProvinsi)
        if (options.kodeKabkot) params.append('kode_kabkot', options.kodeKabkot)
        if (options.kodeKecamatan) params.append('kode_kecamatan', options.kodeKecamatan)
        if (options.kodeKelurahanDesa) params.append('kode_kelurahan_desa', options.kodeKelurahanDesa)

        if (options.wilayahLevel) {
          params.append('wilayah_level', options.wilayahLevel)
        }

        if (options.keyword) {
          params.append('keyword', options.keyword)
        }

        appendArrayParams(params, 'program', options.programFilter)
        appendArrayParams(params, 'desil', options.desilFilter)
        appendArrayParams(params, 'prioritas', options.prioritasFilter)
        appendArrayParams(params, 'asesmen', options.asesmenFilter)
        appendArrayParams(params, 'status', options.statusProgramFilter)
        appendArrayParams(params, 'statusData', options.statusDataFilter)

        const response = await fetch(`${API_BASE}/recommendations?${params.toString()}`)

        if (!response.ok) {
          throw new Error('Gagal mengambil data rekomendasi keluarga')
        }

        const data = await response.json()
        const items = normalizeArrayResponse(data.items || data.data || data.rows || data)

        if (requestId !== this.recommendationRequestId) return

        this.keluargaData = normalizeRecommendationRows(items)
        this.recommendationOffset = offset
        this.recommendationLimit = limit
        this.recommendationTotalRows = Number(
          data.totalItems ??
            data.total_items ??
            data.totalRows ??
            data.total_rows ??
            data.total ??
            data.count ??
            data.meta?.totalItems ??
            data.meta?.total_items ??
            data.meta?.totalRows ??
            data.meta?.total_rows ??
            data.meta?.total ??
            items.length ??
            0
        )
      } catch (error) {
        this.errorMessage = error.message
        console.error(error)
        throw error
      } finally {
        if (requestId === this.recommendationRequestId) {
          this.isRecommendationLoading = false
          this.isLoading = false
        }
      }
    },

    async fetchInitialData() {

      await this.fetchSummary()

      if (this.tableMode === 'wilayah') {
        await this.fetchTableAreas({
          force: true,
          level: localStorage.getItem('webgis_table_wilayah_level') || 'kabupaten'
        })
      } else {

        await this.fetchRecommendations({
          force: true
        })
      }
    },

    async fetchMapData({ force = false } = {}) {
      if (this.activeMenu === 'peta_konsentrasi') {
        await Promise.all([
          this.fetchAreas({ force }),
          this.fetchConcentration({ force })
        ])

        return
      }
      
      await this.fetchAreas({ force })
    }
  }
})

function safeLower(value) {
  return String(value || '').toLowerCase()
}

function normalizeKode(value) {
  return String(value || '')
    .trim()
    .replace(/\./g, '')
}

function normalizeArrayResponse(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data.items)) return data.items
  if (Array.isArray(data.data)) return data.data
  if (Array.isArray(data.rows)) return data.rows
  return []
}

function normalizeList(value) {
  if (Array.isArray(value)) return value
  if (value === null || value === undefined || value === '') return []

  return String(value)
    .split(',')
    .map((item) => {
      const trimmed = item.trim()
      const numberValue = Number(trimmed)
      return Number.isNaN(numberValue) ? trimmed : numberValue
    })
    .filter((item) => item !== '')
}

function normalizeProgramTotals(value = {}) {
  return {
    pip: Number(value.pip || value.PIP || 0),
    pkh: Number(value.pkh || value.PKH || 0),
    pbi_jk: Number(value.pbi_jk || value.pbiJk || value['PBI JK'] || value.PBI_JK || 0),
    atensi: Number(value.atensi || value.ATENSI || 0),
    sembako: Number(value.sembako || value.SEMBAKO || 0),
    blt: Number(value.blt || value.BLT || 0),
    bsps: Number(value.bsps || value.BSPS || 0)
  }
}

function normalizeBantuan(value = {}) {
  return {
    pip: Number(value.pip || value.PIP || 0),
    pkh: Number(value.pkh || value.PKH || 0),
    pbi_jk: Number(value.pbi_jk || value.pbiJk || value.pbi_jk_count || value['PBI JK'] || 0),
    atensi: Number(value.atensi || value.ATENSI || 0),
    sembako: Number(value.sembako || value.SEMBAKO || 0),
    blt: Number(value.blt || value.BLT || 0),
    bsps: Number(value.bsps || value.BSPS || 0)
  }
}

function normalizeDeprivasi(value = {}) {
  return {
    pendidikan: Number(value.pendidikan || 0),
    kesehatan: Number(value.kesehatan || 0),
    perumahan: Number(value.perumahan || 0),
    ekonomi: Number(value.ekonomi || 0)
  }
}

function normalizeRecommendationRows(items) {
  return items.map((item, index) => {
    const idKeluarga =
      item.idKeluarga ||
      item.id_keluarga ||
      item.id_keluarga_pensasaran ||
      `keluarga-${index}`

    const kodeProvinsi =
      item.kodeProvinsi ||
      item.kode_provinsi ||
      ''

    const kodeKabkot =
      item.kodeKabkot ||
      item.kodeKabupatenKota ||
      item.kode_kabkot ||
      item.kode_kabupaten_kota ||
      ''

    const kodeKecamatan =
      item.kodeKecamatan ||
      item.kode_kecamatan ||
      ''

    const kodeKelurahanDesa =
      item.kodeKelurahanDesa ||
      item.kode_kelurahan_desa ||
      item.kode_desa ||
      ''

    const namaProvinsi =
      item.namaProvinsi ||
      item.nama_provinsi ||
      item.provinsi ||
      kodeProvinsi ||
      '-'

    const namaKabkot =
      item.namaKabkot ||
      item.namaKabupatenKota ||
      item.nama_kabkot ||
      item.nama_kabupaten_kota ||
      item.kabupatenKota ||
      item.kabupaten_kota ||
      item.kabupaten ||
      item.kota ||
      kodeKabkot ||
      '-'

    const namaKecamatan =
      item.namaKecamatan ||
      item.nama_kecamatan ||
      item.kecamatan ||
      kodeKecamatan ||
      '-'

    const namaDesa =
      item.namaKelurahanDesa ||
      item.nama_kelurahan_desa ||
      item.kelurahanDesa ||
      item.kelurahan_desa ||
      item.namaDesa ||
      item.nama_desa ||
      item.desa ||
      kodeKelurahanDesa ||
      '-'

    const recommendation = {
      program: normalizeProgramKey(item.program || item.program_bantuan),
      statusProgram: normalizeStatusProgram(
        item.statusProgram ||
          item.status_program ||
          item.status
      ),
      prioritas: normalizePrioritas(
        item.prioritas ||
          item.kelompok_prioritas
      ),
      catatanAsesmen: normalizeAsesmen(
        item.catatanAsesmen ||
          item.catatan_asesmen ||
          item.asesmen
      ),
      kelengkapanData: normalizeStatusData(
        item.kelengkapanData ||
          item.kelengkapan_data ||
          item.status_data
      ),
      dasarRekomendasi: normalizeIndicatorText(
        item.indikatorDeprivasi ||
          item.indikator_deprivasi ||
          item.dasarRekomendasi ||
          item.dasar_rekomendasi
      )
    }

    return {
      id_keluarga: idKeluarga,

      wilayahId:
        kodeKelurahanDesa ||
        kodeKecamatan ||
        kodeKabkot ||
        item.kodeWilayah ||
        item.kode_wilayah ||
        '',

      wilayah:
        namaDesa !== '-'
          ? namaDesa
          : namaKecamatan !== '-'
            ? namaKecamatan
            : namaKabkot !== '-'
              ? namaKabkot
              : namaProvinsi,

      provinsi: namaProvinsi,
      kabupaten: namaKabkot,
      kecamatan: namaKecamatan,
      desa: namaDesa,

      kodeProvinsi,
      kodeKabkot,
      kodeKecamatan,
      kodeKelurahanDesa,

      desil:
        item.desil ??
        item.desilNasional ??
        item.desil_nasional ??
        null,

      desilLabel:
        item.desilLabel ||
        item.desil_label ||
        (
          item.desil ||
          item.desilNasional ||
          item.desil_nasional
            ? `Desil ${item.desil || item.desilNasional || item.desil_nasional}`
            : 'Belum Diketahui'
        ),

      rekomendasi: [recommendation]
    }
  })
}

function normalizeProgramKey(value) {
  const raw = String(value || '').trim().toLowerCase()

  if (raw === 'pbi jk' || raw === 'pbi-jk') return 'pbi_jk'
  return raw
}

function normalizeStatusProgram(value) {
  const raw = String(value || '').trim().toLowerCase()

  if (raw === '1' || raw === 'direkomendasikan') return 'direkomendasikan'
  if (raw === '0' || raw === 'dipertimbangkan') return 'dipertimbangkan'
  return raw || '-'
}

function normalizePrioritas(value) {
  const raw = String(value || '').trim().toLowerCase()

  if (raw === '1' || raw === 'sangat tinggi') return 'Sangat Tinggi'
  if (raw === '2' || raw === 'tinggi') return 'Tinggi'
  if (raw === '3' || raw === 'sedang') return 'Sedang'
  if (raw === '4' || raw === 'rendah') return 'Rendah'
  if (raw === '5' || raw === 'sangat rendah') return 'Sangat Rendah'
  if (raw === '' || raw === 'null' || raw === 'belum ditetapkan' || raw === 'belum_ditetapkan') {
    return 'Belum Ditetapkan'
  }

  return value
}

function normalizeAsesmen(value) {
  const raw = String(value || '').trim().toLowerCase()

  if (raw === 'perlu asesmen' || raw === 'perlu' || raw === '1') return 'Perlu Asesmen'
  if (raw === 'tidak perlu' || raw === 'tidak_perlu' || raw === '0') return 'Tidak Perlu'
  return 'Belum Ditetapkan'
}

function normalizeStatusData(value) {
  const raw = String(value || '').trim().toLowerCase()

  if (raw === 'data lengkap' || raw === 'lengkap' || raw === '1') return 'Data Lengkap'
  if (raw === 'tidak lengkap' || raw === 'tidak_lengkap' || raw === '0') return 'Tidak Lengkap'
  return 'Tidak Lengkap'
}

function normalizeIndicatorText(value) {
  if (Array.isArray(value)) return value.join(', ')
  return String(value || '').trim()
}

function appendArrayParams(params, key, value) {
  if (!Array.isArray(value)) return
  value.forEach((item) => params.append(key, item))
}

function getFallbackLat(index) {
  return -7.78 + (index % 7) * 0.015
}

function getFallbackLng(index) {
  return 110.35 + (index % 7) * 0.015
}
