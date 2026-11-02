import { defineStore } from 'pinia'
import { computed, ref, type Ref } from 'vue'

export type DatasetKind = 'common' | 'infer'
export type Permission = '全部' | '我创建的' | '公开数据集'

export interface LasDataset {
  id: string
  /** 英文名（超链接） */
  name: string
  /** 中文名 / 描述 */
  cnName: string
  /** 模态，'-' 表示多模态混合 */
  modality: string
  /** 标签数量 */
  tagCount: number
  format: string
  samples: number
  /** 展示用大小 */
  sizeText: string
  /** 字节数，用于大小区间筛选 */
  sizeBytes: number
  creator: string
  createdAt: string
  updatedAt: string
  /** 权限：公开数据集 / 我创建的 */
  permission: Exclude<Permission, '全部'>
  kind: DatasetKind
  /** 数据来源 */
  source: string
  /** 存储路径 */
  path: string
}

/** 模态枚举 */
export const MODALITIES = ['文本', '图片', '音频', '视频'] as const

/** 数据集格式枚举（与火山引擎 LAS 保持一致） */
export const FORMATS = [
  'Lance',
  'Iceberg',
  'CSV',
  'JSONL',
  'Parquet',
  'Image',
  'Audio',
  'Video',
  'Text',
  'Lerobot',
] as const

export const PERMISSIONS: Permission[] = ['全部', '我创建的', '公开数据集']

/** 样本数刻度，index 与滑块档位一一对应 */
export const SAMPLE_MARKS = ['<1千', '1万', '10万', '100万', '1000万', '1亿', '>10亿'] as const
export const SAMPLE_EDGES = [0, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, Number.POSITIVE_INFINITY]

/** 大小刻度 */
export const SIZE_MARKS = ['<10M', '100M', '1G', '10G', '100G', '1T', '>10T'] as const
export const SIZE_EDGES = [
  0,
  10 * 1024 ** 2,
  100 * 1024 ** 2,
  1024 ** 3,
  10 * 1024 ** 3,
  100 * 1024 ** 3,
  1024 ** 4,
  Number.POSITIVE_INFINITY,
]

export const ALL_TAGS = ['训练集', '验证集', '测试集', '公开', '已标注', '已清洗']

const seedDatasets: LasDataset[] = [
  {
    id: 'las_dataset_20261022_102848',
    name: 'las_dataset_20261022_102848',
    cnName: '测试',
    modality: '-',
    tagCount: 0,
    format: 'Lance',
    samples: 6872,
    sizeText: '366.96 MB',
    sizeBytes: 366.96 * 1024 ** 2,
    creator: '2530手机用户#QGDdVm',
    createdAt: '2026-10-22 10:30:02',
    updatedAt: '2026-10-22 10:30:02',
    permission: '我创建的',
    kind: 'common',
    source: '对象存储 OSS',
    path: 'oss://default/las_dataset_20261022_102848.lance',
  },
  {
    id: 'ds_public',
    name: 'ds_public',
    cnName: '示例数据集',
    modality: '-',
    tagCount: 0,
    format: 'Lance',
    samples: 902,
    sizeText: '2.79 MB',
    sizeBytes: 2.79 * 1024 ** 2,
    creator: 'SYSTEM',
    createdAt: '2025-04-12 15:45:04',
    updatedAt: '2025-11-25 23:05:55',
    permission: '公开数据集',
    kind: 'common',
    source: '对象存储 OSS',
    path: 'oss://default/ds_public.lance',
  },
  {
    id: 'ds_public_1',
    name: 'ds_public_1',
    cnName: '示例数据集',
    modality: '-',
    tagCount: 0,
    format: 'Lance',
    samples: 6872,
    sizeText: '80.67 MB',
    sizeBytes: 80.67 * 1024 ** 2,
    creator: 'SYSTEM',
    createdAt: '2025-04-12 15:45:04',
    updatedAt: '2025-11-25 23:31:36',
    permission: '公开数据集',
    kind: 'infer',
    source: '对象存储 OSS',
    path: 'oss://default/ds_public_1.lance',
  },
  {
    id: 'multimodal_cn_2026',
    name: 'multimodal_cn_2026',
    cnName: '中文多模态图文训练集',
    modality: '图片',
    tagCount: 3,
    format: 'Parquet',
    samples: 128640,
    sizeText: '12.40 GB',
    sizeBytes: 12.4 * 1024 ** 3,
    creator: '2530手机用户#QGDdVm',
    createdAt: '2026-03-18 09:12:41',
    updatedAt: '2026-09-02 16:48:10',
    permission: '我创建的',
    kind: 'common',
    source: '文件存储 veFS',
    path: 'vefs://lake/datasets/multimodal_cn_2026.parquet',
  },
  {
    id: 'asr_zh_10w',
    name: 'asr_zh_10w',
    cnName: '中文语音识别数据集',
    modality: '音频',
    tagCount: 1,
    format: 'Lerobot',
    samples: 102400,
    sizeText: '86.30 GB',
    sizeBytes: 86.3 * 1024 ** 3,
    creator: '2530手机用户#QGDdVm',
    createdAt: '2026-01-06 14:03:22',
    updatedAt: '2026-08-21 11:07:35',
    permission: '我创建的',
    kind: 'common',
    source: '对象存储 OSS',
    path: 'oss://default/asr_zh_10w.lerobot',
  },
]

export type SortKey = 'name' | 'createdAt' | 'updatedAt'

export const useLasStore = defineStore('las', () => {
  const datasets = ref<LasDataset[]>(seedDatasets.map((d) => ({ ...d })))

  const query = ref('')
  const sortKey = ref<SortKey>('name')
  const sortDir = ref<'asc' | 'desc'>('asc')
  /** 当前列表类型：通用数据集 / 推理数据集 */
  const kind = ref<DatasetKind>('common')

  const modalities = ref<Set<string>>(new Set<string>())
  const formats = ref<Set<string>>(new Set<string>())
  const permission = ref<Permission>('全部')
  const tags = ref<string[]>([])
  const createdFrom = ref('')
  const createdTo = ref('')
  /** 样本数滑块档位 [min, max] */
  const sampleRange = ref<[number, number]>([0, SAMPLE_MARKS.length - 1])
  /** 大小滑块档位 [min, max] */
  const sizeRange = ref<[number, number]>([0, SIZE_MARKS.length - 1])

  const page = ref(1)
  const pageSize = ref(10)

  const filtered = computed(() => {
    const q = query.value.trim().toLowerCase()
    return datasets.value.filter((item) => {
      if (item.kind !== kind.value) return false
      if (q && !`${item.name} ${item.cnName}`.toLowerCase().includes(q)) return false
      if (modalities.value.size > 0 && !modalities.value.has(item.modality)) return false
      if (formats.value.size > 0 && !formats.value.has(item.format)) return false
      if (permission.value !== '全部' && item.permission !== permission.value) return false
      if (tags.value.length && !tags.value.every((t) => ALL_TAGS.includes(t))) return false
      if (createdFrom.value && item.createdAt.slice(0, 10) < createdFrom.value) return false
      if (createdTo.value && item.createdAt.slice(0, 10) > createdTo.value) return false

      const sampleMin = SAMPLE_EDGES[sampleRange.value[0]]
      const sampleMax = SAMPLE_EDGES[sampleRange.value[1] + 1]
      if (item.samples < sampleMin || item.samples >= sampleMax) return false

      const sizeMin = SIZE_EDGES[sizeRange.value[0]]
      const sizeMax = SIZE_EDGES[sizeRange.value[1] + 1]
      if (item.sizeBytes < sizeMin || item.sizeBytes >= sizeMax) return false

      return true
    })
  })

  const sorted = computed(() => {
    const rows = [...filtered.value]
    const dir = sortDir.value === 'asc' ? 1 : -1
    rows.sort((a, b) => {
      if (sortKey.value === 'name') return a.name.localeCompare(b.name) * dir
      if (sortKey.value === 'createdAt') return a.createdAt.localeCompare(b.createdAt) * dir
      return a.updatedAt.localeCompare(b.updatedAt) * dir
    })
    return rows
  })

  const total = computed(() => sorted.value.length)
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const pageRows = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return sorted.value.slice(start, start + pageSize.value)
  })

  /** 是否存在生效中的筛选条件 */
  const hasFilter = computed(
    () =>
      modalities.value.size > 0 ||
      formats.value.size > 0 ||
      permission.value !== '全部' ||
      tags.value.length > 0 ||
      Boolean(createdFrom.value || createdTo.value) ||
      sampleRange.value[0] !== 0 ||
      sampleRange.value[1] !== SAMPLE_MARKS.length - 1 ||
      sizeRange.value[0] !== 0 ||
      sizeRange.value[1] !== SIZE_MARKS.length - 1,
  )

  function setQuery(value: string) {
    query.value = value
    page.value = 1
  }

  function setSort(key: SortKey) {
    if (sortKey.value === key) {
      sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
    } else {
      sortKey.value = key
      sortDir.value = 'asc'
    }
  }

  function setPageSize(value: number) {
    pageSize.value = value
    page.value = 1
  }

  function toggleIn(set: Ref<Set<string>>, value: string) {
    const next = new Set(set.value)
    if (next.has(value)) next.delete(value)
    else next.add(value)
    set.value = next
    page.value = 1
  }

  function toggleModality(value: string) {
    toggleIn(modalities, value)
  }

  function toggleFormat(value: string) {
    toggleIn(formats, value)
  }

  function resetFilters() {
    modalities.value = new Set<string>()
    formats.value = new Set<string>()
    permission.value = '全部'
    tags.value = []
    createdFrom.value = ''
    createdTo.value = ''
    sampleRange.value = [0, SAMPLE_MARKS.length - 1]
    sizeRange.value = [0, SIZE_MARKS.length - 1]
    page.value = 1
  }

  function setKind(next: DatasetKind) {
    kind.value = next
    page.value = 1
  }

  function listByKind(kind: DatasetKind) {
    return datasets.value.filter((d) => d.kind === kind)
  }

  function datasetById(id: string | null | undefined) {
    if (!id) return null
    return datasets.value.find((d) => d.id === id) ?? null
  }

  function addDataset(
    payload: Omit<LasDataset, 'id' | 'createdAt' | 'updatedAt'> & { id?: string },
  ) {
    const now = new Date()
    const stamp = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
      now.getDate(),
    ).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(
      now.getMinutes(),
    ).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
    const item: LasDataset = {
      ...payload,
      id: payload.id ?? `las_dataset_${now.getTime()}`,
      createdAt: stamp,
      updatedAt: stamp,
    }
    datasets.value.unshift(item)
    return item
  }

  return {
    datasets,
    query,
    sortKey,
    sortDir,
    kind,
    modalities,
    formats,
    permission,
    tags,
    createdFrom,
    createdTo,
    sampleRange,
    sizeRange,
    page,
    pageSize,
    filtered,
    sorted,
    total,
    pageCount,
    pageRows,
    hasFilter,
    setQuery,
    setSort,
    setPageSize,
    toggleIn,
    toggleModality,
    toggleFormat,
    resetFilters,
    setKind,
    listByKind,
    datasetById,
    addDataset,
  }
})
