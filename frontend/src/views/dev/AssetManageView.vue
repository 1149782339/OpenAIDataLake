<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import type { IconDef } from '@/components/icons'
import {
  DEV_ASSET_PIPELINES,
  DEV_AUTH_SCOPES,
  DEV_LINEAGE_EDGES,
  DEV_LINEAGE_NODES,
  DEV_PERMISSIONS,
  DEV_PUBLISHED_DATASETS,
} from '@/mock/dev/assets'
import type {
  DevAssetPipeline,
  DevLineageEdge,
  DevLineageNode,
  DevPublishedDataset,
} from '@/mock/dev/assets'

/* ------------------------------------------------------------------ 通用 */

const TABS = [
  { key: 'register', label: '资产登记' },
  { key: 'dataset', label: '发布数据集' },
  { key: 'lineage', label: '数据血缘' },
] as const

type DaTab = (typeof TABS)[number]['key']

const activeTab = ref<DaTab>('register')

function nowText(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

/* ------------------------------------------------------------- 资产登记 */

const assetRows = ref<DevAssetPipeline[]>(
  DEV_ASSET_PIPELINES.map((p) => ({ ...p, outputTables: [...p.outputTables] })),
)
const assetKeyword = ref('')

const filteredAssets = computed(() => {
  const kw = assetKeyword.value.trim().toLowerCase()
  if (!kw) return assetRows.value
  return assetRows.value.filter(
    (p) =>
      p.name.toLowerCase().includes(kw) ||
      p.outputTables.some((t) => t.toLowerCase().includes(kw)),
  )
})

function assetStatusClass(status: DevAssetPipeline['autoRegister']): string {
  if (status === '已登记') return 'is-ok'
  if (status === '登记失败') return 'is-fail'
  return 'is-idle'
}

/* 手动登记弹窗 */
const registerOpen = ref(false)
const registerPipelineId = ref('')
const registerOwner = ref('')
const registerError = ref('')

const registerPipeline = computed(
  () => assetRows.value.find((p) => p.id === registerPipelineId.value) ?? null,
)

watch(registerPipelineId, () => {
  registerError.value = ''
  if (registerPipeline.value) registerOwner.value = registerPipeline.value.owner
})

function openRegister() {
  registerPipelineId.value = ''
  registerOwner.value = ''
  registerError.value = ''
  registerOpen.value = true
}

function confirmRegister() {
  const pipe = registerPipeline.value
  if (!pipe) {
    registerError.value = '请选择需要登记的管道'
    return
  }
  pipe.autoRegister = '已登记'
  pipe.updatedAt = nowText()
  registerOpen.value = false
}

/* ----------------------------------------------------------- 发布数据集 */

const datasetRows = ref<DevPublishedDataset[]>(DEV_PUBLISHED_DATASETS.map((d) => ({ ...d })))
const datasetKeyword = ref('')

const filteredDatasets = computed(() => {
  const kw = datasetKeyword.value.trim().toLowerCase()
  if (!kw) return datasetRows.value
  return datasetRows.value.filter(
    (d) =>
      d.name.toLowerCase().includes(kw) ||
      d.table.toLowerCase().includes(kw) ||
      d.scope.toLowerCase().includes(kw),
  )
})

/* 发布弹窗 */
const publishOpen = ref(false)
const pubName = ref('')
const pubPipelineId = ref('')
const pubTable = ref('')
const pubScopes = ref<string[]>([])
const pubPermission = ref<'只读' | '读写'>('只读')
const pubError = ref('')

const pubPipeline = computed(
  () => assetRows.value.find((p) => p.id === pubPipelineId.value) ?? null,
)
const pubTables = computed(() => pubPipeline.value?.outputTables ?? [])

watch(pubPipelineId, () => {
  pubTable.value = ''
})

function openPublish() {
  pubName.value = ''
  pubPipelineId.value = ''
  pubTable.value = ''
  pubScopes.value = []
  pubPermission.value = '只读'
  pubError.value = ''
  publishOpen.value = true
}

function confirmPublish() {
  const name = pubName.value.trim()
  if (!name) {
    pubError.value = '请输入数据集名称'
    return
  }
  if (!pubTable.value) {
    pubError.value = '请选择关联的表'
    return
  }
  if (!pubScopes.value.length) {
    pubError.value = '请至少选择一项授权范围'
    return
  }
  datasetRows.value.unshift({
    id: `pub_ds_${Date.now()}`,
    name,
    table: pubTable.value,
    scope: pubScopes.value.join('、'),
    permission: pubPermission.value,
    version: 'v1.0.0',
    updatedAt: nowText(),
  })
  publishOpen.value = false
}

/* ------------------------------------------------------------- 数据血缘 */

const NODE_W = 152
const NODE_H = 56
const COL_GAP = 44
const ROW_GAP = 26
/** 顶部留白，用于放置边上的管道名 */
const TOP_PAD = 46

const NODE_META: Record<DevLineageNode['type'], { label: string; icon: IconDef; color: string }> = {
  raw: { label: '原始表', icon: LAS_ICONS.db, color: '#86909c' },
  table: { label: '中间表', icon: LAS_ICONS.asset, color: '#165dff' },
  dataset: { label: '数据集', icon: LAS_ICONS.dataset, color: '#00b42a' },
  index: { label: '索引表', icon: LAS_ICONS.search, color: '#ff7d00' },
}

const selectedNodeId = ref<string | null>(null)

/** 每个节点所处的列（层级） */
const nodeLevels = computed(() => {
  const level = new Map<string, number>()
  const incoming = new Map<string, string[]>()
  DEV_LINEAGE_NODES.forEach((n) => incoming.set(n.id, []))
  DEV_LINEAGE_EDGES.forEach((e) => {
    const list = incoming.get(e.to)
    if (list) list.push(e.from)
  })

  const visit = (id: string, seen: Set<string>): number => {
    const cached = level.get(id)
    if (cached !== undefined) return cached
    if (seen.has(id)) return 0
    seen.add(id)
    let lv = 0
    const preds = incoming.get(id) ?? []
    preds.forEach((p) => {
      lv = Math.max(lv, visit(p, seen) + 1)
    })
    level.set(id, lv)
    return lv
  }

  DEV_LINEAGE_NODES.forEach((n) => visit(n.id, new Set()))
  return level
})

const nodePos = computed(() => {
  const level = nodeLevels.value
  const cols = new Map<number, DevLineageNode[]>()
  DEV_LINEAGE_NODES.forEach((n) => {
    const lv = level.get(n.id) ?? 0
    const arr = cols.get(lv) ?? []
    arr.push(n)
    cols.set(lv, arr)
  })

  const pos = new Map<string, { x: number; y: number }>()
  const colKeys = [...cols.keys()].sort((a, b) => a - b)
  colKeys.forEach((lv, ci) => {
    const arr = cols.get(lv) ?? []
    arr.forEach((n, ri) => {
      pos.set(n.id, { x: ci * (NODE_W + COL_GAP), y: TOP_PAD + ri * (NODE_H + ROW_GAP) })
    })
  })
  return pos
})

const nodeItems = computed(() =>
  DEV_LINEAGE_NODES.map((node) => {
    const p = nodePos.value.get(node.id)
    return { node, x: p?.x ?? 0, y: p?.y ?? 0 }
  }),
)

interface DaEdgeGeom {
  id: string
  pipeline: string
  manual: boolean
  d: string
  mx: number
  my: number
  labelW: number
  state: 'normal' | 'active' | 'dim'
}

/** 高亮集合：选中节点 + 其全部上游 / 下游 */
const highlight = computed(() => {
  const id = selectedNodeId.value
  if (!id) return null

  const nodes = new Set<string>([id])
  const edges = new Set<string>()

  const upStack = [id]
  const upSeen = new Set<string>([id])
  while (upStack.length) {
    const cur = upStack.pop() as string
    DEV_LINEAGE_EDGES.forEach((e) => {
      if (e.to === cur && !upSeen.has(e.from)) {
        upSeen.add(e.from)
        nodes.add(e.from)
        edges.add(e.id)
        upStack.push(e.from)
      }
    })
  }

  const downStack = [id]
  const downSeen = new Set<string>([id])
  while (downStack.length) {
    const cur = downStack.pop() as string
    DEV_LINEAGE_EDGES.forEach((e) => {
      if (e.from === cur && !downSeen.has(e.to)) {
        downSeen.add(e.to)
        nodes.add(e.to)
        edges.add(e.id)
        downStack.push(e.to)
      }
    })
  }

  DEV_LINEAGE_EDGES.forEach((e) => {
    if (nodes.has(e.from) && nodes.has(e.to)) edges.add(e.id)
  })

  return { nodes, edges }
})

function textWidth(text: string): number {
  let w = 0
  for (const ch of text) w += ch.charCodeAt(0) > 255 ? 12 : 7
  return w + 14
}

const edgeGeoms = computed<DaEdgeGeom[]>(() => {
  const pos = nodePos.value
  const hl = highlight.value
  return DEV_LINEAGE_EDGES.map((e: DevLineageEdge) => {
    const a = pos.get(e.from)
    const b = pos.get(e.to)
    const x1 = (a?.x ?? 0) + NODE_W
    const y1 = (a?.y ?? 0) + NODE_H / 2
    const x2 = b?.x ?? 0
    const y2 = (b?.y ?? 0) + NODE_H / 2
    const dx = Math.max(40, Math.abs(x2 - x1) / 2)
    const state: DaEdgeGeom['state'] = !hl
      ? 'normal'
      : hl.edges.has(e.id)
        ? 'active'
        : 'dim'
    return {
      id: e.id,
      pipeline: e.pipeline,
      manual: e.manual === true,
      d: `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`,
      mx: (x1 + x2) / 2,
      my: (y1 + y2) / 2,
      labelW: textWidth(e.pipeline),
      state,
    }
  })
})

const graphWidth = computed(() => {
  let max = 0
  nodeItems.value.forEach((n) => {
    max = Math.max(max, n.x + NODE_W)
  })
  return max + 48
})

const graphHeight = computed(() => {
  let max = 0
  nodeItems.value.forEach((n) => {
    max = Math.max(max, n.y + NODE_H)
  })
  return max + 48
})

function edgeStroke(g: DaEdgeGeom): string {
  if (g.state === 'dim') return '#e5e6eb'
  if (g.manual) return '#ff7d00'
  return g.state === 'active' ? '#165dff' : '#c9cdd4'
}

function edgeMarker(g: DaEdgeGeom): string {
  if (g.state === 'dim') return `url(#da-arrow-dim${g.manual ? '-manual' : ''})`
  if (g.state === 'active') return g.manual ? 'url(#da-arrow-manual)' : 'url(#da-arrow-active)'
  return g.manual ? 'url(#da-arrow-manual)' : 'url(#da-arrow-normal)'
}

function edgeTextColor(g: DaEdgeGeom): string {
  if (g.state === 'dim') return '#c9cdd4'
  if (g.manual) return '#ff7d00'
  return g.state === 'active' ? '#165dff' : '#4e5969'
}

function nodeStateClass(id: string): string {
  const hl = highlight.value
  if (!hl) return ''
  if (id === selectedNodeId.value) return 'is-active'
  return hl.nodes.has(id) ? 'is-linked' : 'is-dim'
}

function edgeStateClass(g: DaEdgeGeom): string {
  return g.state === 'dim' ? 'is-dim' : g.state === 'active' ? 'is-active' : ''
}

function toggleNode(id: string) {
  selectedNodeId.value = selectedNodeId.value === id ? null : id
}

function clearHighlight() {
  selectedNodeId.value = null
}
</script>

<template>
  <div class="da-page">
    <nav class="da-tabs">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        class="da-tab"
        :class="{ 'is-active': activeTab === tab.key }"
        type="button"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div class="da-body">
      <!-- ============================ 资产登记 ============================ -->
      <section v-show="activeTab === 'register'" class="da-panel">
        <div class="da-toolbar">
          <button class="da-primary-btn" type="button" @click="openRegister">
            <SvgIcon :icon="LAS_ICONS.plus" />
            <span>注册资产 / 手动登记</span>
          </button>

          <div class="da-search">
            <SvgIcon class="da-search-ic" :icon="LAS_ICONS.search" />
            <input v-model="assetKeyword" type="text" placeholder="搜索管道名或表名">
          </div>
        </div>

        <div class="da-table-wrap">
          <table class="da-table">
            <thead>
              <tr>
                <th>管道名</th>
                <th>版本</th>
                <th>产出的表</th>
                <th>自动登记状态</th>
                <th>Owner</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredAssets" :key="row.id">
                <td>
                  <span class="da-strong">{{ row.name }}</span>
                </td>
                <td class="da-mono">{{ row.version }}</td>
                <td>
                  <div class="da-chips">
                    <span v-for="t in row.outputTables" :key="t" class="da-chip">{{ t }}</span>
                  </div>
                </td>
                <td>
                  <span class="da-status" :class="assetStatusClass(row.autoRegister)">
                    {{ row.autoRegister }}
                  </span>
                </td>
                <td class="da-sub">{{ row.owner }}</td>
                <td class="da-sub da-mono">{{ row.updatedAt }}</td>
              </tr>
              <tr v-if="!filteredAssets.length">
                <td colspan="6">
                  <div class="da-empty">没有匹配的资产</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ========================== 发布数据集 ========================== -->
      <section v-show="activeTab === 'dataset'" class="da-panel">
        <div class="da-toolbar">
          <button class="da-primary-btn" type="button" @click="openPublish">
            <SvgIcon :icon="LAS_ICONS.plus" />
            <span>发布</span>
          </button>

          <div class="da-search">
            <SvgIcon class="da-search-ic" :icon="LAS_ICONS.search" />
            <input v-model="datasetKeyword" type="text" placeholder="搜索数据集名或表名">
          </div>
        </div>

        <div class="da-table-wrap">
          <table class="da-table">
            <thead>
              <tr>
                <th>数据集名称</th>
                <th>关联表</th>
                <th>授权范围</th>
                <th>权限</th>
                <th>版本</th>
                <th>更新时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredDatasets" :key="row.id">
                <td>
                  <span class="da-strong">{{ row.name }}</span>
                </td>
                <td class="da-mono">{{ row.table }}</td>
                <td class="da-sub">{{ row.scope }}</td>
                <td>
                  <span class="da-perm" :class="row.permission === '读写' ? 'is-rw' : 'is-ro'">
                    {{ row.permission }}
                  </span>
                </td>
                <td class="da-mono">{{ row.version }}</td>
                <td class="da-sub da-mono">{{ row.updatedAt }}</td>
              </tr>
              <tr v-if="!filteredDatasets.length">
                <td colspan="6">
                  <div class="da-empty">没有匹配的数据集</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ========================== 数据血缘 ============================ -->
      <section v-show="activeTab === 'lineage'" class="da-panel">
        <div class="da-toolbar">
          <div class="da-legend">
            <span class="da-legend-item">
              <i class="da-dot" style="background: #86909c" />原始表
            </span>
            <span class="da-legend-item">
              <i class="da-dot" style="background: #165dff" />中间表
            </span>
            <span class="da-legend-item">
              <i class="da-dot" style="background: #00b42a" />数据集
            </span>
            <span class="da-legend-item">
              <i class="da-dot" style="background: #ff7d00" />索引表
            </span>
            <span class="da-legend-item da-legend-manual">
              <i class="da-dash" />人工确认
            </span>
          </div>

          <div class="da-toolbar-right">
            <span class="da-hint">
              {{ selectedNodeId ? '已高亮上下游链路，再次点击节点可取消' : '点击节点可展开上游 / 下游' }}
            </span>
            <button class="da-ghost-btn" type="button" :disabled="!selectedNodeId" @click="clearHighlight">
              <SvgIcon :icon="LAS_ICONS.refresh" />
              <span>清除高亮</span>
            </button>
          </div>
        </div>

        <div class="da-canvas">
          <div class="da-canvas-inner" :style="{ width: graphWidth + 'px', height: graphHeight + 'px' }">
            <svg
              class="da-graph"
              :width="graphWidth"
              :height="graphHeight"
              :viewBox="`0 0 ${graphWidth} ${graphHeight}`"
            >
              <defs>
                <marker
                  id="da-arrow-normal"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="7"
                  markerHeight="7"
                  orient="auto-start-reverse"
                >
                  <path d="M0,0 L10,5 L0,10 z" fill="#c9cdd4" />
                </marker>
                <marker
                  id="da-arrow-active"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="7"
                  markerHeight="7"
                  orient="auto-start-reverse"
                >
                  <path d="M0,0 L10,5 L0,10 z" fill="#165dff" />
                </marker>
                <marker
                  id="da-arrow-dim"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="7"
                  markerHeight="7"
                  orient="auto-start-reverse"
                >
                  <path d="M0,0 L10,5 L0,10 z" fill="#e5e6eb" />
                </marker>
                <marker
                  id="da-arrow-manual"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="7"
                  markerHeight="7"
                  orient="auto-start-reverse"
                >
                  <path d="M0,0 L10,5 L0,10 z" fill="#ff7d00" />
                </marker>
                <marker
                  id="da-arrow-dim-manual"
                  viewBox="0 0 10 10"
                  refX="9"
                  refY="5"
                  markerWidth="7"
                  markerHeight="7"
                  orient="auto-start-reverse"
                >
                  <path d="M0,0 L10,5 L0,10 z" fill="#e5e6eb" />
                </marker>
              </defs>

              <g
                v-for="g in edgeGeoms"
                :key="g.id"
                class="da-edge"
                :class="[edgeStateClass(g), { 'is-manual': g.manual }]"
              >
                <path
                  class="da-edge-line"
                  :d="g.d"
                  :style="{ stroke: edgeStroke(g) }"
                  :marker-end="edgeMarker(g)"
                />
                <rect
                  class="da-edge-bg"
                  :x="g.mx - g.labelW / 2"
                  :y="g.my - 52"
                  :width="g.labelW"
                  height="20"
                  rx="4"
                />
                <text
                  class="da-edge-text"
                  :x="g.mx"
                  :y="g.my - 37"
                  text-anchor="middle"
                  :style="{ fill: edgeTextColor(g) }"
                >
                  {{ g.pipeline }}
                </text>
              </g>
            </svg>

            <button
              v-for="item in nodeItems"
              :key="item.node.id"
              class="da-node"
              :class="[nodeStateClass(item.node.id), 'is-' + item.node.type]"
              type="button"
              :style="{ left: item.x + 'px', top: item.y + 'px', width: NODE_W + 'px', height: NODE_H + 'px' }"
              @click="toggleNode(item.node.id)"
            >
              <span
                class="da-node-icon"
                :style="{ color: NODE_META[item.node.type].color }"
              >
                <SvgIcon :icon="NODE_META[item.node.type].icon" />
              </span>
              <span class="da-node-main">
                <span class="da-node-name">{{ item.node.label }}</span>
                <span class="da-node-type">{{ NODE_META[item.node.type].label }}</span>
              </span>
            </button>
          </div>
        </div>
      </section>
    </div>

    <!-- ========================== 手动登记资产 ========================== -->
    <LasModal :visible="registerOpen" title="手动登记资产" width="560px" @close="registerOpen = false">
      <div class="da-tip">
        <SvgIcon :icon="LAS_ICONS.info" />
        <span>登记后该管道的产出表将进入资产目录，可被发布为数据集。</span>
      </div>

      <div class="da-row">
        <label class="da-label"><i class="da-req">*</i>管道</label>
        <div class="da-control">
          <select v-model="registerPipelineId" class="da-input da-select">
            <option value="">请选择管道</option>
            <option v-for="p in assetRows" :key="p.id" :value="p.id">
              {{ p.name }}（{{ p.version }}）
            </option>
          </select>
          <p v-if="registerPipeline" class="da-hint-text">
            产出表：{{ registerPipeline.outputTables.join('、') }}
          </p>
          <div v-if="registerError" class="da-error">{{ registerError }}</div>
        </div>
      </div>

      <div class="da-row">
        <label class="da-label">Owner</label>
        <div class="da-control">
          <input v-model="registerOwner" type="text" class="da-input" placeholder="资产负责人">
        </div>
      </div>

      <template #footer>
        <button class="da-btn da-btn-ghost" type="button" @click="registerOpen = false">取消</button>
        <button class="da-btn da-btn-primary" type="button" @click="confirmRegister">确认登记</button>
      </template>
    </LasModal>

    <!-- ========================== 发布数据集 ============================ -->
    <LasModal :visible="publishOpen" title="发布数据集" width="620px" @close="publishOpen = false">
      <div class="da-tip">
        <SvgIcon :icon="LAS_ICONS.info" />
        <span>数据集发布后仅授权范围内的用户 / 部门可见，并按所选权限访问。</span>
      </div>

      <div class="da-row">
        <label class="da-label"><i class="da-req">*</i>数据集名称</label>
        <div class="da-control">
          <input
            v-model="pubName"
            type="text"
            class="da-input"
            :class="{ 'is-error': pubError && !pubName.trim() }"
            placeholder="例如：fire_dataset_v4"
          >
        </div>
      </div>

      <div class="da-row">
        <label class="da-label"><i class="da-req">*</i>关联管道</label>
        <div class="da-control">
          <select v-model="pubPipelineId" class="da-input da-select">
            <option value="">请选择管道</option>
            <option v-for="p in assetRows" :key="p.id" :value="p.id">
              {{ p.name }}（{{ p.version }}）
            </option>
          </select>
        </div>
      </div>

      <div class="da-row">
        <label class="da-label"><i class="da-req">*</i>关联表</label>
        <div class="da-control">
          <select
            v-model="pubTable"
            class="da-input da-select"
            :disabled="!pubPipelineId"
          >
            <option value="">{{ pubPipelineId ? '请选择产出表' : '请先选择管道' }}</option>
            <option v-for="t in pubTables" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
      </div>

      <div class="da-row">
        <label class="da-label"><i class="da-req">*</i>授权范围</label>
        <div class="da-control">
          <div class="da-scopes">
            <label v-for="s in DEV_AUTH_SCOPES" :key="s" class="da-check">
              <input v-model="pubScopes" type="checkbox" :value="s">
              <span>{{ s }}</span>
            </label>
          </div>
          <p class="da-hint-text">授权对象为用户 / 部门，可多选。</p>
        </div>
      </div>

      <div class="da-row">
        <label class="da-label">权限</label>
        <div class="da-control">
          <div class="da-radios">
            <label v-for="p in DEV_PERMISSIONS" :key="p" class="da-radio">
              <input v-model="pubPermission" type="radio" :value="p">
              <span>{{ p }}</span>
            </label>
          </div>
        </div>
      </div>

      <div v-if="pubError" class="da-error da-error-block">{{ pubError }}</div>

      <template #footer>
        <button class="da-btn da-btn-ghost" type="button" @click="publishOpen = false">取消</button>
        <button class="da-btn da-btn-primary" type="button" @click="confirmPublish">确认发布</button>
      </template>
    </LasModal>
  </div>
</template>

<style scoped>
.da-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

/* --------------------------------------------- Tab 切换条 */
.da-tabs {
  flex: none;
  display: flex;
  align-items: stretch;
  gap: 28px;
  height: 48px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.da-tab {
  position: relative;
  padding: 0;
  border: 0;
  background: none;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
}

.da-tab:hover {
  color: #165dff;
}

.da-tab.is-active {
  color: #165dff;
  font-weight: 500;
}

.da-tab.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: #165dff;
}

/* --------------------------------------------- 内容区 */
.da-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

.da-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* --------------------------------------------- 工具栏 */
.da-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.da-toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.da-hint {
  font-size: 12.5px;
  color: #86909c;
}

.da-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.da-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: #4e5969;
}

.da-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.da-dash {
  width: 18px;
  height: 0;
  border-top: 2px dashed #ff7d00;
}

.da-legend-manual {
  color: #ff7d00;
}

.da-primary-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.da-primary-btn:hover {
  background: #0e42d2;
}

.da-primary-btn svg {
  width: 13px;
  height: 13px;
}

.da-ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.da-ghost-btn:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}

.da-ghost-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.da-ghost-btn svg {
  width: 14px;
  height: 14px;
}

/* --------------------------------------------- 搜索框 */
.da-search {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 240px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.da-search:focus-within {
  border-color: #165dff;
}

.da-search-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.da-search input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.da-search input::placeholder {
  color: #86909c;
}

/* --------------------------------------------- 表格 */
.da-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e6eb;
  border-radius: 8px 8px 0 0;
}

.da-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.da-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.da-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.da-table tbody tr:hover td {
  background: #f7f8fa;
}

.da-strong {
  font-weight: 500;
}

.da-sub {
  color: #4e5969;
}

.da-mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #4e5969;
  font-size: 12px;
  white-space: nowrap;
}

.da-empty {
  height: 100px;
  text-align: center;
  color: #86909c;
}

/* --------------------------------------------- 单元格内的标签 */
.da-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.da-chip {
  padding: 1px 8px;
  border-radius: 3px;
  background: #f2f3f5;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #4e5969;
}

.da-status {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}

.da-status.is-ok {
  background: #e8ffea;
  color: #00b42a;
}

.da-status.is-idle {
  background: #f2f3f5;
  color: #86909c;
}

.da-status.is-fail {
  background: #ffece8;
  color: #f53f3f;
}

.da-perm {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}

.da-perm.is-ro {
  background: #f2f3f5;
  color: #4e5969;
}

.da-perm.is-rw {
  background: #e8f3ff;
  color: #165dff;
}

/* --------------------------------------------- 血缘画布 */
.da-canvas {
  flex: 1;
  min-height: 320px;
  overflow: auto;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background:
    linear-gradient(90deg, #f7f8fa 1px, transparent 1px) 0 0 / 20px 20px,
    linear-gradient(#f7f8fa 1px, transparent 1px) 0 0 / 20px 20px,
    #fff;
}

.da-canvas-inner {
  position: relative;
}

.da-graph {
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
}

.da-edge-line {
  fill: none;
  stroke-width: 1.6;
}

.da-edge.is-manual .da-edge-line {
  stroke-dasharray: 5 4;
}

.da-edge-bg {
  fill: #fff;
  stroke: #f2f3f5;
  stroke-width: 1;
}

.da-edge-text {
  font-size: 12px;
}

.da-edge.is-dim .da-edge-bg {
  fill: #fff;
}

.da-node {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
}

.da-node:hover {
  border-color: #9cc6fa;
  box-shadow: 0 2px 8px rgba(29, 33, 41, 0.08);
}

.da-node.is-active {
  border-color: #165dff;
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.12);
}

.da-node.is-linked {
  border-color: #9cc6fa;
}

.da-node.is-dim {
  opacity: 0.28;
}

.da-node-icon {
  flex: none;
  display: inline-flex;
}

.da-node-icon svg {
  width: 18px;
  height: 18px;
}

.da-node-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.da-node-name {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.da-node-type {
  font-size: 11.5px;
  color: #86909c;
}

/* --------------------------------------------- 弹窗表单 */
.da-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #e8f3ff;
  font-size: 12.5px;
  color: #165dff;
}

.da-tip svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.da-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.da-label {
  flex: none;
  width: 100px;
  padding-top: 7px;
  padding-right: 16px;
  box-sizing: border-box;
  text-align: right;
  font-size: 13px;
  color: #1d2129;
}

.da-req {
  margin-right: 4px;
  font-style: normal;
  color: #f53f3f;
}

.da-control {
  flex: 1;
  min-width: 0;
}

.da-input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  box-sizing: border-box;
}

.da-input:focus {
  border-color: #165dff;
}

.da-input.is-error {
  border-color: #f53f3f;
}

.da-input::placeholder {
  color: #86909c;
}

.da-input:disabled {
  background: #f7f8fa;
  color: #86909c;
  cursor: not-allowed;
}

.da-select {
  appearance: none;
  cursor: pointer;
  padding-right: 24px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.da-select:disabled {
  cursor: not-allowed;
}

.da-hint-text {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

.da-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f53f3f;
}

.da-error-block {
  margin-top: -8px;
  margin-bottom: 16px;
}

.da-scopes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.da-check,
.da-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.da-check input,
.da-radio input {
  width: 14px;
  height: 14px;
  margin: 0;
  accent-color: #165dff;
  cursor: pointer;
}

.da-radios {
  display: flex;
  gap: 24px;
}

.da-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.da-btn-ghost {
  border: 1px solid #dde2e9;
  background: #f6f8fa;
  color: #42464e;
}

.da-btn-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}

.da-btn-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.da-btn-primary:hover {
  background: #0e42d2;
}
</style>
