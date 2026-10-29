import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export interface SchemaField {
  name: string
  type: string
  nullable?: boolean
  desc?: string
}

export interface Dataset {
  id: number
  name: string
  desc: string
  format: string
  samples: number
  size: string
  ossPath: string
  schema?: SchemaField[]
  /** 由某张表发布而来 */
  sourceTableId?: string | null
}

export const ALL_FORMATS = ['Lance', 'Parquet', 'Paimon', 'JSONL', 'CSV'] as const

const seedDatasets: Dataset[] = [
  {
    id: 1,
    name: 'las_dataset_20261022_102848',
    desc: '图像训练数据集（Lance）',
    format: 'Lance',
    samples: 6872,
    size: '366.96 MB',
    ossPath: 'oss://default/las_dataset_20261022_102848.lance',
    schema: [
      { name: 'id', type: 'int64', nullable: false, desc: '主键' },
      { name: 'image', type: 'binary', nullable: false, desc: '图片' },
      { name: 'caption', type: 'string', nullable: true, desc: '描述' },
      { name: 'embedding', type: 'vector<float32>[512]', nullable: false, desc: '图像向量' },
      { name: 'label', type: 'string', nullable: true, desc: '标签' },
      { name: 'created_at', type: 'timestamp[us]', nullable: true, desc: '创建时间' },
    ],
  },
  {
    id: 2,
    name: 'ds_public',
    desc: '示例多模态数据集（Lance）',
    format: 'Lance',
    samples: 902,
    size: '2.79 MB',
    ossPath: 'oss://default/ds_public.lance',
    schema: [
      { name: 'id', type: 'int64', nullable: false, desc: '主键' },
      { name: 'text', type: 'string', nullable: true, desc: '文本' },
      { name: 'embedding', type: 'vector<float32>[768]', nullable: false, desc: '向量' },
      { name: 'split', type: 'string', nullable: true, desc: '数据集划分' },
    ],
  },
  {
    id: 3,
    name: 'sensor_readings',
    desc: '传感器时序读数（Parquet 列式存储）',
    format: 'Parquet',
    samples: 128640,
    size: '86.42 MB',
    ossPath: 'oss://default/sensor_readings.parquet',
    schema: [
      { name: 'id', type: 'int64', nullable: false, desc: '主键' },
      { name: 'device_id', type: 'string', nullable: false, desc: '设备编号' },
      { name: 'temperature', type: 'float64', nullable: true, desc: '温度' },
      { name: 'humidity', type: 'float64', nullable: true, desc: '湿度' },
      { name: 'ts', type: 'timestamp[us]', nullable: false, desc: '采集时间' },
    ],
  },
  {
    id: 4,
    name: 'video_clips',
    desc: '视频切片数据（Paimon 表格式，含向量列）',
    format: 'Paimon',
    samples: 20480,
    size: '1.24 GB',
    ossPath: 'oss://default/video_clips',
    schema: [
      { name: 'id', type: 'int64', nullable: false, desc: '主键' },
      { name: 'clip_id', type: 'string', nullable: false, desc: '切片编号' },
      { name: 'embedding', type: 'vector<float32>[512]', nullable: false, desc: '视频向量' },
      { name: 'label', type: 'string', nullable: true, desc: '标签' },
      { name: 'created_at', type: 'timestamp[us]', nullable: true, desc: '创建时间' },
    ],
  },
]

export const useDatasetStore = defineStore('datasets', () => {
  const datasets = ref<Dataset[]>(
    seedDatasets.map((d) => ({ ...d, schema: d.schema?.map((s) => ({ ...s })) })),
  )
  const query = ref('')
  const sortDir = ref<'asc' | 'desc' | null>(null)
  const formats = ref<Set<string>>(new Set<string>(ALL_FORMATS))
  const page = ref(1)
  const pageSize = ref(10)

  /** 经过搜索 / 格式筛选 / 排序后的列表 */
  const visibleRows = computed(() => {
    const q = query.value.trim().toLowerCase()
    const rows = datasets.value.filter((item) => {
      const matchQuery =
        !q ||
        item.name.toLowerCase().includes(q) ||
        String(item.desc || '').toLowerCase().includes(q)
      return matchQuery && formats.value.has(item.format)
    })
    if (sortDir.value) {
      rows.sort((a, b) => {
        const result = a.name.localeCompare(b.name)
        return sortDir.value === 'asc' ? result : -result
      })
    }
    return rows
  })

  const total = computed(() => visibleRows.value.length)
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  const pageRows = computed(() => {
    const start = (page.value - 1) * pageSize.value
    return visibleRows.value.slice(start, start + pageSize.value)
  })

  function setQuery(value: string) {
    query.value = value
    page.value = 1
  }

  function toggleSort() {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  }

  function setPageSize(value: number) {
    pageSize.value = value
    page.value = 1
  }

  function setFormats(next: Set<string>) {
    formats.value = next
    page.value = 1
  }

  function resetFilters() {
    formats.value = new Set<string>(['Lance', 'Parquet', 'JSONL', 'CSV'])
    page.value = 1
  }

  function datasetById(id: number | string | null | undefined) {
    if (id === null || id === undefined) return null
    return datasets.value.find((d) => String(d.id) === String(id)) ?? null
  }

  function addDataset(dataset: Omit<Dataset, 'id'> & { id?: number }) {
    const item: Dataset = { ...dataset, id: dataset.id ?? Date.now() }
    datasets.value.push(item)
    return item
  }

  function removeDataset(id: number) {
    datasets.value = datasets.value.filter((d) => d.id !== id)
  }

  function datasetsInFormat(format: string) {
    return datasets.value.filter((ds) => ds.format === format)
  }

  function datasetSchema(ds: Dataset | null | undefined): SchemaField[] | null {
    if (!ds) return null
    if (ds.schema && ds.schema.length) return ds.schema
    return null
  }

  return {
    datasets,
    query,
    sortDir,
    formats,
    page,
    pageSize,
    visibleRows,
    total,
    pageCount,
    pageRows,
    setQuery,
    toggleSort,
    setPageSize,
    setFormats,
    resetFilters,
    datasetById,
    addDataset,
    removeDataset,
    datasetsInFormat,
    datasetSchema,
  }
})
