# 🍍 Pineapple Admin Dashboard & Client Frontend

[![Next.js](https://img.shields.io/badge/Next.js-15.0-black?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-v4-38bdf8?logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![Playwright](https://img.shields.io/badge/Playwright-E2E%20Tests-orange?logo=playwright&logoColor=white)](https://playwright.dev/)

This repository contains the administrative control center (**Admin Dashboard**) and client storefront interface for the Pineapple E-Commerce platform, built using modern React structures.

---

## 🛠️ Technology Stack & Libraries

| Category | Technology | Usage Description |
|---|---|---|
| **Core Framework** | **Next.js 15** (App Router) | Tối ưu hóa SEO, React Server Components (RSC) integrated with interactive client views, utilizing Turbopack in development. |
| **Language** | **TypeScript** | Strict mode compilation, mirroring all database structures and backend DTOs. |
| **Styling** | **TailwindCSS v4** | Clean utility structure using the new `@theme` API, custom organic design tokens, and dark mode optimizations. |
| **Server State** | **TanStack Query v5** | API request state management, automated query caching, silent revalidation, mutations tracking, and global cache invalidation. |
| **Client State** | **Zustand v5** | Lightweight client-side store managing session state (`useAuthStore` with local persistence) and navigation drawer state (`useUIStore`). |
| **Forms & State** | **React Hook Form v7** | Uncontrolled form inputs minimizing re-renders, integrated with Zod validation adapters. |
| **Validation** | **Zod v3** | Strongly typed client-side schema validation matching backend DTO schemas. |
| **Animations** | **Framer Motion v11** | Spring-based physics for collapsable components, modal layouts, and navigation indicator trails. |
| **Grids & Tables** | **TanStack Table v8** | High-density tables with built-in hooks for server-side pagination, remote sorting, and filtering. |
| **Charts** | **Recharts v2** | Interactive metrics widgets: Area charts (Revenue dynamics), Pie charts (Order allocations), and Bar charts (Inventory volume). |
| **Unit Testing** | **Vitest v2** | Executed in a virtual `jsdom` sandbox testing utilities, hooks, and presentation badges. |
| **E2E Testing** | **Playwright v1.49** | Automated browser flows simulating authentication guards, route protections, and CRUD operations. |
| **Code Quality** | **ESLint 9 & Prettier 3** | Automated style consistency checks and class sort algorithms. |
| **Git Hooks** | **Husky & Commitlint** | Checks commit formatting (Conventional Commits) and runs pre-push linters. |

---

## 📁 Directory Structure

The application follows the Next.js App Router modular directory pattern:

```
frontend/
├── src/
│   ├── app/                    # Next.js routes, layouts, and page structures
│   │   ├── (auth)/             # Route Group for Login, Register, Verification
│   │   ├── admin/              # Admin panels, orders tables, farm moderation
│   │   ├── globals.css         # CSS imports, Tailwind directives, and design tokens
│   │   └── layout.tsx          # Root layout defining context providers
│   │
│   ├── components/             # Reusable UI elements
│   │   ├── admin/              # Layout structures, sidebar drawers, route locks (AdminGuard)
│   │   └── shared/             # DataTable wrapper, StatusBadge, toast triggers
│   │
│   ├── lib/                    # Core configuration and helpers
│   │   ├── api-client.ts       # Axios instance with silent refresh token interceptors
│   │   ├── query-keys.ts       # Dynamic TanStack Query cache key factory
│   │   └── utils.ts            # Formatting functions (currency, dates)
│   │
│   ├── services/               # REST API connector classes
│   │   ├── auth.service.ts     # User sessions and credentials verification
│   │   └── admin.service.ts    # Dashboard metrics, approval forms, inventory updates
│   │
│   ├── store/                  # Client state stores (Zustand)
│   │   ├── auth.store.ts       # Persistence logic for access tokens and user roles
│   │   └── ui.store.ts         # Global sidebar drawers display states
│   │
│   ├── types/                  # TypeScript interface mappings
│   │   └── index.ts            # Generated type structures mirroring backend DTO specs
│   │
│   └── tests/                  # Unit and component test specifications
│       ├── setup.ts            # Vitest initialization script
│       └── status-badge.test.tsx # Status badges presentation tests
│
├── e2e/                        # End-to-End tests (Playwright)
│   ├── login.spec.ts           # Login verification workflows
│   └── dashboard.spec.ts       # Audit and permission verification checks
│
└── tsconfig.json               # TypeScript compiler config
```

---

## 📡 Core Frontend Architecture Highlights

### 1. Unified Auditing Interface & Form Validation
The admin interface bridges with backend validation rules for entity workflows:
*   **Farm & Batch Approvals:** When an administrator clicks to reject a farm certification or inventory batch, the frontend displays an overlay dialog requesting justification (`rejection_reason` / `status_reason`). This form is validated by a `Zod` schema enforcing character boundaries (between `10` and `500` characters) before launching the query mutation.
*   **Measurement Standardizations:** The frontend dynamically renders the custom unit field (`unit` - e.g., `kg` or custom counts added in migration V7) across product listings and Excel summaries, facilitating weight calculations sent to Giao Hàng Nhanh (GHN) API clients.
*   **Dynamic Presentation Badges:** Component `<StatusBadge>` maps status strings (e.g., `PENDING_DEACTIVATION`, `EXPIRED`, `AVAILABLE`) to organic theme colors. The component's rendering logic is fully verified by Vitest unit tests.

### 2. Silent Token Refresh Interceptor
Utilizes Axios interceptors configured in [api-client.ts](file:///d:/Self_Study/Java/Project_CV/Pineapple_E-commerce/frontend/src/lib/api-client.ts) to manage JWT rotation behind the scenes:
*   Requests inject the `accessToken` header automatically.
*   Upon intercepting a `401 Unauthorized` response (due to access token expiration), the client queues pending requests.
*   Issues a silent request to `POST /api/v1/auth/refresh` sending the browser's HttpOnly cookie (`withCredentials: true`).
*   On refresh success, updates the local Access Token and automatically retries all queued requests.
*   If the refresh token itself has expired (invalidated by backend DB validation), clears active sessions and redirects the user to `/login`.

### 3. Next.js Standalone Docker Deployment
Configured with `output: 'standalone'` in [next.config.ts](file:///d:/Self_Study/Java/Project_CV/Pineapple_E-commerce/frontend/next.config.ts):
*   `npm run build` generates a standalone Node.js server inside `.next/standalone`.
*   Includes only the dependency files needed for runtime execution, excluding development node modules.
*   Reduces the final Docker container size to approximately **180MB**, optimizing startup latencies and deployment speeds.

---

## 🧪 Testing Guidelines

The codebase implements a dual testing strategy to maintain interface consistency:

### 1. Vitest Unit & Integration Tests
Tests utility functions and layout components:
```bash
# Run unit tests
npm test

# Run tests in UI watch mode
npm run test:ui

# Generate test coverage reports
npm run test:coverage
```

### 2. Playwright E2E Integration Tests
Runs simulated browser workflows across Chromium, Firefox, and WebKit:
*Note: Requires local backend and frontend instances running.*
```bash
# Execute playwright test files
npm run test:e2e

# Launch interactive UI mode
npx playwright test --ui

# Open HTML test execution reports
npx playwright show-report
```

---

## 🚀 Installation & Local Run

### 1. Environment Configurations
Create a `.env.local` file in the `/frontend` directory:
```bash
cp .env.local.example .env.local
```
Configure your backend API base endpoint:
```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 2. Running the Client
```bash
# 1. Install required node dependencies
npm install

# 2. Start Next.js Turbopack dev server
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) in your browser.
To compile a production bundle and run the server locally:
```bash
npm run build
npm run start
```
The optimized bundle is served on port `3000`.
