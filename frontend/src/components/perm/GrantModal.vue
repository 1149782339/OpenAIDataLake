<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { useLakeStore } from '@/stores/lake'
import { usePermissionStore, type Permission } from '@/stores/permissions'
import { useUiStore } from '@/stores/ui'

const props = defineProps<{ open: boolean; keyId?: string | null }>()
const emit = defineEmits<{ close: []; granted: [] }>()

const lake = useLakeStore()
const perm = usePermissionStore()
const ui = useUiStore()

const keyId = ref('')
const catalogId = ref('')
const scope = ref('')
const permission = ref<Permission>('read')
const errorText = ref('')
const showError = ref(false)
const hint = ref('')

const scopeOptions = computed(() => {
  const options = [{ value: `cat:${catalogId.value}`, label: '全部数据库（数据目录级）' }]
  lake.dbsInCatalog(catalogId.value).forEach((db) => {
    options.push({ value: `db:${db.id}`, label: db.name })
  })
  return options
})

function parseScope(value: string) {
  const parts = String(value || '').split(':')
  return { scope: parts[0] === 'cat' ? ('cat' as const) : ('db' as const), scopeId: parts[1] || '' }
}

function updateHint() {
  const key = perm.apiKeyById(keyId.value)
  const parsed = parseScope(scope.value)
  if (!key || !parsed.scopeId) {
    hint.value = ''
    return
  }

  const scopeText =
    parsed.scope === 'cat'
      ? `该授权对数据目录 ${lake.catalogById(parsed.scopeId).name} 下的全部数据库生效。`
      : `该授权仅对数据库 ${lake.dbById(parsed.scopeId).name} 生效。`

  const direct = key.grants.find(
    (g) => g.scope === parsed.scope && g.scopeId === parsed.scopeId,
  )
  const targetDb =
    parsed.scope === 'cat'
      ? lake.dbsInCatalog(parsed.scopeId)[0] ?? lake.dbs[0]
      : lake.dbById(parsed.scopeId)
  const inherited = targetDb
    ? perm.effectiveGrant(key, targetDb.id, direct ? direct.id : null)
    : null

  if (direct) {
    hint.value = `${scopeText} 已直接授予${perm.permissionLabel(direct.permission)}权限。`
  } else if (inherited) {
    hint.value = `${scopeText} 当前从「${perm.scopeLabel(inherited)}」继承${perm.permissionLabel(
      inherited.permission,
    )}权限。`
  } else {
    hint.value = `${scopeText} 当前没有已授予的权限。`
  }
}

function fillScopes(nextCatalogId: string, preferred?: string) {
  catalogId.value = nextCatalogId
  const options = [{ value: `cat:${nextCatalogId}` }, ...lake.dbsInCatalog(nextCatalogId).map((db) => ({ value: `db:${db.id}` }))]
  scope.value = preferred && options.some((o) => o.value === preferred) ? preferred : options[0]?.value ?? ''
  updateHint()
}

watch(
  () => props.open,
  (open) => {
    if (!open) return
    const key = perm.apiKeyById(props.keyId ?? perm.selectedKeyId) ?? perm.keys[0]
    if (!key) {
      ui.toast('请先创建 API Key')
      return
    }
    keyId.value = key.id
    const first = key.grants[0]
    const firstCatalog = first
      ? first.scope === 'cat'
        ? first.scopeId
        : lake.dbCatalogId(lake.dbById(first.scopeId))
      : (lake.catalogs[0]?.id ?? '')

    let available: string | null = null
    lake.dbsInCatalog(firstCatalog).forEach((db) => {
      if (!available && !perm.effectiveGrant(key, db.id)) available = `db:${db.id}`
    })
    if (!available && !perm.effectiveGrant(key, firstCatalog)) available = `cat:${firstCatalog}`

    permission.value = 'read'
    showError.value = false
    errorText.value = ''
    fillScopes(firstCatalog, available ?? undefined)
  },
)

watch(scope, updateHint)
watch(keyId, updateHint)

function onCatalogChange() {
  fillScopes(catalogId.value)
  showError.value = false
}

function setPermission(value: Permission) {
  permission.value = value
  showError.value = false
  updateHint()
}

function submit() {
  const key = perm.apiKeyById(keyId.value)
  if (!key) {
    ui.toast('请先创建 API Key')
    return
  }
  const parsed = parseScope(scope.value)
  if (!parsed.scopeId) {
    errorText.value = '请选择授权对象'
    showError.value = true
    return
  }
  const duplicate = key.grants.some(
    (g) => g.scope === parsed.scope && g.scopeId === parsed.scopeId,
  )
  if (duplicate) {
    errorText.value = '该 API Key 已直接拥有此授权，请在授权列表中直接修改。'
    showError.value = true
    return
  }

  const inherited = parsed.scope === 'db' ? perm.effectiveGrant(key, parsed.scopeId) : null
  if (inherited && perm.permissionRank(permission.value) <= perm.permissionRank(inherited.permission)) {
    errorText.value =
      perm.permissionRank(inherited.permission) === 2
        ? `该数据库已从「${perm.scopeLabel(inherited)}」继承读写权限，无法再授予更低权限。`
        : `该数据库已从「${perm.scopeLabel(inherited)}」继承只读权限，如需授权请选择「读写」。`
    showError.value = true
    return
  }

  const merged = perm.addGrant(key, parsed.scope, parsed.scopeId, permission.value)
  perm.selectedKeyId = key.id
  showError.value = false
  emit('close')
  emit('granted')
  ui.toast(
    `已授予 ${key.name} 对 ${perm.scopeLabel(parsed)} 的${perm.permissionLabel(permission.value)}权限` +
      (merged > 0 ? `，并合并 ${merged} 项被覆盖的授权` : ''),
  )
}
</script>

<template>
  <BaseModal id="grantModal" :open="open" title="授予权限" variant="db-modal" aria-label="授予权限" @close="emit('close')">
    <div class="form-row">
      <label for="grantKey">API Key<span class="req">*</span></label>
      <select id="grantKey" v-model="keyId" @change="showError = false">
        <option v-for="key in perm.keys" :key="key.id" :value="key.id">{{ key.name }}</option>
      </select>
    </div>

    <div class="form-row">
      <label for="grantCatalog">所属数据目录<span class="req">*</span></label>
      <select id="grantCatalog" v-model="catalogId" @change="onCatalogChange">
        <option v-for="cat in lake.catalogs" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
      </select>
    </div>

    <div class="form-row">
      <label for="grantScope">授权对象<span class="req">*</span></label>
      <select id="grantScope" v-model="scope" @change="showError = false">
        <option v-for="option in scopeOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
      <p class="field-error" :class="{ show: showError }">{{ errorText }}</p>
      <p class="form-hint" id="grantScopeHint">{{ hint }}</p>
    </div>

    <div class="form-row">
      <label>权限<span class="req">*</span></label>
      <div class="seg-row perm-seg">
        <button
          class="seg-btn"
          :class="{ active: permission === 'read' }"
          type="button"
          data-perm="read"
          @click="setPermission('read')"
        >
          只读
        </button>
        <button
          class="seg-btn"
          :class="{ active: permission === 'write' }"
          type="button"
          data-perm="write"
          @click="setPermission('write')"
        >
          读写
        </button>
      </div>
    </div>

    <template #footer>
      <div class="modal-foot">
        <button class="btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="btn-primary" type="button" @click="submit">确认授予</button>
      </div>
    </template>
  </BaseModal>
</template>
