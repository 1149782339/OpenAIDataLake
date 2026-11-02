<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import OperatorParamsPanel from '@/components/dev/OperatorParamsPanel.vue'
import WorkflowCanvas from '@/components/dev/WorkflowCanvas.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { findPipelineDataset } from '@/mock/dev/datasets'
import { getOperator } from '@/mock/dev/operators'
import { useDevWorkflowStore } from '@/stores/devWorkflow'
import { useUiStore } from '@/stores/ui'

const route = useRoute()
const router = useRouter()
const store = useDevWorkflowStore()
const ui = useUiStore()

const tab = ref<'runs' | 'versions'>('runs')
const selectedVersionId = ref<string>('')
const selectedNodeId = ref<string | null>(null)

const workflowId = computed(() => String(route.params.id ?? ''))
const workflow = computed(() => store.byId(workflowId.value))
const currentVersion = computed(() =>
  workflow.value ? store.currentVersion(workflow.value) : undefined,
)

/** 正在查看的版本（默认当前版本） */
const viewingVersion = computed(() => {
  const list = workflow.value?.versions ?? []
  return list.find((v) => v.id === selectedVersionId.value) ?? currentVersion.value
})

const isDraft = computed(() => viewingVersion.value?.status === '草稿')
const frozen = computed(() => {
  const v = viewingVersion.value
  if (!v) return true
  if (v.status === '已发布') return true
  return store.runsOfVersion(workflowId.value, v.id).length > 0
})

const runs = computed(() => store.runsOfWorkflow(workflowId.value))

/** 管道输入绑定的数据集 */
const inputDataset = computed(() => findPipelineDataset(viewingVersion.value?.inputDatasetId))

/** 首算子接受的数据类型（数据类型下拉的候选项） */
const dataTypeOptions = computed(() => {
  const node = viewingVersion.value?.nodes[0]
  const op = node ? getOperator(node.opId) : undefined
  return (op?.accepts ?? []).map((t) => ({ value: t, label: t }))
})

const selectedParams = computed(() => {
  const v = viewingVersion.value
  const id = selectedNodeId.value
  if (!v || !id) return {}
  return v.params[id] ?? {}
})

const selectedOperatorId = computed(() => {
  const v = viewingVersion.value
  if (!v) return null
  const node = v.nodes.find((n) => n.id === selectedNodeId.value)
  return node?.opId ?? null
})

watch(
  () => route.params.id,
  () => {
    const v = currentVersion.value
    selectedVersionId.value = v?.id ?? ''
    selectedNodeId.value = v?.nodes[0]?.id ?? null
  },
  { immediate: true },
)

function selectVersion(id: string) {
  selectedVersionId.value = id
  const v = workflow.value?.versions.find((item) => item.id === id)
  selectedNodeId.value = v?.nodes[0]?.id ?? null
}

function onResume(runId: string) {
  if (store.resumeRun(runId)) ui.toast('运行实例已恢复')
}

function onEdit() {
  router.push({ path: '/dev/workflow/create', query: { id: workflowId.value } })
}

function onBack() {
  router.push('/dev/workflow')
}

function versionRuns(versionId: string): number {
  return store.runsOfVersion(workflowId.value, versionId).length
}

function statusClass(status: string): string {
  if (status === '运行中') return 'is-running'
  if (status === '挂起') return 'is-suspended'
  if (status === '成功') return 'is-success'
  return 'is-failed'
}
</script>

<template>
  <div class="wd-page">
    <header class="wd-head">
      <button class="wd-back" type="button" title="返回工作流列表" @click="onBack">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="wd-title">{{ workflow?.name ?? '工作流详情' }}</span>
      <span class="wd-desc">{{ workflow?.description }}</span>
      <div class="wd-head-right">
        <button v-if="isDraft" class="wd-btn" type="button" @click="onEdit">
          <SvgIcon :icon="LAS_ICONS.doc" />
          <span>编辑草稿</span>
        </button>
        <button class="wd-btn" type="button" @click="onBack">返回列表</button>
      </div>
    </header>

    <div class="wd-status-bar" :class="isDraft ? 'is-draft' : 'is-published'">
      <SvgIcon :icon="isDraft ? LAS_ICONS.doc : LAS_ICONS.check" />
      <span v-if="isDraft">草稿（可编辑）</span>
      <span v-else>{{ viewingVersion?.version }} 已发布（只读）</span>
      <span v-if="frozen && isDraft" class="wd-lock">该版本已有运行记录，只读</span>
      <span class="wd-status-tip">
        发布即冻结：已发布版本或已有运行实例的版本，画布与参数只读，修改需创建新版本
      </span>
    </div>

    <div class="wd-canvas-row">
      <WorkflowCanvas
        v-model:selected-id="selectedNodeId"
        :nodes="viewingVersion?.nodes ?? []"
        :edges="viewingVersion?.edges ?? []"
        :input-dataset="inputDataset ? { name: inputDataset.name, type: inputDataset.type } : null"
        readonly
        :hint="`${viewingVersion?.version ?? ''} 版本画布快照（只读）`"
      />
      <OperatorParamsPanel
        :op-id="selectedOperatorId"
        :params="selectedParams"
        :data-type="viewingVersion?.dataType ?? ''"
        :data-type-locked="Boolean(viewingVersion?.inputDatasetId)"
        :data-type-options="dataTypeOptions"
        readonly
      />
    </div>

    <section class="wd-tabs">
      <div class="wd-tab-bar">
        <button
          class="wd-tab"
          :class="{ active: tab === 'runs' }"
          type="button"
          @click="tab = 'runs'"
        >
          运行实例
          <span class="wd-tab-count">{{ runs.length }}</span>
        </button>
        <button
          class="wd-tab"
          :class="{ active: tab === 'versions' }"
          type="button"
          @click="tab = 'versions'"
        >
          版本历史
          <span class="wd-tab-count">{{ workflow?.versions.length ?? 0 }}</span>
        </button>
      </div>

      <div v-if="tab === 'runs'" class="wd-tab-body">
        <table class="wd-table">
          <thead>
            <tr>
              <th style="width: 190px">运行 ID</th>
              <th style="width: 90px">版本</th>
              <th style="width: 100px">状态</th>
              <th style="width: 170px">开始时间</th>
              <th style="width: 130px">耗时</th>
              <th style="width: 130px">当前算子</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in runs" :key="run.id">
              <td class="wd-mono">{{ run.id }}</td>
              <td class="wd-mono">
                {{ workflow?.versions.find((v) => v.id === run.versionId)?.version ?? '-' }}
              </td>
              <td>
                <span class="wd-status" :class="statusClass(run.status)">{{ run.status }}</span>
              </td>
              <td class="wd-mono">{{ run.startTime }}</td>
              <td>{{ run.duration }}</td>
              <td>{{ run.currentOp }}</td>
              <td>
                <button
                  v-if="run.status === '挂起'"
                  class="wd-op-btn"
                  type="button"
                  @click="onResume(run.id)"
                >
                  恢复
                </button>
                <span v-else class="wd-muted">-</span>
              </td>
            </tr>
            <tr v-if="!runs.length">
              <td class="wd-empty" colspan="7">暂无运行实例</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="wd-tab-body">
        <table class="wd-table">
          <thead>
            <tr>
              <th style="width: 110px">版本</th>
              <th style="width: 120px">状态</th>
              <th style="width: 170px">发布时间</th>
              <th style="width: 120px">运行次数</th>
              <th>调度</th>
              <th style="width: 130px">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="v in workflow?.versions ?? []"
              :key="v.id"
              :class="{ 'is-viewing': v.id === viewingVersion?.id }"
            >
              <td class="wd-mono">{{ v.version }}</td>
              <td>
                <span class="wd-status" :class="v.status === '已发布' ? 'is-published' : 'is-draft'">
                  {{ v.status }}
                </span>
              </td>
              <td class="wd-mono">{{ v.publishedAt || '-' }}</td>
              <td>{{ versionRuns(v.id) }}</td>
              <td>{{ v.schedule }}</td>
              <td>
                <button class="wd-op-btn" type="button" @click="selectVersion(v.id)">
                  查看快照
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wd-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1180px;
  background: #fff;
}

.wd-head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.wd-back {
  width: 26px;
  height: 26px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.wd-back:hover {
  color: #165dff;
  background: #f2f3f5;
}

.wd-back svg {
  width: 16px;
  height: 16px;
}

.wd-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.wd-desc {
  font-size: 13px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wd-head-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex: none;
}

.wd-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.wd-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.wd-btn svg {
  width: 13px;
  height: 13px;
}

/* ---------- 状态条 ---------- */
.wd-status-bar {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 20px;
  font-size: 13px;
}

.wd-status-bar svg {
  width: 14px;
  height: 14px;
  flex: none;
}

.wd-status-bar.is-draft {
  background: #fff7e8;
  color: #d9762c;
}

.wd-status-bar.is-published {
  background: #e8f3ff;
  color: #165dff;
}

.wd-status-tip {
  margin-left: auto;
  font-size: 12px;
  color: #86909c;
}

.wd-lock {
  padding: 1px 8px;
  border-radius: 3px;
  background: rgba(29, 33, 41, 0.06);
  font-size: 12px;
  color: #4e5969;
}

.wd-canvas-row {
  flex: 1;
  min-height: 260px;
  display: flex;
  overflow: hidden;
  border-bottom: 1px solid #f2f3f5;
}

/* ---------- Tab ---------- */
.wd-tabs {
  flex: none;
  height: 280px;
  display: flex;
  flex-direction: column;
}

.wd-tab-bar {
  flex: none;
  display: flex;
  align-items: stretch;
  gap: 24px;
  height: 42px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.wd-tab {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
}

.wd-tab.active {
  color: #165dff;
  font-weight: 500;
}

.wd-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #165dff;
}

.wd-tab-count {
  padding: 0 6px;
  border-radius: 8px;
  background: #f2f3f5;
  font-size: 11.5px;
  color: #86909c;
}

.wd-tab-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.wd-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.wd-table th {
  height: 38px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.wd-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.wd-table tbody tr:hover td {
  background: #f7f8fa;
}

.wd-table tbody tr.is-viewing td {
  background: #f0f7ff;
}

.wd-mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
  color: #4e5969;
  white-space: nowrap;
}

.wd-status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 11px;
  font-size: 12px;
}

.wd-status.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.wd-status.is-suspended {
  background: #fff7e8;
  color: #d9762c;
}

.wd-status.is-success {
  background: #f0fbf3;
  color: #00893a;
}

.wd-status.is-failed {
  background: #fff1f0;
  color: #d9363e;
}

.wd-status.is-published {
  background: #e8f3ff;
  color: #165dff;
}

.wd-status.is-draft {
  background: #fff7e8;
  color: #d9762c;
}

.wd-op-btn {
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.wd-op-btn:hover {
  text-decoration: underline;
}

.wd-muted {
  color: #c9cdd4;
}

.wd-empty {
  height: 80px;
  text-align: center;
  color: #86909c;
}
</style>
