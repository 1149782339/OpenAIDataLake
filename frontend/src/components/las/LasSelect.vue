<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'

const props = withDefaults(
  defineProps<{
    modelValue: string
    options: string[]
    placeholder?: string
    width?: string
    clearable?: boolean
  }>(),
  { placeholder: '请选择', width: '400px', clearable: true },
)

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

function toggle() {
  open.value = !open.value
}

function pick(value: string) {
  emit('update:modelValue', value)
  open.value = false
}

function clear(event: MouseEvent) {
  event.stopPropagation()
  emit('update:modelValue', '')
}

function onDocClick(event: MouseEvent) {
  if (!root.value) return
  if (!root.value.contains(event.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('mousedown', onDocClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))

watch(
  () => props.modelValue,
  () => {
    if (props.modelValue) open.value = false
  },
)
</script>

<template>
  <div ref="root" class="ls-select" :style="{ width }">
    <div class="ls-select-view" @click="toggle">
      <span class="ls-select-value" :class="{ 'is-placeholder': !modelValue }">
        {{ modelValue || placeholder }}
      </span>
      <span class="ls-select-suffix">
        <button v-if="clearable && modelValue" class="ls-select-clear" type="button" title="清除" @click="clear">
          <SvgIcon :icon="LAS_ICONS.close" />
        </button>
        <SvgIcon class="ls-select-arrow" :icon="LAS_ICONS.chevronDown" />
      </span>
    </div>

    <ul v-if="open" class="ls-select-options">
      <li v-for="opt in options" :key="opt" class="ls-select-option" @click="pick(opt)">
        {{ opt }}
      </li>
      <li v-if="!options.length" class="ls-select-option is-empty">暂无可选项</li>
    </ul>
  </div>
</template>

<style scoped>
.ls-select {
  position: relative;
  display: inline-block;
  vertical-align: top;
}

.ls-select-view {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 11px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #0c0d0e;
  cursor: pointer;
}

.ls-select-view:hover {
  border-color: #9cc6fa;
}

.ls-select-value {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: #0c0d0e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ls-select-value.is-placeholder {
  color: #86909c;
}

.ls-select-suffix {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: none;
}

.ls-select-clear {
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #86909c;
  cursor: pointer;
}

.ls-select-clear svg {
  width: 11px;
  height: 11px;
}

.ls-select-clear:hover {
  color: #0c0d0e;
}

.ls-select-arrow {
  width: 12px;
  height: 12px;
  color: #86909c;
}

.ls-select-options {
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

.ls-select-option {
  padding: 6px 12px;
  font-size: 13px;
  color: #0c0d0e;
  cursor: pointer;
}

.ls-select-option:hover {
  background: #f2f3f5;
}

.ls-select-option.is-empty {
  color: #86909c;
  cursor: default;
}

.ls-select-option.is-empty:hover {
  background: none;
}
</style>
