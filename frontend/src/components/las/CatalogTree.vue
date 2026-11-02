<script setup lang="ts">
import { ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { useLasCatalogStore } from '@/stores/lasCatalog'

const props = defineProps<{
  /** 当前选中的 Catalog */
  catalog: string
  /** 当前选中的数据库 */
  database: string
  /** 当前选中的数据表 */
  table?: string
}>()

const emit = defineEmits<{
  (e: 'select-catalog', name: string): void
  (e: 'select-database', catalog: string, database: string): void
  (e: 'select-table', catalog: string, database: string, table: string): void
}>()

const store = useLasCatalogStore()

/** 展开状态，key 为节点路径，如 catalog1 / catalog1/default / catalog1/default/tables */
const expanded = ref<Record<string, boolean>>({})

function path(...parts: string[]) {
  return parts.join('/')
}

function isOpen(...parts: string[]) {
  return Boolean(expanded.value[path(...parts)])
}

function toggle(...parts: string[]) {
  const key = path(...parts)
  expanded.value[key] = !expanded.value[key]
}

function onCatalog(name: string) {
  expanded.value[path(name)] = true
  emit('select-catalog', name)
}

function onDatabase(catalogName: string, name: string) {
  expanded.value[path(catalogName, name)] = true
  emit('select-database', catalogName, name)
}

function onTable(catalogName: string, databaseName: string, name: string) {
  emit('select-table', catalogName, databaseName, name)
}

/** 选中项变化时自动展开其祖先链 */
watch(
  () => [props.catalog, props.database, props.table],
  () => {
    if (props.catalog) expanded.value[path(props.catalog)] = true
    if (props.catalog && props.database) {
      expanded.value[path(props.catalog, props.database)] = true
    }
    if (props.catalog && props.database && props.table) {
      expanded.value[path(props.catalog, props.database, 'tables')] = true
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="ct-tree">
    <div v-for="cat in store.treeCatalogs" :key="cat.name" class="ct-node">
      <div
        class="ct-row ct-l0"
        :class="{ on: cat.name === props.catalog }"
        @click="onCatalog(cat.name)"
      >
        <button class="ct-arrow" type="button" @click.stop="toggle(cat.name)">
          <SvgIcon :icon="LAS_ICONS.chevronRight" :class="{ open: isOpen(cat.name) }" />
        </button>
        <SvgIcon class="ct-ic" :icon="LAS_ICONS.fmtLance" />
        <span class="ct-text">{{ cat.name }}</span>
      </div>

      <template v-if="isOpen(cat.name)">
        <div v-for="db in cat.databases" :key="db.name" class="ct-node">
          <div
            class="ct-row ct-l1"
            :class="{ on: db.name === props.database && cat.name === props.catalog }"
            @click="onDatabase(cat.name, db.name)"
          >
            <button class="ct-arrow" type="button" @click.stop="toggle(cat.name, db.name)">
              <SvgIcon :icon="LAS_ICONS.chevronRight" :class="{ open: isOpen(cat.name, db.name) }" />
            </button>
            <SvgIcon class="ct-ic" :icon="LAS_ICONS.db" />
            <span class="ct-text">{{ db.name }}</span>
          </div>

          <template v-if="isOpen(cat.name, db.name)">
            <div
              class="ct-row ct-l2"
              @click="toggle(cat.name, db.name, 'tables')"
            >
              <button
                class="ct-arrow"
                type="button"
                @click.stop="toggle(cat.name, db.name, 'tables')"
              >
                <SvgIcon
                  :icon="LAS_ICONS.chevronRight"
                  :class="{ open: isOpen(cat.name, db.name, 'tables') }"
                />
              </button>
              <SvgIcon class="ct-ic" :icon="LAS_ICONS.fmtLance" />
              <span class="ct-text">表 ({{ db.tables.length }})</span>
            </div>

            <template v-if="isOpen(cat.name, db.name, 'tables')">
              <div
                v-for="item in db.tables"
                :key="item.name"
                class="ct-row ct-l3"
                :class="{ on: item.name === props.table }"
                @click="onTable(cat.name, db.name, item.name)"
              >
                <span class="ct-arrow" />
                <SvgIcon class="ct-ic" :icon="LAS_ICONS.fmtLance" />
                <span class="ct-text">{{ item.name }}</span>
              </div>
            </template>

            <div
              class="ct-row ct-l2"
              @click="toggle(cat.name, db.name, 'functions')"
            >
              <button
                class="ct-arrow"
                type="button"
                @click.stop="toggle(cat.name, db.name, 'functions')"
              >
                <SvgIcon
                  :icon="LAS_ICONS.chevronRight"
                  :class="{ open: isOpen(cat.name, db.name, 'functions') }"
                />
              </button>
              <SvgIcon class="ct-ic" :icon="LAS_ICONS.func" />
              <span class="ct-text">函数 ({{ db.functions.length }})</span>
            </div>

            <template v-if="isOpen(cat.name, db.name, 'functions')">
              <div
                v-for="fn in db.functions"
                :key="fn"
                class="ct-row ct-l3"
                @click="onTable(cat.name, db.name, fn)"
              >
                <span class="ct-arrow" />
                <SvgIcon class="ct-ic" :icon="LAS_ICONS.func" />
                <span class="ct-text">{{ fn }}</span>
              </div>
            </template>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.ct-tree {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

.ct-node {
  width: 100%;
}

.ct-row {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding-right: 8px;
  border-radius: 4px;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
  white-space: nowrap;
}

.ct-row:hover {
  background: #f7f8fa;
}

.ct-row.on {
  background: #e8f3ff;
  color: #165dff;
}

.ct-l0 {
  padding-left: 8px;
}

.ct-l1 {
  padding-left: 24px;
}

.ct-l2 {
  padding-left: 40px;
}

.ct-l3 {
  padding-left: 58px;
}

.ct-arrow {
  width: 16px;
  height: 16px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: none;
  padding: 0;
  color: #86909c;
  cursor: pointer;
}

.ct-arrow svg {
  width: 12px;
  height: 12px;
  transition: transform 0.15s ease;
}

.ct-arrow svg.open {
  transform: rotate(90deg);
}

.ct-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.ct-l0 .ct-ic {
  color: #165dff;
}

.ct-text {
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
