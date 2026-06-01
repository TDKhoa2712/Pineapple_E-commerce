'use client'

import { useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { toast } from 'sonner'
import { authApi, cartApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import { guestCartStorage } from '@/lib/api-client'

function OAuth2CallbackContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { setAuthenticated } = useAuthStore()
  const { setCart } = useCartStore()

  useEffect(() => {
    const code = searchParams.get('code')
    if (!code) {
      toast.error('Đăng nhập thất bại. Vui lòng thử lại.')
      router.replace('/login')
      return
    }

    ;(async () => {
      try {
        const { data: tokenData } = await authApi.oauth2Exchange({ code })
        const { data: userData } = await authApi.me()
        setAuthenticated(userData, tokenData.accessToken)

        // Smart cart merge
        const guestItems = guestCartStorage.get()
        if (guestItems.length > 0) {
          try {
            const mergeRes = await cartApi.mergeCart({ items: guestItems })
            mergeRes.data.skippedItems?.forEach((item) =>
              toast.warning(`"${item.productName}" không thể thêm: ${item.reason}`)
            )
            guestCartStorage.clear()
          } catch { /* ignore */ }
        }

        try {
          const cartRes = await cartApi.getCart()
          setCart(cartRes.data)
        } catch { /* ignore */ }

        toast.success('Đăng nhập thành công!')
        router.replace('/')
      } catch {
        toast.error('Đăng nhập thất bại. Vui lòng thử lại.')
        router.replace('/login')
      }
    })()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center">
      <div className="text-center">
        <span className="text-5xl animate-bounce block mb-4">🍍</span>
        <p className="text-[var(--color-text-muted)]">Đang xử lý đăng nhập...</p>
      </div>
    </div>
  )
}

export default function OAuth2CallbackPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center">
        <div className="text-center">
          <span className="text-5xl animate-bounce block mb-4">🍍</span>
          <p className="text-[var(--color-text-muted)]">Đang xử lý đăng nhập...</p>
        </div>
      </div>
    }>
      <OAuth2CallbackContent />
    </Suspense>
  )
}