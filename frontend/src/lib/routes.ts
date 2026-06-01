export const ROUTES = {
  HOME: "/",
  PRODUCTS: "/products",
  PRODUCT_DETAIL: (slug: string) => `/products/${slug}`,
  CATEGORIES: (slug: string) => `/categories/${slug}`,
  FARMS: "/farms",
  FARM_DETAIL: (id: number) => `/farms/${id}`,
  CART: "/cart",
  CHECKOUT: "/checkout",
  ORDERS: "/orders",
  ORDER_DETAIL: (id: number) => `/orders/${id}`,
  WISHLIST: "/wishlist",
  PROFILE: "/profile",
  ADDRESSES: "/profile/addresses",
  CHANGE_PASSWORD: "/profile/change-password",

  // Auth
  LOGIN: "/login",
  REGISTER: "/register",
  FORGOT_PASSWORD: "/forgot-password",
  VERIFY_EMAIL: "/verify-email",

  // Admin
  ADMIN: {
    DASHBOARD: "/admin/dashboard",
    ORDERS: "/admin/orders",
    USERS: "/admin/users",
    FARMS: "/admin/farms",
    PRODUCTS: "/admin/products",
    CATEGORIES: "/admin/categories",
    COUPONS: "/admin/coupons",
    REVIEWS: "/admin/reviews",
    INVENTORY: "/admin/inventory",
  },

  // Farmer
  FARMER: {
    FARMS: "/farmer/farms",
  },
} as const;
