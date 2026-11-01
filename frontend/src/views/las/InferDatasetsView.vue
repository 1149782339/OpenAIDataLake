<script setup lang="ts">
import DatasetListView from '@/components/las/DatasetListView.vue'
</script>

<template>
  <DatasetListView
    kind="infer"
    title="推理结果集"
    desc="支持方舟平台大模型推理数据一键回流，为模型精调等场景提供数据支撑。协同方舟，构建模型持续迭代的增益回路。"
    action-label="前往创建"
  />
</template>
