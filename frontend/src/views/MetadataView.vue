<script setup lang="ts">
import { ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import CatalogTree from '@/components/lake/CatalogTree.vue'
import CreateDbModal from '@/components/lake/CreateDbModal.vue'
import CreateTablePage from '@/components/lake/CreateTablePage.vue'
import IndexModal from '@/components/lake/IndexModal.vue'
import LakeDetail from '@/components/lake/LakeDetail.vue'
import { useLakeStore } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'

const lake = useLakeStore()
const ui = useUiStore()

const dbModalOpen = ref(false)
const editingDbId = ref<string | null>(null)
const tablePageOpen = ref(false)
const indexModalOpen = ref(false)

function openCreateDb() {
  editingDbId.value = null
  dbModalOpen.value = true
}

function openEditDb(id: string) {
  editingDbId.value = id
  dbModalOpen.value = true
}

function refresh() {
  ui.toast('库表已刷新')
}

function deleteTable() {
  const t = lake.currentTable
  if (!t) return
  const ok = window.confirm(`确定删除表 "${t.name}" 吗？该操作不可恢复。`)
  if (!ok) return
  lake.removeTable(t.id)
  ui.toast('表已删除（演示）')
}
</script>

<template>
  <main class="main meta-main">
    <aside class="catalog-panel">
      <div class="catalog-head">
        <h2>Catalog</h2>
        <div class="catalog-actions">
          <button class="icon-btn" type="button" title="创建数据库" @click="openCreateDb">
            <SvgIcon name="plus" />
          </button>
          <button class="icon-btn" type="button" title="刷新" @click="refresh">
            <SvgIcon name="refresh" />
          </button>
        </div>
      </div>

      <div class="catalog-search">
        <SvgIcon name="search" />
        <input v-model="lake.query" type="text" placeholder="请输入Catalog名称" autocomplete="off">
      </div>

      <CatalogTree />
    </aside>

    <section class="meta-detail">
      <LakeDetail
        @edit-db="openEditDb"
        @create-table="tablePageOpen = true"
        @new-index="indexModalOpen = true"
        @delete-table="deleteTable"
      />
    </section>
  </main>

  <CreateDbModal
    :open="dbModalOpen"
    :db-id="editingDbId"
    @close="dbModalOpen = false"
  />

  <CreateTablePage :open="tablePageOpen" @close="tablePageOpen = false" />

  <IndexModal :open="indexModalOpen" @close="indexModalOpen = false" />
</template>
