<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import CopyButton from '@/components/CopyButton.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import CreateKeyModal from '@/components/perm/CreateKeyModal.vue'
import KeyRevealModal from '@/components/perm/KeyRevealModal.vue'
import { usePermissionStore } from '@/stores/permissions'
import { useUiStore } from '@/stores/ui'
import { maskApiKey } from '@/utils/format'

const perm = usePermissionStore()
const ui = useUiStore()
const router = useRouter()

const createOpen = ref(false)
const revealOpen = ref(false)
const revealedName = ref('')
const revealedValue = ref('')

function handleCreated(name: string) {
  const key = perm.createApiKey(name)
  createOpen.value = false
  revealedName.value = key.name
  revealedValue.value = key.key
  revealOpen.value = true
  ui.toast('API Key 已创建')
}

function grant(keyId: string) {
  const key = perm.apiKeyById(keyId)
  if (!key) return
  perm.selectedKeyId = key.id
  router.push('/grants')
  ui.toast(`已选择 API Key：${key.name}`)
}

function remove(keyId: string) {
  const key = perm.apiKeyById(keyId)
  if (!key) return
  const ok = window.confirm(
    `确定删除 API Key "${key.name}" 吗？其全部数据目录 / 数据库授权也会一并移除。`,
  )
  if (!ok) return
  perm.removeApiKey(key.id)
  ui.toast('API Key 已删除')
}
</script>

<template>
  <main class="main">
    <header class="page-header">
      <h1>API Key 管理</h1>
      <p>创建用于访问数据平台的 API Key，并在「授权管理」中为它授予 Catalog / 数据库级别的读写权限。</p>
    </header>

    <section class="content">
      <div class="toolbar">
        <button class="btn-primary" type="button" @click="createOpen = true">
          <SvgIcon name="plus" />
          创建 API Key
        </button>
      </div>

      <div class="table-card">
        <div class="table-scroll">
          <table class="data-table perm-table key-table">
            <colgroup>
              <col style="width: 23%">
              <col style="width: 30%">
              <col style="width: 16%">
              <col style="width: 17%">
              <col style="width: 14%">
            </colgroup>
            <thead>
              <tr>
                <th>名称</th>
                <th>API Key</th>
                <th>覆盖数据库</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="key in perm.keys" :key="key.id">
                <td>
                  <div class="perm-name">{{ key.name }}</div>
                </td>
                <td>
                  <div class="perm-key">
                    <span class="mono">{{ maskApiKey(key.key) }}</span>
                    <CopyButton :value="key.key" title="复制 API Key" />
                  </div>
                </td>
                <td>
                  <span class="perm-count" title="包含通过数据目录授权继承覆盖的数据库">
                    {{ perm.coveredDbCount(key) }} 个
                  </span>
                </td>
                <td>
                  <span class="perm-time">{{ key.createdAt }}</span>
                </td>
                <td>
                  <div class="actions">
                    <button class="link-btn" type="button" @click="grant(key.id)">授权</button>
                    <button class="link-btn danger-link" type="button" @click="remove(key.id)">
                      删除
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!perm.keys.length">
                <td class="empty-cell" colspan="5">暂无 API Key，点击左上角创建。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <p class="perm-footnote">
        API Key 默认没有任何数据访问权限；Catalog 级授权会自动覆盖其下所有数据库，无需逐库重复授权。
      </p>
    </section>
  </main>

  <CreateKeyModal :open="createOpen" @close="createOpen = false" @created="handleCreated" />

  <KeyRevealModal
    :open="revealOpen"
    :name="revealedName"
    :value="revealedValue"
    @close="revealOpen = false"
  />
</template>
