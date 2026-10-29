<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SvgIcon from './SvgIcon.vue'

const route = useRoute()
const permGroupCollapsed = ref(false)

const current = computed(() => route.name as string)
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <SvgIcon name="brand" />
      <span>多模态数据库管理平台</span>
    </div>

    <nav class="nav">
      <RouterLink
        class="nav-title nav-entry"
        :class="{ active: current === 'datasets' }"
        to="/datasets"
      >
        <SvgIcon class="grp-ic" name="navDatasets" />
        <span>数据集</span>
      </RouterLink>

      <RouterLink
        class="nav-title nav-entry"
        :class="{ active: current === 'metadata' }"
        to="/metadata"
      >
        <SvgIcon class="grp-ic" name="navCatalog" />
        <span>数据目录</span>
      </RouterLink>

      <RouterLink
        class="nav-title nav-entry"
        :class="{ active: current === 'explore' }"
        to="/explore"
      >
        <SvgIcon class="grp-ic" name="run" />
        <span>数据探查</span>
      </RouterLink>

      <div class="nav-group" :class="{ collapsed: permGroupCollapsed }">
        <button class="nav-title" type="button" @click="permGroupCollapsed = !permGroupCollapsed">
          <SvgIcon class="grp-ic" name="navShield" />
          <span>权限管理</span>
          <SvgIcon class="chev" name="chevDown" />
        </button>
        <div class="nav-sub">
          <RouterLink
            class="nav-item"
            :class="{ active: current === 'apikeys' }"
            to="/apikeys"
          >
            API Key 管理
          </RouterLink>
          <RouterLink
            class="nav-item"
            :class="{ active: current === 'grants' }"
            to="/grants"
          >
            授权管理
          </RouterLink>
        </div>
      </div>
    </nav>
  </aside>
</template>
