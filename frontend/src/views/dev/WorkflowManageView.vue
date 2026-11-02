<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { useDevWorkflowStore } from '@/stores/devWorkflow'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const store = useDevWorkflowStore()
const ui = useUiStore()

const keyword = ref('')

const rows = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return store.workflowList
  return store.workflowList.filter(
    (w) =>
      w.name.toLowerCase().includes(q) ||
      w.versions.some((v) => v.version.toLowerCase().includes(q)),
  )
})

function onCreate() {
  router.push('/dev/workflow/create')
}

function onEdit(id: string) {
  router.push({ path: '/dev/workflow/create', query: { id } })
}

function onDetail(id: string) {
  router.push(`/dev/workflow/${id}`)
}

function onPublish(id: string) {
  const workflow = store.byId(id)
  if (!workflow) return
  const version = store.currentVersion(workflow)
  if (!version) return
  if (store.publish(id, version.id)) {
    ui.toast(`${workflow.name} ${version.version} 已发布，画布与参数已冻结`)
  } else {
    ui.toast('该版本已发布或已有运行记录，不可重复发布')
  }
}

function onNewVersion(id: string) {
  const version = store.createNewVersion(id)
  if (version) {
    ui.toast(`已复制为 ${version.version} 草稿，修改将在新版本上进行`)
    router.push({ path: '/dev/workflow/create', query: { id } })
  }
}

function onRun(id: string) {
  const run = store.startRun(id)
  if (run) ui.toast(`已启动运行实例 ${run.id}`)
}

function onDelete(id: string) {
  const workflow = store.byId(id)
  if (!workflow) return
  if (!store.canDelete(id)) {
    ui.toast('该管道已有运行记录，删除会破坏版本与运行的绑定关系')
    return
  }
  if (store.deleteWorkflow(id)) ui.toast(`已删除工作流「${workflow.name}」`)
}
</script>

<template>
  <div class="wf-page">
    <header class="wf-head">
      <div class="wf-head-left">
        <span class="wf-title">工作流管理</span>
        <span class="wf-sub">
          共 {{ store.workflowList.length }} 条 · 草稿可改，发布即冻结，修改 = 创建新版本
        </span>
      </div>
      <button class="wf-primary" type="button" @click="onCreate">
        <SvgIcon :icon="LAS_ICONS.plus" />
        <span>新建工作流</span>
      </button>
    </header>

    <div class="wf-body">
      <div class="wf-toolbar">
        <div class="wf-search">
          <SvgIcon :icon="LAS_ICONS.search" class="wf-search-ic" />
          <input v-model="keyword" type="text" placeholder="搜索工作流名称或版本号">
        </div>
      </div>

      <div class="wf-table-wrap">
        <table class="wf-table">
          <thead>
            <tr>
              <th style="width: 24%">名称</th>
              <th style="width: 10%">当前版本</th>
              <th style="width: 10%">状态</th>
              <th style="width: 18%">最近运行时间</th>
              <th style="width: 26%">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in rows" :key="item.id">
              <td>
                <button class="wf-link" type="button" @click="onDetail(item.id)">
                  {{ item.name }}
                </button>
                <div class="wf-desc">{{ item.description }}</div>
              </td>
              <td class="wf-mono">{{ store.versionLabel(item) }}</td>
              <td>
                <span
                  class="wf-status"
                  :class="store.statusOf(item) === '已发布' ? 'is-published' : 'is-draft'"
                >
                  {{ store.statusOf(item) }}
                </span>
              </td>
              <td class="wf-mono">{{ item.lastRunAt }}</td>
              <td>
                <div class="wf-ops">
                  <template v-if="store.statusOf(item) === '草稿'">
                    <button class="wf-op-btn" type="button" @click="onEdit(item.id)">编辑</button>
                    <button class="wf-op-btn" type="button" @click="onPublish(item.id)">发布</button>
                    <button class="wf-op-btn is-danger" type="button" @click="onDelete(item.id)">
                      删除
                    </button>
                  </template>
                  <template v-else>
                    <button class="wf-op-btn" type="button" @click="onDetail(item.id)">详情</button>
                    <button class="wf-op-btn" type="button" @click="onNewVersion(item.id)">
                      创建新版本
                    </button>
                    <button class="wf-op-btn" type="button" @click="onRun(item.id)">运行</button>
                  </template>
                </div>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td class="wf-empty" colspan="5">暂无工作流</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.wf-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

.wf-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.wf-head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.wf-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.wf-sub {
  font-size: 13px;
  color: #86909c;
}

.wf-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.wf-primary:hover {
  background: #0e42d2;
}

.wf-primary svg {
  width: 13px;
  height: 13px;
}

.wf-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

.wf-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.wf-search {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 260px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.wf-search:focus-within {
  border-color: #165dff;
}

.wf-search-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.wf-search input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.wf-search input::placeholder {
  color: #86909c;
}

.wf-table-wrap {
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  overflow: hidden;
}

.wf-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.wf-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.wf-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.wf-table tbody tr:last-child td {
  border-bottom: none;
}

.wf-table tbody tr:hover td {
  background: #f7f8fa;
}

.wf-link {
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.wf-link:hover {
  text-decoration: underline;
}

.wf-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #86909c;
}

.wf-mono {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12.5px;
  color: #4e5969;
  white-space: nowrap;
}

.wf-status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 9px;
  border-radius: 11px;
  font-size: 12px;
}

.wf-status.is-draft {
  background: #fff7e8;
  color: #d9762c;
}

.wf-status.is-published {
  background: #e8f3ff;
  color: #165dff;
}

.wf-ops {
  display: flex;
  align-items: center;
  gap: 16px;
}

.wf-op-btn {
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.wf-op-btn:hover {
  text-decoration: underline;
}

.wf-op-btn.is-danger {
  color: #f53f3f;
}

.wf-empty {
  height: 90px;
  text-align: center;
  color: #86909c;
}
</style>
