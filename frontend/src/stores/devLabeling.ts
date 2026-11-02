import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  DEV_LABEL_TASKS,
  type DevLabelItem,
  type DevLabelTask,
  type DevReviewAction,
} from '@/mock/dev/labeling'
import { useDevWorkflowStore } from './devWorkflow'

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T
}

export interface ReviewPatch {
  /** 人工确认后的类别 */
  label?: string
  /** 人工确认后的框 [x, y, w, h] */
  bbox?: [number, number, number, number]
}

/**
 * 标注任务 store
 *
 * 口径：自动预标注在管道内的「数据标注」算子里完成，算子跑完后
 * 为每个运行实例创建一个标注任务；本 store 只负责人工环节（确认 / 修改 / 驳回）。
 */
export const useDevLabelingStore = defineStore('dev-labeling', () => {
  const workflowStore = useDevWorkflowStore()
  const tasks = ref<DevLabelTask[]>(clone(DEV_LABEL_TASKS))

  const taskList = computed(() => tasks.value)

  function taskById(id: string): DevLabelTask | undefined {
    return tasks.value.find((t) => t.id === id)
  }

  /** 某个运行实例产生的标注任务 */
  function taskOfRun(runId: string): DevLabelTask | undefined {
    return tasks.value.find((t) => t.runId === runId)
  }

  /** 任务中待人工确认的样本 */
  function pendingItems(task: DevLabelTask): DevLabelItem[] {
    return task.items.filter((item) => !item.reviewAction)
  }

  function remainingOf(task: DevLabelTask): number {
    return pendingItems(task).length
  }

  function progressOf(task: DevLabelTask): number {
    if (!task.items.length) return 0
    return Math.round(((task.items.length - remainingOf(task)) / task.items.length) * 100)
  }

  /** 写一次人工审核动作，并同步任务状态 */
  function review(
    taskId: string,
    itemId: string,
    action: DevReviewAction,
    patch?: ReviewPatch,
  ): void {
    const task = taskById(taskId)
    if (!task) return
    const item = task.items.find((i) => i.id === itemId)
    if (!item) return

    item.reviewAction = action
    if (patch?.label) item.finalLabel = patch.label
    if (patch?.bbox) item.finalBbox = [...patch.bbox] as [number, number, number, number]

    if (action === 'excluded' && !item.finalLabel) item.finalLabel = '正常'

    const remaining = remainingOf(task)
    if (remaining === 0) {
      task.status = '已完成'
      // 挂起解除的唯一触发器：标注任务完成后，对应运行实例自动续跑
      workflowStore.resumeRun(task.runId)
    } else if (task.status === '待处理') {
      task.status = '进行中'
    }
  }

  /** 驳回全部剩余样本 → 任务标记为已驳回 */
  function rejectAll(taskId: string): void {
    const task = taskById(taskId)
    if (!task) return
    pendingItems(task).forEach((item) => {
      item.reviewAction = 'excluded'
      item.finalLabel = item.finalLabel ?? '正常'
    })
    task.status = '已驳回'
  }

  return {
    tasks,
    taskList,
    taskById,
    taskOfRun,
    pendingItems,
    remainingOf,
    progressOf,
    review,
    rejectAll,
  }
})
