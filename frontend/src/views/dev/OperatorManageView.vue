<script setup lang="ts">
import { computed, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import RegisterOperatorModal, {
  type RegisterOperatorPayload,
} from '@/components/dev/RegisterOperatorModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { DEV_OPERATORS, type DevOperator } from '@/mock/dev/operators'

const registerOpen = ref(false)
/** 已注册的自定义算子 */
const customOperators = ref<DevOperator[]>([])

const operators = computed(() => [...DEV_OPERATORS, ...customOperators.value])

function onRegister(payload: RegisterOperatorPayload) {
  customOperators.value.push({
    id: `op_custom_${Date.now()}`,
    name: payload.name,
    category: '自定义算子',
    desc: '用户注册的算子，已声明输入输出契约。',
    runtime: payload.runtime,
    builtin: false,
    inputs: payload.inputs.map((c) => ({ ...c, required: true })),
    outputs: payload.outputs.map((c) => ({ ...c, required: true })),
    params: [],
  })
  registerOpen.value = false
}
</script>

<template>
  <div class="op-page">
    <header class="op-head">
      <div class="op-head-left">
        <span class="op-title">算子管理</span>
        <span class="op-sub">共 {{ operators.length }} 个算子</span>
      </div>
      <button class="op-primary" type="button" @click="registerOpen = true">
        <SvgIcon :icon="LAS_ICONS.plus" />
        <span>注册算子</span>
      </button>
    </header>

    <div class="op-body">
      <div class="op-cards">
        <article v-for="op in operators" :key="op.id" class="op-card">
          <div class="op-card-head">
            <span class="op-name">{{ op.name }}</span>
            <span class="op-tag" :class="{ 'is-custom': !op.builtin }">{{ op.category }}</span>
          </div>

          <p class="op-desc">{{ op.desc }}</p>

          <div class="op-block">
            <div class="op-block-title">
              <SvgIcon :icon="LAS_ICONS.db" />
              <span>输入契约要求列</span>
            </div>
            <ul class="op-cols">
              <li v-for="col in op.inputs" :key="col.name" class="op-col">
                <span class="op-col-name">{{ col.name }}</span>
                <span class="op-col-type">{{ col.type }}</span>
                <span v-if="col.required" class="op-col-req">必填</span>
              </li>
              <li v-if="!op.inputs.length" class="op-col-empty">无</li>
            </ul>
          </div>

          <div class="op-block">
            <div class="op-block-title">
              <SvgIcon :icon="LAS_ICONS.fmtLance" />
              <span>输出 schema 声明列</span>
            </div>
            <ul class="op-cols">
              <li v-for="col in op.outputs" :key="col.name" class="op-col">
                <span class="op-col-name">{{ col.name }}</span>
                <span class="op-col-type">{{ col.type }}</span>
                <span v-if="col.required" class="op-col-req">必填</span>
              </li>
              <li v-if="!op.outputs.length" class="op-col-empty">无</li>
            </ul>
          </div>

          <div class="op-card-foot">
            <span class="op-foot-label">运行环境镜像</span>
            <span class="op-foot-value">{{ op.runtime }}</span>
          </div>
        </article>
      </div>
    </div>

    <RegisterOperatorModal :open="registerOpen" @close="registerOpen = false" @submit="onRegister" />
  </div>
</template>

<style scoped>
.op-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 1100px;
  background: #fff;
}

.op-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
}

.op-head-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.op-title {
  font-size: 16px;
  font-weight: 500;
  color: #1d2129;
}

.op-sub {
  font-size: 13px;
  color: #86909c;
}

.op-primary {
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

.op-primary:hover {
  background: #0e42d2;
}

.op-primary svg {
  width: 13px;
  height: 13px;
}

.op-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 20px 24px;
}

.op-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.op-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.op-card:hover {
  border-color: #9cc6fa;
  box-shadow: 0 2px 8px rgba(29, 33, 41, 0.06);
}

.op-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.op-name {
  font-size: 15px;
  font-weight: 500;
  color: #1d2129;
}

.op-tag {
  padding: 1px 7px;
  border-radius: 3px;
  background: #f2f3f5;
  font-size: 12px;
  color: #4e5969;
}

.op-tag.is-custom {
  background: #e8f3ff;
  color: #165dff;
}

.op-desc {
  margin: 10px 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #4e5969;
}

.op-block {
  margin-bottom: 12px;
}

.op-block-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: #86909c;
}

.op-block-title svg {
  width: 13px;
  height: 13px;
}

.op-cols {
  margin: 0;
  padding: 0;
  list-style: none;
}

.op-col {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 26px;
  font-size: 12.5px;
  color: #1d2129;
}

.op-col-name {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}

.op-col-type {
  padding: 1px 6px;
  border-radius: 3px;
  background: #f7f8fa;
  font-size: 11.5px;
  color: #4e5969;
}

.op-col-req {
  font-size: 11.5px;
  color: #f53f3f;
}

.op-col-empty {
  height: 26px;
  font-size: 12.5px;
  color: #c9cdd4;
}

.op-card-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid #f2f3f5;
  font-size: 12px;
}

.op-foot-label {
  flex: none;
  color: #86909c;
}

.op-foot-value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #4e5969;
}
</style>
