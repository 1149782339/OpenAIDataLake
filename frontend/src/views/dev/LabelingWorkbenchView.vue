<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import FramePreview from '@/components/dev/FramePreview.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { DEV_LABEL_CLASSES, type DevLabelItem } from '@/mock/dev/labeling'
import { useDevLabelingStore } from '@/stores/devLabeling'

const route = useRoute()
const router = useRouter()
const store = useDevLabelingStore()

const taskId = computed(() => String(route.params.taskId ?? ''))
const task = computed(() => store.taskById(taskId.value))

const currentItemId = ref<string>('')
const editLabel = ref('')
const editBox = ref<[number, number, number, number]>([0, 0, 0, 0])

const items = computed(() => task.value?.items ?? [])
const currentItem = computed(() => items.value.find((i) => i.id === currentItemId.value) ?? null)
const pending = computed(() => items.value.filter((i) => !i.reviewAction))
const remaining = computed(() => pending.value.length)
const doneCount = computed(() => items.value.length - remaining.value)
const progress = computed(() =>
  items.value.length ? Math.round((doneCount.value / items.value.length) * 100) : 0,
)

/** 挂起策略对应的提示 */
const policyTip = computed(() => {
  if (task.value?.suspendPolicy === 'suspend') return '全部确认后管道自动续跑'
  if (task.value?.suspendPolicy === 'split') return '确认完成后请触发下游管道'
  return ''
})

function seedOf(item: DevLabelItem): number {
  return item.id.split('').reduce((sum, ch) => sum + ch.charCodeAt(0), 0)
}

/** 缩略图上的框颜色：待确认红虚线 / 已确认绿实线 / 已驳回灰 */
function boxStyle(item: DevLabelItem): { color: string; dash: boolean } {
  if (item.reviewAction === 'confirmed' || item.reviewAction === 'modified') {
    return { color: '#00b42a', dash: false }
  }
  if (item.reviewAction === 'excluded') return { color: '#86909c', dash: true }
  return { color: '#f53f3f', dash: true }
}

function loadItem(item: DevLabelItem | null) {
  if (!item) return
  editLabel.value = item.finalLabel ?? item.label
  const box = item.finalBbox ?? item.bbox
  editBox.value = [box[0], box[1], box[2], box[3]]
}

watch(
  () => task.value?.id,
  () => {
    const first = pending.value[0] ?? items.value[0] ?? null
    currentItemId.value = first?.id ?? ''
    loadItem(first)
  },
  { immediate: true },
)

function selectItem(item: DevLabelItem) {
  currentItemId.value = item.id
  loadItem(item)
}

function nextPending(fromId: string): DevLabelItem | null {
  const list = pending.value
  const index = list.findIndex((i) => i.id === fromId)
  if (index < 0) return list[0] ?? null
  return list[index + 1] ?? list[index] ?? null
}

/** 确认（保留预标注） */
function onConfirm() {
  const item = currentItem.value
  if (!item || !task.value) return
  store.review(task.value.id, item.id, 'confirmed')
  const next = nextPending(item.id)
  currentItemId.value = next?.id ?? item.id
  loadItem(next)
}

/** 保存修改 */
function onSaveModify() {
  const item = currentItem.value
  if (!item || !task.value) return
  store.review(task.value.id, item.id, 'modified', {
    label: editLabel.value,
    bbox: [...editBox.value] as [number, number, number, number],
  })
  const next = nextPending(item.id)
  currentItemId.value = next?.id ?? item.id
  loadItem(next)
}

/** 驳回：该条不进入结果表 */
function onExclude() {
  const item = currentItem.value
  if (!item || !task.value) return
  store.review(task.value.id, item.id, 'excluded', { label: '正常' })
  const next = nextPending(item.id)
  currentItemId.value = next?.id ?? item.id
  loadItem(next)
}

/** 确认下一张（快捷键 Enter） */
function onConfirmNext() {
  const item = currentItem.value
  if (!item) return
  if (item.reviewAction) {
    const next = nextPending(item.id) ?? pending.value[0] ?? null
    currentItemId.value = next?.id ?? item.id
    loadItem(next)
    return
  }
  onConfirm()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter') return
  const tag = (event.target as HTMLElement | null)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
  event.preventDefault()
  onConfirmNext()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))

function onBack() {
  router.push('/dev/labeling')
}

function statusText(item: DevLabelItem): string {
  if (item.reviewAction === 'confirmed') return '已确认'
  if (item.reviewAction === 'modified') return '已修改'
  if (item.reviewAction === 'excluded') return '已驳回'
  return '待确认'
}
</script>

<template>
  <div class="lw-page">
    <header class="lw-head">
      <button class="lw-back" type="button" title="返回任务列表" @click="onBack">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="lw-title">{{ task?.name ?? '标注任务' }}</span>
      <span class="lw-task-id">{{ task?.id }}</span>
      <span
        v-if="task"
        class="lw-status"
        :class="{
          'is-running': task.status === '进行中',
          'is-done': task.status === '已完成',
          'is-rejected': task.status === '已驳回',
        }"
      >
        {{ task.status }}
      </span>
    </header>

    <div v-if="task" class="lw-bar">
      <SvgIcon :icon="LAS_ICONS.info" />
      <span>
        来源：{{ task.pipelineName }} {{ task.versionName }} · run#{{ task.runNo }} · 剩余
        {{ remaining }} 张待确认
      </span>
      <span v-if="policyTip" class="lw-bar-tip">{{ policyTip }}</span>
    </div>

    <div v-if="task" class="lw-body">
      <!-- 左侧缩略图列表 -->
      <aside class="lw-side">
        <div class="lw-side-head">
          <span>待确认样本</span>
          <span class="lw-side-count">{{ remaining }} / {{ items.length }}</span>
        </div>
        <div class="lw-list">
          <button
            v-for="item in items"
            :key="item.id"
            class="lw-item"
            :class="{
              'is-active': item.id === currentItemId,
              'is-pending': !item.reviewAction,
            }"
            type="button"
            @click="selectItem(item)"
          >
            <span class="lw-thumb" :class="{ 'is-pending': !item.reviewAction }">
              <FramePreview
                :src="item.image"
                :seed="seedOf(item)"
                :width="104"
                :height="58"
                show-bbox
                :bbox="item.finalBbox ?? item.bbox"
                :bbox-label="item.finalLabel ?? item.label"
                :bbox-color="boxStyle(item).color"
                :bbox-dash="boxStyle(item).dash"
              />
            </span>
            <span class="lw-item-main">
              <span class="lw-item-name">{{ item.file.split('/').pop() }}</span>
              <span class="lw-item-meta">
                {{ item.camera }} · 置信度 {{ item.confidence.toFixed(2) }}
              </span>
              <span class="lw-item-state" :class="`is-${item.reviewAction ?? 'pending'}`">
                {{ statusText(item) }}
              </span>
            </span>
          </button>
        </div>
      </aside>

      <!-- 右侧大图标注区 -->
      <section class="lw-main">
        <div v-if="currentItem" class="lw-canvas">
          <FramePreview
            :src="currentItem.image"
            :seed="seedOf(currentItem)"
            :width="720"
            :height="405"
            show-bbox
            :bbox="editBox"
            :bbox-label="editLabel"
            :bbox-color="currentItem.reviewAction ? '#00b42a' : '#f53f3f'"
            :bbox-dash="!currentItem.reviewAction"
            :caption="`${currentItem.camera} ${currentItem.ts}`"
          />
          <div v-if="currentItem.reviewAction === 'excluded'" class="lw-excluded">
            已驳回 · 不写入结果表
          </div>
        </div>
        <div v-else class="lw-canvas is-empty">该任务没有待确认样本</div>

        <div class="lw-editor">
          <div class="lw-edit-row">
            <label class="lw-edit-label">标注类别</label>
            <select v-model="editLabel" class="lw-select">
              <option v-for="item in DEV_LABEL_CLASSES" :key="item" :value="item">
                {{ item }}
              </option>
            </select>
            <span class="lw-edit-tip">机器预标注：{{ currentItem?.label }}（{{ currentItem?.confidence.toFixed(2) }}）</span>
          </div>

          <div class="lw-edit-row">
            <label class="lw-edit-label">检测框</label>
            <span class="lw-box-input">
              <label>X<input v-model.number="editBox[0]" type="number" step="0.01" class="lw-input"></label>
              <label>Y<input v-model.number="editBox[1]" type="number" step="0.01" class="lw-input"></label>
              <label>W<input v-model.number="editBox[2]" type="number" step="0.01" class="lw-input"></label>
              <label>H<input v-model.number="editBox[3]" type="number" step="0.01" class="lw-input"></label>
            </span>
          </div>

          <div class="lw-actions">
            <button class="lw-btn is-primary" type="button" @click="onConfirm">
              确认（保留预标注）
            </button>
            <button class="lw-btn" type="button" @click="onSaveModify">保存修改</button>
            <button class="lw-btn is-danger" type="button" @click="onExclude">驳回</button>
          </div>
        </div>

        <!-- 底部：任务进度 -->
        <div class="lw-foot">
          <button class="lw-next" type="button" @click="onConfirmNext">
            确认下一张
            <span class="lw-next-key">Enter</span>
          </button>
          <div class="lw-progress">
            <div class="lw-progress-head">
              <span>任务进度</span>
              <span>{{ doneCount }} / {{ items.length }}（{{ progress }}%）</span>
            </div>
            <div class="lw-bar-track">
              <i class="lw-bar-in" :style="{ width: `${progress}%` }" />
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-else class="lw-empty-page">
      <span>标注任务不存在</span>
      <button class="lw-btn" type="button" @click="onBack">返回任务列表</button>
    </div>
  </div>
</template>

<style scoped>
.lw-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1180px;
  background: #fff;
}

.lw-head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.lw-back {
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

.lw-back:hover {
  color: #165dff;
  background: #f2f3f5;
}

.lw-back svg {
  width: 16px;
  height: 16px;
}

.lw-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.lw-task-id {
  padding: 1px 8px;
  border-radius: 3px;
  background: #f2f3f5;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: #4e5969;
}

.lw-status {
  padding: 1px 9px;
  border-radius: 11px;
  background: #f2f3f5;
  font-size: 12px;
  color: #86909c;
}

.lw-status.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.lw-status.is-done {
  background: #f0fbf3;
  color: #00893a;
}

.lw-status.is-rejected {
  background: #fff1f0;
  color: #d9363e;
}

/* ---------- 顶部状态条 ---------- */
.lw-bar {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 20px;
  background: #fff7e8;
  font-size: 12.5px;
  color: #d9762c;
}

.lw-bar svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.lw-bar-tip {
  margin-left: auto;
  padding: 1px 9px;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.7);
  color: #b8621f;
}

.lw-body {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

/* ---------- 左侧列表 ---------- */
.lw-side {
  flex: none;
  width: 268px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #f2f3f5;
  overflow: hidden;
}

.lw-side-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 14px;
  border-bottom: 1px solid #f2f3f5;
  font-size: 13px;
  color: #1d2129;
}

.lw-side-count {
  font-size: 12px;
  color: #86909c;
}

.lw-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 10px;
}

.lw-item {
  display: flex;
  gap: 10px;
  width: 100%;
  margin-bottom: 8px;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.lw-item:hover {
  background: #f7f8fa;
}

.lw-item.is-active {
  border-color: #165dff;
  background: #f0f7ff;
}

.lw-thumb {
  flex: none;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #e5e6eb;
}

.lw-thumb.is-pending {
  border-color: #f53f3f;
}

.lw-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lw-item-name {
  font-size: 12.5px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lw-item-meta {
  font-size: 11.5px;
  color: #86909c;
}

.lw-item-state {
  font-size: 11.5px;
}

.lw-item-state.is-pending {
  color: #f53f3f;
}

.lw-item-state.is-confirmed,
.lw-item-state.is-modified {
  color: #00b42a;
}

.lw-item-state.is-excluded {
  color: #86909c;
}

/* ---------- 主区 ---------- */
.lw-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.lw-canvas {
  flex: none;
  position: relative;
  display: flex;
  justify-content: center;
  padding: 16px;
  background: #fbfcfd;
}

.lw-canvas.is-empty {
  height: 220px;
  align-items: center;
  color: #86909c;
  font-size: 13px;
}

.lw-excluded {
  position: absolute;
  top: 28px;
  left: 50%;
  transform: translateX(-50%);
  padding: 4px 12px;
  border-radius: 4px;
  background: rgba(29, 33, 41, 0.72);
  color: #fff;
  font-size: 12px;
}

.lw-editor {
  flex: none;
  padding: 14px 20px;
  border-top: 1px solid #f2f3f5;
}

.lw-edit-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.lw-edit-label {
  flex: none;
  width: 72px;
  font-size: 13px;
  color: #4e5969;
}

.lw-select {
  height: 30px;
  padding: 0 26px 0 10px;
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

.lw-edit-tip {
  font-size: 12px;
  color: #86909c;
}

.lw-box-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.lw-box-input label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #86909c;
}

.lw-input {
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

.lw-input:focus {
  border-color: #165dff;
}

.lw-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.lw-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.lw-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.lw-btn.is-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.lw-btn.is-primary:hover {
  background: #0e42d2;
  color: #fff;
}

.lw-btn.is-danger {
  border-color: #f53f3f;
  color: #f53f3f;
}

.lw-btn.is-danger:hover {
  background: #fff1f0;
}

/* ---------- 底部进度 ---------- */
.lw-foot {
  flex: none;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 20px;
  border-top: 1px solid #f2f3f5;
  background: #fff;
}

.lw-next {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 18px;
  border: 1px solid #1759dd;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
}

.lw-next:hover {
  background: #0e42d2;
}

.lw-next-key {
  padding: 1px 6px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 11px;
}

.lw-progress {
  flex: 1;
  min-width: 0;
}

.lw-progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12.5px;
  color: #4e5969;
}

.lw-bar-track {
  height: 6px;
  border-radius: 3px;
  background: #f2f3f5;
  overflow: hidden;
}

.lw-bar-in {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: #165dff;
}

.lw-empty-page {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #86909c;
  font-size: 13px;
}
</style>
