<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS, FORMAT_ICON } from '@/components/las/lasIcons'
import { useLasStore } from '@/stores/las'

const route = useRoute()
const router = useRouter()
const store = useLasStore()

const activeTab = ref<'info' | 'preview'>('info')

const dataset = computed(() => store.datasetById(route.params.id as string))

const infoRows = computed(() => {
  const d = dataset.value
  if (!d) return []
  return [
    { k: '数据集名称', v: d.name },
    { k: '数据集中文名称', v: d.cnName || '—' },
    { k: '数据集格式', v: d.format, format: d.format },
    { k: '模态', v: d.modality },
    { k: '标签', v: String(d.tagCount) },
    { k: '样本数', v: d.samples.toLocaleString('en-US') },
    { k: '大小', v: d.sizeText },
    { k: '权限', v: d.permission },
    { k: '创建人', v: d.creator },
    { k: '创建时间', v: d.createdAt },
    { k: '更新时间', v: d.updatedAt },
    { k: '数据来源', v: d.source },
    { k: '存储路径', v: d.path || '—' },
  ]
})

const previewColumns = ['id', 'image', 'caption', 'label', 'created_at']
const previewRows = [
  ['1001', 'oss://demo/1001.png', '一只坐在窗台上的猫', '猫', '2026-10-22 10:30:02'],
  ['1002', 'oss://demo/1002.png', '城市夜景与车流', '城市', '2026-10-22 10:30:01'],
  ['1003', 'oss://demo/1003.png', '航拍视角的河流', '自然', '2026-10-22 10:30:00'],
  ['1004', 'oss://demo/1004.png', '实验室里的机械臂', '工业', '2026-10-22 10:29:58'],
  ['1005', 'oss://demo/1005.png', '雨中的街道', '街拍', '2026-10-22 10:29:55'],
]
</script>

<template>
  <div class="detail-page">
    <header class="detail-head">
      <button class="back-btn" type="button" @click="router.back()">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <div class="head-text">
        <div class="head-name">
          <span class="name">{{ dataset?.name ?? '数据集' }}</span>
          <span v-if="dataset" class="format-badge">
            <SvgIcon :icon="LAS_ICONS[FORMAT_ICON[dataset.format]]" />
            <span>{{ dataset.format }}</span>
          </span>
        </div>
        <div class="head-desc">{{ dataset?.cnName || '—' }}</div>
      </div>
    </header>

    <div class="tabs">
      <button
        class="tab"
        :class="{ on: activeTab === 'info' }"
        type="button"
        @click="activeTab = 'info'"
      >
        基本信息
      </button>
      <button
        class="tab"
        :class="{ on: activeTab === 'preview' }"
        type="button"
        @click="activeTab = 'preview'"
      >
        数据预览
      </button>
    </div>

    <div class="detail-body">
      <template v-if="activeTab === 'info'">
        <div v-if="dataset" class="info-grid">
          <template v-for="row in infoRows" :key="row.k">
            <div class="k">{{ row.k }}</div>
            <div class="v">
              <span v-if="row.format" class="format-badge">
                <SvgIcon :icon="LAS_ICONS[FORMAT_ICON[row.format]]" />
                <span>{{ row.v }}</span>
              </span>
              <span v-else>{{ row.v }}</span>
            </div>
          </template>
        </div>
        <div v-else class="empty-state">未找到该数据集</div>
      </template>

      <template v-else>
        <div v-if="dataset" class="preview">
          <div class="preview-bar">
            <span class="preview-title">前 {{ previewRows.length }} 条样本</span>
            <span class="preview-tip">演示数据</span>
          </div>
          <table class="preview-table">
            <thead>
              <tr>
                <th v-for="col in previewColumns" :key="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in previewRows" :key="index">
                <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
}

.back-btn {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.back-btn:hover {
  color: #165dff;
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.head-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}

.head-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #86909c;
}

.format-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 22px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  color: #4e5969;
  font-size: 12px;
  font-weight: 400;
}

.format-badge svg {
  width: 12px;
  height: 12px;
}

.tabs {
  display: flex;
  gap: 24px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e6eb;
}

.tab {
  height: 38px;
  border: 0;
  background: none;
  padding: 0;
  font-size: 14px;
  color: #4e5969;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.tab.on {
  color: #165dff;
  border-bottom-color: #165dff;
}

.detail-body {
  flex: 1;
  overflow: auto;
  padding: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: 140px 1fr 140px 1fr;
  gap: 0;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  overflow: hidden;
  font-size: 13px;
}

.info-grid .k,
.info-grid .v {
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
}

.info-grid .k {
  background: #f7f8fa;
  color: #4e5969;
}

.info-grid .v {
  color: #1d2129;
  word-break: break-all;
}

.preview-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-title {
  font-size: 14px;
  color: #1d2129;
}

.preview-tip {
  font-size: 12px;
  color: #86909c;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.preview-table th {
  height: 36px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.preview-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #4e5969;
}

.empty-state {
  padding: 60px 0;
  text-align: center;
  color: #86909c;
}
</style>
