<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import AppToast from '@/components/AppToast.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'

interface NavChild {
  label: string
  to: string
}

interface NavGroup {
  key: string
  label: string
  icon: string
  /** 暂未开放的功能 */
  disabled?: boolean
  children: NavChild[]
}

const route = useRoute()
const collapsed = ref(false)

const navGroups: NavGroup[] = [
  { key: 'operators', label: '算子服务', icon: 'operators', disabled: true, children: [] },
  {
    key: 'dataset',
    label: '数据集',
    icon: 'dataset',
    children: [
      { label: '通用数据集', to: '/las/dataset/common' },
      { label: '推理数据集', to: '/las/dataset/infer' },
    ],
  },
  {
    key: 'dev',
    label: '数据开发',
    icon: 'pipeline',
    children: [
      { label: '算子管理', to: '/dev/operators' },
      { label: '工作流管理', to: '/dev/workflow' },
      { label: '执行引擎', to: '/dev/engine' },
      { label: '资产管理', to: '/dev/assets' },
      { label: '数据标注', to: '/dev/labeling' },
    ],
  },
  {
    key: 'lake',
    label: '湖管理',
    icon: 'lake',
    children: [
      { label: '元数据', to: '/las/catalog/metadata' },
      { label: '数据探查', to: '/las/catalog/explore' },
      { label: '权限管理', to: '/las/catalog/permission' },
    ],
  },
]

/** 默认展开：数据集、数据开发、湖管理 */
const expanded = ref<Record<string, boolean>>({ dataset: true, dev: true, lake: true })

const activePath = computed(() => route.path)

function toggleGroup(key: string) {
  expanded.value[key] = !expanded.value[key]
}
</script>

<template>
  <div class="las-shell" :class="{ collapsed }">
    <aside class="las-sidebar">
      <nav class="las-nav">
        <div v-for="group in navGroups" :key="group.key" class="nav-group">
          <button
            class="group-title"
            type="button"
            :class="{ disabled: group.disabled, open: expanded[group.key] }"
            :title="group.disabled ? `${group.label}（暂未开放）` : group.label"
            @click="group.disabled ? undefined : toggleGroup(group.key)"
          >
            <SvgIcon class="grp-ic" :icon="LAS_ICONS[group.icon]" />
            <span class="grp-text">{{ group.label }}</span>
            <SvgIcon
              v-if="group.children.length"
              class="grp-chev"
              :icon="LAS_ICONS.chevronRight"
              :class="{ open: expanded[group.key] }"
            />
          </button>
          <div v-show="expanded[group.key]" class="group-sub">
            <RouterLink
              v-for="child in group.children"
              :key="child.to"
              class="sub-item"
              :class="{ active: activePath.startsWith(child.to) }"
              :to="child.to"
            >
              {{ child.label }}
            </RouterLink>
          </div>
        </div>
      </nav>

      <button class="fold-btn" type="button" title="收起导航" @click="collapsed = !collapsed">
        <SvgIcon :icon="LAS_ICONS.menuFold" />
      </button>
    </aside>

    <div class="las-main">
      <RouterView />
    </div>

    <AppToast />
  </div>
</template>

<style scoped>
.las-shell {
  --las-blue: #165dff;
  --las-text: #1d2129;
  --las-text-2: #4e5969;
  --las-text-3: #86909c;
  --las-border: #e5e6eb;
  --las-fill: #f7f8fa;
  --las-sidebar-w: 200px;

  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #fff;
  color: var(--las-text);
  font-size: 14px;
}

.las-sidebar {
  width: var(--las-sidebar-w);
  flex: none;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #f2f3f5;
  transition: width 0.18s ease;
}

.collapsed .las-sidebar {
  width: 56px;
}

.las-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 8px 0;
}

.nav-group {
  margin-top: 0;
  margin-bottom: 2px;
}

.group-title {
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--las-text);
  font-size: 14px;
  cursor: pointer;
}

.group-title:hover {
  background: rgba(29, 33, 41, 0.06);
}

.group-title.disabled {
  color: var(--las-text-3);
  cursor: not-allowed;
}

.group-title.disabled:hover {
  background: transparent;
}

.grp-ic {
  width: 16px;
  height: 16px;
  flex: none;
  color: var(--las-text-2);
}

.grp-text {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
}

.grp-chev {
  width: 14px;
  height: 14px;
  flex: none;
  color: var(--las-text-3);
  transform: rotate(90deg);
  transition: transform 0.18s ease;
}

.grp-chev.open {
  transform: rotate(-90deg);
}

.sub-item {
  display: block;
  height: 32px;
  line-height: 32px;
  padding: 0 12px 0 32px;
  border-radius: 6px;
  color: var(--las-text-2);
  font-size: 13px;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
}

.sub-item:hover {
  background: #f2f3f5;
  color: var(--las-text);
}

.sub-item.active {
  background: #f2f3f5;
  color: var(--las-text);
  font-weight: 500;
}

.group-sub {
  padding: 2px 0 4px;
}

.collapsed .grp-text,
.collapsed .grp-chev,
.collapsed .sub-item {
  display: none;
}

.collapsed .group-title {
  justify-content: center;
  padding: 0;
}

.fold-btn {
  height: 40px;
  border: 0;
  border-top: 1px solid var(--las-border);
  background: transparent;
  color: var(--las-text-2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fold-btn:hover {
  color: var(--las-text);
}

.fold-btn svg {
  width: 16px;
  height: 16px;
}

.las-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
  background: #fff;
}
</style>
