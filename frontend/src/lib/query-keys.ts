export const queryKeys = {
  // Auth
  me: ["me"] as const,

  // Users
  users: (params?: Record<string, unknown>) => ["users", params] as const,
  user: (id: number) => ["users", id] as const,

  // Orders
  orders: (params?: Record<string, unknown>) => ["orders", params] as const,
  order: (id: number) => ["orders", id] as const,

  // Farms
  farms: (params?: Record<string, unknown>) => ["farms", params] as const,
  farm: (id: number) => ["farms", id] as const,

  // Coupons
  coupons: (params?: Record<string, unknown>) => ["coupons", params] as const,
  coupon: (id: number) => ["coupons", id] as const,
  couponUsage: (id: number) => ["coupons", id, "usage"] as const,

  // Reviews
  reviews: (params?: Record<string, unknown>) => ["reviews", params] as const,

  // Inventory
  inventory: (params?: Record<string, unknown>) => ["inventory", params] as const,
  inventoryReport: (params?: Record<string, unknown>) => ["inventory", "report", params] as const,
  inventoryExpiring: (days?: number) => ["inventory", "expiring-soon", days] as const,
  inventorySummary: (params?: Record<string, unknown>) => ["inventory", "summary", params] as const,
  inventoryAdjustments: (batchId: number) => ["inventory", "batches", batchId, "adjustments"] as const,

  // Categories
  categories: ["categories"] as const,
  categoriesTree: ["categories", "tree"] as const,

  // Products
  products: (params?: Record<string, unknown>) => ["products", params] as const,
  product: (id: number) => ["products", id] as const,

  // Dashboard
  dashboardStats: ["dashboard", "stats"] as const,
} as const;