<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import {
  DEV_ENGINE_METRICS,
  DEV_OP_RECORDS,
  DEV_QUOTAS,
  type DevEngineMetric,
  type DevOpRecord,
  type DevQuota,
} from '@/mock/dev/engine'

/** 指标卡图标 */
const METRIC_ICON: Record<string, string> = {
  running: 'play',
  queued: 'clock',
  retry: 'refresh',
  quota: 'engine',
}

const metrics = computed<DevEngineMetric[]>(() => DEV_ENGINE_METRICS)
const records = computed<DevOpRecord[]>(() => DEV_OP_RECORDS)
const quotas = computed<DevQuota[]>(() => DEV_QUOTAS)

/** 状态机芯片配色 */
const STATE_CLASS: Record<string, string> = {
  PENDING: 'is-pending',
  RUNNING: 'is-running',
  SUCCESS: 'is-success',
  FAILED: 'is-failed',
  RETRYING: 'is-retrying',
  SUSPENDED: 'is-suspended',
}

/** 状态机中文说明 */
const STATE_LABEL: Record<string, string> = {
  PENDING: '等待调度',
  RUNNING: '执行中',
  SUCCESS: '执行成功',
  FAILED: '执行失败',
  RETRYING: '重试中',
  SUSPENDED: '已挂起',
}

const STATUS_CLASS: Record<string, string> = {
  成功: 'is-success',
  失败: 'is-failed',
  运行中: 'is-running',
  重试中: 'is-retrying',
}

function stateClass(state: string): string {
  return STATE_CLASS[state] ?? 'is-pending'
}

function stateLabel(state: string): string {
  return STATE_LABEL[state] ?? state
}

function statusClass(status: string): string {
  return STATUS_CLASS[status] ?? 'is-pending'
}

/** 配额占用比例对应的进度条颜色分档 */
function usageClass(usage: number): string {
  if (usage >= 70) return 'is-danger'
  if (usage >= 40) return 'is-warning'
  return 'is-safe'
}

function metricIcon(key: string) {
  return LAS_ICONS[METRIC_ICON[key] ?? 'engine']
}
</script>

<template>
  <div class="de-page">
    <header class="de-head">
      <div class="de-head-left">
        <span class="de-title">执行引擎</span>
        <span class="de-sub">监控算子运行状态与集群资源配额占用情况</span>
      </div>
    </header>

    <div class="de-body">
      <!-- 指标卡 -->
      <section class="de-metrics">
        <article v-for="m in metrics" :key="m.key" class="de-metric">
          <div class="de-metric-head">
            <span class="de-metric-label">{{ m.label }}</span>
            <span class="de-metric-icon">
              <SvgIcon :icon="metricIcon(m.key)" />
            </span>
          </div>
          <div class="de-metric-value">{{ m.value }}</div>
          <div class="de-metric-foot">
            <span class="de-metric-desc">{{ m.desc }}</span>
            <span class="de-trend" :class="`is-${m.trendType}`">{{ m.trend }}</span>
          </div>
        </article>
      </section>

      <!-- 算子执行记录 -->
      <section class="de-panel">
        <div class="de-panel-head">
          <span class="de-panel-title">算子执行记录</span>
          <span class="de-panel-sub">共 {{ records.length }} 条执行记录</span>
        </div>
        <div class="de-table-wrap">
          <table class="de-table">
            <thead>
              <tr>
                <th>管道名称</th>
                <th>算子名称</th>
                <th>状态流转</th>
                <th>最终状态</th>
                <th>重试次数</th>
                <th>耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in records" :key="r.id">
                <td>{{ r.pipeline }}</td>
                <td>{{ r.operator }}</td>
                <td>
                  <div class="de-flow">
                    <template v-for="(step, i) in r.flow" :key="`${r.id}-${i}`">
                      <span
                        v-if="i > 0"
                        class="de-flow-arrow"
                        aria-hidden="true"
                      >→</span>
                      <span
                        class="de-chip"
                        :class="stateClass(step.state)"
                        :title="`${stateLabel(step.state)} · ${step.time}`"
                      >{{ step.state }}</span>
                    </template>
                  </div>
                </td>
                <td>
                  <span class="de-status" :class="statusClass(r.status)">{{ r.status }}</span>
                </td>
                <td>{{ r.retries }}</td>
                <td>{{ r.duration }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 资源配额 -->
      <section class="de-panel">
        <div class="de-panel-head">
          <span class="de-panel-title">资源配额</span>
          <span class="de-panel-sub">共 {{ quotas.length }} 条管道配额</span>
        </div>
        <div class="de-table-wrap">
          <table class="de-table">
            <thead>
              <tr>
                <th>管道名称</th>
                <th>CPU 配额</th>
                <th>内存配额</th>
                <th style="width: 30%">占用比例</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="q in quotas" :key="q.pipeline">
                <td>{{ q.pipeline }}</td>
                <td>
                  <span class="de-usage-num">{{ q.cpuUsed }}</span>
                  <span class="de-usage-total">/ {{ q.cpuTotal }} 核</span>
                </td>
                <td>
                  <span class="de-usage-num">{{ q.memUsed }}</span>
                  <span class="de-usage-total">/ {{ q.memTotal }} GB</span>
                </td>
                <td>
                  <div class="de-bar">
                    <div
                      class="de-bar-fill"
                      :class="usageClass(q.usage)"
                      :style="{ width: `${Math.min(q.usage, 100)}%` }"
                    ></div>
                  </div>
                  <span class="de-bar-text" :class="usageClass(q.usage)">{{ q.usage }}%</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.de-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

.de-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.de-head-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.de-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.de-sub {
  font-size: 13px;
  color: #86909c;
}

.de-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

/* ---------- 指标卡 ---------- */
.de-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.de-metric {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.de-metric:hover {
  border-color: #9cc6fa;
  box-shadow: 0 2px 8px rgba(29, 33, 41, 0.06);
}

.de-metric-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.de-metric-label {
  font-size: 13px;
  color: #4e5969;
}

.de-metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: #f2f3f5;
  color: #4e5969;
}

.de-metric-icon svg {
  width: 14px;
  height: 14px;
}

.de-metric-value {
  margin: 10px 0 8px;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  color: #1d2129;
}

.de-metric-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.de-metric-desc {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #86909c;
}

.de-trend {
  flex: none;
  font-size: 12px;
}

.de-trend.is-up {
  color: #ff7d00;
}

.de-trend.is-down {
  color: #00b42a;
}

.de-trend.is-flat {
  color: #86909c;
}

/* ---------- 面板 ---------- */
.de-panel {
  margin-bottom: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}

.de-panel:last-child {
  margin-bottom: 0;
}

.de-panel-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  height: 44px;
  padding: 0 16px;
  border-bottom: 1px solid #f2f3f5;
}

.de-panel-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.de-panel-sub {
  font-size: 12.5px;
  color: #86909c;
}

/* ---------- 表格 ---------- */
.de-table-wrap {
  overflow-x: auto;
}

.de-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.de-table th {
  height: 40px;
  padding: 0 12px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
  white-space: nowrap;
}

.de-table td {
  padding: 12px;
  border-bottom: 1px solid #f2f3f5;
  color: #1d2129;
  vertical-align: middle;
}

.de-table tbody tr:hover td {
  background: #f7f8fa;
}

/* ---------- 状态机流转 ---------- */
.de-flow {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.de-flow-arrow {
  color: #c9cdd4;
  font-size: 12px;
}

.de-chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 7px;
  border-radius: 3px;
  font-size: 11.5px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  white-space: nowrap;
  cursor: default;
}

.de-chip.is-pending {
  background: #f2f3f5;
  color: #86909c;
}

.de-chip.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.de-chip.is-success {
  background: #e8ffea;
  color: #00b42a;
}

.de-chip.is-failed {
  background: #ffece8;
  color: #f53f3f;
}

.de-chip.is-retrying {
  background: #fff7e8;
  color: #ff7d00;
}

.de-chip.is-suspended {
  background: #fffbe6;
  color: #fadb14;
}

/* ---------- 最终状态标签 ---------- */
.de-status {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}

.de-status.is-success {
  background: #e8ffea;
  color: #00b42a;
}

.de-status.is-failed {
  background: #ffece8;
  color: #f53f3f;
}

.de-status.is-running {
  background: #e8f3ff;
  color: #165dff;
}

.de-status.is-retrying {
  background: #fff7e8;
  color: #ff7d00;
}

.de-status.is-pending {
  background: #f2f3f5;
  color: #86909c;
}

/* ---------- 配额 ---------- */
.de-usage-num {
  font-weight: 500;
  color: #1d2129;
}

.de-usage-total {
  margin-left: 2px;
  font-size: 12px;
  color: #86909c;
}

.de-bar {
  display: inline-block;
  vertical-align: middle;
  width: 180px;
  height: 6px;
  border-radius: 3px;
  background: #f2f3f5;
  overflow: hidden;
}

.de-bar-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.2s ease;
}

.de-bar-fill.is-safe {
  background: #00b42a;
}

.de-bar-fill.is-warning {
  background: #ff7d00;
}

.de-bar-fill.is-danger {
  background: #f53f3f;
}

.de-bar-text {
  margin-left: 8px;
  font-size: 12px;
  white-space: nowrap;
}

.de-bar-text.is-safe {
  color: #00b42a;
}

.de-bar-text.is-warning {
  color: #ff7d00;
}

.de-bar-text.is-danger {
  color: #f53f3f;
}
</style>
