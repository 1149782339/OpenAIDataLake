/** 数据开发 - 资产管理 mock 数据 */

export interface DevAssetPipeline {
  id: string
  /** 管道名 */
  name: string
  /** 版本 */
  version: string
  /** 产出的表 */
  outputTables: string[]
  /** 自动登记状态 */
  autoRegister: '已登记' | '未登记' | '登记失败'
  owner: string
  updatedAt: string
}

export const DEV_ASSET_PIPELINES: DevAssetPipeline[] = [
  {
    id: 'asset_pipe_001',
    name: '火灾检测管道',
    version: 'v3.2.1',
    outputTables: ['fire_frames', 'fire_features', 'fire_candidates'],
    autoRegister: '已登记',
    owner: '2530手机用户#QGDdVm',
    updatedAt: '2026-11-02 10:42:03',
  },
  {
    id: 'asset_pipe_002',
    name: '视频帧清洗管道',
    version: 'v1.8.0',
    outputTables: ['clean_frames'],
    autoRegister: '已登记',
    owner: '2530手机用户#QGDdVm',
    updatedAt: '2026-11-02 09:08:14',
  },
  {
    id: 'asset_pipe_003',
    name: '标注预训练管道',
    version: 'v0.9.6',
    outputTables: ['fire_prelabel'],
    autoRegister: '未登记',
    owner: 'data-team',
    updatedAt: '2026-11-01 02:23:36',
  },
  {
    id: 'asset_pipe_004',
    name: '索引构建管道',
    version: 'v2.0.1',
    outputTables: ['fire_index_v1'],
    autoRegister: '登记失败',
    owner: 'data-team',
    updatedAt: '2026-10-31 18:20:41',
  },
]

export interface DevPublishedDataset {
  id: string
  /** 数据集名称 */
  name: string
  /** 关联的表 */
  table: string
  /** 授权范围 */
  scope: string
  /** 权限 */
  permission: '只读' | '读写'
  version: string
  updatedAt: string
}

export const DEV_PUBLISHED_DATASETS: DevPublishedDataset[] = [
  {
    id: 'pub_ds_001',
    name: 'fire_dataset_v3',
    table: 'fire_dataset_v3',
    scope: '消防安全组 / 张明',
    permission: '读写',
    version: 'v3.0.0',
    updatedAt: '2026-11-02 10:45:12',
  },
  {
    id: 'pub_ds_002',
    name: 'fire_dataset_v3_只读视图',
    table: 'fire_candidates',
    scope: '算法组 / 李婷',
    permission: '只读',
    version: 'v1.4.2',
    updatedAt: '2026-11-01 15:20:08',
  },
  {
    id: 'pub_ds_003',
    name: 'clean_frames_dataset',
    table: 'clean_frames',
    scope: '数据平台组',
    permission: '只读',
    version: 'v1.2.0',
    updatedAt: '2026-10-30 09:12:41',
  },
]

export interface DevLineageNode {
  id: string
  /** 表名 */
  label: string
  /** 节点类型 */
  type: 'raw' | 'table' | 'dataset' | 'index'
}

export interface DevLineageEdge {
  id: string
  from: string
  to: string
  /** 边上的管道名 */
  pipeline: string
  /** 是否为人工确认环节 */
  manual?: boolean
}

/** 火灾检测链路的表血缘 */
export const DEV_LINEAGE_NODES: DevLineageNode[] = [
  { id: 'ln_fire_raw', label: 'fire_raw', type: 'raw' },
  { id: 'ln_fire_frames', label: 'fire_frames', type: 'table' },
  { id: 'ln_fire_features', label: 'fire_features', type: 'table' },
  { id: 'ln_fire_candidates', label: 'fire_candidates', type: 'table' },
  { id: 'ln_fire_prelabel', label: 'fire_prelabel', type: 'table' },
  { id: 'ln_fire_dataset_v3', label: 'fire_dataset_v3', type: 'dataset' },
  { id: 'ln_fire_index_v1', label: 'fire_index_v1', type: 'index' },
]

export const DEV_LINEAGE_EDGES: DevLineageEdge[] = [
  { id: 'le1', from: 'ln_fire_raw', to: 'ln_fire_frames', pipeline: '火灾检测管道' },
  { id: 'le2', from: 'ln_fire_frames', to: 'ln_fire_features', pipeline: '火灾检测管道' },
  { id: 'le3', from: 'ln_fire_features', to: 'ln_fire_candidates', pipeline: '火灾检测管道' },
  { id: 'le4', from: 'ln_fire_candidates', to: 'ln_fire_prelabel', pipeline: '标注预训练管道' },
  {
    id: 'le5',
    from: 'ln_fire_prelabel',
    to: 'ln_fire_dataset_v3',
    pipeline: '人工确认',
    manual: true,
  },
  { id: 'le6', from: 'ln_fire_dataset_v3', to: 'ln_fire_index_v1', pipeline: '索引构建管道' },
]

/** 授权对象候选 */
export const DEV_AUTH_SCOPES = [
  '消防安全组 / 张明',
  '消防安全组 / 王强',
  '算法组 / 李婷',
  '数据平台组',
  '算法组 / 陈晓',
]

export const DEV_PERMISSIONS = ['只读', '读写'] as const
