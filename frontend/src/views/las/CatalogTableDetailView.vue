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

const activeTab = ref<'base' | 'overview' | 'storage'>('base')

const catalogName = computed(() => String(route.params.catalog ?? 'hive'))
const databaseName = computed(() => String(route.params.schema ?? 'default'))
const tableName = computed(() => String(route.params.table ?? 'test'))

const database = computed(() => {
  const cat = store.catalogs.find((c) => c.name === catalogName.value)
  return cat?.databases.find((d) => d.name === databaseName.value) ?? null
})

const table = computed(() =>
  database.value?.tables.find((t) => t.name === tableName.value) ?? null,
)

const baseInfo = computed(() => {
  const t = table.value
  if (!t) return []
  return [
    { k: '数据表名称', v: t.name, plain: true },
    { k: 'owner', v: t.owner, plain: true },
    { k: '所属数据目录', v: catalogName.value, plain: true },
    { k: '创建时间', v: '2026-10-22 11:07:32', plain: true },
    { k: '所属数据库', v: databaseName.value, plain: true },
    { k: '更新时间', v: '2026-10-22 11:07:32', plain: true },
    { k: '存储位置', v: t.location, link: true },
    { k: '数据表备注', v: '-', plain: true },
    { k: '表生命周期', v: '未开启', tag: true },
  ]
})

const formatInfo = computed(() => [
  { k: '存储格式', v: table.value?.tableType ?? 'Lance' },
  { k: '输入格式', v: 'Lance' },
  { k: '输出格式', v: 'Lance' },
  { k: '压缩格式', v: '-' },
  { k: '格式版本', v: '-' },
  { k: '写入版本', v: '-', help: true },
])

function onSelectCatalog(name: string) {
  router.push(`/las/catalog/metadata?catalog=${name}`)
}

function onSelectDatabase(catalog: string, database: string) {
  router.push(`/las/catalog/metadata/database/${catalog}/${database}`)
}

function onSelectTable(catalog: string, database: string, table: string) {
  router.push(`/las/catalog/metadata/database/table/detail/${catalog}/${database}/${table}`)
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
        :table="tableName"
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
          <span class="lm-crumb-link" @click="onSelectDatabase(catalogName, databaseName)">
            {{ databaseName }}
          </span>
          <span class="lm-crumb-sep">›</span>
          <span class="lm-crumb-link">表</span>
          <span class="lm-crumb-sep">›</span>
          <span class="lm-crumb-item">{{ tableName }}</span>
          <SvgIcon class="lm-crumb-ic" :icon="ICONS.copy" />
          <span class="lm-tag">{{ table?.partitionType ?? '非分区表' }}</span>
        </div>
        <button class="lm-ghost-btn" type="button">
          <SvgIcon :icon="LAS_ICONS.doc" />
          <span>使用说明</span>
        </button>
      </div>

      <div v-if="table" class="lm-card">
        <div class="lm-card-ic">
          <SvgIcon :icon="LAS_ICONS.fmtLance" />
        </div>
        <div class="lm-card-main">
          <div class="lm-card-title">
            {{ table.name }}
            <span class="lm-format-badge">{{ table.tableType }}</span>
          </div>
          <div class="lm-card-meta">
            <span>Owner：{{ table.owner }}</span>
            <span class="lm-path-link">存储位置：{{ table.location }}</span>
            <span>创建时间：2026-10-22 11:07:32</span>
            <span>更新时间：2026-10-22 11:07:32</span>
          </div>
        </div>
        <div class="lm-card-ops">
          <button class="lm-ghost-btn" type="button" disabled>删除</button>
          <button class="lm-primary-btn" type="button">创建数据集</button>
        </div>
      </div>

      <div class="lm-tabs">
        <button
          class="lm-tab"
          :class="{ on: activeTab === 'base' }"
          type="button"
          @click="activeTab = 'base'"
        >
          基本信息
        </button>
        <button
          class="lm-tab"
          :class="{ on: activeTab === 'overview' }"
          type="button"
          @click="activeTab = 'overview'"
        >
          数据概况
        </button>
        <button
          class="lm-tab"
          :class="{ on: activeTab === 'storage' }"
          type="button"
          @click="activeTab = 'storage'"
        >
          存储优化
        </button>
      </div>

      <template v-if="activeTab === 'base'">
        <section class="lm-block">
          <h3 class="lm-block-title">基本详情</h3>
          <div class="lm-grid">
            <template v-for="row in baseInfo" :key="row.k">
              <div class="lm-grid-k">{{ row.k }}</div>
              <div class="lm-grid-v">
                <span v-if="row.tag" class="lm-grey-tag">{{ row.v }}</span>
                <a v-else-if="row.link" class="lm-link" href="#" @click.prevent>{{ row.v }}</a>
                <button v-else-if="row.link" class="lm-copy" type="button" title="复制">
                  <SvgIcon :icon="ICONS.copy" />
                </button>
                <span v-else>{{ row.v }}</span>
              </div>
            </template>
          </div>
        </section>

        <section class="lm-block">
          <h3 class="lm-block-title">格式与序列化</h3>
          <div class="lm-grid">
            <template v-for="row in formatInfo" :key="row.k">
              <div class="lm-grid-k">
                {{ row.k }}
                <SvgIcon v-if="row.help" class="lm-help-ic" :icon="LAS_ICONS.question" />
              </div>
              <div class="lm-grid-v">{{ row.v }}</div>
            </template>
          </div>
        </section>
      </template>

      <div v-else-if="activeTab === 'overview'" class="lm-empty-box">
        该数据表暂未扫描数据，暂无数据概况。
      </div>

      <div v-else class="lm-empty-box">该数据表暂未开启存储优化。</div>
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

.lm-tag {
  height: 22px;
  padding: 0 8px;
  border: 1px solid #165dff;
  border-radius: 4px;
  color: #165dff;
  font-size: 12px;
  line-height: 20px;
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
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 8px;
}

.lm-format-badge {
  height: 22px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  color: #4e5969;
  font-size: 12px;
  font-weight: 400;
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

.lm-block {
  margin-top: 24px;
}

.lm-block-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.lm-grid {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) 160px minmax(0, 1fr);
  row-gap: 14px;
  column-gap: 16px;
  font-size: 13px;
}

.lm-grid-k {
  color: #86909c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.lm-grid-v {
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 6px;
  word-break: break-all;
}

.lm-help-ic {
  width: 13px;
  height: 13px;
  color: #86909c;
}

.lm-grey-tag {
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: #f2f3f5;
  color: #4e5969;
  font-size: 12px;
  line-height: 22px;
}

.lm-link {
  color: #165dff;
  font-size: 13px;
  text-decoration: none;
}

.lm-link:hover {
  text-decoration: underline;
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

.lm-empty-box {
  margin-top: 32px;
  text-align: center;
  color: #86909c;
  font-size: 13px;
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
  white-space: nowrap;
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
</style>
