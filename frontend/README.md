# 🍍 Pineapple Admin Dashboard

Bảng điều khiển quản trị cho hệ thống Pineapple E-Commerce — nền tảng thương mại điện tử nông sản hữu cơ.

---

## 🛠 Tech Stack

| Hạng mục | Công nghệ | Ghi chú |
|---|---|---|
| Framework | **Next.js 15** (App Router) | Turbopack cho dev |
| Ngôn ngữ | **TypeScript** | Strict mode |
| Styling | **TailwindCSS v4** | Custom design tokens, dark mode |
| Components | **shadcn/ui** (Radix UI) | Có thể thêm theo nhu cầu |
| Server state | **TanStack Query v5** | Caching, invalidation, retry logic |
| Client state | **Zustand v5** | Auth store + UI store, devtools |
| Forms | **React Hook Form v7** | Performance-first |
| Validation | **Zod v3** | Schema-first, type-safe |
| Animations | **Framer Motion v11** | Sidebar, modals, page entries |
| Tables | **TanStack Table v8** | Server-side pagination, sort |
| Charts | **Recharts v2** | Revenue AreaChart, Order PieChart, Inventory BarChart |
| Unit tests | **Vitest v2** | jsdom environment |
| E2E tests | **Playwright v1.49** | Chromium + Firefox |
| Linting | **ESLint v9** | Flat config, Next.js rules |
| Formatting | **Prettier v3** | tailwindcss plugin |
| Git hooks | **Husky v9** | pre-commit (lint-staged) + commit-msg |
| Commit lint | **Commitlint** | Conventional Commits spec |

---

## 📦 Tech Stack — Phân tích sử dụng

### ✅ Đã sử dụng

- **Next.js 15** — App Router, layouts, server/client components, metadata
- **TypeScript** — Strict, toàn bộ types/interfaces từ DTO spec
- **TailwindCSS v4** — `@theme`, custom tokens, dark mode, utility classes
- **TanStack Query** — `useQuery`, `useMutation`, `queryClient.invalidateQueries`, devtools
- **Zustand** — `useAuthStore` (user, token, logout), `useUIStore` (sidebar)
- **React Hook Form** — Login form, create coupon, reject farm (với Zod resolver)
- **Zod** — Schema validation cho tất cả forms
- **Framer Motion** — Page entry animations, sidebar collapse, modal drawer, active nav indicator
- **TanStack Table** — `DataTable` component dùng chung, server-side pagination, tất cả trang admin
- **Recharts** — AreaChart (doanh thu), PieChart (trạng thái đơn hàng), BarChart (kho hàng)
- **Vitest** — Unit tests cho `utils`, `StatusBadge`
- **Playwright** — E2E tests cho login flow và dashboard
- **ESLint** — Flat config với Next.js + TypeScript rules
- **Prettier** — Format code + TailwindCSS class sorting
- **Husky** — pre-commit hook (lint-staged), commit-msg hook
- **Commitlint** — Conventional Commits enforcement

### ⏭ Chưa sử dụng / Có thể bỏ qua

- **shadcn/ui** — Dependencies đã khai báo (Radix UI), nhưng chưa generate components (có thể chạy `npx shadcn@latest add button` bất kỳ lúc nào khi cần). Dùng Tailwind classes trực tiếp cho nhanh trong phase này.

---

## 🚀 Cài đặt & Chạy

```bash
# 1. Cài dependencies
npm install

# 2. Cấu hình môi trường
cp .env.local.example .env.local
# Chỉnh NEXT_PUBLIC_API_URL nếu backend chạy port khác

# 3. Chạy dev server
npm run dev
# → http://localhost:3000

# 4. Build production
npm run build
npm start
```

---

## 🧪 Testing

```bash
# Unit tests (Vitest)
npm test

# Unit tests với coverage
npm run test:coverage

# E2E tests (Playwright — cần server đang chạy)
npm run test:e2e

# Xem HTML report
npx playwright show-report
```

---

## 📁 Cấu trúc dự án

```
src/
├── app/
│   ├── (auth)/
│   │   └── login/              # Trang đăng nhập
│   ├── admin/
│   │   ├── layout.tsx          # Layout với Sidebar + Header + AdminGuard
│   │   ├── dashboard/          # Trang tổng quan
│   │   ├── orders/             # Quản lý đơn hàng
│   │   ├── users/              # Quản lý người dùng
│   │   ├── farms/              # Phê duyệt nông trại
│   │   ├── coupons/            # Mã giảm giá
│   │   ├── reviews/            # Kiểm duyệt đánh giá
│   │   └── inventory/          # Báo cáo kho hàng
│   ├── globals.css             # TailwindCSS v4 + design tokens
│   └── layout.tsx              # Root layout + Providers + fonts
│
├── components/
│   ├── admin/
│   │   ├── admin-guard.tsx     # Route protection (ROLE_ADMIN)
│   │   ├── header.tsx          # Top bar
│   │   ├── sidebar.tsx         # Collapsible sidebar
│   │   └── stat-card.tsx       # Dashboard KPI cards
│   └── shared/
│       ├── data-table.tsx      # TanStack Table wrapper
│       ├── providers.tsx       # QueryClient + ThemeProvider
│       └── status-badge.tsx    # Màu sắc theo trạng thái
│
├── lib/
│   ├── api-client.ts           # Axios + interceptors (silent refresh)
│   ├── query-keys.ts           # TanStack Query key factory
│   └── utils.ts                # cn, formatCurrency, formatDate, ...
│
├── services/
│   ├── auth.service.ts         # login, logout, getMe, refresh
│   └── admin.service.ts        # orders, users, farms, coupons, reviews, inventory
│
├── store/
│   ├── auth.store.ts           # Zustand auth (persist)
│   └── ui.store.ts             # Zustand UI (sidebar)
│
├── types/
│   └── index.ts                # Tất cả DTO interfaces + enums
│
└── tests/
    ├── setup.ts                # Vitest + @testing-library setup
    ├── utils.test.ts           # Unit tests
    └── status-badge.test.tsx   # Component tests

e2e/
├── login.spec.ts               # Playwright login flow
└── dashboard.spec.ts           # Playwright dashboard
```

---

## 🔐 Authentication Flow

1. Người dùng nhập email/password → `POST /auth/login`
2. Nếu `ROLE_ADMIN` → lưu `accessToken` vào `localStorage`
3. Mọi request tiếp theo đính kèm `Authorization: Bearer <token>`
4. Khi nhận `401` → Axios interceptor tự gọi `POST /auth/refresh` (HttpOnly cookie)
5. Nếu refresh thành công → retry request gốc
6. Nếu refresh thất bại → xóa token, redirect về `/login`

---

## 🎨 Design System

- **Font chữ**: Bricolage Grotesque (headings) + DM Sans (body)
- **Màu chủ đạo**: Pine Green (`#22c55e`) — organic, tươi sáng
- **Nền**: Slate-950/900/800 — dark mode-first
- **Accent**: Amber warm cho cảnh báo, Cyan cho thông tin
- **Border radius**: `0.625rem` (md) consistent
- **Animations**: Framer Motion spring physics cho sidebar, fade+slide cho modals

---

## 📡 API Base URL

```
http://localhost:8080/api/v1
```

Cấu hình qua biến môi trường `NEXT_PUBLIC_API_URL`.
