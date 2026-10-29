<script setup lang="ts">
import { computed, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import SqlEditor from '@/components/explore/SqlEditor.vue'
import { useExploreStore } from '@/stores/explore'
import { useLakeStore } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'

const store = useExploreStore()
const lake = useLakeStore()
const ui = useUiStore()

const editor = ref<InstanceType<typeof SqlEditor> | null>(null)
const expanded = ref<Set<string>>(new Set(['cat:cat_main']))

interface TreeRow {
  kind: 'cat' | 'db' | 'table'
  id: string
  depth: number
  icon: string
  label: string
  open: boolean
  qualified?: string
}

const treeRows = computed<TreeRow[]>(() => {
  const rows: TreeRow[] = []
  lake.catalogs.forEach((cat) => {
    const catOpen = expanded.value.has(`cat:${cat.id}`)
    rows.push({
      kind: 'cat',
      id: cat.id,
      depth: 0,
      icon: 'catalog',
      label: cat.name,
      open: catOpen,
    })
    if (!catOpen) return
    lake.dbsInCatalog(cat.id).forEach((db) => {
      const dbOpen = expanded.value.has(`db:${db.id}`)
      rows.push({
        kind: 'db',
        id: db.id,
        depth: 1,
        icon: 'db',
        label: db.name,
        open: dbOpen,
      })
      if (!dbOpen) return
      lake.tablesInDb(db.id).forEach((table) => {
        rows.push({
          kind: 'table',
          id: table.id,
          depth: 2,
          icon: 'table',
          label: table.name,
          open: false,
          qualified: `${cat.name}.${db.name}.${table.name}`,
        })
      })
    })
  })
  return rows
})

const sql = computed({
  get: () => store.activeTab?.sql ?? '',
  set: (value: string) => store.setSql(value),
})

const databaseOptions = computed(() => [
  { value: 'system', label: 'system' },
  ...lake.dbs.map((db) => ({ value: db.id, label: db.name })),
])

const resourceOptions = ['默认计算资源', '小规格计算资源', '大规格计算资源']

function toggle(key: string) {
  const next = new Set(expanded.value)
  if (next.has(key)) next.delete(key)
  else next.add(key)
  expanded.value = next
}

function onTreeClick(row: TreeRow) {
  if (row.kind === 'cat') {
    toggle(`cat:${row.id}`)
  } else if (row.kind === 'db') {
    toggle(`db:${row.id}`)
  } else if (row.qualified) {
    editor.value?.insertText(row.qualified)
  }
}

function expandAll() {
  const keys = new Set<string>()
  lake.catalogs.forEach((cat) => {
    keys.add(`cat:${cat.id}`)
    lake.dbsInCatalog(cat.id).forEach((db) => keys.add(`db:${db.id}`))
  })
  expanded.value = keys
}

function newQuery() {
  store.createTab()
}

function refreshCatalog() {
  ui.toast('数据目录已刷新')
}

function onSave() {
  store.saveCurrent()
  ui.toast('查询已保存')
}

function onRun() {
  store.run()
}
</script>

<template>
  <div class="ex-page">
    <aside class="ex-side">
      <div class="ex-side-block">
        <div class="ex-side-title">
          <span>查询列表</span>
          <button class="ex-icon-btn" type="button" title="新建查询" @click="newQuery">
            <SvgIcon name="plus" />
          </button>
          <button class="ex-icon-btn" type="button" title="展开全部" @click="expandAll">
            <SvgIcon name="expand" />
          </button>
        </div>
        <div class="ex-search">
          <SvgIcon name="search" />
          <input v-model="store.queryKeyword" type="text" placeholder="搜索查询" autocomplete="off">
        </div>
        <div class="ex-query-list">
          <button
            v-for="item in store.filteredQueries"
            :key="item.id"
            class="ex-query-item"
            :class="{ active: item.name === store.activeTab?.name }"
            type="button"
            @click="store.openSavedQuery(item)"
          >
            <span class="ex-query-name">{{ item.name }}</span>
            <span class="ex-query-time">{{ item.updatedAt }}</span>
          </button>
          <div v-if="!store.filteredQueries.length" class="ex-empty">没有匹配的查询</div>
        </div>
      </div>

      <div class="ex-side-block">
        <div class="ex-side-title">
          <span>数据目录</span>
          <button class="ex-icon-btn" type="button" title="刷新" @click="refreshCatalog">
            <SvgIcon name="refresh" />
          </button>
          <button class="ex-icon-btn" type="button" title="展开全部" @click="expandAll">
            <SvgIcon name="expand" />
          </button>
        </div>
        <div class="ex-tree">
          <button
            v-for="row in treeRows"
            :key="`${row.kind}:${row.id}`"
            class="ex-tree-row"
            type="button"
            :style="{ '--depth': row.depth }"
            @click="onTreeClick(row)"
          >
            <span class="ex-tree-chev">
              <SvgIcon v-if="row.kind !== 'table'" :name="row.open ? 'chevDown' : 'chevRight'" />
            </span>
            <span class="ex-tree-label">{{ row.label }}</span>
          </button>
          <div v-if="!treeRows.length" class="ex-empty">暂无数据目录</div>
        </div>
      </div>
    </aside>

    <section class="ex-main">
      <div class="ex-topbar">
        <div class="ex-title">
          数据探查
          <span class="ex-badge">Beta</span>
        </div>
        <div class="ex-top-actions">
          <button class="ex-top-btn" type="button">
            <SvgIcon name="chart" />
            可视化面板
          </button>
          <button class="ex-top-btn" type="button">
            <SvgIcon name="search" />
            向量检索
          </button>
          <button class="ex-top-btn is-accent" type="button" title="主题">
            <SvgIcon name="sun" />
          </button>
        </div>
      </div>

      <div class="ex-tabs">
        <button
          v-for="tab in store.tabs"
          :key="tab.id"
          class="ex-tab"
          :class="{ active: tab.id === store.activeTabId }"
          type="button"
          @click="store.selectTab(tab.id)"
        >
          <SvgIcon name="run" />
          {{ tab.name }}
          <span class="ex-tab-close" role="button" aria-label="关闭" @click.stop="store.closeTab(tab.id)">
            ×
          </span>
        </button>
      </div>

      <div class="ex-toolbar">
        <button class="ex-run-btn" type="button" :disabled="store.running" @click="onRun">
          <SvgIcon name="play" />
          运行(1000)
          <SvgIcon name="chevDown" />
        </button>

        <div class="ex-field">
          <label>Catalog</label>
          <div class="ex-select">
            <select v-model="store.catalogId">
              <option v-for="cat in lake.catalogs" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
            <SvgIcon name="chevDown" />
          </div>
        </div>

        <div class="ex-field">
          <label>Database</label>
          <div class="ex-select">
            <select v-model="store.databaseId">
              <option v-for="db in databaseOptions" :key="db.value" :value="db.value">{{ db.label }}</option>
            </select>
            <SvgIcon name="chevDown" />
          </div>
        </div>

        <div class="ex-toolbar-spacer" />

        <div class="ex-field">
          <div class="ex-select">
            <select v-model="store.resource">
              <option v-for="item in resourceOptions" :key="item" :value="item">{{ item }}</option>
            </select>
            <SvgIcon name="chevDown" />
          </div>
        </div>
      </div>

      <div class="ex-notice">
        <SvgIcon name="info" />
        当前 Catalog 允许数据读写准入，请前往目录配置关闭。
      </div>

      <SqlEditor
        ref="editor"
        v-model="sql"
        :last-saved-at="store.lastSavedAt"
        :auto-save="store.autoSave"
        @save="onSave"
      />

      <div class="ex-result">
        <div class="ex-result-title">运行结果</div>
        <div class="ex-result-head">
          <template v-if="store.errorMessage">
            <span class="ex-result-status is-error">
              <span class="ex-dot"><SvgIcon name="info" /></span>
              失败
            </span>
            <span class="ex-result-meta">{{ store.errorMessage }}</span>
          </template>
          <template v-else-if="store.result">
            <span class="ex-result-status">
              <span class="ex-dot"><SvgIcon name="check" /></span>
              成功
            </span>
            <span class="ex-result-meta">
              耗时 {{ store.result.elapsed }}ms · 返回 {{ store.result.rows.length }} 行
            </span>
          </template>
          <template v-else>
            <span class="ex-result-meta">点击「运行」执行当前 SQL</span>
          </template>

          <div class="ex-result-search">
            <SvgIcon name="search" />
            <input v-model="store.resultKeyword" type="text" placeholder="搜索文本字段" autocomplete="off">
          </div>
        </div>

        <div class="ex-result-body">
          <table v-if="store.result" class="ex-table">
            <thead>
              <tr>
                <th v-for="column in store.result.columns" :key="column">{{ column }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in store.filteredRows" :key="index">
                <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
              </tr>
              <tr v-if="!store.filteredRows.length">
                <td :colspan="store.result.columns.length" class="ex-empty">没有匹配的数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </div>
</template>
