<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import DatasetTable from '@/components/las/DatasetTable.vue'
import FilterPanel from '@/components/las/FilterPanel.vue'
import { useLasStore } from '@/stores/las'

const props = defineProps<{
  kind: 'common' | 'infer'
  title: string
  desc: string
  actionLabel: string
}>()

const router = useRouter()
const store = useLasStore()

const guideOpen = ref(false)
const filterCollapsed = ref(false)

watch(
  () => props.kind,
  (kind) => store.setKind(kind),
  { immediate: true },
)

const rows = computed(() => store.pageRows)

const guideSteps = computed(() =>
  props.kind === 'common'
    ? [
        {
          step: '第1步',
          name: '创建数据集',
          text: '通过指定数据所在的存储目录，创建数据集',
          link: '',
        },
        {
          step: '第2步',
          name: '数据加工',
          text: '快速查询和编辑，大量数据可使用工作流中的内置算子，进行批量清洗。',
          link: '算子 SDK',
        },
        {
          step: '第3步',
          name: '使用数据集',
          text: '调用数据集的存储路径，也可直接另存到火山方舟数据集。',
          link: '数据源SDK',
        },
      ]
    : [
        {
          step: '第1步',
          name: '前往方舟',
          text: '在方舟平台发起模型推理，推理结果一键回流到 LAS。',
          link: '前往方舟',
        },
        {
          step: '第2步',
          name: '回流数据',
          text: '回流完成后自动生成推理结果集，可直接用于模型精调。',
          link: '',
        },
        {
          step: '第3步',
          name: '使用数据集',
          text: '在推理结果集中查询推理明细，构建模型持续迭代的增益回路。',
          link: '数据源SDK',
        },
      ],
)

function onSearch(event: Event) {
  store.setQuery((event.target as HTMLInputElement).value)
}

function onPageSize(event: Event) {
  store.setPageSize(Number((event.target as HTMLSelectElement).value))
}

function goPage(page: number) {
  store.page = Math.min(Math.max(1, page), store.pageCount)
}

function onCreate() {
  router.push(
    props.kind === 'common' ? '/las/dataset/common/create' : '/las/dataset/infer/create',
  )
}
</script>

<template>
  <div class="las-page">
    <header class="page-head">
      <div class="head-main">
        <h1 class="page-title">{{ props.title }}</h1>
        <p class="page-desc">{{ props.desc }}</p>
      </div>
      <div class="head-ops">
        <button class="ghost-btn" type="button">
          <SvgIcon :icon="LAS_ICONS.doc" />
          <span>使用说明</span>
        </button>
        <button class="ghost-btn" type="button" @click="guideOpen = !guideOpen">
          <SvgIcon :icon="LAS_ICONS.guide" />
          <span>{{ guideOpen ? '收起引导' : '展开引导' }}</span>
        </button>
      </div>
    </header>

    <section v-if="guideOpen" class="guide">
      <div v-for="item in guideSteps" :key="item.step" class="guide-step">
        <div class="guide-badge">{{ item.step }}</div>
        <div class="guide-body">
          <div class="guide-name">{{ item.name }}</div>
          <div class="guide-text">{{ item.text }}</div>
          <a v-if="item.link" class="guide-link" href="#" @click.prevent>{{ item.link }}</a>
        </div>
      </div>
    </section>

    <div class="page-body">
      <section class="main-col">
        <div class="toolbar">
          <button class="primary-btn" type="button" @click="onCreate">
            <SvgIcon :icon="LAS_ICONS.plus" />
            <span>{{ props.actionLabel }}</span>
          </button>
          <div class="toolbar-right">
            <div class="search">
              <SvgIcon :icon="LAS_ICONS.search" class="search-ic" />
              <input
                type="text"
                placeholder="搜索数据集名称"
                autocomplete="off"
                :value="store.query"
                @input="onSearch"
              >
            </div>
            <button
              class="filter-btn"
              :class="{ active: store.hasFilter }"
              type="button"
              title="筛选"
              @click="filterCollapsed = !filterCollapsed"
            >
              <SvgIcon :icon="LAS_ICONS.filter" />
            </button>
          </div>
        </div>

        <DatasetTable :rows="rows" :kind="props.kind" />

        <div class="pagination">
          <span class="page-total">共 {{ store.total }} 条</span>
          <div class="page-buttons">
            <button
              class="page-btn"
              type="button"
              :disabled="store.page <= 1"
              @click="goPage(store.page - 1)"
            >
              <SvgIcon :icon="LAS_ICONS.chevronLeft" />
            </button>
            <button
              v-for="i in store.pageCount"
              :key="i"
              class="page-btn"
              :class="{ active: i === store.page }"
              type="button"
              @click="goPage(i)"
            >
              {{ i }}
            </button>
            <button
              class="page-btn"
              type="button"
              :disabled="store.page >= store.pageCount"
              @click="goPage(store.page + 1)"
            >
              <SvgIcon :icon="LAS_ICONS.chevronRight" />
            </button>
          </div>
          <select class="page-size" :value="store.pageSize" @change="onPageSize">
            <option :value="10">10 条/页</option>
            <option :value="20">20 条/页</option>
            <option :value="50">50 条/页</option>
          </select>
        </div>
      </section>

      <FilterPanel v-model:collapsed="filterCollapsed" />
    </div>
  </div>
</template>

<style scoped>
.las-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px 14px;
  border-bottom: 1px solid #e5e6eb;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #86909c;
}

.head-ops {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}

.ghost-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.ghost-btn svg {
  width: 14px;
  height: 14px;
}

.guide {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: #f7f8fa;
  border-bottom: 1px solid #e5e6eb;
}

.guide-step {
  flex: 1;
  display: flex;
  gap: 10px;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  padding: 12px 14px;
}

.guide-badge {
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  background: #e8f3ff;
  color: #165dff;
  font-size: 12px;
  line-height: 20px;
  height: fit-content;
}

.guide-name {
  font-size: 13px;
  font-weight: 500;
  color: #1d2129;
  margin-bottom: 4px;
}

.guide-text {
  font-size: 12px;
  color: #86909c;
  line-height: 1.6;
}

.guide-link {
  display: inline-block;
  margin-top: 6px;
  color: #165dff;
  font-size: 12px;
  text-decoration: none;
}

.page-body {
  flex: 1;
  min-height: 0;
  display: flex;
}

.main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 16px 20px 20px;
  overflow: auto;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.primary-btn {
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

.primary-btn:hover {
  background: #0e42d2;
}

.primary-btn svg {
  width: 13px;
  height: 13px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 220px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
}

.search:focus-within {
  border-color: #165dff;
}

.search-ic {
  width: 14px;
  height: 14px;
  color: #86909c;
}

.search input {
  width: 100%;
  height: 30px;
  padding: 0;
  border: 0;
  outline: none;
  font-size: 13px;
  color: #1d2129;
  background: none;
}

.search input::placeholder {
  color: #86909c;
}

.filter-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  cursor: pointer;
}

.filter-btn:hover {
  border-color: #165dff;
  color: #165dff;
}

.filter-btn.active {
  border-color: #165dff;
  color: #165dff;
}

.filter-btn svg {
  width: 15px;
  height: 15px;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.page-total {
  font-size: 13px;
  color: #4e5969;
}

.page-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  cursor: pointer;
}

.page-btn:disabled {
  color: #c9cdd4;
  cursor: not-allowed;
}

.page-btn.active {
  border-color: #165dff;
  color: #165dff;
}

.page-btn svg {
  width: 14px;
  height: 14px;
}

.page-size {
  height: 28px;
  padding: 0 6px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  outline: none;
}
</style>
