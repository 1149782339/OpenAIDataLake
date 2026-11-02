<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { checkContract, getOperator, type DevOperator } from '@/mock/dev/operators'
import { DEV_INPUT_NODE_ID, type DevEdge, type DevNode } from '@/mock/dev/workflow'

const props = withDefaults(
  defineProps<{
    nodes: DevNode[]
    edges: DevEdge[]
    selectedId?: string | null
    /** 只读模式（已发布版本的画布快照） */
    readonly?: boolean
    /** 是否显示算子面板（可拖入算子） */
    showPalette?: boolean
    /** 管道输入绑定的数据集，未选择时为 null */
    inputDataset?: { name: string; type: string } | null
    /** 未选择管道输入数据集（画布顶部提示） */
    inputMissing?: boolean
    hint?: string
  }>(),
  {
    selectedId: null,
    readonly: false,
    showPalette: false,
    inputDataset: null,
    inputMissing: false,
    hint: '',
  },
)

const emit = defineEmits<{
  'update:selectedId': [id: string | null]
  /** 新增节点（从算子面板拖入） */
  'add-node': [opId: string, x: number, y: number]
  /** 建立连线，valid 表示契约校验是否通过 */
  connect: [from: string, to: string, valid: boolean]
  'move-node': [id: string, x: number, y: number]
  'remove-edge': [id: string]
  'remove-node': [id: string]
  /** 点击「管道输入」节点，打开数据集选择器 */
  'select-input': []
}>()

const NODE_W = 172
const NODE_H = 58
const CANVAS_W = 980
const CANVAS_H = 460

/** 管道输入节点固定在画布左侧 */
const inputNode: DevNode = { id: DEV_INPUT_NODE_ID, opId: '__input__', x: 10, y: 60 }

/** 画布上的全部节点：管道输入 + 算子节点 */
const canvasNodes = computed<DevNode[]>(() => [inputNode, ...props.nodes])

const canvasInner = ref<HTMLElement | null>(null)
const connectingFrom = ref<string | null>(null)
const validateMessage = ref<{ type: 'ok' | 'error'; text: string } | null>(null)
/** 从算子面板拖拽中 */
const draggingOp = ref<{ opId: string; x: number; y: number } | null>(null)
/** 画布内拖动节点 */
const draggingNode = ref<{ id: string; dx: number; dy: number } | null>(null)

const PALETTE_OPS: DevOperator[] = [
  ...(getOperator('op_parse_load') ? [getOperator('op_parse_load')!] : []),
  ...(getOperator('op_feature_extract') ? [getOperator('op_feature_extract')!] : []),
  ...(getOperator('op_cross_modal_match') ? [getOperator('op_cross_modal_match')!] : []),
  ...(getOperator('op_feature_transform') ? [getOperator('op_feature_transform')!] : []),
  ...(getOperator('op_retrieve_fusion') ? [getOperator('op_retrieve_fusion')!] : []),
  ...(getOperator('op_metadata_extract') ? [getOperator('op_metadata_extract')!] : []),
  ...(getOperator('op_data_annotate') ? [getOperator('op_data_annotate')!] : []),
]

function opOf(nodeId: string): DevOperator | undefined {
  const node = canvasNodes.value.find((n) => n.id === nodeId)
  return node ? getOperator(node.opId) : undefined
}

interface EdgeView {
  edge: DevEdge
  path: string
  valid: boolean
  message: string
  midX: number
  midY: number
}

const edgeViews = computed<EdgeView[]>(() =>
  props.edges.map((edge) => {
    const from = canvasNodes.value.find((n) => n.id === edge.from)
    const to = canvasNodes.value.find((n) => n.id === edge.to)
    if (!from || !to) {
      return { edge, path: '', valid: false, message: '节点已删除', midX: 0, midY: 0 }
    }
    const x1 = from.x + NODE_W
    const y1 = from.y + NODE_H / 2
    const x2 = to.x
    const y2 = to.y + NODE_H / 2
    const dx = Math.max(40, Math.abs(x2 - x1) / 2)
    const path = `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`
    // 管道输入 → 算子：连线由数据集类型保证，固定不可改指
    const check =
      edge.from === DEV_INPUT_NODE_ID
        ? { valid: true, message: '管道输入' }
        : checkContract(opOf(edge.from), opOf(edge.to))
    return {
      edge,
      path,
      valid: check.valid,
      message: check.message,
      /** 标签锚在目标节点上方的空白处，避免被节点卡片遮住 */
      midX: to.x + NODE_W / 2,
      midY: to.y - 10,
    }
  }),
)

onMounted(() => {
  window.addEventListener('mousemove', onWindowMouseMove)
  window.addEventListener('mouseup', onWindowMouseUp)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onWindowMouseMove)
  window.removeEventListener('mouseup', onWindowMouseUp)
})

function onPaletteMouseDown(op: DevOperator, event: MouseEvent) {
  if (props.readonly) return
  event.preventDefault()
  draggingOp.value = { opId: op.id, x: event.clientX, y: event.clientY }
}

function onNodeMouseDown(node: DevNode, event: MouseEvent) {
  if (node.id === DEV_INPUT_NODE_ID) {
    // 管道输入：固定节点，点击打开数据集选择器
    event.preventDefault()
    emit('select-input')
    return
  }
  if (props.readonly) {
    emit('update:selectedId', node.id)
    return
  }
  event.preventDefault()
  emit('update:selectedId', node.id)
  // 连线状态下点击目标节点即完成连线
  if (connectingFrom.value && connectingFrom.value !== node.id) {
    finishConnect(connectingFrom.value, node.id)
    return
  }
  const rect = canvasInner.value?.getBoundingClientRect()
  if (!rect) return
  draggingNode.value = {
    id: node.id,
    dx: event.clientX - rect.left - node.x,
    dy: event.clientY - rect.top - node.y,
  }
}

function onPortClick(nodeId: string, event: MouseEvent) {
  if (props.readonly) return
  event.stopPropagation()
  if (!connectingFrom.value) {
    connectingFrom.value = nodeId
    validateMessage.value = { type: 'ok', text: '请选择目标算子节点完成连线' }
    return
  }
  if (connectingFrom.value === nodeId) {
    connectingFrom.value = null
    validateMessage.value = null
    return
  }
  finishConnect(connectingFrom.value, nodeId)
}

function finishConnect(from: string, to: string) {
  const check = checkContract(opOf(from), opOf(to))
  connectingFrom.value = null
  validateMessage.value = {
    type: check.valid ? 'ok' : 'error',
    text: check.valid ? '连线已建立：契约校验通过' : `连线已建立但契约不满足：${check.message}`,
  }
  emit('connect', from, to, check.valid)
}

function onCanvasClick() {
  if (connectingFrom.value) {
    connectingFrom.value = null
    validateMessage.value = null
  }
}

function onWindowMouseMove(event: MouseEvent) {
  if (draggingOp.value) {
    draggingOp.value = { ...draggingOp.value, x: event.clientX, y: event.clientY }
    return
  }
  if (!draggingNode.value) return
  const rect = canvasInner.value?.getBoundingClientRect()
  if (!rect) return
  const node = props.nodes.find((n) => n.id === draggingNode.value!.id)
  if (!node) return
  const x = Math.max(0, Math.min(CANVAS_W - NODE_W, event.clientX - rect.left - draggingNode.value.dx))
  const y = Math.max(0, Math.min(CANVAS_H - NODE_H, event.clientY - rect.top - draggingNode.value.dy))
  emit('move-node', node.id, x, y)
}

function onWindowMouseUp(event: MouseEvent) {
  if (draggingOp.value) {
    const rect = canvasInner.value?.getBoundingClientRect()
    if (rect && event.clientX >= rect.left && event.clientX <= rect.right) {
      const x = Math.max(0, Math.min(CANVAS_W - NODE_W, event.clientX - rect.left - NODE_W / 2))
      const y = Math.max(0, Math.min(CANVAS_H - NODE_H, event.clientY - rect.top - NODE_H / 2))
      emit('add-node', draggingOp.value.opId, Math.round(x), Math.round(y))
    }
    draggingOp.value = null
  }
  draggingNode.value = null
}
</script>

<template>
  <div class="wfc-wrap">
    <aside v-if="showPalette" class="wfc-palette">
      <div class="wfc-panel-title">
        <SvgIcon :icon="LAS_ICONS.operators" />
        <span>算子面板</span>
      </div>
      <p class="wfc-panel-hint">按住算子拖入右侧画布</p>
      <div class="wfc-op-list">
        <button
          v-for="op in PALETTE_OPS"
          :key="op.id"
          class="wfc-op"
          type="button"
          @mousedown="onPaletteMouseDown(op, $event)"
        >
          <span class="wfc-op-name">{{ op.name }}</span>
          <span class="wfc-op-cat">{{ op.category }}</span>
        </button>
      </div>
    </aside>

    <section class="wfc-canvas-box">
      <div class="wfc-bar" :class="{ 'is-error': validateMessage?.type === 'error' }">
        <span v-if="validateMessage" class="wfc-message" :class="`is-${validateMessage.type}`">
          <SvgIcon :icon="validateMessage.type === 'ok' ? LAS_ICONS.check : LAS_ICONS.info" />
          <span>{{ validateMessage.text }}</span>
        </span>
        <span v-else-if="inputMissing && !readonly" class="wfc-hint is-error">
          <SvgIcon :icon="LAS_ICONS.info" />
          请先选择管道输入数据集
        </span>
        <span v-else class="wfc-hint">
          {{ readonly ? hint : connectingFrom ? '连线中：请点击目标算子节点' : '点击节点右侧端口开始连线' }}
        </span>
        <span class="wfc-legend">
          <i class="wfc-dot is-ok" />契约通过
          <i class="wfc-dot is-error" />契约不匹配
        </span>
      </div>

      <div class="wfc-scroll">
        <div
          ref="canvasInner"
          class="wfc-canvas"
          :class="{ 'is-readonly': readonly }"
          :style="{ width: `${CANVAS_W}px`, height: `${CANVAS_H}px` }"
          @click="onCanvasClick"
        >
          <svg class="wfc-edges" :width="CANVAS_W" :height="CANVAS_H">
            <defs>
              <marker id="wfc-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                <path d="M0 0 L8 4 L0 8 Z" fill="#94a3b8" />
              </marker>
              <marker id="wfc-arrow-error" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
                <path d="M0 0 L8 4 L0 8 Z" fill="#f53f3f" />
              </marker>
            </defs>
            <g v-for="view in edgeViews" :key="view.edge.id">
              <path
                class="wfc-edge"
                :class="{ 'is-error': !view.valid, 'is-fixed': view.edge.fixed }"
                :d="view.path"
                :marker-end="view.valid ? 'url(#wfc-arrow)' : 'url(#wfc-arrow-error)'"
                @click="!view.edge.fixed && emit('remove-edge', view.edge.id)"
              >
                <title>
                  {{ view.edge.fixed ? '管道输入连线，不可删除' : readonly ? view.message : '点击删除该连线' }}
                </title>
              </path>
              <text v-if="!view.valid" class="wfc-edge-label" :x="view.midX" :y="view.midY">
                {{ view.message }}
              </text>
            </g>
          </svg>

          <!-- 管道输入节点：固定在画布左侧，虚线框，不可删除 -->
          <div
            class="wfc-node is-input"
            :class="{ 'is-empty': !inputDataset }"
            :style="{ left: `${inputNode.x}px`, top: `${inputNode.y}px`, width: `${NODE_W}px`, height: `${NODE_H}px` }"
            @mousedown="onNodeMouseDown(inputNode, $event)"
          >
            <div class="wfc-node-body">
              <span class="wfc-node-name">管道输入</span>
              <span v-if="inputDataset" class="wfc-node-cat">
                输入：{{ inputDataset.name }}（{{ inputDataset.type }}）
              </span>
              <span v-else class="wfc-node-cat is-error">未选择数据集，点击选择</span>
            </div>
          </div>

          <div
            v-for="node in nodes"
            :key="node.id"
            class="wfc-node"
            :class="{
              'is-active': node.id === selectedId,
              'is-source': node.id === connectingFrom,
              'is-target': connectingFrom !== null && node.id !== connectingFrom,
            }"
            :style="{ left: `${node.x}px`, top: `${node.y}px`, width: `${NODE_W}px`, height: `${NODE_H}px` }"
            @mousedown="onNodeMouseDown(node, $event)"
          >
            <button
              v-if="!readonly"
              class="wfc-port is-in"
              type="button"
              title="输入端口"
              @mousedown.stop
              @click.stop="onPortClick(node.id, $event)"
            />
            <div class="wfc-node-body">
              <span class="wfc-node-name">{{ opOf(node.id)?.name }}</span>
              <span class="wfc-node-cat">{{ opOf(node.id)?.category }}</span>
            </div>
            <button
              v-if="!readonly"
              class="wfc-port is-out"
              type="button"
              title="输出端口，点击后选择下游算子"
              @mousedown.stop
              @click.stop="onPortClick(node.id, $event)"
            />
          </div>
        </div>
      </div>
    </section>

    <div
      v-if="draggingOp"
      class="wfc-ghost"
      :style="{ left: `${draggingOp.x + 10}px`, top: `${draggingOp.y + 8}px` }"
    >
      {{ getOperator(draggingOp.opId)?.name }}
    </div>
  </div>
</template>

<style scoped>
.wfc-wrap {
  flex: 1;
  min-width: 0;
  display: flex;
  overflow: hidden;
}

/* ---------- 算子面板 ---------- */
.wfc-palette {
  flex: none;
  width: 190px;
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
  border-right: 1px solid #f2f3f5;
  overflow: hidden;
}

.wfc-panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #1d2129;
}

.wfc-panel-title svg {
  width: 14px;
  height: 14px;
  color: #4e5969;
}

.wfc-panel-hint {
  margin: 6px 0 10px;
  font-size: 12px;
  color: #86909c;
}

.wfc-op-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.wfc-op {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  margin-bottom: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  text-align: left;
  cursor: grab;
}

.wfc-op:hover {
  border-color: #165dff;
  background: #f7faff;
}

.wfc-op:active {
  cursor: grabbing;
}

.wfc-op-name {
  font-size: 13px;
  color: #1d2129;
}

.wfc-op-cat {
  font-size: 11.5px;
  color: #86909c;
}

/* ---------- 画布 ---------- */
.wfc-canvas-box {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.wfc-bar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 38px;
  padding: 0 16px;
  border-bottom: 1px solid #f2f3f5;
  font-size: 12.5px;
  color: #86909c;
}

.wfc-legend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;
}

.wfc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.wfc-dot.is-ok {
  background: #00b42a;
}

.wfc-dot.is-error {
  background: #f53f3f;
}

.wfc-message {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  min-width: 0;
}

.wfc-hint {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.wfc-hint.is-error {
  color: #f53f3f;
}

.wfc-hint.is-error svg {
  width: 13px;
  height: 13px;
  flex: none;
}

.wfc-message span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wfc-message svg {
  width: 13px;
  height: 13px;
  flex: none;
}

.wfc-message.is-ok {
  color: #00893a;
}

.wfc-message.is-error {
  color: #d9363e;
}

.wfc-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #fbfcfd;
}

.wfc-canvas {
  position: relative;
  background-image: radial-gradient(circle, #e5e6eb 1px, transparent 1px);
  background-size: 16px 16px;
}

.wfc-canvas.is-readonly {
  cursor: default;
}

.wfc-edges {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.wfc-edge {
  fill: none;
  stroke: #94a3b8;
  stroke-width: 1.6;
  pointer-events: stroke;
  cursor: pointer;
}

.wfc-edge.is-error {
  stroke: #f53f3f;
  stroke-dasharray: 6 4;
  stroke-width: 2;
}

.wfc-edge.is-fixed {
  stroke: #165dff;
  stroke-dasharray: 5 3;
  cursor: default;
}

.wfc-edge-label {
  font-size: 11px;
  fill: #f53f3f;
  text-anchor: middle;
}

.wfc-node {
  position: absolute;
  display: flex;
  align-items: center;
  border: 1px solid #dde2e9;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(29, 33, 41, 0.08);
  cursor: grab;
  user-select: none;
}

.wfc-node.is-active {
  border-color: #165dff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.15);
}

/* 管道输入节点：虚线框，固定不可删除 */
.wfc-node.is-input {
  border-style: dashed;
  border-color: #165dff;
  background: #f5f9ff;
  cursor: pointer;
}

.wfc-node.is-input.is-empty {
  border-color: #f53f3f;
  background: #fff6f6;
}

.wfc-node.is-input .wfc-node-name {
  color: #165dff;
}

.wfc-node.is-input .wfc-node-cat {
  font-size: 11px;
  line-height: 1.4;
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wfc-node.is-input .wfc-node-cat.is-error {
  color: #f53f3f;
}

.wfc-node.is-source {
  border-color: #00b42a;
  box-shadow: 0 0 0 2px rgba(0, 180, 42, 0.18);
}

.wfc-node.is-target {
  border-color: #94a3b8;
  border-style: dashed;
}

.wfc-node-body {
  flex: 1;
  min-width: 0;
  padding: 0 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wfc-node-name {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wfc-node-cat {
  font-size: 11.5px;
  color: #86909c;
}

.wfc-port {
  position: absolute;
  top: 50%;
  width: 10px;
  height: 10px;
  margin-top: -5px;
  padding: 0;
  border: 2px solid #165dff;
  border-radius: 50%;
  background: #fff;
  cursor: crosshair;
}

.wfc-port:hover {
  background: #165dff;
}

.wfc-port.is-in {
  left: -6px;
}

.wfc-port.is-out {
  right: -6px;
}

/* ---------- 拖拽幽灵 ---------- */
.wfc-ghost {
  position: fixed;
  z-index: 999;
  padding: 6px 10px;
  border: 1px solid #165dff;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #165dff;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(29, 33, 41, 0.12);
}
</style>
