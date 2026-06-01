import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { UserResponse, UserRole } from '@/types'
import { tokenStorage } from '@/lib/api-client'

interface AuthState {
  user: UserResponse | null
  isAuthenticated: boolean
  isHydrated: boolean

  // Actions
  setUser: (user: UserResponse) => void
  setAuthenticated: (user: UserResponse, token: string) => void
  logout: () => void
  setHydrated: () => void

  // Role helpers
  hasRole: (role: UserRole) => boolean
  isAdmin: () => boolean
  isFarmer: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isHydrated: false,

      setUser: (user) => set({ user, isAuthenticated: true }),

      setAuthenticated: (user, token) => {
        tokenStorage.set(token)
        set({ user, isAuthenticated: true })
      },

      logout: () => {
        tokenStorage.remove()
        set({ user: null, isAuthenticated: false })
      },

      setHydrated: () => set({ isHydrated: true }),

      hasRole: (role) => get().user?.roles?.includes(role) ?? false,
      isAdmin: () => get().user?.roles?.includes('ROLE_ADMIN') ?? false,
      isFarmer: () =>
        (get().user?.roles?.includes('ROLE_FARMER') ||
          get().user?.roles?.includes('ROLE_ADMIN')) ??
        false,
    }),
    {
      name: 'pineapple-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        state?.setHydrated()
      },
    }
  )
)