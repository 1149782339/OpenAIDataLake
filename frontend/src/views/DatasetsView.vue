<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import CopyButton from '@/components/CopyButton.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import CreateDatasetPage from '@/components/datasets/CreateDatasetPage.vue'
import { useDatasetStore, type Dataset } from '@/stores/datasets'
import { useUiStore } from '@/stores/ui'
import { fmtNum, copyToClipboard } from '@/utils/format'

const store = useDatasetStore()
const ui = useUiStore()

const FILTER_FORMATS = ['Lance', 'Parquet', 'JSONL', 'CSV']

const filterOpen = ref(false)
const filterStyle = ref<{ left: string; top: string }>({ left: '0px', top: '0px' })
const menuOpen = ref(false)
const menuStyle = ref<{ left: string; top: string }>({ left: '0px', top: '0px' })
const menuTargetId = ref<number | null>(null)
const checks = ref<Record<string, boolean>>({
  Lance: true,
  Parquet: true,
  JSONL: true,
  CSV: true,
})

const createOpen = ref(false)
const detailOpen = ref(false)
const detailItem = ref<Dataset | null>(null)

const filterActive = computed(() => store.formats.size < 4)

function onSearch(event: Event) {
  store.setQuery((event.target as HTMLInputElement).value)
}

function onPageSize(event: Event) {
  store.setPageSize(Number((event.target as HTMLSelectElement).value))
}

function goPage(page: number) {
  store.page = Math.min(Math.max(1, page), store.pageCount)
}

function toggleFilter(event: MouseEvent) {
  event.stopPropagation()
  menuOpen.value = false
  if (filterOpen.value) {
    filterOpen.value = false
    return
  }
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  filterStyle.value = {
    left: `${Math.max(12, Math.min(rect.right - 196, window.innerWidth - 208))}px`,
    top: `${rect.bottom + 8}px`,
  }
  filterOpen.value = true
}

function onFormatChange() {
  const next = new Set(
    Object.entries(checks.value)
      .filter(([, checked]) => checked)
      .map(([format]) => format),
  )
  store.setFormats(next)
}

function resetFilters() {
  checks.value = { Lance: true, Parquet: true, JSONL: true, CSV: true }
  store.resetFilters()
}

function openMoreMenu(event: MouseEvent, item: Dataset) {
  event.stopPropagation()
  filterOpen.value = false
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  menuStyle.value = {
    left: `${Math.max(12, rect.right - 164)}px`,
    top: `${rect.bottom + 6}px`,
  }
  menuTargetId.value = item.id
  menuOpen.value = true
}

function onMenuAction(action: 'copy' | 'delete') {
  const item = store.datasetById(menuTargetId.value)
  menuOpen.value = false
  if (!item) return
  if (action === 'copy') {
    copyToClipboard(item.name).then(
      () => ui.toast('已复制到剪贴板'),
      () => ui.toast('复制失败，请手动复制'),
    )
  } else if (action === 'delete') {
    const ok = window.confirm(`确定删除数据集 “${item.name}” 吗？`)
    if (!ok) return
    store.removeDataset(item.id)
    ui.toast('数据集已删除（演示）')
  }
}

function openDetail(item: Dataset) {
  detailItem.value = item
  detailOpen.value = true
}

function onDocumentClick() {
  filterOpen.value = false
  menuOpen.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    filterOpen.value = false
    menuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <main class="main">
    <header class="page-header">
      <h1>数据集</h1>
      <p>面向AI开发人员的多模态数据集，为模型训练、评估或研究等场景提供数据支撑。</p>
    </header>

    <section class="content">
      <div class="toolbar">
        <button class="btn-primary" type="button" @click="createOpen = true">
          <SvgIcon name="plus" />
          创建数据集
        </button>

        <div class="toolbar-right">
          <div class="search">
            <SvgIcon name="search" />
            <input
              type="text"
              placeholder="搜索数据集名称"
              autocomplete="off"
              :value="store.query"
              @input="onSearch"
            >
          </div>
          <button
            class="filter-btn"
            :class="{ active: filterActive }"
            type="button"
            title="筛选"
            @click="toggleFilter"
          >
            <SvgIcon name="filter" />
          </button>
        </div>
      </div>

      <div class="table-card">
        <div class="table-scroll">
          <table class="data-table">
            <colgroup>
              <col style="width: 36%">
              <col style="width: 14%">
              <col style="width: 18%">
              <col style="width: 14%">
              <col style="width: 18%">
            </colgroup>
            <thead>
              <tr>
                <th>
                  <button class="th-sort" type="button" @click="store.toggleSort()">
                    数据集名称
                    <svg class="sort-svg" width="12" height="14" viewBox="0 0 12 14" aria-hidden="true">
                      <path class="sort-up" :class="{ on: store.sortDir === 'asc' }" d="M6 1.2 9.6 5.8H2.4L6 1.2Z" />
                      <path class="sort-down" :class="{ on: store.sortDir === 'desc' }" d="M6 12.8 2.4 8.2h7.2L6 12.8Z" />
                    </svg>
                  </button>
                </th>
                <th>格式</th>
                <th>样本数</th>
                <th>大小</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in store.pageRows" :key="item.id">
                <td>
                  <button class="ds-link" type="button" @click="openDetail(item)">{{ item.name }}</button>
                  <div class="desc-line">
                    <span>{{ item.desc || '—' }}</span>
                    <CopyButton :value="item.name" title="复制数据集名称" />
                  </div>
                </td>
                <td>
                  <span class="format-badge">
                    <SvgIcon name="layers" />
                    <span>{{ item.format }}</span>
                  </span>
                </td>
                <td class="num">{{ fmtNum(item.samples) }}</td>
                <td class="num">{{ item.size }}</td>
                <td>
                  <div class="actions">
                    <button class="link-btn" type="button" @click="openDetail(item)">详情</button>
                    <button
                      class="more-btn"
                      type="button"
                      title="更多"
                      @click="openMoreMenu($event, item)"
                    >
                      <SvgIcon name="more" />
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!store.pageRows.length">
                <td class="empty-cell" colspan="5">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="pagination">
        <span class="page-total">共 {{ store.total }} 条</span>
        <div class="page-buttons">
          <button
            class="page-btn"
            type="button"
            :disabled="store.page <= 1"
            @click="goPage(store.page - 1)"
          >
            <SvgIcon name="chevLeft" />
          </button>
          <button
            v-for="i in store.pageCount"
            :key="i"
            class="page-btn"
            :class="{ active: i === store.page }"
            type="button"
            @click="goPage(i)"
          >
            {{ i }}
          </button>
          <button
            class="page-btn"
            type="button"
            :disabled="store.page >= store.pageCount"
            @click="goPage(store.page + 1)"
          >
            <SvgIcon name="chevRight" />
          </button>
        </div>
        <select class="page-size" :value="store.pageSize" @change="onPageSize">
          <option value="10">10条/页</option>
          <option value="20">20条/页</option>
          <option value="50">50条/页</option>
        </select>
      </div>
    </section>
  </main>

  <!-- 格式筛选浮层 -->
  <div class="popover" :class="{ show: filterOpen }" :style="filterStyle" @click.stop>
    <div class="popover-hd">格式</div>
    <label v-for="format in FILTER_FORMATS" :key="format" class="check-row">
      <input
        v-model="checks[format]"
        type="checkbox"
        :value="format"
        @change="onFormatChange"
      >
      <span>{{ format }}</span>
    </label>
    <div class="popover-ft">
      <button class="link-btn" type="button" @click="resetFilters">重置</button>
    </div>
  </div>

  <!-- 行内更多操作 -->
  <div class="menu" :class="{ show: menuOpen }" :style="menuStyle" @click.stop>
    <button type="button" @click="onMenuAction('copy')">复制数据集名称</button>
    <button type="button" class="danger" @click="onMenuAction('delete')">删除数据集</button>
  </div>

  <!-- 数据集详情 -->
  <BaseModal
    id="detailModal"
    :open="detailOpen"
    title="数据集详情"
    aria-label="数据集详情"
    @close="detailOpen = false"
  >
    <div v-if="detailItem" class="detail-grid">
      <div class="k">数据集名称</div>
      <div class="v">{{ detailItem.name }}</div>
      <div class="k">描述</div>
      <div class="v">{{ detailItem.desc || '—' }}</div>
      <div class="k">格式</div>
      <div class="v">
        <span class="format-badge">
          <SvgIcon name="layers" />
          <span>{{ detailItem.format }}</span>
        </span>
      </div>
      <div class="k">样本数</div>
      <div class="v">{{ fmtNum(detailItem.samples) }}</div>
      <div class="k">大小</div>
      <div class="v">{{ detailItem.size }}</div>
      <div class="k">存储位置</div>
      <div class="v">{{ detailItem.ossPath || '-' }}</div>
    </div>
    <template #footer>
      <div class="modal-foot">
        <button class="btn-ghost" type="button" @click="detailOpen = false">关闭</button>
      </div>
    </template>
  </BaseModal>

  <CreateDatasetPage :open="createOpen" @close="createOpen = false" />
</template>
