<script setup lang="ts">
import SvgIcon from '@/components/SvgIcon.vue'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import type { DevPipelineDataset } from '@/mock/dev/datasets'

const props = withDefaults(
  defineProps<{
    visible: boolean
    /** 数据集列表（来源于数据集管理模块） */
    datasets: DevPipelineDataset[]
    /** 第一个算子接受的数据类型白名单 */
    acceptedTypes?: string[]
    /** 当前已选数据集 id */
    currentId?: string
  }>(),
  { acceptedTypes: () => [], currentId: '' },
)

const emit = defineEmits<{
  close: []
  select: [dataset: DevPipelineDataset]
}>()

/** 契约不接受的数据集类型灰置不可选 */
function disabledReason(item: DevPipelineDataset): string {
  if (!props.acceptedTypes.length) return ''
  if (props.acceptedTypes.includes(item.type)) return ''
  return `首算子不接受${item.type}类型数据`
}

function onPick(item: DevPipelineDataset) {
  if (disabledReason(item)) return
  emit('select', item)
}
</script>

<template>
  <LasModal :visible="visible" title="选择管道输入数据集" width="720px" @close="emit('close')">
    <div class="dp-tip">
      <SvgIcon :icon="LAS_ICONS.info" />
      <span>
        数据集来自「数据集管理」模块，仅列出首算子输入契约接受的类型；选定后数据类型由数据集决定。
      </span>
    </div>

    <div class="dp-filters">
      <span class="dp-filter-label">可接受类型</span>
      <span v-if="acceptedTypes.length" class="dp-chip is-ok">{{ acceptedTypes.join(' / ') }}</span>
      <span v-else class="dp-chip">不限</span>
    </div>

    <div class="dp-list">
      <button
        v-for="item in datasets"
        :key="item.id"
        class="dp-item"
        :class="{
          'is-disabled': !!disabledReason(item),
          'is-active': item.id === currentId,
        }"
        type="button"
        :disabled="!!disabledReason(item)"
        @click="onPick(item)"
      >
        <span class="dp-radio">
          <i v-if="item.id === currentId" />
        </span>
        <span class="dp-main">
          <span class="dp-name">{{ item.name }}</span>
          <span class="dp-cn">{{ item.cnName }}</span>
        </span>
        <span class="dp-type">{{ item.type }}</span>
        <span class="dp-lake">{{ item.lakeType }}</span>
        <span class="dp-meta">{{ item.format }} · {{ item.samples }} 条</span>
        <span v-if="disabledReason(item)" class="dp-reason">{{ disabledReason(item) }}</span>
      </button>
    </div>

    <template #footer>
      <button class="dp-btn" type="button" @click="emit('close')">取消</button>
    </template>
  </LasModal>
</template>

<style scoped>
.dp-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 8px 12px;
  border-radius: 4px;
  background: #e8f3ff;
  font-size: 12.5px;
  color: #165dff;
}

.dp-tip svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.dp-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12.5px;
  color: #4e5969;
}

.dp-chip {
  padding: 1px 8px;
  border-radius: 3px;
  background: #f2f3f5;
  font-size: 12px;
  color: #4e5969;
}

.dp-chip.is-ok {
  background: #f0fbf3;
  color: #00893a;
}

.dp-list {
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow: hidden;
}

.dp-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 14px;
  border: 0;
  border-bottom: 1px solid #f2f3f5;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  text-align: left;
  cursor: pointer;
}

.dp-item:last-child {
  border-bottom: none;
}

.dp-item:hover:not(:disabled) {
  background: #f7faff;
}

.dp-item.is-active {
  background: #f0f7ff;
}

.dp-item.is-disabled {
  color: #c9cdd4;
  background: #fafbfc;
  cursor: not-allowed;
}

.dp-radio {
  width: 14px;
  height: 14px;
  flex: none;
  display: grid;
  place-items: center;
  border: 1px solid #c9cdd4;
  border-radius: 50%;
  background: #fff;
}

.dp-item.is-active .dp-radio {
  border-color: #165dff;
}

.dp-radio i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #165dff;
}

.dp-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dp-name {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
}

.dp-cn {
  font-size: 11.5px;
  color: #86909c;
}

.dp-type,
.dp-lake {
  flex: none;
  width: 76px;
  font-size: 12.5px;
  color: #4e5969;
}

.dp-meta {
  flex: none;
  width: 150px;
  font-size: 12px;
  color: #86909c;
}

.dp-reason {
  flex: none;
  font-size: 11.5px;
  color: #f53f3f;
}

.dp-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #f6f8fa;
  font-size: 13px;
  color: #42464e;
  cursor: pointer;
}

.dp-btn:hover {
  border-color: #165dff;
  color: #165dff;
}
</style>
