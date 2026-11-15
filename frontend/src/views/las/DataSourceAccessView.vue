<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { ApiError, errorMessage } from '@/api/http'
import { listDataSourceGrants, listUsers, saveDataSourceGrants, type ConsoleUser } from '@/api/console'
import { useLasDataSourceStore } from '@/stores/lasDataSources'

const route = useRoute()
const router = useRouter()
const store = useLasDataSourceStore()

const users = ref<ConsoleUser[]>([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const userError = ref('')
let grantKey = 1

interface GrantDraft {
  key: number
  userId: string
  writable: boolean
}

const grants = ref<GrantDraft[]>([])

const sourceId = computed(() => Number(route.params.id))
const source = computed(() => store.items.find((item) => item.id === sourceId.value))
const clickHouse = computed(() => source.value?.engine === 'ClickHouse')

function addGrant(userId = '', writable = false) {
  grants.value.push({
    key: grantKey++,
    userId,
    writable: clickHouse.value ? false : writable,
  })
}

function removeGrant(index: number) {
  grants.value.splice(index, 1)
}

function back() {
  router.push('/catalog/sources')
}

async function load() {
  loading.value = true
  error.value = ''
  userError.value = ''
  try {
    await store.load()
    if (!source.value) {
      error.value = '数据源不存在'
      return
    }
    const [all, existing] = await Promise.all([listUsers(), listDataSourceGrants(sourceId.value)])
    users.value = all.filter((item) => item.enabled)
    grants.value = existing.map((grant) => ({
      key: grantKey++,
      userId: String(grant.userId),
      writable: clickHouse.value ? false : grant.writable,
    }))
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    error.value = errorMessage(cause, '加载权限失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  error.value = ''
  const chosen = grants.value.filter((row) => row.userId)
  if (chosen.length !== grants.value.length) {
    error.value = '请选择用户。'
    return
  }
  if (new Set(chosen.map((row) => row.userId)).size !== chosen.length) {
    error.value = '同一个用户不能重复授权。'
    return
  }
  if (clickHouse.value && chosen.some((row) => row.writable)) {
    error.value = 'ClickHouse 数据源不支持写入。'
    return
  }
  saving.value = true
  try {
    const saved = await saveDataSourceGrants(
      sourceId.value,
      chosen.map((row) => ({ userId: Number(row.userId), writable: row.writable })),
    )
    grants.value = saved.map((grant) => ({
      key: grantKey++,
      userId: String(grant.userId),
      writable: grant.writable,
    }))
    await router.push('/catalog/sources')
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: route.fullPath } })
      return
    }
    error.value = errorMessage(cause, '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="ct-page">
    <header class="ct-head">
      <button class="ct-back" type="button" title="返回" @click="back">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="ct-title">{{ source ? source.name + ' 的权限' : '数据源权限' }}</span>
    </header>

    <div class="ct-body">
      <form class="ct-form" @submit.prevent="onSubmit">
        <p v-if="loading" class="ct-hint">加载中…</p>
        <template v-else-if="source">
          <p class="ct-hint ct-grant-hint">只读可以查询，读写可以增删改。没有出现在这里的用户不能访问这个数据源。ClickHouse 只能只读。</p>
          <p v-if="userError" class="ct-error ct-block-error">{{ userError }}</p>
          <div class="ct-table-wrap">
            <table class="ct-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>权限</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-if="!grants.length">
                  <td class="ct-empty" colspan="3">还没有授权</td>
                </tr>
                <tr v-for="(row, index) in grants" :key="row.key">
                  <td>
                    <select v-model="row.userId" class="ct-input ct-select" aria-label="授权用户">
                      <option value="">选择用户</option>
                      <option v-for="item in users" :key="item.id" :value="String(item.id)">{{ item.username }}</option>
                    </select>
                  </td>
                  <td>
                    <select v-model="row.writable" class="ct-input ct-select" aria-label="权限" :disabled="clickHouse">
                      <option :value="false">只读</option>
                      <option :value="true">读写</option>
                    </select>
                  </td>
                  <td>
                    <button class="ct-link-btn ct-remove" type="button" @click="removeGrant(index)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button class="ct-link-btn" type="button" @click="addGrant()">
            <SvgIcon class="ct-link-ic" :icon="LAS_ICONS.plus" />
            <span>添加用户</span>
          </button>
        </template>
        <p v-if="error" class="ct-error ct-block-error">{{ error }}</p>
        <footer class="ct-foot">
          <button class="ct-ghost" type="button" @click="back">取消</button>
          <button class="ct-primary" type="submit" :disabled="saving || loading || !source">{{ saving ? '保存中…' : '保存' }}</button>
        </footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.ct-page { display: flex; flex-direction: column; height: 100%; min-width: 980px; background: #fff; }
.ct-head { height: 48px; flex: none; display: flex; align-items: center; gap: 8px; padding: 0 20px; border-bottom: 1px solid #e5e6eb; }
.ct-back { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; padding: 0; border: 0; border-radius: 4px; background: none; color: #4e5969; cursor: pointer; }
.ct-back:hover { color: #165dff; }
.ct-back svg { width: 16px; height: 16px; }
.ct-title { font-size: 14px; font-weight: 500; color: #1d2129; }
.ct-body { flex: 1; min-height: 0; overflow: auto; }
.ct-form { max-width: 960px; margin: 0 auto; padding: 24px 20px 80px; }
.ct-hint { margin: 0; font-size: 12px; color: #86909c; }
.ct-grant-hint { margin: 0 0 12px; }
.ct-error { margin-top: 4px; font-size: 12px; color: #f53f3f; }
.ct-block-error { margin: 12px 0 0; }
.ct-input { width: 100%; height: 32px; padding: 0 10px; border: 1px solid #e5e6eb; border-radius: 4px; background: #fff; font-size: 13px; color: #1d2129; outline: none; box-sizing: border-box; }
.ct-input:focus { border-color: #165dff; }
.ct-select { appearance: none; cursor: pointer; padding-right: 26px; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 8px center; background-size: 12px 12px; }
.ct-select:disabled { color: #86909c; background-color: #f7f8fa; cursor: not-allowed; }
.ct-table-wrap { overflow-x: auto; border: 1px solid #e5e6eb; border-radius: 4px; }
.ct-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.ct-table th { height: 40px; padding: 0 16px; background: #f7f8fa; text-align: left; color: #1d2129; font-weight: 500; border-bottom: 1px solid #e5e6eb; }
.ct-table td { padding: 12px 16px; border-bottom: 1px solid #f2f3f5; vertical-align: middle; }
.ct-table tbody tr:last-child td { border-bottom: none; }
.ct-table .ct-input { width: 220px; }
.ct-empty { color: #86909c; }
.ct-link-btn { display: inline-flex; align-items: center; gap: 4px; margin-top: 12px; padding: 0; border: 0; background: none; font-size: 13px; color: #165dff; cursor: pointer; }
.ct-remove { margin-top: 0; }
.ct-link-btn:hover { text-decoration: underline; }
.ct-link-ic { width: 13px; height: 13px; }
.ct-foot { display: flex; gap: 12px; margin-top: 24px; padding: 12px 0; border-top: 1px solid #f2f3f5; }
.ct-primary, .ct-ghost { height: 32px; padding: 0 18px; border-radius: 4px; font-size: 13px; cursor: pointer; }
.ct-primary { border: 0; background: #165dff; color: #fff; }
.ct-primary:hover:not(:disabled) { background: #0e42d2; }
.ct-primary:disabled, .ct-ghost:disabled { opacity: 0.7; cursor: not-allowed; }
.ct-ghost { border: 1px solid #e5e6eb; background: #fff; color: #1d2129; }
.ct-ghost:hover:not(:disabled) { border-color: #165dff; color: #165dff; }
</style>
