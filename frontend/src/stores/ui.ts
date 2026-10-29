import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 全局提示（Toast） */
export const useUiStore = defineStore('ui', () => {
  const toastMessage = ref('')
  const toastVisible = ref(false)
  let timer: number | undefined

  function toast(message: string) {
    toastMessage.value = message
    toastVisible.value = true
    if (timer !== undefined) window.clearTimeout(timer)
    timer = window.setTimeout(() => {
      toastVisible.value = false
    }, 2200)
  }

  return { toastMessage, toastVisible, toast }
})
