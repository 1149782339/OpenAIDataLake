<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { useLasStore } from '@/stores/las'

const route = useRoute()
const router = useRouter()
const store = useLasStore()

const dataset = computed(() => store.datasetById(route.params.id as string))

const sql = ref(
  dataset.value
    ? `SELECT *\nFROM ${dataset.value.name}\nLIMIT 100;`
    : 'SELECT *\nFROM ds_public\nLIMIT 100;',
)

const running = ref(false)
const executed = ref(false)

const columns = ['id', 'image', 'caption', 'label', 'created_at']
const rows = [
  ['1001', 'oss://demo/1001.png', '一只坐在窗台上的猫', '猫', '2026-10-22 10:30:02'],
  ['1002', 'oss://demo/1002.png', '城市夜景与车流', '城市', '2026-10-22 10:30:01'],
  ['1003', 'oss://demo/1003.png', '航拍视角的河流', '自然', '2026-10-22 10:30:00'],
  ['1004', 'oss://demo/1004.png', '实验室里的机械臂', '工业', '2026-10-22 10:29:58'],
]

function run() {
  running.value = true
  window.setTimeout(() => {
    running.value = false
    executed.value = true
  }, 400)
}
</script>

<template>
  <div class="sql-page">
    <header class="sql-head">
      <button class="back-btn" type="button" @click="router.back()">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <div class="head-text">
        <div class="head-name">SQL 查询 · {{ dataset?.name ?? '数据集' }}</div>
        <div class="head-desc">在 LAS 中使用 SQL 直接查询数据集内容</div>
      </div>
      <button class="run-btn" type="button" :disabled="running" @click="run">
        <SvgIcon v-if="!running" :icon="LAS_ICONS.play" />
        <span>{{ running ? '执行中…' : '运行' }}</span>
      </button>
    </header>

    <div class="sql-body">
      <section class="editor">
        <div class="editor-head">
          <span class="editor-title">SQL 编辑器</span>
          <span class="editor-tip">演示环境，不真实访问数据</span>
        </div>
        <textarea v-model="sql" class="sql-input" spellcheck="false" />
      </section>

      <section class="result">
        <div class="result-head">
          <span class="result-title">查询结果</span>
          <span v-if="executed" class="result-count">共 {{ rows.length }} 行</span>
        </div>
        <table class="result-table">
          <thead>
            <tr>
              <th v-for="col in columns" :key="col">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in executed ? rows : []" :key="index">
              <td v-for="(cell, ci) in row" :key="ci">{{ cell }}</td>
            </tr>
            <tr v-if="!executed">
              <td class="empty" :colspan="columns.length">点击「运行」查看查询结果</td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<style scoped>
.sql-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
}

.sql-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid #e5e6eb;
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

.head-text {
  flex: 1;
}

.head-name {
  font-size: 15px;
  font-weight: 500;
  color: #1d2129;
}

.head-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #86909c;
}

.run-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 16px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.run-btn:disabled {
  background: #94bfff;
  cursor: not-allowed;
}

.run-btn svg {
  width: 13px;
  height: 13px;
}

.sql-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.editor,
.result {
  padding: 16px 20px;
}

.editor {
  flex: none;
}

.editor-head,
.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.editor-title,
.result-title {
  font-size: 13px;
  color: #1d2129;
}

.editor-tip,
.result-count {
  font-size: 12px;
  color: #86909c;
}

.sql-input {
  width: 100%;
  height: 180px;
  padding: 12px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #f7f8fa;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #1d2129;
  outline: none;
  resize: vertical;
}

.sql-input:focus {
  border-color: #165dff;
  background: #fff;
}

.result {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border-top: 1px solid #f2f3f5;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.result-table th {
  height: 36px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.result-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #4e5969;
}

.result-table td.empty {
  height: 100px;
  text-align: center;
  color: #86909c;
}
</style>
