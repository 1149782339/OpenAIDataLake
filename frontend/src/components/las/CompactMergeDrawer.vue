<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'

export interface CompactMergeConfig {
  resources: number
  resourceUnit: string
  targetSize: number
  targetSizeUnit: string
  cronEnabled: boolean
  cronTime: string
  stopPrevious: boolean
  customKeys: { key: string; value: string }[]
}

const props = defineProps<{ visible: boolean }>()

const emit = defineEmits<{
  close: []
  confirm: [config: CompactMergeConfig]
}>()

/** 可选的自定义配置项 */
const CONFIG_KEYS = [
  'target_rows_per_fragment',
  'max_rows_per_group',
  'materialize_deletions',
  'materialize_deletions_threshold',
  'batch_size',
  'defer_index_remap',
]

const resources = ref(10)
const targetSize = ref(1)
const cronEnabled = ref(true)
const cronTime = ref('')
const stopPrevious = ref(false)
const customKeys = ref<{ key: string; value: string }[]>([{ key: '', value: '' }])

const pickOpen = ref(false)
const pickRoot = ref<HTMLElement | null>(null)
const timeInput = ref<HTMLInputElement | null>(null)

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    resources.value = 10
    targetSize.value = 1
    cronEnabled.value = true
    cronTime.value = ''
    stopPrevious.value = false
    customKeys.value = [{ key: '', value: '' }]
    pickOpen.value = false
  },
)

/** 打开原生时间选择器 */
function openTimePicker() {
  const el = timeInput.value
  if (!el) return
  try {
    el.showPicker()
  } catch (e) {
    el.focus()
  }
}

function onDocClick(event: MouseEvent) {
  if (!pickRoot.value) return
  if (!pickRoot.value.contains(event.target as Node)) pickOpen.value = false
}

onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

function pickKey(key: string) {
  const row = customKeys.value[0]
  if (row) row.key = key
  pickOpen.value = false
}

function addRow() {
  customKeys.value.push({ key: '', value: '' })
}

function removeRow(index: number) {
  customKeys.value.splice(index, 1)
  if (!customKeys.value.length) customKeys.value.push({ key: '', value: '' })
}

function onConfirm() {
  emit('confirm', {
    resources: resources.value,
    resourceUnit: 'CU',
    targetSize: targetSize.value,
    targetSizeUnit: 'MB',
    cronEnabled: cronEnabled.value,
    cronTime: cronTime.value,
    stopPrevious: stopPrevious.value,
    customKeys: customKeys.value.map((r) => ({ ...r })),
  })
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="cm-mask" @click="emit('close')" />
    <aside v-if="visible" class="cm-drawer">
      <header class="cm-header">
        <h3 class="cm-title">开启小文件合并</h3>
        <button class="cm-close" type="button" title="关闭" @click="emit('close')">
          <SvgIcon :icon="LAS_ICONS.close" />
        </button>
      </header>

      <div class="cm-body">
        <!-- 合并任务 -->
        <section class="cm-section">
          <div class="cm-section-title">合并任务</div>

          <div class="cm-row">
            <div class="cm-label">资源分配</div>
            <div class="cm-control">
              <div class="cm-number">
                <button class="cm-number-btn" type="button" title="减少" @click="resources = Math.max(1, resources - 1)">
                  <SvgIcon :icon="LAS_ICONS.minus" />
                </button>
                <div class="cm-number-field">
                  <input v-model.number="resources" type="text" class="cm-number-input">
                  <span class="cm-number-unit">CU</span>
                </div>
                <button class="cm-number-btn" type="button" title="增加" @click="resources += 1">
                  <SvgIcon :icon="LAS_ICONS.plus" />
                </button>
              </div>
            </div>
          </div>

          <div class="cm-row">
            <div class="cm-label">目标文件大小</div>
            <div class="cm-control">
              <div class="cm-number">
                <button class="cm-number-btn is-disabled" type="button" title="减少" disabled>
                  <SvgIcon :icon="LAS_ICONS.minus" />
                </button>
                <div class="cm-number-field">
                  <input v-model.number="targetSize" type="text" class="cm-number-input">
                  <span class="cm-number-unit">MB</span>
                </div>
                <button class="cm-number-btn" type="button" title="增加" @click="targetSize += 1">
                  <SvgIcon :icon="LAS_ICONS.plus" />
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- 调度 -->
        <section class="cm-section">
          <div class="cm-section-title">调度</div>

          <div class="cm-row">
            <div class="cm-label">周期调度</div>
            <div class="cm-control">
              <button
                class="cm-switch"
                :class="{ on: cronEnabled }"
                type="button"
                role="switch"
                :aria-checked="cronEnabled"
                @click="cronEnabled = !cronEnabled"
              >
                <i class="cm-switch-dot" />
              </button>
            </div>
          </div>

          <div v-if="cronEnabled" class="cm-row">
            <div class="cm-label"><i class="cm-asterisk">*</i>调度频率</div>
            <div class="cm-control">
              <div class="cm-cron">
                <span class="cm-cron-text">每天</span>
                <div class="cm-time">
                  <input
                    ref="timeInput"
                    v-model="cronTime"
                    type="time"
                    class="cm-time-input"
                    :class="{ 'is-empty': !cronTime }"
                  >
                  <span v-if="!cronTime" class="cm-time-ph">请选择时间</span>
                  <SvgIcon class="cm-time-ic" :icon="LAS_ICONS.clock" @click="openTimePicker" />
                </div>
              </div>
            </div>
          </div>

          <div class="cm-row">
            <div class="cm-label">终止前一个优化任务</div>
            <div class="cm-control">
              <button
                class="cm-switch"
                :class="{ on: stopPrevious }"
                type="button"
                role="switch"
                :aria-checked="stopPrevious"
                @click="stopPrevious = !stopPrevious"
              >
                <i class="cm-switch-dot" />
              </button>
            </div>
          </div>
        </section>

        <!-- 高级配置 -->
        <section class="cm-section">
          <div class="cm-section-title">高级配置</div>

          <div
            v-for="(row, index) in customKeys"
            :key="index"
            class="cm-row"
          >
            <div class="cm-label">自定义配置</div>
            <div class="cm-control cm-custom">
              <div ref="index === 0 ? pickRoot : undefined" class="cm-pick">
                <button class="cm-pick-view" :class="{ on: pickOpen }" type="button" @click="pickOpen = !pickOpen">
                  <span class="cm-pick-value">{{ row.key || '请选择' }}</span>
                  <SvgIcon class="cm-pick-arrow" :class="{ up: pickOpen }" :icon="LAS_ICONS.chevronDown" />
                </button>
                <ul v-if="pickOpen" class="cm-pick-options">
                  <li
                    v-for="key in CONFIG_KEYS"
                    :key="key"
                    class="cm-pick-option"
                    @click="pickKey(key)"
                  >
                    {{ key }}
                  </li>
                </ul>
              </div>

              <input v-model="row.value" type="text" class="cm-custom-input" placeholder="请输入 Value">

              <button class="cm-del" type="button" @click="removeRow(index)">
                <SvgIcon class="cm-del-ic" :icon="LAS_ICONS.trash" />
                <span>删除</span>
              </button>
            </div>
          </div>

          <button class="cm-add" type="button" @click="addRow">
            <SvgIcon class="cm-add-ic" :icon="LAS_ICONS.plus" />
            <span>添加配置</span>
          </button>
        </section>
      </div>

      <footer class="cm-footer">
        <button class="cm-btn cm-btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="cm-btn cm-btn-primary" type="button" @click="onConfirm">确定</button>
      </footer>
    </aside>
  </Teleport>
</template>

<style scoped>
.cm-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
}

.cm-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  width: 766px;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  animation: cm-slide-in 0.2s ease;
}

@keyframes cm-slide-in {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.cm-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px 0 24px;
}

.cm-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  line-height: 28px;
  color: #0c0d0e;
}

.cm-close {
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #0c0d0e;
  cursor: pointer;
}

.cm-close svg {
  width: 20px;
  height: 20px;
}

.cm-close:hover {
  color: #165dff;
}

.cm-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px 24px;
}

.cm-section + .cm-section {
  margin-top: 44px;
}

.cm-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
  color: rgba(20, 20, 20, 0.9);
}

.cm-section-title::before {
  content: '';
  display: block;
  width: 3px;
  height: 14px;
  border-radius: 0 3px 3px 0;
  background: #5652ff;
}

.cm-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  padding-left: 11px;
}

.cm-label {
  flex: none;
  width: 130px;
  padding-top: 7px;
  box-sizing: border-box;
  font-size: 13px;
  line-height: 18px;
  color: #0c0d0e;
}

.cm-asterisk {
  margin-right: 4px;
  font-style: normal;
  color: #e33232;
}

.cm-control {
  flex: 1;
  min-width: 0;
}

/* 数字输入 */
.cm-number {
  display: inline-flex;
  align-items: stretch;
  width: 156px;
  height: 32px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
}

.cm-number-btn {
  width: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: #fff;
  color: #4e5969;
  cursor: pointer;
}

.cm-number-btn svg {
  width: 12px;
  height: 12px;
}

.cm-number-btn:hover:not(:disabled) {
  color: #165dff;
}

.cm-number-btn.is-disabled,
.cm-number-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.cm-number-field {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  border-left: 1px solid #dde2e9;
  border-right: 1px solid #dde2e9;
}

.cm-number-input {
  flex: 1;
  min-width: 0;
  height: 30px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  text-align: right;
  font-size: 13px;
  color: #0c0d0e;
}

.cm-number-unit {
  flex: none;
  font-size: 13px;
  color: #4e5969;
}

/* 开关 */
.cm-switch {
  width: 40px;
  height: 20px;
  position: relative;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #c7ccd6;
  cursor: pointer;
  transition: background 0.15s ease;
}

.cm-switch.on {
  background: #165dff;
}

.cm-switch-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s ease;
}

.cm-switch.on .cm-switch-dot {
  transform: translateX(20px);
}

/* 调度频率 */
.cm-cron {
  display: flex;
  align-items: center;
  gap: 16px;
}

.cm-cron-text {
  font-size: 13px;
  color: #0c0d0e;
}

.cm-time {
  position: relative;
  width: 167px;
  height: 32px;
  display: flex;
  align-items: center;
  padding: 0 11px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
}

.cm-time:hover {
  border-color: #9cc6fa;
}

.cm-time-input {
  flex: 1;
  min-width: 0;
  height: 30px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #0c0d0e;
}

.cm-time-input::-webkit-calendar-picker-indicator {
  display: none;
}

.cm-time-input.is-empty {
  color: transparent;
}

.cm-time-ph {
  position: absolute;
  left: 11px;
  font-size: 13px;
  color: #86909c;
  pointer-events: none;
}

.cm-time-ic {
  flex: none;
  width: 13px;
  height: 13px;
  color: #86909c;
  cursor: pointer;
}

/* 自定义配置 */
.cm-custom {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cm-pick {
  position: relative;
  flex: none;
  width: 220px;
}

.cm-pick-view {
  width: 100%;
  height: 32px;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 11px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #0c0d0e;
  cursor: pointer;
}

.cm-pick-view.on {
  border-color: #165dff;
}

.cm-pick-value {
  flex: 1;
  min-width: 0;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cm-pick-arrow {
  flex: none;
  width: 12px;
  height: 12px;
  color: #86909c;
  transition: transform 0.15s ease;
}

.cm-pick-arrow.up {
  transform: rotate(180deg);
}

.cm-pick-options {
  position: absolute;
  top: 34px;
  left: 0;
  right: 0;
  z-index: 20;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  max-height: 240px;
  overflow: auto;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.cm-pick-option {
  padding: 6px 12px;
  font-size: 13px;
  color: #0c0d0e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.cm-pick-option:hover {
  background: #f2f3f5;
}

.cm-custom-input {
  flex: none;
  width: 218px;
  height: 32px;
  padding: 0 11px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #0c0d0e;
  outline: none;
}

.cm-custom-input:hover {
  border-color: #9cc6fa;
}

.cm-custom-input::placeholder {
  color: #86909c;
}

.cm-del {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.cm-del:hover {
  text-decoration: underline;
}

.cm-del-ic {
  width: 14px;
  height: 14px;
}

.cm-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 141px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.cm-add-ic {
  width: 13px;
  height: 13px;
}

.cm-footer {
  flex: none;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e5e6eb;
}

.cm-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.cm-btn-ghost {
  border: 1px solid #dde2e9;
  background: #fff;
  color: #42464e;
}

.cm-btn-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}

.cm-btn-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.cm-btn-primary:hover {
  background: #0e42d2;
}
</style>
