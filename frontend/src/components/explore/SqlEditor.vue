<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { highlightSql } from '@/utils/sql'

const props = withDefaults(
  defineProps<{
    modelValue: string
    lastSavedAt: string
    autoSave: boolean
  }>(),
  { lastSavedAt: '', autoSave: true },
)

const emit = defineEmits<{ 'update:modelValue': [string]; save: [] }>()

const textarea = ref<HTMLTextAreaElement | null>(null)
const highlightLayer = ref<HTMLElement | null>(null)
const gutterLayer = ref<HTMLElement | null>(null)
const line = ref(1)
const col = ref(1)
const selectionCount = ref(0)

const highlighted = computed(() => highlightSql(props.modelValue))
const lineNumbers = computed(() => {
  const count = Math.max(1, props.modelValue.split('\n').length)
  return Array.from({ length: count }, (_, i) => i + 1)
})

function syncScroll() {
  const ta = textarea.value
  if (!ta) return
  if (highlightLayer.value) {
    highlightLayer.value.style.transform = `translate(${-ta.scrollLeft}px, ${-ta.scrollTop}px)`
  }
  if (gutterLayer.value) {
    gutterLayer.value.style.transform = `translateY(${-ta.scrollTop}px)`
  }
}

function updateCursor() {
  const ta = textarea.value
  if (!ta) return
  const upto = ta.value.slice(0, ta.selectionStart ?? 0)
  const parts = upto.split('\n')
  line.value = parts.length
  col.value = (parts[parts.length - 1]?.length ?? 0) + 1
  selectionCount.value = (ta.selectionEnd ?? 0) - (ta.selectionStart ?? 0)
}

function onInput(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
  updateCursor()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Tab') {
    event.preventDefault()
    const ta = textarea.value
    if (!ta) return
    const start = ta.selectionStart ?? 0
    const end = ta.selectionEnd ?? 0
    const value = `${ta.value.slice(0, start)}    ${ta.value.slice(end)}`
    emit('update:modelValue', value)
    nextTick(() => {
      ta.selectionStart = ta.selectionEnd = start + 4
      updateCursor()
    })
    return
  }
  if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
    event.preventDefault()
    emit('save')
  }
}

/** 在光标处插入文本 */
function insertText(text: string) {
  const ta = textarea.value
  if (!ta) return
  const start = ta.selectionStart ?? ta.value.length
  const end = ta.selectionEnd ?? start
  const value = `${ta.value.slice(0, start)}${text}${ta.value.slice(end)}`
  emit('update:modelValue', value)
  nextTick(() => {
    ta.focus()
    ta.selectionStart = ta.selectionEnd = start + text.length
    updateCursor()
  })
}

defineExpose({ insertText })

watch(() => props.modelValue, () => nextTick(updateCursor))
onMounted(() => nextTick(updateCursor))
</script>

<template>
  <div class="ex-editor-area">
    <div class="ex-editor">
      <div class="ex-gutter">
        <div ref="gutterLayer" class="ex-gutter-inner">
          <div v-for="n in lineNumbers" :key="n">{{ n }}</div>
        </div>
      </div>
      <div class="ex-code">
        <pre ref="highlightLayer" class="ex-highlight" aria-hidden="true"><code v-html="highlighted" /></pre>
        <textarea
          ref="textarea"
          :value="modelValue"
          spellcheck="false"
          autocomplete="off"
          autocapitalize="off"
          @input="onInput"
          @keydown="onKeydown"
          @scroll="syncScroll"
          @click="updateCursor"
          @keyup="updateCursor"
          @select="updateCursor"
        />
        <div class="ex-float-bar">
          <button type="button" title="选中字符数">{{ selectionCount }} S</button>
          <button type="button" title="上一处"><SvgIcon name="chevUp" /></button>
          <button type="button" title="下一处"><SvgIcon name="chevDown" /></button>
          <button type="button" title="保存（Ctrl+S）" @click="emit('save')"><SvgIcon name="save" /></button>
          <button type="button" title="更多"><SvgIcon name="chevDown" /></button>
        </div>
      </div>
    </div>

    <div class="ex-status">
      <span>最近保存: {{ lastSavedAt }}</span>
      <span>自动保存: {{ autoSave ? '开启' : '关闭' }}</span>
      <span style="margin-left: auto">Ln {{ line }}, Col {{ col }}</span>
      <span>Spaces: 4</span>
    </div>
  </div>
</template>
