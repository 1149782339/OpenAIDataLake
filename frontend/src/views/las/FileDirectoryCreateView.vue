<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { ApiError, errorMessage } from '@/api/http'
import { createDirectory, listUsers, probeDirectory, type ConsoleUser } from '@/api/console'

interface GrantRow {
  key: number
  userId: string
  canList: boolean
  canRead: boolean
  canWrite: boolean
  canDelete: boolean
}

const router = useRouter()

const logicalName = ref('')
const endpoint = ref('')
const accessKey = ref('')
const secretKey = ref('')
const folder = ref('')
const directories = ref<string[]>([])
const users = ref<ConsoleUser[]>([])
const grants = ref<GrantRow[]>([blankGrant()])

const nameError = ref('')
const connError = ref('')
const folderError = ref('')
const grantError = ref('')
const formError = ref('')
const testMessage = ref('')
const testOk = ref(false)
const connected = ref(false)
const testing = ref(false)
const saving = ref(false)
const userError = ref('')

let rowKey = 1
let probeSeq = 0

function blankGrant(): GrantRow {
  rowKey += 1
  return { key: rowKey, userId: '', canList: false, canRead: false, canWrite: false, canDelete: false }
}

function addGrant() {
  grants.value.push(blankGrant())
}

function removeGrant(index: number) {
  if (grants.value.length === 1) {
    grants.value[0] = blankGrant()
    return
  }
  grants.value.splice(index, 1)
}

watch([endpoint, accessKey, secretKey], () => {
  probeSeq += 1
  connected.value = false
  testOk.value = false
  testMessage.value = ''
  connError.value = ''
  directories.value = []
  folder.value = ''
})

function back() {
  router.push('/catalog/directories')
}

async function onTest() {
  connError.value = ''
  testMessage.value = ''
  testOk.value = false
  connected.value = false
  directories.value = []
  folder.value = ''
  if (!endpoint.value.trim() || !accessKey.value.trim() || !secretKey.value) {
    connError.value = '先填写 Endpoint、Access Key 和 Secret Key。'
    return
  }
  const seq = ++probeSeq
  testing.value = true
  try {
    const result = await probeDirectory(endpoint.value.trim(), accessKey.value.trim(), secretKey.value)
    if (seq !== probeSeq) return
    directories.value = result.directories
    connected.value = true
    testOk.value = true
    testMessage.value = result.directories.length
      ? '已连通，可以在下面选择目录。'
      : '已连通，这台存储上没有可选目录。'
  } catch (cause) {
    if (seq !== probeSeq) return
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/directories/create' } })
      return
    }
    testOk.value = false
    testMessage.value = errorMessage(cause, '连接失败')
  } finally {
    if (seq === probeSeq) testing.value = false
  }
}

function selectedGrants() {
  return grants.value.filter((row) => row.userId)
}

async function onSubmit() {
  nameError.value = ''
  connError.value = ''
  folderError.value = ''
  grantError.value = ''
  formError.value = ''
  if (!logicalName.value.trim()) nameError.value = '请输入逻辑名称。'
  if (!connected.value) connError.value = '请先测试连接。'
  if (connected.value && !folder.value) folderError.value = '请选择文件目录。'
  const chosen = selectedGrants()
  const userIds = chosen.map((row) => row.userId)
  if (new Set(userIds).size !== userIds.length) {
    grantError.value = '同一个用户不能重复授权。'
  }
  if (chosen.some((row) => !row.canList && !row.canRead && !row.canWrite && !row.canDelete)) {
    grantError.value = '每一行权限至少要勾选一项。'
  }
  if (nameError.value || connError.value || folderError.value || grantError.value) return

  saving.value = true
  try {
    await createDirectory({
      logicalName: logicalName.value.trim(),
      endpoint: endpoint.value.trim(),
      accessKey: accessKey.value.trim(),
      secretKey: secretKey.value,
      directory: folder.value,
      grants: chosen.map((row) => ({
        userId: Number(row.userId),
        canList: row.canList,
        canRead: row.canRead,
        canWrite: row.canWrite,
        canDelete: row.canDelete,
      })),
    })
    await router.push('/catalog/directories')
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/directories/create' } })
      return
    }
    formError.value = errorMessage(cause, '创建失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const all = await listUsers()
    users.value = all.filter((user) => user.enabled)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/directories/create' } })
      return
    }
    userError.value = errorMessage(cause, '加载用户失败')
  }
})
</script>

<template>
  <div class="ct-page">
    <header class="ct-head">
      <button class="ct-back" type="button" title="返回" @click="back">
        <SvgIcon :icon="LAS_ICONS.chevronLeft" />
      </button>
      <span class="ct-title">创建文件目录</span>
    </header>

    <div class="ct-body">
      <form class="ct-form" @submit.prevent="onSubmit">
        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">基本信息</span>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>逻辑名称</label>
            <div class="ct-control">
              <input
                v-model="logicalName"
                type="text"
                class="ct-input"
                :class="{ 'is-error': nameError }"
                placeholder="camera_capture_project/bucket1/camera"
                autocomplete="off"
                @input="nameError = ''"
              >
              <div v-if="nameError" class="ct-error">{{ nameError }}</div>
            </div>
          </div>
        </section>

        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">数据源</span>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>Endpoint</label>
            <div class="ct-control">
              <input
                v-model="endpoint"
                type="url"
                class="ct-input"
                placeholder="http://10.0.0.8:9000"
                autocomplete="off"
              >
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>Access Key</label>
            <div class="ct-control">
              <input v-model="accessKey" type="text" class="ct-input" autocomplete="off">
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>Secret Key</label>
            <div class="ct-control">
              <input v-model="secretKey" type="password" class="ct-input" autocomplete="off">
            </div>
          </div>
          <div class="ct-field">
            <span class="ct-label" />
            <div class="ct-control">
              <div class="ct-test">
                <button class="ct-ghost" type="button" :disabled="testing" @click="onTest">
                  {{ testing ? '测试中…' : '测试连接' }}
                </button>
                <p v-if="testMessage" class="ct-test-msg" :class="{ ok: testOk }">{{ testMessage }}</p>
              </div>
              <div v-if="connError" class="ct-error">{{ connError }}</div>
            </div>
          </div>
        </section>

        <section v-if="connected" class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">物理目录</span>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>文件目录</label>
            <div class="ct-control">
              <select v-model="folder" class="ct-input ct-select" :class="{ 'is-error': folderError }" @change="folderError = ''">
                <option value="">选择测试连接返回的目录</option>
                <option v-for="item in directories" :key="item" :value="item">{{ item }}</option>
              </select>
              <div v-if="folderError" class="ct-error">{{ folderError }}</div>
            </div>
          </div>
        </section>

        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">权限</span>
          </div>
          <p v-if="userError" class="ct-error ct-block-error">{{ userError }}</p>
          <div class="ct-table-wrap">
            <table class="ct-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>列出</th>
                  <th>读</th>
                  <th>写</th>
                  <th>删</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, index) in grants" :key="row.key">
                  <td>
                    <select v-model="row.userId" class="ct-input ct-select" aria-label="授权用户">
                      <option value="">选择用户</option>
                      <option v-for="user in users" :key="user.id" :value="String(user.id)">{{ user.username }}</option>
                    </select>
                  </td>
                  <td><input v-model="row.canList" type="checkbox" aria-label="列出"></td>
                  <td><input v-model="row.canRead" type="checkbox" aria-label="读"></td>
                  <td><input v-model="row.canWrite" type="checkbox" aria-label="写"></td>
                  <td><input v-model="row.canDelete" type="checkbox" aria-label="删"></td>
                  <td>
                    <button class="ct-link-btn ct-remove" type="button" @click="removeGrant(index)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button class="ct-link-btn" type="button" @click="addGrant">
            <SvgIcon class="ct-link-ic" :icon="LAS_ICONS.plus" />
            <span>添加用户</span>
          </button>
          <div v-if="grantError" class="ct-error">{{ grantError }}</div>
        </section>

        <p v-if="formError" class="ct-error ct-block-error">{{ formError }}</p>

        <footer class="ct-foot">
          <button class="ct-ghost" type="button" @click="back">取消</button>
          <button class="ct-primary" type="submit" :disabled="saving">{{ saving ? '提交中…' : '确定' }}</button>
        </footer>
      </form>
    </div>
  </div>
</template>

<style scoped>
.ct-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 980px;
  background: #fff;
}

.ct-head {
  height: 48px;
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e6eb;
  background: #fff;
}

.ct-back {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: none;
  color: #4e5969;
  cursor: pointer;
}

.ct-back:hover {
  color: #165dff;
}

.ct-back svg {
  width: 16px;
  height: 16px;
}

.ct-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.ct-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.ct-form {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.ct-section {
  margin-bottom: 32px;
}

.ct-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.ct-bar {
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: #165dff;
}

.ct-section-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
}

.ct-field {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.ct-label {
  flex: none;
  width: 145px;
  padding-top: 7px;
  box-sizing: border-box;
  text-align: left;
  font-size: 13px;
  color: #4e5969;
}

.ct-req {
  margin-right: 4px;
  font-style: normal;
  color: #f53f3f;
}

.ct-control {
  flex: 1;
  min-width: 0;
  max-width: 500px;
}

.ct-input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #1d2129;
  outline: none;
  box-sizing: border-box;
}

.ct-input:focus {
  border-color: #165dff;
}

.ct-input.is-error {
  border-color: #f53f3f;
}

.ct-input::placeholder {
  color: #86909c;
}

.ct-select {
  appearance: none;
  cursor: pointer;
  padding-right: 26px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}

.ct-error {
  margin-top: 4px;
  font-size: 12px;
  color: #f53f3f;
}

.ct-block-error {
  margin: 0 0 12px;
}

.ct-test {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ct-test-msg {
  margin: 0;
  font-size: 12px;
  color: #f53f3f;
}

.ct-test-msg.ok {
  color: #00b42a;
}

.ct-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e6eb;
  border-radius: 4px;
}

.ct-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.ct-table th {
  height: 40px;
  padding: 0 16px;
  background: #f7f8fa;
  text-align: left;
  color: #1d2129;
  font-weight: 500;
  border-bottom: 1px solid #e5e6eb;
}

.ct-table td {
  padding: 12px 16px;
  border-bottom: 1px solid #f2f3f5;
  vertical-align: middle;
}

.ct-table tbody tr:last-child td {
  border-bottom: none;
}

.ct-table .ct-input {
  width: 220px;
}

.ct-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 12px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 13px;
  color: #165dff;
  cursor: pointer;
}

.ct-remove {
  margin-top: 0;
}

.ct-link-btn:hover {
  text-decoration: underline;
}

.ct-link-ic {
  width: 13px;
  height: 13px;
}

.ct-foot {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid #f2f3f5;
}

.ct-primary,
.ct-ghost {
  height: 32px;
  padding: 0 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}

.ct-primary {
  border: 0;
  background: #165dff;
  color: #fff;
}

.ct-primary:hover:not(:disabled) {
  background: #0e42d2;
}

.ct-primary:disabled,
.ct-ghost:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ct-ghost {
  border: 1px solid #e5e6eb;
  background: #fff;
  color: #1d2129;
}

.ct-ghost:hover:not(:disabled) {
  border-color: #165dff;
  color: #165dff;
}
</style>
