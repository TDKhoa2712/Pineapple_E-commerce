'use client'

import { useEffect, Suspense } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion } from 'framer-motion'
import { Mail, Lock, Loader2, AlertTriangle } from 'lucide-react'
import { toast } from 'sonner'
import { loginSchema, type LoginFormData } from '@/lib/validations'
import { useLogin } from '@/hooks'
import { useAuthStore } from '@/stores/auth-store'
import { Button, Input, Divider } from '@/components/ui'

const OAUTH_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
const REDIRECT_URI = `${typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000'}/oauth2/callback`

function LoginContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const { isAuthenticated, isAdmin } = useAuthStore()
  const loginMutation = useLogin()

  const redirectTo = searchParams.get('redirect') || '/'

  // Hiển thị thông báo lỗi OAuth2 nếu redirect về từ backend failure (ví dụ tài khoản bị khoá)
  useEffect(() => {
    const oauthError = searchParams.get('error')
    const oauthMessage = searchParams.get('message')
    if (oauthError) {
      const msg =
        oauthError === 'oauth2' && oauthMessage
          ? decodeURIComponent(oauthMessage)
          : oauthError === 'account_banned'
          ? 'Tài khoản đã bị khoá, không thể đăng nhập'
          : oauthError === 'account_inactive'
          ? 'Tài khoản chưa được kích hoạt'
          : 'Đăng nhập mạng xã hội thất bại. Vui lòng thử lại.'
      toast.error(msg, { icon: <AlertTriangle className="w-4 h-4" /> })
      // Xóa error params khỏi URL
      router.replace('/login')
    }
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (isAuthenticated) {
      const isAdminPath = redirectTo.startsWith('/admin')
      if (isAdminPath && !isAdmin()) {
        toast.error('Tài khoản của bạn không có quyền truy cập trang quản trị')
        router.replace('/')
      } else {
        router.replace(redirectTo)
      }
    }
  }, [isAuthenticated, redirectTo, router, isAdmin])

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({ resolver: zodResolver(loginSchema) })

  const onSubmit = async (data: LoginFormData) => {
    try {
      await loginMutation.mutateAsync(data)
      router.replace(redirectTo)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } }
      const msg = apiErr?.response?.data?.message
      
      // Redirect if account is not verified
      if (msg && (msg.includes('chưa được xác thực') || msg.includes('xác thực email') || msg.includes('chưa kích hoạt'))) {
        toast.error(msg)
        router.push(`/verify-email?email=${encodeURIComponent(data.email)}`)
        return
      }

      const fieldErrors = apiErr?.response?.data?.errors
      if (fieldErrors) {
        Object.entries(fieldErrors).forEach(([k, v]) =>
          setError(k as keyof LoginFormData, { message: v })
        )
      } else {
        toast.error(msg || 'Đăng nhập thất bại')
      }
    }
  }

  const handleGoogleLogin = () => {
    const url = `${OAUTH_BASE}/oauth2/authorization/google?redirect_uri=${encodeURIComponent(REDIRECT_URI)}`
    window.location.href = url
  }

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex">
      {/* Left — decorative */}
      <div className="hidden lg:flex lg:w-[45%] gradient-hero texture-overlay relative overflow-hidden flex-col items-center justify-center p-12">
        <div className="relative z-10 text-center">
          <span className="text-7xl mb-6 block">🍍</span>
          <h2
            className="text-4xl font-bold text-white mb-3"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Pineapple
          </h2>
          <p className="text-[var(--color-brown-200)] text-lg">
            Nông sản tươi sạch từ trang trại
          </p>
          <div className="mt-10 grid grid-cols-2 gap-4 text-left">
            {[
              { icon: '🥦', text: 'Rau hữu cơ' },
              { icon: '🍅', text: 'Trái cây tươi' },
              { icon: '🚚', text: 'Giao hỏa tốc' },
              { icon: '♻️', text: 'Thân thiện môi trường' },
            ].map(({ icon, text }) => (
              <div key={text} className="flex items-center gap-2.5 bg-white/10 rounded-xl p-3">
                <span className="text-xl">{icon}</span>
                <span className="text-sm text-white/90 font-medium">{text}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="absolute -bottom-20 -left-20 w-80 h-80 rounded-full bg-[var(--color-gold-500)]/10" />
        <div className="absolute -top-10 -right-10 w-60 h-60 rounded-full bg-white/5" />
      </div>

      {/* Right — form */}
      <div className="flex-1 flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="w-full max-w-md"
        >
          {/* Mobile logo */}
          <div className="lg:hidden text-center mb-8">
            <Link href="/" className="inline-flex items-center gap-2">
              <span className="text-3xl">🍍</span>
              <span
                className="text-2xl font-bold text-[var(--color-brown-900)]"
                style={{ fontFamily: 'var(--font-display)' }}
              >
                Pineapple
              </span>
            </Link>
          </div>

          <h1
            className="text-3xl font-bold text-[var(--color-brown-900)] mb-1"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Xin chào!
          </h1>
          <p className="text-[var(--color-text-muted)] mb-8">
            Đăng nhập để tiếp tục mua sắm
          </p>

          {/* Social login */}
          <div className="space-y-3 mb-6">
            <button
              onClick={handleGoogleLogin}
              className="w-full h-11 flex items-center justify-center gap-3 bg-white border border-[var(--color-border)] rounded-xl text-sm font-medium text-[var(--color-text)] hover:bg-[var(--color-brown-50)] transition-colors shadow-sm"
            >
              <svg className="w-5 h-5" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
              </svg>
              Tiếp tục với Google
            </button>
          </div>

          <Divider label="hoặc đăng nhập bằng email" />

          {/* Email form */}
          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
            <Input
              label="Email"
              type="email"
              placeholder="ten@email.com"
              leftIcon={<Mail className="w-4 h-4" />}
              error={errors.email?.message}
              {...register('email')}
            />
            <Input
              label="Mật khẩu"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              {...register('password')}
            />

            <div className="flex justify-end">
              <Link
                href="/forgot-password"
                className="text-sm text-[var(--color-gold-600)] hover:underline"
              >
                Quên mật khẩu?
              </Link>
            </div>

            <Button
              type="submit"
              fullWidth
              size="lg"
              loading={isSubmitting || loginMutation.isPending}
            >
              Đăng nhập
            </Button>
          </form>

          <p className="text-center text-sm text-[var(--color-text-muted)] mt-6">
            Chưa có tài khoản?{' '}
            <Link
              href="/register"
              className="text-[var(--color-brown-900)] font-semibold hover:text-[var(--color-gold-600)] transition-colors"
            >
              Đăng ký ngay
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center">
        <Loader2 className="w-10 h-10 animate-spin text-[var(--color-gold-600)]" />
      </div>
    }>
      <LoginContent />
    </Suspense>
  )
}