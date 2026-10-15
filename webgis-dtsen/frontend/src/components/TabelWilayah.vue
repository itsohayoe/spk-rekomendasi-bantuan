<template>
  <div
    class="table-filter-card wilayah-filter"
    :class="{ 'has-reset': topFilterActive }"
  >
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
      :value="search"
      :placeholder="wilayahSearchPlaceholder"
      @input="$emit('update:search', $event.target.value)"
    />

    <button
      v-if="topFilterActive"
      class="btn-reset wilayah-filter-reset"
      @click="$emit('reset')"
    >
      Reset
    </button>

    <div class="filter-dropdown">
      <button
        class="btn-filter table-filter-button"
        :class="{ active: desilFilter.length > 0 }"
        @click.stop="toggleDropdown('wilayah-desil')"
      >
        <span>Desil</span>
      </button>

      <div
        v-if="dropdown === 'wilayah-desil'"
        class="dropdown-menu"
        @click.stop
      >
        <label v-for="item in desilOptions" :key="item">
          <input
            type="checkbox"
            :checked="desilFilter.includes(item)"
            @change="toggleArrayValue(desilFilter, item, 'desilFilter')"
          />
          Desil {{ item }}
        </label>
      </div>
    </div>

    <div class="filter-dropdown">
      <button
        class="btn-filter table-filter-button"
        :class="{ active: prioritasFilter.length > 0 }"
        @click.stop="toggleDropdown('wilayah-prioritas')"
      >
        <span>Prioritas</span>
      </button>

      <div
        v-if="dropdown === 'wilayah-prioritas'"
        class="dropdown-menu"
        @click.stop
      >
        <label v-for="item in prioritasList" :key="item">
          <input
            type="checkbox"
            :checked="prioritasFilter.includes(item)"
            @change="toggleArrayValue(prioritasFilter, item, 'prioritasFilter')"
          />
          {{ item }}
        </label>
      </div>
    </div>

    <div class="filter-dropdown">
      <button
        class="btn-filter table-filter-button"
        :class="{ active: asesmenFilter.length > 0 }"
        @click.stop="toggleDropdown('wilayah-asesmen')"
      >
        <span>Asesmen</span>
      </button>

      <div
        v-if="dropdown === 'wilayah-asesmen'"
        class="dropdown-menu"
        @click.stop
      >
        <label v-for="item in asesmenList" :key="item.key">
          <input
            type="checkbox"
            :checked="asesmenFilter.includes(item.key)"
            @change="toggleArrayValue(asesmenFilter, item.key, 'asesmenFilter')"
          />
          {{ item.label }}
        </label>
      </div>
    </div>
  </div>

  <div class="table-card table-scroll-card">
    <table class="wilayah-table">
      <colgroup>
        <col class="col-wilayah-name" />
        <col class="col-wilayah-total" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-program" />
        <col class="col-wilayah-total-rekom" />
        <col class="col-wilayah-detail" />
      </colgroup>

      <thead>
        <tr>
          <th>{{ wilayahColumnLabel }}</th>
          <th>Total Keluarga</th>
          <th>PIP</th>
          <th>PKH</th>
          <th>PBI JK</th>
          <th>ATENSI</th>
          <th>SEMBAKO</th>
          <th>BLT</th>
          <th>BSPS</th>
          <th>Total Rekomendasi</th>
          <th>Detail</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="isLoading">
          <td colspan="11" class="table-state-cell">
            <div class="inline-spinner"></div>
          </td>
        </tr>

        <tr v-else-if="rows.length === 0">
          <td colspan="11" class="table-state-cell">
            Data tidak ditemukan
          </td>
        </tr>

        <tr
          v-else
          v-for="row in rows"
          :key="`${wilayahLevel}-${row.kodeWilayah || row.id || row.kodeKelurahanDesa || row.kodeKecamatan || row.kodeKabkot || row.kodeProvinsi || row.nama}`"
        >
          <td>
            <span class="td-main-lite">
              {{ getWilayahNameByLevel(row, wilayahLevel) }}
            </span>
            <small>{{ getWilayahSubLabel(row, wilayahLevel) }}</small>
          </td>

          <td>{{ formatNumber(row.totalKeluarga) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'pip')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'pkh')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'pbi_jk')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'atensi')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'sembako')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'blt')) }}</td>
          <td>{{ formatNumber(getProgramValue(row, 'bsps')) }}</td>
          <td>{{ formatNumber(totalBantuan(row)) }}</td>

          <td>
            <button
              class="table-action-button"
              @click="$emit('open-family-detail', row)"
            >
              Daftar Keluarga
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  rows: {
    type: Array,
    default: () => []
  },
  wilayahLevel: {
    type: String,
    default: 'kabupaten'
  },
  search: {
    type: String,
    default: ''
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
  dropdown: {
    type: String,
    default: null
  },
  wilayahColumnLabel: {
    type: String,
    default: 'Kabupaten/Kota'
  },
  prioritasList: {
    type: Array,
    default: () => []
  },
  asesmenList: {
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
  'update:search',
  'update:desilFilter',
  'update:prioritasFilter',
  'update:asesmenFilter',
  'update:dropdown',
  'reset',
  'open-family-detail'
])

const desilOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

const wilayahSearchPlaceholder = computed(() => {
  const map = {
    provinsi: 'Cari nama provinsi...',
    kabupaten: 'Cari nama kabupaten/kota...',
    kecamatan: 'Cari nama kecamatan...',
    desa: 'Cari nama desa/kelurahan...'
  }

  return map[props.wilayahLevel] || 'Cari nama wilayah...'
})

const topFilterActive = computed(() => {
  return (
    props.hasActiveFilter ||
    props.search.trim() !== '' ||
    props.desilFilter.length > 0 ||
    props.prioritasFilter.length > 0 ||
    props.asesmenFilter.length > 0
  )
})

function toggleDropdown(name) {
  emit('update:dropdown', props.dropdown === name ? null : name)
}

function toggleArrayValue(currentArray, value, propName) {
  const nextArray = currentArray.includes(value)
    ? currentArray.filter((item) => item !== value)
    : [...currentArray, value]

  emit(`update:${propName}`, nextArray)
}

function getProgramValue(row, key) {
  return row?.bantuan?.[key] || 0
}

function totalBantuan(row) {
  return Object.values(row.bantuan || {}).reduce((sum, value) => sum + Number(value || 0), 0)
}

function getWilayahNameByLevel(row, level) {
  if (level === 'provinsi') return row.namaWilayah || row.provinsi || '-'
  if (level === 'kabupaten') return row.namaWilayah || row.kabupaten || row.kodeKabkot || '-'
  if (level === 'kecamatan') return row.namaWilayah || row.kecamatan || row.kodeKecamatan || '-'
  if (level === 'desa') return row.namaWilayah || row.desa || row.kodeKelurahanDesa || '-'

  return row.namaWilayah || row.nama || row.wilayah || row.kodeWilayah || '-'
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

function formatNumber(value) {
  return new Intl.NumberFormat('id-ID').format(value || 0)
}
</script>