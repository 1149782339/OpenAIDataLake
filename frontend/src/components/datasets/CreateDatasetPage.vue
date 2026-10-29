<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { useDatasetStore } from '@/stores/datasets'
import { useUiStore } from '@/stores/ui'

const props = withDefaults(
  defineProps<{
    open: boolean
    prefill?: { name?: string; cnName?: string; ossPath?: string; samples?: number; size?: string } | null
  }>(),
  { prefill: null },
)

const emit = defineEmits<{ close: []; created: [] }>()

const store = useDatasetStore()
const ui = useUiStore()

const name = ref('')
const cnName = ref('')
const ossPath = ref('')
const nameInvalid = ref(false)
const ossInvalid = ref(false)
const nameInput = ref<HTMLInputElement | null>(null)

function makeDatasetName(): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const d = new Date()
  return (
    `las_dataset_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}` +
    `_${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
  )
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    name.value = props.prefill?.name ?? makeDatasetName()
    cnName.value = props.prefill?.cnName ?? ''
    ossPath.value = props.prefill?.ossPath ?? ''
    nameInvalid.value = false
    ossInvalid.value = false
    nextTick(() => {
      nameInput.value?.focus()
      nameInput.value?.select()
    })
  },
)

const ossHint = computed(() => name.value.trim() || makeDatasetName())

function pickOssPath() {
  ossPath.value = `oss://lance-demo/datasets/${ossHint.value}.lance`
  ossInvalid.value = false
  ui.toast('已选择 OSS 存储位置（演示）')
}

function submit() {
  const datasetName = name.value.trim()
  const path = ossPath.value.trim()
  let valid = true

  if (!datasetName) {
    nameInvalid.value = true
    valid = false
  } else {
    nameInvalid.value = false
  }
  if (!path) {
    ossInvalid.value = true
    valid = false
  } else {
    ossInvalid.value = false
  }
  if (!valid) return

  const samples = props.prefill?.samples ?? 100 + Math.floor(Math.random() * 9000)
  const size = props.prefill?.size ?? `${(Math.random() * 240 + 1.2).toFixed(2)} MB`

  store.addDataset({
    name: datasetName,
    desc: cnName.value.trim() || '暂无描述',
    format: 'Lance',
    samples,
    size,
    ossPath: path,
    sourceTableId: null,
  })

  store.setQuery('')
  store.page = 1
  emit('close')
  emit('created')
  ui.toast('数据集创建成功（演示）')
}
</script>

<template>
  <div v-if="open" id="createPage" class="create-page">
    <header class="create-head">
      <button class="back-btn" type="button" aria-label="返回" @click="emit('close')">
        <SvgIcon name="back" />
      </button>
      <h1>创建数据集</h1>
    </header>

    <div class="create-body">
      <div class="create-form">
        <div class="section-title">基本信息</div>

        <div class="create-field">
          <label for="newName">数据集英文名称<span class="req">*</span></label>
          <div class="field-control">
            <input
              id="newName"
              ref="nameInput"
              v-model="name"
              type="text"
              placeholder="请输入数据集英文名称"
              autocomplete="off"
              :class="{ invalid: nameInvalid }"
              @input="nameInvalid = false"
              @keydown.enter="submit"
            >
            <p class="field-error" :class="{ show: nameInvalid }">请输入数据集英文名称</p>
          </div>
        </div>

        <div class="create-field">
          <label for="newCnName">数据集中文名称</label>
          <div class="field-control">
            <input
              id="newCnName"
              v-model="cnName"
              type="text"
              placeholder="请输入"
              autocomplete="off"
              @keydown.enter="submit"
            >
          </div>
        </div>

        <div class="create-field">
          <label>数据来源<span class="req">*</span></label>
          <div class="field-control">
            <div class="seg-row">
              <span class="seg-btn active">对象存储 OSS</span>
            </div>
            <div class="oss-row">
              <input
                id="newOssPath"
                v-model="ossPath"
                type="text"
                placeholder="请填写 OSS 存储位置，例如 oss://bucket/path"
                autocomplete="off"
                :class="{ invalid: ossInvalid }"
                @input="ossInvalid = false"
                @keydown.enter="submit"
              >
              <button class="btn-ghost" type="button" @click="pickOssPath">+ 从 OSS 选择</button>
            </div>
            <p class="field-error" :class="{ show: ossInvalid }">请填写 OSS 存储位置</p>
          </div>
        </div>

        <div class="create-field">
          <label>数据集格式</label>
          <div class="field-control">
            <div class="seg-row">
              <span class="seg-btn active">Lance</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <footer class="create-foot">
      <button id="createSubmit" class="btn-primary" type="button" @click="submit">创建</button>
      <button id="createCancel" class="btn-ghost" type="button" @click="emit('close')">取消</button>
    </footer>
  </div>
</template>
