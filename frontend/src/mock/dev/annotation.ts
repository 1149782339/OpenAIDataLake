/** 数据开发 - 数据标注 mock 数据 */

export interface DevPretask {
  id: string
  /** 任务名 */
  name: string
  /** 来源管道 */
  pipeline: string
  /** 样本数 */
  samples: number
  /** 置信度阈值 */
  threshold: number
  /** 回写目标表 */
  writeBackTable: string
  status: '标注中' | '已完成' | '等待人工确认'
  createdAt: string
}

export const DEV_PRETASKS: DevPretask[] = [
  {
    id: 'task_fire_prelabel',
    name: '火灾场景预标注',
    pipeline: '标注预训练管道',
    samples: 1286,
    threshold: 0.8,
    writeBackTable: 'fire_prelabel',
    status: '等待人工确认',
    createdAt: '2026-11-02 10:42:03',
  },
  {
    id: 'task_smoke_prelabel',
    name: '烟雾场景预标注',
    pipeline: '火灾检测管道',
    samples: 640,
    threshold: 0.8,
    writeBackTable: 'smoke_prelabel',
    status: '标注中',
    createdAt: '2026-11-02 09:15:22',
  },
  {
    id: 'task_day_prelabel',
    name: '日光干扰样本预标注',
    pipeline: '视频帧清洗管道',
    samples: 320,
    threshold: 0.85,
    writeBackTable: 'daylight_prelabel',
    status: '已完成',
    createdAt: '2026-11-01 14:02:11',
  },
]

export interface DevFrame {
  id: string
  /** 所属任务 */
  taskId: string
  /** 图片引用 */
  file: string
  /** 采集时间 */
  ts: string
  /** 摄像头 */
  camera: string
  /** 模型置信度 */
  confidence: number
  /** 预标注类别 */
  label: string
  /** 标注框 [x, y, w, h]，相对坐标 0~1 */
  bbox: [number, number, number, number]
  /** 是否已人工确认 */
  confirmed: boolean
}

export const DEV_FRAMES: DevFrame[] = [
  {
    id: 'f_001',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1001/000001.jpg',
    ts: '2026-11-02 10:41:52',
    camera: 'cam-1001',
    confidence: 0.42,
    label: '明火',
    bbox: [0.28, 0.32, 0.26, 0.3],
    confirmed: false,
  },
  {
    id: 'f_002',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1001/000002.jpg',
    ts: '2026-11-02 10:41:53',
    camera: 'cam-1001',
    confidence: 0.35,
    label: '浓烟',
    bbox: [0.45, 0.2, 0.3, 0.34],
    confirmed: false,
  },
  {
    id: 'f_003',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1002/000003.jpg',
    ts: '2026-11-02 10:41:55',
    camera: 'cam-1002',
    confidence: 0.91,
    label: '火光',
    bbox: [0.12, 0.44, 0.2, 0.24],
    confirmed: false,
  },
  {
    id: 'f_004',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1002/000004.jpg',
    ts: '2026-11-02 10:41:57',
    camera: 'cam-1002',
    confidence: 0.63,
    label: '明火',
    bbox: [0.52, 0.38, 0.22, 0.26],
    confirmed: false,
  },
  {
    id: 'f_005',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1003/000005.jpg',
    ts: '2026-11-02 10:41:59',
    camera: 'cam-1003',
    confidence: 0.28,
    label: '浓烟',
    bbox: [0.3, 0.16, 0.34, 0.36],
    confirmed: false,
  },
  {
    id: 'f_006',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1003/000006.jpg',
    ts: '2026-11-02 10:42:01',
    camera: 'cam-1003',
    confidence: 0.86,
    label: '火光',
    bbox: [0.6, 0.5, 0.18, 0.2],
    confirmed: false,
  },
  {
    id: 'f_007',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1004/000007.jpg',
    ts: '2026-11-02 10:42:03',
    camera: 'cam-1004',
    confidence: 0.55,
    label: '明火',
    bbox: [0.22, 0.5, 0.24, 0.22],
    confirmed: false,
  },
  {
    id: 'f_008',
    taskId: 'task_fire_prelabel',
    file: 'tos://fire-frames/2026/11/02/cam1004/000008.jpg',
    ts: '2026-11-02 10:42:05',
    camera: 'cam-1004',
    confidence: 0.94,
    label: '浓烟',
    bbox: [0.48, 0.22, 0.26, 0.3],
    confirmed: false,
  },
]

/** 标注类别 */
export const DEV_LABEL_CLASSES = ['明火', '浓烟', '火光', '正常'] as const
