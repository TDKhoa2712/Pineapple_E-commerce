'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { orderApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import type { CreateOrderRequest } from '@/types'
import { queryKeys } from './query-keys'

export function useOrders(params?: { page?: number; size?: number; status?: string }) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.orders(params),
    queryFn: () => orderApi.getMyOrders(params),
    enabled: isAuthenticated,
  })
}

export function useOrder(id: number) {
  return useQuery({
    queryKey: queryKeys.order(id),
    queryFn: () => orderApi.getOrder(id),
    enabled: !!id,
  })
}

export function useCreateOrder() {
  const queryClient = useQueryClient()
  const { clearCart } = useCartStore()

  return useMutation({
    mutationFn: (data: CreateOrderRequest) => orderApi.createOrder(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.orders() })
      queryClient.invalidateQueries({ queryKey: queryKeys.cart })
      clearCart()
    },
  })
}

export function useCancelOrder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => orderApi.cancelOrder(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.order(id) })
      queryClient.invalidateQueries({ queryKey: queryKeys.orders() })
      toast.success('Đã huỷ đơn hàng')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Không thể huỷ đơn hàng')
    },
  })
}
