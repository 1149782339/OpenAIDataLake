<script setup lang="ts">
import { ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import LasModal from '@/components/las/LasModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import type { LasDataset } from '@/stores/las'

const props = defineProps<{
  visible: boolean
  dataset: LasDataset | null
}>()

const emit = defineEmits<{
  close: []
  submit: [payload: { manageScope: string; manageUsers: string[]; readScope: string; readUsers: string[] }]
}>()

type Scope = '指定用户' | '组织内所有人'
const SCOPES: Scope[] = ['指定用户', '组织内所有人']

const manageScope = ref<Scope>('指定用户')
const manageUsers = ref('')
const readScope = ref<Scope>('指定用户')
const readUsers = ref('')

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    manageScope.value = '指定用户'
    manageUsers.value = ''
    readScope.value = '指定用户'
    readUsers.value = ''
  },
)

function parseUsers(value: string): string[] {
  return value
    .split(/[,，、\s]+/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function onConfirm() {
  emit('submit', {
    manageScope: manageScope.value,
    manageUsers: manageScope.value === '指定用户' ? parseUsers(manageUsers.value) : [],
    readScope: readScope.value,
    readUsers: readScope.value === '指定用户' ? parseUsers(readUsers.value) : [],
  })
  emit('close')
}
</script>

<template>
  <LasModal :visible="visible" title="数据集权限" width="600px" @close="emit('close')">
    <!-- 数据集 -->
    <div class="pm-section">
      <span class="pm-bar" />
      <span class="pm-section-title">数据集</span>
    </div>

    <div class="pm-row">
      <label class="pm-label">数据集英文名</label>
      <span class="pm-text">{{ dataset?.name ?? '-' }}</span>
    </div>

    <div class="pm-row">
      <label class="pm-label">数据集中文名</label>
      <span class="pm-text">{{ dataset?.cnName || '-' }}</span>
    </div>

    <!-- 权限控制 -->
    <div class="pm-section">
      <span class="pm-bar" />
      <span class="pm-section-title">权限控制</span>
    </div>

    <div class="pm-row">
      <label class="pm-label">Owner</label>
      <span class="pm-text">{{ dataset?.creator ?? '-' }}</span>
    </div>

    <div class="pm-row">
      <label class="pm-label">
        管理权限
        <SvgIcon class="pm-help" :icon="LAS_ICONS.question" />
      </label>
      <div class="pm-control">
        <div class="pm-segmented">
          <button
            v-for="item in SCOPES"
            :key="item"
            class="pm-seg"
            :class="{ on: manageScope === item }"
            type="button"
            @click="manageScope = item"
          >
            {{ item }}
          </button>
        </div>
        <div v-if="manageScope === '指定用户'" class="pm-input-wrap">
          <input v-model="manageUsers" type="text" class="pm-input" placeholder="请输入名称">
          <SvgIcon class="pm-input-ic" :icon="LAS_ICONS.search" />
        </div>
      </div>
    </div>

    <div class="pm-row">
      <label class="pm-label">
        只读权限
        <SvgIcon class="pm-help" :icon="LAS_ICONS.question" />
      </label>
      <div class="pm-control">
        <div class="pm-segmented">
          <button
            v-for="item in SCOPES"
            :key="item"
            class="pm-seg"
            :class="{ on: readScope === item }"
            type="button"
            @click="readScope = item"
          >
            {{ item }}
          </button>
        </div>
        <div v-if="readScope === '指定用户'" class="pm-input-wrap">
          <input v-model="readUsers" type="text" class="pm-input" placeholder="请输入名称">
          <SvgIcon class="pm-input-ic" :icon="LAS_ICONS.search" />
        </div>
      </div>
    </div>

    <template #footer>
      <button class="pm-btn" type="button" @click="emit('close')">取消</button>
      <button class="pm-btn is-primary" type="button" @click="onConfirm">配置</button>
    </template>
  </LasModal>
</template>

<style scoped>
.pm-section {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 0 16px;
}

.pm-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #165dff;
}

.pm-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.pm-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 18px;
}

.pm-label {
  flex: none;
  width: 116px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding-top: 6px;
  font-size: 13px;
  color: #4e5969;
}

.pm-help {
  width: 13px;
  height: 13px;
  color: #86909c;
}

.pm-text {
  flex: 1;
  min-width: 0;
  padding-top: 6px;
  font-size: 13px;
  color: #1d2129;
  word-break: break-all;
}

.pm-control {
  flex: 1;
  min-width: 0;
}

.pm-segmented {
  display: inline-flex;
  padding: 2px;
  border-radius: 4px;
  background: #f2f3f5;
}

.pm-seg {
  height: 28px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 3px;
  background: transparent;
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
}

.pm-seg.on {
  border-color: #165dff;
  background: #fff;
  color: #165dff;
}

.pm-input-wrap {
  position: relative;
  margin-top: 10px;
}

.pm-input {
  width: 100%;
  height: 32px;
  padding: 0 32px 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  box-sizing: border-box;
}

.pm-input:focus {
  border-color: #165dff;
}

.pm-input::placeholder {
  color: #86909c;
}

.pm-input-ic {
  position: absolute;
  top: 50%;
  right: 10px;
  width: 14px;
  height: 14px;
  transform: translateY(-50%);
  color: #86909c;
  pointer-events: none;
}

.pm-btn {
  height: 32px;
  padding: 0 18px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #f6f8fa;
  font-size: 13px;
  color: #42464e;
  cursor: pointer;
}

.pm-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.pm-btn.is-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.pm-btn.is-primary:hover {
  background: #0e42d2;
  color: #fff;
}
</style>
