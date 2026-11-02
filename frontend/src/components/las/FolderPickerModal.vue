<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { ICONS } from '@/components/icons'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { LAS_STORAGE_FOLDERS, type LasStorageNode } from '@/stores/lasCatalog'

const props = defineProps<{
  visible: boolean
  /** 当前已填写的存储路径 */
  modelValue?: string
}>()

const emit = defineEmits<{
  close: []
  select: [path: string]
}>()

const keyword = ref('')
const expanded = ref<Set<string>>(new Set(['bucket1']))
const selected = ref<string>('')

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    keyword.value = ''
    // 已填写的路径（形如 oss://bucket1/tb1.lance/）回显为选中项
    const current = props.modelValue ?? ''
    selected.value = /^oss:\/\/.+\/.+/.test(current) ? current : ''
  },
)

/** 过滤后的桶 / 表列表 */
const tree = computed<LasStorageNode[]>(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return LAS_STORAGE_FOLDERS
  const result: LasStorageNode[] = []
  LAS_STORAGE_FOLDERS.forEach((bucket) => {
    const children = (bucket.children ?? []).filter((c) => c.name.toLowerCase().includes(q))
    const hit = bucket.name.toLowerCase().includes(q)
    if (!hit && !children.length) return
    result.push({ ...bucket, children: hit ? bucket.children : children })
  })
  return result
})

/** 当前选中的完整路径 */
const selectedPath = computed(() => {
  if (!selected.value) return ''
  return selected.value.startsWith('oss://') ? selected.value : `oss://${selected.value}`
})

function isOpen(name: string): boolean {
  return expanded.value.has(name)
}

function toggle(name: string) {
  const next = new Set(expanded.value)
  if (next.has(name)) next.delete(name)
  else next.add(name)
  expanded.value = next
}

function pick(path: string) {
  selected.value = path
}

function onConfirm() {
  if (!selectedPath.value) return
  emit('select', selectedPath.value)
  emit('close')
}
</script>

<template>
  <LasModal :visible="visible" title="选择文件夹" width="560px" @close="emit('close')">
    <div class="fp-tip">
      <SvgIcon :icon="LAS_ICONS.info" />
      <span>
        如果没有创建对象存储，可以在 OSS 控制台 已创建，创建完成后请刷新下方列表
      </span>
    </div>

    <div class="fp-search">
      <SvgIcon :icon="LAS_ICONS.search" class="fp-search-ic" />
      <input v-model="keyword" type="text" placeholder="请输入桶名称搜索">
    </div>

    <div class="fp-tree">
      <div v-for="bucket in tree" :key="bucket.name" class="fp-node">
        <div class="fp-row fp-l0" @click="toggle(bucket.name)">
          <button class="fp-arrow" type="button">
            <SvgIcon
              class="fp-arrow-ic"
              :class="{ open: isOpen(bucket.name) }"
              :icon="LAS_ICONS.chevronRight"
            />
          </button>
          <SvgIcon class="fp-ic" :icon="LAS_ICONS.db" />
          <span class="fp-name">{{ bucket.name }}</span>
          <span class="fp-tag">OSS</span>
        </div>

        <template v-if="isOpen(bucket.name)">
          <div
            v-for="child in bucket.children ?? []"
            :key="child.name"
            class="fp-row fp-l1"
            :class="{ 'is-on': selected === `${bucket.name}/${child.name}/` }"
            @click="pick(`${bucket.name}/${child.name}/`)"
          >
            <span class="fp-arrow" />
            <SvgIcon class="fp-ic" :icon="LAS_ICONS.asset" />
            <span class="fp-name">{{ child.name }}</span>
          </div>
        </template>
      </div>

      <div v-if="!tree.length" class="fp-empty">没有匹配的桶</div>
    </div>

    <div class="fp-selected">
      <span>当前选中：{{ selectedPath || '未选择' }}</span>
      <button v-if="selectedPath" class="fp-copy" type="button" title="复制路径">
        <SvgIcon :icon="ICONS.copy" />
      </button>
    </div>

    <template #footer>
      <button class="fp-btn" type="button" @click="emit('close')">取消</button>
      <button class="fp-btn is-primary" type="button" :disabled="!selectedPath" @click="onConfirm">
        确定
      </button>
    </template>
  </LasModal>
</template>

<style scoped>
.fp-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 12.5px;
  color: #86909c;
}

.fp-tip svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.fp-search {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px;
  margin-bottom: 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.fp-search:focus-within {
  border-color: #165dff;
}

.fp-search-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.fp-search input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.fp-tree {
  height: 280px;
  padding: 6px 0;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow-y: auto;
}

.fp-row {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding-right: 10px;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.fp-row:hover {
  background: #f7f8fa;
}

.fp-row.is-on {
  background: #e8f3ff;
  color: #165dff;
}

.fp-l0 {
  padding-left: 8px;
}

.fp-l1 {
  padding-left: 32px;
}

.fp-arrow {
  width: 16px;
  height: 16px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: none;
  color: #86909c;
}

.fp-arrow-ic {
  width: 12px;
  height: 12px;
  transition: transform 0.15s ease;
}

.fp-arrow-ic.open {
  transform: rotate(90deg);
}

.fp-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.fp-l0 .fp-ic {
  color: #165dff;
}

.fp-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fp-tag {
  padding: 0 6px;
  border-radius: 3px;
  background: #e8f3ff;
  font-size: 11px;
  color: #165dff;
}

.fp-empty {
  padding: 30px 0;
  text-align: center;
  font-size: 13px;
  color: #86909c;
}

.fp-selected {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12.5px;
  color: #4e5969;
}

.fp-copy {
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #86909c;
  cursor: pointer;
}

.fp-copy svg {
  width: 12px;
  height: 12px;
}

.fp-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #f6f8fa;
  font-size: 13px;
  color: #42464e;
  cursor: pointer;
}

.fp-btn:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}

.fp-btn.is-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.fp-btn.is-primary:hover:not(:disabled) {
  background: #0e42d2;
  color: #fff;
}

.fp-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
