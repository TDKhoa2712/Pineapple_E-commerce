'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion, AnimatePresence } from 'framer-motion'
import { Mail, Lock } from 'lucide-react'
import { toast } from 'sonner'
import { forgotPasswordSchema, resetPasswordSchema, type ForgotPasswordFormData, type ResetPasswordFormData } from '@/lib/validations'
import { authApi } from '@/services/api'
import { Button, Input } from '@/components/ui'

type Step = 'email' | 'otp' | 'password'

export default function ForgotPasswordPage() {
  const router = useRouter()
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center p-6">
      <motion.div className="w-full max-w-md" layout>
        <div className="text-center mb-8">
          <Link href="/"><span className="text-3xl">🍍</span></Link>
          <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mt-4"
            style={{ fontFamily: 'var(--font-display)' }}>
            Quên mật khẩu
          </h1>
        </div>

        {/* Progress */}
        <div className="flex items-center gap-2 mb-8">
          {(['email', 'otp', 'password'] as Step[]).map((s, i) => (
            <div key={s} className="flex items-center flex-1">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all
                ${step === s || (['otp','password'].includes(step) && i === 0) || (step === 'password' && i === 1)
                  ? 'bg-[var(--color-brown-900)] text-white'
                  : 'bg-[var(--color-brown-100)] text-[var(--color-text-muted)]'}`}>
                {i + 1}
              </div>
              {i < 2 && <div className="flex-1 h-0.5 mx-1 bg-[var(--color-border)]" />}
            </div>
          ))}
        </div>

        <AnimatePresence mode="wait">
          {step === 'email' && (
            <EmailStep key="email" onSuccess={(e) => { setEmail(e); setStep('otp') }} />
          )}
          {step === 'otp' && (
            <OtpStep key="otp" email={email} onSuccess={() => setStep('password')} />
          )}
          {step === 'password' && (
            <NewPasswordStep key="password" email={email} onSuccess={() => router.push('/login')} />
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  )
}

function EmailStep({ onSuccess }: { onSuccess: (email: string) => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<ForgotPasswordFormData>({ resolver: zodResolver(forgotPasswordSchema) })

  const onSubmit = async (data: ForgotPasswordFormData) => {
    try {
      await authApi.passwordResetInitiate(data.email)
      toast.success('Mã OTP đã gửi tới email của bạn')
      onSuccess(data.email)
    } catch {
      toast.error('Email không tồn tại trong hệ thống')
    }
  }

  return (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
      <p className="text-[var(--color-text-muted)] mb-6">Nhập email để nhận mã xác nhận</p>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input label="Email" type="email" placeholder="ten@email.com"
          leftIcon={<Mail className="w-4 h-4" />}
          error={errors.email?.message} {...register('email')} />
        <Button type="submit" fullWidth size="lg" loading={isSubmitting}>Gửi mã OTP</Button>
      </form>
      <p className="text-center text-sm mt-4">
        <Link href="/login" className="text-[var(--color-gold-600)] hover:underline">← Quay lại đăng nhập</Link>
      </p>
    </motion.div>
  )
}

function OtpStep({ email, onSuccess }: { email: string; onSuccess: () => void }) {
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)

  const handleVerify = async () => {
    if (otp.length !== 6) { toast.error('Nhập đủ 6 chữ số'); return }
    // We just move to next step; OTP verified on final submit
    onSuccess()
  }

  return (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
      <p className="text-[var(--color-text-muted)] mb-6">
        Mã OTP đã gửi tới <strong>{email}</strong>
      </p>
      <div className="space-y-4">
        <Input label="Mã OTP (6 chữ số)" type="text" inputMode="numeric" maxLength={6}
          placeholder="000000" value={otp} onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))} />
        <Button fullWidth size="lg" onClick={handleVerify} loading={loading}>Xác nhận</Button>
      </div>
    </motion.div>
  )
}

function NewPasswordStep({ email, onSuccess }: { email: string; onSuccess: () => void }) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<ResetPasswordFormData>({ resolver: zodResolver(resetPasswordSchema),
      defaultValues: { email } })

  const onSubmit = async (data: ResetPasswordFormData) => {
    try {
      await authApi.passwordResetConfirm(data)
      toast.success('Đổi mật khẩu thành công!')
      onSuccess()
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      toast.error(e?.response?.data?.message || 'Mã OTP không đúng hoặc đã hết hạn')
    }
  }

  return (
    <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}>
      <p className="text-[var(--color-text-muted)] mb-6">Tạo mật khẩu mới cho tài khoản của bạn</p>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <input type="hidden" {...register('email')} />
        <Input label="Mã OTP" type="text" inputMode="numeric" maxLength={6} placeholder="000000"
          error={errors.otp?.message} {...register('otp')} />
        <Input label="Mật khẩu mới" type="password" placeholder="Ít nhất 8 ký tự"
          leftIcon={<Lock className="w-4 h-4" />}
          error={errors.newPassword?.message} {...register('newPassword')} />
        <Input label="Xác nhận mật khẩu" type="password" placeholder="Nhập lại"
          leftIcon={<Lock className="w-4 h-4" />}
          error={errors.confirmPassword?.message} {...register('confirmPassword')} />
        <Button type="submit" fullWidth size="lg" loading={isSubmitting}>Đặt lại mật khẩu</Button>
      </form>
    </motion.div>
  )
}