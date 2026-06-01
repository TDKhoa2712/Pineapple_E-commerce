'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useRouter } from 'next/navigation'
import { authApi, cartApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import { guestCartStorage } from '@/lib/api-client'
import type { LoginRequest, RegisterRequest } from '@/types'
import { queryKeys } from './query-keys'

export function useMe() {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: () => authApi.me(),
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  })
}

export function useLogin() {
  const { setAuthenticated } = useAuthStore()
  const { setCart } = useCartStore()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: async (response) => {
      const { accessToken } = response.data
      
      // Store token first so that subsequent API requests (like authApi.me()) have the Authorization header
      const { tokenStorage } = await import('@/lib/api-client')
      tokenStorage.set(accessToken)

      const meRes = await authApi.me()
      setAuthenticated(meRes.data, accessToken)

      // Smart cart merge
      const guestItems = guestCartStorage.get()
      if (guestItems.length > 0) {
        try {
          const mergeRes = await cartApi.mergeCart({ items: guestItems })
          if (mergeRes.data.skippedItems?.length > 0) {
            mergeRes.data.skippedItems.forEach((item) => {
              toast.warning(`"${item.productName}" không thể thêm vào giỏ: ${item.reason}`)
            })
          }
          guestCartStorage.clear()
        } catch {
          // ignore merge failure
        }
      }

      // Refresh cart — backend returns CartResponse with cartId field
      try {
        const cartRes = await cartApi.getCart()
        setCart(cartRes.data)
      } catch {
        // ignore
      }

      queryClient.invalidateQueries({ queryKey: queryKeys.cart })
      toast.success('Đăng nhập thành công!')
    },
  })
}

export function useLogout() {
  const { logout } = useAuthStore()
  const { clearCart } = useCartStore()
  const queryClient = useQueryClient()
  const router = useRouter()

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => {
      logout()
      clearCart()
      queryClient.clear()
      toast.success('Đã đăng xuất')
      router.push('/')
    },
  })
}

export function useRegister() {
  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
  })
}
