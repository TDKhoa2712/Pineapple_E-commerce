import type { ProductSearchParams } from '@/types'

export const queryKeys = {
  me: ['me'] as const,
  products: (params?: ProductSearchParams) => ['products', params] as const,
  product: (slug: string) => ['product', slug] as const,
  productStock: (id: number) => ['product-stock', id] as const,
  categories: ['categories'] as const,
  cart: ['cart'] as const,
  cartCount: ['cart-count'] as const,
  addresses: ['addresses'] as const,
  orders: (params?: object) => ['orders', params] as const,
  order: (id: number) => ['order', id] as const,
  reviews: (productId: number, params?: object) => ['reviews', productId, params] as const,
  productRating: (productId: number) => ['product-rating', productId] as const,
  reviewEligibility: (productId: number) => ['review-eligibility', productId] as const,
  wishlist: (params?: object) => ['wishlist', params] as const,
  wishlistCheck: (productId: number) => ['wishlist-check', productId] as const,
  relatedProducts: (id: number) => ['products', 'related', id] as const,
  farms: (params?: object) => ['farms', params] as const,
  farm: (id: number) => ['farm', id] as const,
  farmProducts: (farmId: number, params?: object) => ['farm-products', farmId, params] as const,
}
