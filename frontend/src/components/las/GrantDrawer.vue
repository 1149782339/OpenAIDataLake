<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import SvgIcon from '@/components/SvgIcon.vue'
import { LAS_ICONS } from '@/components/las/lasIcons'
import LasSelect from '@/components/las/LasSelect.vue'

export interface GrantPayload {
  scope: string
  resource: string
  targetType: string
  targetName: string
  permission: string
}

const props = defineProps<{ visible: boolean }>()

const emit = defineEmits<{
  close: []
  submit: [payload: GrantPayload]
}>()

const SCOPES = ['数据目录', '数据库', '数据表', '函数']
const TARGET_TYPES = ['用户', '用户组', '角色']
const PERMISSIONS = ['读', '写', '读写', '全部权限']

/** 授权范围 → 资源下拉可选值 */
const RESOURCE_OPTIONS: Record<string, string[]> = {
  数据目录: ['catalog1', 'catalog2'],
  数据库: ['default', 'db2'],
  数据表: ['test', 'test3'],
  函数: [],
}

const scope = ref('数据目录')
const targetType = ref('用户')
const resource = ref('')
const targetName = ref('')
const permission = ref('')
const targetError = ref('')

const resourceOptions = computed(() => RESOURCE_OPTIONS[scope.value] ?? [])

watch(
  () => props.visible,
  (v) => {
    if (!v) return
    scope.value = '数据目录'
    targetType.value = '用户'
    resource.value = ''
    targetName.value = ''
    permission.value = ''
    targetError.value = ''
  },
)

watch(scope, () => {
  resource.value = ''
})

function onSubmit() {
  if (!targetName.value.trim()) {
    targetError.value = '请输入对象名称'
    return
  }
  emit('submit', {
    scope: scope.value,
    resource: resource.value,
    targetType: targetType.value,
    targetName: targetName.value.trim(),
    permission: permission.value,
  })
}
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="gd-mask" @click="emit('close')" />
    <aside v-if="visible" class="gd-drawer">
      <header class="gd-header">
        <h3 class="gd-title">新增授权</h3>
        <button class="gd-close" type="button" title="关闭" @click="emit('close')">
          <SvgIcon :icon="LAS_ICONS.close" />
        </button>
      </header>

      <div class="gd-body">
        <!-- 授权资源 -->
        <section class="gd-section">
          <div class="gd-section-title">授权资源</div>

          <div class="gd-row">
            <div class="gd-label">授权范围</div>
            <div class="gd-control">
              <div class="gd-radio-group">
                <button
                  v-for="item in SCOPES"
                  :key="item"
                  class="gd-radio"
                  :class="{ 'is-checked': scope === item }"
                  type="button"
                  @click="scope = item"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>

          <div class="gd-row">
            <div class="gd-label"><i class="gd-asterisk">*</i>{{ scope }}</div>
            <div class="gd-control">
              <LasSelect v-model="resource" :options="resourceOptions" placeholder="请选择" />
            </div>
          </div>
        </section>

        <!-- 被授权对象 -->
        <section class="gd-section">
          <div class="gd-section-title">被授权对象</div>

          <div class="gd-row">
            <div class="gd-label"><i class="gd-asterisk">*</i>对象类型</div>
            <div class="gd-control">
              <div class="gd-radio-group">
                <button
                  v-for="item in TARGET_TYPES"
                  :key="item"
                  class="gd-radio"
                  :class="{ 'is-checked': targetType === item }"
                  type="button"
                  @click="targetType = item"
                >
                  {{ item }}
                </button>
              </div>
            </div>
          </div>

          <div class="gd-row">
            <div class="gd-label"><i class="gd-asterisk">*</i>对象名称</div>
            <div class="gd-control">
              <div class="gd-input" :class="{ 'is-error': targetError }">
                <input
                  v-model="targetName"
                  type="text"
                  placeholder="请输入名称"
                  @input="targetError = ''"
                >
                <SvgIcon class="gd-input-ic" :icon="LAS_ICONS.search" />
              </div>
              <div v-if="targetError" class="gd-error">{{ targetError }}</div>
            </div>
          </div>
        </section>

        <!-- 授予权限 -->
        <section class="gd-section">
          <div class="gd-section-title">授予权限</div>

          <div class="gd-row">
            <div class="gd-label"><i class="gd-asterisk">*</i>授予权限</div>
            <div class="gd-control">
              <LasSelect v-model="permission" :options="PERMISSIONS" placeholder="请选择" />
              <button class="gd-link-btn" type="button">查看权限说明</button>
            </div>
          </div>
        </section>
      </div>

      <footer class="gd-footer">
        <button class="gd-btn gd-btn-ghost" type="button" @click="emit('close')">取消</button>
        <button class="gd-btn gd-btn-primary" type="button" @click="onSubmit">授权</button>
      </footer>
    </aside>
  </Teleport>
</template>

<style scoped>
.gd-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
}

.gd-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  width: 578px;
  display: flex;
  flex-direction: column;
  background: #fff;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.15);
  animation: gd-slide-in 0.2s ease;
}

@keyframes gd-slide-in {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.gd-header {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px 0 24px;
}

.gd-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  line-height: 28px;
  color: #0c0d0e;
}

.gd-close {
  display: inline-flex;
  padding: 0;
  border: 0;
  background: none;
  color: #0c0d0e;
  cursor: pointer;
}

.gd-close svg {
  width: 20px;
  height: 20px;
}

.gd-close:hover {
  color: #165dff;
}

.gd-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px 24px;
}

.gd-section + .gd-section {
  margin-top: 44px;
}

.gd-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
  line-height: 22px;
  color: rgba(20, 20, 20, 0.9);
}

.gd-section-title::before {
  content: '';
  display: block;
  width: 3px;
  height: 14px;
  border-radius: 0 3px 3px 0;
  background: #5652ff;
}

.gd-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
}

.gd-label {
  flex: none;
  width: 129px;
  padding-right: 18px;
  box-sizing: border-box;
  text-align: right;
  font-size: 13px;
  line-height: 32px;
  color: #0c0d0e;
}

.gd-asterisk {
  margin-right: 4px;
  font-style: normal;
  color: #e33232;
}

.gd-control {
  flex: 1;
  min-width: 0;
}

.gd-radio-group {
  display: flex;
  gap: 8px;
}

.gd-radio {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 8px;
  border: 1px solid #c9cdd4;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  line-height: 30px;
  color: #4e5969;
  cursor: pointer;
  white-space: nowrap;
}

.gd-radio:hover {
  color: #3073f2;
  border-color: #9cc6fa;
}

.gd-radio.is-checked {
  color: #3073f2;
  border-color: #9cc6fa;
  background: #f4f7ff;
}

.gd-input {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 11px;
  border: 1px solid #dde2e9;
  border-radius: 4px;
  background: #fff;
}

.gd-input:hover {
  border-color: #9cc6fa;
}

.gd-input.is-error {
  border-color: #e33232;
}

.gd-input input {
  flex: 1;
  min-width: 0;
  height: 30px;
  padding: 0;
  border: 0;
  outline: none;
  background: none;
  font-size: 13px;
  color: #0c0d0e;
}

.gd-input input::placeholder {
  color: #86909c;
}

.gd-input-ic {
  flex: none;
  width: 14px;
  height: 14px;
  color: #86909c;
}

.gd-error {
  margin-top: 4px;
  font-size: 12px;
  color: #e33232;
}

.gd-link-btn {
  display: block;
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: none;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  color: #165dff;
  cursor: pointer;
}

.gd-link-btn:hover {
  text-decoration: underline;
}

.gd-footer {
  flex: none;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e5e6eb;
}

.gd-btn {
  width: 96px;
  height: 32px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.gd-btn-ghost {
  border: 1px solid #dde2e9;
  background: #f6f8fa;
  color: #42464e;
}

.gd-btn-ghost:hover {
  border-color: #9cc6fa;
  color: #165dff;
}

.gd-btn-primary {
  border: 1px solid #1759dd;
  background: #165dff;
  color: #fff;
}

.gd-btn-primary:hover {
  background: #0e42d2;
}
</style>
