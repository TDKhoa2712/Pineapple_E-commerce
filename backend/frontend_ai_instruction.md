# SYSTEM PROMPT & SPECIFICATION: BUILD PINEAPPLE E-COMMERCE FRONTEND

Copy and paste the entire content of this document into any AI Developer Assistant (such as Claude, ChatGPT, Cursor, or Gemini) to prompt it to build the complete Frontend application.

---

### **[START OF AI PROMPT]**

You are an expert Senior Frontend Developer. Your task is to build a premium, highly responsive, and modern Web Frontend for **"Pineapple E-Commerce"**, an online retail store specializing in fresh agricultural and organic foods.

Use a modern, professional, and visually stunning design system with organic tones (e.g., forest greens, warm ambers, clean white/slate backgrounds) and high-quality UI details (such as micro-animations, skeletons for loading states, and toast notifications).

---

## 1. TECH STACK & CONFIGURATION

*   **Framework:** React (Vite or Next.js App Router).
*   **Styling:** TailwindCSS (with a custom theme featuring organic green, warm yellow, and clean dark mode settings).
*   **State Management:** React Context, Redux Toolkit, or Zustand (for user profile, wishlist count, and cart badge).
*   **HTTP Client:** Axios (configured with `withCredentials: true` to support HttpOnly Cookie handling).
*   **Base API URL:** `http://localhost:8080/api/v1`

---

## 2. API CONTRACTS & STANDARD ENVELOPE

All backend APIs wrap responses in a standard JSON wrapper:

### Success Response:
```json
{
  "success": true,
  "message": "Action completed successfully",
  "data": { ... }, // Can be an object or list
  "timestamp": "2026-05-30T06:11:04"
}
```

### Error Response:
```json
{
  "success": false,
  "message": "Error details here",
  "errors": { ... }, // Optional object/list for form validation errors
  "timestamp": "2026-05-30T06:11:04"
}
```

### Paginated Response (PageResponse):
When calling list APIs (e.g. search products, view orders), the `data` field contains:
```json
{
  "content": [ ... ], // Array of items for current page
  "page": 0,          // Current page index (0-indexed)
  "size": 20,         // Size of page
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

---

## 3. CORE FRONTEND WORKFLOWS & INTEGRATION PATTERNS

### A. Authentication & Silent Token Refresh (JWT)
1.  **AccessToken:** Valid for **15 minutes** (returned in body). Store in `localStorage`. Attach to request headers as: `Authorization: Bearer <token>`.
2.  **RefreshToken:** Valid for **7 days**. Managed as a secure **HttpOnly Cookie** by the browser.
3.  **Axios Interceptor for Silent Refresh:**
    *   Set up a response interceptor. If an API request fails with a `401 Unauthorized` status:
        *   Initiate a background request to `POST /api/v1/auth/refresh` (configured with `withCredentials: true`).
        *   If successful, save the new `accessToken` to `localStorage` and retry the original failed request.
        *   If the refresh call fails, clear the local token and redirect to `/login`.

### B. OAuth2 Social Login (Google / Facebook)
1.  Provide Google & Facebook login buttons.
2.  On click, redirect the user's browser to the backend authorization URL:
    *   Google: `http://localhost:8080/oauth2/authorization/google?redirect_uri=http://localhost:3000/oauth2/callback`
3.  After successful login on the provider site, the backend redirects the browser back to the frontend:
    *   Frontend URL: `http://localhost:3000/oauth2/callback?code=<exchange_code>`
4.  In the `/oauth2/callback` route, parse the `code` parameter and immediately dispatch a POST request:
    *   `POST /api/v1/auth/oauth2/exchange`
    *   Body: `{ "code": "<exchange_code>" }`
5.  Save the returned `accessToken` to `localStorage` and redirect to the Home page.

### C. Smart Cart Merge Flow
1.  When users browse as guests, store their cart items in `localStorage`.
2.  Immediately after the user logs in (local or social) and you save the token, call:
    *   `POST /api/v1/cart/merge`
    *   Body: `{ "items": [ { "productId": 101, "quantity": 2 }, ... ] }`
3.  Display any skipped items returned in `data.skippedItems` (e.g., out-of-stock or inactive products) via a toast notification.
4.  Clear the guest cart from `localStorage` once merged.

### D. VNPay Payment Verification
1.  During checkout, if the user selects `VNPAY` as their payment method, call:
    *   `POST /api/v1/payments/{orderId}/initiate`
2.  Extract the `paymentUrl` from the response and redirect the user.
3.  VNPay will redirect the user back to the frontend at:
    *   Frontend URL: `http://localhost:3000/payment/result?status=success&txnRef=...`
4.  **Security Rule:** Do **NOT** rely on the query parameters to show the success screen. Instead, immediately call the backend query API using the order ID:
    *   `GET /api/v1/payments/order/{orderId}`
    *   Verify the actual database payment status before displaying the "Payment Successful" screen.

---

## 4. API ENDPOINT DIRECTORY

Integrate the frontend features with the following endpoints:

### Address Module (`/api/v1/addresses`)
*   `GET /api/v1/addresses/` - Fetch all user addresses.
*   `POST /api/v1/addresses/` - Add a new address.
    *   *Payload:* `{ "receiverName": "...", "phone": "...", "province": "...", "district": "...", "ward": "...", "detail": "...", "isDefault": false, "carrierMetadata": { "GHN": { "provinceId": "...", "districtId": "...", "wardCode": "..." } } }`
*   `PUT /api/v1/addresses/{addressId}` - Update address details.
*   `PATCH /api/v1/addresses/{addressId}/default` - Set address as default.
*   `DELETE /api/v1/addresses/{addressId}` - Delete address.

### Authentication Module (`/api/v1/auth`)
*   `POST /api/v1/auth/register` - Create new local account.
*   `POST /api/v1/auth/verify-email` - Verify email using 6-digit OTP. Body: `{ "email": "...", "otp": "..." }`.
*   `POST /api/v1/auth/resend-verification` - Resend email OTP. Body: `{ "email": "..." }`.
*   `POST /api/v1/auth/login` - Local email/password login.
*   `POST /api/v1/auth/oauth2/exchange` - Exchange OAuth2 temporary code for tokens.
*   `POST /api/v1/auth/refresh` - Refresh access token (sends HttpOnly cookie).
*   `POST /api/v1/auth/logout` - Clear cookies and log out.
*   `GET /api/v1/auth/me` - Fetch profile of logged-in user.
*   `POST /api/v1/auth/password-reset/initiate` - Request OTP to reset password. Body: `{ "email": "..." }`.
*   `POST /api/v1/auth/password-reset/confirm` - Confirm reset OTP and set new password. Body: `{ "email": "...", "otp": "...", "newPassword": "..." }`.

### Shopping Cart Module (`/api/v1/cart`)
*   `GET /api/v1/cart/` - Get active cart and items.
*   `GET /api/v1/cart/count` - Get count of distinct items in cart.
*   `POST /api/v1/cart/items` - Add item to cart. Body: `{ "productId": 101, "quantity": 1 }`.
*   `PUT /api/v1/cart/items/{cartItemId}` - Update quantity of item (quantity=0 deletes it). Body: `{ "quantity": 3 }`.
*   `DELETE /api/v1/cart/items/{cartItemId}` - Remove item from cart.
*   `DELETE /api/v1/cart/` - Clear cart.
*   `POST /api/v1/cart/validate` - Validate stock constraints for cart items before checkout.
*   `POST /api/v1/cart/merge` - Merge guest items with database items.

### Category Module (`/api/v1/categories`)
*   `GET /api/v1/categories/tree` - Fetch categories structured as a tree (useful for headers/side menus).
*   `GET /api/v1/categories/` - Fetch flat list of categories.
*   `GET /api/v1/categories/{id}` - Get category by ID.
*   `GET /api/v1/categories/slug/{slug}` - Get category by slug (SEO-friendly routing).

### Coupon Module (`/api/v1/coupons` & `/api/v1/admin/coupons`)
*   `POST /api/v1/coupons/preview` - Preview discount calculation. Body: `{ "couponCode": "...", "orderAmount": ... }`.
*   `POST /api/v1/admin/coupons/` - [Admin] Create new coupon.
*   `GET /api/v1/admin/coupons/` - [Admin] List all coupons.
*   `PUT /api/v1/admin/coupons/{id}` - [Admin] Update coupon.
*   `GET /api/v1/admin/coupons/{id}/usage` - [Admin] Retrieve usage log for coupon.

### Farm Module (`/api/v1/farms`)
*   `GET /api/v1/farms/` - Get all active farms (paginated).
*   `GET /api/v1/farms/{farmId}` - Get farm by ID.
*   `GET /api/v1/farms/{farmId}/products` - Get products belonging to a specific farm.
*   `GET /api/v1/farms/my` - [Farmer] View farms managed by current account.
*   `POST /api/v1/farms/` - [Farmer] Register a new farm (status will be PENDING_APPROVAL).
*   `PUT /api/v1/farms/{farmId}` - [Farmer] Update farm details.
*   `POST /api/v1/farms/{farmId}/image` - [Farmer] Upload image for farm (via Multipart File).
*   `DELETE /api/v1/farms/{farmId}` - Soft delete farm.
*   `GET /api/v1/farms/admin` - [Admin] List all farms (with status filters).
*   `PATCH /api/v1/farms/admin/{farmId}/approve` - [Admin] Approve a pending farm.
*   `PATCH /api/v1/farms/admin/{farmId}/reject` - [Admin] Reject a pending farm. Body: `{ "reason": "..." }`.

### Order Module (`/api/v1/orders`)
*   `POST /api/v1/orders/` - Convert cart to order.
    *   *Payload:* `{ "addressId": ..., "paymentMethod": "COD|VNPAY", "note": "...", "couponCode": "..." }`
*   `GET /api/v1/orders/my` - List current user's orders (supports filter param `status`).
*   `GET /api/v1/orders/{orderId}` - Get order details.
*   `POST /api/v1/orders/{orderId}/cancel` - Cancel order (only allowed if status is `PENDING`).
*   `POST /api/v1/orders/{orderId}/request-refund` - Request online payment refund.
*   `GET /api/v1/orders/admin` - [Admin] Query all orders with filter criteria.
*   `PATCH /api/v1/orders/admin/{orderId}/status` - [Admin] Update order status. Body: `{ "status": "PROCESSING|SHIPPED|DELIVERED|CANCELLED" }`.

### Payment Module (`/api/v1/payments`)
*   `POST /api/v1/payments/{orderId}/initiate` - Initialize VNPay payment link.
*   `GET /api/v1/payments/order/{orderId}` - Fetch order payment details.

### Product Module (`/api/v1/products`)
*   `GET /api/v1/products/` - Search and filter products (paginated).
    *   *Params:* `keyword`, `categoryId`, `farmId`, `inStock` (true/false), `minPrice`, `maxPrice`, `isOrganic`, `page`, `size`, `sortBy` (newest, price_asc, price_desc).
*   `GET /api/v1/products/{id}` - Get product detail by ID.
*   `GET /api/v1/products/slug/{slug}` - Get product detail by SEO slug.
*   `GET /api/v1/products/{id}/related` - Get related products (same category).
*   `GET /api/v1/products/{id}/stock` - Fetch current physical stock count.
*   `POST /api/v1/products/` - [Farmer/Admin] Create product.
*   `POST /api/v1/products/{id}/images` - [Farmer/Admin] Upload multi-images (Multipart Form Data).
*   `POST /api/v1/products/{id}/thumbnail` - [Farmer/Admin] Upload main thumbnail (Multipart Form Data).
*   `PUT /api/v1/products/{id}` - [Farmer/Admin] Update product.
*   `DELETE /api/v1/products/{id}` - [Admin] Delete product (soft-delete, switches status to INACTIVE).

### Review Module (`/api/v1/reviews`)
*   `GET /api/v1/reviews/product/{productId}` - View product reviews (paginated).
*   `GET /api/v1/reviews/product/{productId}/rating` - View average rating.
*   `POST /api/v1/reviews/` - Create a review. Body: `{ "productId": ..., "rating": 5, "comment": "..." }`. (Only allowed for users who purchased the item).
*   `PUT /api/v1/reviews/{reviewId}` - Update review.
*   `POST /api/v1/reviews/{reviewId}/vote` - Vote helpfulness of review.
*   `DELETE /api/v1/reviews/{reviewId}` - Delete review.
*   `GET /api/v1/reviews/admin/all` - [Admin] View all reviews in the system.
*   `PATCH /api/v1/reviews/admin/{reviewId}/hide` - [Admin] Hide review.

### Shipping Module (`/api/v1/shipping`)
*   `GET /api/v1/shipping/carriers` - List supported shipping carrier codes.
*   `GET /api/v1/shipping/provinces` - Get province list.
*   `GET /api/v1/shipping/districts?provinceId=...` - Get districts list.
*   `GET /api/v1/shipping/wards?districtId=...` - Get wards list.
*   `POST /api/v1/shipping/calculate-fee` - Calculate shipping fee based on distance & weight.
*   `GET /api/v1/shipping/orders/{orderId}/tracking` - Get tracking log.

### User Module (`/api/v1/users`)
*   `GET /api/v1/users/me` - Get current user profile.
*   `PUT /api/v1/users/me` - Update profile details. Body: `{ "fullName": "...", "phone": "..." }`.
*   `POST /api/v1/users/me/avatar` - Upload new avatar image (Multipart Form Data).
*   `POST /api/v1/users/me/change-password` - Change password.
*   `GET /api/v1/users/` - [Admin] List users.
*   `PATCH /api/v1/users/{userId}/status` - [Admin] Lock/Unlock account status.
*   `PUT /api/v1/users/{userId}/roles` - [Admin] Update user roles. Body: `{ "roles": ["USER", "FARMER"] }`.

### Wishlist Module (`/api/v1/wishlist`)
*   `GET /api/v1/wishlist/` - List user's wishlist (paginated).
*   `POST /api/v1/wishlist/{productId}` - Toggle item in wishlist (adds if absent, removes if present). Returns updated boolean state.
*   `GET /api/v1/wishlist/{productId}/check` - Check if item is currently wishlisted.

---

## 5. UI/UX PAGES TO BUILD
1.  **Home Page:** Clean header, organic banners, category slider, farm listings, featured items, and custom search bar.
2.  **Product Search & Category Page:** Left-hand sidebar filters (price range, organic toggle, stock state), sort dropdown, and product card grid.
3.  **Product Detail Page:** Image carousel, SEO-friendly slug routing, rating counts, farm information, related products section, and add-to-cart controls.
4.  **Cart & Checkout Page:** Cart item summary, Smart Merge indicator, address picker/adder (integrated with GHN dynamic provinces/districts/wards dropdowns), shipping carrier selector, shipping fee query, order placement, and VNPay redirect.
5.  **Profile & Portal Dashboard:**
    *   *Customer Portal:* Manage addresses, order history, tracking details, and profile avatar changes.
    *   *Farmer Portal:* Register farm, manage products, view stock, add stock batches, and edit details.
    *   *Admin Dashboard:* Moderate farms, unlock accounts, configure coupons, view order records, and run inventory reports.

Now, proceed to structure and write clean, component-oriented, modular code (using React components, custom hooks, and TailwindCSS configuration) that covers these requirements.


## 6. ENUM DEFINITIONS (BUSINESS CONSTANTS)

These enums are used in many DTOs and query parameters. Define them as TypeScript types:

```typescript
// ── User & Auth ──────────────────────────────────────────────────
export type RoleName = "ROLE_USER" | "ROLE_ADMIN" | "ROLE_FARMER";
export type UserStatus = "ACTIVE" | "INACTIVE" | "BANNED";
export type AuthProvider = "LOCAL" | "GOOGLE" | "FACEBOOK";

// ── Product ──────────────────────────────────────────────────────
export type ProductStatus = "ACTIVE" | "INACTIVE" | "OUT_OF_STOCK";

// ── Farm ─────────────────────────────────────────────────────────
export type FarmStatus = "PENDING_APPROVAL" | "ACTIVE" | "INACTIVE" | "REJECTED";

// ── Inventory ────────────────────────────────────────────────────
export type BatchStatus = "AVAILABLE" | "EXPIRED" | "DEPLETED";

// ── Order ────────────────────────────────────────────────────────
export type OrderStatus =
  | "PENDING"          // Chờ xác nhận / chờ thanh toán
  | "CONFIRMED"        // Đã xác nhận (đã thanh toán online hoặc COD)
  | "PROCESSING"       // Đang xử lý / đóng gói
  | "SHIPPING"         // Đang giao hàng
  | "DELIVERED"        // Giao thành công
  | "REFUND_REQUESTED" // Yêu cầu hoàn tiền
  | "REFUNDED"         // Đã hoàn tiền (terminal)
  | "RETURNED"         // Đã hoàn hàng (terminal)
  | "CANCELLED";       // Đã hủy (terminal)

// ── Payment ──────────────────────────────────────────────────────
export type PaymentMethod = "COD" | "VNPAY" | "MOMO" | "BANK_TRANSFER";
export type PaymentStatus = "UNPAID" | "PAID" | "REFUNDED" | "FAILED";
export type PaymentProvider = "COD" | "VNPAY" | "MOMO" | "BANK";

// ── Shipping ─────────────────────────────────────────────────────
export type CarrierCode = "GHN" | "GHTK";
export type ShippingStatus =
  | "PENDING_PICKUP" | "PICKING_UP" | "PICKED_UP"
  | "IN_TRANSIT" | "AT_WAREHOUSE" | "SORTING"
  | "OUT_FOR_DELIVERY" | "DELIVERY_FAILED" | "DELIVERED"
  | "RETURNING" | "RETURNED"
  | "CANCELLED" | "EXCEPTION" | "LOST" | "DAMAGED" | "UNKNOWN";

// ── Coupon ────────────────────────────────────────────────────────
export type CouponType = "PERCENTAGE" | "FIXED_AMOUNT";
```

---

## 7. DATA TRANSFER OBJECT (DTO) SCHEMAS (TYPESCRIPT INTERFACES)

Use the following TypeScript interfaces to type-check and map all request payloads and response shapes.
Fields that may be `null` from the backend are marked optional (`?`).

```typescript
// ═══════════════════════════════════════════════════════════════
// GENERIC WRAPPERS (use these to wrap every API call)
// ═══════════════════════════════════════════════════════════════
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: Record<string, string> | string[];
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;       // 0-indexed
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ═══════════════════════════════════════════════════════════════
// ADDRESS
// ═══════════════════════════════════════════════════════════════
export interface CreateAddressRequest {
  receiverName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  detail: string;
  isDefault?: boolean;
  carrierMetadata?: Record<string, any>;
}

export interface AddressResponse {
  id: number;
  receiverName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  detail: string;
  isDefault: boolean;
  carrierMetadata?: Record<string, any>;
  fullAddress: string; // computed: "detail, ward, district, province"
}

// ═══════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════
export interface RegisterRequest {
  email: string;
  password: string;        // min 8, max 100
  fullName: string;        // max 100
  phone?: string;          // regex: ^(0[3|5|7|8|9])+([0-9]{8})$
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface VerifyEmailRequest {
  email: string;
  otp: string;             // exactly 6 digits
}

export interface ResendVerificationRequest {
  email: string;
}

export interface OAuth2ExchangeRequest {
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface InitiatePasswordResetRequest {
  email: string;
}

export interface ConfirmPasswordResetRequest {
  email: string;
  otp: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

export interface AuthResponse {
  accessToken?: string;    // null when emailVerified=false
  refreshToken?: string;   // null when emailVerified=false or OAuth2 (stored in cookie)
  tokenType?: string;      // "Bearer"
  expiresIn?: number;      // milliseconds
  userId: number;
  email: string;
  fullName: string;
  roles: string[];         // e.g. ["ROLE_USER"]
  emailVerified: boolean;
  message?: string;        // guidance text when emailVerified=false
}

// ═══════════════════════════════════════════════════════════════
// CART
// ═══════════════════════════════════════════════════════════════
export interface AddToCartRequest {
  productId: number;
  quantity: number;        // min 1
}

export interface UpdateCartItemRequest {
  quantity: number;        // 0 = remove item
}

export interface CartItemMerge {
  productId: number;
  quantity: number;
}

export interface MergeCartRequest {
  items: CartItemMerge[];
}

export interface CartItemResponse {
  id: number;
  productId: number;
  productName: string;
  productThumbnail: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  availableStock: number;
}

export interface CartResponse {
  cartId: number;
  items: CartItemResponse[];
  totalItems: number;
  totalAmount: number;
}

export interface CartItemWarning {
  productId: number;
  productName: string;
  warningType: "OUT_OF_STOCK" | "INSUFFICIENT_STOCK";
  message: string;
  requestedQty: number;
  availableQty: number;
}

export interface CartValidationResponse {
  isValid: boolean;
  warnings: CartItemWarning[];
  estimatedTotal: number;
}

export interface SkippedItem {
  productId: number;
  productName: string;
  reason: "OUT_OF_STOCK" | "PRODUCT_INACTIVE" | "STOCK_CAPPED";
  message: string;
  requestedQty: number;
  actualQty: number;
}

export interface MergeCartResponse {
  cart: CartResponse;
  mergedCount: number;
  skippedItems: SkippedItem[];
}

// ═══════════════════════════════════════════════════════════════
// CATEGORY
// ═══════════════════════════════════════════════════════════════
export interface CreateCategoryRequest {
  name: string;
  slug?: string;
  image?: string;
  parentId?: number;
}

export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  image?: string;
  parentId?: number;
  parentName?: string;
  children?: CategoryResponse[];  // populated in /tree endpoint
}

// ═══════════════════════════════════════════════════════════════
// COUPON
// ═══════════════════════════════════════════════════════════════
export interface CouponPreviewRequest {
  couponCode: string;
  cartTotal: number;
}

export interface CouponPreviewResponse {
  couponCode: string;
  discountAmount: number;
  newTotal: number;
}

export interface CreateCouponRequest {
  code: string;
  type: CouponType;
  value: number;
  maxDiscountAmount?: number;
  minOrderValue?: number;
  startDate: string;        // ISO datetime
  expiryDate: string;       // ISO datetime
  totalLimit?: number;
  perUserLimit?: number;
  applicableProductIds?: number[];
  applicableCategoryIds?: number[];
}

export interface UpdateCouponRequest {
  isActive?: boolean;
  totalLimit?: number;
  perUserLimit?: number;
}

export interface CouponResponse {
  id: number;
  code: string;
  type: CouponType;
  value: number;
  maxDiscountAmount?: number;
  minOrderValue?: number;
  startDate: string;
  expiryDate: string;
  totalLimit?: number;
  usedCount: number;
  perUserLimit?: number;
  isActive: boolean;
  applicableProductIds: number[];
  applicableCategoryIds: number[];
  createdById: number;
  createdByEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface CouponUsageResponse {
  id: number;
  couponId: number;
  couponCode: string;
  userId: number;
  userEmail: string;
  orderId: number;
  discountApplied: number;
  usedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// FARM
// ═══════════════════════════════════════════════════════════════
export interface CreateFarmRequest {
  name: string;
  location: string;
  description?: string;
  certificate?: string;
  imageUrl?: string;
}

export interface RejectFarmRequest {
  reason: string;
}

export interface FarmResponse {
  id: number;
  ownerId: number;
  ownerName: string;
  name: string;
  location: string;
  description?: string;
  certificate?: string;
  imageUrl?: string;
  status: FarmStatus;
  rejectionReason?: string;
  createdAt: string;
}

// ═══════════════════════════════════════════════════════════════
// INVENTORY
// ═══════════════════════════════════════════════════════════════
export interface CreateInventoryBatchRequest {
  productId: number;
  farmId: number;
  batchCode: string;
  quantity: number;
  harvestDate: string;       // ISO date
  expiryDate: string;        // ISO date
  sweetnessLevel?: number;
}

export interface StockAdjustmentRequest {
  adjustmentQty: number;     // positive = increase, negative = decrease
  reason: string;
}

export interface InventoryBatchResponse {
  id: number;
  productId: number;
  productName: string;
  farmId: number;
  farmName: string;
  batchCode: string;
  quantity: number;
  remainingQuantity: number;
  harvestDate: string;
  expiryDate: string;
  sweetnessLevel?: number;
  status: BatchStatus;
}

export interface StockAdjustmentResponse {
  id: number;
  batchId: number;
  batchCode: string;
  productId: number;
  productName: string;
  adjustmentQty: number;
  reason: string;
  qtyBefore: number;
  qtyAfter: number;
  adjustedByName: string;
  createdAt: string;
}

export interface InventorySummaryResponse {
  productId: number;
  productName: string;
  totalStock: number;
  batchCount: number;
}

export interface InventoryReportResponse {
  from: string;
  to: string;
  summary: ReportSummary;
  details: ProductReportDetail[];
}

export interface ReportSummary {
  totalBatchesImported: number;
  totalQuantityImported: number;
  totalQuantitySold: number;
  totalBatchesExpired: number;
  totalQuantityExpired: number;
  currentAvailableStock: number;
}

export interface ProductReportDetail {
  productId: number;
  productName: string;
  batchesImported: number;
  quantityImported: number;
  quantitySold: number;
  batchesExpired: number;
  quantityExpired: number;
  currentStock: number;
  earliestImport: string;
  latestImport: string;
}

// ═══════════════════════════════════════════════════════════════
// ORDER
// ═══════════════════════════════════════════════════════════════
export interface CreateOrderRequest {
  addressId: number;
  paymentMethod: PaymentMethod;
  note?: string;
  couponCode?: string;
}

export interface BulkOrderStatusRequest {
  orderIds: number[];
  newStatus: OrderStatus;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  productThumbnail: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  batchId?: number;
  batchCode?: string;
  batches?: BatchAllocationResponse[];
}

export interface BatchAllocationResponse {
  batchId: number;
  batchCode: string;
  quantity: number;
}

export interface OrderResponse {
  id: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  paymentMethod: PaymentMethod;
  shippingAddress: string;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  note?: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// PAYMENT
// ═══════════════════════════════════════════════════════════════
export interface PaymentResponse {
  id: number;
  orderId: number;
  provider: PaymentProvider;
  transactionCode?: string;
  amount: number;
  status: PaymentStatus;
  paidAt?: string;
  paymentUrl?: string;       // present only from /initiate for VNPAY
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT
// ═══════════════════════════════════════════════════════════════
export interface CreateProductRequest {
  name: string;
  slug?: string;             // auto-generated if blank
  description?: string;
  price: number;
  discountPrice?: number;
  weight?: number;           // grams
  calories?: number;
  brand?: string;
  origin?: string;
  isOrganic?: boolean;
  thumbnail: string;         // must be Cloudinary URL
  categoryId: number;
  imageUrls?: string[];      // must be Cloudinary URLs
}

export interface UpdateProductRequest {
  name?: string;
  description?: string;
  price?: number;
  discountPrice?: number;
  weight?: number;
  calories?: number;
  brand?: string;
  origin?: string;
  isOrganic?: boolean;
  thumbnail?: string;
  categoryId?: number;
  status?: ProductStatus;
  imageUrls?: string[];
}

export interface ProductSummaryResponse {
  id: number;
  name: string;
  slug: string;
  price: number;
  discountPrice?: number;
  effectivePrice: number;
  thumbnail: string;
  isOrganic: boolean;
  status: ProductStatus;
  categoryName: string;
  totalStock: number;
  averageRating?: number;
  reviewCount?: number;
}

export interface ProductDetailResponse {
  id: number;
  name: string;
  slug: string;
  description?: string;
  price: number;
  discountPrice?: number;
  effectivePrice: number;
  weight?: number;
  calories?: number;
  brand?: string;
  origin?: string;
  isOrganic: boolean;
  thumbnail: string;
  status: ProductStatus;
  categoryId: number;
  categoryName: string;
  imageUrls: string[];
  totalStock: number;
  averageRating?: number;
  reviewCount?: number;
  createdAt: string;
  updatedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// REVIEW
// ═══════════════════════════════════════════════════════════════
export interface CreateReviewRequest {
  productId: number;
  rating: number;           // 1-5
  comment: string;
  imageUrls?: string[];
}

export interface UpdateReviewRequest {
  rating?: number;
  comment?: string;
  imageUrls?: string[];
}

export interface VoteReviewRequest {
  helpful: boolean;
}

export interface ReviewResponse {
  id: number;
  userId: number;
  userFullName: string;
  userAvatar?: string;
  productId: number;
  rating: number;
  comment: string;
  imageUrls: string[];
  helpfulCount: number;
  unhelpfulCount: number;
  isHidden: boolean;
  createdAt: string;
  updatedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// SHIPPING
// ═══════════════════════════════════════════════════════════════
export interface LocationItem {
  id: string;
  name: string;
}

export interface CalculateShippingFeeRequest {
  toDistrictId: string;
  toWardCode: string;
  weight?: number;          // default 500g
  length?: number;          // default 20cm
  width?: number;           // default 20cm
  height?: number;          // default 10cm
  insuranceValue?: number;  // VND
  serviceTypeId?: string;   // GHN: "2" (E-commerce)
  coupon?: string;
}

export interface ShippingFeeResponse {
  carrierCode: CarrierCode;
  carrierName: string;
  serviceFee: number;
  insuranceFee: number;
  totalFee: number;
  codFee: number;
  couponDiscount: number;
  expectedDeliveryTime: string;
  serviceId: string;
}

export interface StatusLogEntry {
  status: string;
  statusLabel: string;      // Vietnamese label
  rawStatus: string;        // original carrier status
  updatedAt: string;
}

export interface ShippingTrackingResponse {
  orderId: number;
  carrierCode: CarrierCode;
  carrierName: string;
  externalOrderCode: string;  // tracking code for carrier website
  currentStatus: ShippingStatus;
  currentStatusLabel: string;
  shippingFee: number;
  totalFee: number;
  expectedDeliveryTime?: string;
  failReason?: string;
  statusHistory: StatusLogEntry[];
  createdOnCarrierAt?: string;
  lastSyncAt?: string;
}

// ═══════════════════════════════════════════════════════════════
// USER
// ═══════════════════════════════════════════════════════════════
export interface UpdateProfileRequest {
  fullName?: string;
  phone?: string;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
  reason?: string;
}

export interface UpdateUserRolesRequest {
  roles: RoleName[];
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  avatar?: string;
  avatarPublicId?: string;
  status: UserStatus;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// WISHLIST
// ═══════════════════════════════════════════════════════════════
export interface WishlistResponse {
  id: number;
  productId: number;
  productName: string;
  productSlug: string;
  productThumbnail: string;
  productPrice: number;
  productDiscountPrice?: number;
  productStatus: ProductStatus;
  createdAt: string;
}

// ═══════════════════════════════════════════════════════════════
// UPLOAD (shared)
// ═══════════════════════════════════════════════════════════════
export interface UploadResponse {
  url: string;
  publicId: string;
  format: string;
  bytes: number;
  width: number;
  height: number;
}
```

---

## 8. HTTP ERROR CODES & ERROR HANDLING PATTERNS

The backend returns consistent HTTP status codes. Frontend should handle them as follows:

| HTTP Status | Meaning | When | Frontend Action |
|---|---|---|---|
| **400** | Bad Request | Validation failure, business logic error (e.g., "Không thể hủy đơn hàng đã giao") | Show `response.data.message` as toast. If `response.data.errors` exists (a `Record<string, string>`), highlight individual form fields. |
| **401** | Unauthorized | Invalid/expired JWT, bad credentials, disabled/banned account | Trigger silent token refresh. If refresh fails, redirect to `/login`. |
| **403** | Forbidden | Valid JWT but insufficient role (e.g., USER accessing ADMIN endpoint) | Show "Bạn không có quyền thực hiện hành động này" toast and redirect away. |
| **404** | Not Found | Resource does not exist | Show "Không tìm thấy" page or toast. |
| **429** | Too Many Requests | Rate limit exceeded (register: 5/min, resend OTP: 3/10min) | Show countdown timer or "Vui lòng thử lại sau" message. |
| **500** | Internal Server Error | Unexpected backend error | Show generic error toast. |

### Validation Error Shape (400)
When a form submission fails `@Valid` checks, the error body looks like:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải từ 8-100 ký tự",
    "phone": "Số điện thoại không hợp lệ"
  }
}
```
Map each key in `errors` to the corresponding form field to display inline validation messages.

---

## 9. ROLE-BASED ACCESS CONTROL (RBAC) REFERENCE

Use this table to conditionally render UI elements and protect routes:

| Role | Value in `roles[]` | Can Access |
|---|---|---|
| **Customer** | `ROLE_USER` | Home, Products, Cart, Checkout, My Orders, My Addresses, Wishlist, Reviews, Profile |
| **Farmer** | `ROLE_FARMER` | Everything in Customer + Farm Portal (My Farms, Create Products, Manage Inventory) |
| **Admin** | `ROLE_ADMIN` | Everything + Admin Dashboard (All Orders, All Users, Approve Farms, Manage Coupons, Inventory Reports, Export CSV/Excel) |

### Route Protection Rules:
- `/admin/*` routes → require `ROLE_ADMIN`
- `/farmer/*` routes → require `ROLE_FARMER` or `ROLE_ADMIN`
- `/orders`, `/wishlist`, `/profile`, `/addresses`, `/cart/checkout` → require any authenticated user
- `/products`, `/categories`, `/farms` (public views) → no auth required

---

## 10. ORDER STATE MACHINE & BUSINESS RULES

The order lifecycle follows strict state transitions. Use this to render status badges and available user actions:

```
PENDING ──→ CONFIRMED ──→ PROCESSING ──→ SHIPPING ──→ DELIVERED
  │                                         │              │
  └──→ CANCELLED                            └→ CANCELLED   ├→ REFUND_REQUESTED ──→ REFUNDED
                                                           └→ RETURNED
```

### Allowed User Actions by Status:
| Order Status | Customer Actions | Admin Actions |
|---|---|---|
| `PENDING` | Cancel | Update to CONFIRMED, Cancel |
| `CONFIRMED` | — | Update to PROCESSING |
| `PROCESSING` | — | Update to SHIPPING, Create Shipment |
| `SHIPPING` | — | Update to DELIVERED, Cancel, Sync Tracking |
| `DELIVERED` | Request Refund, Write Review | — |
| `REFUND_REQUESTED` | — | Update to REFUNDED or RETURNED |
| `CANCELLED` / `REFUNDED` / `RETURNED` | — (terminal) | — (terminal) |

### Payment Flow by Method:
| Payment Method | After `POST /orders` | Next Step |
|---|---|---|
| `COD` | Order status = `PENDING` | Admin confirms → `CONFIRMED` |
| `VNPAY` | Order status = `PENDING` | Call `POST /payments/{orderId}/initiate` → redirect to VNPay → IPN confirms → status = `CONFIRMED` |

---

## 11. IMPORTANT VALIDATION RULES SUMMARY

| Field | Rule | Used In |
|---|---|---|
| `email` | Valid email format | Register, Login, Password Reset |
| `password` | 8–100 characters | Register, Change Password |
| `phone` | Vietnamese format: `^(0[3\|5\|7\|8\|9])+([0-9]{8})$` | Register, Address |
| `fullName` | Max 100 chars | Register, Update Profile |
| `otp` | Exactly 6 digits (`\d{6}`) | Verify Email, Password Reset |
| `product.name` | Max 200 chars | Create Product |
| `product.price` | > 0, max 10 integer + 2 decimal | Create/Update Product |
| `thumbnail` / `imageUrls` | Must be Cloudinary URL (`^https://res\.cloudinary\.com/.*`) | Create/Update Product |
| `rating` | 1–5 | Create Review |
| `cart.quantity` | ≥ 1 for add; ≥ 0 for update (0 = remove) | Cart operations |

---

## 12. FRONTEND ROUTING PLAN (SUGGESTED)

```
/                          → Home Page
/login                     → Login (email/password + social buttons)
/register                  → Register
/verify-email              → OTP input screen
/forgot-password           → Password reset flow
/oauth2/callback           → OAuth2 redirect handler (parse ?code=)
/products                  → Product listing with filters
/products/:slug            → Product detail (use slug endpoint)
/categories/:slug          → Products filtered by category
/farms                     → Farm listing
/farms/:farmId             → Farm detail + farm products
/cart                      → Shopping cart
/checkout                  → Checkout (address, shipping, payment)
/payment/result            → VNPay return handler (parse ?status=&txnRef=)
/orders                    → My order history
/orders/:orderId           → Order detail + tracking
/wishlist                  → My wishlist
/profile                   → User profile + avatar
/profile/addresses         → Address management
/profile/change-password   → Change password
/farmer/farms              → [Farmer] My farms
/farmer/products           → [Farmer] My products
/farmer/inventory          → [Farmer] Stock management
/admin/dashboard           → [Admin] Overview
/admin/orders              → [Admin] All orders + filters + export
/admin/users               → [Admin] User management
/admin/farms               → [Admin] Farm approval
/admin/coupons             → [Admin] Coupon management
/admin/reviews             → [Admin] Review moderation
/admin/inventory           → [Admin] Inventory reports
```

### **[END OF AI PROMPT]**
