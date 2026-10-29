<script setup lang="ts">
import { computed, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import GrantModal from '@/components/perm/GrantModal.vue'
import { useLakeStore } from '@/stores/lake'
import { usePermissionStore } from '@/stores/permissions'
import { useUiStore } from '@/stores/ui'

const lake = useLakeStore()
const perm = usePermissionStore()
const ui = useUiStore()

const grantOpen = ref(false)
const grantKeyId = ref<string | null>(null)

const key = computed(() => perm.selectedKey)
const grants = computed(() => key.value?.grants ?? [])
const emptyText = computed(() =>
  key.value
    ? '该 API Key 还没有任何数据目录 / 数据库授权，点击右上角「授予权限」添加。'
    : '暂无 API Key，请先在「API Key 管理」中创建。',
)

function openGrantModal() {
  grantKeyId.value = perm.selectedKeyId
  grantOpen.value = true
}

function onKeyChange(event: Event) {
  perm.selectedKeyId = (event.target as HTMLSelectElement).value
}

function togglePermission(grantId: string) {
  const current = perm.selectedKey
  if (!current) return
  const grant = current.grants.find((g) => g.id === grantId)
  if (!grant) return

  const nextPermission = grant.permission === 'write' ? 'read' : 'write'
  const inherited =
    grant.scope === 'db' ? perm.effectiveGrant(current, grant.scopeId, grant.id) : null
  if (inherited && perm.permissionRank(nextPermission) <= perm.permissionRank(inherited.permission)) {
    ui.toast(
      `该数据库已从「${perm.scopeLabel(inherited)}」继承${perm.permissionLabel(
        inherited.permission,
      )}权限，无法降级为${perm.permissionLabel(nextPermission)}`,
    )
    return
  }
  perm.toggleGrantPermission(current, grant)
  ui.toast(`权限已设为${perm.permissionLabel(grant.permission)}`)
}

function revoke(grantId: string) {
  const current = perm.selectedKey
  if (!current) return
  const grant = current.grants.find((g) => g.id === grantId)
  if (!grant) return
  const ok = window.confirm(`确定撤销该 API Key 对 "${perm.scopeLabel(grant)}" 的授权吗？`)
  if (!ok) return
  perm.revokeGrant(current, grant.id)
  ui.toast('授权已撤销')
}
</script>

<template>
  <main class="main">
    <header class="page-header">
      <h1>授权管理</h1>
      <p>为指定 API Key 授予 Catalog 或单个数据库的只读 / 读写权限；Catalog 级授权会自动继承到该目录下的所有数据库。</p>
    </header>

    <section class="content">
      <div class="toolbar">
        <div class="grant-key-picker">
          <label for="grantKeySelect">API Key</label>
          <select
            id="grantKeySelect"
            :value="perm.selectedKeyId"
            :disabled="!perm.keys.length"
            @change="onKeyChange"
          >
            <option v-if="!perm.keys.length" value="">暂无 API Key</option>
            <option v-for="item in perm.keys" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <button class="btn-primary" type="button" :disabled="!perm.keys.length" @click="openGrantModal">
          <SvgIcon name="plus" />
          授予权限
        </button>
      </div>

      <div class="table-card">
        <div class="table-scroll">
          <table class="data-table perm-table grant-table">
            <colgroup>
              <col style="width: 20%">
              <col style="width: 30%">
              <col style="width: 16%">
              <col style="width: 20%">
              <col style="width: 14%">
            </colgroup>
            <thead>
              <tr>
                <th>所属数据目录</th>
                <th>授权对象</th>
                <th>权限</th>
                <th>授予时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="grant in grants" :key="grant.id">
                <td>
                  {{
                    grant.scope === 'cat'
                      ? lake.catalogById(grant.scopeId).name
                      : lake.catalogById(lake.dbCatalogId(lake.dbById(grant.scopeId))).name
                  }}
                </td>
                <td>
                  <div class="perm-ns mono">{{ perm.scopeLabel(grant) }}</div>
                  <div class="perm-scope">
                    <span v-if="grant.scope === 'cat'" class="tag blue">数据目录</span>
                    {{ grant.scope === 'cat' ? '含全部数据库' : '仅该数据库' }}
                  </div>
                </td>
                <td>
                  <span v-if="grant.permission === 'write'" class="tag blue">读写</span>
                  <span v-else class="tag">只读</span>
                </td>
                <td>
                  <span class="perm-time">{{ grant.grantedAt }}</span>
                </td>
                <td>
                  <div class="actions">
                    <button class="link-btn" type="button" @click="togglePermission(grant.id)">
                      {{ grant.permission === 'write' ? '设为只读' : '设为读写' }}
                    </button>
                    <button class="link-btn danger-link" type="button" @click="revoke(grant.id)">
                      撤销
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!grants.length">
                <td class="empty-cell" colspan="5">{{ emptyText }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </main>

  <GrantModal :open="grantOpen" :key-id="grantKeyId" @close="grantOpen = false" />
</template>
