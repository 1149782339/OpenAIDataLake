/** 数据开发 - 执行引擎监控 mock 数据 */

export interface DevEngineMetric {
  key: string
  label: string
  value: string
  /** 辅助说明 */
  desc: string
  /** 趋势描述 */
  trend: string
  /** 指标方向：up 为上升告警，down 为下降，flat 为平稳 */
  trendType: 'up' | 'down' | 'flat'
}

export const DEV_ENGINE_METRICS: DevEngineMetric[] = [
  {
    key: 'running',
    label: '运行中实例数',
    value: '7',
    desc: '当前正在执行的管道实例',
    trend: '较昨日 +2',
    trendType: 'up',
  },
  {
    key: 'queued',
    label: '队列等待数',
    value: '3',
    desc: '排队等待资源分配的实例',
    trend: '较昨日 -1',
    trendType: 'down',
  },
  {
    key: 'retry',
    label: '今日重试次数',
    value: '12',
    desc: '今日各算子自动重试总次数',
    trend: '较昨日 +4',
    trendType: 'up',
  },
  {
    key: 'quota',
    label: '资源配额使用率',
    value: '68%',
    desc: '集群 CPU 配额整体使用比例',
    trend: '较昨日 +6%',
    trendType: 'up',
  },
]

export interface DevOpRecord {
  id: string
  pipeline: string
  operator: string
  /** 状态机流转时间线 */
  flow: { state: string; time: string }[]
  /** 最终状态 */
  status: '成功' | '失败' | '运行中' | '重试中'
  retries: number
  duration: string
}

export const DEV_OP_RECORDS: DevOpRecord[] = [
  {
    id: 'op_rec_001',
    pipeline: '火灾检测管道',
    operator: '检索融合',
    flow: [
      { state: 'PENDING', time: '10:30:12' },
      { state: 'RUNNING', time: '10:30:15' },
      { state: 'SUCCESS', time: '10:41:54' },
    ],
    status: '成功',
    retries: 0,
    duration: '11 分 39 秒',
  },
  {
    id: 'op_rec_002',
    pipeline: '火灾检测管道',
    operator: '数据标注',
    flow: [
      { state: 'PENDING', time: '10:41:54' },
      { state: 'RUNNING', time: '10:41:56' },
      { state: 'SUSPENDED', time: '10:42:03' },
    ],
    status: '运行中',
    retries: 0,
    duration: '11 分 39 秒',
  },
  {
    id: 'op_rec_003',
    pipeline: '火灾检测管道',
    operator: '特征提取',
    flow: [
      { state: 'PENDING', time: '10:00:22' },
      { state: 'RUNNING', time: '10:00:25' },
      { state: 'FAILED', time: '10:01:58' },
      { state: 'RETRYING', time: '10:02:10' },
      { state: 'RUNNING', time: '10:02:12' },
    ],
    status: '重试中',
    retries: 2,
    duration: '1 分 36 秒',
  },
  {
    id: 'op_rec_004',
    pipeline: '视频帧清洗管道',
    operator: '数据解析加载',
    flow: [
      { state: 'PENDING', time: '09:00:02' },
      { state: 'RUNNING', time: '09:00:03' },
      { state: 'SUCCESS', time: '09:08:14' },
    ],
    status: '成功',
    retries: 0,
    duration: '8 分 11 秒',
  },
  {
    id: 'op_rec_005',
    pipeline: '视频帧清洗管道',
    operator: '特征转换',
    flow: [
      { state: 'PENDING', time: '09:08:14' },
      { state: 'RUNNING', time: '09:08:16' },
      { state: 'FAILED', time: '09:08:22' },
    ],
    status: '失败',
    retries: 3,
    duration: '8 分 11 秒',
  },
  {
    id: 'op_rec_006',
    pipeline: '标注预训练管道',
    operator: '数据标注',
    flow: [
      { state: 'PENDING', time: '02:00:00' },
      { state: 'RUNNING', time: '02:00:04' },
      { state: 'SUCCESS', time: '02:23:36' },
    ],
    status: '成功',
    retries: 1,
    duration: '23 分 32 秒',
  },
]

export interface DevQuota {
  pipeline: string
  cpuUsed: number
  cpuTotal: number
  memUsed: number
  memTotal: number
  /** 占用比例（%） */
  usage: number
}

export const DEV_QUOTAS: DevQuota[] = [
  { pipeline: '火灾检测管道', cpuUsed: 18, cpuTotal: 32, memUsed: 96, memTotal: 128, usage: 74 },
  { pipeline: '视频帧清洗管道', cpuUsed: 9, cpuTotal: 16, memUsed: 38, memTotal: 64, usage: 52 },
  { pipeline: '标注预训练管道', cpuUsed: 4, cpuTotal: 16, memUsed: 12, memTotal: 64, usage: 21 },
  { pipeline: '检索服务管道', cpuUsed: 7, cpuTotal: 12, memUsed: 28, memTotal: 32, usage: 63 },
]
