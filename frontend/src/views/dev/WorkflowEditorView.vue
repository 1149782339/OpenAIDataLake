<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import DatasetPickerModal from '@/components/dev/DatasetPickerModal.vue'
import OperatorParamsPanel from '@/components/dev/OperatorParamsPanel.vue'
import WorkflowCanvas from '@/components/dev/WorkflowCanvas.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { DEV_PIPELINE_DATASETS, findPipelineDataset } from '@/mock/dev/datasets'
import { getOperator } from '@/mock/dev/operators'
import { DEV_INPUT_NODE_ID, type DevEdge, type DevNode } from '@/mock/dev/workflow'
import { useDevWorkflowStore } from '@/stores/devWorkflow'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const store = useDevWorkflowStore()
const ui = useUiStore()

const scheduleOptions = ['每 30 分钟', '每小时', '每天 02:00', '手动']

/** 编辑中的工作流 id；为空表示新建 */
const workflowId = ref<string>('')
const versionId = ref<string>('')
const name = ref('')
const description = ref('')
const schedule = ref('手动')
/** 管道输入绑定的数据集 id */
const inputDatasetId = ref('')
/** 管道数据类型（继承自输入数据集，未选数据集时可手动指定） */
const dataType = ref('')
const nodes = ref<DevNode[]>([])
const edges = ref<DevEdge[]>([])
const params = ref<Record<string, Record<string, unknown>>>({})
const selectedNodeId = ref<string | null>(null)
const dirty = ref(false)
const pickerVisible = ref(false)

const isNew = computed(() => !workflowId.value)
const workflow = computed(() => (workflowId.value ? store.byId(workflowId.value) : undefined))
const version = computed(() =>
  workflowId.value && versionId.value ? store.versionOf(workflowId.value, versionId.value) : undefined,
)
const versionNo = computed(() => version.value?.version ?? 'v1')
/** 版本发布状态：草稿 / 已发布 */
const versionStatus = computed(() => version.value?.status ?? '草稿')
/** 版本是否已冻结：已发布或已绑定运行实例 */
const frozen = computed(() =>
  workflowId.value && versionId.value ? store.isFrozen(workflowId.value, versionId.value) : false,
)

const inputDataset = computed(() => findPipelineDataset(inputDatasetId.value))
const dataTypeLocked = computed(() => Boolean(inputDatasetId.value))

/** 第一个算子节点：管道输入连线的目标 */
const firstOperatorNode = computed(() => nodes.value[0] ?? null)

/** 首算子接受的数据类型（数据集类型白名单） */
const acceptedTypes = computed(() => {
  const node = firstOperatorNode.value
  const op = node ? getOperator(node.opId) : undefined
  return op?.accepts ?? []
})

const dataTypeOptions = computed(() =>
  acceptedTypes.value.map((t) => ({ value: t, label: t })),
)

function isEmpty(value: unknown): boolean {
  if (value === undefined || value === null) return true
  if (typeof value === 'string') return value.trim() === ''
  if (typeof value === 'number') return Number.isNaN(value)
  return false
}

/** 联动字段取值：data_type 取管道级数据类型，其余取算子自身参数 */
function driverValue(driver: string, values: Record<string, unknown>): string {
  if (driver === 'data_type') return dataType.value
  const v = values[driver]
  if (typeof v === 'boolean') return v ? 'true' : 'false'
  return v === undefined || v === null ? '' : String(v)
}

/** 所有算子上未配置的必填参数 */
const missingParams = computed(() => {
  const list: { nodeId: string; key: string; label: string }[] = []
  nodes.value.forEach((node) => {
    const op = getOperator(node.opId)
    if (!op) return
    const values = params.value[node.id] ?? {}
    const collect = (defs: typeof op.params) => {
      defs.forEach((def) => {
        if (!def.required) return
        if (isEmpty(values[def.key] ?? def.default)) {
          list.push({ nodeId: node.id, key: def.key, label: def.label })
        }
      })
    }
    collect(op.params)
    ;(op.groups ?? [])
      .filter((group) => driverValue(group.driver, values) === group.value)
      .forEach((group) => collect(group.params))
  })
  return list
})

/** 发布条件：已选定管道输入数据集，且必填参数配置完整 */
const canPublish = computed(() => Boolean(inputDatasetId.value) && missingParams.value.length === 0)

onMounted(() => {
  const id = String(route.query.id ?? '')
  const target = id ? store.byId(id) : undefined
  if (!target) {
    // 新建工作流：v1 草稿，尚未落库
    name.value = ''
    description.value = ''
    schedule.value = '手动'
    return
  }
  const current = store.currentVersion(target)
  if (!current) return
  workflowId.value = target.id
  versionId.value = current.id
  name.value = target.name
  description.value = target.description
  schedule.value = current.schedule
  inputDatasetId.value = current.inputDatasetId
  dataType.value = current.dataType
  nodes.value = current.nodes.map((n) => ({ ...n }))
  edges.value = current.edges.map((e) => ({ ...e }))
  params.value = JSON.parse(JSON.stringify(current.params)) as Record<string, Record<string, unknown>>
  selectedNodeId.value = nodes.value[0]?.id ?? null
})

const selectedOperatorId = computed(() => {
  const node = nodes.value.find((n) => n.id === selectedNodeId.value)
  return node?.opId ?? null
})

const selectedParams = computed(() => {
  const id = selectedNodeId.value
  return id ? params.value[id] ?? {} : {}
})

/** 管道输入 → 第一个算子的固定连线 */
function syncInputEdge() {
  const existing = edges.value.find((e) => e.from === DEV_INPUT_NODE_ID)
  const target = firstOperatorNode.value
  if (!inputDatasetId.value || !target) {
    if (existing) edges.value = edges.value.filter((e) => e.id !== existing.id)
    return
  }
  if (existing) {
    existing.to = target.id
  } else {
    edges.value.unshift({ id: 'e_input', from: DEV_INPUT_NODE_ID, to: target.id, fixed: true })
  }
}

function onAddNode(opId: string, x: number, y: number) {
  if (frozen.value) return
  const id = `n_${Date.now()}`
  nodes.value.push({ id, opId, x, y })
  params.value[id] = {}
  selectedNodeId.value = id
  syncInputEdge()
  dirty.value = true
}

function onConnect(from: string, to: string) {
  if (frozen.value) return
  if (edges.value.some((e) => e.from === from && e.to === to)) return
  edges.value.push({ id: `e_${Date.now()}`, from, to })
  dirty.value = true
}

function onMoveNode(id: string, x: number, y: number) {
  if (frozen.value) return
  const node = nodes.value.find((n) => n.id === id)
  if (!node) return
  node.x = x
  node.y = y
  dirty.value = true
}

function onRemoveEdge(id: string) {
  if (frozen.value) return
  const edge = edges.value.find((e) => e.id === id)
  if (edge?.fixed) return
  edges.value = edges.value.filter((e) => e.id !== id)
  dirty.value = true
}

function onRemoveNode(id: string) {
  if (frozen.value) return
  nodes.value = nodes.value.filter((n) => n.id !== id)
  edges.value = edges.value.filter((e) => e.from !== id && e.to !== id)
  delete params.value[id]
  if (selectedNodeId.value === id) selectedNodeId.value = nodes.value[0]?.id ?? null
  syncInputEdge()
  dirty.value = true
}

function onUpdateParam(key: string, value: unknown) {
  if (frozen.value) return
  const id = selectedNodeId.value
  if (!id) return
  if (!params.value[id]) params.value[id] = {}
  params.value[id][key] = value
  dirty.value = true
}

function onUpdateDataType(value: string) {
  if (frozen.value || dataTypeLocked.value) return
  dataType.value = value
  dirty.value = true
}

/** 打开数据集选择器 */
function onSelectInput() {
  if (frozen.value) return
  pickerVisible.value = true
}

/** 选定管道输入数据集：类型继承自数据集，并自动连到第一个算子 */
function onPickDataset(dataset: (typeof DEV_PIPELINE_DATASETS)[number]) {
  inputDatasetId.value = dataset.id
  dataType.value = dataset.type
  pickerVisible.value = false
  syncInputEdge()
  dirty.value = true
  ui.toast(`管道输入已绑定数据集「${dataset.name}」，数据类型：${dataset.type}`)
}

function ensureWorkflow(): string {
  if (workflowId.value) return workflowId.value
  const created = store.createWorkflow({
    name: name.value.trim() || '未命名工作流',
    description: description.value.trim(),
  })
  workflowId.value = created.id
  versionId.value = created.currentVersionId
  return created.id
}

function onSave() {
  if (frozen.value) {
    ui.toast('该版本已发布或已有运行记录，不可修改，请创建新版本')
    return
  }
  const id = ensureWorkflow()
  const vid = versionId.value
  const ok = store.saveDraft(id, vid, {
    name: name.value.trim() || '未命名工作流',
    description: description.value.trim(),
    schedule: schedule.value,
    inputDatasetId: inputDatasetId.value,
    dataType: dataType.value,
    nodes: nodes.value,
    edges: edges.value,
    params: params.value,
  })
  dirty.value = false
  if (!ok) {
    ui.toast('该版本已发布或已有运行记录，不可修改，请创建新版本')
    return
  }
  if (missingParams.value.length) {
    ui.toast('草稿已保存，但必填参数未配置完整')
  } else {
    ui.toast('草稿已保存')
  }
}

function onPublish() {
  if (frozen.value) {
    ui.toast('该版本已发布或已有运行记录，不可重复发布')
    return
  }
  if (!inputDatasetId.value) {
    ui.toast('请先选择管道输入数据集')
    return
  }
  if (missingParams.value.length) {
    ui.toast(`必填参数未配置：${missingParams.value.map((m) => m.label).join('、')}`)
    return
  }
  onSave()
  const id = workflowId.value
  const vid = versionId.value
  if (store.publish(id, vid)) {
    const v = store.versionOf(id, vid)
    ui.toast(`${v?.version ?? ''} 已发布，画布与参数已冻结，后续修改请创建新版本`)
  } else {
    ui.toast('发布失败：该版本已冻结')
  }
}

function onCreateNewVersion() {
  const id = workflowId.value
  if (!id) return
  const created = store.createNewVersion(id)
  if (!created) return
  versionId.value = created.id
  name.value = workflow.value?.name ?? name.value
  description.value = workflow.value?.description ?? description.value
  schedule.value = created.schedule
  inputDatasetId.value = created.inputDatasetId
  dataType.value = created.dataType
  nodes.value = created.nodes.map((n) => ({ ...n }))
  edges.value = created.edges.map((e) => ({ ...e }))
  params.value = JSON.parse(JSON.stringify(created.params)) as Record<string, Record<string, unknown>>
  selectedNodeId.value = nodes.value[0]?.id ?? null
  dirty.value = false
  ui.toast(`已复制为 ${created.version} 草稿，修改将在新版本上进行`)
}

function onBack() {
  router.push('/dev/workflow')
}
</script>

<template>
  <div class="we-page">
    <header class="we-head">
      <button class="we-back" type="button" title="返回工作流列表" @click="onBack">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>

      <div class="we-title-area">
        <input
          v-model="name"
          type="text"
          class="we-name"
          :disabled="frozen"
          placeholder="请输入工作流名称"
        >
        <span class="we-version">{{ versionNo }}</span>
        <span v-if="isNew" class="we-status is-new">新建</span>
        <span class="we-status" :class="versionStatus === '已发布' ? 'is-published' : 'is-draft'">
          {{ versionStatus }}
        </span>
        <span class="we-status" :class="frozen ? 'is-lock' : 'is-edit'">
          {{ frozen ? '只读' : '可编辑' }}
        </span>
        <span v-if="dirty" class="we-dirty">未保存</span>
        <span v-if="missingParams.length" class="we-incomplete">未配置完整</span>
      </div>

      <div class="we-head-right">
        <span class="we-field">
          <label>调度配置</label>
          <select v-model="schedule" class="we-select" :disabled="frozen">
            <option v-for="item in scheduleOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </span>
        <button class="we-btn" type="button" :disabled="frozen" @click="onSave">保存草稿</button>
        <button
          class="we-btn is-primary"
          type="button"
          :disabled="frozen || !canPublish"
          :title="canPublish ? '发布当前草稿' : '请先选择管道输入数据集并补全必填参数'"
          @click="onPublish"
        >
          发布
        </button>
      </div>
    </header>

    <div v-if="frozen" class="we-notice">
      <SvgIcon :icon="LAS_ICONS.info" />
      <span>
        该版本已发布或已绑定运行实例，画布与参数只读。任何修改请通过「创建新版本」复制出新草稿，旧版本与其运行记录保持不变。
      </span>
      <button class="we-notice-btn" type="button" @click="onCreateNewVersion">创建新版本</button>
    </div>

    <div class="we-body">
      <WorkflowCanvas
        v-model:selected-id="selectedNodeId"
        :nodes="nodes"
        :edges="edges"
        :readonly="frozen"
        :input-dataset="inputDataset ? { name: inputDataset.name, type: inputDataset.type } : null"
        :input-missing="!inputDatasetId"
        show-palette
        @add-node="onAddNode"
        @connect="onConnect"
        @move-node="onMoveNode"
        @remove-edge="onRemoveEdge"
        @remove-node="onRemoveNode"
        @select-input="onSelectInput"
      />
      <OperatorParamsPanel
        :op-id="selectedOperatorId"
        :node-id="selectedNodeId"
        :params="selectedParams"
        :readonly="frozen"
        :data-type="dataType"
        :data-type-locked="dataTypeLocked"
        :data-type-options="dataTypeOptions"
        @update-param="onUpdateParam"
        @update-data-type="onUpdateDataType"
        @remove-node="onRemoveNode"
      />
    </div>

    <DatasetPickerModal
      :visible="pickerVisible"
      :datasets="DEV_PIPELINE_DATASETS"
      :accepted-types="acceptedTypes"
      :current-id="inputDatasetId"
      @close="pickerVisible = false"
      @select="onPickDataset"
    />
  </div>
</template>

<style scoped>
.we-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1180px;
  background: #fff;
}

.we-head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 14px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.we-back {
  width: 26px;
  height: 26px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.we-back:hover {
  color: #165dff;
  background: #f2f3f5;
}

.we-back svg {
  width: 16px;
  height: 16px;
}

.we-title-area {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.we-name {
  width: 240px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  font-size: 15px;
  font-weight: 500;
  color: #1d2129;
  outline: none;
}

.we-name:hover:not(:disabled) {
  border-color: #e5e6eb;
}

.we-name:focus {
  border-color: #165dff;
  background: #fff;
}

.we-name:disabled {
  color: #1d2129;
  cursor: not-allowed;
}

.we-version {
  padding: 1px 8px;
  border-radius: 3px;
  background: #f2f3f5;
  font-size: 12px;
  color: #4e5969;
}

.we-status {
  flex: none;
  padding: 1px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.we-status.is-draft {
  background: #fff7e8;
  color: #d9762c;
}

.we-status.is-published {
  background: #e8f3ff;
  color: #165dff;
}

.we-status.is-new {
  background: #f2f3f5;
  color: #4e5969;
}

.we-status.is-lock {
  background: #f2f3f5;
  color: #86909c;
}

.we-status.is-edit {
  background: #f0fbf3;
  color: #00893a;
}

.we-dirty {
  flex: none;
  font-size: 12px;
  color: #ff7d00;
}

.we-incomplete {
  flex: none;
  padding: 1px 8px;
  border-radius: 3px;
  background: #fff7e8;
  font-size: 12px;
  color: #d9762c;
}

.we-head-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.we-field {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.we-field label {
  font-size: 13px;
  color: #4e5969;
}

.we-select {
  height: 32px;
  padding: 0 28px 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  appearance: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.we-select:disabled {
  background-color: #f7f8fa;
  color: #86909c;
  cursor: not-allowed;
}

.we-btn {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.we-btn:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}

.we-btn.is-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.we-btn.is-primary:hover:not(:disabled) {
  background: #0e42d2;
  color: #fff;
}

.we-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.we-notice {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 20px;
  background: #fff7e8;
  font-size: 12.5px;
  color: #d9762c;
}

.we-notice svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.we-notice-btn {
  margin-left: auto;
  flex: none;
  height: 26px;
  padding: 0 12px;
  border: 1px solid #ffcf8b;
  border-radius: 4px;
  background: #fff;
  font-size: 12.5px;
  color: #d9762c;
  cursor: pointer;
}

.we-notice-btn:hover {
  border-color: #d9762c;
}

.we-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}
</style>
