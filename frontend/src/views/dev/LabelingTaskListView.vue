<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import type { DevLabelTask, DevLabelTaskStatus } from '@/mock/dev/labeling'
import { useDevLabelingStore } from '@/stores/devLabeling'

const router = useRouter()
const store = useDevLabelingStore()

const statusFilter = ref<'' | DevLabelTaskStatus>('')
const pipelineFilter = ref('')

const STATUS_OPTIONS: DevLabelTaskStatus[] = ['待处理', '进行中', '已完成', '已驳回']

const pipelineOptions = computed(() => {
  const set = new Set<string>()
  store.taskList.forEach((t) => set.add(t.pipelineName))
  return [...set]
})

const rows = computed(() => {
  const kw = pipelineFilter.value.trim().toLowerCase()
  return store.taskList.filter((task) => {
    const matchStatus = !statusFilter.value || task.status === statusFilter.value
    const matchPipeline = !kw || task.pipelineName.toLowerCase().includes(kw)
    return matchStatus && matchPipeline
  })
})

function remainingOf(task: DevLabelTask): number {
  return task.items.filter((item) => !item.reviewAction).length
}

function progressOf(task: DevLabelTask): number {
  if (!task.items.length) return 0
  return Math.round(((task.items.length - remainingOf(task)) / task.items.length) * 100)
}

function statusClass(status: DevLabelTaskStatus): string {
  if (status === '进行中') return 'is-running'
  if (status === '已完成') return 'is-done'
  if (status === '已驳回') return 'is-rejected'
  return 'is-wait'
}

function onEnter(task: DevLabelTask) {
  router.push(`/dev/labeling/${task.id}`)
}

function onView(task: DevLabelTask) {
  router.push(`/dev/labeling/${task.id}`)
}
</script>

<template>
  <div class="lb-page">
    <header class="lb-head">
      <div class="lb-head-left">
        <span class="lb-title">数据标注</span>
        <span class="lb-sub">
          人工标注工作台 · 任务由管道内标注算子生成，每个运行实例一个任务
        </span>
      </div>
    </header>

    <div class="lb-body">
      <div class="lb-toolbar">
        <div class="lb-field">
          <label>状态</label>
          <select v-model="statusFilter" class="lb-select">
            <option value="">全部状态</option>
            <option v-for="item in STATUS_OPTIONS" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <div class="lb-field">
          <label>管道</label>
          <select v-model="pipelineFilter" class="lb-select">
            <option value="">全部管道</option>
            <option v-for="item in pipelineOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <div class="lb-search">
          <SvgIcon :icon="LAS_ICONS.search" class="lb-search-ic" />
          <input v-model="pipelineFilter" type="text" placeholder="按管道名筛选">
        </div>
      </div>

      <div class="lb-table-wrap">
        <table class="lb-table">
          <thead>
            <tr>
              <th style="width: 15%">任务ID</th>
              <th style="width: 18%">任务名称</th>
              <th style="width: 20%">来源</th>
              <th style="width: 16%">待确认张数</th>
              <th style="width: 9%">状态</th>
              <th style="width: 13%">创建时间</th>
              <th style="width: 9%">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in rows" :key="task.id">
              <td class="lb-mono">{{ task.id }}</td>
              <td>
                <button class="lb-link" type="button" @click="onEnter(task)">
                  {{ task.name }}
                </button>
              </td>
              <td class="lb-source">
                {{ task.pipelineName }} {{ task.versionName }} · run#{{ task.runNo }}
              </td>
              <td>
                <div class="lb-progress">
                  <div class="lb-progress-head">
                    <span class="lb-progress-num">
                      剩余 {{ remainingOf(task) }} / {{ task.items.length }}
                    </span>
                    <span class="lb-progress-pct">{{ progressOf(task) }}%</span>
                  </div>
                  <div class="lb-bar">
                    <i class="lb-bar-in" :style="{ width: `${progressOf(task)}%` }" />
                  </div>
                </div>
              </td>
              <td>
                <span class="lb-status" :class="statusClass(task.status)">{{ task.status }}</span>
              </td>
              <td class="lb-mono">{{ task.createdAt }}</td>
              <td>
                <div class="lb-ops">
                  <button
                    v-if="task.status !== '已完成'"
                    class="lb-op-btn"
                    type="button"
                    @click="onEnter(task)"
                  >
                    进入标注
                  </button>
                  <button class="lb-op-btn" type="button" @click="onView(task)">查看</button>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="lb-empty" colspan="7">暂无标注任务</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.lb-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

.lb-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.lb-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.lb-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.lb-sub {
  font-size: 13px;
  color: #86909c;
}

.lb-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

.lb-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.lb-field {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.lb-field label {
  font-size: 13px;
  color: #4e5969;
}

.lb-select {
  height: 32px;
  padding: 0 28px 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  appearance: none;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.lb-select:focus {
  border-color: #165dff;
}

.lb-search {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 220px;
  height: 32px;
  margin-left: auto;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.lb-search:focus-within {
  border-color: #165dff;
}

.lb-search-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.lb-search input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.lb-table-wrap {
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow: hidden;
}

.lb-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.lb-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.lb-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.lb-table tbody tr:last-child td {
  border-bottom: none;
}

.lb-table tbody tr:hover td {
  background: #f7f8fa;
}

.lb-mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
  color: #4e5969;
  white-space: nowrap;
}

.lb-link {
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.lb-link:hover {
  text-decoration: underline;
}

.lb-source {
  font-size: 12.5px;
  color: #4e5969;
  white-space: nowrap;
}

/* ---------- 进度 ---------- */
.lb-progress {
  min-width: 130px;
}

.lb-progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 5px;
}

.lb-progress-num {
  font-size: 12.5px;
  color: #1d2129;
}

.lb-progress-pct {
  font-size: 11.5px;
  color: #86909c;
}

.lb-bar {
  height: 5px;
  border-radius: 3px;
  background: #f2f3f5;
  overflow: hidden;
}

.lb-bar-in {
  display: block;
  height: 100%;
  border-radius: 3px;
  background: #165dff;
}

/* ---------- 状态 ---------- */
.lb-status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 11px;
  font-size: 12px;
  white-space: nowrap;
}

.lb-status.is-wait {
  background: #f2f3f5;
  color: #86909c;
}

.lb-status.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.lb-status.is-done {
  background: #f0fbf3;
  color: #00893a;
}

.lb-status.is-rejected {
  background: #fff1f0;
  color: #d9363e;
}

/* ---------- 操作 ---------- */
.lb-ops {
  display: flex;
  align-items: center;
  gap: 14px;
}

.lb-op-btn {
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
  white-space: nowrap;
}

.lb-op-btn:hover {
  text-decoration: underline;
}

.lb-empty {
  height: 90px;
  text-align: center;
  color: #86909c;
}
</style>
