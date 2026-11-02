<script setup lang="ts">
/**
 * 数据开发 - 数据标注
 * 两个 Tab：自动标注（预标注任务列表）、人工标注工作台（核心页）。
 */
import { computed, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import FramePreview from '@/components/dev/FramePreview.vue'
import {
  DEV_FRAMES,
  DEV_LABEL_CLASSES,
  DEV_PRETASKS,
  type DevFrame,
  type DevPretask,
} from '@/mock/dev/annotation'
import { DEV_PIPELINES } from '@/mock/dev/workflow'

type TabKey = 'auto' | 'manual'

/** 低置信度阈值，低于该值的样本标红 */
const LOW_CONF = 0.8

const tab = ref<TabKey>('auto')

/* ---------------- 自动标注 ---------------- */

const tasks = ref<DevPretask[]>(DEV_PRETASKS.map((t) => ({ ...t })))
const keyword = ref('')

const STATUS_CLASS: Record<DevPretask['status'], string> = {
  标注中: 'is-running',
  已完成: 'is-done',
  等待人工确认: 'is-wait',
}

const filteredTasks = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return tasks.value
  return tasks.value.filter(
    (t) => t.name.toLowerCase().includes(k) || t.pipeline.toLowerCase().includes(k),
  )
})

const createOpen = ref(false)
const formName = ref('')
const formPipeline = ref(DEV_PIPELINES[0]?.name ?? '')
const formSamples = ref(100)
const formThreshold = ref(LOW_CONF)
const formTable = ref('')

function nowText(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function openCreate(): void {
  formName.value = ''
  formPipeline.value = DEV_PIPELINES[0]?.name ?? ''
  formSamples.value = 100
  formThreshold.value = LOW_CONF
  formTable.value = ''
  createOpen.value = true
}

function onCreate(): void {
  const name = formName.value.trim()
  if (!name) return
  tasks.value.unshift({
    id: `task_custom_${Date.now()}`,
    name,
    pipeline: formPipeline.value,
    samples: formSamples.value,
    threshold: formThreshold.value,
    writeBackTable: formTable.value.trim() || `${name}_prelabel`,
    status: '标注中',
    createdAt: nowText(),
  })
  createOpen.value = false
}

/* ---------------- 人工标注工作台 ---------------- */

const frames = ref<DevFrame[]>(
  DEV_FRAMES.map((f) => ({ ...f, bbox: [...f.bbox] as [number, number, number, number] })),
)

const currentTaskId = ref<string>(frames.value[0]?.taskId ?? '')
const currentFrameId = ref<string>(frames.value[0]?.id ?? '')

const currentTask = computed(() => tasks.value.find((t) => t.id === currentTaskId.value))
const taskFrames = computed(() => frames.value.filter((f) => f.taskId === currentTaskId.value))
const current = computed(() => frames.value.find((f) => f.id === currentFrameId.value))
const remaining = computed(() => taskFrames.value.filter((f) => !f.confirmed).length)
const allDone = computed(() => taskFrames.value.length > 0 && remaining.value === 0)

const statusText = computed(() =>
  allDone.value
    ? '全部确认完成，管道已自动续跑'
    : `管道挂起中，剩余 ${remaining.value} 张待确认，全部确认后管道自动续跑`,
)

/** 可编辑的标注框与类别（与当前帧同步） */
const editBox = ref<[number, number, number, number]>([0, 0, 0, 0])
const editLabel = ref<string>(DEV_LABEL_CLASSES[0])

watch(
  currentFrameId,
  () => {
    const f = current.value
    editBox.value = f ? ([...f.bbox] as [number, number, number, number]) : [0, 0, 0, 0]
    editLabel.value = f ? f.label : ''
  },
  { immediate: true },
)

watch(currentTaskId, () => {
  const first = frames.value.find((f) => f.taskId === currentTaskId.value)
  currentFrameId.value = first?.id ?? ''
})

watch(
  editBox,
  (v) => {
    const f = current.value
    if (f) f.bbox = [...v] as [number, number, number, number]
  },
  { deep: true },
)

watch(editLabel, (v) => {
  const f = current.value
  if (f) f.label = v
})

function selectFrame(id: string): void {
  currentFrameId.value = id
}

function onConfirm(): void {
  const f = current.value
  if (!f) return
  f.confirmed = true
  const next = taskFrames.value.find((x) => !x.confirmed)
  if (next) currentFrameId.value = next.id
}

function onSkip(): void {
  const list = taskFrames.value
  if (!list.length) return
  const idx = list.findIndex((x) => x.id === currentFrameId.value)
  const pending = list.find((x) => !x.confirmed && x.id !== currentFrameId.value)
  if (pending) {
    currentFrameId.value = pending.id
    return
  }
  const fallback = list[(idx + 1) % list.length]
  if (fallback) currentFrameId.value = fallback.id
}

/** 由帧 id 生成稳定 seed，让每张示意图长得不一样 */
function seedOf(id: string): number {
  let h = 7
  for (let i = 0; i < id.length; i += 1) h = (h * 31 + id.charCodeAt(i)) % 100000
  return h
}

function fileNameOf(file: string): string {
  const parts = file.split('/')
  return parts[parts.length - 1] ?? file
}
</script>

<template>
  <div class="dn-page">
    <header class="dn-top">
      <span class="dn-title">数据标注</span>
      <nav class="dn-tabs">
        <button
          class="dn-tab"
          :class="{ 'is-active': tab === 'auto' }"
          type="button"
          @click="tab = 'auto'"
        >
          自动标注
        </button>
        <button
          class="dn-tab"
          :class="{ 'is-active': tab === 'manual' }"
          type="button"
          @click="tab = 'manual'"
        >
          人工标注工作台
        </button>
      </nav>
    </header>

    <div class="dn-body">
      <!-- 自动标注 -->
      <section v-show="tab === 'auto'" class="dn-auto">
        <div class="dn-toolbar">
          <div class="dn-search">
            <SvgIcon :icon="LAS_ICONS.search" class="dn-search-ic" />
            <input v-model="keyword" type="text" placeholder="按任务名或来源管道搜索">
          </div>
          <button class="dn-primary" type="button" @click="openCreate">
            <SvgIcon :icon="LAS_ICONS.plus" />
            <span>新建预标注任务</span>
          </button>
        </div>

        <div class="dn-table-wrap">
          <table class="dn-table">
            <colgroup>
              <col style="width: 20%">
              <col style="width: 18%">
              <col style="width: 10%">
              <col style="width: 12%">
              <col style="width: 16%">
              <col style="width: 12%">
              <col style="width: 12%">
            </colgroup>
            <thead>
              <tr>
                <th>任务名</th>
                <th>来源管道</th>
                <th>样本数</th>
                <th>置信度阈值</th>
                <th>回写目标表</th>
                <th>状态</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in filteredTasks" :key="t.id">
                <td class="dn-strong">{{ t.name }}</td>
                <td>{{ t.pipeline }}</td>
                <td class="dn-mono">{{ t.samples }}</td>
                <td class="dn-mono">{{ t.threshold.toFixed(2) }}</td>
                <td class="dn-mono">{{ t.writeBackTable }}</td>
                <td>
                  <span class="dn-status" :class="STATUS_CLASS[t.status]">{{ t.status }}</span>
                </td>
                <td class="dn-mono">{{ t.createdAt }}</td>
              </tr>
              <tr v-if="!filteredTasks.length">
                <td class="dn-empty" colspan="7">暂无匹配任务</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="dn-pager">
          <span class="dn-pager-total">共 {{ filteredTasks.length }} 条</span>
        </div>
      </section>

      <!-- 人工标注工作台 -->
      <section v-show="tab === 'manual'" class="dn-work">
        <div class="dn-status-bar" :class="allDone ? 'is-done' : 'is-wait'">
          <SvgIcon :icon="allDone ? LAS_ICONS.checkSmall : LAS_ICONS.clock" class="dn-status-ic" />
          <span class="dn-status-text">{{ statusText }}</span>
          <span class="dn-status-extra">
            <label class="dn-status-task">当前任务</label>
            <select v-model="currentTaskId" class="dn-task-select">
              <option v-for="t in tasks" :key="t.id" :value="t.id">{{ t.name }}</option>
            </select>
          </span>
        </div>

        <div class="dn-work-main">
          <!-- 左侧：样本列表 -->
          <aside class="dn-list">
            <div class="dn-list-head">
              <span>待确认样本</span>
              <span class="dn-list-count">{{ taskFrames.length }} 张 / 待确认 {{ remaining }} 张</span>
            </div>
            <ul class="dn-list-body">
              <li
                v-for="f in taskFrames"
                :key="f.id"
                class="dn-item"
                :class="{ 'is-active': f.id === currentFrameId }"
                @click="selectFrame(f.id)"
              >
                <span class="dn-thumb" :class="{ 'is-low': f.confidence < LOW_CONF }">
                  <FramePreview :seed="seedOf(f.id)" :width="84" :height="47" :bbox="f.bbox" />
                </span>
                <span class="dn-item-main">
                  <span class="dn-item-name" :title="f.file">{{ fileNameOf(f.file) }}</span>
                  <span class="dn-item-meta">{{ f.ts }} · {{ f.camera }}</span>
                  <span class="dn-item-conf" :class="{ 'is-low': f.confidence < LOW_CONF }">
                    置信度 {{ f.confidence.toFixed(2) }}
                  </span>
                </span>
                <span v-if="f.confirmed" class="dn-item-check">
                  <SvgIcon :icon="LAS_ICONS.checkSmall" />
                </span>
              </li>
              <li v-if="!taskFrames.length" class="dn-item-empty">该任务暂无待确认样本</li>
            </ul>
          </aside>

          <!-- 右侧：大图标注区 -->
          <section class="dn-stage">
            <div class="dn-stage-main">
              <div class="dn-canvas">
                <FramePreview
                  v-if="current"
                  class="dn-big"
                  :seed="seedOf(current.id)"
                  :width="760"
                  :height="428"
                  :show-bbox="true"
                  :bbox="editBox"
                  :bbox-label="editLabel"
                  :caption="`${current.camera}  ${current.ts}`"
                />
                <div v-else class="dn-canvas-empty">请从左侧选择需要标注的样本</div>
              </div>

              <div class="dn-form">
                <div class="dn-form-row">
                  <span class="dn-form-label">检测框坐标</span>
                  <label class="dn-field">
                    <span>X</span>
                    <input v-model.number="editBox[0]" type="number" step="0.01" min="0" max="1">
                  </label>
                  <label class="dn-field">
                    <span>Y</span>
                    <input v-model.number="editBox[1]" type="number" step="0.01" min="0" max="1">
                  </label>
                  <label class="dn-field">
                    <span>W</span>
                    <input v-model.number="editBox[2]" type="number" step="0.01" min="0" max="1">
                  </label>
                  <label class="dn-field">
                    <span>H</span>
                    <input v-model.number="editBox[3]" type="number" step="0.01" min="0" max="1">
                  </label>
                </div>
                <div class="dn-form-row">
                  <span class="dn-form-label">标注类别</span>
                  <select v-model="editLabel" class="dn-select">
                    <option v-for="c in DEV_LABEL_CLASSES" :key="c" :value="c">{{ c }}</option>
                  </select>
                  <span class="dn-form-tip">
                    预标注类别：{{ current?.label ?? '-' }} · 置信度 {{ current?.confidence.toFixed(2) ?? '-' }}
                  </span>
                </div>
              </div>
            </div>

            <aside class="dn-inspector">
              <div class="dn-insp-title">样本信息</div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">文件名</span>
                <span class="dn-insp-value">{{ current ? fileNameOf(current.file) : '-' }}</span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">存储路径</span>
                <span class="dn-insp-value dn-mono">{{ current?.file ?? '-' }}</span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">来源管道</span>
                <span class="dn-insp-value">{{ currentTask?.pipeline ?? '-' }}</span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">摄像头</span>
                <span class="dn-insp-value">{{ current?.camera ?? '-' }}</span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">采集时间</span>
                <span class="dn-insp-value dn-mono">{{ current?.ts ?? '-' }}</span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">模型置信度</span>
                <span
                  class="dn-insp-value"
                  :class="{ 'is-low': (current?.confidence ?? 1) < LOW_CONF }"
                >
                  {{ current ? current.confidence.toFixed(2) : '-' }}
                </span>
              </div>
              <div class="dn-insp-row">
                <span class="dn-insp-label">人工确认</span>
                <span class="dn-insp-value">
                  <span
                    class="dn-status"
                    :class="current?.confirmed ? 'is-done' : 'is-wait'"
                  >
                    {{ current?.confirmed ? '已确认' : '待确认' }}
                  </span>
                </span>
              </div>
            </aside>
          </section>
        </div>

        <footer class="dn-foot">
          <button class="dn-foot-skip" type="button" @click="onSkip">跳过</button>
          <button class="dn-foot-ok" type="button" :disabled="!current" @click="onConfirm">确认</button>
        </footer>
      </section>
    </div>

    <!-- 新建预标注任务 -->
    <LasModal
      :visible="createOpen"
      title="新建预标注任务"
      width="520px"
      @close="createOpen = false"
    >
      <div class="dn-modal-form">
        <label class="dn-modal-row">
          <span class="dn-modal-label">任务名</span>
          <input v-model="formName" class="dn-modal-input" type="text" placeholder="例如：火焰场景预标注">
        </label>
        <label class="dn-modal-row">
          <span class="dn-modal-label">来源管道</span>
          <select v-model="formPipeline" class="dn-modal-input">
            <option v-for="p in DEV_PIPELINES" :key="p.id" :value="p.name">{{ p.name }}</option>
          </select>
        </label>
        <label class="dn-modal-row">
          <span class="dn-modal-label">样本数</span>
          <input v-model.number="formSamples" class="dn-modal-input" type="number" min="1">
        </label>
        <label class="dn-modal-row">
          <span class="dn-modal-label">置信度阈值</span>
          <input v-model.number="formThreshold" class="dn-modal-input" type="number" step="0.01" min="0" max="1">
        </label>
        <label class="dn-modal-row">
          <span class="dn-modal-label">回写目标表</span>
          <input v-model="formTable" class="dn-modal-input" type="text" placeholder="例如：fire_prelabel">
        </label>
      </div>
      <template #footer>
        <button class="dn-ghost" type="button" @click="createOpen = false">取消</button>
        <button class="dn-primary" type="button" @click="onCreate">创建</button>
      </template>
    </LasModal>
  </div>
</template>

<style scoped>
.dn-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

/* 顶部 Tab */
.dn-top {
  flex: none;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.dn-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.dn-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 100%;
}

.dn-tab {
  position: relative;
  height: 100%;
  padding: 0 14px;
  border: 0;
  background: none;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
}

.dn-tab:hover {
  color: #165dff;
}

.dn-tab.is-active {
  color: #165dff;
  font-weight: 500;
}

.dn-tab.is-active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: -1px;
  height: 2px;
  border-radius: 1px;
  background: #165dff;
}

.dn-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
}

/* 自动标注 */
.dn-auto {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 20px 20px;
}

.dn-toolbar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dn-search {
  display: flex;
  align-items: center;
  width: 280px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #f7f8fa;
}

.dn-search:focus-within {
  border-color: #165dff;
  background: #fff;
}

.dn-search-ic {
  flex: none;
  width: 14px;
  height: 14px;
  color: #86909c;
}

.dn-search input {
  flex: 1;
  min-width: 0;
  margin-left: 6px;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.dn-primary {
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

.dn-primary:hover {
  background: #0e42d2;
}

.dn-primary svg {
  width: 13px;
  height: 13px;
}

.dn-ghost {
  height: 32px;
  padding: 0 14px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.dn-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}

.dn-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
}

.dn-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.dn-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.dn-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.dn-table tbody tr:hover td {
  background: #f7f8fa;
}

.dn-strong {
  font-weight: 500;
}

.dn-mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.dn-empty {
  padding: 40px 12px;
  text-align: center;
  color: #86909c;
}

.dn-status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}

.dn-status.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.dn-status.is-done {
  background: #e8ffea;
  color: #00b42a;
}

.dn-status.is-wait {
  background: #fff7e8;
  color: #ff7d00;
}

.dn-pager {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 12px;
}

.dn-pager-total {
  font-size: 12.5px;
  color: #86909c;
}

/* 人工标注工作台 */
.dn-work {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding-bottom: 12px;
}

.dn-status-bar {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 16px 0;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
}

.dn-status-bar.is-wait {
  border: 1px solid #ffe4ba;
  background: #fff7e8;
  color: #ff7d00;
}

.dn-status-bar.is-done {
  border: 1px solid #aff0b5;
  background: #e8ffea;
  color: #00b42a;
}

.dn-status-ic {
  flex: none;
  width: 15px;
  height: 15px;
}

.dn-status-text {
  font-weight: 500;
}

.dn-status-extra {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #4e5969;
}

.dn-status-task {
  color: #86909c;
}

.dn-task-select {
  height: 26px;
  padding: 0 6px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 12.5px;
  outline: none;
}

.dn-work-main {
  flex: 1;
  min-height: 0;
  display: flex;
  margin: 12px 16px 0;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  overflow: hidden;
}

/* 左侧样本列表 */
.dn-list {
  width: 268px;
  flex: none;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
  border-right: 1px solid #f2f3f5;
}

.dn-list-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
}

.dn-list-count {
  font-size: 12px;
  font-weight: 400;
  color: #86909c;
}

.dn-list-body {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 8px;
  overflow: auto;
  list-style: none;
}

.dn-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  margin-bottom: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.dn-item:hover {
  border-color: #c9cdd4;
}

.dn-item.is-active {
  border-color: #165dff;
  background: #f0f5ff;
}

.dn-thumb {
  flex: none;
  display: inline-flex;
  line-height: 0;
  border: 2px solid #e5e6eb;
  border-radius: 4px;
  overflow: hidden;
  background: #1b2438;
}

.dn-thumb.is-low {
  border-color: #f53f3f;
}

.dn-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.dn-item-name {
  font-size: 12.5px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dn-item-meta {
  font-size: 11.5px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dn-item-conf {
  font-size: 12px;
  font-weight: 500;
  color: #00b42a;
}

.dn-item-conf.is-low {
  color: #f53f3f;
}

.dn-item-check {
  flex: none;
  color: #00b42a;
}

.dn-item-check svg {
  width: 15px;
  height: 15px;
}

.dn-item-empty {
  padding: 24px 8px;
  text-align: center;
  font-size: 12.5px;
  color: #86909c;
}

/* 右侧大图标注区 */
.dn-stage {
  flex: 1;
  min-width: 0;
  display: flex;
}

.dn-stage-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.dn-canvas {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  overflow: auto;
  background: #f7f8fa;
}

.dn-big {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  border-radius: 6px;
  box-shadow: 0 2px 10px rgba(29, 33, 41, 0.12);
}

.dn-canvas-empty {
  font-size: 13px;
  color: #86909c;
}

.dn-form {
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid #f2f3f5;
}

.dn-form-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dn-form-label {
  flex: none;
  width: 76px;
  font-size: 13px;
  color: #4e5969;
}

.dn-field {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: #86909c;
}

.dn-field input {
  width: 76px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.dn-field input:focus {
  border-color: #165dff;
}

.dn-select {
  height: 30px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.dn-select:focus {
  border-color: #165dff;
}

.dn-form-tip {
  margin-left: auto;
  font-size: 12.5px;
  color: #86909c;
}

/* 样本信息 */
.dn-inspector {
  width: 212px;
  flex: none;
  padding: 14px;
  border-left: 1px solid #f2f3f5;
  overflow: auto;
}

.dn-insp-title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
}

.dn-insp-row {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-bottom: 12px;
}

.dn-insp-label {
  font-size: 12px;
  color: #86909c;
}

.dn-insp-value {
  font-size: 12.5px;
  color: #1d2129;
  word-break: break-all;
}

.dn-insp-value.is-low {
  color: #f53f3f;
}

/* 底部操作 */
.dn-foot {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px 16px 0;
}

.dn-foot-skip {
  min-width: 120px;
  height: 40px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
}

.dn-foot-skip:hover {
  border-color: #165dff;
  background: #f2f3f5;
  color: #165dff;
}

.dn-foot-ok {
  min-width: 180px;
  height: 40px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  font-size: 15px;
  font-weight: 500;
  color: #fff;
  cursor: pointer;
}

.dn-foot-ok:hover {
  background: #0e42d2;
}

.dn-foot-ok:disabled {
  background: #c9cdd4;
  cursor: not-allowed;
}

/* 弹窗表单 */
.dn-modal-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dn-modal-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dn-modal-label {
  flex: none;
  width: 84px;
  font-size: 13px;
  color: #4e5969;
}

.dn-modal-input {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.dn-modal-input:focus {
  border-color: #165dff;
}
</style>
