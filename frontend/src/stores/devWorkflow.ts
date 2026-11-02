import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  DEV_RUN_INSTANCES,
  DEV_WORKFLOWS,
  type DevEdge,
  type DevNode,
  type DevRunInstance,
  type DevWorkflow,
  type DevWorkflowVersion,
} from '@/mock/dev/workflow'

export interface DevDraftPayload {
  name: string
  description: string
  schedule: string
  /** 管道输入绑定的数据集 id */
  inputDatasetId: string
  /** 管道数据类型（继承自输入数据集） */
  dataType: string
  nodes: DevNode[]
  edges: DevEdge[]
  params: Record<string, Record<string, unknown>>
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

/**
 * 工作流 store
 *
 * 核心规则：草稿可改，发布即冻结，修改 = 创建新版本。
 * - 版本一旦发布、或已绑定运行实例，画布与参数即只读；
 * - 任何改动都通过「创建新版本」复制出新的草稿完成，旧版本与它的运行记录原封不动；
 * - 运行实例绑定到它运行时那个版本，版本不可变是血缘与运行记录的地基。
 */
export const useDevWorkflowStore = defineStore('dev-workflow', () => {
  const workflows = ref<DevWorkflow[]>(clone(DEV_WORKFLOWS))
  const runs = ref<DevRunInstance[]>(clone(DEV_RUN_INSTANCES))

  const workflowList = computed(() => workflows.value)

  function byId(id: string): DevWorkflow | undefined {
    return workflows.value.find((w) => w.id === id)
  }

  function versionOf(workflowId: string, versionId: string): DevWorkflowVersion | undefined {
    return byId(workflowId)?.versions.find((v) => v.id === versionId)
  }

  function currentVersion(workflow: DevWorkflow): DevWorkflowVersion | undefined {
    return workflow.versions.find((v) => v.id === workflow.currentVersionId)
  }

  function versionLabel(workflow: DevWorkflow): string {
    return currentVersion(workflow)?.version ?? '-'
  }

  function statusOf(workflow: DevWorkflow): '草稿' | '已发布' {
    return currentVersion(workflow)?.status ?? '草稿'
  }

  /** 该版本已有的运行实例 */
  function runsOfVersion(workflowId: string, versionId: string): DevRunInstance[] {
    return runs.value.filter((r) => r.workflowId === workflowId && r.versionId === versionId)
  }

  /** 该管道的全部运行实例（按开始时间倒序） */
  function runsOfWorkflow(workflowId: string): DevRunInstance[] {
    return runs.value
      .filter((r) => r.workflowId === workflowId)
      .slice()
      .sort((a, b) => (a.startTime < b.startTime ? 1 : -1))
  }

  /**
   * 版本是否已冻结：已发布，或已经绑定了运行实例。
   * 冻结后画布与参数只读，任何修改都必须创建新版本。
   */
  function isFrozen(workflowId: string, versionId: string): boolean {
    const version = versionOf(workflowId, versionId)
    if (!version) return true
    if (version.status === '已发布') return true
    return runsOfVersion(workflowId, versionId).length > 0
  }

  /** 管道是否可删除：当前版本为草稿，且没有历史运行记录 */
  function canDelete(workflowId: string): boolean {
    const workflow = byId(workflowId)
    if (!workflow) return false
    if (statusOf(workflow) !== '草稿') return false
    return runsOfWorkflow(workflowId).length === 0
  }

  function nextVersionNo(workflow: DevWorkflow): string {
    const nums = workflow.versions.map((v) => Number(v.version.replace('v', '')) || 0)
    return `v${Math.max(0, ...nums) + 1}`
  }

  /** 新建工作流：初始为 v1 草稿 */
  function createWorkflow(payload: { name: string; description: string }): DevWorkflow {
    const id = `wf_${Date.now()}`
    const version: DevWorkflowVersion = {
      id: `${id}_v1`,
      version: 'v1',
      status: '草稿',
      schedule: '手动',
      inputDatasetId: '',
      dataType: '',
      nodes: [],
      edges: [],
      params: {},
      createdAt: nowText(),
      updatedAt: nowText(),
      publishedAt: '',
    }
    const workflow: DevWorkflow = {
      id,
      name: payload.name,
      description: payload.description,
      owner: '2530手机用户#QGDdVm',
      currentVersionId: version.id,
      versions: [version],
      lastRunAt: '-',
    }
    workflows.value.unshift(workflow)
    return workflow
  }

  /** 保存草稿：仅允许写入未冻结的草稿版本 */
  function saveDraft(workflowId: string, versionId: string, payload: DevDraftPayload): boolean {
    if (isFrozen(workflowId, versionId)) return false
    const workflow = byId(workflowId)
    const version = versionOf(workflowId, versionId)
    if (!workflow || !version) return false
    workflow.name = payload.name
    workflow.description = payload.description
    version.schedule = payload.schedule
    version.inputDatasetId = payload.inputDatasetId
    version.dataType = payload.dataType
    version.nodes = clone(payload.nodes)
    version.edges = clone(payload.edges)
    version.params = clone(payload.params)
    version.updatedAt = nowText()
    return true
  }

  /** 发布：草稿 → 已发布，发布即冻结；未绑定管道输入数据集时不允许发布 */
  function publish(workflowId: string, versionId: string): boolean {
    if (isFrozen(workflowId, versionId)) return false
    const version = versionOf(workflowId, versionId)
    if (!version) return false
    if (!version.inputDatasetId) return false
    version.status = '已发布'
    version.publishedAt = nowText()
    version.updatedAt = nowText()
    return true
  }

  /** 创建新版本：复制当前版本为新的草稿，旧版本与其运行记录保持不变 */
  function createNewVersion(workflowId: string): DevWorkflowVersion | null {
    const workflow = byId(workflowId)
    if (!workflow) return null
    const source = currentVersion(workflow)
    if (!source) return null
    const version: DevWorkflowVersion = {
      id: `${workflow.id}_v${Date.now()}`,
      version: nextVersionNo(workflow),
      status: '草稿',
      schedule: source.schedule,
      inputDatasetId: source.inputDatasetId,
      dataType: source.dataType,
      nodes: clone(source.nodes),
      edges: clone(source.edges),
      params: clone(source.params),
      createdAt: nowText(),
      updatedAt: nowText(),
      publishedAt: '',
    }
    workflow.versions.push(version)
    workflow.currentVersionId = version.id
    return version
  }

  function deleteWorkflow(workflowId: string): boolean {
    if (!canDelete(workflowId)) return false
    workflows.value = workflows.value.filter((w) => w.id !== workflowId)
    runs.value = runs.value.filter((r) => r.workflowId !== workflowId)
    return true
  }

  /** 运行：绑定当前版本创建运行实例 */
  function startRun(workflowId: string): DevRunInstance | null {
    const workflow = byId(workflowId)
    if (!workflow) return null
    const version = currentVersion(workflow)
    if (!version) return null
    const run: DevRunInstance = {
      id: `run_${Date.now()}`,
      workflowId,
      versionId: version.id,
      status: '运行中',
      startTime: nowText(),
      duration: '0 分 0 秒',
      currentOp: '数据解析加载',
    }
    runs.value.unshift(run)
    workflow.lastRunAt = run.startTime
    return run
  }

  /** 恢复挂起的运行实例 */
  function resumeRun(runId: string): boolean {
    const run = runs.value.find((r) => r.id === runId)
    if (!run || run.status !== '挂起') return false
    run.status = '运行中'
    return true
  }

  return {
    workflows,
    runs,
    workflowList,
    byId,
    versionOf,
    currentVersion,
    versionLabel,
    statusOf,
    runsOfVersion,
    runsOfWorkflow,
    isFrozen,
    canDelete,
    createWorkflow,
    saveDraft,
    publish,
    createNewVersion,
    deleteWorkflow,
    startRun,
    resumeRun,
  }
})

function nowText(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
