# Payadmin UI

基于 Vue 3 + Vite 构建的演示控制台，首页 UI 参考了仓库根目录下的 `index.html`。目前页面展示 mock 数据，后续可通过替换 `src/data/mock.js` 或接入真实 API。

## 使用说明

```bash
cd payadmin-ui
npm install
npm run dev
```

- `npm run dev`：本地启动 Vite 开发服务（默认端口 5173）。
- `npm run build`：打包生成 `dist` 目录，可用于部署。
- `npm run preview`：在本地预览打包结果。

## 目录结构

```
payadmin-ui/
├── src/
│   ├── components/      # 头部、侧边栏、统计、通道卡片组件
│   ├── data/mock.js     # 首页所需的 mock 数据
│   ├── styles/base.css  # 全局样式
│   ├── App.vue
│   └── main.js
├── index.html           # Vite 入口文件
├── package.json
└── vite.config.js
```

后续需要对接 Java API 时，可在组件内通过 `fetch`/`axios` 请求接口，再将返回结果赋值给对应的响应式状态即可。
