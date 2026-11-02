/** 数据开发 - 算子契约与参数定义（mock） */

export interface DevColumn {
  /** 列名 */
  name: string
  /** 列类型 */
  type: string
  /** 是否为必填契约列 */
  required?: boolean
  /** 说明 */
  desc?: string
}

export type DevParamType = 'string' | 'number' | 'boolean' | 'list' | 'text' | 'enum'

export interface DevParamOption {
  value: string
  label: string
}

export interface DevParamDef {
  key: string
  label: string
  type: DevParamType
  default: string | number | boolean | string[]
  desc: string
  /** 必填：发布前必须配置 */
  required?: boolean
  /** 枚举字段的可选项 */
  options?: DevParamOption[]
}

/**
 * 联动参数组：由一个「枚举 / 开关字段」控制显示哪一组参数。
 * - driver 为 'data_type' 时由管道级数据类型（继承自管道输入数据集）驱动；
 * - 其余 driver 为算子自身的字段，例如「检索融合」的 use_index 开关。
 */
export interface DevParamGroup {
  /** 控制字段 key */
  driver: string
  /** 控制字段取该值时渲染这组参数 */
  value: string
  params: DevParamDef[]
}

export interface DevOperator {
  id: string
  /** 算子名 */
  name: string
  /** 算子分类 */
  category: string
  /** 一句话功能描述 */
  desc: string
  /** 运行环境镜像 */
  runtime: string
  /** 输入契约要求列 */
  inputs: DevColumn[]
  /** 输出 schema 声明列 */
  outputs: DevColumn[]
  /** 算子参数 */
  params: DevParamDef[]
  /** 联动参数组 */
  groups?: DevParamGroup[]
  /** 接受的数据类型（管道输入数据集的类型白名单），为空表示不限 */
  accepts?: string[]
  /** 是否内置算子 */
  builtin: boolean
}

export const DEV_OPERATORS: DevOperator[] = [
  {
    id: 'op_parse_load',
    name: '数据解析加载',
    category: '数据接入',
    desc: '按声明格式解析原始文件并加载为统一数据结构，产出解析元信息。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    accepts: ['视频', '音频', '文本'],
    inputs: [
      { name: 'file_path', type: 'string', required: true, desc: '数据文件在对象存储中的路径' },
      { name: 'format', type: 'string', required: true, desc: '解析格式，如 jsonl / parquet / mp4' },
    ],
    outputs: [
      { name: 'raw_data', type: 'binary', required: true, desc: '解析后的原始记录' },
      { name: 'parse_meta', type: 'string', desc: '解析元信息（行数、耗时等）' },
    ],
    params: [],
    groups: [
      {
        driver: 'data_type',
        value: '视频',
        params: [
          {
            key: 'frame_interval',
            label: '抽帧间隔（秒）',
            type: 'number',
            default: 2,
            required: true,
            desc: '每隔多少秒从视频中抽取一帧',
          },
          {
            key: 'image_format',
            label: '输出图片格式',
            type: 'enum',
            default: 'jpg',
            options: [
              { value: 'jpg', label: 'jpg' },
              { value: 'png', label: 'png' },
            ],
            desc: '抽帧后图片的存储格式',
          },
          {
            key: 'max_resolution',
            label: '分辨率上限',
            type: 'string',
            default: '1920x1080',
            desc: '超过该分辨率的图片将被等比缩放',
          },
        ],
      },
      {
        driver: 'data_type',
        value: '音频',
        params: [
          {
            key: 'sample_rate',
            label: '采样率（kHz）',
            type: 'enum',
            default: '16',
            options: [
              { value: '8', label: '8' },
              { value: '16', label: '16' },
              { value: '44.1', label: '44.1' },
              { value: '48', label: '48' },
            ],
            desc: '音频重采样的目标采样率',
          },
          {
            key: 'slice_duration',
            label: '切片时长（秒）',
            type: 'number',
            default: 30,
            desc: '单条音频切片的时长',
          },
          {
            key: 'enable_transcribe',
            label: '开启语音转写',
            type: 'boolean',
            default: true,
            desc: '是否在解析时同步产出语音转写文本',
          },
        ],
      },
      {
        driver: 'data_type',
        value: '文本',
        params: [
          {
            key: 'file_encoding',
            label: '文件编码',
            type: 'enum',
            default: 'UTF-8',
            options: [
              { value: 'UTF-8', label: 'UTF-8' },
              { value: 'GBK', label: 'GBK' },
              { value: 'UTF-16', label: 'UTF-16' },
            ],
            desc: '文本文件的读取编码',
          },
          {
            key: 'chunk_rule',
            label: '分片规则',
            type: 'enum',
            default: '按段落',
            options: [
              { value: '按行', label: '按行' },
              { value: '按段落', label: '按段落' },
              { value: '按 token 数', label: '按 token 数' },
            ],
            desc: '长文本的切分方式',
          },
        ],
      },
    ],
  },
  {
    id: 'op_feature_extract',
    name: '特征提取',
    category: '特征工程',
    desc: '对解析后的图像与文本抽取多模态向量特征，产出检测框信息。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'raw_data', type: 'binary', required: true, desc: '上游解析后的原始记录' },
    ],
    outputs: [
      { name: 'image_emb', type: 'vector<float32>[512]', required: true, desc: '图像向量' },
      { name: 'text_emb', type: 'vector<float32>[512]', required: true, desc: '文本向量' },
      { name: 'detect_box', type: 'string', desc: '检测框坐标，JSON 数组' },
      { name: 'camera_id', type: 'string', required: true, desc: '帧所属摄像头编号' },
    ],
    params: [
      {
        key: 'model_type',
        label: '特征模型类型',
        type: 'enum',
        default: '多模态',
        options: [
          { value: '图像', label: '图像' },
          { value: '文本', label: '文本' },
          { value: '多模态', label: '多模态' },
        ],
        desc: '抽取特征的模型类型，决定后续渲染的参数组',
      },
      { key: 'model_name', label: '特征模型', type: 'string', default: 'multimodal-base-v3', desc: '抽取特征使用的模型版本' },
      { key: 'batch_size', label: '批大小', type: 'number', default: 64, desc: '单批推理样本数' },
    ],
    groups: [
      {
        driver: 'model_type',
        value: '图像',
        params: [
          { key: 'image_size', label: '输入尺寸', type: 'string', default: '224x224', desc: '图像特征提取的输入分辨率' },
        ],
      },
      {
        driver: 'model_type',
        value: '文本',
        params: [
          { key: 'max_token', label: '最大 token 数', type: 'number', default: 512, desc: '单条文本的最大 token 长度' },
        ],
      },
      {
        driver: 'model_type',
        value: '多模态',
        params: [
          { key: 'fusion_mode', label: '融合方式', type: 'enum', default: '加权求和', options: [
            { value: '加权求和', label: '加权求和' },
            { value: '拼接', label: '拼接' },
          ], desc: '图像与文本特征的融合方式' },
        ],
      },
    ],
  },
  {
    id: 'op_cross_modal_match',
    name: '跨模态匹配',
    category: '多模态',
    desc: '在同一空间内计算图像与文本向量的匹配得分，产出跨模态候选对。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'image_emb', type: 'vector<float32>[512]', required: true, desc: '图像向量' },
      { name: 'text_emb', type: 'vector<float32>[512]', required: true, desc: '文本向量' },
    ],
    outputs: [
      { name: 'query_emb', type: 'vector<float32>[512]', required: true, desc: '跨模态融合后的查询向量' },
      { name: 'camera_id', type: 'string', required: true, desc: '查询来源摄像头编号' },
      { name: 'match_score', type: 'float64', required: true, desc: '匹配得分，取值 0~1' },
      { name: 'pair_id', type: 'string', desc: '图文对标识' },
    ],
    params: [
      { key: 'threshold', label: '匹配阈值', type: 'number', default: 0.75, desc: '低于该得分的候选对被丢弃' },
    ],
  },
  {
    id: 'op_feature_transform',
    name: '特征转换',
    category: '特征工程',
    desc: '对向量做归一化、投影或降维等变换，保持下游检索契约兼容。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'embedding', type: 'vector<float32>[512]', required: true, desc: '待变换的向量列' },
    ],
    outputs: [
      { name: 'embedding', type: 'vector<float32>[512]', required: true, desc: '变换后的向量列' },
      { name: 'norm', type: 'float64', desc: '变换前的向量模长' },
    ],
    params: [
      { key: 'normalize', label: 'L2 归一化', type: 'boolean', default: true, desc: '是否对向量做 L2 归一化' },
      { key: 'target_dim', label: '目标维度', type: 'number', default: 512, desc: '投影后的向量维度' },
    ],
  },
  {
    id: 'op_retrieve_fusion',
    name: '检索融合',
    category: '检索',
    desc: '按多目标关键词分别向量化检索，再以 RRF 融合各路 top-k 召回结果。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'image_emb', type: 'vector<float32>[512]', required: true, desc: '候选帧图像向量' },
      { name: 'camera_id', type: 'string', required: true, desc: '摄像头编号，用于标量过滤' },
    ],
    outputs: [
      { name: 'topk_id', type: 'string', required: true, desc: '融合排序后的候选帧标识' },
      { name: 'topk_score', type: 'float64', required: true, desc: '融合后的最终得分' },
    ],
    params: [
      {
        key: 'query_texts',
        label: 'query_texts',
        type: 'list',
        default: ['明火', '浓烟', '火光', '火焰蔓延'],
        desc: '多目标关键词列表，每词独立 embedding',
      },
      {
        key: 'scalar_filter',
        label: 'scalar_filter',
        type: 'string',
        default: 'camera_id IN (1001, 1002, 1003)',
        desc: '标量过滤条件',
      },
      { key: 'top_k', label: 'top_k', type: 'number', default: 500, desc: '每路查询向量 top-k' },
      { key: 'rrf_k', label: 'rrf_k', type: 'number', default: 60, desc: 'RRF 融合常数' },
      { key: 'use_index', label: 'use_index', type: 'boolean', default: true, desc: '是否使用向量索引' },
    ],
    groups: [
      {
        driver: 'use_index',
        value: 'true',
        params: [
          {
            key: 'index_type',
            label: '索引类型',
            type: 'enum',
            default: 'IVF-PQ',
            options: [
              { value: 'IVF-PQ', label: 'IVF-PQ' },
              { value: 'HNSW', label: 'HNSW' },
              { value: 'FLAT', label: 'FLAT' },
            ],
            desc: '检索使用的向量索引类型，关闭索引时不生效',
          },
        ],
      },
    ],
  },
  {
    id: 'op_metadata_extract',
    name: '元数据提取',
    category: '数据接入',
    desc: '从人工确认后的回写表中提取时间戳、来源等业务元信息，供下游写表使用。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'image_ref', type: 'string', required: true, desc: '人工确认回写表中的帧图片引用路径' },
      { name: 'meta_json', type: 'string', required: true, desc: '人工确认回写表中的业务元信息' },
    ],
    outputs: [
      { name: 'meta_json', type: 'string', required: true, desc: '业务元信息，JSON 字符串' },
      { name: 'image_ref', type: 'string', desc: '帧图片引用路径' },
      { name: 'ts', type: 'timestamp[us]', desc: '采集时间' },
    ],
    params: [
      { key: 'time_field', label: '时间字段', type: 'string', default: 'event_time', desc: '用于解析采集时间的字段名' },
    ],
  },
  {
    id: 'op_data_annotate',
    name: '数据标注',
    category: '标注',
    desc: '跑完自动预标注后按置信度分流：高置信度直接写入结果表，低置信度进入标注任务等待人工确认。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'topk_id', type: 'string', required: true, desc: '检索融合筛出的候选帧标识' },
      { name: 'topk_score', type: 'float64', required: true, desc: '候选帧的融合得分' },
    ],
    outputs: [
      { name: 'image_ref', type: 'string', required: true, desc: '候选帧图片引用路径' },
      { name: 'label', type: 'string', required: true, desc: '预标注类别' },
      { name: 'bbox', type: 'string', desc: '预标注框坐标，JSON 数组' },
      { name: 'annotated_at', type: 'timestamp[us]', desc: '标注完成时间' },
      { name: 'task_id', type: 'string', desc: '低置信度样本生成的标注任务 ID' },
    ],
    params: [
      {
        key: 'auto_model',
        label: '预标注模型',
        type: 'string',
        default: 'fire-smoke-det-v2',
        desc: '自动预标注使用的模型版本',
      },
      {
        key: 'confidence_threshold',
        label: '置信度阈值',
        type: 'number',
        default: 0.5,
        desc: '不低于该值自动通过并写入结果表，低于该值进入标注任务等待人工确认',
      },
      {
        key: 'suspend_policy',
        label: '挂起策略',
        type: 'enum',
        default: 'suspend',
        options: [
          { value: 'never', label: 'never（不挂起）' },
          { value: 'suspend', label: 'suspend（挂起等待人工确认）' },
          { value: 'split', label: 'split（拆分下游由人工触发）' },
        ],
        desc: '存在待确认样本时管道的处理方式',
      },
    ],
  },
  {
    id: 'op_manual_confirm',
    name: '人工确认',
    category: '标注',
    desc: '接收标注任务的预标注结果，人工确认后回写结果表，输出 schema 即回写表结构。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'image_ref', type: 'string', required: true, desc: '帧图片引用路径' },
      { name: 'label', type: 'string', required: true, desc: '机器预标注类别' },
      { name: 'bbox', type: 'string', required: true, desc: '机器预标注框，JSON 数组' },
    ],
    outputs: [
      { name: 'image_ref', type: 'string', required: true, desc: '确认后的帧图片引用路径' },
      { name: 'label', type: 'string', required: true, desc: '确认后的标注类别' },
      { name: 'bbox', type: 'string', desc: '确认后的标注框，JSON 数组' },
      { name: 'meta_json', type: 'string', required: true, desc: '业务元信息，JSON 字符串' },
      { name: 'annotated_at', type: 'timestamp[us]', desc: '人工确认时间' },
      { name: 'excluded', type: 'boolean', desc: '是否被驳回（不写入结果表）' },
    ],
    params: [
      {
        key: 'writeback_table',
        label: '回写表',
        type: 'string',
        default: 'fire_frames_labeled',
        desc: '人工确认结果写入的结果表',
      },
    ],
  },
  {
    id: 'op_write_table',
    name: '写表',
    category: '数据接入',
    desc: '把管道产出写入目标表，作为管道终点，产出最终数据集。',
    runtime: 'registry.cn-beijing.aliyuncs.com/hexadb/op-runtime:py3.11-v1',
    builtin: true,
    inputs: [
      { name: 'meta_json', type: 'string', required: true, desc: '上游提取的业务元信息' },
      { name: 'image_ref', type: 'string', desc: '帧图片引用路径' },
      { name: 'label', type: 'string', desc: '标注类别' },
      { name: 'bbox', type: 'string', desc: '标注框坐标，JSON 数组' },
    ],
    outputs: [
      { name: 'table_name', type: 'string', required: true, desc: '写入的目标表名' },
      { name: 'row_count', type: 'bigint', desc: '写入的行数' },
    ],
    params: [
      {
        key: 'target_table',
        label: '目标表',
        type: 'string',
        default: 'fire_dataset_v3',
        desc: '管道最终产出的数据集表',
      },
      {
        key: 'write_mode',
        label: '写入模式',
        type: 'enum',
        default: '追加',
        options: [
          { value: '追加', label: '追加' },
          { value: '覆盖', label: '覆盖' },
        ],
        desc: '重复运行时写入目标表的方式',
      },
    ],
  },
]

export function getOperator(id: string): DevOperator | undefined {
  return DEV_OPERATORS.find((op) => op.id === id)
}

/** 校验一条连线是否满足契约：下游必填输入列需在上游输出列中存在同名同类型列 */
export function checkContract(
  from: DevOperator | undefined,
  to: DevOperator | undefined,
): { valid: boolean; message: string } {
  if (!from || !to) return { valid: false, message: '算子不存在' }
  const missing = to.inputs
    .filter((col) => col.required)
    .filter((col) => !from.outputs.some((o) => o.name === col.name && o.type === col.type))
  if (!missing.length) return { valid: true, message: '契约校验通过' }
  return {
    valid: false,
    message: `契约不匹配：缺少 ${missing.map((c) => c.name).join('、')}`,
  }
}
