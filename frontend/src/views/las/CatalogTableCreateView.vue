<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import FolderPickerModal from '@/components/las/FolderPickerModal.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { LAS_STORAGE_FOLDERS, useLasCatalogStore } from '@/stores/lasCatalog'

const route = useRoute()
const router = useRouter()
const store = useLasCatalogStore()

const TABLE_FORMATS = ['Iceberg', 'Paimon', 'Lance']
const FIELD_TYPES = [
  'string',
  'int',
  'bigint',
  'int64',
  'double',
  'float',
  'float64',
  'boolean',
  'timestamp',
  'timestamp[us]',
  'date',
  'binary',
  'vector<float32>[512]',
]

interface FieldRow {
  name: string
  type: string
  desc: string
}

interface ConfigRow {
  name: string
  value: string
}

const catalogName = computed(() => String(route.params.catalog ?? 'catalog1'))
const databaseName = computed(() => String(route.params.schema ?? 'default'))

const tableName = ref('')
const tableFormat = ref('Lance')
const storagePath = ref('oss://')
const remark = ref('')
const importFromDataset = ref(false)
const nameError = ref(false)
const pickerVisible = ref(false)

const fields = ref<FieldRow[]>([{ name: '', type: '', desc: '' }])
/** 自定义配置 */
const configs = ref<ConfigRow[]>([{ name: '', value: '' }])
/** 字段信息是否由存储位置自动解析而来 */
const fieldsParsed = ref(false)

/** 根据存储位置匹配对象存储中的表 */
function findStorageNode(path: string) {
  const m = path.match(/^oss:\/\/([^/]+)\/(.+?)\/?$/)
  if (!m) return null
  const bucket = LAS_STORAGE_FOLDERS.find((b) => b.name === m[1])
  if (!bucket) return null
  const name = m[2].split('/')[0]
  return (bucket.children ?? []).find((c) => c.name === name) ?? null
}

/** 选择存储位置后自动解析字段信息 */
function parseFields(path: string) {
  const node = findStorageNode(path)
  if (!node?.schema?.length) {
    fieldsParsed.value = false
    return
  }
  fields.value = node.schema.map((f) => ({ name: f.name, type: f.type, desc: '' }))
  fieldsParsed.value = true
}

function onPickPath(path: string) {
  storagePath.value = path
  parseFields(path)
}

function addField() {
  fields.value.push({ name: '', type: '', desc: '' })
}

function removeField(index: number) {
  fields.value.splice(index, 1)
  if (!fields.value.length) fields.value.push({ name: '', type: '', desc: '' })
}

function addConfig() {
  configs.value.push({ name: '', value: '' })
}

function removeConfig(index: number) {
  configs.value.splice(index, 1)
  if (!configs.value.length) configs.value.push({ name: '', value: '' })
}

/** 汇总自定义配置；导入模式创建的是外表，额外以 EXTERNAL=true 标记 */
function buildProperties(): Record<string, string> {
  const props: Record<string, string> = {}
  configs.value.forEach((row) => {
    if (row.name.trim()) props[row.name.trim()] = row.value.trim()
  })
  if (importFromDataset.value) props.EXTERNAL = 'true'
  return props
}

function onSubmit() {
  nameError.value = !tableName.value.trim()
  if (nameError.value) return
  store.addTable(catalogName.value, databaseName.value, tableName.value.trim(), {
    location: importFromDataset.value ? storagePath.value : undefined,
    tableType: tableFormat.value,
    properties: buildProperties(),
    schema: fieldsParsed.value
      ? fields.value.filter((f) => f.name).map((f) => ({ name: f.name, type: f.type }))
      : undefined,
  })
  router.push(`/catalog/metadata/database/${catalogName.value}/${databaseName.value}`)
}

function onCancel() {
  router.push(`/catalog/metadata/database/${catalogName.value}/${databaseName.value}`)
}
</script>

<template>
  <div class="ct-page">
    <header class="ct-head">
      <button class="ct-back" type="button" title="返回" @click="onCancel">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="ct-title">创建数据表</span>
    </header>

    <div class="ct-body">
      <form class="ct-form" @submit.prevent="onSubmit">
        <!-- 基本信息 -->
        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">基本信息</span>
          </div>

          <div class="ct-field">
            <label class="ct-label">所属 Catalog</label>
            <div class="ct-control">
              <span class="ct-text">{{ catalogName }}</span>
            </div>
          </div>

          <div class="ct-field">
            <label class="ct-label">所属数据库</label>
            <div class="ct-control">
              <span class="ct-text">{{ databaseName }}</span>
            </div>
          </div>

          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>数据表名称</label>
            <div class="ct-control">
              <input
                v-model="tableName"
                type="text"
                class="ct-input"
                :class="{ 'is-error': nameError }"
                placeholder="由小写字母、数字、下划线组成，且不能以数字开头"
                @input="nameError = false"
              >
              <div v-if="nameError" class="ct-error">请输入</div>
            </div>
          </div>

          <!-- 表格式 / 存储位置：仅导入模式（外表）下出现 -->
          <template v-if="importFromDataset">
            <div class="ct-field">
              <label class="ct-label">表格式</label>
              <div class="ct-control">
                <div class="ct-radios">
                  <label v-for="item in TABLE_FORMATS" :key="item" class="ct-radio">
                    <input v-model="tableFormat" type="radio" :value="item">
                    <span class="ct-radio-mark" />
                    <span>{{ item }}</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="ct-field">
              <label class="ct-label"><i class="ct-req">*</i>选择存储位置</label>
              <div class="ct-control">
                <div class="ct-inline">
                  <input v-model="storagePath" type="text" class="ct-input" placeholder="oss://">
                  <button class="ct-plus" type="button" title="选择路径" @click="pickerVisible = true">
                    +
                  </button>
                </div>
                <p class="ct-hint">
                  系统建议使用的格式：oss://[数据目录路径]/[库目录]/[表名称]
                  <a href="#" class="ct-link" @click.prevent>使用默认路径</a>
                </p>
                <p v-if="fieldsParsed" class="ct-hint is-ok">
                  已根据所选存储位置自动解析字段信息，可在「字段信息」中查看
                </p>
              </div>
            </div>
          </template>

          <div class="ct-field">
            <label class="ct-label">从数据集中导入数据</label>
            <div class="ct-control">
              <button
                class="ct-switch"
                :class="{ on: importFromDataset }"
                type="button"
                role="switch"
                :aria-checked="importFromDataset"
                @click="importFromDataset = !importFromDataset"
              >
                <i class="ct-switch-dot" />
              </button>
              <p class="ct-hint">
                {{
                  importFromDataset
                    ? '导入模式下创建的是外表：需指定表格式与数据所在的存储位置'
                    : '请确保要导入的数据集文件存放在上方「存储位置」的路径中'
                }}
              </p>
            </div>
          </div>

          <div class="ct-field">
            <label class="ct-label">数据表备注</label>
            <div class="ct-control">
              <textarea
                v-model="remark"
                class="ct-textarea"
                placeholder="请输入数据表备注信息"
              />
            </div>
          </div>
        </section>

        <!-- 字段信息 -->
        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">字段信息</span>
            <span v-if="fieldsParsed" class="ct-section-tip">已自动解析</span>
          </div>

          <div class="ct-table-wrap">
            <table class="ct-table">
              <thead>
                <tr>
                  <th><i class="ct-req">*</i>字段名称</th>
                  <th><i class="ct-req">*</i>字段类型</th>
                  <th>字段描述</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in fields" :key="index">
                  <td>
                    <input
                      v-model="row.name"
                      type="text"
                      class="ct-input"
                      :disabled="fieldsParsed"
                      placeholder="请输入"
                    >
                  </td>
                  <td>
                    <select v-model="row.type" class="ct-input ct-select" :disabled="fieldsParsed">
                      <option value="">请选择</option>
                      <option v-for="t in FIELD_TYPES" :key="t" :value="t">{{ t }}</option>
                    </select>
                  </td>
                  <td>
                    <input v-model="row.desc" type="text" class="ct-input" placeholder="请输入">
                  </td>
                  <td>
                    <button
                      class="ct-trash"
                      type="button"
                      title="删除"
                      :disabled="fieldsParsed"
                      @click="removeField(index)"
                    >
                      <SvgIcon :icon="LAS_ICONS.trash" />
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <button v-if="!fieldsParsed" class="ct-link-btn" type="button" @click="addField">
            <SvgIcon class="ct-link-ic" :icon="LAS_ICONS.plus" />
            <span>添加行</span>
            <SvgIcon class="ct-link-caret" :icon="LAS_ICONS.chevronDown" />
          </button>
        </section>

        <!-- 高级配置 -->
        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">高级配置</span>
          </div>

          <div class="ct-field">
            <label class="ct-label">自定义配置</label>
            <div class="ct-control">
              <div v-for="(row, index) in configs" :key="index" class="ct-cfg">
                <input v-model="row.name" type="text" class="ct-input" placeholder="请输入属性名称">
                <input v-model="row.value" type="text" class="ct-input" placeholder="请输入属性值">
                <button class="ct-trash" type="button" title="删除" @click="removeConfig(index)">
                  <SvgIcon :icon="LAS_ICONS.trash" />
                </button>
              </div>
              <button class="ct-link-btn" type="button" @click="addConfig">
                <SvgIcon class="ct-link-ic" :icon="LAS_ICONS.plus" />
                <span>添加自定义配置</span>
              </button>
            </div>
          </div>
        </section>

        <footer class="ct-foot">
          <button class="ct-primary" type="button" @click="onSubmit">提交</button>
          <button class="ct-ghost" type="button" @click="onCancel">取消</button>
        </footer>
      </form>
    </div>

    <FolderPickerModal
      :visible="pickerVisible"
      :model-value="storagePath"
      @close="pickerVisible = false"
      @select="onPickPath"
    />
  </div>
</template>

<style scoped>
.ct-page {
  position: static;
  inset: auto;
  z-index: auto;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
  background: #fff;
}

.ct-head {
  height: 48px;
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}

.ct-back {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.ct-back:hover {
  color: #165dff;
}

.ct-back svg {
  width: 16px;
  height: 16px;
}

.ct-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.ct-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.ct-form {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.ct-section {
  margin-bottom: 32px;
}

.ct-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.ct-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #165dff;
}

.ct-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.ct-section-tip {
  padding: 1px 8px;
  border-radius: 3px;
  background: #f0fbf3;
  font-size: 11.5px;
  color: #00893a;
}

.ct-hint.is-ok {
  color: #00893a;
}


.ct-field {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.ct-label {
  flex: none;
  width: 145px;
  padding-top: 7px;
  box-sizing: border-box;
  text-align: left;
  font-size: 13px;
  color: #4e5969;
}

.ct-req {
  margin-right: 4px;
  font-style: normal;
  color: #f53f3f;
}

.ct-control {
  flex: 1;
  min-width: 0;
  max-width: 500px;
}

.ct-text {
  display: inline-block;
  padding-top: 7px;
  font-size: 13px;
  color: #1d2129;
}

.ct-input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
}

.ct-input:focus {
  border-color: #165dff;
}

.ct-input.is-error {
  border-color: #f53f3f;
}

.ct-input::placeholder {
  color: #86909c;
}

.ct-input:disabled {
  background: #f7f8fa;
  color: #86909c;
  cursor: not-allowed;
}

.ct-select {
  appearance: none;
  cursor: pointer;
  padding-right: 26px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.ct-textarea {
  width: 100%;
  height: 60px;
  padding: 8px 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  resize: none;
  box-sizing: border-box;
}

.ct-textarea:focus {
  border-color: #165dff;
}

.ct-textarea::placeholder {
  color: #86909c;
}

.ct-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f53f3f;
}

/* 单选 */
.ct-radios {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 6px;
}

.ct-radio {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.ct-radio input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.ct-radio-mark {
  width: 14px;
  height: 14px;
  border: 1px solid #c9cdd4;
  border-radius: 50%;
  background: #fff;
  box-sizing: border-box;
  position: relative;
}

.ct-radio input:checked + .ct-radio-mark {
  border-color: #165dff;
  background: #165dff;
}

.ct-radio input:checked + .ct-radio-mark::after {
  content: '';
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: #fff;
}

.ct-radio input:focus-visible + .ct-radio-mark {
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.2);
}

/* 存储位置 */
.ct-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ct-plus {
  width: 28px;
  height: 28px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.ct-plus:hover {
  border-color: #165dff;
  color: #165dff;
}

.ct-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

.ct-link {
  margin-left: 4px;
  color: #165dff;
  text-decoration: none;
}

.ct-link:hover {
  text-decoration: underline;
}

/* 开关 */
.ct-switch {
  width: 40px;
  height: 20px;
  position: relative;
  padding: 0;
  border: 0;
  border-radius: 10px;
  background: #c7ccd6;
  cursor: pointer;
  transition: background 0.15s ease;
}

.ct-switch.on {
  background: #165dff;
}

.ct-switch-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s ease;
}

.ct-switch.on .ct-switch-dot {
  transform: translateX(20px);
}

/* 字段表 */
.ct-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e6eb;
  border-radius: 4px 4px 0 0;
}

.ct-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 13px;
}

.ct-table th {
  height: 44px;
  padding: 0 20px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.ct-table td {
  padding: 12px 20px;
  border-bottom: 1px solid #f2f3f5;
  vertical-align: middle;
}

.ct-table td .ct-input {
  width: 260px;
}

.ct-table tbody tr:last-child td {
  border-bottom: none;
}

.ct-trash {
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #165dff;
  cursor: pointer;
}

.ct-trash svg {
  width: 15px;
  height: 15px;
}

.ct-trash:hover {
  color: #0e42d2;
}

.ct-trash:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.ct-trash:disabled:hover {
  color: #c9cdd4;
}

/* 链接按钮 */
.ct-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.ct-link-btn:hover {
  text-decoration: underline;
}

.ct-link-ic {
  width: 13px;
  height: 13px;
}

.ct-link-caret {
  width: 12px;
  height: 12px;
}

/* 高级配置 */
.ct-cfg {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
}

.ct-cfg .ct-input {
  width: 285px;
}

/* 底部 */
.ct-foot {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f2f3f5;
  background: #fff;
}

.ct-primary {
  height: 32px;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.ct-primary:hover {
  background: #0e42d2;
}

.ct-ghost {
  height: 32px;
  padding: 0 18px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  cursor: pointer;
}

.ct-ghost:hover {
  border-color: #165dff;
  color: #165dff;
}
</style>
