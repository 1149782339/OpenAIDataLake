<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { usePermissionStore } from '@/stores/permissions'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: []; created: [name: string] }>()

const perm = usePermissionStore()

const name = ref('')
const invalid = ref(false)
const errorText = ref('请输入 API Key 名称')
const nameInput = ref<HTMLInputElement | null>(null)

watch(
  () => props.open,
  (open) => {
    if (!open) return
    name.value = ''
    invalid.value = false
    errorText.value = '请输入 API Key 名称'
    nextTick(() => nameInput.value?.focus())
  },
)

function submit() {
  const value = name.value.trim()
  if (!value) {
    invalid.value = true
    errorText.value = '请输入 API Key 名称'
    nameInput.value?.focus()
    return
  }
  if (perm.keys.some((key) => key.name === value)) {
    invalid.value = true
    errorText.value = '该名称已存在，请更换'
    nameInput.value?.focus()
    return
  }
  emit('created', value)
}
</script>

<template>
  <BaseModal id="createKeyModal" :open="open" title="创建 API Key" aria-label="创建 API Key" @close="emit('close')">
    <div class="form-row">
      <label for="keyName">名称<span class="req">*</span></label>
      <input
        id="keyName"
        ref="nameInput"
        v-model="name"
        type="text"
        placeholder="例如：数据导入服务"
        autocomplete="off"
        :class="{ invalid }"
        @input="invalid = false"
        @keydown.enter="submit"
      >
      <p class="field-error" :class="{ show: invalid }">{{ errorText }}</p>
      <p class="form-hint">名称用于区分调用方，创建后不可修改。</p>
    </div>

    <template #footer>
      <div class="modal-foot">
        <button class="btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="btn-primary" type="button" @click="submit">创建</button>
      </div>
    </template>
  </BaseModal>
</template>
