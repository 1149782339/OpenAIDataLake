/** 数据开发 - 管道输入可用的数据集（来源于「数据集管理」模块） */

/** 数据类型：视频 / 音频 / 文本 / 结构化 */
export type DevDataType = '视频' | '音频' | '文本' | '结构化'

/** 入湖方式：逻辑入湖 / 物理入湖 */
export type DevLakeType = '逻辑入湖' | '物理入湖'

export interface DevPipelineDataset {
  id: string
  /** 数据集名称（英文名） */
  name: string
  /** 中文名 */
  cnName: string
  /** 数据类型 */
  type: DevDataType
  /** 入湖方式 */
  lakeType: DevLakeType
  format: string
  samples: number
  sizeText: string
  creator: string
  updatedAt: string
}

export const DEV_DATA_TYPES: DevDataType[] = ['视频', '音频', '文本', '结构化']

export const DEV_PIPELINE_DATASETS: DevPipelineDataset[] = [
  {
    id: 'ds_fire_video_clips',
    name: 'fire_video_clips',
    cnName: '火灾监控视频切片',
    type: '视频',
    lakeType: '逻辑入湖',
    format: 'Video',
    samples: 20480,
    sizeText: '1.24 GB',
    creator: '2530手机用户#QGDdVm',
    updatedAt: '2026-11-01 10:24:00',
  },
  {
    id: 'ds_road_crash',
    name: 'road_crash_2026',
    cnName: '道路事故监控视频',
    type: '视频',
    lakeType: '物理入湖',
    format: 'Video',
    samples: 5120,
    sizeText: '860.00 MB',
    creator: 'data-team',
    updatedAt: '2026-10-28 15:40:12',
  },
  {
    id: 'ds_asr_zh_10w',
    name: 'asr_zh_10w',
    cnName: '中文语音识别数据集',
    type: '音频',
    lakeType: '逻辑入湖',
    format: 'Audio',
    samples: 102400,
    sizeText: '86.30 GB',
    creator: '2530手机用户#QGDdVm',
    updatedAt: '2026-08-21 11:07:35',
  },
  {
    id: 'ds_factory_sound',
    name: 'factory_sound_alarm',
    cnName: '厂区异常声音样本',
    type: '音频',
    lakeType: '物理入湖',
    format: 'Audio',
    samples: 8600,
    sizeText: '12.60 GB',
    creator: 'data-team',
    updatedAt: '2026-10-30 09:02:33',
  },
  {
    id: 'ds_news_text',
    name: 'news_text_2026',
    cnName: '新闻文本语料',
    type: '文本',
    lakeType: '逻辑入湖',
    format: 'JSONL',
    samples: 128640,
    sizeText: '3.20 GB',
    creator: '2530手机用户#QGDdVm',
    updatedAt: '2026-11-02 08:15:00',
  },
  {
    id: 'ds_fire_report',
    name: 'fire_report_text',
    cnName: '火灾警情文本记录',
    type: '文本',
    lakeType: '物理入湖',
    format: 'Text',
    samples: 4210,
    sizeText: '46.00 MB',
    creator: 'data-team',
    updatedAt: '2026-10-26 17:22:08',
  },
  {
    id: 'ds_sensor_readings',
    name: 'sensor_readings',
    cnName: '传感器时序读数',
    type: '结构化',
    lakeType: '逻辑入湖',
    format: 'Parquet',
    samples: 128640,
    sizeText: '86.42 MB',
    creator: '2530手机用户#QGDdVm',
    updatedAt: '2026-09-18 14:02:00',
  },
  {
    id: 'ds_public',
    name: 'ds_public',
    cnName: '示例数据集',
    type: '结构化',
    lakeType: '物理入湖',
    format: 'Lance',
    samples: 902,
    sizeText: '2.79 MB',
    creator: 'SYSTEM',
    updatedAt: '2025-11-25 23:05:55',
  },
]

export function findPipelineDataset(id: string | null | undefined): DevPipelineDataset | null {
  if (!id) return null
  return DEV_PIPELINE_DATASETS.find((d) => d.id === id) ?? null
}
