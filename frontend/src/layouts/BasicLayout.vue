<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppToast from '@/components/AppToast.vue'
</script>

<template>
  <div class="app">
    <AppSidebar />
    <RouterView />
    <AppToast />
  </div>
</template>
