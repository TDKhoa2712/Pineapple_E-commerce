'use client'

import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { couponApi, paymentApi } from '@/services/api'
import type { CouponPreviewRequest } from '@/types'

export function usePreviewCoupon() {
  return useMutation({
    mutationFn: (data: CouponPreviewRequest) =>
      couponApi.preview(data),
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Mã coupon không hợp lệ')
    },
  })
}

export function useInitiatePayment() {
  return useMutation({
    mutationFn: (orderId: number) => paymentApi.initiate(orderId),
    onSuccess: (res) => {
      window.location.href = res.data.paymentUrl
    },
  })
}
