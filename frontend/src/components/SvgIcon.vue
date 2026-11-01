<script setup lang="ts">
import { computed } from 'vue'
import { ICONS, type IconDef } from './icons'

const props = withDefaults(
  defineProps<{
    name?: string
    icon?: IconDef
    viewBox?: string
  }>(),
  { name: '', icon: undefined, viewBox: undefined },
)

const def = computed<IconDef>(() => props.icon ?? ICONS[props.name] ?? ICONS.more)
const box = computed(() => props.viewBox ?? def.value.viewBox ?? '0 0 24 24')
</script>

<template>
  <svg :viewBox="box" v-bind="def.attrs" v-html="def.body" />
</template>
