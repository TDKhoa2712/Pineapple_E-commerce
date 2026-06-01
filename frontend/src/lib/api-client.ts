import axios, { type AxiosRequestConfig } from 'axios'
import { toast } from 'sonner'
import type { ApiResponse, AuthTokens } from '@/types'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// ─── Token helpers ───────────────────────────────────────────────────────────
const TOKEN_KEY = 'pineapple_access_token'

export const tokenStorage = {
  get: (): string | null => {
    return null
  },
  set: (token: string): void => {
    // No-op: stored in HttpOnly cookie set by backend
  },
  remove: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEY)
    }
  },
}

// ─── Guest cart helpers ──────────────────────────────────────────────────────
const GUEST_CART_KEY = 'pineapple_guest_cart'

export type GuestCartItem = { productId: number; quantity: number }

export const guestCartStorage = {
  get: (): GuestCartItem[] => {
    if (typeof window === 'undefined') return []
    try {
      return JSON.parse(localStorage.getItem(GUEST_CART_KEY) || '[]')
    } catch {
      return []
    }
  },
  set: (items: GuestCartItem[]): void => {
    if (typeof window === 'undefined') return
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items))
  },
  clear: (): void => {
    if (typeof window === 'undefined') return
    localStorage.removeItem(GUEST_CART_KEY)
  },
}

// ─── Request interceptor: attach access token ────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ─── Response interceptor: silent token refresh on 401 ──────────────────────
let isRefreshing = false
let failedQueue: {
  resolve: (token: string) => void
  reject: (error: unknown) => void
}[] = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error)
    else prom.resolve(token!)
  })
  failedQueue = []
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      const serverMessage: string = error.response?.data?.message || ''

      // Nếu tài khoản bị khoá hoặc chưa kích hoạt → không refresh, logout luôn
      const isBanned = serverMessage.includes('bị khoá') || serverMessage.includes('bi khoa')
      const isInactive = serverMessage.includes('chưa được kích hoạt')

      if (isBanned || isInactive) {
        tokenStorage.remove()
        processQueue(error, null)
        const { useAuthStore } = await import('@/stores/auth-store')
        useAuthStore.getState().logout()
        toast.error(serverMessage || 'Tài khoản của bạn không thể đăng nhập.', {
          id: 'account-blocked',
        })
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            if (originalRequest.headers) {
              originalRequest.headers['Authorization'] = `Bearer ${token}`
            }
            return apiClient(originalRequest)
          })
          .catch(Promise.reject)
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Refresh uses HttpOnly cookie automatically (withCredentials: true)
        const { data } = await axios.post<ApiResponse<AuthTokens>>(
          `${BASE_URL}/api/v1/auth/refresh`,
          {},
          { withCredentials: true }
        )
        const newToken = data.data.accessToken
        tokenStorage.set(newToken)
        processQueue(null, newToken)
        if (originalRequest.headers) {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`
        }
        return apiClient(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        tokenStorage.remove()
        // Dynamic import to avoid circular dependency
        const { useAuthStore } = await import('@/stores/auth-store')
        useAuthStore.getState().logout()
        toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', {
          id: 'session-expired',
        })
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Handle other HTTP errors
    const status = error.response?.status
    const message = error.response?.data?.message

    if (status === 403) {
      toast.error('Bạn không có quyền thực hiện hành động này.', { id: 'forbidden' })
    } else if (status === 429) {
      toast.error('Quá nhiều yêu cầu. Vui lòng thử lại sau.', { id: 'rate-limit' })
    } else if (status === 500) {
      toast.error('Lỗi hệ thống. Vui lòng thử lại sau.', { id: 'server-error' })
    } else if (status === 404 && message) {
      // Let callers handle 404 individually
    }

    return Promise.reject(error)
  }
)

export default apiClient