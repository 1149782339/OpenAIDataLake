import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export interface LasDataTable {
  name: string
  location: string
  tableType: string
  partitionType: string
  owner: string
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

const seedCatalogs: LasCatalog[] = [
  {
    name: 'hive',
    owner: '2530手机用户#QGDdVm',
    location: '-',
    createdAt: '2026-10-22 10:24:47',
    updatedAt: '2026-10-22 10:24:47',
    databases: [
      {
        name: 'default',
        tableCount: 2,
        location: 'file:/user/hive/warehouse/default.db',
        owner: '2530手机用户#QGDdVm',
        createdAt: '2026-10-22 10:24:47',
        updatedAt: '2026-10-22 10:24:47',
        tables: [
          {
            name: 'test',
            location: 'tos://user-hexadb/lance_tb2.lance',
            tableType: 'Lance',
            partitionType: '非分区表',
            owner: '2530手机用户#QGDdVm',
          },
          {
            name: 'test3',
            location: 'tos://user-hexadb/lance_tb2.lance',
            tableType: 'Lance',
            partitionType: '非分区表',
            owner: '2530手机用户#QGDdVm',
          },
        ],
        functions: [],
      },
    ],
  },
  {
    name: 'catalog1',
    owner: '2530手机用户#QGDdVm',
    location: '-',
    createdAt: '2026-10-24 09:12:03',
    updatedAt: '2026-10-28 17:40:21',
    databases: [
      {
        name: 'default',
        tableCount: 0,
        location: 'file:/user/catalog1/warehouse/default.db',
        owner: '2530手机用户#QGDdVm',
        createdAt: '2026-10-24 09:12:03',
        updatedAt: '2026-10-24 09:12:03',
        tables: [],
        functions: [],
      },
      {
        name: 'db2',
        tableCount: 0,
        location: 'file:/user/catalog1/warehouse/db2.db',
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
  const selectedCatalog = ref<string>('hive')
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

  function addTable(catalogName: string, databaseName: string, name: string) {
    const target = catalogs.value
      .find((c) => c.name === catalogName)
      ?.databases.find((d) => d.name === databaseName)
    if (!target) return null
    target.tables.push({
      name,
      location: 'tos://user-hexadb/lance_tb2.lance',
      tableType: 'Lance',
      partitionType: '非分区表',
      owner: '2530手机用户#QGDdVm',
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
