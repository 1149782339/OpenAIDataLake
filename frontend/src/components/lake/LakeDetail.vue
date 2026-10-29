<script setup lang="ts">
import { computed } from 'vue'
import CopyButton from '@/components/CopyButton.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { useDatasetStore } from '@/stores/datasets'
import { META_TABS, useLakeStore, type TableIndex } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'
import { fmtNum } from '@/utils/format'

const lake = useLakeStore()
const datasetStore = useDatasetStore()
const ui = useUiStore()

const emit = defineEmits<{
  editDb: [id: string]
  createTable: []
  newIndex: []
  deleteTable: []
}>()

const table = computed(() => lake.currentTable)
/** 当前上下文所属数据库：选中表时取表所在的库，否则取左侧选中的库 */
const db = computed(() => {
  if (table.value) return lake.dbById(lake.tableDbId(table.value))
  return lake.selectedDbId ? lake.dbById(lake.selectedDbId) : null
})
const catalog = computed(() => (db.value ? lake.catalogById(lake.dbCatalogId(db.value)) : null))

const linkedDatasets = computed(() => {
  const current = table.value
  if (!current) return []
  return datasetStore.datasets.filter((d) => d.sourceTableId === current.id)
})

const sourceDataset = computed(() => {
  const current = table.value
  if (!current || !current.sourceDatasetId) return null
  return datasetStore.datasetById(current.sourceDatasetId)
})

function indexSummary(t: { indexes: TableIndex[] }) {
  if (!t.indexes.length) return '0 个'
  const vector = t.indexes.filter((i) => i.kind === 'vector').length
  const scalar = t.indexes.filter((i) => i.kind === 'scalar').length
  const fts = t.indexes.filter((i) => i.kind === 'fts').length
  const parts: string[] = []
  if (vector) parts.push(`${vector} 向量`)
  if (scalar) parts.push(`${scalar} 标量`)
  if (fts) parts.push(`${fts} 全文`)
  return `${t.indexes.length} 个（${parts.join(' / ')}）`
}

function tableFormat(t: { format: string; location: string }) {
  if (t.format) return t.format
  const m = String(t.location || '').match(/\.([A-Za-z0-9]+)$/)
  return m ? m[1].toUpperCase() : 'Lance'
}

function tableProperties(t: {
  external: boolean
  createdAt: string
  updatedAt: string
}) {
  return (
    `EXTERNAL=${t.external ? 'true' : 'false'}` +
    `, createTime=${Date.parse(String(t.createdAt).replace(/-/g, '/')) || 0}` +
    `, updateTime=${Date.parse(String(t.updatedAt).replace(/-/g, '/')) || 0}`
  )
}

function indexCounts(t: { schema: { name: string }[]; indexes: TableIndex[] }) {
  const counts: Record<string, number> = {}
  t.indexes.forEach((idx) => {
    counts[idx.column] = (counts[idx.column] || 0) + 1
  })
  return counts
}

const statusMap: Record<string, [string, string]> = {
  ready: ['ready', '可用'],
  building: ['building', '构建中'],
  stale: ['stale', '待优化'],
  failed: ['failed', '失败'],
}

function indexStatusChip(status: string) {
  const item = statusMap[status] ?? statusMap.ready
  return item
}

const kindLabels: Record<string, string> = { vector: '向量', scalar: '标量', fts: '全文' }

function goDbPage(page: number) {
  lake.dbPage = Math.min(Math.max(1, page), lake.dbPageCount)
}

function onDbPageSize(event: Event) {
  lake.dbPageSize = Number((event.target as HTMLSelectElement).value) || 20
  lake.dbPage = 1
}

function onDbSearch(event: Event) {
  lake.dbQuery = (event.target as HTMLInputElement).value
  lake.dbPage = 1
}

function selectDbTable(id: string) {
  lake.selectTable(id)
}

function removeDbTable(id: string) {
  const t = lake.tableById(id)
  if (!t) return
  const ok = window.confirm(`确定删除表 "${t.name}" 吗？该操作不可恢复。`)
  if (!ok) return
  lake.removeTable(id)
  ui.toast('表已删除（演示）')
}

function compact() {
  const t = table.value
  if (!t) return
  t.lastOptimized = new Date().toISOString().slice(0, 19).replace('T', ' ')
  ui.toast('小文件合并完成，fragment 数量已减少（演示）')
}

function optimizeIndexes() {
  const t = table.value
  if (!t) return
  t.indexes.forEach((idx) => {
    idx.unindexed = 0
    idx.coverage = t.rows
    if (idx.status === 'stale') idx.status = 'ready'
  })
  ui.toast('索引优化完成，已覆盖全部行（演示）')
}

function refreshStats() {
  ui.toast('统计信息已刷新（演示）')
}

function deleteIndex(indexId: string) {
  const t = table.value
  if (!t) return
  const idx = t.indexes.find((i) => i.id === indexId)
  if (!idx) return
  const ok = window.confirm(`确定删除索引 "${idx.name}" 吗？`)
  if (!ok) return
  t.indexes = t.indexes.filter((i) => i.id !== indexId)
  ui.toast('索引已删除（演示）')
}

function optimizeIndex(indexId: string) {
  const t = table.value
  if (!t) return
  const idx = t.indexes.find((i) => i.id === indexId)
  if (!idx) return
  idx.status = 'building'
  ui.toast(`正在优化索引 "${idx.name}"…`)
  const tableId = t.id
  window.setTimeout(() => {
    const target = lake.tableById(tableId)
    const item = target?.indexes.find((i) => i.id === indexId)
    if (!item) return
    item.status = 'ready'
    item.coverage = target!.rows
    item.unindexed = 0
    ui.toast(`索引 "${item.name}" 优化完成`)
  }, 1800)
}
</script>

<template>
  <!-- ==================== 数据库详情 ==================== -->
  <template v-if="!lake.selectedId && db">
    <div class="meta-breadcrumb">
      <span>{{ catalog?.name }}</span>
      <span class="sep">›</span>
      <span class="crumb-ic"><SvgIcon name="db" /></span>
      <span class="cur">{{ db.name }}</span>
      <CopyButton :value="db.name" title="复制库名" />
    </div>

    <div class="meta-title-row">
      <div class="meta-title-left">
        <span class="meta-table-ic"><SvgIcon name="db" /></span>
        <h2>{{ db.name }}</h2>
      </div>
      <div class="meta-actions">
        <button class="btn-primary" type="button" @click="emit('editDb', db.id)">编辑</button>
      </div>
    </div>

    <div class="meta-sub">
      <span class="sub-item">Owner: {{ db.owner || '-' }}</span>
      <span class="sub-item">
        存储位置: <a href="javascript:void(0)">{{ db.location || db.ossPath }}</a>
        <CopyButton :value="db.location || db.ossPath" title="复制存储位置" />
      </span>
      <span class="sub-item">创建时间: {{ db.createdAt || '-' }}</span>
      <span class="sub-item">更新时间: {{ db.updatedAt || '-' }}</span>
    </div>

    <div class="meta-tabs">
      <button class="meta-tab active" type="button" data-db-tab="tables">数据表</button>
    </div>

    <div class="meta-tab-body">
      <div class="toolbar" style="margin: 0 0 14px">
        <button class="btn-primary" type="button" @click="emit('createTable')">
          <SvgIcon name="plus" />
          创建数据表
        </button>
        <div class="toolbar-right">
          <div class="search" style="width: 320px">
            <SvgIcon name="search" />
            <input
              type="text"
              placeholder="输入名称搜索"
              autocomplete="off"
              :value="lake.dbQuery"
              @input="onDbSearch"
            >
          </div>
        </div>
      </div>

      <div class="table-box scroll">
        <table class="mini-table index-table">
          <thead>
            <tr>
              <th>数据表名称</th>
              <th>存储位置</th>
              <th>表类型</th>
              <th>owner</th>
              <th>创建时间</th>
              <th style="text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in lake.dbPageRows" :key="t.id">
              <td>
                <button class="ds-link" type="button" @click="selectDbTable(t.id)">{{ t.name }}</button>
              </td>
              <td class="mono">{{ t.location }}</td>
              <td>{{ t.external ? '外表' : '内表' }}</td>
              <td>{{ t.owner }}</td>
              <td class="nowrap">{{ t.createdAt }}</td>
              <td>
                <div class="actions" style="justify-content: flex-end">
                  <button class="link-btn" type="button" @click="selectDbTable(t.id)">编辑</button>
                  <button class="link-btn danger-link" type="button" @click="removeDbTable(t.id)">
                    删除
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="!lake.dbPageRows.length">
              <td colspan="6" class="muted" style="padding: 26px; text-align: center">
                没有匹配的数据表
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-total">共 {{ lake.filteredDbTables.length }} 条</span>
        <div class="page-buttons">
          <button
            v-for="i in lake.dbPageCount"
            :key="i"
            class="page-btn"
            :class="{ active: i === lake.dbPage }"
            type="button"
            @click="goDbPage(i)"
          >
            {{ i }}
          </button>
          <button
            class="page-btn"
            type="button"
            :disabled="lake.dbPage >= lake.dbPageCount"
            @click="goDbPage(lake.dbPage + 1)"
          >
            <SvgIcon name="chevRight" />
          </button>
        </div>
        <select class="page-size" :value="lake.dbPageSize" @change="onDbPageSize">
          <option :value="20">20条/页</option>
          <option :value="50">50条/页</option>
          <option :value="100">100条/页</option>
        </select>
      </div>
    </div>
  </template>

  <!-- ==================== 表详情 ==================== -->
  <template v-else-if="table">
    <div class="meta-breadcrumb">
      <span>{{ catalog?.name }}</span>
      <span class="sep">›</span>
      <span class="crumb-ic"><SvgIcon name="db" /></span>
      <span>{{ db?.name }}</span>
      <span class="sep">›</span>
      <span class="crumb-table">表</span>
      <span class="sep">›</span>
      <span class="crumb-ic"><SvgIcon name="table" /></span>
      <span class="cur">{{ table.name }}</span>
      <CopyButton :value="table.name" title="复制表名" />
    </div>

    <div class="meta-title-row">
      <div class="meta-title-left">
        <span class="meta-table-ic"><SvgIcon name="table" /></span>
        <h2>{{ table.name }}</h2>
        <span class="tag">{{ tableFormat(table) }}</span>
        <span class="tag">{{ table.external ? '外表' : '内表' }}</span>
      </div>
      <div class="meta-actions">
        <button class="btn-ghost" type="button" @click="emit('deleteTable')">删除</button>
        <button class="btn-primary" type="button" @click="emit('createTable')">创建表</button>
      </div>
    </div>

    <div class="meta-sub">
      <span class="sub-item">Owner: {{ table.owner }}</span>
      <span class="sub-item">
        存储位置: <a href="javascript:void(0)">{{ table.location }}</a>
        <CopyButton :value="table.location" title="复制存储位置" />
      </span>
      <span class="sub-item">创建时间: {{ table.createdAt }}</span>
      <span class="sub-item">更新时间: {{ table.updatedAt }}</span>
    </div>

    <div class="meta-tabs">
      <button
        v-for="tab in META_TABS"
        :key="tab.id"
        class="meta-tab"
        :class="{ active: lake.tab === tab.id }"
        type="button"
        @click="lake.tab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="meta-tab-body">
      <!-- ---------- 基本信息 ---------- -->
      <template v-if="lake.tab === 'basic'">
        <div class="meta-section">
          <h3>基本详情</h3>
          <div class="kv-grid">
            <div class="k">数据表名称</div>
            <div class="v">{{ table.name }}</div>
            <div class="k">owner</div>
            <div class="v">{{ table.owner }}</div>
            <div class="k">所属数据目录</div>
            <div class="v">{{ catalog?.name }}</div>
            <div class="k">创建时间</div>
            <div class="v">{{ table.createdAt }}</div>
            <div class="k">所属数据库</div>
            <div class="v">{{ db?.name }}</div>
            <div class="k">更新时间</div>
            <div class="v">{{ table.updatedAt }}</div>
            <div class="k">存储位置</div>
            <div class="v mono">
              <a href="javascript:void(0)">{{ table.location }}</a>
              <CopyButton :value="table.location" title="复制存储位置" />
            </div>
            <div class="k">数据表备注</div>
            <div class="v">{{ table.description || '-' }}</div>
            <template v-if="table.sourceDatasetId">
              <div class="k">来源数据集</div>
              <div class="v">
                <span v-if="sourceDataset" class="tag blue">{{ sourceDataset.name }}</span>
                <span v-else class="muted">已删除</span>
              </div>
            </template>
          </div>
        </div>

        <div class="meta-section">
          <h3>格式与序列化</h3>
          <div class="kv-grid">
            <div class="k">存储格式</div>
            <div class="v">{{ tableFormat(table) }}</div>
            <div class="k">输入格式</div>
            <div class="v">{{ tableFormat(table) }}</div>
            <div class="k">输出格式</div>
            <div class="v">{{ tableFormat(table) }}</div>
            <div class="k">压缩格式</div>
            <div class="v">-</div>
            <div class="k">格式版本</div>
            <div class="v">{{ table.formatVersion || '2.0' }}</div>
            <div class="k">Table properties</div>
            <div class="v mono">{{ tableProperties(table) }}</div>
          </div>
        </div>

        <div class="meta-section">
          <h3>字段信息</h3>
          <div class="table-box scroll">
            <table class="mini-table schema-table">
              <thead>
                <tr>
                  <th>字段名称</th>
                  <th>字段类型</th>
                  <th>字段描述</th>
                  <th>索引数量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="col in table.schema" :key="col.name">
                  <td class="mono">{{ col.name }}</td>
                  <td class="mono muted">{{ col.type }}</td>
                  <td class="muted">{{ col.desc || '-' }}</td>
                  <td class="muted">{{ indexCounts(table)[col.name] || 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="meta-section">
          <h3>统计</h3>
          <div class="kv-grid">
            <div class="k">行数</div>
            <div class="v">{{ fmtNum(table.rows) }}</div>
            <div class="k">列数</div>
            <div class="v">{{ table.columns }}</div>
            <div class="k">大小</div>
            <div class="v">{{ table.size }}</div>
            <div class="k">索引</div>
            <div class="v">{{ indexSummary(table) }}</div>
          </div>
        </div>

        <div class="meta-section">
          <h3>关联数据集</h3>
          <div class="kv-grid">
            <div class="k">关联数据集</div>
            <div class="v">
              <template v-if="linkedDatasets.length">
                <span v-for="d in linkedDatasets" :key="d.id" class="tag blue">{{ d.name }}</span>
              </template>
              <span v-else class="muted" style="color: var(--text-3)">未发布</span>
            </div>
          </div>
        </div>
      </template>

      <!-- ---------- 数据概况 ---------- -->
      <template v-else-if="lake.tab === 'data'">
        <div class="stat-grid">
          <div class="stat-card">
            <div class="label">行数</div>
            <div class="value">{{ fmtNum(table.rows) }}</div>
          </div>
          <div class="stat-card">
            <div class="label">大小</div>
            <div class="value">{{ table.size }}</div>
          </div>
          <div class="stat-card">
            <div class="label">列数</div>
            <div class="value">{{ table.columns }}</div>
          </div>
        </div>

        <div class="meta-section">
          <h3>索引概览</h3>
          <div class="kv-grid">
            <div class="k">索引数量</div>
            <div class="v">{{ indexSummary(table) }}</div>
            <div class="k">未索引行</div>
            <div class="v">
              {{ fmtNum(table.indexes.reduce((n, i) => n + (i.unindexed || 0), 0)) }}
            </div>
            <div class="k">最近优化</div>
            <div class="v">{{ table.lastOptimized }}</div>
            <div class="k">存储位置</div>
            <div class="v mono">{{ table.location }}</div>
          </div>
        </div>
      </template>

      <!-- ---------- 索引 ---------- -->
      <template v-else-if="lake.tab === 'indexes'">
        <template v-if="table.external">
          <div class="panel-head"><h3>索引</h3></div>
          <p class="section-sub">外表的数据由外部系统维护，平台侧不构建索引。</p>
          <div class="meta-empty">
            外表不支持索引。外表检索时由平台走 KNN 暴力扫描，无需索引加速。
          </div>
        </template>
        <template v-else>
          <div class="panel-head">
            <h3>索引（{{ table.indexes.length }}）</h3>
            <button class="btn-primary" type="button" @click="emit('newIndex')">新建索引</button>
          </div>
          <p class="section-sub">索引在后台异步构建；新写入的数据需要优化索引后才能被检索。</p>
          <div v-if="table.indexes.length" class="table-box scroll">
            <table class="mini-table index-table">
              <thead>
                <tr>
                  <th>名称</th>
                  <th>字段 / 参数</th>
                  <th>状态</th>
                  <th>覆盖行数</th>
                  <th>大小 / 创建时间</th>
                  <th style="text-align: right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="idx in table.indexes" :key="idx.id">
                  <td>
                    <div class="mono">{{ idx.name }}</div>
                    <div class="muted">{{ idx.type }} · {{ kindLabels[idx.kind] || '' }}</div>
                  </td>
                  <td>
                    <div class="mono">{{ idx.column }}</div>
                    <div class="muted">{{ idx.params }}</div>
                  </td>
                  <td>
                    <span class="status" :class="indexStatusChip(idx.status)[0]">
                      {{ indexStatusChip(idx.status)[1] }}
                    </span>
                  </td>
                  <td>
                    {{ fmtNum(idx.coverage) }} / {{ fmtNum(table.rows) }}
                    <div v-if="idx.unindexed" class="muted">未索引 {{ fmtNum(idx.unindexed) }}</div>
                  </td>
                  <td class="muted nowrap">
                    {{ idx.size }}
                    <div>{{ idx.createdAt }}</div>
                  </td>
                  <td>
                    <div style="display: flex; gap: 12px; justify-content: flex-end">
                      <button class="link-btn" type="button" @click="optimizeIndex(idx.id)">
                        优化
                      </button>
                      <button class="link-btn" type="button" @click="deleteIndex(idx.id)">删除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="meta-empty">还没有索引。向量、标量或全文索引都可以在这里创建。</div>
        </template>
      </template>

      <!-- ---------- 存储优化 ---------- -->
      <template v-else>
        <div class="panel-head"><h3>存储优化</h3></div>
        <div class="opt-grid">
          <div class="opt-card">
            <h4>小文件合并</h4>
            <p>合并 fragment 与索引文件，减少文件数量，提升扫描效率。</p>
            <div class="opt-meta">最近优化：{{ table.lastOptimized }}</div>
            <button class="btn-ghost" type="button" @click="compact">立即合并</button>
          </div>
          <div class="opt-card">
            <h4>索引优化</h4>
            <p>重建或增量优化索引，覆盖新写入但尚未索引的行。</p>
            <div class="opt-meta">
              未索引行：{{ fmtNum(table.indexes.reduce((n, i) => n + (i.unindexed || 0), 0)) }}
            </div>
            <button class="btn-ghost" type="button" @click="optimizeIndexes">优化索引</button>
          </div>
          <div class="opt-card">
            <h4>统计信息</h4>
            <p>刷新行数、大小与列统计信息。</p>
            <div class="opt-meta">行数 / 大小来自表元数据</div>
            <button class="btn-ghost" type="button" @click="refreshStats">刷新统计</button>
          </div>
        </div>
      </template>
    </div>
  </template>

  <div v-else class="meta-empty">请选择一张表</div>
</template>
