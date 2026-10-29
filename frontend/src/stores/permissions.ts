import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { useLakeStore } from './lake'
import { nowStamp } from '@/utils/format'

export type Permission = 'read' | 'write'
export type GrantScope = 'cat' | 'db'

export interface Grant {
  id: string
  scope: GrantScope
  scopeId: string
  permission: Permission
  grantedAt: string
}

export interface ApiKey {
  id: string
  name: string
  key: string
  createdAt: string
  grants: Grant[]
}

function generateApiKey(): string {
  const chars = '0123456789abcdef'
  let body = ''
  for (let i = 0; i < 32; i += 1) {
    body += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return `ak_live_${body}`
}

export const usePermissionStore = defineStore('permissions', () => {
  const lake = useLakeStore()

  const keys = ref<ApiKey[]>([
    {
      id: 'key_ingest',
      name: '数据导入服务',
      key: 'ak_live_7f4c9a21d8e34b6fa05c72e91b3d4a86',
      createdAt: '2026-10-18 10:24:00',
      grants: [
        {
          id: 'grant_1',
          scope: 'db',
          scopeId: 'db_main',
          permission: 'write',
          grantedAt: '2026-10-18 10:30:00',
        },
        {
          id: 'grant_2',
          scope: 'db',
          scopeId: 'db_2',
          permission: 'read',
          grantedAt: '2026-10-20 16:05:00',
        },
      ],
    },
    {
      id: 'key_query',
      name: '训练查询服务',
      key: 'ak_live_3b8e1c5d9a724f0e86b2d47c1f9e5a30',
      createdAt: '2026-10-21 09:12:00',
      grants: [
        {
          id: 'grant_3',
          scope: 'cat',
          scopeId: 'cat_main',
          permission: 'read',
          grantedAt: '2026-10-21 09:15:00',
        },
      ],
    },
  ])

  const selectedKeyId = ref('key_ingest')
  const nextKey = ref(3)
  const nextGrant = ref(4)

  const selectedKey = computed(
    () => keys.value.find((k) => k.id === selectedKeyId.value) ?? keys.value[0] ?? null,
  )

  function apiKeyById(id: string | null | undefined): ApiKey | null {
    if (!id) return null
    return keys.value.find((k) => k.id === id) ?? null
  }

  function scopeDepth(grant: Grant) {
    return grant.scope === 'cat' ? 0 : 1
  }

  function scopeLabel(grant: Pick<Grant, 'scope' | 'scopeId'>): string {
    return grant.scope === 'cat'
      ? `${lake.catalogById(grant.scopeId).name}（数据目录）`
      : lake.dbById(grant.scopeId).name
  }

  function grantCoversDb(grant: Grant, dbId: string) {
    const db = lake.dbById(dbId)
    if (grant.scope === 'db') return grant.scopeId === db.id
    return grant.scopeId === lake.dbCatalogId(db)
  }

  function permissionRank(permission: Permission) {
    return permission === 'write' ? 2 : permission === 'read' ? 1 : 0
  }

  function permissionLabel(permission: Permission) {
    return permission === 'write' ? '读写' : '只读'
  }

  /** 计算某个 API Key 对指定数据库最终生效的授权 */
  function effectiveGrant(key: ApiKey, dbId: string, excludeId?: string | null): Grant | null {
    const db = lake.dbById(dbId)
    let best: Grant | null = null
    key.grants.forEach((grant) => {
      if (grant.id === excludeId) return
      if (!grantCoversDb(grant, db.id)) return
      if (
        !best ||
        permissionRank(grant.permission) > permissionRank(best.permission) ||
        (permissionRank(grant.permission) === permissionRank(best.permission) &&
          scopeDepth(grant) < scopeDepth(best))
      ) {
        best = grant
      }
    })
    return best
  }

  /** 移除被上层目录授权覆盖的多余授权 */
  function pruneCoveredGrants(key: ApiKey): number {
    let removed = 0
    key.grants = key.grants.filter((grant) => {
      const covered = key.grants.some((other) => {
        if (other.id === grant.id) return false
        if (scopeDepth(other) >= scopeDepth(grant)) return false
        if (permissionRank(other.permission) < permissionRank(grant.permission)) return false
        if (grant.scope === 'db') return grantCoversDb(other, lake.dbById(grant.scopeId).id)
        return other.scope === 'cat' && grant.scopeId === other.scopeId
      })
      if (covered) removed += 1
      return !covered
    })
    return removed
  }

  function coveredDbCount(key: ApiKey): number {
    let count = 0
    lake.dbs.forEach((db) => {
      if (effectiveGrant(key, db.id)) count += 1
    })
    return count
  }

  function createApiKey(name: string): ApiKey {
    const key: ApiKey = {
      id: `key_${nextKey.value++}`,
      name,
      key: generateApiKey(),
      createdAt: nowStamp(),
      grants: [],
    }
    keys.value.unshift(key)
    selectedKeyId.value = key.id
    return key
  }

  function removeApiKey(id: string) {
    keys.value = keys.value.filter((k) => k.id !== id)
    if (selectedKeyId.value === id) {
      selectedKeyId.value = keys.value.length ? keys.value[0].id : ''
    }
  }

  function addGrant(key: ApiKey, scope: GrantScope, scopeId: string, permission: Permission) {
    key.grants.push({
      id: `grant_${nextGrant.value++}`,
      scope,
      scopeId,
      permission,
      grantedAt: nowStamp(),
    })
    return pruneCoveredGrants(key)
  }

  function revokeGrant(key: ApiKey, grantId: string) {
    key.grants = key.grants.filter((g) => g.id !== grantId)
  }

  function toggleGrantPermission(key: ApiKey, grant: Grant) {
    grant.permission = grant.permission === 'write' ? 'read' : 'write'
    pruneCoveredGrants(key)
  }

  return {
    keys,
    selectedKeyId,
    selectedKey,
    apiKeyById,
    scopeDepth,
    scopeLabel,
    grantCoversDb,
    permissionRank,
    permissionLabel,
    effectiveGrant,
    pruneCoveredGrants,
    coveredDbCount,
    createApiKey,
    removeApiKey,
    addGrant,
    revokeGrant,
    toggleGrantPermission,
  }
})
