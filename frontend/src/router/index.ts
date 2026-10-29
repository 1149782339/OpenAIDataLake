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
