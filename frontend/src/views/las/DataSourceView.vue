<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { ApiError, errorMessage } from '@/api/http'
import { useLasDataSourceStore } from '@/stores/lasDataSources'

const router = useRouter()
const store = useLasDataSourceStore()
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    await store.load()
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/sources' } })
      return
    }
    error.value = errorMessage(cause, '加载数据源失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="ds-page">
    <header class="ds-head">
      <h2>数据源</h2>
      <button class="ds-primary" type="button" @click="router.push('/catalog/sources/create')">
        <SvgIcon :icon="LAS_ICONS.plus" />
        <span>创建数据源</span>
      </button>
    </header>

    <p v-if="error" class="ds-error">{{ error }}</p>

    <div class="ds-table-wrap">
      <table class="ds-table">
        <thead>
          <tr>
            <th>数据源名称</th>
            <th>数据源类型</th>
            <th>地址</th>
            <th>数据库</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td class="ds-empty" colspan="5">加载中…</td>
          </tr>
          <tr v-else-if="!store.items.length">
            <td class="ds-empty" colspan="5">暂无数据源</td>
          </tr>
          <template v-else>
            <tr v-for="row in store.items" :key="row.id">
              <td>{{ row.name }}</td>
              <td>{{ row.engine }}</td>
              <td>{{ row.host }}:{{ row.port }}</td>
              <td>{{ row.database }}</td>
              <td>
                <button class="ds-link" type="button" @click="router.push(`/catalog/sources/${row.id}/access`)">权限</button>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.ds-page {
  height: 100%;
  padding: 16px 20px 24px;
  background: #fff;
  color: #1d2129;
  overflow: auto;
}

.ds-head {
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

.ds-primary {
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

.ds-primary:hover {
  background: #0e42d2;
}

.ds-primary svg {
  width: 13px;
  height: 13px;
}

.ds-table-wrap {
  overflow-x: auto;
}

.ds-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.ds-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.ds-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
}

.ds-table tbody tr:hover td {
  background: #f7f8fa;
}

.ds-empty {
  text-align: center;
  color: #86909c;
}

.ds-error {
  margin: 0 0 12px;
  font-size: 13px;
  color: #f53f3f;
}

.ds-link {
  padding: 0;
  border: 0;
  background: none;
  color: #165dff;
  font-size: 13px;
  cursor: pointer;
}

.ds-link:hover {
  text-decoration: underline;
}
</style>
