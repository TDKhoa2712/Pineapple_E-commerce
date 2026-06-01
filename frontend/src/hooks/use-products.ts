'use client'

import { useQuery } from '@tanstack/react-query'
import { productApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import type { ProductSearchParams } from '@/types'
import { queryKeys } from './query-keys'

export function useProducts(params?: ProductSearchParams) {
  return useQuery({
    queryKey: queryKeys.products(params),
    queryFn: () => productApi.getProducts(params),
    staleTime: 2 * 60 * 1000,
  })
}

export function useProduct(slug: string) {
  return useQuery({
    queryKey: queryKeys.product(slug),
    queryFn: () => productApi.getBySlug(slug),
    enabled: !!slug,
    staleTime: 5 * 60 * 1000,
  })
}

export function useRelatedProducts(id: number) {
  return useQuery({
    queryKey: queryKeys.relatedProducts(id),
    queryFn: () => productApi.getRelated(id),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  })
}

export function useProductStock(id: number) {
  return useQuery({
    queryKey: queryKeys.productStock(id),
    queryFn: () => productApi.getStock(id),
    enabled: !!id,
    refetchInterval: 30 * 1000, // refresh stock every 30s
  })
}

export function useMyProducts(params?: {
  page?: number
  size?: number
  keyword?: string
  status?: string
  sortBy?: string
  sortDirection?: string
}) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: ['my-products', params],
    queryFn: () => productApi.getMyProducts(params),
    enabled: isAuthenticated,
  })
}
