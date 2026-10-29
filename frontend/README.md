# OpenAILake 前端

多模态数据库管理平台前端，基于 Vue 3 + TypeScript + Vite + Pinia + Vue Router 4 实现，
界面与交互复刻自 `tmp/demo.html`（原型 demo）。

## 运行

```bash
npm install
npm run dev      # 开发服务器，默认 http://localhost:5173
npm run build    # 类型检查 + 生产构建
npm run preview  # 预览构建产物
npm run type-check
```

## 页面 / 路由

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/datasets` | 数据集 | 数据集列表：搜索、排序、格式筛选、分页、创建数据集、数据集详情 |
| `/metadata` | 数据目录 | Catalog 树（目录 / 数据库 / 表）+ 表详情（基本信息 / 数据概况 / 索引 / 存储优化），支持创建数据库、创建数据表、新建索引 |
| `/apikeys` | API Key 管理 | API Key 创建（创建后一次性展示明文）、掩码展示、覆盖数据库统计 |
| `/grants` | 授权管理 | 为 API Key 授予数据目录 / 数据库级只读、读写权限，支持继承与覆盖合并 |

## 目录结构

```
src/
├── assets/styles/
│   ├── main.css          # 基础重置
│   └── platform.css      # 平台样式（自 demo.html 提取，保持 1:1 视觉还原）
├── components/
│   ├── AppSidebar.vue    # 侧边栏导航
│   ├── AppToast.vue      # 全局提示
│   ├── BaseModal.vue     # 通用弹层（支持 Esc 关闭）
│   ├── CopyButton.vue    # 复制按钮
│   ├── SvgIcon.vue       # 内联 SVG 图标组件
│   ├── icons.ts          # 图标定义
│   ├── datasets/         # 创建数据集页
│   ├── lake/             # Catalog 树、表详情、创建库 / 表、索引弹层
│   └── perm/             # API Key / 授权相关弹层
├── layouts/BasicLayout.vue
├── router/index.ts
├── stores/
│   ├── datasets.ts       # 数据集状态
│   ├── lake.ts           # Catalog / 数据库 / 表 / 索引状态
│   ├── permissions.ts    # API Key 与授权状态
│   ├── ui.ts             # Toast
│   └── app.ts
├── utils/format.ts       # 时间、数值、脱敏、剪贴板等工具
└── views/                # 四个业务页面
```

## 说明

- 当前为前端演示实现，数据均为本地 mock，尚未接入后端接口；`vite.config.ts` 中预留了 `/api/` 代理。
- `ant-design-vue` 已不再使用（原型界面完全由自定义 CSS 实现），依赖暂保留在 `package.json` 中。
