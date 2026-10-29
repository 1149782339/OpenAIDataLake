<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { useLakeStore } from '@/stores/lake'
import { useUiStore } from '@/stores/ui'

const props = withDefaults(
  defineProps<{
    open: boolean
    /** 编辑模式传入数据库 id */
    dbId?: string | null
  }>(),
  { dbId: null },
)

const emit = defineEmits<{ close: []; saved: [] }>()

const lake = useLakeStore()
const ui = useUiStore()

const name = ref('')
const ossPath = ref('oss://')
const note = ref('')
const nameInvalid = ref(false)
const pathInvalid = ref(false)
const nameError = ref('请输入数据库名称')
const pathError = ref('请输入以 oss:// 开头且包含桶和路径的存储位置')
const nameInput = ref<HTMLInputElement | null>(null)

const editing = computed(() => !!props.dbId)
const title = computed(() => (editing.value ? '编辑数据库' : '创建数据库'))

watch(
  () => props.open,
  (open) => {
    if (!open) return
    const db = props.dbId ? lake.dbById(props.dbId) : null
    name.value = db?.name ?? ''
    ossPath.value = db?.ossPath ?? 'oss://'
    note.value = db?.note ?? ''
    nameInvalid.value = false
    pathInvalid.value = false
    nextTick(() => nameInput.value?.focus())
  },
)

function suggestDbPath() {
  const value = name.value.trim() || 'new_database'
  ossPath.value = `oss://db1/${value}`
  pathInvalid.value = false
}

function pickPath() {
  suggestDbPath()
  ui.toast('已选择 OSS 存储位置（演示）')
}

function submit() {
  const dbName = name.value.trim()
  const path = ossPath.value.trim()
  let valid = true

  nameInvalid.value = false
  pathInvalid.value = false

  if (!dbName) {
    nameInvalid.value = true
    nameError.value = '请输入数据库名称'
    valid = false
  } else if (!/^[a-z][a-z0-9_]*$/.test(dbName)) {
    nameInvalid.value = true
    nameError.value = '仅支持小写字母、数字和下划线，且需以字母开头'
    valid = false
  } else if (lake.dbs.some((db) => db.name === dbName && db.id !== props.dbId)) {
    nameInvalid.value = true
    nameError.value = '数据库名称已存在'
    valid = false
  }

  if (!/^oss:\/\/[^/]+\/.+$/.test(path)) {
    pathInvalid.value = true
    valid = false
  }

  if (!valid) return

  if (props.dbId) {
    lake.updateDatabase(props.dbId, { name: dbName, ossPath: path, note: note.value.trim() })
    ui.toast(`数据库 "${dbName}" 已更新（演示）`)
  } else {
    lake.createDatabase({ name: dbName, ossPath: path, note: note.value.trim() })
    ui.toast(`数据库 "${dbName}" 创建成功（演示）`)
  }
  emit('close')
  emit('saved')
}
</script>

<template>
  <BaseModal id="createDbModal" :open="open" :title="title" variant="db-modal" aria-label="创建数据库" @close="emit('close')">
    <div class="form-row">
      <label for="dbName">数据库名称<span class="req">*</span></label>
      <input
        id="dbName"
        ref="nameInput"
        v-model="name"
        type="text"
        placeholder="小写字母、下划线、数字组成"
        autocomplete="off"
        :class="{ invalid: nameInvalid }"
        @input="nameInvalid = false"
        @keydown.enter="submit"
      >
      <p class="field-error" :class="{ show: nameInvalid }">{{ nameError }}</p>
    </div>

    <div class="form-row">
      <label for="dbOssPath">存储位置<span class="req">*</span></label>
      <div class="path-row">
        <input
          id="dbOssPath"
          v-model="ossPath"
          type="text"
          placeholder="oss://"
          autocomplete="off"
          :class="{ invalid: pathInvalid }"
          @input="pathInvalid = false"
          @keydown.enter="submit"
        >
        <button id="dbPathPick" class="path-pick" type="button" title="选择 OSS 路径" aria-label="选择 OSS 路径" @click="pickPath">
          <SvgIcon name="plus" />
        </button>
      </div>
      <p class="field-error" :class="{ show: pathInvalid }">{{ pathError }}</p>
      <p class="form-hint">
        系统建议使用的格式 oss://[OSS 目录存储位置]/[数据库名称]
        <button type="button" class="link-btn" @click="suggestDbPath">使用默认路径</button>
      </p>
    </div>

    <div class="form-row">
      <label for="dbNote">备注信息</label>
      <textarea id="dbNote" v-model="note" placeholder="请输入" />
    </div>

    <template #footer>
      <div class="modal-foot">
        <button class="btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="btn-primary" type="button" @click="submit">确认</button>
      </div>
    </template>
  </BaseModal>
</template>
