'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion } from 'framer-motion'
import { Mail, Lock, User, Phone } from 'lucide-react'
import { toast } from 'sonner'
import { registerSchema, type RegisterFormData } from '@/lib/validations'
import { useRegister } from '@/hooks'
import { Button, Input, Divider } from '@/components/ui'

const OAUTH_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
const REDIRECT_URI = `${typeof window !== 'undefined' ? window.location.origin : 'http://localhost:3000'}/oauth2/callback`

export default function RegisterPage() {
  const router = useRouter()
  const registerMutation = useRegister()

  const {
    register,
    handleSubmit,
    setError,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({ resolver: zodResolver(registerSchema) })

  const onSubmit = async (data: RegisterFormData) => {
    try {
      await registerMutation.mutateAsync({
        email: data.email,
        password: data.password,
        fullName: data.fullName,
        phone: data.phone || undefined,
      })
      toast.success('Đăng ký thành công! Vui lòng xác nhận email.')
      router.push(`/verify-email?email=${encodeURIComponent(data.email)}`)
    } catch (err: unknown) {
      const apiErr = err as { response?: { data?: { message?: string; errors?: Record<string, string> } } }
      const fieldErrors = apiErr?.response?.data?.errors
      if (fieldErrors) {
        Object.entries(fieldErrors).forEach(([k, v]) =>
          setError(k as keyof RegisterFormData, { message: v })
        )
      } else {
        toast.error(apiErr?.response?.data?.message || 'Đăng ký thất bại')
      }
    }
  }

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md"
      >
        <div className="text-center mb-8">
          <Link href="/" className="inline-flex items-center gap-2 mb-6">
            <span className="text-3xl">🍍</span>
            <span className="text-2xl font-bold text-[var(--color-brown-900)]"
              style={{ fontFamily: 'var(--font-display)' }}>
              Pineapple
            </span>
          </Link>
          <h1 className="text-3xl font-bold text-[var(--color-brown-900)]"
            style={{ fontFamily: 'var(--font-display)' }}>
            Tạo tài khoản
          </h1>
          <p className="text-[var(--color-text-muted)] mt-1">Bắt đầu hành trình ăn sạch của bạn</p>
        </div>

        {/* Google */}
        <button
          onClick={() => { window.location.href = `${OAUTH_BASE}/oauth2/authorization/google?redirect_uri=${encodeURIComponent(REDIRECT_URI)}` }}
          className="w-full h-11 flex items-center justify-center gap-3 bg-white border border-[var(--color-border)] rounded-xl text-sm font-medium hover:bg-[var(--color-brown-50)] transition-colors shadow-sm mb-6"
        >
          <svg className="w-5 h-5" viewBox="0 0 24 24">
            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
          </svg>
          Đăng ký với Google
        </button>

        <Divider label="hoặc đăng ký bằng email" />

        <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
          <Input label="Họ và tên" placeholder="Nguyễn Văn A"
            leftIcon={<User className="w-4 h-4" />}
            error={errors.fullName?.message} {...register('fullName')} />
          <Input label="Email" type="email" placeholder="ten@email.com"
            leftIcon={<Mail className="w-4 h-4" />}
            error={errors.email?.message} {...register('email')} />
          <Input label="Số điện thoại (tuỳ chọn)" type="tel" placeholder="0901234567"
            leftIcon={<Phone className="w-4 h-4" />}
            error={errors.phone?.message} {...register('phone')} />
          <Input label="Mật khẩu" type="password" placeholder="Ít nhất 8 ký tự"
            leftIcon={<Lock className="w-4 h-4" />}
            error={errors.password?.message} {...register('password')} />
          <Input label="Xác nhận mật khẩu" type="password" placeholder="Nhập lại mật khẩu"
            leftIcon={<Lock className="w-4 h-4" />}
            error={errors.confirmPassword?.message} {...register('confirmPassword')} />

          <Button type="submit" fullWidth size="lg" loading={isSubmitting || registerMutation.isPending}>
            Tạo tài khoản
          </Button>
        </form>

        <p className="text-center text-sm text-[var(--color-text-muted)] mt-6">
          Đã có tài khoản?{' '}
          <Link href="/login" className="text-[var(--color-brown-900)] font-semibold hover:text-[var(--color-gold-600)]">
            Đăng nhập
          </Link>
        </p>
      </motion.div>
    </div>
  )
}