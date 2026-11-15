export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

interface CsrfToken {
  headerName: string
  token: string
}

let csrf: CsrfToken | null = null

export function clearCsrf() {
  csrf = null
}

function messageOf(payload: unknown, fallback: string) {
  if (payload && typeof payload === 'object' && 'error' in payload) {
    const error = payload.error
    if (typeof error === 'string' && error.trim()) return error
  }
  return fallback
}

async function ensureCsrf() {
  if (csrf) return csrf
  let response: Response
  try {
    response = await fetch('/api/auth/csrf')
  } catch {
    throw new ApiError(0, '无法连接控制台')
  }
  const text = await response.text()
  const payload = text ? JSON.parse(text) as unknown : null
  if (!response.ok || !payload || typeof payload !== 'object' || !('token' in payload) || !('headerName' in payload)) {
    throw new ApiError(response.status, '无法获取登录凭证')
  }
  const token = payload as CsrfToken
  csrf = { headerName: String(token.headerName), token: String(token.token) }
  return csrf
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (method !== 'GET' && method !== 'HEAD') {
    const token = await ensureCsrf()
    headers.set(token.headerName, token.token)
  }
  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, '无法连接控制台')
  }
  if (response.status === 204) return undefined as T
  const text = await response.text()
  let payload: unknown = null
  if (text) {
    try {
      payload = JSON.parse(text) as unknown
    } catch {
      throw new ApiError(response.status, '服务器返回了无法识别的响应')
    }
  }
  if (!response.ok) {
    throw new ApiError(response.status, messageOf(payload, '请求失败'))
  }
  return payload as T
}

export function errorMessage(error: unknown, fallback = '请求失败') {
  return error instanceof Error && error.message ? error.message : fallback
}
