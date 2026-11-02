import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export interface LasTableField {
  name: string
  type: string
}

export interface LasDataTable {
  name: string
  location: string
  tableType: string
  partitionType: string
  owner: string
  /** 表属性（Table properties），外部表以 EXTERNAL=true 标记 */
  properties?: Record<string, string>
  /** 表结构，用于数据探查生成结果集 */
  schema?: LasTableField[]
}

export interface LasCatalogDatabase {
  name: string
  tableCount: number
  location: string
  owner: string
  createdAt: string
  updatedAt: string
  tables: LasDataTable[]
  /** 该库下的函数 */
  functions: string[]
}

export interface LasCatalog {
  name: string
  owner: string
  location: string
  createdAt: string
  updatedAt: string
  databases: LasCatalogDatabase[]
}

/** 存储位置选择器中的节点：桶 → 表 */
export interface LasStorageNode {
  name: string
  type: 'bucket' | 'table'
  /** 选择该路径后自动解析出的字段结构 */
  schema?: LasTableField[]
  children?: LasStorageNode[]
}

/** 对象存储中可选的桶与表 */
export const LAS_STORAGE_FOLDERS: LasStorageNode[] = [
  {
    name: 'bucket1',
    type: 'bucket',
    children: [
      {
        name: 'tb1.lance',
        type: 'table',
        schema: [
          { name: 'id', type: 'int64' },
          { name: 'name', type: 'string' },
          { name: 'embedding', type: 'vector<float32>[512]' },
          { name: 'label', type: 'string' },
          { name: 'created_at', type: 'timestamp[us]' },
        ],
      },
      {
        name: 'tb2_parquet',
        type: 'table',
        schema: [
          { name: 'id', type: 'int64' },
          { name: 'device_id', type: 'string' },
          { name: 'temperature', type: 'float64' },
          { name: 'humidity', type: 'float64' },
          { name: 'ts', type: 'timestamp[us]' },
        ],
      },
      {
        name: 'tb3_paimon',
        type: 'table',
        schema: [
          { name: 'id', type: 'int64' },
          { name: 'clip_id', type: 'string' },
          { name: 'embedding', type: 'vector<float32>[512]' },
          { name: 'created_at', type: 'timestamp[us]' },
        ],
      },
      {
        name: 'tb4_iceberg',
        type: 'table',
        schema: [
          { name: 'id', type: 'int64' },
          { name: 'event_time', type: 'timestamp[us]' },
          { name: 'camera_id', type: 'string' },
          { name: 'score', type: 'double' },
        ],
      },
    ],
  },
]

const seedCatalogs: LasCatalog[] = [
  {
    name: 'catalog1',
    owner: '2530手机用户#QGDdVm',
    location: '-',
    createdAt: '2026-10-22 10:24:47',
    updatedAt: '2026-10-22 10:24:47',
    databases: [
      {
        name: 'default',
        tableCount: 2,
        location: 'file:/user/catalog1/warehouse/default.db',
        owner: '2530手机用户#QGDdVm',
        createdAt: '2026-10-22 10:24:47',
        updatedAt: '2026-10-22 10:24:47',
        tables: [
          {
            name: 'test',
            location: 'oss://user-hexadb/lance_tb2.lance',
            tableType: 'Lance',
            partitionType: '非分区表',
            owner: '2530手机用户#QGDdVm',
            schema: [
              { name: 'id', type: 'int64' },
              { name: 'name', type: 'string' },
              { name: 'source', type: 'string' },
              { name: 'duration', type: 'float64' },
              { name: 'created_at', type: 'timestamp[us]' },
            ],
          },
          {
            name: 'test3',
            location: 'oss://user-hexadb/lance_tb2.lance',
            tableType: 'Lance',
            partitionType: '非分区表',
            owner: '2530手机用户#QGDdVm',
            schema: [
              { name: 'id', type: 'int64' },
              { name: 'name', type: 'string' },
              { name: 'size', type: 'int64' },
              { name: 'created_at', type: 'timestamp[us]' },
            ],
          },
        ],
        functions: [],
      },
    ],
  },
  {
    name: 'catalog2',
    owner: '2530手机用户#QGDdVm',
    location: '-',
    createdAt: '2026-10-24 09:12:03',
    updatedAt: '2026-10-28 17:40:21',
    databases: [
      {
        name: 'default',
        tableCount: 0,
        location: 'file:/user/catalog2/warehouse/default.db',
        owner: '2530手机用户#QGDdVm',
        createdAt: '2026-10-24 09:12:03',
        updatedAt: '2026-10-24 09:12:03',
        tables: [],
        functions: [],
      },
      {
        name: 'db2',
        tableCount: 0,
        location: 'file:/user/catalog2/warehouse/db2.db',
        owner: '2530手机用户#QGDdVm',
        createdAt: '2026-10-25 11:20:18',
        updatedAt: '2026-10-28 17:40:21',
        tables: [],
        functions: [],
      },
    ],
  },
]

export const useLasCatalogStore = defineStore('las-catalog', () => {
  const catalogs = ref<LasCatalog[]>(seedCatalogs.map((c) => ({ ...c })))
  const selectedCatalog = ref<string>('catalog1')
  const selectedDatabase = ref<string>('default')
  const auditLog = ref(false)
  const treeQuery = ref('')

  const treeCatalogs = computed(() => {
    const q = treeQuery.value.trim().toLowerCase()
    if (!q) return catalogs.value
    return catalogs.value.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        c.databases.some((db) => db.name.toLowerCase().includes(q)),
    )
  })

  const catalog = computed(
    () => catalogs.value.find((c) => c.name === selectedCatalog.value) ?? null,
  )

  const database = computed(
    () =>
      catalog.value?.databases.find((d) => d.name === selectedDatabase.value) ?? null,
  )

  function selectCatalog(name: string) {
    selectedCatalog.value = name
    const first = catalogs.value.find((c) => c.name === name)?.databases[0]
    selectedDatabase.value = first?.name ?? ''
  }

  function selectDatabase(name: string) {
    selectedDatabase.value = name
  }

  function addCatalog(name: string) {
    const item: LasCatalog = {
      name,
      owner: '2530手机用户#QGDdVm',
      location: '-',
      createdAt: '2026-10-30 12:00:00',
      updatedAt: '2026-10-30 12:00:00',
      databases: [],
    }
    catalogs.value.push(item)
    return item
  }

  function addDatabase(catalogName: string, name: string) {
    const target = catalogs.value.find((c) => c.name === catalogName)
    if (!target) return null
    const item: LasCatalogDatabase = {
      name,
      tableCount: 0,
      location: `file:/user/${catalogName}/warehouse/${name}.db`,
      owner: '2530手机用户#QGDdVm',
      createdAt: '2026-10-30 12:00:00',
      updatedAt: '2026-10-30 12:00:00',
      tables: [],
      functions: [],
    }
    target.databases.push(item)
    return item
  }

  function addTable(
    catalogName: string,
    databaseName: string,
    name: string,
    extra: {
      /** 存储位置 */
      location?: string
      /** 表格式 */
      tableType?: string
      /** 表属性（Table properties） */
      properties?: Record<string, string>
      /** 字段结构 */
      schema?: LasTableField[]
    } = {},
  ) {
    const target = catalogs.value
      .find((c) => c.name === catalogName)
      ?.databases.find((d) => d.name === databaseName)
    if (!target) return null
    target.tables.push({
      name,
      location: extra.location ?? 'oss://user-hexadb/lance_tb2.lance',
      tableType: extra.tableType ?? 'Lance',
      partitionType: '非分区表',
      owner: '2530手机用户#QGDdVm',
      properties: extra.properties,
      schema: extra.schema ?? [
        { name: 'id', type: 'int64' },
        { name: 'name', type: 'string' },
        { name: 'created_at', type: 'timestamp[us]' },
      ],
    })
    target.tableCount = target.tables.length
    return target
  }

  return {
    catalogs,
    selectedCatalog,
    selectedDatabase,
    auditLog,
    treeQuery,
    treeCatalogs,
    catalog,
    database,
    selectCatalog,
    selectDatabase,
    addCatalog,
    addDatabase,
    addTable,
  }
})
