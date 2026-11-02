/** 数据开发 - 标注任务（人工标注工作台）mock 数据
 *
 * 口径：自动预标注由管道内的「数据标注」算子完成；
 * 算子跑完后按置信度分流，低置信度样本生成一个「标注任务」对象，
 * 出现在数据标注列表中，等待人工确认。任务与运行实例一一对应，不跨运行合并。
 */

/* 示例画面：前 3 张为火灾，第 4 张为烟雾，其余为炼钢厂 */
import imgFire1 from '@/assets/images/000001.jpg'
import imgFire2 from '@/assets/images/000002.jpg'
import imgFire3 from '@/assets/images/000003.jpg'
import imgSmoke from '@/assets/images/000004.jpg'
import imgPlant1 from '@/assets/images/000005.jpg'
import imgPlant2 from '@/assets/images/000006.jpg'
import imgPlant3 from '@/assets/images/000007.jpg'
import imgPlant4 from '@/assets/images/000008.jpg'

/** 标注任务状态 */
export type DevLabelTaskStatus = '待处理' | '进行中' | '已完成' | '已驳回'

/** 人工审核动作 */
export type DevReviewAction = 'confirmed' | 'modified' | 'excluded'

/** 标注算子的挂起策略 */
export type DevSuspendPolicy = 'never' | 'suspend' | 'split'

export interface DevLabelItem {
  id: string
  /** 图片引用 */
  file: string
  /** 示例画面（前端打包资源） */
  image: string
  /** 采集时间 */
  ts: string
  /** 摄像头 */
  camera: string
  /** 机器预标注置信度 */
  confidence: number
  /** 机器预标注类别 */
  label: string
  /** 机器预标注框 [x, y, w, h]，相对坐标 0~1 */
  bbox: [number, number, number, number]
  /** 人工审核动作，未处理时为空 */
  reviewAction?: DevReviewAction
  /** 人工修改后的类别 */
  finalLabel?: string
  /** 人工修改后的框 */
  finalBbox?: [number, number, number, number]
}

export interface DevLabelTask {
  /** 任务 ID */
  id: string
  /** 任务名称，默认「管道名 · run#12」 */
  name: string
  /** 来源管道 id */
  pipelineId: string
  /** 来源管道名 */
  pipelineName: string
  /** 来源版本号 */
  versionName: string
  /** 来源运行实例 id */
  runId: string
  /** 来源运行实例序号，展示为 run#12 */
  runNo: number
  /** 挂起策略 */
  suspendPolicy: DevSuspendPolicy
  status: DevLabelTaskStatus
  createdAt: string
  /** 标注结果回写表 */
  resultTable: string
  /** 样本明细 */
  items: DevLabelItem[]
}

/** 标注类别 */
export const DEV_LABEL_CLASSES = ['明火', '浓烟', '火光', '正常'] as const

export const DEV_LABEL_TASKS: DevLabelTask[] = [
  {
    id: 'LT_20261102_0031',
    name: '火灾检测管道 · run#12',
    pipelineId: 'wf_fire_detect',
    pipelineName: '火灾检测管道',
    versionName: 'v1',
    runId: 'run_20261102_0030',
    runNo: 12,
    suspendPolicy: 'suspend',
    status: '进行中',
    createdAt: '2026-11-02 10:42:03',
    resultTable: 'fire_frames_labeled',
    items: [
      {
        id: 'li_001',
        file: 'oss://fire-frames/2026/11/02/cam1001/000001.jpg',
        image: imgFire1,
        ts: '2026-11-02 10:41:52',
        camera: 'cam-1001',
        confidence: 0.42,
        label: '明火',
        bbox: [0.28, 0.32, 0.26, 0.3],
      },
      {
        id: 'li_002',
        file: 'oss://fire-frames/2026/11/02/cam1001/000002.jpg',
        image: imgFire2,
        ts: '2026-11-02 10:41:53',
        camera: 'cam-1001',
        confidence: 0.35,
        label: '浓烟',
        bbox: [0.45, 0.2, 0.3, 0.34],
        reviewAction: 'excluded',
        finalLabel: '正常',
      },
      {
        id: 'li_003',
        file: 'oss://fire-frames/2026/11/02/cam1002/000003.jpg',
        image: imgFire3,
        ts: '2026-11-02 10:41:55',
        camera: 'cam-1002',
        confidence: 0.91,
        label: '火光',
        bbox: [0.12, 0.44, 0.2, 0.24],
        reviewAction: 'confirmed',
      },
      {
        id: 'li_004',
        file: 'oss://fire-frames/2026/11/02/cam1002/000004.jpg',
        image: imgSmoke,
        ts: '2026-11-02 10:41:57',
        camera: 'cam-1002',
        confidence: 0.63,
        label: '明火',
        bbox: [0.52, 0.38, 0.22, 0.26],
        reviewAction: 'modified',
        finalLabel: '浓烟',
        finalBbox: [0.5, 0.36, 0.24, 0.28],
      },
      {
        id: 'li_005',
        file: 'oss://fire-frames/2026/11/02/cam1003/000005.jpg',
        image: imgPlant1,
        ts: '2026-11-02 10:41:59',
        camera: 'cam-1003',
        confidence: 0.28,
        label: '浓烟',
        bbox: [0.3, 0.16, 0.34, 0.36],
      },
      {
        id: 'li_006',
        file: 'oss://fire-frames/2026/11/02/cam1003/000006.jpg',
        image: imgPlant2,
        ts: '2026-11-02 10:42:01',
        camera: 'cam-1003',
        confidence: 0.86,
        label: '火光',
        bbox: [0.6, 0.5, 0.18, 0.2],
        reviewAction: 'confirmed',
      },
    ],
  },
  {
    id: 'LT_20261101_0028',
    name: '视频帧清洗管道 · run#11',
    pipelineId: 'wf_frame_clean',
    pipelineName: '视频帧清洗管道',
    versionName: 'v1',
    runId: 'run_20261102_0029',
    runNo: 11,
    suspendPolicy: 'split',
    status: '已完成',
    createdAt: '2026-11-01 02:03:10',
    resultTable: 'clean_frames_labeled',
    items: [
      {
        id: 'li_101',
        file: 'oss://clean-frames/2026/11/01/cam2001/000101.jpg',
        image: imgPlant3,
        ts: '2026-11-01 02:02:40',
        camera: 'cam-2001',
        confidence: 0.38,
        label: '明火',
        bbox: [0.2, 0.34, 0.24, 0.28],
        reviewAction: 'confirmed',
      },
      {
        id: 'li_102',
        file: 'oss://clean-frames/2026/11/01/cam2001/000102.jpg',
        image: imgPlant4,
        ts: '2026-11-01 02:02:41',
        camera: 'cam-2001',
        confidence: 0.44,
        label: '浓烟',
        bbox: [0.42, 0.22, 0.3, 0.32],
        reviewAction: 'confirmed',
      },
      {
        id: 'li_103',
        file: 'oss://clean-frames/2026/11/01/cam2002/000103.jpg',
        image: imgPlant1,
        ts: '2026-11-01 02:02:43',
        camera: 'cam-2002',
        confidence: 0.31,
        label: '火光',
        bbox: [0.6, 0.48, 0.2, 0.22],
        reviewAction: 'confirmed',
      },
      {
        id: 'li_104',
        file: 'oss://clean-frames/2026/11/01/cam2002/000104.jpg',
        image: imgSmoke,
        ts: '2026-11-01 02:02:45',
        camera: 'cam-2002',
        confidence: 0.39,
        label: '明火',
        bbox: [0.34, 0.3, 0.26, 0.3],
        reviewAction: 'confirmed',
      },
    ],
  },
]
