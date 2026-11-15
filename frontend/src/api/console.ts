import { api, clearCsrf } from './http'

export interface ConsoleUser {
  id: number
  username: string
  role: string
  enabled: boolean
}

export interface DirectoryItem {
  id: number
  logicalName: string
  endpoint: string | null
  directory: string | null
}

export interface DirectoryGrant {
  userId: number
  canList: boolean
  canRead: boolean
  canWrite: boolean
  canDelete: boolean
}

export function login(username: string, password: string) {
  return api<ConsoleUser>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  }).finally(clearCsrf)
}

export function listDirectories() {
  return api<DirectoryItem[]>('/api/directories')
}

export function probeDirectory(endpoint: string, accessKey: string, secretKey: string) {
  return api<{ directories: string[] }>('/api/directories/probe', {
    method: 'POST',
    body: JSON.stringify({ endpoint, accessKey, secretKey }),
  })
}

export function createDirectory(body: {
  logicalName: string
  endpoint: string
  accessKey: string
  secretKey: string
  directory: string
  grants: DirectoryGrant[]
}) {
  return api<DirectoryItem>('/api/directories', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function listUsers() {
  return api<ConsoleUser[]>('/api/users')
}

export function currentUser() {
  return api<{ id: number; username: string; role: string }>('/api/auth/me')
}

export type DataSourceEngine = 'PostgreSQL' | 'MySQL' | 'ClickHouse'

export interface DataSourceItem {
  id: number
  name: string
  engine: DataSourceEngine
  host: string
  port: number
  database: string
}

export interface DataSourceSchema {
  name: string
  tables: string[]
}

export interface DataSourceCatalog {
  name: string
  schemas: DataSourceSchema[]
  error: string | null
}

export interface DataSourceConnection {
  engine: DataSourceEngine
  host: string
  port: number
  user: string
  password: string
  database: string
}

export interface ExploreResult {
  columns: string[]
  rows: (string | number | boolean | null)[][]
  elapsed: number
}

export function listDataSources() {
  return api<DataSourceItem[]>('/api/data-sources')
}

export function dataSourceCatalog() {
  return api<DataSourceCatalog[]>('/api/data-sources/catalog')
}

export function probeDataSource(body: DataSourceConnection) {
  return api<{ ok: boolean }>('/api/data-sources/probe', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export interface DataSourceGrantInput {
  userId: number
  writable: boolean
}

export interface DataSourceGrant {
  userId: number
  username: string
  writable: boolean
}

export function createDataSource(body: DataSourceConnection & { name: string; grants: DataSourceGrantInput[] }) {
  return api<DataSourceItem>('/api/data-sources', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function listDataSourceGrants(id: number) {
  return api<DataSourceGrant[]>(`/api/data-sources/${id}/grants`)
}

export function saveDataSourceGrants(id: number, grants: DataSourceGrantInput[]) {
  return api<DataSourceGrant[]>(`/api/data-sources/${id}/grants`, {
    method: 'PUT',
    body: JSON.stringify({ grants }),
  })
}

export function runExploreQuery(sql: string) {
  return api<ExploreResult>('/api/explore/query', {
    method: 'POST',
    body: JSON.stringify({ sql }),
  })
}
