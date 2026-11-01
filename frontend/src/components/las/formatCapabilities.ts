/**
 * 数据集格式对应的「支持功能」矩阵。
 * 数据取自 LAS 控制台创建数据集页面的能力说明弹层。
 */

export type CapabilityKey =
  | 'preview'
  | 'file'
  | 'insight'
  | 'sql'
  | 'filter'
  | 'edit'
  | 'saveAs'

export interface FormatCapability {
  /** 格式用途描述 */
  desc: string
  /** 数据样本预览条数 */
  preview: '不限' | '10条'
  file: boolean
  insight: boolean
  sql: boolean
  filter: boolean
  edit: boolean
  saveAs: boolean
}

export const FORMAT_CAPABILITY: Record<string, FormatCapability> = {
  Lance: {
    desc: '用于Lance表',
    preview: '不限',
    file: true,
    insight: true,
    sql: true,
    filter: true,
    edit: true,
    saveAs: true,
  },
  Iceberg: {
    desc: '用于Iceberg表',
    preview: '不限',
    file: true,
    insight: true,
    sql: true,
    filter: true,
    edit: false,
    saveAs: true,
  },
  CSV: {
    desc: '用于 CSV 和 TSV',
    preview: '不限',
    file: true,
    insight: false,
    sql: true,
    filter: false,
    edit: false,
    saveAs: false,
  },
  JSONL: {
    desc: '用于 JSON 和 JSONL',
    preview: '不限',
    file: true,
    insight: false,
    sql: true,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Parquet: {
    desc: '用于 Parquet',
    preview: '不限',
    file: true,
    insight: false,
    sql: true,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Image: {
    desc: '用于图像文件夹',
    preview: '10条',
    file: true,
    insight: false,
    sql: false,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Audio: {
    desc: '用于音频文件夹',
    preview: '10条',
    file: true,
    insight: false,
    sql: false,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Video: {
    desc: '用于视频文件夹',
    preview: '10条',
    file: true,
    insight: false,
    sql: false,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Text: {
    desc: '用于txt文件',
    preview: '10条',
    file: true,
    insight: false,
    sql: false,
    filter: false,
    edit: false,
    saveAs: false,
  },
  Lerobot: {
    desc: '用于Lerobot数据集',
    preview: '不限',
    file: true,
    insight: false,
    sql: true,
    filter: false,
    edit: false,
    saveAs: false,
  },
}

/** 功能项，顺序与控制台一致 */
export const FORMAT_FEATURES: { key: CapabilityKey; label: string }[] = [
  { key: 'preview', label: '数据样本预览' },
  { key: 'file', label: '文件管理' },
  { key: 'insight', label: '洞察统计' },
  { key: 'sql', label: 'SQL 查询' },
  { key: 'filter', label: '搜索筛选' },
  { key: 'edit', label: '编辑删除' },
  { key: 'saveAs', label: '另存为' },
]
