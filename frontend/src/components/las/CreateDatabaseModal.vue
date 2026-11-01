<script setup lang="ts">
import { ref, watch } from 'vue'
import LasModal from '@/components/las/LasModal.vue'

export interface DatabasePayload {
  name: string
  location: string
  remark: string
}

const props = defineProps<{ visible: boolean; catalog: string }>()

const emit = defineEmits<{
  close: []
  submit: [payload: DatabasePayload]
}>()

const dbName = ref('')
const location = ref('tos://')
const remark = ref('')
const nameError = ref(false)

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    dbName.value = ''
    location.value = 'tos://'
    remark.value = ''
    nameError.value = false
  },
)

function onConfirm() {
  if (!dbName.value.trim()) {
    nameError.value = true
    return
  }
  emit('submit', {
    name: dbName.value.trim(),
    location: location.value.trim(),
    remark: remark.value.trim(),
  })
}
</script>

<template>
  <LasModal :visible="visible" title="创建数据库" @close="emit('close')">
    <div class="cd-row">
      <label class="cd-label"><i class="cd-req">*</i>所属 Catalog</label>
      <div class="cd-control">
        <span class="cd-text">{{ catalog }}</span>
      </div>
    </div>

    <div class="cd-row">
      <label class="cd-label"><i class="cd-req">*</i>数据库名称</label>
      <div class="cd-control">
        <input
          v-model="dbName"
          type="text"
          class="cd-input"
          :class="{ 'is-error': nameError }"
          placeholder="小写字母、下划线、数字组成"
          @input="nameError = false"
        >
        <div v-if="nameError" class="cd-error">请输入</div>
      </div>
    </div>

    <div class="cd-row">
      <label class="cd-label"><i class="cd-req">*</i>存储位置</label>
      <div class="cd-control">
        <div class="cd-inline">
          <input v-model="location" type="text" class="cd-input" placeholder="tos://">
          <button class="cd-plus" type="button" title="选择路径">+</button>
        </div>
        <p class="cd-hint">
          系统建议使用的格式:tos://[数据目录存储位置]/[库名称]
          <a href="#" class="cd-link" @click.prevent>使用默认路径</a>
        </p>
      </div>
    </div>

    <div class="cd-row">
      <label class="cd-label">备注信息</label>
      <div class="cd-control">
        <textarea v-model="remark" class="cd-textarea" placeholder="请输入" />
      </div>
    </div>

    <template #footer>
      <button class="cd-btn cd-btn-ghost" type="button" @click="emit('close')">取消</button>
      <button class="cd-btn cd-btn-primary" type="button" @click="onConfirm">确认</button>
    </template>
  </LasModal>
</template>

<style scoped>
.cd-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
}

.cd-label {
  flex: none;
  width: 133px;
  padding-top: 5px;
  padding-right: 40px;
  box-sizing: border-box;
  text-align: right;
  font-size: 13px;
  line-height: 18px;
  color: #1d2129;
}

.cd-req {
  margin-right: 4px;
  font-style: normal;
  color: #f53f3f;
}

.cd-control {
  flex: 1;
  min-width: 0;
}

.cd-text {
  display: inline-block;
  padding-top: 5px;
  font-size: 13px;
  color: #1d2129;
}

.cd-input {
  width: 100%;
  height: 28px;
  padding: 0 12px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #0c0d0e;
  outline: none;
  box-sizing: border-box;
}

.cd-input:focus {
  border-color: #165dff;
}

.cd-input.is-error {
  border-color: #f53f3f;
}

.cd-input::placeholder {
  color: #86909c;
}

.cd-textarea {
  width: 100%;
  height: 54px;
  padding: 4px 12px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #0c0d0e;
  outline: none;
  resize: none;
  box-sizing: border-box;
}

.cd-textarea:focus {
  border-color: #165dff;
}

.cd-textarea::placeholder {
  color: #86909c;
}

.cd-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f53f3f;
}

.cd-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cd-plus {
  width: 28px;
  height: 28px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}

.cd-plus:hover {
  border-color: #165dff;
  color: #165dff;
}

.cd-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

.cd-link {
  margin-left: 8px;
  color: #165dff;
  text-decoration: none;
}

.cd-link:hover {
  text-decoration: underline;
}

.cd-btn {
  height: 32px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.cd-btn-ghost {
  border: 1px solid #dde2e9;
  background: #f6f8fa;
  color: #42464e;
}

.cd-btn-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}

.cd-btn-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.cd-btn-primary:hover {
  background: #0e42d2;
}
</style>
