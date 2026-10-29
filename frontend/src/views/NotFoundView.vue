<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div style="padding: 80px 20px; text-align: center">
    <h1 style="margin: 0 0 8px; font-size: 40px; font-weight: 600; color: #1f2329">404</h1>
    <p style="margin: 0 0 24px; font-size: 14px; color: #86909c">抱歉，你访问的页面不存在。</p>
    <RouterLink to="/datasets">
      <button class="btn-primary" type="button">返回首页</button>
    </RouterLink>
  </div>
</template>
