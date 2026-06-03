'use client'

import { useState, useRef, useEffect, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { motion } from 'framer-motion'
import { toast } from 'sonner'
import { Mail } from 'lucide-react'
import { authApi } from '@/services/api'
import { Button, Input } from '@/components/ui'
import Link from 'next/link'

function VerifyEmailContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const initialEmail = searchParams.get('email') || ''

  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const [countdown, setCountdown] = useState(0)
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])

  useEffect(() => {
    if (initialEmail) {
      setEmail(initialEmail)
    }
  }, [initialEmail])

  useEffect(() => {
    if (countdown > 0) {
      const t = setTimeout(() => setCountdown((c) => c - 1), 1000)
      return () => clearTimeout(t)
    }
  }, [countdown])

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return
    const next = [...otp]
    next[index] = value.slice(-1)
    setOtp(next)
    if (value && index < 5) inputRefs.current[index + 1]?.focus()
  }

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  const handlePaste = (e: React.ClipboardEvent) => {
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    if (text.length === 6) {
      setOtp(text.split(''))
      inputRefs.current[5]?.focus()
    }
  }

  const handleVerify = async () => {
    if (!email) {
      toast.error('Vui lòng nhập email')
      return
    }
    const code = otp.join('')
    if (code.length !== 6) {
      toast.error('Vui lòng nhập đủ 6 chữ số')
      return
    }
    setLoading(true)
    try {
      await authApi.verifyEmail({ email, otp: code })
      toast.success('Xác nhận email thành công!')
      router.push('/login')
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      toast.error(e?.response?.data?.message || 'Mã OTP không đúng')
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    if (!email) {
      toast.error('Vui lòng nhập email để nhận mã OTP')
      return
    }
    if (countdown > 0) return
    setResending(true)
    try {
      await authApi.resendVerification(email)
      toast.success('Đã gửi lại mã OTP')
      setCountdown(60)
    } catch {
      toast.error('Không thể gửi lại. Vui lòng thử lại sau.')
    } finally {
      setResending(false)
    }
  }

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md text-center"
      >
        <Link href="/" className="inline-flex items-center gap-2 mb-8">
          <span className="text-3xl">🍍</span>
        </Link>

        {initialEmail && (
          <div className="bg-[var(--color-green-50)] border border-[var(--color-green-100)] rounded-2xl p-4 mb-6 flex items-center gap-3">
            <span className="text-2xl">📧</span>
            <div className="text-left">
              <p className="text-sm font-semibold text-[var(--color-green-700)]">Kiểm tra hộp thư</p>
              <p className="text-xs text-[var(--color-text-muted)]">
                Mã OTP đã được gửi tới <strong>{initialEmail}</strong>
              </p>
            </div>
          </div>
        )}

        <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-2"
          style={{ fontFamily: 'var(--font-display)' }}>
          Xác nhận tài khoản
        </h1>
        <p className="text-[var(--color-text-muted)] mb-6">Nhập email và mã xác thực 6 chữ số</p>

        {/* Email input field */}
        <div className="text-left mb-6">
          <Input
            label="Địa chỉ Email"
            type="email"
            placeholder="ten@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading || resending}
            leftIcon={<Mail className="w-4 h-4" />}
          />
        </div>

        {/* OTP inputs */}
        <div className="text-left mb-2">
          <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide mb-1.5">
            Mã xác thực (OTP)
          </label>
        </div>
        <div className="flex justify-center gap-3 mb-8" onPaste={handlePaste}>
          {otp.map((digit, i) => (
            <input
              key={i}
              ref={(el) => { inputRefs.current[i] = el }}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(i, e.target.value)}
              onKeyDown={(e) => handleKeyDown(i, e)}
              className="w-12 h-14 text-center text-xl font-bold bg-white border-2 rounded-xl
                text-[var(--color-brown-900)] transition-all
                focus:outline-none focus:border-[var(--color-gold-500)] focus:ring-2 focus:ring-[var(--color-gold-300)]
                border-[var(--color-border)]"
            />
          ))}
        </div>

        <Button fullWidth size="lg" onClick={handleVerify} loading={loading}>
          Xác nhận
        </Button>

        <div className="mt-4">
          <button
            onClick={handleResend}
            disabled={countdown > 0 || resending}
            className="text-sm text-[var(--color-gold-600)] hover:underline disabled:opacity-50 disabled:no-underline font-medium"
          >
            {countdown > 0 ? `Gửi lại sau ${countdown}s` : 'Gửi lại mã OTP'}
          </button>
        </div>

        <p className="text-sm text-[var(--color-text-muted)] mt-6">
          <Link href="/login" className="text-[var(--color-brown-900)] font-semibold hover:underline">
            ← Quay lại đăng nhập
          </Link>
        </p>
      </motion.div>
    </div>
  )
}

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center">
        <p className="text-[var(--color-text-muted)] text-sm">Đang tải...</p>
      </div>
    }>
      <VerifyEmailContent />
    </Suspense>
  )
}