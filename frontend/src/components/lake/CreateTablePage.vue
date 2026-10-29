<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { useDatasetStore, type SchemaField } from '@/stores/datasets'
import { useLakeStore, type LakeTable } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'
import { fmtNum, nowStamp, tableNameFromDataset } from '@/utils/format'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; created: [] }>()

const lake = useLakeStore()
const datasetStore = useDatasetStore()
const ui = useUiStore()

const FIELD_TYPES = [
  { value: 'string', label: '字符串 string' },
  { value: 'int32', label: '32 位整数 int32' },
  { value: 'int64', label: '64 位整数 int64' },
  { value: 'float32', label: '单精度浮点 float32' },
  { value: 'float64', label: '双精度浮点 float64' },
  { value: 'boolean', label: '布尔 boolean' },
  { value: 'binary', label: '二进制 binary' },
  { value: 'decimal(12,2)', label: '定点小数 decimal(12,2)' },
  { value: 'date', label: '日期 date' },
  { value: 'timestamp[us]', label: '时间戳 timestamp[us]' },
  { value: 'list<string>', label: '字符串数组 list<string>' },
  { value: 'vector', label: '向量 vector' },
]

interface SchemaRow {
  name: string
  type: string
  desc: string
  dim: number | string
  showDim: boolean
  nameInvalid: boolean
  dimInvalid: boolean
}

const FORMATS = ['Parquet', 'Paimon', 'Lance']

const tableType = ref<'internal' | 'external'>('external')
const selectedFormat = ref('Lance')
const catalogId = ref('')
const dbId = ref('')
const tableName = ref('')
const tableNote = ref('')
const tableNameInvalid = ref(false)
const tableNameError = ref('请输入数据表名称')
const datasetError = ref(false)
const schemaError = ref('')
const schemaRows = ref<SchemaRow[]>([])
const selectedDatasetId = ref('')
const autoFilledTableName = ref('')
const nameInput = ref<HTMLInputElement | null>(null)

const isExternal = computed(() => tableType.value === 'external')
const selectedDataset = computed(() =>
  selectedDatasetId.value ? datasetStore.datasetById(selectedDatasetId.value) : null,
)
const importing = computed(() => !!selectedDataset.value)

const availableDatasets = computed(() => datasetStore.datasetsInFormat(selectedFormat.value))

const catalogOptions = computed(() => lake.catalogs)
const dbOptions = computed(() => {
  const list = lake.dbsInCatalog(catalogId.value)
  return list.length ? list : lake.dbs
})

const datasetSchema = computed(() => {
  const ds = selectedDataset.value
  if (!ds) return null
  return datasetStore.datasetSchema(ds)
})

const formatHint = computed(
  () =>
    `当前格式：${selectedFormat.value}，数据集下拉仅展示同格式数据集（共 ${availableDatasets.value.length} 个）。`,
)

const tableTypeHint = computed(() =>
  isExternal.value
    ? '外表：数据存放在外部对象存储，向量检索走 KNN 暴力搜索；仅外表可选择数据来源与数据集。'
    : '内表：数据由平台统一存储与维护，不支持从数据集导入。',
)

watch(
  () => props.open,
  (open) => {
    if (!open) return
    const selected = lake.currentTable
    const selectedDb = selected ? lake.dbById(lake.tableDbId(selected)) : lake.dbs[0]
    catalogId.value = selectedDb?.catalogId ?? lake.catalogs[0]?.id ?? ''
    dbId.value = selectedDb?.id ?? ''
    autoFilledTableName.value = ''
    selectedDatasetId.value = ''
    tableType.value = 'external'
    tableName.value = ''
    tableNote.value = ''
    tableNameInvalid.value = false
    tableNameError.value = '请输入数据表名称'
    schemaError.value = ''
    schemaRows.value = [newRow()]
    selectedFormat.value = 'Lance'
    nextTick(() => nameInput.value?.focus())
  },
)

function newRow(): SchemaRow {
  return {
    name: '',
    type: 'string',
    desc: '',
    dim: 768,
    showDim: false,
    nameInvalid: false,
    dimInvalid: false,
  }
}

function onTypeChange(row: SchemaRow) {
  row.showDim = row.type === 'vector'
  if (row.showDim) {
    row.dimInvalid = false
  }
}

function addRow(focus = false) {
  const row = newRow()
  schemaRows.value.push(row)
  if (focus) {
    nextTick(() => {
      const inputs = document.querySelectorAll<HTMLInputElement>('.schema-field-table .field-name')
      inputs[inputs.length - 1]?.focus()
    })
  }
}

function removeRow(index: number) {
  schemaRows.value.splice(index, 1)
}

function onCatalogChange() {
  const list = lake.dbsInCatalog(catalogId.value)
  dbId.value = (list.length ? list : lake.dbs)[0]?.id ?? ''
}

function applyDatasetMode() {
  if (!importing.value && autoFilledTableName.value && tableName.value.trim() === autoFilledTableName.value) {
    tableName.value = ''
    autoFilledTableName.value = ''
  }
}

function selectDataset() {
  datasetError.value = false
  applyDatasetMode()
  if (!selectedDataset.value) return
  const suggested = tableNameFromDataset(selectedDataset.value.name)
  if (!tableName.value.trim() || tableName.value.trim() === autoFilledTableName.value) {
    tableName.value = suggested
    autoFilledTableName.value = suggested
    tableNameInvalid.value = false
  }
}

watch(selectedDatasetId, selectDataset)

function onTypeRadioChange(value: 'internal' | 'external') {
  tableType.value = value
  if (!isExternal.value) {
    if (selectedFormat.value !== 'Lance') {
      selectedFormat.value = 'Lance'
    }
    selectedDatasetId.value = ''
  }
}

function collectSchema(): { cols?: SchemaField[]; error?: string; index?: number } {
  const rows = schemaRows.value
  if (!rows.length) return { error: '请至少添加一个字段' }

  const cols: SchemaField[] = []
  const seen: Record<string, boolean> = {}
  for (let i = 0; i < rows.length; i += 1) {
    const row = rows[i]
    const name = row.name.trim()
    row.nameInvalid = false

    if (!name) {
      row.nameInvalid = true
      return { error: '请填写字段名称', index: i }
    }
    if (!/^[a-z_][a-z0-9_]*$/.test(name)) {
      row.nameInvalid = true
      return { error: '字段名仅支持小写字母、数字和下划线，且不能以数字开头', index: i }
    }
    if (seen[name]) {
      row.nameInvalid = true
      return { error: `字段名称重复："${name}"`, index: i }
    }
    seen[name] = true

    let type = row.type
    if (type === 'vector') {
      const dim = Number(row.dim)
      row.dimInvalid = false
      if (!dim || dim < 1 || dim > 65536 || Math.floor(dim) !== dim) {
        row.dimInvalid = true
        return { error: '请填写 1-65536 之间的向量维度', index: i }
      }
      type = `vector<float32>[${dim}]`
    }

    cols.push({ name, type, nullable: true, desc: row.desc.trim() })
  }
  return { cols }
}

function submit() {
  const importMode = importing.value
  const ds = selectedDataset.value
  const dsSchema = datasetSchema.value
  const name = tableName.value.trim()
  const targetDb = lake.dbById(dbId.value)
  const catalog = lake.dbCatalogId(targetDb)
  let valid = true

  tableNameInvalid.value = false
  datasetError.value = false

  if (importMode && !dsSchema) {
    datasetError.value = true
    valid = false
  }

  if (!name) {
    tableNameError.value = '请输入数据表名称'
    tableNameInvalid.value = true
    valid = false
  } else if (!/^[a-z_][a-z0-9_]*$/.test(name)) {
    tableNameError.value = '仅支持小写字母、数字和下划线，且不能以数字开头'
    tableNameInvalid.value = true
    valid = false
  } else if (lake.tables.some((t) => lake.tableDbId(t) === dbId.value && t.name === name)) {
    tableNameError.value = '该数据库下已存在同名表'
    tableNameInvalid.value = true
    valid = false
  }

  if (!valid) return

  let schema: SchemaField[]
  if (importMode && dsSchema) {
    schema = dsSchema.map((c) => ({
      name: c.name,
      type: c.type,
      nullable: c.nullable === undefined ? true : !!c.nullable,
      desc: c.desc || '',
    }))
    schemaError.value = ''
  } else {
    const result = collectSchema()
    if (result.error) {
      schemaError.value = result.error
      if (result.index !== undefined) {
        nextTick(() => {
          const inputs = document.querySelectorAll<HTMLInputElement>('.schema-field-table .field-name')
          inputs[result.index!]?.focus()
        })
      }
      return
    }
    schemaError.value = ''
    schema = result.cols!
  }

  const ts = nowStamp()
  const ossBase = targetDb.ossPath.replace(/\/+$/, '')
  const suffix =
    selectedFormat.value === 'Parquet' ? '.parquet' : selectedFormat.value === 'Paimon' ? '' : '.lance'
  const location = `${ossBase}/${name}${suffix}`
  const rowCount = importMode && ds ? Number(ds.samples) || 0 : 0
  const tableSize = importMode && ds ? ds.size : '0 B'
  const description = tableNote.value.trim() || (importMode && ds ? ds.desc : '')

  const table: LakeTable = {
    id: `t_${Date.now()}`,
    dbId: dbId.value,
    catalogId: catalog,
    name,
    external: isExternal.value,
    tableType: isExternal.value ? '外表' : '内表',
    format: selectedFormat.value,
    owner: '2530手机用户#QGDdVm',
    location,
    createdAt: ts,
    updatedAt: ts,
    formatVersion: '2.0',
    rows: rowCount,
    size: tableSize,
    columns: schema.length,
    description,
    lastOptimized: '-',
    schema,
    indexes: [],
    sourceDatasetId: importMode && ds ? ds.id : null,
  }

  lake.tables.push(table)
  lake.selectedId = table.id
  lake.selectedDbId = dbId.value
  lake.tab = 'basic'
  lake.expanded = new Set([...lake.expanded, `cat:${catalog}`, `db:${dbId.value}`, `grp:${dbId.value}:tables`])

  emit('close')
  emit('created')
  ui.toast(
    importMode && ds
      ? `已从数据集 "${ds.name}" 导入并创建表（演示）`
      : `表 "${name}" 创建成功（演示）`,
  )
}
</script>

<template>
  <div v-if="open" id="createTablePage" class="create-page">
    <header class="create-head">
      <button class="back-btn" type="button" aria-label="返回" @click="emit('close')">
        <SvgIcon name="back" />
      </button>
      <h1>创建数据表</h1>
    </header>

    <div class="create-body">
      <div class="create-form">
        <div class="section-title">基本信息</div>

        <div class="create-field">
          <label>表类型<span class="req">*</span></label>
          <div class="field-control">
            <div class="radio-row">
              <label class="radio-item">
                <input
                  type="radio"
                  name="tableType"
                  value="internal"
                  :checked="tableType === 'internal'"
                  @change="onTypeRadioChange('internal')"
                >
                <span class="radio-mark" />
                <span>内表</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  name="tableType"
                  value="external"
                  :checked="tableType === 'external'"
                  @change="onTypeRadioChange('external')"
                >
                <span class="radio-mark" />
                <span>外表</span>
              </label>
            </div>
            <p class="form-hint">{{ tableTypeHint }}</p>
          </div>
        </div>

        <div class="create-field">
          <label>数据格式<span class="req">*</span></label>
          <div class="field-control">
            <div class="seg-row">
              <button
                v-for="format in FORMATS"
                :key="format"
                class="seg-btn"
                :class="{ active: selectedFormat === format }"
                :disabled="!isExternal"
                type="button"
                @click="selectedFormat = format"
              >
                {{ format }}
              </button>
            </div>
            <p class="form-hint">{{ formatHint }}</p>
          </div>
        </div>

        <div class="create-field">
          <label for="importDataset">数据集</label>
          <div class="field-control">
            <select id="importDataset" v-model="selectedDatasetId" :disabled="!isExternal">
              <option value="">空表（不导入数据集）</option>
              <option
                v-for="ds in availableDatasets"
                :key="ds.id"
                :value="String(ds.id)"
                :disabled="!datasetStore.datasetSchema(ds)"
              >
                {{ ds.name }} · {{ fmtNum(ds.samples) }} 样本 · {{ ds.size
                }}{{ datasetStore.datasetSchema(ds) ? '' : '（Schema 未解析）' }}
              </option>
            </select>
            <p class="field-error" :class="{ show: datasetError }">请选择要导入的数据集</p>
          </div>
        </div>

        <div class="create-field">
          <label>数据集信息</label>
          <div class="field-control">
            <div class="dataset-summary">
              <template v-if="selectedDataset">
                <div class="ds-meta">
                  <span>格式 <b>{{ selectedDataset.format }}</b></span>
                  <span>样本数 <b>{{ fmtNum(selectedDataset.samples) }}</b></span>
                  <span>大小 <b>{{ selectedDataset.size }}</b></span>
                </div>
                <div class="ds-path">{{ selectedDataset.ossPath || '-' }}</div>
              </template>
              <span v-else-if="!availableDatasets.length" class="ds-empty">
                当前格式（{{ selectedFormat }}）下还没有数据集，请先在「数据集」中创建。
              </span>
              <span v-else class="ds-empty">
                选择数据集后，将自动继承字段结构并导入全部数据。
              </span>
            </div>
          </div>
        </div>

        <div class="create-field">
          <label for="ctCatalog">所属数据目录</label>
          <div class="field-control">
            <select id="ctCatalog" v-model="catalogId" @change="onCatalogChange">
              <option v-for="cat in catalogOptions" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
        </div>

        <div class="create-field">
          <label for="ctDb">所属数据库</label>
          <div class="field-control">
            <select id="ctDb" v-model="dbId">
              <option v-for="db in dbOptions" :key="db.id" :value="db.id">{{ db.name }}</option>
            </select>
          </div>
        </div>

        <div class="create-field">
          <label for="tableName">数据表名称<span class="req">*</span></label>
          <div class="field-control">
            <input
              id="tableName"
              ref="nameInput"
              v-model="tableName"
              type="text"
              placeholder="由小写字母、数字、下划线组成，且不能以数字开头"
              autocomplete="off"
              :class="{ invalid: tableNameInvalid }"
              @input="tableNameInvalid = false"
              @keydown.enter="submit"
            >
            <p class="field-error" :class="{ show: tableNameInvalid }">{{ tableNameError }}</p>
          </div>
        </div>

        <div class="create-field">
          <label for="tableNote">数据表备注</label>
          <div class="field-control">
            <textarea id="tableNote" v-model="tableNote" placeholder="请输入" />
          </div>
        </div>

        <div class="section-title create-section-gap">
          字段信息
          <span v-if="importing" class="schema-mode-tag">只读</span>
        </div>

        <!-- 空表：手工编辑字段 -->
        <div v-if="!importing">
          <div class="schema-table-wrap">
            <table class="schema-field-table">
              <colgroup>
                <col style="width: 27%">
                <col style="width: 34%">
                <col style="width: 29%">
                <col style="width: 10%">
              </colgroup>
              <thead>
                <tr>
                  <th>字段名称<span class="req">*</span></th>
                  <th>字段类型<span class="req">*</span></th>
                  <th>字段描述</th>
                  <th style="text-align: right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in schemaRows" :key="index">
                  <td>
                    <input
                      v-model="row.name"
                      type="text"
                      class="field-name"
                      placeholder="请输入"
                      autocomplete="off"
                      :class="{ invalid: row.nameInvalid }"
                      @input="row.nameInvalid = false"
                    >
                  </td>
                  <td>
                    <div class="type-cell">
                      <select v-model="row.type" class="field-type" @change="onTypeChange(row)">
                        <option v-for="item in FIELD_TYPES" :key="item.value" :value="item.value">
                          {{ item.label }}
                        </option>
                      </select>
                      <span v-show="row.showDim" class="dim-wrap">
                        <input
                          v-model="row.dim"
                          type="number"
                          class="field-dim"
                          min="1"
                          max="65536"
                          step="1"
                          placeholder="维度"
                          aria-label="向量维度"
                          :class="{ invalid: row.dimInvalid }"
                          @input="row.dimInvalid = false"
                        >
                        <span class="dim-suffix">维</span>
                      </span>
                    </div>
                  </td>
                  <td>
                    <input v-model="row.desc" type="text" class="field-desc" placeholder="请输入" autocomplete="off">
                  </td>
                  <td class="row-actions">
                    <button class="row-del" type="button" aria-label="删除字段" @click="removeRow(index)">
                      ×
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="field-error" :class="{ show: !!schemaError }">{{ schemaError }}</p>

          <button class="add-field" type="button" @click="addRow(true)">
            <SvgIcon name="plus" />
            添加字段
          </button>
        </div>

        <!-- 导入数据集：只读预览 -->
        <div v-else>
          <div class="schema-table-wrap">
            <table class="schema-field-table">
              <colgroup>
                <col style="width: 27%">
                <col style="width: 33%">
                <col style="width: 40%">
              </colgroup>
              <thead>
                <tr>
                  <th>字段名称</th>
                  <th>字段类型</th>
                  <th>字段描述</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!datasetSchema">
                  <td colspan="3" class="muted">该数据集暂未解析到 Schema</td>
                </tr>
                <tr v-for="col in datasetSchema || []" :key="col.name">
                  <td><span class="mono">{{ col.name }}</span></td>
                  <td><span class="mono muted">{{ col.type }}</span></td>
                  <td class="muted">{{ col.desc || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="schema-hint">
            结构继承自数据集 {{ selectedDataset?.name }}，不可编辑；如需自定义结构，请切换为「空表」。
          </p>
        </div>
      </div>
    </div>

    <footer class="create-foot">
      <button id="createTableSubmit" class="btn-primary" type="button" @click="submit">
        {{ importing ? '导入并创建' : '创建' }}
      </button>
      <button id="createTableCancel" class="btn-ghost" type="button" @click="emit('close')">取消</button>
    </footer>
  </div>
</template>
