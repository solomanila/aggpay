# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
npm run dev       # Start Vite dev server at http://localhost:5173
npm run build     # Production build → dist/
npm run preview   # Preview production build locally
```

There are no test or lint scripts configured.

## Architecture

### Navigation (no vue-router)
Custom two-level routing lives entirely in `src/App.vue`. The `activeParent` and `activeChild` refs track the current view, and components are conditionally rendered via `v-if`. The menu hierarchy is defined in `src/data/mock.js` under `menuItems`.

### State (no Pinia/Vuex)
`App.vue` is the single source of truth for cross-cutting state: authentication (`isAuthenticated`, `authProfile`), navigation, and dashboard polling data. Everything flows down via props. Individual view components manage their own local filter/pagination/loading state with `ref()`.

### API Layer (`src/services/http.js`)
Axios instance that reads `VITE_API_BASE_URL` from env (defaults to `/api` on current origin). A request interceptor injects `Bearer <token>` from `localStorage` key `payadmin_admin_token`, skipping OTP/login endpoints. Dev target: `http://localhost:8080/api` (see `.env.development`).

### Component Pattern
All 56 components in `src/components/` are Vue 3 SFCs using `<script setup>`. Page-level components follow the `*View.vue` naming convention. Styles are scoped per component; global styles live in `src/styles/base.css`.

### Mock Data
`src/data/mock.js` (105 KB) exports all demo data used during development. Real API calls replace these in production — components call HTTP endpoints like `/admin/pay/home/metrics`.

### Environment
- `.env.development` — `VITE_API_BASE_URL=http://localhost:8080/api`
- `.env.production` — update `VITE_API_BASE_URL` before deploying

### Key Conventions
- UI text is Chinese-first
- Utility functions (`formatCurrency`, `formatCount`, `formatRate`, etc.) are pure functions co-located in components or `App.vue`
- Dashboard data auto-refreshes every 5 minutes when authenticated
- Authentication uses OTP + QR code; token is stored in `localStorage`

## 后端编码规范（强制）

### Servlet API
- 项目使用 **Spring Boot 2.x**，Servlet 包名为 `javax.servlet.*`
- **禁止**使用 `jakarta.servlet.*`（Jakarta EE 9+ / Spring Boot 3.x 专属）
- 凡需要 `HttpServletRequest` 的地方，import `javax.servlet.http.HttpServletRequest`

### 获取当前登录用户身份
- 网关（gateway）验证 JWT 后会向下游注入请求头 `X-User-ID`（用户ID）和 `X-User-Name`（账号）
- **在 admin-service / pay-service 的 Controller 中，统一使用 `AuthContextHolder` 获取当前用户**：
  ```java
  Long userId = AuthContextHolder.getUserId();       // 可能为 null
  Long userId = AuthContextHolder.getRequiredUserId(); // null 时抛 IllegalStateException
  ```
- **禁止**在下游 Controller 中重新解析 JWT（不要调用 `JwtUtils.getUserIdFromToken`）
- `AuthContextHolder` 位于 `common-core/src/main/java/com/letsvpn/common/core/util/AuthContextHolder.java`

## 微服务数据库隔离规则（强制）

- **admin-service** 只能直接访问 `admin` schema（admin.* 表）
- **`aggpay.*` 表属于 pay-service**，admin-service **必须**通过 Feign 调用 pay-service 接口获取这些数据
- **禁止**在 admin-service 中使用 `@TableName("aggpay.*")` 的任何实体或 Mapper
- 如需新增跨服务数据访问，在 pay-service 新增接口，在 admin-service 通过 `PayServiceClient` → `PayServiceFacade` 调用
- 完整规则见 `backend/docs/cross-schema-prohibition.md`
