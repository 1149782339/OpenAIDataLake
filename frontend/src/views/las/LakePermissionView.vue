<script setup lang="ts">
import { ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import GrantDrawer, { type GrantPayload } from '@/components/las/GrantDrawer.vue'

interface Grant {
  id: number
  resource: string
  target: string
  accountScope: string
  sourceId: string
  scope: string
  permission: string
  operator: string
  updatedAt: string
}

const RESOURCE_TYPES = ['数据目录', '数据集', '数据表', '湖']
const CATALOGS = ['catalog1', 'catalog2']
const ACCOUNT_SCOPES = ['全部账号', '仅当前账号']

const resourceType = ref('数据目录')
const catalog = ref('catalog1')
const accountScope = ref('')
const target = ref('')

const grants = ref<Grant[]>([])
const grantVisible = ref(false)

function onAdd() {
  grantVisible.value = true
}

function onGrantSubmit(payload: GrantPayload) {
  grants.value.unshift({
    id: Date.now(),
    resource: payload.resource || catalog.value || 'catalog1',
    target: payload.targetName,
    accountScope: accountScope.value || '全部账号',
    sourceId: '2530手机用户#QGDdVm',
    scope: payload.scope,
    permission: payload.permission,
    operator: '2530手机用户#QGDdVm',
    updatedAt: '2026-10-30 12:00:00',
  })
  grantVisible.value = false
}
</script>

<template>
  <div class="pm-page">
    <div class="pm-filters">
      <label class="pm-field">
        <span class="pm-field-label">资源类型</span>
        <select v-model="resourceType" class="pm-select">
          <option v-for="item in RESOURCE_TYPES" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="pm-field">
        <span class="pm-field-label">数据目录</span>
        <select v-model="catalog" class="pm-select">
          <option value="" disabled>请选择</option>
          <option v-for="item in CATALOGS" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="pm-field">
        <span class="pm-field-label">账号范围</span>
        <select v-model="accountScope" class="pm-select pm-select-empty">
          <option value="" disabled>&nbsp;</option>
          <option v-for="item in ACCOUNT_SCOPES" :key="item" :value="item">{{ item }}</option>
        </select>
      </label>

      <label class="pm-field pm-field-grow">
        <span class="pm-field-label">被授权对象</span>
        <div class="pm-input">
          <input v-model="target" type="text" placeholder="请输入名称">
          <SvgIcon class="pm-input-ic" :icon="LAS_ICONS.search" />
        </div>
      </label>

      <button class="pm-primary-btn" type="button" @click="onAdd">
        <SvgIcon :icon="LAS_ICONS.plus" />
        <span>新增授权</span>
      </button>
    </div>

    <div class="pm-table-wrap">
      <table class="pm-table">
        <colgroup>
          <col style="width: 16%">
          <col style="width: 12%">
          <col style="width: 12%">
          <col style="width: 12%">
          <col style="width: 10%">
          <col style="width: 10%">
          <col style="width: 12%">
          <col style="width: 12%">
          <col style="width: 8%">
        </colgroup>
        <thead>
          <tr>
            <th>资源名称</th>
            <th>被授权对象</th>
            <th>账号范围</th>
            <th>来源账号 ID</th>
            <th>授权范围</th>
            <th>授予权限</th>
            <th>最近操作用户</th>
            <th>最近更新时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in grants" :key="row.id">
            <td>{{ row.resource }}</td>
            <td>{{ row.target }}</td>
            <td>{{ row.accountScope }}</td>
            <td>{{ row.sourceId }}</td>
            <td>{{ row.scope }}</td>
            <td>{{ row.permission }}</td>
            <td>{{ row.operator }}</td>
            <td class="pm-mono">{{ row.updatedAt }}</td>
            <td>
              <button class="pm-link-btn" type="button">编辑</button>
            </td>
          </tr>
          <tr v-if="!grants.length">
            <td class="pm-empty" colspan="9">
              <div class="pm-empty-box">
                <svg class="pm-empty-ic" width="64" height="52" viewBox="0 0 64 52" aria-hidden="true">
                  <rect x="20" y="6" width="24" height="30" rx="3" fill="#f2f3f5" stroke="#e5e6eb" />
                  <text x="32" y="20" text-anchor="middle" font-size="7" fill="#94a1b5">EMPTY</text>
                  <path d="M25 26h14M25 31h9" stroke="#dcdfe6" stroke-width="2" stroke-linecap="round" />
                </svg>
                <span class="pm-empty-text">暂无数据</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <GrantDrawer
      :visible="grantVisible"
      @close="grantVisible = false"
      @submit="onGrantSubmit"
    />
  </div>
</template>

<style scoped>
.pm-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  padding: 12px 16px 16px;
  background: #fff;
}

.pm-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding-bottom: 12px;
}

.pm-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pm-field-grow {
  flex: 1;
  min-width: 240px;
  max-width: 420px;
}

.pm-field-label {
  font-size: 13px;
  color: #4e5969;
  white-space: nowrap;
}

.pm-select,
.pm-input {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.pm-select {
  min-width: 160px;
}

.pm-select:focus,
.pm-input:focus-within {
  border-color: #165dff;
}

.pm-input {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 200px;
}

.pm-input input {
  width: 100%;
  height: 28px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #1d2129;
}

.pm-input input::placeholder {
  color: #86909c;
}

.pm-input-ic {
  width: 14px;
  height: 14px;
  flex: none;
  color: #86909c;
}

.pm-primary-btn {
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
  white-space: nowrap;
}

.pm-primary-btn:hover {
  background: #0e42d2;
}

.pm-primary-btn svg {
  width: 13px;
  height: 13px;
}

.pm-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.pm-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.pm-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.pm-table tbody tr:hover td {
  background: #f7f8fa;
}

.pm-mono {
  color: #4e5969;
  font-size: 12px;
  white-space: nowrap;
}

.pm-link-btn {
  border: 0;
  background: none;
  padding: 0;
  color: #165dff;
  font-size: 13px;
  cursor: pointer;
}

.pm-link-btn:hover {
  text-decoration: underline;
}

.pm-empty {
  height: 240px;
  text-align: center;
}

.pm-empty-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.pm-empty-ic {
  display: block;
}

.pm-empty-text {
  font-size: 13px;
  color: #86909c;
}
</style>
