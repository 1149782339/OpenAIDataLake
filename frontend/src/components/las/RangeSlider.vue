<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  marks: readonly string[]
  modelValue: [number, number]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: [number, number]): void
}>()

const max = computed(() => props.marks.length - 1)

function onInput(which: 0 | 1, event: Event) {
  const raw = Number((event.target as HTMLInputElement).value)
  const next: [number, number] = [props.modelValue[0], props.modelValue[1]]
  next[which] = raw
  if (next[0] > next[1]) {
    if (which === 0) next[1] = next[0]
    else next[0] = next[1]
  }
  emit('update:modelValue', next)
}

const leftPct = computed(() => (props.modelValue[0] / max.value) * 100)
const rightPct = computed(() => (props.modelValue[1] / max.value) * 100)
</script>

<template>
  <div class="range">
    <div class="range-track">
      <div class="range-fill" :style="{ left: `${leftPct}%`, right: `${100 - rightPct}%` }" />
    </div>
    <input
      class="range-input"
      type="range"
      min="0"
      :max="max"
      :value="modelValue[0]"
      @input="onInput(0, $event)"
    >
    <input
      class="range-input"
      type="range"
      min="0"
      :max="max"
      :value="modelValue[1]"
      @input="onInput(1, $event)"
    >
    <div class="range-marks">
      <span v-for="mark in marks" :key="mark" class="range-mark">{{ mark }}</span>
    </div>
  </div>
</template>

<style scoped>
.range {
  position: relative;
  padding-top: 2px;
}

.range-track {
  position: absolute;
  top: 8px;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 2px;
  background: #e5e6eb;
}

.range-fill {
  position: absolute;
  top: 0;
  height: 4px;
  border-radius: 2px;
  background: var(--las-blue, #165dff);
}

.range-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 20px;
  margin: 0;
  padding: 0;
  background: none;
  pointer-events: none;
  -webkit-appearance: none;
  appearance: none;
}

.range-input::-webkit-slider-runnable-track {
  height: 4px;
  background: transparent;
}

.range-input::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  margin-top: -4px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--las-blue, #165dff);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.16);
  cursor: pointer;
  pointer-events: auto;
}

.range-input::-moz-range-track {
  height: 4px;
  background: transparent;
}

.range-input::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--las-blue, #165dff);
  cursor: pointer;
  pointer-events: auto;
}

.range-marks {
  display: flex;
  justify-content: space-between;
  margin-top: 22px;
}

.range-mark {
  color: #86909c;
  font-size: 11px;
  white-space: nowrap;
  transform: translateX(-50%);
}

.range-marks span:first-child {
  transform: none;
}

.range-marks span:last-child {
  transform: translateX(0);
}
</style>
