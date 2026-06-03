// ═══════════════════════════════════════════════════════════════
// ENUMS / CONSTANTS
// ═══════════════════════════════════════════════════════════════
export type RoleName = "ROLE_USER" | "ROLE_ADMIN" | "ROLE_FARMER";
export type UserStatus = "ACTIVE" | "INACTIVE" | "BANNED";
export type AuthProvider = "LOCAL" | "GOOGLE" | "FACEBOOK";

export type ProductStatus =
  | "ACTIVE"
  | "PENDING_DEACTIVATION"
  | "INACTIVE"
  | "OUT_OF_STOCK";

export type FarmStatus =
  | "PENDING_APPROVAL"
  | "PENDING_DEACTIVATION"
  | "PENDING_REACTIVATION"
  | "ACTIVE"
  | "INACTIVE"
  | "REJECTED";

export type BatchStatus =
  | "PENDING_APPROVAL"
  | "AVAILABLE"
  | "REJECTED"
  | "SOLD_OUT"
  | "EXPIRED";

export type OrderStatus =
  | "PENDING"
  | "CONFIRMED"
  | "PROCESSING"
  | "SHIPPING"
  | "DELIVERED"
  | "REFUND_REQUESTED"
  | "REFUNDED"
  | "RETURNED"
  | "CANCELLED";

export type PaymentMethod = "COD" | "VNPAY" | "MOMO" | "BANK_TRANSFER";
export type PaymentStatus = "UNPAID" | "PAID" | "REFUNDED" | "FAILED";
export type PaymentProvider = "COD" | "VNPAY" | "MOMO" | "BANK";

export type CarrierCode = "GHN" | "GHTK";
export type ShippingStatus =
  | "PENDING_PICKUP" | "PICKING_UP" | "PICKED_UP"
  | "IN_TRANSIT" | "AT_WAREHOUSE" | "SORTING"
  | "OUT_FOR_DELIVERY" | "DELIVERY_FAILED" | "DELIVERED"
  | "RETURNING" | "RETURNED"
  | "CANCELLED" | "EXCEPTION" | "LOST" | "DAMAGED" | "UNKNOWN";

export type CouponType = "PERCENTAGE" | "FIXED_AMOUNT";

// ═══════════════════════════════════════════════════════════════
// GENERIC WRAPPERS
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
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ═══════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════
export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
  userId: number;
  email: string;
  fullName: string;
  roles: string[];
  emailVerified: boolean;
  message?: string;
}

// ═══════════════════════════════════════════════════════════════
// USER
// ═══════════════════════════════════════════════════════════════
export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  avatar?: string;
  status: UserStatus;
  roles: string[];
  provider?: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
  reason?: string;
}

export interface UpdateUserRolesRequest {
  roles: RoleName[];
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

export interface AdminUpdateUserRequest {
  fullName?: string;
  phone?: string;
}

// ═══════════════════════════════════════════════════════════════
// ORDER
// ═══════════════════════════════════════════════════════════════
export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  productThumbnail: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  productUnit?: string;
}

export interface OrderResponse {
  id: number;
  userId?: number;
  userEmail?: string;
  userFullName?: string;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  paymentMethod: PaymentMethod;
  shippingAddress: string;
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
  note?: string;
  couponCode?: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateOrderStatusRequest {
  status: OrderStatus;
}

export interface BulkOrderStatusRequest {
  orderIds: number[];
  newStatus: OrderStatus;
}

// ═══════════════════════════════════════════════════════════════
// FARM
// ═══════════════════════════════════════════════════════════════
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

export interface RejectFarmRequest {
  reason: string;
}

export interface CreateFarmRequest {
  name: string;
  location?: string;
  description?: string;
  certificate?: string;
}

// ═══════════════════════════════════════════════════════════════
// COUPON
// ═══════════════════════════════════════════════════════════════
export interface CreateCouponRequest {
  code: string;
  type: CouponType;
  value: number;
  maxDiscountAmount?: number;
  minOrderValue?: number;
  startDate: string;
  expiryDate: string;
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
// REVIEW
// ═══════════════════════════════════════════════════════════════
export interface ReviewResponse {
  id: number;
  userId: number;
  userFullName: string;
  userAvatar?: string;
  productId: number;
  productName?: string;
  rating: number;
  comment: string;
  imageUrls: string[];
  helpfulCount: number;
  unhelpfulCount: number;
  isHidden: boolean;
  userVote?: boolean | null;
  createdAt: string;
  updatedAt: string;
}

// ═══════════════════════════════════════════════════════════════
// INVENTORY
// ═══════════════════════════════════════════════════════════════
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
  rejectionReason?: string;
}

export interface InventoryReportResponse {
  from: string;
  to: string;
  summary: {
    totalBatchesImported: number;
    totalQuantityImported: number;
    totalQuantitySold: number;
    totalBatchesExpired: number;
    totalQuantityExpired: number;
    currentAvailableStock: number;
  };
  details: {
    productId: number;
    productName: string;
    batchesImported: number;
    quantityImported: number;
    quantitySold: number;
    batchesExpired: number;
    quantityExpired: number;
    currentStock: number;
    earliestImport?: string;
    latestImport?: string;
  }[];
  timeline: {
    label: string;
    date: string;
    quantityImported: number;
    quantitySold: number;
  }[];
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
  children?: CategoryResponse[];
}

// ═══════════════════════════════════════════════════════════════
// PRODUCT
// ═══════════════════════════════════════════════════════════════
export interface CreateProductRequest {
  name: string;
  slug?: string;
  description?: string;
  price: number;
  discountPrice?: number;
  weight?: number;
  calories?: number;
  brand?: string;
  origin?: string;
  unit?: string;
  isOrganic?: boolean;
  thumbnail: string;
  categoryId: number;
  imageUrls?: string[];
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
  unit?: string;
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
  categoryId: number;
  categoryName: string;
  totalStock: number;
  averageRating?: number;
  reviewCount?: number;
  unit?: string;
  createdAt: string;
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
  unit?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductQueryParams {
  keyword?: string;
  categoryId?: number;
  farmId?: number;
  inStock?: boolean;
  minPrice?: number;
  maxPrice?: number;
  isOrganic?: boolean;
  status?: ProductStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

// ═══════════════════════════════════════════════════════════════
// INVENTORY (additional)
// ═══════════════════════════════════════════════════════════════
export interface CreateInventoryBatchRequest {
  productId: number;
  farmId?: number;
  batchCode: string;
  quantity: number;
  harvestDate: string;
  expiryDate: string;
  sweetnessLevel?: number;
}

export interface InventorySummaryResponse {
  productId: number;
  productName: string;
  totalStock: number;
  batchCount: number;
}

// ═══════════════════════════════════════════════════════════════
// DASHBOARD (derived/aggregated)
// ═══════════════════════════════════════════════════════════════
export interface DashboardStats {
  totalRevenue: number;
  totalOrders: number;
  totalUsers: number;
  pendingFarms: number;
  revenueChange: number;
  ordersChange: number;
  usersChange: number;
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

// ═══════════════════════════════════════════════════════════════
// USER-FACING TYPES (supplementary)
// ═══════════════════════════════════════════════════════════════
export type UserRole = 'ROLE_USER' | 'ROLE_FARMER' | 'ROLE_ADMIN'

export interface AuthTokens {
  accessToken: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
  phone?: string
}

export interface VerifyEmailRequest {
  email: string
  otp: string
}

export interface PasswordResetConfirmRequest {
  email: string
  otp: string
  newPassword: string
}

export interface OAuth2ExchangeRequest {
  code: string
}

export interface ApiError {
  success: false
  message: string
  errors?: Record<string, string>
  timestamp: string
}

// ── ProductResponse: unified type matching backend ProductSummaryResponse/ProductDetailResponse
export interface ProductResponse {
  id: number
  name: string
  slug: string
  description?: string
  price: number
  discountPrice?: number
  // effectivePrice: min(price, discountPrice) — computed by backend
  effectivePrice?: number
  thumbnail: string
  imageUrls?: string[]
  // unit: đơn vị tính (kg, bó, quả...) — optional vì ProductSummary có thể không có
  unit?: string
  // stockQuantity: alias cho totalStock từ BE
  stockQuantity?: number
  totalStock?: number
  soldCount?: number
  isOrganic: boolean
  status: ProductStatus
  averageRating?: number
  reviewCount?: number
  weight?: number
  calories?: number
  brand?: string
  origin?: string
  categoryId?: number
  categoryName: string
  farmId?: number
  farmName?: string
  createdAt: string
  updatedAt?: string
}

export interface ProductSearchParams {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  farmId?: number
  isOrganic?: boolean
  sort?: 'price_asc' | 'price_desc' | 'newest' | 'rating' | 'sold'
  minPrice?: number
  maxPrice?: number
}

export interface CategoryNode {
  id: number
  name: string
  slug: string
  icon?: string
  imageUrl?: string
  productCount: number
  children?: CategoryNode[]
}

// CartItemResponse matches backend CartItemResponse.java
// BE fields: id, productId, productName, productThumbnail, unitPrice, quantity, subtotal, availableStock
// Added: productSlug (optional, may not be in all BE versions)
export interface CartItemResponse {
  id: number
  productId: number
  productName: string
  productSlug?: string
  productThumbnail: string
  // Backend uses "unitPrice" — keep both for compat
  unitPrice: number
  price?: number       // alias for unitPrice (legacy compat)
  quantity: number
  subtotal: number
  // Backend uses "availableStock"
  availableStock: number
  stockQuantity?: number  // alias for availableStock (legacy compat)
  productStatus?: ProductStatus
  productUnit?: string
  productWeight?: number
}

// CartResponse matches backend CartResponse.java
// BE fields: cartId, items, totalItems, totalAmount
export interface CartResponse {
  // Backend returns "cartId" — map to id for internal use
  cartId?: number
  id?: number
  items: CartItemResponse[]
  totalAmount: number
  totalItems: number
}

export interface CartMergeRequest {
  items: { productId: number; quantity: number }[]
}

// MergeCartResponse matches backend MergeCartResponse.java
export interface CartMergeResponse {
  cart?: CartResponse
  mergedCount: number
  skippedItems: {
    productId: number
    productName: string
    reason: string
    message?: string
    requestedQty?: number
    actualQty?: number
  }[]
}

export interface AddressResponse {
  id: number
  receiverName: string
  phone: string
  province: string
  district: string
  ward: string
  detail: string
  isDefault: boolean
  carrierMetadata?: {
    GHN?: { provinceId: string; districtId: string; wardCode: string }
  }
}

export interface AddressRequest {
  receiverName: string
  phone: string
  province: string
  district: string
  ward: string
  detail: string
  isDefault?: boolean
  carrierMetadata?: AddressResponse['carrierMetadata']
}

export interface ShipmentResponse {
  id: number
  trackingCode?: string
  carrier: string
  estimatedDelivery?: string
  shippedAt?: string
  deliveredAt?: string
  trackingEvents?: { timestamp: string; status: string; location?: string; description: string }[]
}

export interface CreateOrderRequest {
  addressId: number
  paymentMethod: PaymentMethod
  couponCode?: string
  note?: string
}

export interface ReviewRatingResponse {
  averageRating: number
  totalReviews: number
  distribution: Record<1 | 2 | 3 | 4 | 5, number>
}

export interface ReviewEligibilityResponse {
  eligible: boolean
  purchasedQuantity: number
  reviewedCount: number
}

export interface CreateReviewRequest {
  productId: number
  orderId: number
  rating: number
  comment?: string
  images?: { url: string; publicId: string }[]
}

export interface WishlistResponse {
  id: number
  productId: number
  productName: string
  productSlug: string
  productThumbnail: string
  productPrice: number
  productDiscountPrice?: number
  productStatus: ProductStatus
  productUnit?: string
  createdAt: string
}

export interface CouponPreviewRequest {
  couponCode: string
  cartTotal: number
}

export interface CouponPreviewResponse {
  couponCode: string
  discountAmount: number
  newTotal: number
}

export interface PaymentResponse {
  id: number
  orderId: number
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  transactionRef?: string
  paidAt?: string
}

export interface PaymentInitiateResponse {
  paymentUrl: string
  transactionRef: string
}

export interface UploadResponse {
  url: string
  publicId: string
  format: string
  bytes: number
  width: number
  height: number
}

export interface MonthlyRevenue {
  month: string
  revenue: number
  orderCount: number
}

export interface AdminStatisticsResponse {
  totalRevenue: number
  totalOrders: number
  totalUsers: number
  pendingFarms: number
  revenueChangePercentage: number
  orderChangePercentage: number
  monthlyRevenueList: MonthlyRevenue[]
  orderStatusDistribution: Record<string, number>
}

// ── SHIPPING & TRACKING TYPES (Admin) ────────────────────────
export interface ShippingTrackingStatusLog {
  status: string;
  statusLabel: string;
  rawStatus: string;
  updatedAt: string;
}

export interface ShippingTrackingResponse {
  orderId: number;
  carrierCode: string;
  carrierName: string;
  externalOrderCode: string;
  currentStatus: string;
  currentStatusLabel: string;
  shippingFee: number;
  totalFee: number;
  expectedDeliveryTime?: string;
  failReason?: string;
  statusHistory?: ShippingTrackingStatusLog[];
  createdOnCarrierAt?: string;
  lastSyncAt?: string;
}

export interface LocationItem {
  id: string;
  name: string;
  parentId?: string;
}

export interface CalculateShippingFeeRequest {
  toDistrictId: string
  toWardCode: string
  weight?: number
  length?: number
  width?: number
  height?: number
  insuranceValue?: number
  serviceTypeId?: string
  coupon?: string
}

export interface ShippingFeeResponse {
  carrierCode: CarrierCode
  carrierName: string
  serviceFee: number
  insuranceFee: number
  totalFee: number
  codFee: number
  couponDiscount: number
  expectedDeliveryTime?: string
  serviceId?: string
}