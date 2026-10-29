import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { SchemaField } from './datasets'
import { nowStamp } from '@/utils/format'

export interface Catalog {
  id: string
  name: string
  note?: string
}

export interface LakeDatabase {
  id: string
  name: string
  ossPath: string
  note?: string
  catalogId: string
  owner: string
  location: string
  createdAt: string
  updatedAt: string
}

export type IndexKind = 'vector' | 'scalar' | 'fts'
export type IndexStatus = 'ready' | 'building' | 'stale' | 'failed'

export interface TableIndex {
  id: string
  name: string
  kind: IndexKind
  type: string
  column: string
  params: string
  status: IndexStatus
  coverage: number
  unindexed: number
  size: string
  createdAt: string
}

export interface LakeTable {
  id: string
  name: string
  dbId: string
  catalogId: string
  external: boolean
  tableType: '外表' | '内表'
  format: string
  owner: string
  location: string
  createdAt: string
  updatedAt: string
  formatVersion: string
  rows: number
  size: string
  columns: number
  description: string
  lastOptimized: string
  schema: SchemaField[]
  indexes: TableIndex[]
  sourceDatasetId?: number | null
}

export const META_TABS = [
  { id: 'basic', label: '基本信息' },
  { id: 'data', label: '数据概况' },
  { id: 'indexes', label: '索引' },
  { id: 'optimize', label: '存储优化' },
] as const

export type MetaTabId = (typeof META_TABS)[number]['id']

export const useLakeStore = defineStore('lake', () => {
  const catalogs = ref<Catalog[]>([{ id: 'cat_main', name: 'catalog1', note: '多模态数据目录' }])

  const dbs = ref<LakeDatabase[]>([
    {
      id: 'db_main',
      name: 'default',
      ossPath: 'oss://default',
      note: '默认数据库',
      catalogId: 'cat_main',
      owner: '2530手机用户#QGDdVm',
      location: 'oss://user-hexadb/default.db',
      createdAt: '2026-10-22 10:30:00',
      updatedAt: '2026-10-29 03:33:08',
    },
    {
      id: 'db_2',
      name: 'db2',
      ossPath: 'oss://db2',
      note: '示例数据库',
      catalogId: 'cat_main',
      owner: 'data-team',
      location: 'oss://user-hexadb/db2.db',
      createdAt: '2026-09-10 09:00:00',
      updatedAt: '2026-10-28 11:20:00',
    },
  ])

  const tables = ref<LakeTable[]>([
    {
      id: 't_table1',
      name: 'table1',
      dbId: 'db_main',
      catalogId: 'cat_main',
      external: true,
      tableType: '外表',
      format: 'Parquet',
      owner: '2530手机用户#QGDdVm',
      location: 'oss://default/table1.parquet',
      createdAt: '2026-10-28 10:12:00',
      updatedAt: '2026-10-29 03:34:05',
      formatVersion: '2.0',
      rows: 128640,
      size: '86.42 MB',
      columns: 5,
      description: '外部表：数据存放在外部对象存储，平台侧不建索引，向量检索走 KNN 暴力扫描。',
      lastOptimized: '-',
      schema: [
        { name: 'id', type: 'int64', nullable: false, desc: '主键' },
        { name: 'device_id', type: 'string', nullable: false, desc: '设备编号' },
        { name: 'temperature', type: 'float64', nullable: true, desc: '温度' },
        { name: 'humidity', type: 'float64', nullable: true, desc: '湿度' },
        { name: 'ts', type: 'timestamp[us]', nullable: false, desc: '采集时间' },
      ],
      indexes: [],
    },
    {
      id: 't_table2',
      name: 'table2',
      dbId: 'db_2',
      catalogId: 'cat_main',
      external: false,
      tableType: '内表',
      format: 'Lance',
      owner: 'data-team',
      location: 'oss://db2/table2.lance',
      createdAt: '2026-10-20 15:40:00',
      updatedAt: '2026-10-29 02:10:00',
      formatVersion: '2.0',
      rows: 902,
      size: '2.79 MB',
      columns: 4,
      description: '内部表：数据由平台统一存储与维护，支持标量索引与向量索引。',
      lastOptimized: '2026-10-28 03:20:00',
      schema: [
        { name: 'id', type: 'int64', nullable: false, desc: '主键' },
        { name: 'text', type: 'string', nullable: true, desc: '原始文本' },
        { name: 'embedding', type: 'vector<float32>[768]', nullable: false, desc: '文本向量' },
        { name: 'split', type: 'string', nullable: true, desc: '数据集划分' },
      ],
      indexes: [
        {
          id: 'i1',
          name: 'idx_id_btree',
          kind: 'scalar',
          type: 'BTREE',
          column: 'id',
          params: '-',
          status: 'ready',
          coverage: 902,
          unindexed: 0,
          size: '0.2 MB',
          createdAt: '2026-10-20 16:02:00',
        },
        {
          id: 'i2',
          name: 'idx_embedding_ivf_rq',
          kind: 'vector',
          type: 'IVF-RQ',
          column: 'embedding',
          params: 'num_partitions=64, num_sub_vectors=16 · l2',
          status: 'ready',
          coverage: 902,
          unindexed: 0,
          size: '3.4 MB',
          createdAt: '2026-10-21 09:15:00',
        },
      ],
    },
  ])

  const selectedId = ref<string | null>('t_table1')
  const selectedDbId = ref<string | null>(null)
  const dbTab = ref('tables')
  const dbQuery = ref('')
  const dbPage = ref(1)
  const dbPageSize = ref(20)
  const query = ref('')
  const tab = ref<MetaTabId>('basic')
  const expanded = ref<Set<string>>(
    new Set(['cat:cat_main', 'db:db_main', 'db:db_2', 'grp:db_main:tables']),
  )

  const defaultDb = computed(() => dbs.value[0])

  function tableDbId(t: LakeTable | null | undefined): string {
    if (!t) return defaultDb.value?.id ?? ''
    return t.dbId || defaultDb.value?.id || ''
  }

  function dbById(id: string | null | undefined): LakeDatabase {
    return dbs.value.find((db) => db.id === id) ?? defaultDb.value!
  }

  function catalogById(id: string | null | undefined): Catalog {
    return catalogs.value.find((c) => c.id === id) ?? catalogs.value[0]
  }

  function dbCatalogId(db: LakeDatabase): string {
    return db.catalogId || catalogs.value[0]?.id || ''
  }

  function dbsInCatalog(catalogId: string): LakeDatabase[] {
    return dbs.value.filter((db) => dbCatalogId(db) === catalogId)
  }

  function tablesInDb(dbId: string): LakeTable[] {
    return tables.value.filter((t) => tableDbId(t) === dbId)
  }

  function tableById(id: string | null | undefined): LakeTable | null {
    return tables.value.find((t) => t.id === id) ?? null
  }

  const currentTable = computed(() => tableById(selectedId.value))

  function expandTableContext(t: LakeTable | null) {
    if (!t) return
    const db = dbById(tableDbId(t))
    expanded.value.add(`cat:${dbCatalogId(db)}`)
    expanded.value.add(`db:${db.id}`)
    expanded.value.add(`grp:${db.id}:tables`)
  }

  function toggleExpanded(key: string) {
    const next = new Set(expanded.value)
    if (next.has(key)) next.delete(key)
    else next.add(key)
    expanded.value = next
  }

  function selectTable(id: string) {
    const t = tableById(id)
    if (!t) return
    selectedId.value = t.id
    selectedDbId.value = tableDbId(t)
    expandTableContext(t)
  }

  function selectDb(id: string) {
    selectedDbId.value = id
    selectedId.value = null
    dbTab.value = 'tables'
    dbQuery.value = ''
    dbPage.value = 1
  }

  function createDatabase(payload: { name: string; ossPath: string; note: string }) {
    const db: LakeDatabase = {
      id: `db_${Date.now()}`,
      name: payload.name,
      ossPath: payload.ossPath,
      note: payload.note,
      catalogId: catalogs.value[0]?.id ?? 'cat_main',
      owner: '2530手机用户#QGDdVm',
      location: `oss://user-hexadb/${payload.name}.db`,
      createdAt: nowStamp(),
      updatedAt: nowStamp(),
    }
    dbs.value.push(db)
    expanded.value = new Set([...expanded.value, `db:${db.id}`])
    return db
  }

  function updateDatabase(id: string, payload: { name: string; ossPath: string; note: string }) {
    const db = dbs.value.find((item) => item.id === id)
    if (!db) return
    db.name = payload.name
    db.ossPath = payload.ossPath
    db.note = payload.note
    db.updatedAt = nowStamp()
  }

  function removeTable(id: string) {
    tables.value = tables.value.filter((item) => item.id !== id)
    if (selectedId.value === id) {
      selectedId.value = tables.value.length ? tables.value[0].id : null
    }
  }

  /** 数据库下的数据表（支持搜索过滤） */
  const filteredDbTables = computed(() => {
    const q = (dbQuery.value || '').trim().toLowerCase()
    if (!selectedDbId.value) return []
    return tablesInDb(selectedDbId.value).filter(
      (t) => !q || t.name.toLowerCase().includes(q),
    )
  })

  const dbPageCount = computed(() =>
    Math.max(1, Math.ceil(filteredDbTables.value.length / dbPageSize.value)),
  )

  const dbPageRows = computed(() => {
    const page = Math.min(Math.max(1, dbPage.value), dbPageCount.value)
    const start = (page - 1) * dbPageSize.value
    return filteredDbTables.value.slice(start, start + dbPageSize.value)
  })

  return {
    catalogs,
    dbs,
    tables,
    selectedId,
    selectedDbId,
    dbTab,
    dbQuery,
    dbPage,
    dbPageSize,
    query,
    tab,
    expanded,
    currentTable,
    tableDbId,
    dbById,
    catalogById,
    dbCatalogId,
    dbsInCatalog,
    tablesInDb,
    tableById,
    expandTableContext,
    toggleExpanded,
    selectTable,
    selectDb,
    createDatabase,
    updateDatabase,
    removeTable,
    filteredDbTables,
    dbPageCount,
    dbPageRows,
  }
})
