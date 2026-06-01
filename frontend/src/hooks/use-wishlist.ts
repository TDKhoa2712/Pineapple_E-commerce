'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { wishlistApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { queryKeys } from './query-keys'

export function useWishlist(params?: { page?: number }) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.wishlist(params),
    queryFn: () => wishlistApi.getWishlist(params),
    enabled: isAuthenticated,
  })
}

export function useWishlistCheck(productId: number) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.wishlistCheck(productId),
    queryFn: () => wishlistApi.check(productId),
    enabled: isAuthenticated && !!productId,
    staleTime: 60 * 1000,
  })
}

export function useToggleWishlist() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productId: number) => wishlistApi.toggle(productId),
    onSuccess: (res, productId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.wishlistCheck(productId) })
      queryClient.invalidateQueries({ queryKey: ['wishlist'] })
      const wishlisted = res.data
      toast.success(wishlisted ? 'Đã thêm vào yêu thích' : 'Đã xoá khỏi yêu thích')
    },
  })
}
