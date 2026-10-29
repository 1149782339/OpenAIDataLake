<script setup lang="ts">
import SvgIcon from './SvgIcon.vue'
import { useUiStore } from '@/stores/ui'
import { copyToClipboard } from '@/utils/format'

const props = withDefaults(defineProps<{ value: string; title?: string }>(), {
  title: '复制',
})

const ui = useUiStore()

async function handleCopy() {
  try {
    await copyToClipboard(props.value)
    ui.toast('已复制到剪贴板')
  } catch {
    ui.toast('复制失败，请手动复制')
  }
}
</script>

<template>
  <button
    class="copy-btn"
    type="button"
    :title="title"
    :aria-label="title"
    @click.stop="handleCopy"
  >
    <SvgIcon name="copy" />
  </button>
</template>
