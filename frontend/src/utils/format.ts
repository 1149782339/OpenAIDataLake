/** 通用格式化工具 */

export function nowStamp(): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const d = new Date()
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

export function fmtNum(n: number | string | null | undefined): string {
  return Number(n || 0).toLocaleString('en-US')
}

export function maskApiKey(value: string): string {
  if (value.length <= 16) return `${value.slice(0, 4)}••••••••`
  return `${value.slice(0, 12)}••••••••${value.slice(-4)}`
}

/** 由数据集名推导合法的表名 */
export function tableNameFromDataset(name: string): string {
  let s = String(name || '')
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .slice(0, 63)
  if (!s) s = 'imported_table'
  if (!/^[a-z]/.test(s)) s = `t_${s}`
  return s
}

export function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard && window.isSecureContext) {
    return navigator.clipboard.writeText(text)
  }
  return new Promise((resolve) => {
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    try {
      document.execCommand('copy')
    } catch {
      /* 忽略复制失败 */
    }
    document.body.removeChild(area)
    resolve()
  })
}
