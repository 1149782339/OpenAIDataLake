<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import { ApiError, errorMessage } from '@/api/http'
import { createDataSource, currentUser, listUsers, probeDataSource, type ConsoleUser } from '@/api/console'
import { useLasDataSourceStore, type DataSourceEngine } from '@/stores/lasDataSources'

const router = useRouter()
const store = useLasDataSourceStore()

const name = ref('')
const engine = ref<DataSourceEngine>('PostgreSQL')
const host = ref('')
const port = ref('')
const user = ref('')
const password = ref('')
const database = ref('')

const nameError = ref('')
const connError = ref('')
const testMessage = ref('')
const testOk = ref(false)
const connected = ref(false)
const testing = ref(false)
const saving = ref(false)
const users = ref<ConsoleUser[]>([])
const userError = ref('')
const grantError = ref('')
let probeSeq = 0
let grantKey = 1

interface GrantDraft {
  key: number
  userId: string
  writable: boolean
}

const grants = ref<GrantDraft[]>([])

const NAME_PATTERN = /^[A-Za-z][A-Za-z0-9_]*$/

const portPlaceholder = computed(() => {
  if (engine.value === 'MySQL') return '3306'
  if (engine.value === 'ClickHouse') return '8123'
  return '5432'
})

const databasePlaceholder = computed(() => {
  if (engine.value === 'MySQL') return 'db1'
  if (engine.value === 'ClickHouse') return 'demo'
  return 'benchmark'
})

function connectionReady() {
  const portNumber = Number(port.value)
  return Boolean(
    host.value.trim()
    && user.value.trim()
    && password.value
    && database.value.trim()
    && Number.isInteger(portNumber)
    && portNumber >= 1
    && portNumber <= 65535,
  )
}

function connectionBody() {
  return {
    engine: engine.value,
    host: host.value.trim(),
    port: Number(port.value),
    user: user.value.trim(),
    password: password.value,
    database: database.value.trim(),
  }
}

function clearTest() {
  connected.value = false
  testOk.value = false
  testMessage.value = ''
}

watch([engine, host, port, user, password, database], () => {
  probeSeq += 1
  testing.value = false
  clearTest()
  connError.value = ''
})

watch(engine, (value) => {
  if (value !== 'ClickHouse') return
  grants.value = grants.value.map((row) => ({ ...row, writable: false }))
})

function addGrant(userId = '', writable = true) {
  grants.value.push({
    key: grantKey++,
    userId,
    writable: engine.value === 'ClickHouse' ? false : writable,
  })
}

function removeGrant(index: number) {
  grants.value.splice(index, 1)
}

function back() {
  router.push('/catalog/sources')
}

async function onTest() {
  connError.value = ''
  clearTest()
  if (!connectionReady()) {
    connError.value = '先填写 Host、Port、用户、密码和数据库名称。'
    return
  }
  const seq = ++probeSeq
  testing.value = true
  try {
    await probeDataSource(connectionBody())
    if (seq !== probeSeq) return
    connected.value = true
    testOk.value = true
    testMessage.value = '已连通。'
  } catch (cause) {
    if (seq !== probeSeq) return
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/sources/create' } })
      return
    }
    connError.value = errorMessage(cause, '连接失败')
  } finally {
    if (seq === probeSeq) testing.value = false
  }
}

async function onSubmit() {
  nameError.value = ''
  connError.value = ''
  const trimmed = name.value.trim()
  if (!NAME_PATTERN.test(trimmed)) {
    nameError.value = '数据源名称只能使用英文字母、数字和下划线，并且以字母开头。'
  } else if (store.findByName(trimmed)) {
    nameError.value = '数据源名称已存在。'
  }
  if (!connected.value || !connectionReady()) connError.value = '请先测试连接。'
  grantError.value = ''
  const chosen = grants.value.filter((row) => row.userId)
  if (chosen.length !== grants.value.length) grantError.value = '请选择用户。'
  else if (new Set(chosen.map((row) => row.userId)).size !== chosen.length) grantError.value = '同一个用户不能重复授权。'
  else if (engine.value === 'ClickHouse' && chosen.some((row) => row.writable)) grantError.value = 'ClickHouse 数据源不支持写入。'
  if (nameError.value || connError.value || grantError.value || saving.value) return

  saving.value = true
  try {
    await createDataSource({
      name: trimmed,
      ...connectionBody(),
      grants: chosen.map((row) => ({ userId: Number(row.userId), writable: row.writable })),
    })
    await router.push('/catalog/sources')
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/sources/create' } })
      return
    }
    const message = errorMessage(cause, '创建失败')
    if (message.includes('名称')) nameError.value = message
    else connError.value = message
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    await store.load()
    const [all, me] = await Promise.all([listUsers(), currentUser()])
    users.value = all.filter((item) => item.enabled)
    addGrant(String(me.id), true)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      await router.push({ path: '/login', query: { redirect: '/catalog/sources/create' } })
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
      <span class="ct-title">创建数据源</span>
    </header>

    <div class="ct-body">
      <form class="ct-form" @submit.prevent="onSubmit">
        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">基本信息</span>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>数据源名称</label>
            <div class="ct-control">
              <input
                v-model="name"
                type="text"
                class="ct-input"
                :class="{ 'is-error': nameError }"
                placeholder="postgres_datasource"
                autocomplete="off"
                @input="nameError = ''"
              >
              <p class="ct-hint">只能使用英文字母、数字和下划线，并且以字母开头。查询里表名的第一段就是这个名称。</p>
              <div v-if="nameError" class="ct-error">{{ nameError }}</div>
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>数据源类型</label>
            <div class="ct-control">
              <select v-model="engine" class="ct-input ct-select">
                <option value="PostgreSQL">PostgreSQL</option>
                <option value="MySQL">MySQL</option>
                <option value="ClickHouse">ClickHouse</option>
              </select>
            </div>
          </div>
        </section>

        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">连接</span>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>Host</label>
            <div class="ct-control">
              <input v-model="host" type="text" class="ct-input" placeholder="localhost" autocomplete="off">
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>Port</label>
            <div class="ct-control">
              <input
                v-model="port"
                type="number"
                class="ct-input"
                min="1"
                max="65535"
                :placeholder="portPlaceholder"
              >
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>用户</label>
            <div class="ct-control">
              <input v-model="user" type="text" class="ct-input" autocomplete="off">
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>密码</label>
            <div class="ct-control">
              <input v-model="password" type="password" class="ct-input" autocomplete="off">
            </div>
          </div>
          <div class="ct-field">
            <label class="ct-label"><i class="ct-req">*</i>数据库名称</label>
            <div class="ct-control">
              <input
                v-model="database"
                type="text"
                class="ct-input"
                autocomplete="off"
                :placeholder="databasePlaceholder"
              >
              <p class="ct-hint">
                PostgreSQL 的数据库名称只用于连接，查询路径的中间段是 schema，例如 public。MySQL 和 ClickHouse 的数据库名称会出现在查询路径里，例如 db1、demo。ClickHouse 的端口填 HTTP 端口 8123。
              </p>
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

        <section class="ct-section">
          <div class="ct-section-head">
            <span class="ct-bar" />
            <span class="ct-section-title">权限</span>
          </div>
          <p class="ct-hint ct-grant-hint">只读可以查询，读写可以增删改。没有出现在这里的用户不能访问这个数据源。ClickHouse 只能只读。</p>
          <p v-if="userError" class="ct-error ct-block-error">{{ userError }}</p>
          <div class="ct-table-wrap">
            <table class="ct-table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>权限</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                <tr v-if="!grants.length">
                  <td class="ct-empty" colspan="3">还没有授权</td>
                </tr>
                <tr v-for="(row, index) in grants" :key="row.key">
                  <td>
                    <select v-model="row.userId" class="ct-input ct-select" aria-label="授权用户">
                      <option value="">选择用户</option>
                      <option v-for="item in users" :key="item.id" :value="String(item.id)">{{ item.username }}</option>
                    </select>
                  </td>
                  <td>
                    <select v-model="row.writable" class="ct-input ct-select" aria-label="权限" :disabled="engine === 'ClickHouse'">
                      <option :value="false">只读</option>
                      <option :value="true">读写</option>
                    </select>
                  </td>
                  <td>
                    <button class="ct-link-btn ct-remove" type="button" @click="removeGrant(index)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <button class="ct-link-btn" type="button" @click="addGrant()">
            <SvgIcon class="ct-link-ic" :icon="LAS_ICONS.plus" />
            <span>添加用户</span>
          </button>
          <div v-if="grantError" class="ct-error">{{ grantError }}</div>
        </section>

        <footer class="ct-foot">
          <button class="ct-ghost" type="button" @click="back">取消</button>
          <button class="ct-primary" type="submit" :disabled="saving">确定</button>
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

.ct-back:hover { color: #165dff; }
.ct-back svg { width: 16px; height: 16px; }
.ct-title { font-size: 14px; font-weight: 500; color: #1d2129; }
.ct-body { flex: 1; min-height: 0; overflow: auto; }
.ct-form { max-width: 960px; margin: 0 auto; padding: 24px 20px 80px; }
.ct-section { margin-bottom: 32px; }
.ct-section-head { display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
.ct-bar { width: 3px; height: 14px; border-radius: 2px; background: #165dff; }
.ct-section-title { font-size: 14px; font-weight: 500; color: #1d2129; }
.ct-field { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 20px; }
.ct-label { flex: none; width: 145px; padding-top: 7px; font-size: 13px; color: #4e5969; }
.ct-req { margin-right: 4px; font-style: normal; color: #f53f3f; }
.ct-control { flex: 1; min-width: 0; max-width: 500px; }
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
.ct-input:focus { border-color: #165dff; }
.ct-input.is-error { border-color: #f53f3f; }
.ct-input::placeholder { color: #86909c; }
.ct-select {
  appearance: none;
  cursor: pointer;
  padding-right: 26px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2386909c' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 8px center;
  background-size: 12px 12px;
}
.ct-hint { margin: 8px 0 0; font-size: 12px; color: #86909c; }
.ct-error { margin-top: 4px; font-size: 12px; color: #f53f3f; }
.ct-test { display: flex; align-items: center; gap: 12px; }
.ct-test-msg { margin: 0; font-size: 12px; color: #f53f3f; }
.ct-test-msg.ok { color: #00b42a; }
.ct-foot { display: flex; gap: 12px; padding: 12px 0; border-top: 1px solid #f2f3f5; }
.ct-primary,
.ct-ghost {
  height: 32px;
  padding: 0 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
}
.ct-primary { border: 0; background: #165dff; color: #fff; }
.ct-primary:hover { background: #0e42d2; }
.ct-ghost { border: 1px solid #e5e6eb; background: #fff; color: #1d2129; }
.ct-ghost:hover:not(:disabled) { border-color: #165dff; color: #165dff; }
.ct-ghost:disabled { opacity: 0.7; cursor: not-allowed; }
.ct-grant-hint { margin: 0 0 12px; }
.ct-block-error { margin: 0 0 12px; }
.ct-table-wrap { overflow-x: auto; border: 1px solid #e5e6eb; border-radius: 4px; }
.ct-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.ct-table th { height: 40px; padding: 0 16px; background: #f7f8fa; text-align: left; color: #1d2129; font-weight: 500; border-bottom: 1px solid #e5e6eb; }
.ct-table td { padding: 12px 16px; border-bottom: 1px solid #f2f3f5; vertical-align: middle; }
.ct-table tbody tr:last-child td { border-bottom: none; }
.ct-table .ct-input { width: 220px; }
.ct-empty { color: #86909c; }
.ct-link-btn { display: inline-flex; align-items: center; gap: 4px; margin-top: 12px; padding: 0; border: 0; background: none; font-size: 13px; color: #165dff; cursor: pointer; }
.ct-remove { margin-top: 0; }
.ct-link-btn:hover { text-decoration: underline; }
.ct-link-ic { width: 13px; height: 13px; }
</style>
