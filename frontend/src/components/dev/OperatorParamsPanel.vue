<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { getOperator, type DevParamDef } from '@/mock/dev/operators'

const props = withDefaults(
  defineProps<{
    /** 选中的算子 id */
    opId?: string | null
    /** 当前选中的节点 id（用于删除节点） */
    nodeId?: string | null
    /** 算子参数值 */
    params?: Record<string, unknown>
    /** 只读模式（已发布版本的参数快照） */
    readonly?: boolean
    /** 管道数据类型（继承自管道输入数据集） */
    dataType?: string
    /** 数据类型是否被数据集锁定 */
    dataTypeLocked?: boolean
    /** 数据类型可选项 */
    dataTypeOptions?: { value: string; label: string }[]
    /** 是否存在未配置完整的必填参数 */
    incomplete?: boolean
  }>(),
  {
    opId: null,
    nodeId: null,
    params: () => ({}),
    readonly: false,
    dataType: '',
    dataTypeLocked: false,
    dataTypeOptions: () => [],
    incomplete: false,
  },
)

const emit = defineEmits<{
  'update-param': [key: string, value: unknown]
  'update-data-type': [value: string]
  'remove-node': [id: string]
}>()

const operator = computed(() => (props.opId ? getOperator(props.opId) : undefined))

/** 联动字段的当前取值 */
function driverValue(driver: string): string {
  if (driver === 'data_type') return props.dataType ?? ''
  const v = props.params[driver]
  if (typeof v === 'boolean') return v ? 'true' : 'false'
  return v === undefined || v === null ? '' : String(v)
}

function isEmpty(value: unknown): boolean {
  if (value === undefined || value === null) return true
  if (typeof value === 'string') return value.trim() === ''
  if (typeof value === 'number') return Number.isNaN(value)
  return false
}

interface ParamItem {
  def: DevParamDef
  value: unknown
  /** 是否缺少必填配置 */
  missing: boolean
  /** 是否来自联动参数组 */
  linked: boolean
  groupLabel: string
}

const paramItems = computed<ParamItem[]>(() => {
  const op = operator.value
  if (!op) return []

  const toItem = (def: DevParamDef, linked: boolean, groupLabel: string): ParamItem => {
    const value = props.params[def.key] ?? def.default
    return {
      def,
      value,
      missing: Boolean(def.required) && isEmpty(value),
      linked,
      groupLabel,
    }
  }

  const base = op.params.map((def) => toItem(def, false, ''))

  const linked = (op.groups ?? [])
    .filter((group) => driverValue(group.driver) === group.value)
    .flatMap((group) => group.params.map((def) => toItem(def, true, group.value)))

  return [...base, ...linked]
})

/** 当前节点是否配置完整 */
const nodeIncomplete = computed(() => paramItems.value.some((item) => item.missing))

function parseList(value: string): string[] {
  return value
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function onNumberInput(def: DevParamDef, event: Event) {
  const raw = (event.target as HTMLInputElement).value
  emit('update-param', def.key, raw === '' ? '' : Number(raw))
}
</script>

<template>
  <aside class="wpp">
    <div class="wpp-title">
      <SvgIcon :icon="LAS_ICONS.doc" />
      <span>算子参数</span>
      <span v-if="readonly" class="wpp-lock">只读</span>
    </div>

    <!-- 数据类型：管道级字段，位于参数面板顶部 -->
    <div class="wpp-type">
      <label class="wpp-type-label">
        数据类型
        <span v-if="dataTypeLocked" class="wpp-type-lock">由数据集决定</span>
      </label>
      <select
        class="wpp-select"
        :disabled="readonly || dataTypeLocked"
        :value="dataType"
        @change="emit('update-data-type', ($event.target as HTMLSelectElement).value)"
      >
        <option value="">请选择数据类型</option>
        <option v-for="opt in dataTypeOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </option>
      </select>
      <p class="wpp-type-hint">
        {{ dataTypeLocked ? '数据类型继承自管道输入数据集，不可修改' : '未选择数据集时可手动指定数据类型' }}
      </p>
    </div>

    <div v-if="!operator" class="wpp-empty">点击画布中的算子节点查看参数</div>

    <div v-else class="wpp-body">
      <div class="wpp-head">
        <span class="wpp-name">{{ operator.name }}</span>
        <span class="wpp-cat">{{ operator.category }}</span>
        <span v-if="nodeIncomplete" class="wpp-warn">未配置完整</span>
      </div>
      <p class="wpp-desc">{{ operator.desc }}</p>

      <div class="wpp-block">
        <div class="wpp-block-title">输入契约要求列</div>
        <div v-for="col in operator.inputs" :key="col.name" class="wpp-col">
          <span class="wpp-col-name">{{ col.name }}</span>
          <span class="wpp-col-type">{{ col.type }}</span>
        </div>
      </div>

      <div class="wpp-block">
        <div class="wpp-block-title">输出 schema 声明列</div>
        <div v-for="col in operator.outputs" :key="col.name" class="wpp-col">
          <span class="wpp-col-name">{{ col.name }}</span>
          <span class="wpp-col-type">{{ col.type }}</span>
        </div>
      </div>

      <div class="wpp-block">
        <div class="wpp-block-title">参数</div>
        <div v-if="!paramItems.length" class="wpp-empty is-inline">该算子无需额外参数</div>

        <div v-for="item in paramItems" :key="item.def.key" class="wpp-param">
          <label class="wpp-param-label">
            <span class="wpp-param-key">
              {{ item.def.label }}
              <i v-if="item.def.required" class="wpp-req">必填</i>
            </span>
            <span class="wpp-param-default">
              默认 {{ Array.isArray(item.value) ? item.value.join('、') : item.value }}
            </span>
          </label>

          <div v-if="item.def.type === 'boolean'" class="wpp-control">
            <button
              class="wpp-switch"
              :class="{ on: item.value === true }"
              type="button"
              role="switch"
              :disabled="readonly"
              :aria-checked="item.value === true"
              @click="emit('update-param', item.def.key, item.value !== true)"
            >
              <i class="wpp-switch-dot" />
            </button>
            <span class="wpp-switch-text">{{ item.value === true ? '开启' : '关闭' }}</span>
          </div>

          <div v-else-if="item.def.type === 'number'" class="wpp-control">
            <input
              class="wpp-input"
              :class="{ 'is-error': item.missing }"
              type="number"
              :disabled="readonly"
              :value="item.value"
              @input="onNumberInput(item.def, $event)"
            >
          </div>

          <div v-else-if="item.def.type === 'enum'" class="wpp-control">
            <select
              class="wpp-input wpp-select"
              :class="{ 'is-error': item.missing }"
              :disabled="readonly"
              :value="item.value"
              @change="emit('update-param', item.def.key, ($event.target as HTMLSelectElement).value)"
            >
              <option v-for="opt in item.def.options ?? []" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div v-else-if="item.def.type === 'list'" class="wpp-control">
            <input
              class="wpp-input"
              :class="{ 'is-error': item.missing }"
              type="text"
              :disabled="readonly"
              :value="(item.value as string[]).join('、')"
              @input="emit('update-param', item.def.key, parseList(($event.target as HTMLInputElement).value))"
            >
          </div>

          <div v-else class="wpp-control">
            <input
              class="wpp-input"
              :class="{ 'is-error': item.missing }"
              type="text"
              :disabled="readonly"
              :value="item.value"
              @input="emit('update-param', item.def.key, ($event.target as HTMLInputElement).value)"
            >
          </div>

          <p v-if="item.missing" class="wpp-missing">必填参数未配置，发布前需补全</p>
          <p v-else class="wpp-param-desc">{{ item.def.desc }}</p>
        </div>
      </div>

      <button
        v-if="!readonly && nodeId"
        class="wpp-danger"
        type="button"
        @click="emit('remove-node', nodeId)"
      >
        <SvgIcon :icon="LAS_ICONS.trash" />
        <span>删除该节点</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.wpp {
  flex: none;
  width: 284px;
  display: flex;
  flex-direction: column;
  padding: 14px 14px 16px;
  border-left: 1px solid #f2f3f5;
  overflow-y: auto;
}

.wpp-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #1d2129;
}

.wpp-title svg {
  width: 14px;
  height: 14px;
  color: #4e5969;
}

.wpp-lock {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 3px;
  background: #f2f3f5;
  font-size: 11.5px;
  font-weight: 400;
  color: #86909c;
}

/* ---------- 数据类型 ---------- */
.wpp-type {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fafbfc;
}

.wpp-type-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: #4e5969;
}

.wpp-type-lock {
  padding: 1px 6px;
  border-radius: 3px;
  background: #e8f3ff;
  font-size: 11px;
  color: #165dff;
}

.wpp-select {
  width: 100%;
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
  box-sizing: border-box;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.wpp-select:focus {
  border-color: #165dff;
}

.wpp-select:disabled {
  background-color: #f2f3f5;
  color: #86909c;
  cursor: not-allowed;
}

.wpp-type-hint {
  margin: 8px 0 0;
  font-size: 11.5px;
  line-height: 1.6;
  color: #86909c;
}

/* ---------- 节点信息 ---------- */
.wpp-body {
  margin-top: 14px;
}

.wpp-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wpp-name {
  font-size: 15px;
  font-weight: 500;
  color: #1d2129;
}

.wpp-cat {
  padding: 1px 7px;
  border-radius: 3px;
  background: #f2f3f5;
  font-size: 12px;
  color: #4e5969;
}

.wpp-warn {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 3px;
  background: #fff7e8;
  font-size: 11.5px;
  color: #d9762c;
}

.wpp-desc {
  margin: 8px 0 14px;
  font-size: 12.5px;
  line-height: 1.7;
  color: #4e5969;
}

.wpp-block {
  margin-bottom: 16px;
}

.wpp-block-title {
  margin-bottom: 6px;
  font-size: 12.5px;
  color: #86909c;
}

.wpp-col {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 24px;
  font-size: 12.5px;
}

.wpp-col-name {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #1d2129;
}

.wpp-col-type {
  padding: 1px 6px;
  border-radius: 3px;
  background: #f7f8fa;
  font-size: 11.5px;
  color: #4e5969;
}

/* ---------- 参数 ---------- */
.wpp-param {
  margin-bottom: 14px;
}

.wpp-param-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #1d2129;
}

.wpp-param-key {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.wpp-req {
  padding: 0 5px;
  border-radius: 3px;
  background: #fff1f0;
  font-style: normal;
  font-family: inherit;
  font-size: 11px;
  color: #f53f3f;
}

.wpp-param-default {
  font-size: 11.5px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wpp-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wpp-input {
  width: 100%;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  box-sizing: border-box;
}

.wpp-input:focus {
  border-color: #165dff;
}

.wpp-input.is-error {
  border-color: #f53f3f;
  background: #fffafa;
}

.wpp-input:disabled {
  background: #f7f8fa;
  color: #86909c;
  cursor: not-allowed;
}

.wpp-switch {
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

.wpp-switch.on {
  background: #165dff;
}

.wpp-switch:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.wpp-switch-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s ease;
}

.wpp-switch.on .wpp-switch-dot {
  transform: translateX(20px);
}

.wpp-switch-text {
  font-size: 12.5px;
  color: #4e5969;
}

.wpp-param-desc {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #86909c;
}

.wpp-missing {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #f53f3f;
}

.wpp-empty {
  margin-top: 24px;
  font-size: 13px;
  color: #86909c;
  text-align: center;
}

.wpp-danger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #f53f3f;
  cursor: pointer;
}

.wpp-danger:hover {
  text-decoration: underline;
}

.wpp-danger svg {
  width: 13px;
  height: 13px;
}

.wpp-empty.is-inline {
  margin: 0;
  text-align: left;
}
</style>
