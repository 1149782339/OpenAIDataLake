<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title?: string
    /** '' | 'db-modal' | 'wide' */
    variant?: '' | 'db-modal' | 'wide'
    ariaLabel?: string
    id?: string
  }>(),
  { title: '', variant: '', ariaLabel: '', id: undefined },
)

const emit = defineEmits<{ close: [] }>()

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.open) emit('close')
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div :id="id" class="modal-mask" :class="{ show: open }" @mousedown.self="emit('close')">
    <div
      class="modal"
      :class="variant"
      role="dialog"
      aria-modal="true"
      :aria-label="ariaLabel || title"
    >
      <div class="modal-head">
        <h3>{{ title }}</h3>
        <button class="icon-close" type="button" aria-label="关闭" @click="emit('close')">×</button>
      </div>
      <slot />
      <slot name="footer" />
    </div>
  </div>
</template>
