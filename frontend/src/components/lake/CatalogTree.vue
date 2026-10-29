<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { useLakeStore } from '@/stores/lake'

interface TreeRow {
  kind: 'cat' | 'db' | 'grp' | 'table'
  id: string
  depth: number
  icon: string
  label: string
  open: boolean
  active?: boolean
  count?: number
  indexCount?: number
}

const lake = useLakeStore()

const rows = computed<TreeRow[]>(() => {
  const q = lake.query.trim().toLowerCase()
  const out: TreeRow[] = []

  lake.catalogs.forEach((cat) => {
    const catDbs = lake.dbsInCatalog(cat.id)
    const catNameHit = !!q && cat.name.toLowerCase().includes(q)
    const dbHit = catDbs.some((db) => !!q && db.name.toLowerCase().includes(q))
    const tableHit = catDbs.some((db) =>
      lake.tablesInDb(db.id).some((t) => !!q && t.name.toLowerCase().includes(q)),
    )
    if (q && !catNameHit && !dbHit && !tableHit) return

    const catOpen = q ? true : lake.expanded.has(`cat:${cat.id}`)
    out.push({ kind: 'cat', id: cat.id, depth: 0, icon: 'catalog', label: cat.name, open: catOpen })
    if (!catOpen) return

    catDbs.forEach((db) => {
      const dbTables = lake.tablesInDb(db.id)
      const dbOpen = q ? true : lake.expanded.has(`db:${db.id}`)
      out.push({ kind: 'db', id: db.id, depth: 1, icon: 'db', label: db.name, open: dbOpen })
      if (!dbOpen) return

      const tblOpen = q ? true : lake.expanded.has(`grp:${db.id}:tables`)
      out.push({
        kind: 'grp',
        id: `${db.id}:tables`,
        depth: 2,
        icon: 'table',
        label: '表',
        count: dbTables.length,
        open: tblOpen,
      })
      if (!tblOpen) return

      dbTables.forEach((t) => {
        out.push({
          kind: 'table',
          id: t.id,
          depth: 3,
          icon: 'table',
          label: t.name,
          open: false,
          active: t.id === lake.selectedId,
          indexCount: t.indexes.length,
        })
      })
    })
  })

  return out
})

function onRowClick(row: TreeRow, event: MouseEvent) {
  const onChevron = !!(event.target as HTMLElement).closest('.tree-chev')

  if (row.kind === 'cat') {
    lake.toggleExpanded(`cat:${row.id}`)
    return
  }
  if (row.kind === 'db') {
    if (onChevron) {
      lake.toggleExpanded(`db:${row.id}`)
    } else {
      lake.selectDb(row.id)
    }
    return
  }
  if (row.kind === 'grp') {
    const dbId = row.id.split(':')[0]
    lake.toggleExpanded(`grp:${dbId}:tables`)
    return
  }
  lake.selectTable(row.id)
}
</script>

<template>
  <div class="catalog-tree">
    <template v-if="rows.length">
      <button
        v-for="row in rows"
        :key="`${row.kind}:${row.id}`"
        class="tree-row"
        :class="{ ns: row.kind !== 'table', active: row.active }"
        type="button"
        :style="{ '--depth': row.depth }"
        @click="onRowClick(row, $event)"
      >
        <span class="tree-chev">
          <SvgIcon v-if="row.kind !== 'table'" :name="row.open ? 'chevDown' : 'chevRight'" />
        </span>
        <span class="tree-ic"><SvgIcon :name="row.icon" /></span>
        <span class="tree-label">{{ row.label }}</span>
        <span v-if="row.count !== undefined" class="tree-sub-count">({{ row.count }})</span>
        <span v-if="row.indexCount" class="idx-badge">{{ row.indexCount }}</span>
      </button>
    </template>
    <div v-else class="tree-empty">没有匹配的 Catalog / 数据库 / 表</div>
  </div>
</template>
