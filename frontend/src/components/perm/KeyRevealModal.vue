<script setup lang="ts">
import BaseModal from '@/components/BaseModal.vue'
import CopyButton from '@/components/CopyButton.vue'

defineProps<{
  open: boolean
  name: string
  value: string
}>()

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <BaseModal id="keyRevealModal" :open="open" title="API Key 创建成功" aria-label="API Key 创建成功" @close="emit('close')">
    <p class="modal-sub">API Key <b>{{ name }}</b> 已创建，请复制并妥善保存。</p>
    <div class="key-reveal">
      <span>{{ value }}</span>
      <CopyButton :value="value" title="复制 API Key" />
    </div>

    <template #footer>
      <div class="modal-foot">
        <button class="btn-primary" type="button" @click="emit('close')">完成</button>
      </div>
    </template>
  </BaseModal>
</template>
