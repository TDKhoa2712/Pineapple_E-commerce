import { create } from 'zustand'
import type { CartResponse } from '@/types'

interface CartState {
  cart: CartResponse | null
  itemCount: number

  // Actions
  setCart: (cart: CartResponse | null) => void
  clearCart: () => void
}

export const useCartStore = create<CartState>((set) => ({
  cart: null,
  itemCount: 0,

  setCart: (cart) =>
    set({
      cart,
      // Backend may return cartId instead of id — normalize here
      itemCount: cart?.items?.length ?? 0,
    }),

  clearCart: () => set({ cart: null, itemCount: 0 }),
}))