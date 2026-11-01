import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/datasets',
      },
      {
        path: 'datasets',
        name: 'datasets',
        component: () => import('@/views/DatasetsView.vue'),
        meta: { title: '数据集' },
      },
      {
        path: 'metadata',
        name: 'metadata',
        component: () => import('@/views/MetadataView.vue'),
        meta: { title: '数据目录' },
      },
      {
        path: 'explore',
        name: 'explore',
        component: () => import('@/views/ExploreView.vue'),
        meta: { title: '数据探查' },
      },
      {
        path: 'apikeys',
        name: 'apikeys',
        component: () => import('@/views/ApiKeysView.vue'),
        meta: { title: 'API Key 管理' },
      },
      {
        path: 'grants',
        name: 'grants',
        component: () => import('@/views/GrantsView.vue'),
        meta: { title: '授权管理' },
      },
    ],
  },
  {
    path: '/las',
    component: () => import('@/layouts/LasLayout.vue'),
    children: [
      { path: '', redirect: '/las/dataset/common' },
      {
        path: 'dataset/common',
        name: 'las-common-datasets',
        component: () => import('@/views/las/CommonDatasetsView.vue'),
        meta: { title: '通用数据集 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/common/create',
        name: 'las-common-create',
        component: () => import('@/views/las/DatasetCreateView.vue'),
        meta: { title: '创建通用数据集 - AI 数据湖服务 LAS' },
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
        path: 'dataset/infer',
        name: 'las-infer-datasets',
        component: () => import('@/views/las/InferDatasetsView.vue'),
        meta: { title: '推理结果集 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/infer/create',
        name: 'las-infer-create',
        component: () => import('@/views/las/DatasetCreateView.vue'),
        meta: { title: '创建推理结果集 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/infer/detail/:id',
        name: 'las-infer-detail',
        component: () => import('@/views/las/DatasetDetailView.vue'),
        meta: { title: '数据集详情 - AI 数据湖服务 LAS' },
      },
      {
        path: 'dataset/infer/sql/:id',
        name: 'las-infer-sql',
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

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ?? '多模态数据库管理平台'
})

export default router
