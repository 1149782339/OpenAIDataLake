<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS, FORMAT_ICON } from '@/components/las/lasIcons'
import CapabilityTip from '@/components/las/CapabilityTip.vue'
import { FORMATS, useLasStore } from '@/stores/las'

const route = useRoute()
const router = useRouter()
const store = useLasStore()

const kind = computed(() => (route.path.includes('/infer/') ? 'infer' : 'common'))

const SOURCES = [
  { key: 'oss', label: '对象存储 OSS' },
  { key: 'vefs', label: '文件存储 veFS' },
  { key: 'catalog', label: '元数据 Catalog' },
  { key: 'sample', label: '示例文件' },
]

const source = ref('oss')
const enName = ref('')
const cnName = ref('')
const storagePath = ref('')
const format = ref('Lance')
const catalogBucket = ref('')
const catalogTable = ref('')
const advancedOpen = ref(false)
const errorText = ref('')

/** 鼠标悬停在格式卡片上时展示「支持功能」小弹窗 */
const capTip = ref({ visible: false, format: '', x: 0, y: 0 })

function showCapTip(el: HTMLElement, item: string) {
  const rect = el.getBoundingClientRect()
  const half = 190
  const x = Math.min(Math.max(rect.left + rect.width / 2, half), window.innerWidth - half)
  capTip.value = {
    visible: true,
    format: item,
    x,
    y: rect.top - 8,
  }
}

function hideCapTip() {
  capTip.value = { ...capTip.value, visible: false }
}

const title = computed(() =>
  kind.value === 'common' ? '创建数据集' : '创建推理结果集',
)

function genName() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp =
    `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}` +
    `_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  enName.value = `las_dataset_${stamp}`
}

// 与 LAS 控制台一致：进入创建页时自动生成数据集英文名称
genName()

function onSubmit() {
  errorText.value = ''
  if (!enName.value.trim()) {
    errorText.value = '请填写数据集英文名称'
    return
  }
  store.addDataset({
    name: enName.value.trim(),
    cnName: cnName.value.trim(),
    modality: '-',
    tagCount: 0,
    format: format.value,
    samples: 0,
    sizeText: '0 B',
    sizeBytes: 0,
    creator: '2530手机用户#QGDdVm',
    permission: '我创建的',
    kind: kind.value,
    source: SOURCES.find((s) => s.key === source.value)?.label ?? '对象存储 OSS',
    path: storagePath.value.trim(),
  })
  router.push(kind.value === 'common' ? '/las/dataset/common' : '/las/dataset/infer')
}

function onCancel() {
  router.back()
}
</script>

<template>
  <div class="create-page">
    <header class="create-head">
      <button class="back-btn" type="button" @click="onCancel">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="create-title">{{ title }}</span>
    </header>

    <div class="create-body">
      <form class="create-form" @submit.prevent="onSubmit">
        <section class="form-section">
          <div class="section-head">
            <span class="section-bar" />
            <span class="section-title">基本信息</span>
            <SvgIcon class="section-chev" :icon="LAS_ICONS.chevronDown" />
          </div>

          <div class="field">
            <label class="field-label">数据集英文名称<i class="required">*</i></label>
            <div class="field-control">
              <input v-model="enName" type="text" placeholder="请输入" class="input">
              <button class="mini-btn" type="button" @click="genName">自动生成</button>
            </div>
          </div>

          <div class="field">
            <label class="field-label">数据集中文名称</label>
            <div class="field-control">
              <input v-model="cnName" type="text" placeholder="请输入" class="input">
            </div>
          </div>

          <div class="field">
            <label class="field-label">数据来源<i class="required">*</i></label>
            <div class="field-control">
              <div class="tabs">
                <button
                  v-for="item in SOURCES"
                  :key="item.key"
                  class="tab"
                  :class="{ on: source === item.key }"
                  type="button"
                  @click="source = item.key"
                >
                  {{ item.label }}
                </button>
              </div>
              <div class="inline">
                <input v-model="storagePath" type="text" class="input" placeholder="请填入或选择数据在 OSS 中的存储位置">
                <button class="mini-btn" type="button">＋ 从 OSS 选择</button>
              </div>
            </div>
          </div>

          <div class="field">
            <label class="field-label">
              数据集格式
              <SvgIcon class="label-ic" :icon="LAS_ICONS.question" />
            </label>
            <div class="field-control">
              <div class="chip-grid">
                <button
                  v-for="item in FORMATS"
                  :key="item"
                  class="chip"
                  :class="{ on: format === item }"
                  type="button"
                  @click="format = item"
                  @mouseenter="showCapTip($event.currentTarget as HTMLElement, item)"
                  @mouseleave="hideCapTip"
                >
                  <SvgIcon :icon="LAS_ICONS[FORMAT_ICON[item]]" />
                  <span>{{ item }}</span>
                </button>
              </div>
            </div>
          </div>

          <div class="field">
            <label class="field-label">元数据存储</label>
            <div class="field-control">
              <div class="inline">
                <select v-model="catalogBucket" class="input select">
                  <option value="">请选择</option>
                  <option value="default">default</option>
                  <option value="lakehouse">lakehouse</option>
                </select>
                <input v-model="catalogTable" type="text" class="input" placeholder="请输入 table 路径">
              </div>
              <p class="field-hint">
                非必需。选择Catalog路径，同Catalog注册元数据信息。
                <a href="#" class="link" @click.prevent>使用默认路径</a>
              </p>
            </div>
          </div>
        </section>

        <section class="form-section">
          <button class="section-head as-button" type="button" @click="advancedOpen = !advancedOpen">
            <span class="section-bar" />
            <span class="section-title">高级设置</span>
            <SvgIcon
              class="section-chev"
              :class="{ open: advancedOpen }"
              :icon="LAS_ICONS.chevronRight"
            />
          </button>
          <div v-if="advancedOpen" class="advanced">
            <div class="field">
              <label class="field-label">数据扫描</label>
              <div class="field-control">
                <label class="check-row">
                  <input type="checkbox" checked>
                  <span>创建后自动扫描数据并统计样本数</span>
                </label>
              </div>
            </div>
          </div>
        </section>

        <p v-if="errorText" class="error-text">{{ errorText }}</p>

        <footer class="create-foot">
          <button class="primary-btn" type="button" @click="onSubmit">创建</button>
          <button class="ghost-btn" type="button" @click="onCancel">取消</button>
        </footer>
      </form>
    </div>

    <Teleport to="body">
      <div
        v-if="capTip.visible"
        class="cap-tip-wrap"
        :style="{ left: `${capTip.x}px`, top: `${capTip.y}px` }"
        @mouseleave="hideCapTip"
      >
        <CapabilityTip :format="capTip.format" />
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.create-page {
  /* 覆盖 platform.css 中同名类的 fixed 定位 */
  position: static;
  inset: auto;
  z-index: auto;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
  background: #fff;
}

.create-head {
  height: 48px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e6eb;
}

.back-btn {
  width: 24px;
  height: 24px;
  margin-left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 4px;
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

.create-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.create-body {
  flex: 1;
  overflow: auto;
}

.create-form {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.form-section {
  margin-bottom: 28px;
}

.section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
  border: 0;
  background: none;
  padding: 0;
}

.section-head.as-button {
  cursor: pointer;
}

.section-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #165dff;
}

.create-form .section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

/* 覆盖 platform.css 中同名 section-title 的伪元素装饰 */
.create-form .section-title::before {
  content: none;
}

.section-chev {
  width: 14px;
  height: 14px;
  color: #86909c;
}

.section-chev.open {
  transform: rotate(90deg);
}

.field {
  display: flex;
  gap: 16px;
  margin-bottom: 18px;
}

.field-label {
  width: 120px;
  flex: none;
  padding-top: 7px;
  text-align: right;
  font-size: 13px;
  color: #4e5969;
  position: relative;
}

.required {
  color: #f53f3f;
  font-style: normal;
  margin-left: 2px;
}

.label-ic {
  width: 13px;
  height: 13px;
  color: #86909c;
  vertical-align: -2px;
}

.field-control {
  flex: 1;
  min-width: 0;
}

.input {
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

.input:focus {
  border-color: #165dff;
}

.input::placeholder {
  color: #86909c;
}

.select {
  width: 160px;
  flex: none;
}

.inline {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}

.field-control > .inline:first-child {
  margin-top: 0;
}

.mini-btn {
  height: 32px;
  padding: 0 12px;
  flex: none;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.mini-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab {
  height: 30px;
  padding: 0 14px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.tab:hover {
  border-color: #165dff;
  color: #165dff;
}

.tab.on {
  border-color: #165dff;
  background: #e8f3ff;
  color: #165dff;
}

.chip-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 32px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.chip svg {
  width: 14px;
  height: 14px;
}

.chip:hover {
  border-color: #165dff;
  color: #165dff;
}

.chip.on {
  border-color: #165dff;
  background: #e8f3ff;
  color: #165dff;
}

.field-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}

.link {
  color: #165dff;
  text-decoration: none;
}

.check-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border-radius: 0;
  background: transparent;
  font-size: 13px;
  color: #4e5969;
}

.check-row:hover {
  background: transparent;
}

.error-text {
  color: #f53f3f;
  font-size: 13px;
}

.create-foot {
  position: sticky;
  bottom: 0;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 0;
  background: #fff;
  border-top: 1px solid #f2f3f5;
}

.primary-btn {
  height: 32px;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.primary-btn:hover {
  background: #0e42d2;
}

.ghost-btn {
  height: 32px;
  padding: 0 18px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  cursor: pointer;
}

.ghost-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.cap-tip-wrap {
  position: fixed;
  z-index: 1100;
  transform: translate(-50%, -100%);
}
</style>
