<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { errorMessage } from '@/api/http'
import { login } from '@/api/console'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function onSubmit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码。'
    return
  }
  submitting.value = true
  try {
    await login(username.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : '/'
    await router.replace(redirect)
  } catch (cause) {
    error.value = errorMessage(cause, '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="onSubmit">
      <h1>登录</h1>
      <p class="login-tip">使用控制台管理员账号登录。</p>
      <label>
        <span>用户名</span>
        <input v-model="username" type="text" autocomplete="username" placeholder="请输入用户名">
      </label>
      <label>
        <span>密码</span>
        <input v-model="password" type="password" autocomplete="current-password" placeholder="请输入密码">
      </label>
      <p v-if="error" class="login-error">{{ error }}</p>
      <button type="submit" :disabled="submitting">{{ submitting ? '登录中…' : '登录' }}</button>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  color: #1d2129;
}

.login-card {
  width: 360px;
  padding: 28px 28px 24px;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  background: #fff;
}

h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.login-tip {
  margin: 8px 0 20px;
  font-size: 13px;
  color: #86909c;
}

label {
  display: block;
  margin-bottom: 14px;
}

label span {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #4e5969;
}

input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  box-sizing: border-box;
}

input:focus {
  border-color: #165dff;
}

.login-error {
  margin: 0 0 12px;
  font-size: 12px;
  color: #f53f3f;
}

button {
  width: 100%;
  height: 32px;
  border: 0;
  border-radius: 4px;
  background: #165dff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

button:hover:not(:disabled) {
  background: #0e42d2;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
