<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { ICONS } from '@/components/icons'
import { LAS_ICONS } from '@/components/las/lasIcons'
import CatalogTree from '@/components/las/CatalogTree.vue'
import CompactMergeDrawer, { type CompactMergeConfig } from '@/components/las/CompactMergeDrawer.vue'
import { useLasCatalogStore } from '@/stores/lasCatalog'

const route = useRoute()
const router = useRouter()
const store = useLasCatalogStore()

const activeTab = ref<'base' | 'overview' | 'storage'>('base')
const activeSubTab = ref('小文件合并')
const mergeVisible = ref(false)

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
    { k: '创建时间', v: '2026-10-22 11:09:00', plain: true },
    { k: '所属数据库', v: databaseName.value, plain: true },
    { k: '更新时间', v: '2026-10-22 11:09:00', plain: true },
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
  { k: 'Table properties', v: 'EXTERNAL=true, table_type=lance' },
])

/** 字段信息 */
const fields = [
  { name: 'id', type: 'bigint', desc: '-', index: 0 },
  { name: 'k', type: 'bigint', desc: '-', index: 0 },
  { name: 'name', type: 'string', desc: '-', index: 0 },
  { name: 'embedding', type: 'string', desc: '-', index: 0 },
  { name: 'create_time', type: 'timestamp', desc: '-', index: 0 },
]

/** 数据概况 */
const overviewItems = [
  { k: '表存储大小', v: '-' },
  { k: '行数', v: '-' },
  { k: '表文件总数', v: '-' },
  { k: '版本数', v: '-' },
  { k: 'DDL最后更新时间', v: '2026-10-22 11:09:00' },
  { k: '索引数', v: '-' },
]

const fragmentItems = [
  { k: 'Fragment 数量', v: '-', help: false },
  { k: '行数分布范围', v: '-', help: true },
  { k: '数据文件数分布范围', v: '-', help: true },
]

const SUB_TABS = ['小文件合并', '快照清理', '分区生命周期管理', '孤儿文件治理']
const DISABLED_SUB_TABS = ['分区生命周期管理', '孤儿文件治理']

function onSelectCatalog(name: string) {
  router.push(`/las/catalog/metadata?catalog=${name}`)
}

function onSelectDatabase(catalog: string, database: string) {
  router.push(`/las/catalog/metadata/database/${catalog}/${database}`)
}

function onSelectTable(catalog: string, database: string, table: string) {
  router.push(`/las/catalog/metadata/database/table/detail/${catalog}/${database}/${table}`)
}

function onMergeConfirm(_config: CompactMergeConfig) {
  mergeVisible.value = false
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
            <span class="lm-format-badge">{{ table.tableType.toLowerCase() }}</span>
          </div>
          <div class="lm-card-meta">
            <span>Owner: {{ table.owner }}</span>
            <span class="lm-meta-link">
              存储位置: {{ table.location }}
              <button class="lm-copy" type="button" title="复制">
                <SvgIcon :icon="ICONS.copy" />
              </button>
            </span>
            <span>创建时间: 2026-10-22 11:09:00</span>
            <span>更新时间: 2026-10-22 11:09:00</span>
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

      <!-- 基本信息 -->
      <template v-if="activeTab === 'base'">
        <section class="lm-block">
          <h3 class="lm-block-title">基本详情</h3>
          <div class="lm-grid">
            <template v-for="row in baseInfo" :key="row.k">
              <div class="lm-grid-k">{{ row.k }}</div>
              <div class="lm-grid-v">
                <span v-if="row.tag" class="lm-grey-tag">{{ row.v }}</span>
                <template v-else-if="row.link">
                  <a class="lm-link" href="#" @click.prevent>{{ row.v }}</a>
                  <button class="lm-copy" type="button" title="复制">
                    <SvgIcon :icon="ICONS.copy" />
                  </button>
                </template>
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

        <section class="lm-block">
          <div class="lm-block-head">
            <h3 class="lm-block-title">字段信息</h3>
            <button class="lm-icon-btn" type="button" title="刷新">
              <SvgIcon :icon="LAS_ICONS.refresh" />
            </button>
          </div>
          <div class="lm-table-wrap">
            <table class="lm-table">
              <thead>
                <tr>
                  <th>字段名称</th>
                  <th>字段类型</th>
                  <th>字段描述</th>
                  <th>索引数量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="field in fields" :key="field.name">
                  <td>{{ field.name }}</td>
                  <td>{{ field.type }}</td>
                  <td>{{ field.desc }}</td>
                  <td>{{ field.index }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="lm-pager">
            <button class="lm-page-btn" type="button" disabled>
              <SvgIcon :icon="LAS_ICONS.chevronLeft" />
            </button>
            <button class="lm-page-btn is-active" type="button">1</button>
            <button class="lm-page-btn" type="button" disabled>
              <SvgIcon :icon="LAS_ICONS.chevronRight" />
            </button>
          </div>
        </section>
      </template>

      <!-- 数据概况 -->
      <template v-else-if="activeTab === 'overview'">
        <section class="lm-block">
          <h3 class="lm-block-title">数据概况</h3>
          <div class="lm-ov-grid">
            <div v-for="row in overviewItems" :key="row.k" class="lm-ov-cell">
              <div class="lm-ov-k">{{ row.k }}</div>
              <div class="lm-ov-v">{{ row.v }}</div>
            </div>
          </div>
        </section>

        <section class="lm-block">
          <h3 class="lm-block-title">Fragment 信息</h3>
          <div class="lm-ov-grid">
            <div v-for="row in fragmentItems" :key="row.k" class="lm-ov-cell">
              <div class="lm-ov-k">
                {{ row.k }}
                <SvgIcon v-if="row.help" class="lm-help-ic" :icon="LAS_ICONS.question" />
              </div>
              <div class="lm-ov-v">{{ row.v }}</div>
            </div>
          </div>
        </section>
      </template>

      <!-- 存储优化 -->
      <template v-else>
        <div class="lm-sub-tabs">
          <button
            v-for="item in SUB_TABS"
            :key="item"
            class="lm-sub-tab"
            :class="{
              on: activeSubTab === item,
              'is-disabled': DISABLED_SUB_TABS.includes(item),
            }"
            type="button"
            :disabled="DISABLED_SUB_TABS.includes(item)"
            @click="activeSubTab = item"
          >
            {{ item }}
          </button>
        </div>

        <div v-if="activeSubTab === '小文件合并'" class="lm-switch-box">
          <button
            class="lm-switch-lg"
            type="button"
            role="switch"
            aria-checked="false"
            @click="mergeVisible = true"
          >
            <i class="lm-switch-lg-dot" />
          </button>
          <span>功能未开启</span>
        </div>

        <div v-else class="lm-empty-box">该功能暂未开放</div>
      </template>

      <CompactMergeDrawer
        :visible="mergeVisible"
        @close="mergeVisible = false"
        @confirm="onMergeConfirm"
      />
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
  height: 20px;
  padding: 0 8px;
  border-radius: 4px;
  background: #e8f7f8;
  color: #347a88;
  font-size: 12px;
  line-height: 20px;
}

.lm-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0 20px;
  background: #fff;
}

.lm-card-ic {
  width: 40px;
  height: 40px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #eaedf1;
  border-radius: 4px;
  background: #f6f8fa;
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
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 4px;
}

.lm-format-badge {
  height: 22px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  color: #4e5969;
  font-size: 12px;
  font-weight: 400;
  line-height: 20px;
}

.lm-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #1d2129;
}

.lm-meta-link {
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
  gap: 6px;
  border-bottom: 1px solid #e5e6eb;
}

.lm-tab {
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e6eb;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  background: #f6f8fa;
  font-size: 14px;
  color: #42464e;
  cursor: pointer;
}

.lm-tab:hover {
  color: #165dff;
}

.lm-tab.on {
  background: #fff;
  border-color: #165dff;
  color: #165dff;
  font-weight: 500;
}

.lm-block {
  margin-top: 24px;
}

.lm-block-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.lm-block-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}

.lm-block-head .lm-block-title {
  margin: 0;
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

/* 字段信息 */
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

.lm-pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
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

/* 数据概况 */
.lm-ov-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  row-gap: 16px;
  column-gap: 40px;
  font-size: 13px;
}

.lm-ov-cell {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  align-items: center;
}

.lm-ov-k {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4e5969;
}

.lm-ov-v {
  color: #1d2129;
  word-break: break-all;
}

/* 存储优化 */
.lm-sub-tabs {
  display: inline-flex;
  align-self: flex-start;
  gap: 3px;
  padding: 3px;
  margin-top: 16px;
  border-radius: 4px;
  background: #f1f3f5;
}

.lm-sub-tab {
  height: 26px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  font-size: 13px;
  color: #42464e;
  cursor: pointer;
}

.lm-sub-tab:hover:not(:disabled) {
  color: #165dff;
}

.lm-sub-tab.on {
  background: #fff;
  border-color: #165dff;
  color: #165dff;
  font-weight: 500;
}

.lm-sub-tab.is-disabled,
.lm-sub-tab:disabled {
  color: #c7ccd6;
  cursor: not-allowed;
}

.lm-switch-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 20px;
  border: 1px solid #e3e5eb;
  border-radius: 10px;
  background: #fafbfc;
  font-size: 14px;
  color: #1d2129;
}

.lm-switch-lg {
  width: 40px;
  height: 20px;
  position: relative;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #c7ccd6;
  cursor: pointer;
  transition: background 0.15s ease;
}

.lm-switch-lg:hover {
  background: #165dff;
}

.lm-switch-lg-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
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
