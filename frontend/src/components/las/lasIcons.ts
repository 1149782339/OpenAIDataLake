/**
 * LAS（AI 数据湖服务）控制台专用图标。
 * 与 icons.ts 保持同样的 IconDef 结构，便于 SvgIcon 直接渲染。
 */

import type { IconDef } from '@/components/icons'

const STROKE = {
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '1.7',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
} as const

const FILL = {
  fill: 'currentColor',
  stroke: 'none',
} as const

export const LAS_ICONS: Record<string, IconDef> = {
  /* 模态 */
  modalText: {
    attrs: { ...STROKE },
    body: '<path d="M4.5 6.5h15M12 6.5v11M9 17.5h6"/>',
  },
  modalImage: {
    attrs: { ...STROKE },
    body:
      '<rect x="3.5" y="5" width="17" height="14" rx="2"/>' +
      '<circle cx="9" cy="10" r="1.6"/>' +
      '<path d="m4.5 17 4.8-4.6 3.4 3.1 2.6-2.2 4.2 3.7"/>',
  },
  modalAudio: {
    attrs: { ...STROKE },
    body:
      '<path d="M9 18V6.5l9-2V16"/>' +
      '<circle cx="7" cy="18" r="2.4"/>' +
      '<circle cx="16" cy="16" r="2.4"/>',
  },
  modalVideo: {
    attrs: { ...STROKE },
    body:
      '<rect x="3" y="6" width="13" height="12" rx="2"/>' +
      '<path d="m16 11 5-3v8l-5-3Z"/>',
  },
  /* 数据集格式 */
  fmtLance: {
    attrs: { ...STROKE },
    body:
      '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/>' +
      '<path d="M3.5 9h17M9 9v10.5M14.5 9v10.5"/>',
  },
  fmtIceberg: {
    attrs: { ...STROKE },
    body:
      '<path d="M12 3.2 20.5 9 12 14.8 3.5 9 12 3.2Z"/>' +
      '<path d="M12 10.5v10.3M6.6 10.9v6.6M17.4 10.9v6.6"/>',
  },
  fmtCsv: {
    attrs: { ...STROKE },
    body:
      '<rect x="3.5" y="4.5" width="17" height="15" rx="2"/>' +
      '<path d="M3.5 9h17M9 9v10.5M15 9v10.5"/>',
  },
  fmtJsonl: {
    attrs: { ...STROKE },
    body: '<path d="M9.5 4.5H8a2.5 2.5 0 0 0-2.5 2.5v2.8a2 2 0 0 1-2 2 2 2 0 0 1 2 2v2.7A2.5 2.5 0 0 0 8 19.5h1.5M14.5 4.5H16a2.5 2.5 0 0 1 2.5 2.5v2.8a2 2 0 0 0 2 2 2 2 0 0 0-2 2v2.7a2.5 2.5 0 0 1-2.5 2.5h-1.5"/>',
  },
  fmtParquet: {
    attrs: { ...STROKE },
    body:
      '<path d="M4 8h16M4 12h16M4 16h16"/>' +
      '<path d="M7 4.5v15M12 4.5v15M17 4.5v15"/>',
  },
  fmtImage: {
    attrs: { ...STROKE },
    body:
      '<path d="M3.5 8.5h3l1.5-2.5h8l1.5 2.5h3v9a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2Z"/>' +
      '<circle cx="12" cy="13" r="3.2"/>',
  },
  fmtAudio: {
    attrs: { ...STROKE },
    body:
      '<path d="M4 9.5h3.5L12 5.5v13l-4.5-4H4Z"/>' +
      '<path d="M15.5 9.5a4 4 0 0 1 0 5M18 7a7.5 7.5 0 0 1 0 10"/>',
  },
  fmtVideo: {
    attrs: { ...STROKE },
    body:
      '<rect x="3" y="6" width="13" height="12" rx="2"/>' +
      '<path d="m16 11 5-3v8l-5-3Z"/>',
  },
  fmtText: {
    attrs: { ...STROKE },
    body: '<path d="M4.5 6.5h15M12 6.5v11M9 17.5h6"/>',
  },
  fmtLerobot: {
    attrs: { ...STROKE },
    body:
      '<rect x="4.5" y="8" width="15" height="11" rx="3"/>' +
      '<circle cx="9" cy="13.5" r="1.3"/>' +
      '<circle cx="15" cy="13.5" r="1.3"/>' +
      '<path d="M12 8V5.5M9.5 5.5h5"/>',
  },
  /* 其它 */
  tag: {
    attrs: { ...STROKE },
    body:
      '<path d="M4.5 11.2V5.5a1 1 0 0 1 1-1h5.7a1 1 0 0 1 .7.3l7.3 7.3a1 1 0 0 1 0 1.4l-5.7 5.7a1 1 0 0 1-1.4 0L4.8 11.9a1 1 0 0 1-.3-.7Z"/>' +
      '<circle cx="9" cy="9" r="1.2"/>',
  },
  doc: {
    attrs: { ...STROKE },
    body:
      '<rect x="4" y="3.5" width="16" height="17" rx="2"/>' +
      '<path d="M8.5 8.5h7M8.5 12.5h7M8.5 16h4"/>',
  },
  guide: {
    attrs: { ...STROKE },
    body:
      '<circle cx="12" cy="12" r="8.5"/>' +
      '<path d="M9.5 9.5h5M9.5 13h5M12 16.5v-4"/>',
  },
  calendar: {
    attrs: { ...STROKE },
    body:
      '<rect x="3.5" y="5.5" width="17" height="15" rx="2"/>' +
      '<path d="M3.5 10h17M8 3.5v4M16 3.5v4"/>',
  },
  question: {
    attrs: { ...STROKE },
    body:
      '<circle cx="12" cy="12" r="8.5"/>' +
      '<path d="M9.8 9.6a2.3 2.3 0 1 1 2.9 2.2c-.5.2-.7.6-.7 1.1v.5M12 16.6v.4"/>',
  },
  external: {
    attrs: { ...STROKE },
    body: '<path d="M14 4.5h5.5V10M19 5l-7.5 7.5M17.5 14v4.5a1.5 1.5 0 0 1-1.5 1.5H5.5A1.5 1.5 0 0 1 4 18.5V8a1.5 1.5 0 0 1 1.5-1.5H10"/>',
  },
  menuFold: {
    attrs: { ...STROKE },
    body: '<path d="M4 7h16M4 12h10M4 17h16"/>',
  },
  chevronRight: {
    attrs: { ...STROKE },
    body: '<path d="m9 5 7 7-7 7"/>',
  },
  chevronLeft: {
    attrs: { ...STROKE },
    body: '<path d="m15 5-7 7 7 7"/>',
  },
  chevronDown: {
    attrs: { ...STROKE },
    body: '<path d="m6 9 6 6 6-6"/>',
  },
  search: {
    attrs: { ...STROKE },
    body: '<circle cx="11" cy="11" r="6.8"/><path d="m20 20-3.6-3.6"/>',
  },
  filter: {
    attrs: { ...STROKE },
    body: '<path d="M3.5 5h17l-6.6 7.6v5.3l-3.8 2.1v-7.4L3.5 5Z"/>',
  },
  plus: {
    attrs: { ...STROKE },
    body: '<path d="M12 5v14M5 12h14"/>',
  },
  more: {
    attrs: { ...FILL },
    body: '<circle cx="5" cy="12" r="1.7"/><circle cx="12" cy="12" r="1.7"/><circle cx="19" cy="12" r="1.7"/>',
  },
  play: {
    attrs: { ...FILL },
    body: '<path d="M7 4.8 19 12 7 19.2V4.8Z"/>',
  },
  refresh: {
    attrs: { ...STROKE },
    body: '<path d="M20 11a8 8 0 1 0-2.3 6.1"/><path d="M20 4v7h-7"/>',
  },
  db: {
    attrs: { ...STROKE },
    body:
      '<ellipse cx="12" cy="6" rx="7.5" height="2.8"/>' +
      '<path d="M4.5 6v12c0 1.55 3.35 2.8 7.5 2.8s7.5-1.25 7.5-2.8V6"/>' +
      '<path d="M4.5 12c0 1.55 3.35 2.8 7.5 2.8s7.5-1.25 7.5-2.8"/>',
  },
  func: {
    attrs: { ...STROKE },
    body:
      '<path d="M8.5 19 13 5"/>' +
      '<path d="M5.5 10.5h9"/>' +
      '<path d="M14 9.5c2.6 0 3.5 1.4 3.5 4.5s-.9 5-3.5 5"/>',
  },
  operators: {
    attrs: { ...STROKE },
    body:
      '<rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/>' +
      '<rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/>' +
      '<rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/>' +
      '<path d="M17 14v6M14 17h6"/>',
  },
  dataset: {
    attrs: { ...STROKE },
    body:
      '<ellipse cx="12" cy="6" rx="7.5" height="2.8"/>' +
      '<path d="M4.5 6v12c0 1.55 3.35 2.8 7.5 2.8s7.5-1.25 7.5-2.8V6"/>' +
      '<path d="M4.5 12c0 1.55 3.35 2.8 7.5 2.8s7.5-1.25 7.5-2.8"/>',
  },
  lake: {
    attrs: { ...STROKE },
    body:
      '<path d="M3.5 8.5c1.8 0 1.8 1.6 3.5 1.6s1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6 1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6"/>' +
      '<path d="M3.5 14c1.8 0 1.8 1.6 3.5 1.6s1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6 1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6"/>' +
      '<path d="M3.5 19.5c1.8 0 1.8 1.6 3.5 1.6s1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6 1.8-1.6 3.5-1.6 1.8 1.6 3.5 1.6"/>',
  },
  empty: {
    attrs: { ...STROKE },
    body:
      '<path d="M4.5 7.5 12 3.5l7.5 4v9L12 20.5l-7.5-4Z"/>' +
      '<path d="m4.5 7.5 7.5 4 7.5-4M12 11.5v9"/>',
  },
  checkSmall: {
    attrs: { ...FILL },
    body: '<path d="M12 2.5a9.5 9.5 0 1 0 0 19 9.5 9.5 0 0 0 0-19Zm-1.2 13.6-4.4-4.4 1.6-1.6 2.8 2.8 5.2-5.2 1.6 1.6-6.8 6.8Z"/>',
  },
  close: {
    attrs: { ...FILL },
    body:
      '<path d="M12 10.58 6.4 5 5 6.4l5.58 5.6L5 17.6 6.4 19l5.6-5.58 5.6 5.58 1.4-1.4L13.4 12 19 6.4 17.6 5Z"/>',
  },
}

/** 模态 chip 的图标 */
export const MODALITY_ICON: Record<string, string> = {
  文本: 'modalText',
  图片: 'modalImage',
  音频: 'modalAudio',
  视频: 'modalVideo',
}

/** 数据集格式 chip 的图标 */
export const FORMAT_ICON: Record<string, string> = {
  Lance: 'fmtLance',
  Iceberg: 'fmtIceberg',
  CSV: 'fmtCsv',
  JSONL: 'fmtJsonl',
  Parquet: 'fmtParquet',
  Image: 'fmtImage',
  Audio: 'fmtAudio',
  Video: 'fmtVideo',
  Text: 'fmtText',
  Lerobot: 'fmtLerobot',
}
