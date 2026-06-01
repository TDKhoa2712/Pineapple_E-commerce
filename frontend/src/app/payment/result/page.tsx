'use client'

import { useEffect, useState, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui'
import { paymentApi } from '@/services/api'

function PaymentResultContent() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [countdown, setCountdown] = useState(5)
  const [verificationStatus, setVerificationStatus] = useState<'verifying' | 'success' | 'failed'>('verifying')

  const status = searchParams.get('status')
  const txnRef = searchParams.get('txnRef')

  // txnRef is PNP_orderId_timestamp
  const orderIdStr = txnRef ? txnRef.split('_')[1] : null
  const orderId = orderIdStr ? parseInt(orderIdStr) : null

  useEffect(() => {
    if (status !== 'success' || !orderId) {
      setVerificationStatus('failed')
      return
    }

    let attempts = 0
    const maxAttempts = 5
    let isMounted = true

    const checkPaymentStatus = async () => {
      try {
        const res = await paymentApi.getByOrder(orderId)
        if (res.data.status === 'PAID') {
          if (isMounted) setVerificationStatus('success')
        } else if (attempts < maxAttempts) {
          attempts++
          setTimeout(checkPaymentStatus, 1500)
        } else {
          if (isMounted) setVerificationStatus('failed')
        }
      } catch (err) {
        if (attempts < maxAttempts) {
          attempts++
          setTimeout(checkPaymentStatus, 1500)
        } else {
          if (isMounted) setVerificationStatus('failed')
        }
      }
    }

    checkPaymentStatus()

    return () => {
      isMounted = false
    }
  }, [status, orderId])

  useEffect(() => {
    if (verificationStatus !== 'success') return
    const t = setInterval(() => {
      setCountdown((c) => Math.max(0, c - 1))
    }, 1000)
    return () => clearInterval(t)
  }, [verificationStatus])

  useEffect(() => {
    if (verificationStatus === 'success' && countdown === 0) {
      router.push('/orders')
    }
  }, [countdown, verificationStatus, router])

  if (verificationStatus === 'verifying') {
    return (
      <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center p-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.4 }}
          className="bg-white rounded-3xl border border-[var(--color-border)] p-10 max-w-md w-full text-center shadow-[var(--shadow-lg)]"
        >
          <div className="w-20 h-20 bg-amber-50 rounded-full flex items-center justify-center mx-auto mb-6">
            <Loader2 className="w-10 h-10 animate-spin text-[var(--color-gold-600)]" />
          </div>
          <h1 className="text-2xl font-bold text-[var(--color-brown-900)] mb-2"
            style={{ fontFamily: 'var(--font-display)' }}>
            Đang xác thực thanh toán
          </h1>
          <p className="text-[var(--color-text-muted)] text-sm">
            Vui lòng không đóng trình duyệt. Chúng tôi đang kiểm tra trạng thái giao dịch từ VNPay...
          </p>
        </motion.div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center p-6">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4, type: 'spring' }}
        className="bg-white rounded-3xl border border-[var(--color-border)] p-10 max-w-md w-full text-center shadow-[var(--shadow-lg)]"
      >
        {verificationStatus === 'success' ? (
          <>
            <motion.div
              initial={{ scale: 0 }} animate={{ scale: 1 }}
              transition={{ delay: 0.2, type: 'spring', stiffness: 300 }}
              className="w-20 h-20 bg-[var(--color-green-50)] rounded-full flex items-center justify-center mx-auto mb-6"
            >
              <CheckCircle className="w-10 h-10 text-[var(--color-green-600)]" />
            </motion.div>
            <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-2"
              style={{ fontFamily: 'var(--font-display)' }}>
              Thanh toán thành công!
            </h1>
            <p className="text-[var(--color-text-muted)] mb-6">
              Đơn hàng của bạn đã được xác nhận. Chúng tôi sẽ liên hệ sớm nhất!
            </p>
            <div className="bg-[var(--color-green-50)] rounded-xl p-4 mb-6">
              <p className="text-[var(--color-green-700)] text-sm font-medium">
                Tự động chuyển sau {countdown}s...
              </p>
            </div>
            <div className="flex gap-3">
              <Button href="/orders" variant="gold" fullWidth>
                Xem đơn hàng
              </Button>
              <Button href="/products" variant="secondary" fullWidth>
                Tiếp tục mua sắm
              </Button>
            </div>
          </>
        ) : (
          <>
            <div className="w-20 h-20 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-6">
              <XCircle className="w-10 h-10 text-red-500" />
            </div>
            <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-2"
              style={{ fontFamily: 'var(--font-display)' }}>
              Thanh toán thất bại
            </h1>
            <p className="text-[var(--color-text-muted)] mb-8">
              Giao dịch không thành công. Vui lòng thử lại hoặc chọn phương thức khác.
            </p>
            <div className="flex gap-3">
              <Button onClick={() => router.back()} variant="secondary" fullWidth>
                Thử lại
              </Button>
              <Button href="/orders" fullWidth>
                Xem đơn hàng
              </Button>
            </div>
          </>
        )}
      </motion.div>
    </div>
  )
}

export default function PaymentResultPage() {
  return (
    <Suspense fallback={
      <div className="min-h-screen bg-[var(--color-cream)] flex items-center justify-center">
        <Loader2 className="w-10 h-10 animate-spin text-[var(--color-gold-600)]" />
      </div>
    }>
      <PaymentResultContent />
    </Suspense>
  )
}