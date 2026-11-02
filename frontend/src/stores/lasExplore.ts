import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useLasCatalogStore } from './lasCatalog'
import { runMockSql, type SqlColumn, type SqlResult } from '@/utils/sql'

export interface LasSavedQuery {
  id: string
  name: string
  updatedAt: string
  sql: string
}

export interface LasExploreTab {
  id: string
  name: string
  sql: string
}

const DEFAULT_SQL = 'select id, name, created_at from hive.default.test;'

/** 表未声明结构时使用的兜底结构 */
const FALLBACK_SCHEMA: SqlColumn[] = [
  { name: 'id', type: 'int64' },
  { name: 'name', type: 'string' },
  { name: 'value', type: 'double' },
  { name: 'created_at', type: 'timestamp[us]' },
]

function stamp(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const useLasExploreStore = defineStore('las-explore', () => {
  const catalog = useLasCatalogStore()

  const savedQueries = ref<LasSavedQuery[]>([
    { id: 'q1', name: 'Untitled-1', updatedAt: stamp(), sql: DEFAULT_SQL },
    {
      id: 'q2',
      name: '数据表概览',
      updatedAt: '10-28 18:02',
      sql: 'select id, name, duration from hive.default.test;',
    },
    {
      id: 'q3',
      name: '数据表采样',
      updatedAt: '10-27 09:41',
      sql: 'select * from hive.default.test3 limit 10;',
    },
  ])

  const tabs = ref<LasExploreTab[]>([
    { id: 't1', name: 'Untitled-1', sql: DEFAULT_SQL },
  ])
  const activeTabId = ref('t1')

  const queryKeyword = ref('')
  const catalogName = ref(catalog.selectedCatalog)
  const databaseName = ref(catalog.selectedDatabase)
  const resource = ref('默认计算资源')

  const running = ref(false)
  const result = ref<SqlResult | null>(null)
  const errorMessage = ref('')
  const lastSavedAt = ref(stamp())
  const autoSave = ref(true)
  const resultKeyword = ref('')

  const activeTab = computed(
    () => tabs.value.find((t) => t.id === activeTabId.value) ?? tabs.value[0],
  )

  const filteredQueries = computed(() => {
    const q = queryKeyword.value.trim().toLowerCase()
    if (!q) return savedQueries.value
    return savedQueries.value.filter((item) => item.name.toLowerCase().includes(q))
  })

  const filteredRows = computed(() => {
    const data = result.value
    if (!data) return []
    const q = resultKeyword.value.trim().toLowerCase()
    if (!q) return data.rows
    return data.rows.filter((row) =>
      row.some((cell) => String(cell ?? '').toLowerCase().includes(q)),
    )
  })

  /** 当前 Catalog 下的数据库 */
  const databases = computed(
    () => catalog.catalogs.find((c) => c.name === catalogName.value)?.databases ?? [],
  )

  /** 已知表（供演示环境的 SQL 执行生成结果集） */
  const knownTables = computed(() => {
    const list: { name: string; schema: SqlColumn[] }[] = []
    catalog.catalogs.forEach((cat) => {
      cat.databases.forEach((db) => {
        db.tables.forEach((table) => {
          list.push({
            name: table.name,
            schema: table.schema?.map((f) => ({ name: f.name, type: f.type })) ?? FALLBACK_SCHEMA,
          })
        })
      })
    })
    return list
  })

  function createTab(name?: string) {
    const tab: LasExploreTab = {
      id: `t_${Date.now()}`,
      name: name ?? `Untitled-${tabs.value.length + 1}`,
      sql: '',
    }
    tabs.value.push(tab)
    activeTabId.value = tab.id
    result.value = null
    errorMessage.value = ''
    return tab
  }

  function closeTab(id: string) {
    const index = tabs.value.findIndex((t) => t.id === id)
    if (index < 0) return
    tabs.value.splice(index, 1)
    if (!tabs.value.length) {
      createTab()
      return
    }
    if (activeTabId.value === id) {
      activeTabId.value = tabs.value[Math.min(index, tabs.value.length - 1)].id
    }
  }

  function selectTab(id: string) {
    activeTabId.value = id
  }

  function setSql(sql: string) {
    const tab = activeTab.value
    if (tab) tab.sql = sql
  }

  function openSavedQuery(query: LasSavedQuery) {
    const existing = tabs.value.find((t) => t.name === query.name)
    if (existing) {
      activeTabId.value = existing.id
      return
    }
    tabs.value.push({ id: `t_${Date.now()}`, name: query.name, sql: query.sql })
    activeTabId.value = tabs.value[tabs.value.length - 1].id
  }

  function saveCurrent() {
    const tab = activeTab.value
    if (!tab) return
    const saved = savedQueries.value.find((q) => q.name === tab.name)
    if (saved) {
      saved.sql = tab.sql
      saved.updatedAt = stamp()
    } else {
      savedQueries.value.unshift({
        id: `q_${Date.now()}`,
        name: tab.name,
        updatedAt: stamp(),
        sql: tab.sql,
      })
    }
    lastSavedAt.value = stamp()
  }

  function run() {
    const tab = activeTab.value
    if (!tab) return
    running.value = true
    errorMessage.value = ''
    result.value = null

    window.setTimeout(() => {
      const outcome = runMockSql(tab.sql, knownTables.value)
      running.value = false
      if (outcome.ok) {
        result.value = outcome.result
      } else {
        errorMessage.value = outcome.message
      }
    }, 260)
  }

  return {
    savedQueries,
    tabs,
    activeTabId,
    queryKeyword,
    catalogName,
    databaseName,
    resource,
    running,
    result,
    errorMessage,
    lastSavedAt,
    autoSave,
    resultKeyword,
    activeTab,
    filteredQueries,
    filteredRows,
    databases,
    knownTables,
    createTab,
    closeTab,
    selectTab,
    setSql,
    openSavedQuery,
    saveCurrent,
    run,
  }
})
