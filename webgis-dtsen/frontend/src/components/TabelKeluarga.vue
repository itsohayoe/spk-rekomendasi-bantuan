<template>
  <div class="table-filter-card family-filter-top">
    <select
      class="input table-level-select"
      :value="wilayahLevel"
      @change="$emit('update:wilayahLevel', $event.target.value)"
    >
      <option value="provinsi">Provinsi</option>
      <option value="kabupaten">Kabupaten/Kota</option>
      <option value="kecamatan">Kecamatan</option>
      <option value="desa">Desa/Kelurahan</option>
    </select>

    <input
      class="input"
      type="text"
      :value="keyword"
      :placeholder="familySearchPlaceholder"
      @input="$emit('update:keyword', $event.target.value)"
    />

    <button
      v-if="topFilterActive"
      class="btn-reset"
      @click="$emit('reset')"
    >
      Reset
    </button>
  </div>

  <div class="table-card table-scroll-card">
    <table class="family-table">
      <colgroup>
        <col class="col-family-id" />
        <col class="col-family-region" />
        <col class="col-family-desil" />
        <col class="col-family-program" />
        <col class="col-family-status" />
        <col class="col-family-priority" />
        <col class="col-family-assessment" />
        <col class="col-family-data" />
        <col class="col-family-indicator" />
      </colgroup>

      <thead>
        <tr>
          <th>ID Keluarga</th>
          <th>{{ familyWilayahColumnLabel }}</th>

          <th>
            <div class="th-filter-wrap">
              <span>Desil</span>
              <button
                ref="desilBtn"
                class="th-filter-btn"
                :class="{ active: desilFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-desil', desilBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>
            <div class="th-filter-wrap">
              <span>Program<br />Bantuan</span>
              <button
                ref="programBtn"
                class="th-filter-btn"
                :class="{ active: programFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-program', programBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>
            <div class="th-filter-wrap">
              <span>Status<br />Program</span>
              <button
                ref="statusProgramBtn"
                class="th-filter-btn"
                :class="{ active: statusProgramFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-status-program', statusProgramBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>
            <div class="th-filter-wrap">
              <span>Kelompok<br />Prioritas</span>
              <button
                ref="prioritasBtn"
                class="th-filter-btn"
                :class="{ active: prioritasFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-prioritas', prioritasBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>
            <div class="th-filter-wrap">
              <span>Catatan<br />Asesmen</span>
              <button
                ref="asesmenBtn"
                class="th-filter-btn"
                :class="{ active: asesmenFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-asesmen', asesmenBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>
            <div class="th-filter-wrap">
              <span>Status<br />Data</span>
              <button
                ref="statusDataBtn"
                class="th-filter-btn"
                :class="{ active: statusDataFilter.length > 0 }"
                @click.stop="openHeaderDropdown('family-status-data', statusDataBtn)"
              >
                <span class="filter-caret"></span>
              </button>
            </div>
          </th>

          <th>Indikator Deprivasi</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="isLoading || isSearching">
          <td colspan="9" class="table-state-cell">
            <div class="inline-spinner"></div>
          </td>
        </tr>

        <tr v-else-if="rows.length === 0">
          <td colspan="9" class="table-state-cell">
            Data tidak ditemukan
          </td>
        </tr>

        <tr v-else v-for="row in rows" :key="row.rowId">
          <td class="family-id-cell">
            <span class="family-id-text">{{ row.id_keluarga }}</span>
          </td>

          <td class="family-region-cell">
            <span class="family-region-name">
              {{ getFamilyWilayahByLevel(row, wilayahLevel) }}
            </span>
            <small>{{ getWilayahSubLabel(row, wilayahLevel) }}</small>
          </td>

          <td>{{ row.desilLabel || 'Belum Diketahui' }}</td>
          <td>{{ programLabel(row.program) }}</td>
          <td>{{ statusProgramLabel(row.statusProgram) }}</td>
          <td>{{ row.prioritas || 'Belum ditetapkan' }}</td>
          <td>{{ asesmenLabel(row.catatanAsesmen) }}</td>
          <td>{{ statusDataLabel(row.kelengkapanData) }}</td>

          <td class="indicator-cell">
            <span class="indicator-line">
              {{ row.dasarRekomendasi || '-' }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <Teleport to="body">
    <div
      v-if="dropdown"
      class="floating-filter-menu"
      :style="floatingMenuStyle"
      @click.stop
    >
      <template v-if="dropdown === 'family-desil'">
        <label v-for="i in 10" :key="i">
          <input
            type="checkbox"
            :checked="desilFilter.includes(i)"
            @change="toggleArrayValue(desilFilter, i, 'desilFilter')"
          />
          Desil {{ i }}
        </label>
      </template>

      <template v-if="dropdown === 'family-program'">
        <label v-for="program in programList" :key="program.key">
          <input
            type="checkbox"
            :checked="programFilter.includes(program.key)"
            @change="toggleArrayValue(programFilter, program.key, 'programFilter')"
          />
          {{ program.label }}
        </label>
      </template>

      <template v-if="dropdown === 'family-status-program'">
        <label v-for="item in statusProgramList" :key="item.key">
          <input
            type="checkbox"
            :checked="statusProgramFilter.includes(item.key)"
            @change="toggleArrayValue(statusProgramFilter, item.key, 'statusProgramFilter')"
          />
          {{ item.label }}
        </label>
      </template>

      <template v-if="dropdown === 'family-prioritas'">
        <label v-for="item in prioritasList" :key="item">
          <input
            type="checkbox"
            :checked="prioritasFilter.includes(item)"
            @change="toggleArrayValue(prioritasFilter, item, 'prioritasFilter')"
          />
          {{ item }}
        </label>
      </template>

      <template v-if="dropdown === 'family-asesmen'">
        <label v-for="item in asesmenList" :key="item.key">
          <input
            type="checkbox"
            :checked="asesmenFilter.includes(item.key)"
            @change="toggleArrayValue(asesmenFilter, item.key, 'asesmenFilter')"
          />
          {{ item.label }}
        </label>
      </template>

      <template v-if="dropdown === 'family-status-data'">
        <label v-for="item in statusDataList" :key="item.key">
          <input
            type="checkbox"
            :checked="statusDataFilter.includes(item.key)"
            @change="toggleArrayValue(statusDataFilter, item.key, 'statusDataFilter')"
          />
          {{ item.label }}
        </label>
      </template>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  rows: {
    type: Array,
    default: () => []
  },
  wilayahLevel: {
    type: String,
    default: 'kabupaten'
  },
  keyword: {
    type: String,
    default: ''
  },
  programFilter: {
    type: Array,
    default: () => []
  },
  desilFilter: {
    type: Array,
    default: () => []
  },
  prioritasFilter: {
    type: Array,
    default: () => []
  },
  asesmenFilter: {
    type: Array,
    default: () => []
  },
  statusProgramFilter: {
    type: Array,
    default: () => []
  },
  statusDataFilter: {
    type: Array,
    default: () => []
  },
  dropdown: {
    type: String,
    default: null
  },
  familyWilayahColumnLabel: {
    type: String,
    default: 'Kabupaten/Kota'
  },
  programList: {
    type: Array,
    default: () => []
  },
  prioritasList: {
    type: Array,
    default: () => []
  },
  asesmenList: {
    type: Array,
    default: () => []
  },
  statusProgramList: {
    type: Array,
    default: () => []
  },
  statusDataList: {
    type: Array,
    default: () => []
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  isSearching: {
    type: Boolean,
    default: false
  },
  hasActiveFilter: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'update:wilayahLevel',
  'update:keyword',
  'update:programFilter',
  'update:desilFilter',
  'update:prioritasFilter',
  'update:asesmenFilter',
  'update:statusProgramFilter',
  'update:statusDataFilter',
  'update:dropdown',
  'reset'
])

const desilBtn = ref(null)
const programBtn = ref(null)
const statusProgramBtn = ref(null)
const prioritasBtn = ref(null)
const asesmenBtn = ref(null)
const statusDataBtn = ref(null)

const menuTop = ref(0)
const menuLeft = ref(0)

const familySearchPlaceholder = computed(() => {
  const map = {
    provinsi: 'Cari nama provinsi atau ID keluarga...',
    kabupaten: 'Cari nama kabupaten/kota atau ID keluarga...',
    kecamatan: 'Cari nama kecamatan atau ID keluarga...',
    desa: 'Cari nama desa/kelurahan atau ID keluarga...'
  }

  return map[props.wilayahLevel] || 'Cari nama wilayah atau ID keluarga...'
})

const topFilterActive = computed(() => {
  return (
    props.keyword.trim() !== '' ||
    props.programFilter.length > 0 ||
    props.desilFilter.length > 0 ||
    props.prioritasFilter.length > 0 ||
    props.asesmenFilter.length > 0 ||
    props.statusProgramFilter.length > 0 ||
    props.statusDataFilter.length > 0
  )
})

const floatingMenuStyle = computed(() => ({
  top: `${menuTop.value}px`,
  left: `${menuLeft.value}px`
}))

async function openHeaderDropdown(name, buttonEl) {
  if (props.dropdown === name) {
    emit('update:dropdown', null)
    return
  }

  emit('update:dropdown', name)

  await nextTick()
  updateMenuPosition(buttonEl)
}

function updateMenuPosition(buttonEl) {
  if (!buttonEl) return

  const rect = buttonEl.getBoundingClientRect()
  const menuWidth = 158
  const padding = 12

  let left = rect.left
  const maxLeft = window.innerWidth - menuWidth - padding

  if (left > maxLeft) {
    left = maxLeft
  }

  menuTop.value = rect.bottom + 7
  menuLeft.value = Math.max(padding, left)
}

function toggleArrayValue(currentArray, value, propName) {
  const nextArray = currentArray.includes(value)
    ? currentArray.filter((item) => item !== value)
    : [...currentArray, value]

  emit(`update:${propName}`, nextArray)
}

function programLabel(key) {
  return props.programList.find((program) => program.key === key)?.label || key || '-'
}

function statusProgramLabel(value) {
  if (value === 'direkomendasikan') return 'Direkomendasikan'
  if (value === 'dipertimbangkan') return 'Dipertimbangkan'
  return value || '-'
}

function asesmenLabel(value) {
  if (value === 'perlu') return 'Perlu Asesmen'
  if (value === 'tidak_perlu') return 'Tidak Perlu'
  if (value === 'belum_ditetapkan') return 'Belum Ditetapkan'
  return value || 'Belum Ditetapkan'
}

function statusDataLabel(value) {
  if (value === 'lengkap') return 'Data Lengkap'
  if (value === 'tidak_lengkap') return 'Tidak Lengkap'
  return value || '-'
}

function getFamilyWilayahByLevel(row, level) {
  if (level === 'provinsi') return row.provinsi || '-'
  if (level === 'kabupaten') return row.kabupaten || '-'
  if (level === 'kecamatan') return row.kecamatan || '-'
  if (level === 'desa') return row.desa || '-'

  return row.wilayah || '-'
}

function getWilayahSubLabel(row, level) {
  if (level === 'desa') {
    return [row.kecamatan, row.kabupaten]
      .filter((value) => value && value !== '-')
      .join(', ')
  }

  if (level === 'kecamatan') {
    return [row.kabupaten, row.provinsi]
      .filter((value) => value && value !== '-')
      .join(', ')
  }

  if (level === 'kabupaten') {
    return row.provinsi || ''
  }

  return ''
}

function closeDropdown(event) {
  const target = event.target

  if (!(target instanceof Element)) {
    return
  }

  if (
    !target.closest('.floating-filter-menu') &&
    !target.closest('.th-filter-wrap')
  ) {
    emit('update:dropdown', null)
  }
}

onMounted(() => {
  document.addEventListener('click', closeDropdown)
  window.addEventListener('scroll', closeDropdown, true)
  window.addEventListener('resize', closeDropdown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeDropdown)
  window.removeEventListener('scroll', closeDropdown, true)
  window.removeEventListener('resize', closeDropdown)
})
</script>