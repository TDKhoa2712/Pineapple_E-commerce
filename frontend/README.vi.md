# 🍍 Pineapple Admin Dashboard & Client Frontend

[![Next.js](https://img.shields.io/badge/Next.js-15.0-black?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v4-38bdf8?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![Playwright](https://img.shields.io/badge/Playwright-E2E%20Tests-orange?logo=playwright&logoColor=white)](https://playwright.dev/)

Tài liệu này cung cấp chi tiết kiến trúc, các giải pháp kỹ thuật Frontend, hướng dẫn cài đặt và các tính năng nâng cao của trang quản trị (**Admin Dashboard**) và giao diện người dùng thuộc hệ thống Pineapple E-Commerce.

---

## 🛠️ Công Nghệ và Thư Viện Sử Dụng (Technical Stack)

| Hạng mục | Công nghệ | Chi tiết sử dụng |
|---|---|---|
| **Framework** | **Next.js 15** (App Router) | Tối ưu hóa SEO, React Server Components (RSC) kết hợp Client Components, sử dụng Turbopack cho quá trình phát triển (Dev Server). |
| **Ngôn ngữ** | **TypeScript** | Strict mode, đồng bộ toàn bộ Type/Interface từ DTO đặc tả của Backend. |
| **Styling** | **TailwindCSS v4** | Sử dụng cú pháp `@theme` mới, các biến Design Tokens tùy chỉnh cho màu sắc Organic và Dark Mode-First. |
| **Server State** | **TanStack Query v5** | Quản lý đồng bộ dữ liệu API, cơ chế tự động cache, revalidation ngầm, retry khi lỗi và giải phóng cache (`invalidateQueries`). |
| **Client State** | **Zustand v5** | Store gọn nhẹ cho trạng thái Đăng nhập/Quyền hạn (`useAuthStore` kèm persistence lưu trữ) và UI State (`useUIStore` quản lý Sidebar). |
| **Forms** | **React Hook Form v7** | Quản lý form với hiệu năng tối đa (hạn chế re-render), tích hợp Zod resolver. |
| **Validation** | **Zod v3** | Định nghĩa Schema Validation đồng bộ dữ liệu chặt chẽ từ phía client. |
| **Animations** | **Framer Motion v11** | Tạo chuyển động mượt mà cho Sidebar (spring physics), Modal Drawers, hiệu ứng chuyển trang và thanh chỉ mục menu hoạt họa. |
| **Tables** | **TanStack Table v8** | Hỗ trợ hiển thị dữ liệu bảng lớn với phân trang phía server (Server-side Pagination), sắp xếp và tìm kiếm động. |
| **Charts** | **Recharts v2** | Vẽ biểu đồ thống kê trực quan: AreaChart (Doanh thu), PieChart (Trạng thái đơn hàng), BarChart (Báo cáo tồn kho). |
| **Unit Testing** | **Vitest v2** | Kiểm thử các hàm tiện ích (utils) và logic hiển thị của các Component trong môi trường giả lập `jsdom`. |
| **E2E Testing** | **Playwright v1.49** | Kiểm thử tích hợp toàn trình (End-to-End) các luồng quan trọng: Luồng đăng nhập, bảo vệ tuyến đường (Guard Routes) và Dashboard Admin. |
| **Code Quality** | **ESLint 9 & Prettier 3** | Ràng buộc chuẩn code phẳng, tự động định dạng mã nguồn và sắp xếp class Tailwind. |
| **Git Hooks** | **Husky & Commitlint** | Kiểm tra cú pháp commit (Conventional Commits) và tự động chạy lint trước khi đẩy code lên Git (`pre-commit`). |

---

## 📁 Cấu Trúc Thư Mục Dự Án (Folder Structure)

Mã nguồn Frontend được tổ chức khoa học theo mô hình Modular của Next.js:

```
frontend/
├── src/
│   ├── app/                    # Thư mục chứa các trang & Layouts (App Router)
│   │   ├── (auth)/             # Route Group cho Đăng nhập, Đăng ký, Đổi mật khẩu
│   │   ├── admin/              # Dashboard điều hành, quản lý đơn hàng, duyệt nông trại
│   │   ├── globals.css         # TailwindCSS v4 directives & cấu hình design tokens
│   │   └── layout.tsx          # Root Layout chứa các Providers (QueryClient, Theme)
│   │
│   ├── components/             # Các Component tái sử dụng
│   │   ├── admin/              # Header, Sidebar, Thẻ thống kê KPI, Khóa bảo vệ Admin (AdminGuard)
│   │   └── shared/             # DataTable (TanStack Table dùng chung), StatusBadge, Toast
│   │
│   ├── lib/                    # Cấu hình lõi và Tiện ích dùng chung
│   │   ├── api-client.ts       # Axios instance cấu hình Silent Refresh Token Interceptor
│   │   ├── query-keys.ts       # Factory tạo key truy vấn động cho TanStack Query
│   │   └── utils.ts            # Các hàm định dạng tiền tệ, ngày tháng, cn class merge
│   │
│   ├── services/               # Lớp gọi API kết nối đến Backend
│   │   ├── auth.service.ts     # Các API xác thực (login, logout, refresh, OTP)
│   │   └── admin.service.ts    # Các API quản trị đơn hàng, người dùng, nông trại, coupon
│   │
│   ├── store/                  # Trạng thái toàn cục của ứng dụng (Zustand)
│   │   ├── auth.store.ts       # Lưu thông tin user, accessToken, vai trò (Role)
│   │   └── ui.store.ts             # Lưu trạng thái đóng/mở Sidebar
│   │
│   ├── types/                  # Định nghĩa kiểu dữ liệu tĩnh (DTOs & Enums)
│   │   └── index.ts            # Toàn bộ interface từ API specification
│   │
│   └── tests/                  # Các bài kiểm thử Unit & Component
│       ├── setup.ts            # Khởi tạo môi trường Vitest
│       └── status-badge.test.tsx # Kiểm thử hiển thị màu sắc theo trạng thái đơn hàng
│
├── e2e/                        # Các bài kiểm thử End-to-End (Playwright)
│   ├── login.spec.ts           # Kiểm thử luồng đăng nhập thành công/thất bại
│   └── dashboard.spec.ts       # Kiểm thử giao diện và quyền truy cập Admin Dashboard
│
└── tsconfig.json               # Cấu hình TypeScript compiler
```

---

## 📡 Các Điểm Nhấn Kỹ Thuật Nâng Cao (Advanced Engineering Practices)

### 1. Đồng Bộ Luồng Trạng Thái & Form Kiểm Duyệt Hợp Lệ
Màn hình quản trị tích hợp sâu các workflows trạng thái của Backend thông qua các luồng tương tác an toàn:
*   **Duyệt/Từ chối Nông trại & Lô hàng:** Khi Admin nhấp "Từ chối" một nông trại đăng ký mới hoặc một lô hàng nông sản của Farmer gửi lên, Frontend hiển thị Dialog yêu cầu nhập lý do từ chối. Form này được quản lý bằng `React Hook Form` kết hợp schema `Zod` bắt buộc độ dài ký tự từ `10` đến `500` để đảm bảo thông tin minh bạch.
*   **Trực Quan Hóa Tồn Kho:** Cột "Đơn vị tính" (`unit = 'kg'`) được hiển thị thống nhất trên cả catalog bán hàng lẫn bảng báo cáo Excel, giúp tính toán trọng lượng giao hàng chính xác qua API Giao Hàng Nhanh.
*   **Status Badge Động:** Sử dụng component `<StatusBadge>` được kiểm thử tự động bằng Vitest, tự động ánh xạ mã màu tương ứng với các trạng thái nghiệp vụ phức tạp của Backend như `PENDING_DEACTIVATION`, `EXPIRED`, `AVAILABLE`.

### 2. Cơ Chế Xử Lỗi Xác Thực Ngầm (Axios Silent Token Refresh)
Ứng dụng sử dụng cấu hình Axios Interceptor đặc biệt tại [api-client.ts](file:///d:/Self_Study/Java/Project_CV/Pineapple_E-commerce/frontend/src/lib/api-client.ts) để giải quyết bài toán trải nghiệm người dùng không bị ngắt quãng khi token hết hạn:
*   Mọi API request đi từ Client đều tự động đính kèm `accessToken` vào header.
*   Nếu nhận được phản hồi lỗi `401 Unauthorized` từ Backend (do token hết hạn), Interceptor tạm dừng request hiện tại.
*   Gọi ngầm API `POST /api/v1/auth/refresh` bằng phương thức `withCredentials: true` để trình duyệt tự động gửi kèm cookie chứa Refresh Token bảo mật.
*   Nếu làm mới thành công, Access Token mới được ghi đè vào RAM và localStorage, sau đó tự động kích hoạt lại các request đang bị xếp hàng chờ với token mới.
*   Nếu refresh thất bại, hệ thống tự động xóa bộ nhớ đệm và chuyển hướng người dùng về trang `/login`.

### 3. Tối Ưu Hóa Docker Standalone Build
Đối với môi trường chạy thực tế, Next.js được cấu hình chế độ `output: 'standalone'` trong [next.config.ts](file:///d:/Self_Study/Java/Project_CV/Pineapple_E-commerce/frontend/next.config.ts):
*   Khi chạy lệnh `npm run build`, Next.js tự động phân tích mã nguồn và gom các file runtime tối giản cần thiết vào thư mục `.next/standalone`.
*   Giúp loại bỏ toàn bộ thư mục `node_modules` nặng nề của môi trường phát triển.
*   Docker image được build bằng Multi-stage chỉ cần copy thư mục standalone này và chạy trực tiếp bằng lệnh `node server.js`. Dung lượng Image giảm thiểu tối đa (chỉ còn khoảng **180MB**), tăng tốc độ khởi động container và tiết kiệm bộ nhớ RAM trên máy chủ.

---

## 🧪 Hệ Thống Kiểm Thử (Testing Guide)

Dự án triển khai chiến lược kiểm thử nghiêm ngặt để đảm bảo chất lượng phần mềm không bị suy giảm khi nâng cấp mã nguồn:

### 1. Chạy Unit Tests với Vitest
Kiểm thử các hàm xử lý logic thuần túy và hiển thị component cơ bản:
```bash
# Chạy toàn bộ unit tests
npm test

# Chạy kiểm thử có theo dõi thay đổi (Watch mode)
npm run test:ui

# Xem báo cáo độ bao phủ mã nguồn (Code Coverage Report)
npm run test:coverage
```

### 2. Chạy E2E Tests với Playwright
Kiểm thử giả lập hành vi người dùng trên các trình duyệt thực tế (Chromium, Firefox, WebKit):
*Yêu cầu: Backend và Frontend đang chạy tại local.*
```bash
# Chạy kiểm thử toàn trình
npm run test:e2e

# Chạy kiểm thử ở chế độ giao diện trực quan (UI Mode)
npx playwright test --ui

# Xem báo cáo kết quả kiểm thử dạng HTML trực quan
npx playwright show-report
```

---

## 🚀 Cài Đặt & Khởi Chạy Local (Installation)

### 1. Khởi Tạo Tệp Cấu Hình
Sao chép tệp cấu hình mẫu và điền thông số API của bạn:
```bash
cp .env.local.example .env.local
```
Cập nhật URL API của Backend nếu cổng chạy của bạn khác mặc định:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

### 2. Chạy Ứng Dụng
```bash
# 1. Cài đặt các thư viện phụ thuộc
npm install

# 2. Khởi chạy máy chủ phát triển (sử dụng Turbopack)
npm run dev
```
Truy cập [http://localhost:3000](http://localhost:3000) trên trình duyệt của bạn.
Để xây dựng ứng dụng cho Production và chạy thử:
```bash
npm run build
npm run start
```
Ứng dụng sẽ được tối ưu hóa tĩnh và chạy trực tiếp tại cổng `3000`.
