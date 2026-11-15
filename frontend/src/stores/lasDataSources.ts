import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  dataSourceCatalog,
  listDataSources,
  type DataSourceCatalog,
  type DataSourceItem,
} from '@/api/console'

export type { DataSourceEngine } from '@/api/console'

export const useLasDataSourceStore = defineStore('las-data-sources', () => {
  const items = ref<DataSourceItem[]>([])
  const catalog = ref<DataSourceCatalog[]>([])

  async function load() {
    items.value = await listDataSources()
  }

  async function loadCatalog() {
    catalog.value = await dataSourceCatalog()
  }

  function findByName(name: string) {
    return items.value.find((item) => item.name.toLowerCase() === name.toLowerCase())
  }

  return { items, catalog, load, loadCatalog, findByName }
})
