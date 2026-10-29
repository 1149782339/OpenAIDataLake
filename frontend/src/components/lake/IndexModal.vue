<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { useLakeStore, type IndexKind } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; created: [] }>()

const lake = useLakeStore()
const ui = useUiStore()

const name = ref('')
const kind = ref<IndexKind>('vector')
const column = ref('')
const metric = ref('cosine')
const params = ref('')
const nameInvalid = ref(false)
const nameInput = ref<HTMLInputElement | null>(null)

const KIND_DEFAULTS: Record<IndexKind, { type: string; params: string }> = {
  vector: { type: 'IVF_HNSW_SQ', params: 'M=16, ef_construction=200' },
  scalar: { type: 'BTREE', params: '-' },
  fts: { type: 'INVERTED', params: 'tokenizer=simple, stem=false' },
}

const KINDS: { value: IndexKind; label: string }[] = [
  { value: 'vector', label: '向量索引' },
  { value: 'scalar', label: '标量索引' },
  { value: 'fts', label: '全文索引' },
]

function columnsForKind(target: { schema: { name: string; type: string }[] }, targetKind: IndexKind) {
  return target.schema.filter((c) => {
    const type = c.type.toLowerCase()
    if (targetKind === 'vector') return type.includes('vector')
    if (targetKind === 'fts') return type.includes('string') || type.includes('text')
    return !type.includes('vector')
  })
}

const currentTable = computed(() => lake.currentTable)

const availableColumns = computed(() => {
  const t = currentTable.value
  if (!t) return []
  return columnsForKind(t, kind.value)
})

const kindOptions = computed(() =>
  KINDS.map((item) => {
    const t = currentTable.value
    const disabled = t ? columnsForKind(t, item.value).length === 0 : true
    return { ...item, disabled }
  }),
)

watch(
  () => props.open,
  (open) => {
    if (!open) return
    name.value = ''
    params.value = ''
    nameInvalid.value = false
    const t = currentTable.value
    if (t) {
      // 与 demo 一致：默认选中「向量索引」，若该表没有向量列则退回「标量索引」
      const hasVector = columnsForKind(t, 'vector').length > 0
      const hasScalar = columnsForKind(t, 'scalar').length > 0
      kind.value = hasVector ? 'vector' : hasScalar ? 'scalar' : 'fts'
    }
    syncColumns()
    nextTick(() => nameInput.value?.focus())
  },
)

watch(kind, () => syncColumns())

function syncColumns() {
  const cols = availableColumns.value
  column.value = cols.length ? cols[0].name : ''
  params.value = ''
}

function submit() {
  const t = currentTable.value
  if (!t) return
  const indexName = name.value.trim()
  if (!indexName) {
    nameInvalid.value = true
    nameInput.value?.focus()
    ui.toast('请输入索引名称')
    return
  }
  if (t.indexes.some((i) => i.name === indexName)) {
    nameInvalid.value = true
    nameInput.value?.focus()
    ui.toast('索引名称已存在')
    return
  }
  if (!column.value) {
    ui.toast('没有可用字段，无法创建该类型索引')
    return
  }

  const defaults = KIND_DEFAULTS[kind.value]
  let indexParams = params.value.trim() || defaults.params
  if (kind.value === 'vector') {
    indexParams = `${metric.value}, ${indexParams}`
  }

  const index = {
    id: `i${Date.now()}`,
    name: indexName,
    kind: kind.value,
    type: defaults.type,
    column: column.value,
    params: indexParams,
    status: 'building' as const,
    coverage: 0,
    unindexed: t.rows,
    size: '—',
    createdAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
  }
  t.indexes.push(index)
  lake.tab = 'indexes'
  emit('close')
  emit('created')
  ui.toast('索引构建任务已提交，正在后台构建…')

  const tableId = t.id
  window.setTimeout(() => {
    const target = lake.tableById(tableId)
    const item = target?.indexes.find((i) => i.id === index.id)
    if (!target || !item) return
    item.status = 'ready'
    item.coverage = target.rows
    item.unindexed = 0
    const factor = kind.value === 'vector' ? 0.00008 : kind.value === 'fts' ? 0.00021 : 0.00004
    item.size = `${Math.max(0.1, target.rows * factor).toFixed(1)} MB`
    ui.toast(`索引 "${indexName}" 构建完成`)
  }, 2600)
}
</script>

<template>
  <BaseModal id="indexModal" :open="open" title="新建索引" aria-label="新建索引" @close="emit('close')">
    <div class="form-row">
      <label for="idxName">索引名称<span class="req">*</span></label>
      <input
        id="idxName"
        ref="nameInput"
        v-model="name"
        type="text"
        placeholder="例如 idx_embedding"
        autocomplete="off"
        :class="{ invalid: nameInvalid }"
        @input="nameInvalid = false"
        @keydown.enter="submit"
      >
    </div>

    <div class="form-row">
      <label for="idxKind">索引类型</label>
      <select id="idxKind" v-model="kind">
        <option v-for="item in kindOptions" :key="item.value" :value="item.value" :disabled="item.disabled">
          {{ item.label }}
        </option>
      </select>
    </div>

    <div class="form-row">
      <label for="idxColumn">字段</label>
      <select id="idxColumn" v-model="column">
        <option v-if="!availableColumns.length" value="">没有可用字段</option>
        <option v-for="col in availableColumns" :key="col.name" :value="col.name">
          {{ col.name }} · {{ col.type }}
        </option>
      </select>
    </div>

    <div v-show="kind === 'vector'" class="form-row">
      <label for="idxMetric">距离度量</label>
      <select id="idxMetric" v-model="metric">
        <option value="cosine">cosine</option>
        <option value="l2">l2</option>
        <option value="dot">dot</option>
      </select>
    </div>

    <div class="form-row">
      <label for="idxParams">构建参数</label>
      <input
        id="idxParams"
        v-model="params"
        type="text"
        :placeholder="KIND_DEFAULTS[kind].params"
        autocomplete="off"
      >
    </div>

    <p class="section-sub" style="margin: -4px 0 0">索引在后台异步构建，构建期间不阻塞读写。</p>

    <template #footer>
      <div class="modal-foot">
        <button class="btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="btn-primary" type="button" @click="submit">创建索引</button>
      </div>
    </template>
  </BaseModal>
</template>
