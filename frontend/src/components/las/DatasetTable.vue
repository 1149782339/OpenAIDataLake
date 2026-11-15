<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import DatasetPermissionModal from '@/components/las/DatasetPermissionModal.vue'
import { LAS_ICONS, FORMAT_ICON } from '@/components/las/lasIcons'
import { useLasStore, type LasDataset, type SortKey } from '@/stores/las'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{
  rows: LasDataset[]
  kind: 'common' | 'infer'
}>()

const store = useLasStore()
const ui = useUiStore()

const menuOpen = ref(false)
const menuStyle = ref({ left: '0px', top: '0px' })
const menuRow = ref<LasDataset | null>(null)
/** 数据集权限弹窗 */
const permVisible = ref(false)
const permRow = ref<LasDataset | null>(null)

const basePath = computed(() =>
  props.kind === 'common' ? '/dataset/common' : '/dataset/infer',
)

function fmtNum(value: number) {
  return value.toLocaleString('en-US')
}

function sortState(key: SortKey) {
  if (store.sortKey !== key) return 'none'
  return store.sortDir
}

function openMenu(event: MouseEvent, row: LasDataset) {
  event.stopPropagation()
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  menuStyle.value = { left: `${Math.max(12, rect.right - 160)}px`, top: `${rect.bottom + 4}px` }
  menuRow.value = row
  menuOpen.value = true
}

function onMenuAction(action: 'permission' | 'copy' | 'delete') {
  const row = menuRow.value
  menuOpen.value = false
  if (!row) return
  if (action === 'permission') {
    permRow.value = row
    permVisible.value = true
  } else if (action === 'copy') {
    void navigator.clipboard?.writeText(row.name)
  } else {
    store.datasets = store.datasets.filter((d) => d.id !== row.id)
  }
}

function onPermissionSubmit(payload: {
  manageScope: string
  manageUsers: string[]
  readScope: string
  readUsers: string[]
}) {
  const manage = payload.manageScope === '组织内所有人'
    ? '组织内所有人'
    : payload.manageUsers.join('、') || '未指定'
  const read = payload.readScope === '组织内所有人'
    ? '组织内所有人'
    : payload.readUsers.join('、') || '未指定'
  ui.toast(`权限已配置：管理权限 ${manage}，只读权限 ${read}`)
}

function onDocClick() {
  menuOpen.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <div class="table-wrap">
    <table class="las-table">
      <colgroup>
        <col style="width: 26%">
        <col style="width: 7%">
        <col style="width: 7%">
        <col style="width: 10%">
        <col style="width: 8%">
        <col style="width: 9%">
        <col style="width: 11%">
        <col style="width: 12%">
        <col style="width: 12%">
        <col style="width: 11%">
      </colgroup>
      <thead>
        <tr>
          <th>
            <button class="th-sort" type="button" @click="store.setSort('name')">
              数据集名称
              <span class="sort-arrows" :class="sortState('name')">
                <i class="up" /><i class="down" />
              </span>
            </button>
          </th>
          <th>模态</th>
          <th>标签</th>
          <th>格式</th>
          <th class="num">样本数</th>
          <th class="num">大小</th>
          <th>创建人</th>
          <th>
            <button class="th-sort" type="button" @click="store.setSort('createdAt')">
              创建时间
              <span class="sort-arrows" :class="sortState('createdAt')">
                <i class="up" /><i class="down" />
              </span>
            </button>
          </th>
          <th>
            <button class="th-sort" type="button" @click="store.setSort('updatedAt')">
              更新时间
              <span class="sort-arrows" :class="sortState('updatedAt')">
                <i class="up" /><i class="down" />
              </span>
            </button>
          </th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td>
            <RouterLink class="name-link" :to="`${basePath}/detail/${row.id}`">
              {{ row.name }}
            </RouterLink>
            <div class="desc">{{ row.cnName || '—' }}</div>
          </td>
          <td>{{ row.modality }}</td>
          <td>
            <span class="tag-cell">
              <SvgIcon :icon="LAS_ICONS.tag" />
              <span class="tag-count">{{ row.tagCount }}</span>
            </span>
          </td>
          <td>
            <span class="format-badge">
              <SvgIcon :icon="LAS_ICONS[FORMAT_ICON[row.format]] ?? LAS_ICONS.fmtLance" />
              <span>{{ row.format }}</span>
            </span>
          </td>
          <td class="num">{{ fmtNum(row.samples) }}</td>
          <td class="num">{{ row.sizeText }}</td>
          <td class="ellipsis">{{ row.creator }}</td>
          <td class="num-mono">{{ row.createdAt }}</td>
          <td class="num-mono">{{ row.updatedAt }}</td>
          <td>
            <div class="row-ops">
              <RouterLink class="op-link" :to="`${basePath}/detail/${row.id}`">详情</RouterLink>
              <RouterLink class="op-link" :to="`${basePath}/sql/${row.id}`">SQL查询</RouterLink>
              <button class="more-btn" type="button" title="更多" @click="openMenu($event, row)">
                <SvgIcon :icon="LAS_ICONS.more" />
              </button>
            </div>
          </td>
        </tr>
        <tr v-if="!rows.length">
          <td class="empty" colspan="10">暂无数据</td>
        </tr>
      </tbody>
    </table>

    <div v-if="menuOpen" class="row-menu" :style="menuStyle" @click.stop>
      <button type="button" @click="onMenuAction('permission')">权限管理</button>
      <button type="button" @click="onMenuAction('copy')">复制数据集名称</button>
      <button type="button" class="danger" @click="onMenuAction('delete')">删除数据集</button>
    </div>

    <DatasetPermissionModal
      :visible="permVisible"
      :dataset="permRow"
      @close="permVisible = false"
      @submit="onPermissionSubmit"
    />
  </div>
</template>

<style scoped>
.table-wrap {
  position: relative;
}

.las-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 13px;
}

.las-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  color: #1d2129;
  font-weight: 500;
  text-align: left;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.las-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.las-table tbody tr:hover td {
  background: #f7f8fa;
}

.th-sort {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: 0;
  background: none;
  padding: 0;
  font-size: 13px;
  font-weight: 500;
  color: inherit;
  cursor: pointer;
}

.sort-arrows {
  display: inline-flex;
  flex-direction: column;
  gap: 1px;
  line-height: 0;
}

.sort-arrows i {
  width: 0;
  height: 0;
  border-left: 3.5px solid transparent;
  border-right: 3.5px solid transparent;
}

.sort-arrows .up {
  border-bottom: 4px solid #c9cdd4;
}

.sort-arrows .down {
  border-top: 4px solid #c9cdd4;
}

.sort-arrows.asc .up {
  border-bottom-color: #165dff;
}

.sort-arrows.desc .down {
  border-top-color: #165dff;
}

.name-link {
  color: #165dff;
  text-decoration: none;
}

.name-link:hover {
  text-decoration: underline;
}

.desc {
  margin-top: 2px;
  color: #86909c;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #4e5969;
}

.tag-cell svg {
  width: 13px;
  height: 13px;
}

.tag-count {
  border-bottom: 1px solid #c9cdd4;
  cursor: pointer;
}

.format-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  color: #4e5969;
  font-size: 12px;
}

.format-badge svg {
  width: 12px;
  height: 12px;
}

.num,
.num-mono {
  text-align: right;
  font-size: 13px;
}

.num-mono {
  color: #4e5969;
  font-size: 12px;
  white-space: nowrap;
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-ops {
  display: flex;
  align-items: center;
  gap: 12px;
}

.op-link {
  color: #165dff;
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
}

.op-link:hover {
  text-decoration: underline;
}

.more-btn {
  width: 20px;
  height: 20px;
  border: 0;
  border-radius: 4px;
  background: none;
  padding: 0;
  color: #86909c;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-btn svg {
  width: 14px;
  height: 14px;
}

.empty {
  height: 120px;
  text-align: center;
  color: #86909c;
}

.row-menu {
  position: fixed;
  z-index: 30;
  min-width: 150px;
  padding: 4px 0;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.row-menu button {
  display: block;
  width: 100%;
  height: 32px;
  padding: 0 12px;
  border: 0;
  background: none;
  text-align: left;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.row-menu button:hover {
  background: #f7f8fa;
}

.row-menu button.danger {
  color: #f53f3f;
}
</style>
