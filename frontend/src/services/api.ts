import apiClient from '@/lib/api-client'
import type {
  ApiResponse,
  PageResponse,
  UserResponse,
  LoginRequest,
  RegisterRequest,
  VerifyEmailRequest,
  PasswordResetConfirmRequest,
  OAuth2ExchangeRequest,
  AuthTokens,
  ProductResponse,
  ProductSearchParams,
  CategoryNode,
  CartResponse,
  CartMergeRequest,
  CartMergeResponse,
  AddressResponse,
  AddressRequest,
  OrderResponse,
  CreateOrderRequest,
  ReviewResponse,
  ReviewRatingResponse,
  ReviewEligibilityResponse,
  CreateReviewRequest,
  WishlistResponse,
  FarmResponse,
  InventoryBatchResponse,
  CreateFarmRequest,
  CouponPreviewRequest,
  CouponPreviewResponse,
  PaymentResponse,
  PaymentInitiateResponse,
  UploadResponse,
  AdminStatisticsResponse,
  LocationItem,
  CalculateShippingFeeRequest,
  ShippingFeeResponse,
} from '@/types'

type AR<T> = Promise<ApiResponse<T>>
type PAR<T> = Promise<ApiResponse<PageResponse<T>>>

// ─── Auth ────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (data: RegisterRequest): AR<void> =>
    apiClient.post('/api/v1/auth/register', data).then((r) => r.data),

  verifyEmail: (data: VerifyEmailRequest): AR<void> =>
    apiClient.post('/api/v1/auth/verify-email', data).then((r) => r.data),

  resendVerification: (email: string): AR<void> =>
    apiClient.post('/api/v1/auth/resend-verification', { email }).then((r) => r.data),

  login: (data: LoginRequest): AR<AuthTokens> =>
    apiClient.post('/api/v1/auth/login', data).then((r) => r.data),

  oauth2Exchange: (data: OAuth2ExchangeRequest): AR<AuthTokens> =>
    apiClient.post('/api/v1/auth/oauth2/exchange', data).then((r) => r.data),

  logout: (): AR<void> =>
    apiClient.post('/api/v1/auth/logout').then((r) => r.data),

  me: (): AR<UserResponse> =>
    apiClient.get('/api/v1/auth/me').then((r) => r.data),

  passwordResetInitiate: (email: string): AR<void> =>
    apiClient.post('/api/v1/auth/password-reset/initiate', { email }).then((r) => r.data),

  passwordResetConfirm: (data: PasswordResetConfirmRequest): AR<void> =>
    apiClient.post('/api/v1/auth/password-reset/confirm', data).then((r) => r.data),
}

// ─── Products ─────────────────────────────────────────────────────────────────
export const productApi = {
  getProducts: (params?: ProductSearchParams): PAR<ProductResponse> => {
    const { sort, ...rest } = params || {}
    let sortBy: string | undefined = sort
    if (sort === 'sold') {
      sortBy = 'best_seller'
    }
    const queryParams = {
      ...rest,
      sortBy
    }
    return apiClient.get('/api/v1/products', { params: queryParams }).then((r) => r.data)
  },

  getMyProducts: (params?: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    sortBy?: string
    sortDirection?: string
  }): PAR<ProductResponse> =>
    apiClient.get('/api/v1/products/my', { params }).then((r) => r.data),

  getBySlug: (slug: string): AR<ProductResponse> =>
    apiClient.get(`/api/v1/products/slug/${slug}`).then((r) => r.data),

  getById: (id: number): AR<ProductResponse> =>
    apiClient.get(`/api/v1/products/${id}`).then((r) => r.data),

  getRelated: (id: number): AR<ProductResponse[]> =>
    apiClient.get(`/api/v1/products/${id}/related`).then((r) => r.data),

  getStock: (id: number): AR<{ quantity: number }> =>
    apiClient.get(`/api/v1/products/${id}/stock`).then((r) => r.data),
}

// ─── Categories ───────────────────────────────────────────────────────────────
export const categoryApi = {
  getTree: (): AR<CategoryNode[]> =>
    apiClient.get('/api/v1/categories/tree').then((r) => r.data),

  getAll: (): AR<CategoryNode[]> =>
    apiClient.get('/api/v1/categories').then((r) => r.data),

  getBySlug: (slug: string): AR<CategoryNode> =>
    apiClient.get(`/api/v1/categories/slug/${slug}`).then((r) => r.data),
}

// ─── Cart ─────────────────────────────────────────────────────────────────────
// NOTE: Backend endpoint is /api/v1/cart (no trailing slash for GET)
export const cartApi = {
  getCart: (): AR<CartResponse> =>
    apiClient.get('/api/v1/cart').then((r) => r.data),

  // Backend returns { "itemCount": N } — mapped here
  getCount: (): AR<{ itemCount: number }> =>
    apiClient.get('/api/v1/cart/count').then((r) => r.data),

  // Backend: POST /api/v1/cart/items → returns CartResponse (full cart, not single item)
  addItem: (productId: number, quantity: number): AR<CartResponse> =>
    apiClient.post('/api/v1/cart/items', { productId, quantity }).then((r) => r.data),

  // Backend: PUT /api/v1/cart/items/{id} → returns CartResponse
  updateItem: (cartItemId: number, quantity: number): AR<CartResponse> =>
    apiClient.put(`/api/v1/cart/items/${cartItemId}`, { quantity }).then((r) => r.data),

  // Backend: DELETE /api/v1/cart/items/{id} → returns CartResponse
  removeItem: (cartItemId: number): AR<CartResponse> =>
    apiClient.delete(`/api/v1/cart/items/${cartItemId}`).then((r) => r.data),

  clearCart: (): AR<void> =>
    apiClient.delete('/api/v1/cart').then((r) => r.data),

  validateCart: (): AR<{ valid: boolean; issues: string[] }> =>
    apiClient.post('/api/v1/cart/validate').then((r) => r.data),

  mergeCart: (data: CartMergeRequest): AR<CartMergeResponse> =>
    apiClient.post('/api/v1/cart/merge', data).then((r) => r.data),
}

// ─── Addresses ────────────────────────────────────────────────────────────────
export const addressApi = {
  getAll: (): AR<AddressResponse[]> =>
    apiClient.get('/api/v1/addresses').then((r) => r.data),

  create: (data: AddressRequest): AR<AddressResponse> =>
    apiClient.post('/api/v1/addresses', data).then((r) => r.data),

  update: (id: number, data: Partial<AddressRequest>): AR<AddressResponse> =>
    apiClient.put(`/api/v1/addresses/${id}`, data).then((r) => r.data),

  setDefault: (id: number): AR<void> =>
    apiClient.patch(`/api/v1/addresses/${id}/default`).then((r) => r.data),

  delete: (id: number): AR<void> =>
    apiClient.delete(`/api/v1/addresses/${id}`).then((r) => r.data),
}

// ─── Orders ───────────────────────────────────────────────────────────────────
export const orderApi = {
  createOrder: (data: CreateOrderRequest): AR<OrderResponse> =>
    apiClient.post('/api/v1/orders', data).then((r) => r.data),

  getMyOrders: (params?: { page?: number; size?: number; status?: string }): PAR<OrderResponse> =>
    apiClient.get('/api/v1/orders/my', { params }).then((r) => r.data),

  getOrder: (id: number): AR<OrderResponse> =>
    apiClient.get(`/api/v1/orders/${id}`).then((r) => r.data),

  // FIX: Backend uses POST (not PATCH) for cancel
  cancelOrder: (id: number): AR<OrderResponse> =>
    apiClient.post(`/api/v1/orders/${id}/cancel`).then((r) => r.data),

  // FIX: Backend path is /request-refund (not /refund-request), method is POST (not PATCH)
  requestRefund: (id: number): AR<OrderResponse> =>
    apiClient.post(`/api/v1/orders/${id}/request-refund`).then((r) => r.data),
}

// ─── Reviews ──────────────────────────────────────────────────────────────────
export const reviewApi = {
  getProductReviews: (productId: number, params?: { page?: number; size?: number }): PAR<ReviewResponse> =>
    apiClient.get(`/api/v1/reviews/product/${productId}`, { params }).then((r) => r.data),

  getProductRating: (productId: number): AR<ReviewRatingResponse> =>
    apiClient.get(`/api/v1/reviews/product/${productId}/rating`).then((r) => r.data),

  createReview: (data: CreateReviewRequest): AR<ReviewResponse> =>
    apiClient.post('/api/v1/reviews', data).then((r) => r.data),

  deleteReview: (reviewId: number): AR<void> =>
    apiClient.delete(`/api/v1/reviews/${reviewId}`).then((r) => r.data),

  checkEligibility: (productId: number): AR<ReviewEligibilityResponse> =>
    apiClient.get(`/api/v1/reviews/product/${productId}/eligible`).then((r) => r.data),

  voteReview: (reviewId: number, helpful: boolean): AR<void> =>
    apiClient.post(`/api/v1/reviews/${reviewId}/vote`, { helpful }).then((r) => r.data),
}

// ─── Wishlist ─────────────────────────────────────────────────────────────────
export const wishlistApi = {
  getWishlist: (params?: { page?: number; size?: number }): PAR<WishlistResponse> =>
    apiClient.get('/api/v1/wishlist', { params }).then((r) => r.data),

  toggle: (productId: number): AR<boolean> =>
    apiClient.post(`/api/v1/wishlist/${productId}`).then((r) => r.data),

  check: (productId: number): AR<boolean> =>
    apiClient.get(`/api/v1/wishlist/${productId}/check`).then((r) => r.data),
}

// ─── Farms ────────────────────────────────────────────────────────────────────
export const farmApi = {
  getAll: (params?: { page?: number; size?: number }): PAR<FarmResponse> =>
    apiClient.get('/api/v1/farms', { params }).then((r) => r.data),

  getById: (id: number): AR<FarmResponse> =>
    apiClient.get(`/api/v1/farms/${id}`).then((r) => r.data),

  getFarmProducts: (farmId: number, params?: ProductSearchParams): PAR<ProductResponse> =>
    apiClient.get(`/api/v1/farms/${farmId}/products`, { params }).then((r) => r.data),

  getFarmBatches: (
    farmId: number,
    params?: { keyword?: string; page?: number; size?: number; sortBy?: string; sortDirection?: string }
  ): PAR<InventoryBatchResponse> =>
    apiClient.get(`/api/v1/inventory/farms/${farmId}/batches`, { params }).then((r) => r.data),

  // Farmer: gửi yêu cầu duyệt lại lô bị từ chối
  resubmitBatch: (batchId: number): AR<InventoryBatchResponse> =>
    apiClient.patch(`/api/v1/inventory/batches/${batchId}/resubmit`).then((r) => r.data),

  // ─── Farmer-only methods ──────────────────────────────────────
  getMyFarms: (): AR<FarmResponse[]> =>
    apiClient.get('/api/v1/farms/my').then((r) => r.data),

  create: (data: CreateFarmRequest): AR<FarmResponse> =>
    apiClient.post('/api/v1/farms', data).then((r) => r.data),

  update: (id: number, data: CreateFarmRequest): AR<FarmResponse> =>
    apiClient.put(`/api/v1/farms/${id}`, data).then((r) => r.data),

  uploadImage: (id: number, file: File): AR<FarmResponse> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiClient
      .post(`/api/v1/farms/${id}/image`, fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  delete: (id: number): AR<void> =>
    apiClient.delete(`/api/v1/farms/${id}`).then((r) => r.data),
}

// ─── Coupons ──────────────────────────────────────────────────────────────────
export const couponApi = {
  preview: (data: CouponPreviewRequest): AR<CouponPreviewResponse> =>
    apiClient.post('/api/v1/coupons/preview', data).then((r) => r.data),
}

// ─── Payments ─────────────────────────────────────────────────────────────────
export const paymentApi = {
  initiate: (orderId: number): AR<PaymentInitiateResponse> =>
    apiClient.post(`/api/v1/payments/${orderId}/initiate`).then((r) => r.data),

  getByOrder: (orderId: number): AR<PaymentResponse> =>
    apiClient.get(`/api/v1/payments/order/${orderId}`).then((r) => r.data),
}

// ─── Upload ───────────────────────────────────────────────────────────────────
export const uploadApi = {
  uploadImage: (file: File, folder: 'PRODUCT' | 'CATEGORY' | 'FARM' | 'REVIEW' | 'AVATAR' = 'PRODUCT'): AR<UploadResponse> => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('folder', folder)
    return apiClient
      .post('/api/v1/upload/image', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },
}

// ─── Users (profile) ──────────────────────────────────────────────────────────
export const userApi = {
  updateProfile: (data: Partial<Pick<UserResponse, 'fullName' | 'phone'>>): AR<UserResponse> =>
    apiClient.put('/api/v1/users/me', data).then((r) => r.data),

  uploadAvatar: (file: File): AR<UploadResponse> => {
    const fd = new FormData()
    fd.append('file', file)
    return apiClient
      .post('/api/v1/users/me/avatar', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data)
  },

  changePassword: (currentPassword: string, newPassword: string): AR<void> =>
    apiClient.post('/api/v1/users/me/change-password', { currentPassword, newPassword }).then((r) => r.data),
}

// ─── Shipping ────────────────────────────────────────────────────────────────
export const shippingApi = {
  getProvinces: (carrier?: string): AR<LocationItem[]> =>
    apiClient.get('/api/v1/shipping/provinces', { params: { carrier } }).then((r) => r.data),

  getDistricts: (provinceId: string, carrier?: string): AR<LocationItem[]> =>
    apiClient.get('/api/v1/shipping/districts', { params: { provinceId, carrier } }).then((r) => r.data),

  getWards: (districtId: string, carrier?: string): AR<LocationItem[]> =>
    apiClient.get('/api/v1/shipping/wards', { params: { districtId, carrier } }).then((r) => r.data),

  calculateFee: (data: CalculateShippingFeeRequest, carrier?: string): AR<ShippingFeeResponse> =>
    apiClient.post('/api/v1/shipping/calculate-fee', data, { params: { carrier } }).then((r) => r.data),
}