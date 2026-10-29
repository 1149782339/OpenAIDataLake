import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useLakeStore } from './lake'
import { runMockSql, type SqlResult } from '@/utils/sql'

export interface SavedQuery {
  id: string
  name: string
  updatedAt: string
  sql: string
}

export interface ExploreTab {
  id: string
  name: string
  sql: string
}

const DEFAULT_SQL =
  'select tmp1.id, tmp2.id from catalog1.db1.tbtest1 tmp1, catalog1.db1.tbtest1 as tmp2;'

function stamp(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const useExploreStore = defineStore('explore', () => {
  const lake = useLakeStore()

  const savedQueries = ref<SavedQuery[]>([
    { id: 'q1', name: 'Untitled-1', updatedAt: stamp(), sql: DEFAULT_SQL },
    {
      id: 'q2',
      name: '每日销售汇总',
      updatedAt: '10-28 18:02',
      sql: 'select id, device_id, temperature, humidity, ts from catalog1.db1.tbtest1;',
    },
    {
      id: 'q3',
      name: '传感器异常检测',
      updatedAt: '10-27 09:41',
      sql: "select * from catalog1.db1.tbtest1 where temperature > 12 limit 10;",
    },
  ])

  const tabs = ref<ExploreTab[]>([
    { id: 't1', name: 'Untitled-1', sql: DEFAULT_SQL },
  ])
  const activeTabId = ref('t1')

  const queryKeyword = ref('')
  const catalogId = ref(lake.catalogs[0]?.id ?? '')
  const databaseId = ref('system')
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

  function createTab(name?: string) {
    const tab: ExploreTab = {
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

  function openSavedQuery(query: SavedQuery) {
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
      const knownTables = lake.tables.map((t) => ({
        name: t.name,
        schema: t.schema.map((c) => ({ name: c.name, type: c.type })),
      }))
      const outcome = runMockSql(tab.sql, knownTables)
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
    catalogId,
    databaseId,
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
    createTab,
    closeTab,
    selectTab,
    setSql,
    openSavedQuery,
    saveCurrent,
    run,
  }
})
