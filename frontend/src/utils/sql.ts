/** SQL 语法高亮与（演示用）SQL 执行 */

export interface SqlColumn {
  name: string
  type: string
}

export interface SqlResult {
  columns: string[]
  rows: (string | number | null)[][]
  elapsed: number
}

const KEYWORDS = [
  'select', 'from', 'where', 'join', 'left', 'right', 'inner', 'outer', 'full', 'on', 'as', 'and',
  'or', 'not', 'null', 'is', 'in', 'like', 'between', 'order', 'by', 'group', 'having', 'limit',
  'offset', 'distinct', 'all', 'union', 'case', 'when', 'then', 'else', 'end', 'insert', 'into',
  'values', 'update', 'set', 'delete', 'with', 'asc', 'desc', 'using', 'exists', 'interval',
]

const TOKEN_RE =
  /(--[^\n]*|\/\*[\s\S]*?\*\/)|('(?:[^'\\]|\\.)*'|"(?:[^"\\]|\\.)*")|\b(\d+(?:\.\d+)?)\b|\b([A-Za-z_][A-Za-z0-9_]*)\b/g

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/** 将 SQL 文本渲染为带高亮的 HTML */
export function highlightSql(sql: string): string {
  let out = ''
  let last = 0
  TOKEN_RE.lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = TOKEN_RE.exec(sql)) !== null) {
    out += escapeHtml(sql.slice(last, match.index))
    const [text, comment, str, num, word] = match
    if (comment) {
      out += `<span class="sql-com">${escapeHtml(text)}</span>`
    } else if (str) {
      out += `<span class="sql-str">${escapeHtml(text)}</span>`
    } else if (num) {
      out += `<span class="sql-num">${escapeHtml(text)}</span>`
    } else if (word && KEYWORDS.includes(word.toLowerCase())) {
      out += `<span class="sql-kw">${escapeHtml(text)}</span>`
    } else {
      out += escapeHtml(text)
    }
    last = match.index + text.length
  }
  out += escapeHtml(sql.slice(last))
  return out
}

/** 去掉注释与分号，判断是否为 SELECT 查询 */
export function isSelectQuery(sql: string): boolean {
  const cleaned = sql
    .replace(/--[^\n]*/g, ' ')
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .trim()
    .replace(/;\s*$/, '')
    .trim()
  return /^select\b/i.test(cleaned)
}

/** 解析 FROM 后的表名（取最后一段，忽略别名） */
export function parseFromTable(sql: string): string | null {
  const cleaned = sql.replace(/--[^\n]*/g, ' ').replace(/\/\*[\s\S]*?\*\//g, ' ')
  const m = cleaned.match(/\bfrom\s+([A-Za-z_][\w.]*)/i)
  if (!m) return null
  const parts = m[1].split('.')
  return parts[parts.length - 1] ?? null
}

/** 解析 SELECT 列表中的字段名 */
export function parseSelectColumns(sql: string): string[] {
  const cleaned = sql.replace(/--[^\n]*/g, ' ').replace(/\/\*[\s\S]*?\*\//g, ' ').trim()
  const m = cleaned.match(/^select\s+([\s\S]+?)\s+from\b/i)
  if (!m) return []
  return m[1]
    .split(',')
    .map((item) => item.trim().split(/\s+as\s+/i)[0].trim().replace(/^[`"']|[`"']$/g, ''))
    .filter(Boolean)
}

function mockValue(type: string, index: number): string | number | null {
  const t = type.toLowerCase()
  if (t.includes('int')) return 1000 + index * 7
  if (t.includes('float') || t.includes('double') || t.includes('decimal')) return 12.5 + index
  if (t.includes('bool')) return index % 2 === 0 ? 'true' : 'false'
  if (t.includes('timestamp') || t.includes('date')) return `2026-10-2${index % 9} 08:30:00`
  if (t.includes('vector')) return `[0.12, 0.34, 0.56]`
  if (t.includes('binary')) return `<binary ${index + 1} bytes>`
  return `值-${index + 1}`
}

/**
 * 演示环境下的 SQL 执行：按目标表的 Schema 生成结果集。
 * 未匹配到已知表时使用通用字段。
 */
export function runMockSql(
  sql: string,
  knownTables: { name: string; schema: SqlColumn[] }[],
): { ok: true; result: SqlResult } | { ok: false; message: string } {
  const trimmed = sql.trim()
  if (!trimmed) return { ok: false, message: '请输入要执行的 SQL' }
  if (!isSelectQuery(trimmed)) {
    return { ok: false, message: '演示环境仅支持 SELECT 查询' }
  }

  const tableName = parseFromTable(trimmed)
  const table = knownTables.find(
    (t) => t.name.toLowerCase() === (tableName ?? '').toLowerCase(),
  )
  const schema: SqlColumn[] = table?.schema ?? [
    { name: 'id', type: 'int64' },
    { name: 'name', type: 'string' },
    { name: 'value', type: 'double' },
    { name: 'created_at', type: 'timestamp[us]' },
  ]

  const selected = parseSelectColumns(trimmed)
  const columns = selected.length
    ? selected.map((name) => {
        const hit = schema.find((c) => c.name.toLowerCase() === name.toLowerCase())
        return hit ? hit.name : name
      })
    : schema.map((c) => c.name)

  const rowCount = 8
  const rows: (string | number | null)[][] = []
  for (let i = 0; i < rowCount; i += 1) {
    rows.push(
      columns.map((name) => {
        const col = schema.find((c) => c.name === name)
        return mockValue(col?.type ?? 'string', i)
      }),
    )
  }

  return {
    ok: true,
    result: { columns, rows, elapsed: 120 + Math.floor(Math.random() * 180) },
  }
}
