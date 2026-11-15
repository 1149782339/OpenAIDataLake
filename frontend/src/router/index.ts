import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { api } from '@/api/http'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    auth?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layouts/LasLayout.vue'),
    children: [
      {
        path: '',
        name: 'las-common-datasets',
        component: () => import('@/views/las/CommonDatasetsView.vue'),
        meta: { title: '数据集 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/common/create',
        name: 'las-common-create',
        component: () => import('@/views/las/DatasetCreateView.vue'),
        meta: { title: '创建数据集 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/common/detail/:id',
        name: 'las-common-detail',
        component: () => import('@/views/las/DatasetDetailView.vue'),
        meta: { title: '数据集详情 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/common/sql/:id',
        name: 'las-common-sql',
        component: () => import('@/views/las/DatasetSqlView.vue'),
        meta: { title: 'SQL 查询 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/metadata',
        name: 'las-catalog-metadata',
        component: () => import('@/views/las/LakeMetadataView.vue'),
        meta: { title: '元数据 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/metadata/database/:catalog/:schema',
        name: 'las-catalog-database',
        component: () => import('@/views/las/CatalogDatabaseView.vue'),
        meta: { title: '数据库 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/metadata/database/:catalog/:schema/table/create',
        name: 'las-catalog-table-create',
        component: () => import('@/views/las/CatalogTableCreateView.vue'),
        meta: { title: '创建数据表 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/metadata/database/table/detail/:catalog/:schema/:table',
        name: 'las-catalog-table-detail',
        component: () => import('@/views/las/CatalogTableDetailView.vue'),
        meta: { title: '数据表详情 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/permission',
        name: 'las-catalog-permission',
        component: () => import('@/views/las/LakePermissionView.vue'),
        meta: { title: '权限管理 - AI 数据湖服务 LAS' },
      },
      {
        path: 'catalog/explore',
        name: 'las-catalog-explore',
        component: () => import('@/views/las/LasExploreView.vue'),
        meta: { title: '数据探查 - AI 数据湖服务 LAS', auth: true },
      },
      {
        path: 'catalog/directories',
        name: 'las-directories',
        component: () => import('@/views/las/FileDirectoryView.vue'),
        meta: { title: '文件目录 - AI 数据湖服务 LAS', auth: true },
      },
      {
        path: 'catalog/directories/create',
        name: 'las-directories-create',
        component: () => import('@/views/las/FileDirectoryCreateView.vue'),
        meta: { title: '创建文件目录 - AI 数据湖服务 LAS', auth: true },
      },
      {
        path: 'catalog/sources',
        name: 'las-sources',
        component: () => import('@/views/las/DataSourceView.vue'),
        meta: { title: '数据源 - AI 数据湖服务 LAS', auth: true },
      },
      {
        path: 'catalog/sources/create',
        name: 'las-sources-create',
        component: () => import('@/views/las/DataSourceCreateView.vue'),
        meta: { title: '创建数据源 - AI 数据湖服务 LAS', auth: true },
      },
      {
        path: 'catalog/sources/:id/access',
        name: 'las-sources-access',
        component: () => import('@/views/las/DataSourceAccessView.vue'),
        meta: { title: '数据源权限 - AI 数据湖服务 LAS', auth: true },
      },
    ],
  },
  {
    path: '/dev',
    component: () => import('@/layouts/LasLayout.vue'),
    children: [
      { path: '', redirect: '/dev/operators' },
      {
        path: 'operators',
        name: 'dev-operators',
        component: () => import('@/views/dev/OperatorManageView.vue'),
        meta: { title: '算子管理 - AI 数据湖服务 LAS' },
      },
      {
        path: 'workflow',
        name: 'dev-workflow',
        component: () => import('@/views/dev/WorkflowManageView.vue'),
        meta: { title: '工作流管理 - AI 数据湖服务 LAS' },
      },
      {
        path: 'workflow/create',
        name: 'dev-workflow-create',
        component: () => import('@/views/dev/WorkflowEditorView.vue'),
        meta: { title: '编辑工作流 - AI 数据湖服务 LAS' },
      },
      {
        path: 'workflow/:id',
        name: 'dev-workflow-detail',
        component: () => import('@/views/dev/WorkflowDetailView.vue'),
        meta: { title: '工作流详情 - AI 数据湖服务 LAS' },
      },
      {
        path: 'assets',
        name: 'dev-assets',
        component: () => import('@/views/dev/AssetManageView.vue'),
        meta: { title: '资产管理 - AI 数据湖服务 LAS' },
      },
      {
        path: 'labeling',
        name: 'dev-labeling',
        component: () => import('@/views/dev/LabelingTaskListView.vue'),
        meta: { title: '数据标注 - AI 数据湖服务 LAS' },
      },
      {
        path: 'labeling/:taskId',
        name: 'dev-labeling-detail',
        component: () => import('@/views/dev/LabelingWorkbenchView.vue'),
        meta: { title: '标注任务 - AI 数据湖服务 LAS' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '404' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  if (to.path === '/login') return true
  try {
    await api('/api/auth/me')
    return true
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ?? '多模态数据库管理平台'
})

export default router
