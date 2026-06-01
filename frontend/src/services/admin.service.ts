import apiClient from "@/lib/api-client";
import type {
  ApiResponse,
  PageResponse,
  OrderResponse,
  UpdateOrderStatusRequest,
  BulkOrderStatusRequest,
  UserResponse,
  UpdateUserStatusRequest,
  UpdateUserRolesRequest,
  AdminResetPasswordRequest,
  AdminUpdateUserRequest,
  FarmResponse,
  RejectFarmRequest,
  CouponResponse,
  CreateCouponRequest,
  UpdateCouponRequest,
  CouponUsageResponse,
  ReviewResponse,
  InventoryBatchResponse,
  InventoryReportResponse,
  StockAdjustmentResponse,
  InventorySummaryResponse,
  CreateInventoryBatchRequest,
  CategoryResponse,
  CreateCategoryRequest,
  ProductSummaryResponse,
  ProductDetailResponse,
  CreateProductRequest,
  UpdateProductRequest,
  ProductQueryParams,
  AdminStatisticsResponse,
  AddressResponse,
  AddressRequest,
  WishlistResponse,
  PaymentResponse,
  ShippingTrackingResponse,
} from "@/types";

// ── Orders ───────────────────────────────────────────────────
export interface OrderQueryParams {
  status?: string;
  paymentMethod?: string;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const adminOrderService = {
  getAll: async (params: OrderQueryParams = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<OrderResponse>>>(
      "/api/v1/orders/admin",
      { params }
    );
    return data.data;
  },

  getById: async (orderId: number) => {
    const { data } = await apiClient.get<ApiResponse<OrderResponse>>(
      `/api/v1/orders/admin/${orderId}`
    );
    return data.data;
  },

  updateStatus: async (orderId: number, payload: UpdateOrderStatusRequest) => {
    const { data } = await apiClient.patch<ApiResponse<OrderResponse>>(
      `/api/v1/orders/admin/${orderId}/status`,
      payload
    );
    return data.data;
  },

  bulkUpdateStatus: async (payload: BulkOrderStatusRequest) => {
    const { data } = await apiClient.post<ApiResponse<number>>(
      "/api/v1/orders/admin/bulk-status",
      payload
    );
    return data.data;
  },

  exportOrders: async (params: {
    status?: string;
    userId?: number;
    paymentMethod?: string;
    from?: string;
    to?: string;
    format?: string;
  } = {}) => {
    const { data } = await apiClient.get(
      "/api/v1/orders/admin/export",
      {
        params,
        responseType: "blob",
      }
    );
    return data;
  },

  getStatistics: async () => {
    const { data } = await apiClient.get<ApiResponse<AdminStatisticsResponse>>(
      "/api/v1/orders/admin/statistics"
    );
    return data.data;
  },

  getPayment: async (orderId: number) => {
    const { data } = await apiClient.get<ApiResponse<PaymentResponse>>(
      `/api/v1/payments/order/${orderId}`
    );
    return data.data;
  },

  getShippingTracking: async (orderId: number) => {
    const { data } = await apiClient.get<ApiResponse<ShippingTrackingResponse>>(
      `/api/v1/shipping/orders/${orderId}/tracking`
    );
    return data.data;
  },

  createShipment: async (orderId: number, carrier?: string) => {
    const { data } = await apiClient.post<ApiResponse<ShippingTrackingResponse>>(
      `/api/v1/shipping/admin/orders/${orderId}/create-shipment`,
      null,
      { params: { carrier } }
    );
    return data.data;
  },

  cancelShipment: async (orderId: number) => {
    await apiClient.post<ApiResponse<void>>(
      `/api/v1/shipping/admin/orders/${orderId}/cancel-shipment`
    );
  },

  syncShipment: async (orderId: number) => {
    const { data } = await apiClient.post<ApiResponse<ShippingTrackingResponse>>(
      `/api/v1/shipping/admin/orders/${orderId}/sync`
    );
    return data.data;
  },
};

// ── Users ────────────────────────────────────────────────────
export interface UserQueryParams {
  keyword?: string;
  status?: string;
  role?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const adminUserService = {
  getAll: async (params: UserQueryParams = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<UserResponse>>>(
      "/api/v1/users",
      { params }
    );
    return data.data;
  },

  updateStatus: async (userId: number, payload: UpdateUserStatusRequest) => {
    const { data } = await apiClient.patch<ApiResponse<UserResponse>>(
      `/api/v1/users/${userId}/status`,
      payload
    );
    return data.data;
  },

  updateRoles: async (userId: number, payload: UpdateUserRolesRequest) => {
    const { data } = await apiClient.put<ApiResponse<UserResponse>>(
      `/api/v1/users/${userId}/roles`,
      payload
    );
    return data.data;
  },

  resetPassword: async (userId: number, payload: AdminResetPasswordRequest) => {
    await apiClient.post<ApiResponse<void>>(
      `/api/v1/users/${userId}/reset-password`,
      payload
    );
  },

  updateUserInfo: async (userId: number, payload: AdminUpdateUserRequest) => {
    const { data } = await apiClient.put<ApiResponse<UserResponse>>(
      `/api/v1/users/${userId}`,
      payload
    );
    return data.data;
  },

  uploadUserAvatar: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await apiClient.post<ApiResponse<UserResponse>>(
      `/api/v1/users/${userId}/avatar`,
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    return data.data;
  },

  getAddresses: async (userId: number) => {
    const { data } = await apiClient.get<ApiResponse<AddressResponse[]>>(
      `/api/v1/users/${userId}/addresses`
    );
    return data.data;
  },

  getWishlist: async (userId: number, params: { page?: number; size?: number } = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<WishlistResponse>>>(
      `/api/v1/users/${userId}/wishlist`,
      { params }
    );
    return data.data;
  },

  deleteAddress: async (userId: number, addressId: number) => {
    await apiClient.delete<ApiResponse<void>>(
      `/api/v1/users/${userId}/addresses/${addressId}`
    );
  },

  setDefaultAddress: async (userId: number, addressId: number) => {
    const { data } = await apiClient.patch<ApiResponse<AddressResponse>>(
      `/api/v1/users/${userId}/addresses/${addressId}/default`
    );
    return data.data;
  },

  addAddress: async (userId: number, payload: AddressRequest) => {
    const { data } = await apiClient.post<ApiResponse<AddressResponse>>(
      `/api/v1/users/${userId}/addresses`,
      payload
    );
    return data.data;
  },

  updateAddress: async (userId: number, addressId: number, payload: Partial<AddressRequest>) => {
    const { data } = await apiClient.put<ApiResponse<AddressResponse>>(
      `/api/v1/users/${userId}/addresses/${addressId}`,
      payload
    );
    return data.data;
  },

  deleteWishlistItem: async (userId: number, productId: number) => {
    await apiClient.delete<ApiResponse<void>>(
      `/api/v1/users/${userId}/wishlist/${productId}`
    );
  },
};

// ── Farms ────────────────────────────────────────────────────
export interface FarmQueryParams {
  status?: string;
  keyword?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const adminFarmService = {
  getAll: async (params: FarmQueryParams = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<FarmResponse>>>(
      "/api/v1/farms/admin",
      { params }
    );
    return data.data;
  },

  getById: async (farmId: number) => {
    const { data } = await apiClient.get<ApiResponse<FarmResponse>>(
      `/api/v1/farms/${farmId}`
    );
    return data.data;
  },

  approve: async (farmId: number) => {
    const { data } = await apiClient.patch<ApiResponse<FarmResponse>>(
      `/api/v1/farms/admin/${farmId}/approve`
    );
    return data.data;
  },

  reject: async (farmId: number, payload: RejectFarmRequest) => {
    const { data } = await apiClient.patch<ApiResponse<FarmResponse>>(
      `/api/v1/farms/admin/${farmId}/reject`,
      payload
    );
    return data.data;
  },

  activate: async (farmId: number) => {
    const { data } = await apiClient.patch<ApiResponse<FarmResponse>>(
      `/api/v1/farms/admin/${farmId}/activate`
    );
    return data.data;
  },

  deactivate: async (farmId: number) => {
    const { data } = await apiClient.patch<ApiResponse<FarmResponse>>(
      `/api/v1/farms/admin/${farmId}/deactivate`
    );
    return data.data;
  },

  delete: async (farmId: number) => {
    await apiClient.delete<ApiResponse<void>>(`/api/v1/farms/${farmId}`);
  },

  getProducts: async (farmId: number, params: { page?: number; size?: number } = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(
      `/api/v1/farms/${farmId}/products`,
      { params }
    );
    return data.data;
  },
};


// ── Coupons ──────────────────────────────────────────────────
export interface CouponQueryParams {
  active?: boolean;
  expired?: boolean;
  type?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
}

export const adminCouponService = {
  getAll: async (params: CouponQueryParams = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<CouponResponse>>>(
      "/api/v1/admin/coupons",
      { params }
    );
    return data.data;
  },

  create: async (payload: CreateCouponRequest) => {
    const { data } = await apiClient.post<ApiResponse<CouponResponse>>(
      "/api/v1/admin/coupons",
      payload
    );
    return data.data;
  },

  update: async (id: number, payload: UpdateCouponRequest) => {
    const { data } = await apiClient.put<ApiResponse<CouponResponse>>(
      `/api/v1/admin/coupons/${id}`,
      payload
    );
    return data.data;
  },

  getUsage: async (id: number) => {
    const { data } = await apiClient.get<ApiResponse<CouponUsageResponse[]>>(
      `/api/v1/admin/coupons/${id}/usage`
    );
    return data.data;
  },
};

// ── Reviews ──────────────────────────────────────────────────
export const adminReviewService = {
  getAll: async (params: {
    page?: number;
    size?: number;
    keyword?: string;
    rating?: number;
    productId?: number;
    userId?: number;
  } = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<ReviewResponse>>>(
      "/api/v1/reviews/admin/all",
      { params }
    );
    return data.data;
  },

  hide: async (reviewId: number) => {
    await apiClient.patch<ApiResponse<void>>(
      `/api/v1/reviews/admin/${reviewId}/hide`
    );
  },

  delete: async (reviewId: number) => {
    await apiClient.delete<ApiResponse<void>>(
      `/api/v1/reviews/admin/${reviewId}`
    );
  },
};

// ── Inventory ────────────────────────────────────────────────
export const adminInventoryService = {
  getBatches: async (params: {
    productId?: number;
    status?: string;
    keyword?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<InventoryBatchResponse>>>(
      "/api/v1/inventory/admin/batches",
      { params }
    );
    return data.data;
  },

  getReport: async (params: { from: string; to: string; groupBy?: string }) => {
    const { data } = await apiClient.get<ApiResponse<InventoryReportResponse>>(
      "/api/v1/inventory/report",
      { params }
    );
    return data.data;
  },

  adjustBatch: async (batchId: number, payload: { adjustmentQty: number; reason: string }) => {
    const { data } = await apiClient.post<ApiResponse<StockAdjustmentResponse>>(
      `/api/v1/inventory/batches/${batchId}/adjust`,
      payload
    );
    return data.data;
  },

  addBatch: async (payload: CreateInventoryBatchRequest) => {
    const { data } = await apiClient.post<ApiResponse<InventoryBatchResponse>>(
      "/api/v1/inventory/batches",
      payload
    );
    return data.data;
  },

  getExpiringSoon: async (days: number = 7) => {
    const { data } = await apiClient.get<ApiResponse<InventoryBatchResponse[]>>(
      "/api/v1/inventory/batches/expiring-soon",
      { params: { days } }
    );
    return data.data;
  },

  getSummary: async (params: {
    keyword?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<InventorySummaryResponse>>>(
      "/api/v1/inventory/summary",
      { params }
    );
    return data.data;
  },

  markExpired: async () => {
    const { data } = await apiClient.post<ApiResponse<{ markedCount: number }>>(
      "/api/v1/inventory/admin/mark-expired"
    );
    return data.data;
  },

  getAdjustments: async (batchId: number) => {
    const { data } = await apiClient.get<ApiResponse<StockAdjustmentResponse[]>>(
      `/api/v1/inventory/batches/${batchId}/adjustments`
    );
    return data.data;
  },

  approveBatch: async (batchId: number) => {
    const { data } = await apiClient.patch<ApiResponse<InventoryBatchResponse>>(
      `/api/v1/inventory/admin/batches/${batchId}/approve`
    );
    return data.data;
  },

  rejectBatch: async (batchId: number, payload: { reason: string }) => {
    const { data } = await apiClient.patch<ApiResponse<InventoryBatchResponse>>(
      `/api/v1/inventory/admin/batches/${batchId}/reject`,
      payload
    );
    return data.data;
  },
};

// ── Categories ───────────────────────────────────────────────
export const adminCategoryService = {
  getAll: async () => {
    const { data } = await apiClient.get<ApiResponse<CategoryResponse[]>>(
      "/api/v1/categories"
    );
    return data.data;
  },

  getTree: async () => {
    const { data } = await apiClient.get<ApiResponse<CategoryResponse[]>>(
      "/api/v1/categories/tree"
    );
    return data.data;
  },

  create: async (payload: CreateCategoryRequest) => {
    const { data } = await apiClient.post<ApiResponse<CategoryResponse>>(
      "/api/v1/categories",
      payload
    );
    return data.data;
  },

  update: async (id: number, payload: CreateCategoryRequest) => {
    const { data } = await apiClient.put<ApiResponse<CategoryResponse>>(
      `/api/v1/categories/${id}`,
      payload
    );
    return data.data;
  },

  delete: async (id: number) => {
    await apiClient.delete<ApiResponse<void>>(`/api/v1/categories/${id}`);
  },
};

// ── Products ─────────────────────────────────────────────────
export const adminProductService = {
  getAll: async (params: ProductQueryParams = {}) => {
    const { data } = await apiClient.get<ApiResponse<PageResponse<ProductSummaryResponse>>>(
      "/api/v1/products/admin",
      { params }
    );
    return data.data;
  },

  getById: async (id: number) => {
    const { data } = await apiClient.get<ApiResponse<ProductDetailResponse>>(
      `/api/v1/products/${id}`
    );
    return data.data;
  },

  create: async (payload: CreateProductRequest) => {
    const { data } = await apiClient.post<ApiResponse<ProductDetailResponse>>(
      "/api/v1/products",
      payload
    );
    return data.data;
  },

  update: async (id: number, payload: UpdateProductRequest) => {
    const { data } = await apiClient.put<ApiResponse<ProductDetailResponse>>(
      `/api/v1/products/${id}`,
      payload
    );
    return data.data;
  },

  delete: async (id: number) => {
    await apiClient.delete<ApiResponse<void>>(`/api/v1/products/${id}`);
  },
};