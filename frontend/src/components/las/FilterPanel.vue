<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS, FORMAT_ICON, MODALITY_ICON } from '@/components/las/lasIcons'
import RangeSlider from '@/components/las/RangeSlider.vue'
import {
  ALL_TAGS,
  FORMATS,
  MODALITIES,
  PERMISSIONS,
  SAMPLE_MARKS,
  SIZE_MARKS,
  useLasStore,
  type Permission,
} from '@/stores/las'

const props = defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ (e: 'update:collapsed', value: boolean): void }>()

const store = useLasStore()
const tagOpen = ref(false)

const selectedTagsText = computed(() =>
  store.tags.length ? store.tags.join('、') : '请选择',
)

function toggleTag(tag: string) {
  store.tags = store.tags.includes(tag)
    ? store.tags.filter((t) => t !== tag)
    : [...store.tags, tag]
}

function onPermissionChange(event: Event) {
  store.permission = (event.target as HTMLSelectElement).value as Permission
}

function onDocClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.closest('.tag-select')) tagOpen.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<template>
  <aside class="filter-panel" :class="{ collapsed: props.collapsed }">
    <template v-if="!props.collapsed">
      <div class="filter-head">
        <span class="filter-title">筛选条件</span>
        <span class="filter-ops">
          <button class="reset-btn" type="button" @click="store.resetFilters()">重置</button>
          <button
            class="fold-btn"
            type="button"
            title="收起筛选"
            @click="emit('update:collapsed', true)"
          >
            <SvgIcon :icon="LAS_ICONS.chevronRight" />
          </button>
        </span>
      </div>

      <div class="filter-body">
        <section class="f-block">
          <div class="f-label">模态</div>
          <div class="chip-group">
            <button
              v-for="m in MODALITIES"
              :key="m"
              class="chip"
              :class="{ on: store.modalities.has(m) }"
              type="button"
              @click="store.toggleModality(m)"
            >
              <SvgIcon :icon="LAS_ICONS[MODALITY_ICON[m]]" />
              <span>{{ m }}</span>
            </button>
          </div>
        </section>

        <section class="f-block">
          <div class="f-label">格式</div>
          <div class="chip-group">
            <button
              v-for="f in FORMATS"
              :key="f"
              class="chip"
              :class="{ on: store.formats.has(f) }"
              type="button"
              @click="store.toggleFormat(f)"
            >
              <SvgIcon :icon="LAS_ICONS[FORMAT_ICON[f]]" />
              <span>{{ f }}</span>
            </button>
          </div>
        </section>

        <section class="f-block">
          <div class="f-label">权限</div>
          <select class="f-select" :value="store.permission" @change="onPermissionChange">
            <option v-for="p in PERMISSIONS" :key="p" :value="p">{{ p }}</option>
          </select>
        </section>

        <section class="f-block">
          <div class="f-label">标签</div>
          <div class="tag-select">
            <button class="f-select tag-trigger" type="button" @click.stop="tagOpen = !tagOpen">
              <span :class="{ placeholder: !store.tags.length }">{{ selectedTagsText }}</span>
              <SvgIcon class="chev" :icon="LAS_ICONS.chevronDown" />
            </button>
            <div v-if="tagOpen" class="tag-pop" @click.stop>
              <label v-for="tag in ALL_TAGS" :key="tag" class="tag-row">
                <input
                  type="checkbox"
                  :checked="store.tags.includes(tag)"
                  @change="toggleTag(tag)"
                >
                <span>{{ tag }}</span>
              </label>
            </div>
          </div>
        </section>

        <section class="f-block">
          <div class="f-label">创建时间</div>
          <div class="date-range">
            <input v-model="store.createdFrom" type="date" placeholder="开始日期">
            <span class="date-sep">-</span>
            <input v-model="store.createdTo" type="date" placeholder="结束日期">
            <SvgIcon class="date-icon" :icon="LAS_ICONS.calendar" />
          </div>
        </section>

        <section class="f-block">
          <div class="f-label">样本数</div>
          <RangeSlider v-model="store.sampleRange" :marks="SAMPLE_MARKS" />
        </section>

        <section class="f-block">
          <div class="f-label">大小</div>
          <RangeSlider v-model="store.sizeRange" :marks="SIZE_MARKS" />
        </section>
      </div>
    </template>

    <button
      v-else
      class="expand-btn"
      type="button"
      title="展开筛选"
      @click="emit('update:collapsed', false)"
    >
      <span class="expand-text">筛选条件</span>
      <SvgIcon :icon="LAS_ICONS.chevronLeft" />
    </button>
  </aside>
</template>

<style scoped>
.filter-panel {
  width: 300px;
  flex: none;
  border-left: 1px solid #e5e6eb;
  padding: 0 16px 24px;
  overflow-y: auto;
}

.filter-panel.collapsed {
  width: 40px;
  padding: 0;
  overflow: visible;
}

.filter-head {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f2f3f5;
}

.filter-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.filter-ops {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reset-btn {
  border: 0;
  background: none;
  color: var(--las-blue, #165dff);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}

.fold-btn {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: none;
  color: #86909c;
  cursor: pointer;
}

.fold-btn:hover {
  color: #1d2129;
}

.fold-btn svg {
  width: 14px;
  height: 14px;
}

.filter-body {
  padding-top: 12px;
}

.f-block {
  margin-bottom: 20px;
}

.f-label {
  font-size: 13px;
  color: #1d2129;
  margin-bottom: 8px;
}

.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 26px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #4e5969;
  font-size: 12px;
  cursor: pointer;
}

.chip svg {
  width: 13px;
  height: 13px;
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

.f-select {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  color: #1d2129;
  font-size: 13px;
  outline: none;
}

.f-select:focus {
  border-color: #165dff;
}

.tag-select {
  position: relative;
}

.tag-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  text-align: left;
}

.tag-trigger .placeholder {
  color: #86909c;
}

.chev {
  width: 14px;
  height: 14px;
  color: #86909c;
}

.tag-pop {
  position: absolute;
  z-index: 20;
  top: 36px;
  left: 0;
  right: 0;
  padding: 6px 0;
  background: #fff;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 12px;
  font-size: 13px;
  color: #1d2129;
  cursor: pointer;
}

.tag-row:hover {
  background: #f7f8fa;
}

.date-range {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
}

.date-range input {
  width: 100%;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 12px;
  color: #1d2129;
  outline: none;
}

.date-range input:focus {
  border-color: #165dff;
}

.date-range input::placeholder {
  color: #86909c;
}

.date-sep {
  color: #86909c;
  font-size: 12px;
}

.date-icon {
  width: 14px;
  height: 14px;
  color: #86909c;
  margin-left: -26px;
  pointer-events: none;
}

.expand-btn {
  width: 40px;
  height: 100%;
  border: 0;
  border-left: 1px solid #e5e6eb;
  background: #f7f8fa;
  color: #4e5969;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.expand-btn:hover {
  color: #165dff;
}

.expand-btn svg {
  width: 14px;
  height: 14px;
}

.expand-text {
  writing-mode: vertical-rl;
  letter-spacing: 1px;
}
</style>
