/**
 * 图标库：与 tmp/demo.html 中的内联 SVG 保持一致。
 * 每个图标拆成「根元素属性」与「内部内容」两部分，便于在 Vue 模板中
 * 直接渲染为 <svg> 根节点，避免额外包裹元素影响原有的 CSS 选择器。
 */

export interface IconDef {
  /** 根 <svg> 元素上的属性 */
  attrs: Record<string, string>
  /** <svg> 内部内容 */
  body: string
  /** viewBox，默认 0 0 24 24 */
  viewBox?: string
}

const STROKE = {
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '1.8',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
} as const

export const ICONS: Record<string, IconDef> = {
  copy: {
    attrs: { ...STROKE },
    body:
      '<rect x="9" y="9" width="11.5" height="11.5" rx="2"/>' +
      '<path d="M5.5 15H4.5a1 1 0 0 1-1-1V4.5a1 1 0 0 1 1-1H14a1 1 0 0 1 1 1v1"/>',
  },
  more: {
    attrs: { fill: 'currentColor', stroke: 'none' },
    body:
      '<circle cx="5" cy="12" r="1.7"/>' +
      '<circle cx="12" cy="12" r="1.7"/>' +
      '<circle cx="19" cy="12" r="1.7"/>',
  },
  layers: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linejoin': 'round' },
    body: '<path d="m12 2.5 9 5-9 5-9-5 9-5Z"/><path d="m3 12.5 9 5 9-5"/>',
  },
  chevLeft: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="m15 5-7 7 7 7"/>',
  },
  chevRight: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="m9 5 7 7-7 7"/>',
  },
  chevDown: {
    attrs: {
      width: '14',
      height: '14',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    },
    body: '<path d="m6 9 6 6 6-6"/>',
  },
  chevUp: {
    attrs: {
      width: '14',
      height: '14',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    },
    body: '<path d="m6 15 6-6 6 6"/>',
  },
  search: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round' },
    body: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.6-3.6"/>',
  },
  plus: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2.2', 'stroke-linecap': 'round' },
    body: '<path d="M12 5v14M5 12h14"/>',
  },
  filter: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.9', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="M3.5 5h17l-6.6 7.6v5.3l-3.8 2.1v-7.4L3.5 5Z"/>',
  },
  refresh: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.9', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="M20 11a8 8 0 1 0-2.3 6.1"/><path d="M20 4v7h-7"/>',
  },
  back: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="m15 5-7 7 7 7"/>',
  },
  /* Catalog / 数据库 / 表 专用图标 */
  db: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body:
      '<ellipse cx="12" cy="5.5" rx="7.5" ry="2.6"/>' +
      '<path d="M4.5 5.5v13c0 1.45 3.36 2.6 7.5 2.6s7.5-1.15 7.5-2.6v-13"/>' +
      '<path d="M4.5 12c0 1.45 3.36 2.6 7.5 2.6s7.5-1.15 7.5-2.6"/>',
  },
  folder: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linejoin': 'round' },
    body: '<path d="M3.5 6.5a2 2 0 0 1 2-2h4l2 2.5h7a2 2 0 0 1 2 2v8.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2v-11Z"/>',
  },
  table: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linejoin': 'round' },
    body: '<rect x="3.5" y="4" width="17" height="16" rx="2"/><path d="M3.5 9h17M9.5 9v11"/>',
  },
  catalog: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.7', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body:
      '<path d="M3.5 6.5a2 2 0 0 1 2-2h4l2 2.5h7a2 2 0 0 1 2 2v8.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2v-11Z"/>' +
      '<path d="M6.5 12.5h11"/>',
  },
  /* 侧边栏 */
  navDatasets: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linejoin': 'round' },
    body: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M9 9v11"/>',
  },
  navCatalog: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linejoin': 'round' },
    body:
      '<ellipse cx="12" cy="5.5" rx="7.5" ry="2.7"/>' +
      '<path d="M4.5 5.5v13c0 1.5 3.36 2.7 7.5 2.7s7.5-1.2 7.5-2.7v-13"/>' +
      '<path d="M4.5 12c0 1.5 3.36 2.7 7.5 2.7s7.5-1.2 7.5-2.7"/>',
  },
  navShield: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body:
      '<path d="M12 3 4.8 6v5.3c0 4.5 3 8.2 7.2 9.7 4.2-1.5 7.2-5.2 7.2-9.7V6L12 3Z"/>' +
      '<path d="m9 12 2 2 4-4"/>',
  },
  brand: {
    attrs: { fill: 'none', stroke: 'currentColor', 'stroke-width': '1.7', 'stroke-linecap': 'round', 'stroke-linejoin': 'round' },
    body: '<path d="M12 2.6 20.4 7.3v9.4L12 21.4 3.6 16.7V7.3L12 2.6Z"/><path d="M3.6 7.3 12 12l8.4-4.7M12 12v9.4"/>',
  },
}

/** Catalog 树中使用的图标集合 */
export const META_ICONS = {
  chevDown: ICONS.chevDown,
  chevRight: ICONS.chevRight,
  db: ICONS.db,
  folder: ICONS.folder,
  table: ICONS.table,
  catalog: ICONS.catalog,
} as const
