'use client'

import { useEffect, useState } from 'react'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import { authApi, cartApi } from '@/services/api'

export function AppBootstrap({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(false)
  const { isAuthenticated, setUser, logout, isHydrated } = useAuthStore()
  const { setCart } = useCartStore()

  useEffect(() => {
    if (!isHydrated) return

    if (!isAuthenticated) {
      setReady(true)
      return
    }

    let cancelled = false

    ;(async () => {
      try {
        const [meRes, cartRes] = await Promise.all([
          authApi.me(),
          cartApi.getCart(),
        ])
        if (!cancelled) {
          setUser(meRes.data)
          setCart(cartRes.data)
        }
      } catch {
        if (!cancelled) {
          logout()
        }
      } finally {
        if (!cancelled) setReady(true)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [isHydrated]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!ready || !isHydrated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--color-cream)]">
        <div className="flex flex-col items-center gap-3">
          <span className="text-4xl animate-bounce">🍍</span>
          <div className="w-32 h-1 bg-[var(--color-brown-100)] rounded-full overflow-hidden">
            <div className="h-full bg-[var(--color-gold-500)] rounded-full animate-[loading_1s_ease-in-out_infinite]" />
          </div>
        </div>
        <style>{`
          @keyframes loading {
            0% { width: 0%; margin-left: 0; }
            50% { width: 60%; margin-left: 20%; }
            100% { width: 0%; margin-left: 100%; }
          }
        `}</style>
      </div>
    )
  }

  return <>{children}</>
}