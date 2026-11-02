<script setup lang="ts">
import { ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'

export interface OperatorColumnPayload {
  name: string
  type: string
}

export interface RegisterOperatorPayload {
  name: string
  inputs: OperatorColumnPayload[]
  outputs: OperatorColumnPayload[]
  runtime: string
}

const props = defineProps<{ open: boolean }>()

const emit = defineEmits<{
  close: []
  submit: [payload: RegisterOperatorPayload]
}>()

const FIELD_TYPES = [
  'string',
  'int',
  'bigint',
  'float',
  'double',
  'boolean',
  'timestamp',
  'binary',
  'vector<float32>[512]',
]

const name = ref('')
const runtime = ref('registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1')
const inputs = ref<OperatorColumnPayload[]>([{ name: '', type: '' }])
const outputs = ref<OperatorColumnPayload[]>([{ name: '', type: '' }])
const errors = ref<Record<string, string>>({})

watch(
  () => props.open,
  (v) => {
    if (!v) return
    name.value = ''
    runtime.value = 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1'
    inputs.value = [{ name: '', type: '' }]
    outputs.value = [{ name: '', type: '' }]
    errors.value = {}
  },
)

function addRow(list: OperatorColumnPayload[]) {
  list.push({ name: '', type: '' })
}

function removeRow(list: OperatorColumnPayload[], index: number) {
  list.splice(index, 1)
  if (!list.length) list.push({ name: '', type: '' })
}

function onConfirm() {
  const next: Record<string, string> = {}
  if (!name.value.trim()) next.name = '请输入算子名称'
  if (!runtime.value.trim()) next.runtime = '请输入运行环境镜像'
  if (!inputs.value.some((c) => c.name.trim() && c.type)) {
    next.inputs = '至少声明一组输入列（列名 + 类型）'
  }
  if (!outputs.value.some((c) => c.name.trim() && c.type)) {
    next.outputs = '至少声明一组输出列（列名 + 类型）'
  }
  errors.value = next
  if (Object.keys(next).length) return

  emit('submit', {
    name: name.value.trim(),
    runtime: runtime.value.trim(),
    inputs: inputs.value.filter((c) => c.name.trim() && c.type),
    outputs: outputs.value.filter((c) => c.name.trim() && c.type),
  })
}
</script>

<template>
  <LasModal :visible="open" title="注册算子" width="660px" @close="emit('close')">
    <div class="ro-tip">
      <SvgIcon :icon="LAS_ICONS.info" />
      <span>算子注册必须声明输入输出契约，工作流连线时将按契约逐列校验。</span>
    </div>

    <div class="ro-row">
      <label class="ro-label"><i class="ro-req">*</i>算子名称</label>
      <div class="ro-control">
        <input
          v-model="name"
          type="text"
          class="ro-input"
          :class="{ 'is-error': errors.name }"
          placeholder="由小写字母、数字、下划线组成"
          @input="delete errors.name"
        >
        <div v-if="errors.name" class="ro-error">{{ errors.name }}</div>
      </div>
    </div>

    <div class="ro-row">
      <label class="ro-label"><i class="ro-req">*</i>输入列定义</label>
      <div class="ro-control">
        <div v-for="(col, index) in inputs" :key="`in-${index}`" class="ro-cols">
          <input v-model="col.name" type="text" class="ro-input" placeholder="列名">
          <select v-model="col.type" class="ro-input ro-select">
            <option value="">类型</option>
            <option v-for="t in FIELD_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
          <button class="ro-trash" type="button" title="删除" @click="removeRow(inputs, index)">
            <SvgIcon :icon="LAS_ICONS.trash" />
          </button>
        </div>
        <button class="ro-link" type="button" @click="addRow(inputs)">
          <SvgIcon :icon="LAS_ICONS.plus" />
          <span>添加输入列</span>
        </button>
        <div v-if="errors.inputs" class="ro-error">{{ errors.inputs }}</div>
      </div>
    </div>

    <div class="ro-row">
      <label class="ro-label"><i class="ro-req">*</i>输出列定义</label>
      <div class="ro-control">
        <div v-for="(col, index) in outputs" :key="`out-${index}`" class="ro-cols">
          <input v-model="col.name" type="text" class="ro-input" placeholder="列名">
          <select v-model="col.type" class="ro-input ro-select">
            <option value="">类型</option>
            <option v-for="t in FIELD_TYPES" :key="t" :value="t">{{ t }}</option>
          </select>
          <button class="ro-trash" type="button" title="删除" @click="removeRow(outputs, index)">
            <SvgIcon :icon="LAS_ICONS.trash" />
          </button>
        </div>
        <button class="ro-link" type="button" @click="addRow(outputs)">
          <SvgIcon :icon="LAS_ICONS.plus" />
          <span>添加输出列</span>
        </button>
        <div v-if="errors.outputs" class="ro-error">{{ errors.outputs }}</div>
      </div>
    </div>

    <div class="ro-row">
      <label class="ro-label"><i class="ro-req">*</i>运行环境镜像</label>
      <div class="ro-control">
        <input
          v-model="runtime"
          type="text"
          class="ro-input"
          :class="{ 'is-error': errors.runtime }"
          placeholder="registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1"
          @input="delete errors.runtime"
        >
        <p class="ro-hint">算子将在该镜像环境中执行，镜像需包含算子依赖的 Python 运行时。</p>
        <div v-if="errors.runtime" class="ro-error">{{ errors.runtime }}</div>
      </div>
    </div>

    <template #footer>
      <button class="ro-btn ro-btn-ghost" type="button" @click="emit('close')">取消</button>
      <button class="ro-btn ro-btn-primary" type="button" @click="onConfirm">确认注册</button>
    </template>
  </LasModal>
</template>

<style scoped>
.ro-tip {
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

.ro-tip svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.ro-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.ro-label {
  flex: none;
  width: 110px;
  padding-top: 7px;
  padding-right: 20px;
  box-sizing: border-box;
  text-align: right;
  font-size: 13px;
  color: #1d2129;
}

.ro-req {
  margin-right: 4px;
  font-style: normal;
  color: #f53f3f;
}

.ro-control {
  flex: 1;
  min-width: 0;
}

.ro-input {
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

.ro-input:focus {
  border-color: #165dff;
}

.ro-input.is-error {
  border-color: #f53f3f;
}

.ro-input::placeholder {
  color: #86909c;
}

.ro-select {
  appearance: none;
  cursor: pointer;
  padding-right: 24px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.ro-cols {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.ro-cols .ro-input:first-child {
  flex: 1;
}

.ro-cols .ro-select {
  width: 150px;
  flex: none;
}

.ro-trash {
  flex: none;
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #165dff;
  cursor: pointer;
}

.ro-trash svg {
  width: 15px;
  height: 15px;
}

.ro-trash:hover {
  color: #0e42d2;
}

.ro-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.ro-link:hover {
  text-decoration: underline;
}

.ro-link svg {
  width: 13px;
  height: 13px;
}

.ro-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

.ro-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f53f3f;
}

.ro-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.ro-btn-ghost {
  border: 1px solid #dde2e9;
  background: #f6f8fa;
  color: #42464e;
}

.ro-btn-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}

.ro-btn-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.ro-btn-primary:hover {
  background: #0e42d2;
}
</style>
