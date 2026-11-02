<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { ICONS } from '@/components/icons'
import { LAS_ICONS } from '@/components/las/lasIcons'
import CatalogTree from '@/components/las/CatalogTree.vue'
import { useLasCatalogStore } from '@/stores/lasCatalog'

const route = useRoute()
const router = useRouter()
const store = useLasCatalogStore()

const activeTab = ref<'tables' | 'functions'>('tables')
const tableQuery = ref('')

const catalogName = computed(() => String(route.params.catalog ?? 'catalog1'))
const databaseName = computed(() => String(route.params.schema ?? 'default'))

const database = computed(() => {
  const cat = store.catalogs.find((c) => c.name === catalogName.value)
  return cat?.databases.find((d) => d.name === databaseName.value) ?? null
})

const tables = computed(() => {
  const q = tableQuery.value.trim().toLowerCase()
  const list = database.value?.tables ?? []
  if (!q) return list
  return list.filter((t) => t.name.toLowerCase().includes(q))
})

function onSelectCatalog(name: string) {
  router.push('/las/catalog/metadata')
  store.selectCatalog(name)
}

function onSelectDatabase(catalog: string, database: string) {
  router.push(`/las/catalog/metadata/database/${catalog}/${database}`)
}

function onSelectTable(catalog: string, database: string, table: string) {
  router.push(`/las/catalog/metadata/database/table/detail/${catalog}/${database}/${table}`)
}

function onCreateTable() {
  router.push(
    `/las/catalog/metadata/database/${catalogName.value}/${databaseName.value}/table/create`,
  )
}
</script>

<template>
  <div class="lm-shell">
    <aside class="lm-side">
      <div class="lm-side-head">
        <span class="lm-side-title">Catalog</span>
        <div class="lm-side-ops">
          <button class="lm-icon-btn" type="button" title="新建 Catalog">
            <SvgIcon :icon="LAS_ICONS.plus" />
          </button>
          <button class="lm-icon-btn" type="button" title="刷新">
            <SvgIcon :icon="LAS_ICONS.refresh" />
          </button>
        </div>
      </div>

      <div class="lm-audit">
        <span class="lm-audit-label">审计日志</span>
        <button
          class="lm-switch"
          :class="{ on: store.auditLog }"
          type="button"
          @click="store.auditLog = !store.auditLog"
        >
          <i class="lm-switch-dot" />
        </button>
      </div>

      <div class="lm-search">
        <SvgIcon :icon="LAS_ICONS.search" class="lm-search-ic" />
        <input v-model="store.treeQuery" type="text" placeholder="请输入Catalog名称">
      </div>

      <CatalogTree
        :catalog="catalogName"
        :database="databaseName"
        @select-catalog="onSelectCatalog"
        @select-database="onSelectDatabase"
        @select-table="onSelectTable"
      />
    </aside>

    <section class="lm-main">
      <div class="lm-crumb">
        <div class="lm-crumb-left">
          <span class="lm-crumb-link" @click="onSelectCatalog(catalogName)">{{ catalogName }}</span>
          <span class="lm-crumb-sep">›</span>
          <span class="lm-crumb-item">{{ databaseName }}</span>
          <SvgIcon class="lm-crumb-ic" :icon="ICONS.copy" />
        </div>
        <button class="lm-ghost-btn" type="button">
          <SvgIcon :icon="LAS_ICONS.doc" />
          <span>使用说明</span>
        </button>
      </div>

      <div v-if="database" class="lm-card">
        <div class="lm-card-ic">
          <SvgIcon :icon="LAS_ICONS.db" />
        </div>
        <div class="lm-card-main">
          <div class="lm-card-title">{{ database.name }}</div>
          <div class="lm-card-meta">
            <span>Owner：{{ database.owner }}</span>
            <span class="lm-path-link">存储位置：{{ database.location }}</span>
            <span>创建时间：{{ database.createdAt }}</span>
            <span>更新时间：{{ database.updatedAt }}</span>
          </div>
        </div>
        <div class="lm-card-ops">
          <button class="lm-primary-btn" type="button">编辑</button>
        </div>
      </div>

      <div class="lm-tabs">
        <button
          class="lm-tab"
          :class="{ on: activeTab === 'tables' }"
          type="button"
          @click="activeTab = 'tables'"
        >
          数据表
        </button>
        <button
          class="lm-tab"
          :class="{ on: activeTab === 'functions' }"
          type="button"
          @click="activeTab = 'functions'"
        >
          函数
        </button>
      </div>

      <template v-if="activeTab === 'tables'">
        <div class="lm-toolbar">
          <button class="lm-primary-btn" type="button" @click="onCreateTable">
            <SvgIcon :icon="LAS_ICONS.plus" />
            <span>创建数据表</span>
          </button>
          <div class="lm-search">
            <SvgIcon :icon="LAS_ICONS.search" class="lm-search-ic" />
            <input v-model="tableQuery" type="text" placeholder="输入名称搜索">
          </div>
        </div>

        <div class="lm-table-wrap">
          <table class="lm-table">
            <colgroup>
              <col style="width: 20%">
              <col style="width: 30%">
              <col style="width: 16%">
              <col style="width: 20%">
              <col style="width: 14%">
            </colgroup>
            <thead>
              <tr>
                <th>数据表名称</th>
                <th>存储位置</th>
                <th>分区类型</th>
                <th>owner</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in tables" :key="row.name">
                <td>
                  <RouterLink
                    class="lm-link"
                    :to="`/las/catalog/metadata/database/table/detail/${catalogName}/${databaseName}/${row.name}`"
                  >
                    {{ row.name }}
                  </RouterLink>
                </td>
                <td>
                  <span class="lm-path">{{ row.location }}</span>
                  <button class="lm-copy" type="button" title="复制">
                    <SvgIcon :icon="ICONS.copy" />
                  </button>
                </td>
                <td>{{ row.partitionType }}</td>
                <td class="lm-ellipsis">{{ row.owner }}</td>
                <td>
                  <div class="lm-ops">
                    <button class="lm-link-btn" type="button">编辑</button>
                    <button class="lm-link-btn" type="button">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="!tables.length">
                <td class="lm-empty" colspan="5">暂无数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <div v-else class="lm-empty-box">该数据库下暂无函数</div>

      <div class="lm-pager">
        <span class="lm-pager-total">共 {{ tables.length }} 条</span>
        <button class="lm-page-btn" type="button" disabled>
          <SvgIcon :icon="LAS_ICONS.chevronLeft" />
        </button>
        <button class="lm-page-btn is-active" type="button">1</button>
        <button class="lm-page-btn" type="button" disabled>
          <SvgIcon :icon="LAS_ICONS.chevronRight" />
        </button>
        <select class="lm-page-size">
          <option>20 条/页</option>
          <option>10 条/页</option>
          <option>50 条/页</option>
        </select>
      </div>
    </section>
  </div>
</template>

<style scoped>
.lm-shell {
  display: flex;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

.lm-side {
  width: 250px;
  flex: none;
  display: flex;
  flex-direction: column;
  padding: 12px 10px 0;
  border-right: 1px solid #e5e6eb;
  background: #fff;
}

.lm-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 32px;
  padding: 0 4px;
}

.lm-side-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.lm-side-ops {
  display: flex;
  align-items: center;
  gap: 4px;
}

.lm-icon-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 4px;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.lm-icon-btn:hover {
  background: #f2f3f5;
  color: #165dff;
}

.lm-icon-btn svg {
  width: 15px;
  height: 15px;
}

.lm-audit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 34px;
  padding: 0 6px;
}

.lm-audit-label {
  font-size: 13px;
  color: #1d2129;
}

.lm-switch {
  width: 32px;
  height: 18px;
  border: 0;
  border-radius: 9px;
  background: #c9cdd4;
  cursor: pointer;
  position: relative;
  padding: 0;
  transition: background 0.15s ease;
}

.lm-switch.on {
  background: #165dff;
}

.lm-switch-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s ease;
}

.lm-switch.on .lm-switch-dot {
  transform: translateX(14px);
}

.lm-search {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  margin: 4px 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.lm-search:focus-within {
  border-color: #165dff;
}

.lm-search-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.lm-search input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.lm-search input::placeholder {
  color: #86909c;
}

.lm-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
  overflow: auto;
}

.lm-crumb {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 32px;
}

.lm-crumb-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.lm-crumb-link {
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
}

.lm-crumb-link:hover {
  color: #165dff;
}

.lm-crumb-sep {
  color: #86909c;
  font-size: 13px;
}

.lm-crumb-item {
  font-size: 13px;
  color: #1d2129;
}

.lm-crumb-ic {
  width: 13px;
  height: 13px;
  color: #86909c;
  cursor: pointer;
}

.lm-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  margin: 8px 0 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
}

.lm-card-ic {
  width: 40px;
  height: 40px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f2f3f5;
  color: #4e5969;
}

.lm-card-ic svg {
  width: 20px;
  height: 20px;
}

.lm-card-main {
  flex: 1;
  min-width: 0;
}

.lm-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 8px;
}

.lm-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #4e5969;
}

.lm-path-link {
  color: #165dff;
}

.lm-card-ops {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

.lm-tabs {
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #e5e6eb;
}

.lm-tab {
  height: 36px;
  padding: 0;
  border: 0;
  border-bottom: 2px solid transparent;
  background: none;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
}

.lm-tab.on {
  color: #165dff;
  border-bottom-color: #165dff;
}

.lm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0 12px;
}

.lm-toolbar .lm-search {
  width: 220px;
  margin: 0;
}

.lm-primary-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.lm-primary-btn:hover {
  background: #0e42d2;
}

.lm-primary-btn svg {
  width: 13px;
  height: 13px;
}

.lm-ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.lm-ghost-btn:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}

.lm-ghost-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.lm-ghost-btn svg {
  width: 14px;
  height: 14px;
}

.lm-table-wrap {
  overflow-x: auto;
}

.lm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.lm-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.lm-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.lm-table tbody tr:hover td {
  background: #f7f8fa;
}

.lm-link,
.lm-link-btn {
  color: #165dff;
  font-size: 13px;
  text-decoration: none;
  border: 0;
  background: none;
  padding: 0;
  cursor: pointer;
}

.lm-link:hover,
.lm-link-btn:hover {
  text-decoration: underline;
}


.lm-ops {
  display: flex;
  gap: 12px;
}

.lm-path {
  color: #4e5969;
  margin-right: 4px;
}

.lm-copy {
  border: 0;
  background: none;
  padding: 0;
  color: #86909c;
  cursor: pointer;
  display: inline-flex;
}

.lm-copy svg {
  width: 13px;
  height: 13px;
}

.lm-type {
  color: #4e5969;
}

.lm-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lm-empty {
  height: 100px;
  text-align: center;
  color: #86909c;
}

.lm-empty-box {
  margin-top: 32px;
  text-align: center;
  color: #86909c;
  font-size: 13px;
}

.lm-pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.lm-pager-total {
  font-size: 13px;
  color: #4e5969;
  margin-right: 4px;
}

.lm-page-btn {
  min-width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  cursor: pointer;
}

.lm-page-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.lm-page-btn.is-active {
  border-color: #165dff;
  color: #165dff;
}

.lm-page-btn svg {
  width: 14px;
  height: 14px;
}

.lm-page-size {
  height: 28px;
  padding: 0 6px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  outline: none;
}
</style>
