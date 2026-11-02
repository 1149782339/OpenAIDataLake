/** 数据开发 - 工作流（管道）mock 数据
 *
 * 核心规则：草稿可改，发布即冻结，修改 = 创建新版本。
 * 每个版本保存一份独立的画布快照（节点 / 连线 / 参数），
 * 运行实例绑定到它运行时那个版本，版本不可变是血缘与运行记录的地基。
 */

export interface DevNode {
  id: string
  /** 算子 id */
  opId: string
  x: number
  y: number
  /** 该算子产出的中间表名，画布节点下方小字展示 */
  table?: string
}

export interface DevEdge {
  id: string
  from: string
  to: string
  /** 固定连线（如管道输入 → 第一个算子），不可删除、不可改指 */
  fixed?: boolean
}

/** 管道输入节点在画布中的固定 id */
export const DEV_INPUT_NODE_ID = '__input__'

export type DevRunStatus = '运行中' | '挂起' | '成功' | '失败'

export type DevVersionStatus = '草稿' | '已发布'

export interface DevWorkflowVersion {
  id: string
  /** 版本号，如 v1 / v2 */
  version: string
  status: DevVersionStatus
  /** 调度周期 */
  schedule: string
  /** 管道输入绑定的数据集 id */
  inputDatasetId: string
  /** 管道数据类型（继承自输入数据集） */
  dataType: string
  /** 该版本的画布快照 */
  nodes: DevNode[]
  edges: DevEdge[]
  /** nodeId -> 算子参数值 */
  params: Record<string, Record<string, unknown>>
  createdAt: string
  updatedAt: string
  publishedAt: string
}

export interface DevWorkflow {
  id: string
  name: string
  owner: string
  description: string
  /** 当前版本 id */
  currentVersionId: string
  versions: DevWorkflowVersion[]
  lastRunAt: string
}

export interface DevRunInstance {
  id: string
  /** 运行实例序号，展示为 run#12 */
  runNo: number
  /** 所属管道 */
  workflowId: string
  /** 运行时所绑定的版本 */
  versionId: string
  status: DevRunStatus
  startTime: string
  /** 耗时描述 */
  duration: string
  /** 当前停留的算子 */
  currentOp: string
}

/** 供其它页面使用的管道名称列表 */
export const DEV_PIPELINES = [
  { id: 'pipe_fire_detect', name: '火灾检测管道' },
  { id: 'pipe_frame_clean', name: '视频帧清洗管道' },
  { id: 'pipe_label_pretrain', name: '标注预训练管道' },
]

/** 火灾检测链路的画布快照 */
const FIRE_NODES_V1: DevNode[] = [
  { id: 'n1', opId: 'op_parse_load', x: 152, y: 60, table: 'fire_frames' },
  { id: 'n2', opId: 'op_feature_extract', x: 296, y: 60, table: 'fire_features' },
  { id: 'n3', opId: 'op_retrieve_fusion', x: 440, y: 60, table: 'fire_candidates' },
  { id: 'n4', opId: 'op_data_annotate', x: 584, y: 60, table: 'fire_prelabel' },
  { id: 'n5', opId: 'op_manual_confirm', x: 728, y: 60, table: 'fire_frames_labeled' },
  { id: 'n6', opId: 'op_metadata_extract', x: 872, y: 60, table: 'fire_meta' },
  { id: 'n7', opId: 'op_write_table', x: 1016, y: 60, table: 'fire_dataset_v3' },
]

const FIRE_EDGES_V1: DevEdge[] = [
  { id: 'e0', from: DEV_INPUT_NODE_ID, to: 'n1', fixed: true },
  { id: 'e1', from: 'n1', to: 'n2' },
  { id: 'e2', from: 'n2', to: 'n3' },
  { id: 'e3', from: 'n3', to: 'n4' },
  { id: 'e4', from: 'n4', to: 'n5' },
  { id: 'e5', from: 'n5', to: 'n6' },
  { id: 'e6', from: 'n6', to: 'n7' },
]

/** 检索融合算子的默认参数 */
const FIRE_PARAMS: Record<string, Record<string, unknown>> = {
  n3: {
    query_texts: ['明火', '浓烟', '火光', '火焰蔓延'],
    scalar_filter: 'camera_id IN (1001, 1002, 1003)',
    top_k: 500,
    rrf_k: 60,
    use_index: true,
    index_type: 'IVF-PQ',
  },
  n2: {
    model_type: '多模态',
    model_name: 'multimodal-base-v3',
    batch_size: 64,
    fusion_mode: '加权求和',
  },
  n1: { frame_interval: 2, image_format: 'jpg', max_resolution: '1920x1080' },
  n4: {
    auto_model: 'fire-smoke-det-v2',
    confidence_threshold: 0.5,
    suspend_policy: 'suspend',
  },
  n5: { writeback_table: 'fire_frames_labeled' },
  n6: { time_field: 'event_time' },
  n7: { target_table: 'fire_dataset_v3', write_mode: '追加' },
}

export const DEV_WORKFLOWS: DevWorkflow[] = [
  {
    id: 'wf_fire_detect',
    name: '火灾检测管道',
    owner: '2530手机用户#QGDdVm',
    description: '接入监控视频流，多模态匹配后检索融合召回候选火灾帧。',
    currentVersionId: 'wf_fire_detect_v2',
    lastRunAt: '2026-11-02 10:30:12',
    versions: [
      {
        id: 'wf_fire_detect_v1',
        version: 'v1',
        status: '已发布',
        schedule: '每 30 分钟',
        inputDatasetId: 'ds_fire_video_clips',
        dataType: '视频',
        nodes: FIRE_NODES_V1.map((n) => ({ ...n })),
        edges: FIRE_EDGES_V1.map((e) => ({ ...e })),
        params: JSON.parse(JSON.stringify(FIRE_PARAMS)) as Record<string, Record<string, unknown>>,
        createdAt: '2026-10-20 09:30:00',
        updatedAt: '2026-10-28 17:40:21',
        publishedAt: '2026-10-28 18:00:00',
      },
      {
        id: 'wf_fire_detect_v2',
        version: 'v2',
        status: '草稿',
        schedule: '每 30 分钟',
        inputDatasetId: 'ds_fire_video_clips',
        dataType: '视频',
        nodes: FIRE_NODES_V1.map((n) => ({ ...n })),
        edges: FIRE_EDGES_V1.map((e) => ({ ...e })),
        params: JSON.parse(JSON.stringify(FIRE_PARAMS)) as Record<string, Record<string, unknown>>,
        createdAt: '2026-11-02 10:35:00',
        updatedAt: '2026-11-02 10:41:12',
        publishedAt: '',
      },
    ],
  },
  {
    id: 'wf_frame_clean',
    name: '视频帧清洗管道',
    owner: '2530手机用户#QGDdVm',
    description: '抽帧、去重与画质过滤，产出清洗后的监控帧。',
    currentVersionId: 'wf_frame_clean_v1',
    lastRunAt: '2026-11-02 09:00:02',
    versions: [
      {
        id: 'wf_frame_clean_v1',
        version: 'v1',
        status: '已发布',
        schedule: '每小时',
        inputDatasetId: 'ds_road_crash',
        dataType: '视频',
        nodes: [
          { id: 'c1', opId: 'op_parse_load', x: 152, y: 60, table: 'clean_frames' },
          { id: 'c2', opId: 'op_feature_extract', x: 296, y: 60, table: 'clean_features' },
        ],
        edges: [
          { id: 'ce0', from: DEV_INPUT_NODE_ID, to: 'c1', fixed: true },
          { id: 'ce1', from: 'c1', to: 'c2' },
        ],
        params: {
          c1: { frame_interval: 5, image_format: 'jpg', max_resolution: '1280x720' },
          c2: {
            model_type: '图像',
            model_name: 'image-base-v2',
            batch_size: 64,
            image_size: '224x224',
          },
        },
        createdAt: '2026-09-12 14:20:00',
        updatedAt: '2026-10-30 11:02:44',
        publishedAt: '2026-10-30 12:00:00',
      },
    ],
  },
  {
    id: 'wf_pretrain',
    name: '标注预训练管道',
    owner: 'data-team',
    description: '对候选帧做预标注，低置信度样本转入人工标注工作台。',
    currentVersionId: 'wf_pretrain_v1',
    lastRunAt: '2026-11-01 02:00:00',
    versions: [
      {
        id: 'wf_pretrain_v1',
        version: 'v1',
        status: '草稿',
        schedule: '每天 02:00',
        inputDatasetId: 'ds_news_text',
        dataType: '文本',
        nodes: [
          { id: 'p1', opId: 'op_parse_load', x: 152, y: 60, table: 'pretrain_frames' },
          { id: 'p2', opId: 'op_feature_extract', x: 296, y: 60, table: 'pretrain_features' },
          { id: 'p3', opId: 'op_retrieve_fusion', x: 440, y: 60, table: 'pretrain_candidates' },
          { id: 'p4', opId: 'op_data_annotate', x: 584, y: 60, table: 'pretrain_prelabel' },
          { id: 'p5', opId: 'op_manual_confirm', x: 728, y: 60, table: 'pretrain_labeled' },
        ],
        edges: [
          { id: 'pe0', from: DEV_INPUT_NODE_ID, to: 'p1', fixed: true },
          { id: 'pe1', from: 'p1', to: 'p2' },
          { id: 'pe2', from: 'p2', to: 'p3' },
          { id: 'pe3', from: 'p3', to: 'p4' },
          { id: 'pe4', from: 'p4', to: 'p5' },
        ],
        params: {
          p1: { file_encoding: 'UTF-8', chunk_rule: '按段落' },
          p2: {
            model_type: '文本',
            model_name: 'text-base-v2',
            batch_size: 64,
            max_token: 512,
          },
          p3: {
            query_texts: ['明火', '浓烟', '火光', '火焰蔓延'],
            scalar_filter: 'camera_id IN (2001, 2002)',
            top_k: 200,
            rrf_k: 60,
            use_index: true,
            index_type: 'HNSW',
          },
          p4: {
            auto_model: 'fire-smoke-det-v2',
            confidence_threshold: 0.5,
            suspend_policy: 'split',
          },
          p5: { writeback_table: 'pretrain_labeled' },
        },
        createdAt: '2026-11-01 09:12:00',
        updatedAt: '2026-11-01 09:40:00',
        publishedAt: '',
      },
    ],
  },
]

export const DEV_RUN_INSTANCES: DevRunInstance[] = [
  {
    id: 'run_20261102_0031',
    runNo: 13,
    workflowId: 'wf_fire_detect',
    versionId: 'wf_fire_detect_v1',
    status: '运行中',
    startTime: '2026-11-02 10:30:12',
    duration: '11 分 42 秒',
    currentOp: '检索融合',
  },
  {
    id: 'run_20261102_0030',
    runNo: 12,
    workflowId: 'wf_fire_detect',
    versionId: 'wf_fire_detect_v1',
    status: '挂起',
    startTime: '2026-11-02 10:00:05',
    duration: '29 分 58 秒',
    currentOp: '数据标注',
  },
  {
    id: 'run_20261102_0029',
    runNo: 11,
    workflowId: 'wf_frame_clean',
    versionId: 'wf_frame_clean_v1',
    status: '成功',
    startTime: '2026-11-02 09:00:02',
    duration: '8 分 12 秒',
    currentOp: '-',
  },
  {
    id: 'run_20261102_0028',
    runNo: 10,
    workflowId: 'wf_fire_detect',
    versionId: 'wf_fire_detect_v1',
    status: '成功',
    startTime: '2026-11-02 09:30:07',
    duration: '12 分 03 秒',
    currentOp: '-',
  },
  {
    id: 'run_20261102_0027',
    runNo: 9,
    workflowId: 'wf_fire_detect',
    versionId: 'wf_fire_detect_v1',
    status: '失败',
    startTime: '2026-11-02 09:00:11',
    duration: '1 分 47 秒',
    currentOp: '特征提取',
  },
  {
    id: 'run_20261102_0026',
    runNo: 8,
    workflowId: 'wf_frame_clean',
    versionId: 'wf_frame_clean_v1',
    status: '成功',
    startTime: '2026-11-01 02:00:00',
    duration: '23 分 36 秒',
    currentOp: '-',
  },
]
