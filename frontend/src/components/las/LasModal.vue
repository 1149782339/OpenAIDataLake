<script setup lang="ts">
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'

withDefaults(defineProps<{ visible: boolean; title: string; width?: string }>(), {
  width: '580px',
})

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="lm-mask" @click="emit('close')" />
    <div v-if="visible" class="lm-modal" :style="{ width }">
      <header class="lm-modal-head">
        <h3 class="lm-modal-title">{{ title }}</h3>
        <button class="lm-modal-close" type="button" title="关闭" @click="emit('close')">
          <SvgIcon :icon="LAS_ICONS.close" />
        </button>
      </header>

      <div class="lm-modal-body">
        <slot />
      </div>

      <footer v-if="$slots.footer" class="lm-modal-foot">
        <slot name="footer" />
      </footer>
    </div>
  </Teleport>
</template>

<style scoped>
.lm-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(29, 33, 41, 0.6);
}

.lm-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  z-index: 1001;
  transform: translate(-50%, -50%);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  animation: lm-zoom-in 0.18s ease;
}

@keyframes lm-zoom-in {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.lm-modal-head {
  position: relative;
  padding: 20px 24px 16px;
}

.lm-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  line-height: 26px;
  color: #0c0d0e;
}

.lm-modal-close {
  position: absolute;
  top: 22px;
  right: 20px;
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.lm-modal-close svg {
  width: 14px;
  height: 14px;
}

.lm-modal-close:hover {
  color: #0c0d0e;
}

.lm-modal-body {
  padding: 14px 24px 24px;
}

.lm-modal-foot {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 24px 20px;
}
</style>
