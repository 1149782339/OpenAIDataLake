<script setup lang="ts">
import { computed } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { FORMAT_CAPABILITY, FORMAT_FEATURES } from '@/components/las/formatCapabilities'

const props = defineProps<{ format: string }>()

const cap = computed(() => FORMAT_CAPABILITY[props.format])

const supported = computed(() =>
  FORMAT_FEATURES.filter((f) => cap.value?.[f.key]).map((f) => ({
    key: f.key,
    text: f.key === 'preview' ? `${cap.value?.preview}${f.label}` : f.label,
  })),
)

const unsupported = computed(() =>
  FORMAT_FEATURES.filter((f) => !cap.value?.[f.key]).map((f) => ({
    key: f.key,
    text: f.label,
  })),
)
</script>

<template>
  <div class="cap-tip">
    <div class="cap-title">支持功能</div>
    <div class="cap-items">
      <span v-for="item in supported" :key="item.key" class="cap-item">
        <SvgIcon class="cap-ic is-ok" :icon="LAS_ICONS.check" />
        <span>{{ item.text }}</span>
      </span>
    </div>

    <template v-if="unsupported.length">
      <div class="cap-title">不支持</div>
      <div class="cap-items">
        <span v-for="item in unsupported" :key="item.key" class="cap-item">
          <SvgIcon class="cap-ic is-no" :icon="LAS_ICONS.close" />
          <span>{{ item.text }}</span>
        </span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.cap-tip {
  max-width: 340px;
  padding: 12px 14px;
  border: 1px solid #e5e6eb;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  font-size: 13px;
  color: #4e5969;
}

.cap-title {
  font-size: 13px;
  color: #86909c;
  margin-bottom: 8px;
}

.cap-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 28px;
  margin-bottom: 10px;
}

.cap-items:last-child {
  margin-bottom: 0;
}

.cap-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 20px;
}

.cap-ic {
  width: 14px;
  height: 14px;
  flex: none;
}

.cap-ic.is-ok {
  color: #00b42a;
}

.cap-ic.is-no {
  color: #f53f3f;
}
</style>
