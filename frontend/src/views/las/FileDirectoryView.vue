<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { ApiError, errorMessage } from '@/api/http'
import { listDirectories, type DirectoryItem } from '@/api/console'

const router = useRouter()
const rows = ref<DirectoryItem[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await listDirectories()
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/directories' } })
      return
    }
    error.value = errorMessage(cause, '加载文件目录失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="fd-page">
    <header class="fd-head">
      <h2>文件目录</h2>
      <button class="fd-primary" type="button" @click="router.push('/catalog/directories/create')">
        <SvgIcon :icon="LAS_ICONS.plus" />
        <span>创建文件目录</span>
      </button>
    </header>

    <p v-if="error" class="fd-error">{{ error }}</p>

    <div class="fd-table-wrap">
      <table class="fd-table">
        <thead>
          <tr>
            <th>逻辑名称</th>
            <th>Endpoint</th>
            <th>物理目录</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td class="fd-empty" colspan="3">加载中…</td>
          </tr>
          <tr v-else-if="!rows.length">
            <td class="fd-empty" colspan="3">暂无文件目录</td>
          </tr>
          <template v-else>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.logicalName }}</td>
              <td>{{ row.endpoint || '—' }}</td>
              <td>{{ row.directory || '—' }}</td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.fd-page {
  height: 100%;
  padding: 16px 20px 24px;
  background: #fff;
  color: #1d2129;
  overflow: auto;
}

.fd-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.fd-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.fd-primary:hover {
  background: #0e42d2;
}

.fd-primary svg {
  width: 13px;
  height: 13px;
}

.fd-error {
  margin: 0 0 12px;
  font-size: 13px;
  color: #f53f3f;
}

.fd-table-wrap {
  overflow-x: auto;
}

.fd-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.fd-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.fd-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
}

.fd-table tbody tr:hover td {
  background: #f7f8fa;
}

.fd-empty {
  text-align: center;
  color: #86909c;
}
</style>
