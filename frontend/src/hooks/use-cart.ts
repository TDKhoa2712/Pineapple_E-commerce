'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { cartApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import { guestCartStorage } from '@/lib/api-client'
import { queryKeys } from './query-keys'

export function useCart() {
  const { isAuthenticated } = useAuthStore()
  const { setCart } = useCartStore()

  return useQuery({
    queryKey: queryKeys.cart,
    queryFn: async () => {
      const res = await cartApi.getCart()
      setCart(res.data)
      return res
    },
    enabled: isAuthenticated,
    staleTime: 30 * 1000,
  })
}

export function useAddToCart() {
  const queryClient = useQueryClient()
  const { isAuthenticated } = useAuthStore()

  return useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) => {
      if (!isAuthenticated) {
        // Guest mode: store in localStorage
        const items = guestCartStorage.get()
        const existing = items.find((i) => i.productId === productId)
        if (existing) {
          existing.quantity += quantity
        } else {
          items.push({ productId, quantity })
        }
        guestCartStorage.set(items)
        return Promise.resolve({ data: { success: true, message: 'Added to guest cart', data: null, timestamp: '' } } as never)
      }
      return cartApi.addItem(productId, quantity)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.cart })
      toast.success('Đã thêm vào giỏ hàng')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Không thể thêm vào giỏ hàng')
    },
  })
}

export function useUpdateCartItem() {
  const queryClient = useQueryClient()
  const { setCart } = useCartStore()

  return useMutation({
    mutationFn: ({ cartItemId, quantity }: { cartItemId: number; quantity: number }) =>
      cartApi.updateItem(cartItemId, quantity),
    onSuccess: async (res) => {
      // Backend now returns full CartResponse on update
      if (res.data) {
        setCart(res.data)
      } else {
        const freshCart = await cartApi.getCart()
        setCart(freshCart.data)
      }
      queryClient.invalidateQueries({ queryKey: queryKeys.cart })
    },
  })
}

export function useRemoveCartItem() {
  const queryClient = useQueryClient()
  const { setCart } = useCartStore()

  return useMutation({
    mutationFn: (cartItemId: number) => cartApi.removeItem(cartItemId),
    onSuccess: (res) => {
      if (res.data) {
        setCart(res.data)
      }
      queryClient.invalidateQueries({ queryKey: queryKeys.cart })
      toast.success('Đã xoá sản phẩm khỏi giỏ')
    },
  })
}
