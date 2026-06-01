'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { reviewApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import type { CreateReviewRequest } from '@/types'
import { queryKeys } from './query-keys'

export function useProductReviews(productId: number, params?: { page?: number }) {
  return useQuery({
    queryKey: queryKeys.reviews(productId, params),
    queryFn: () => reviewApi.getProductReviews(productId, params),
    enabled: !!productId,
  })
}

export function useProductRating(productId: number) {
  return useQuery({
    queryKey: queryKeys.productRating(productId),
    queryFn: () => reviewApi.getProductRating(productId),
    enabled: !!productId,
  })
}

export function useCreateReview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateReviewRequest) => reviewApi.createReview(data),
    onSuccess: (_, data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.reviews(data.productId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.productRating(data.productId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.reviewEligibility(data.productId) })
      toast.success('Đã gửi đánh giá. Cảm ơn bạn!')
    },
  })
}

export function useReviewEligibility(productId: number) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.reviewEligibility(productId),
    queryFn: () => reviewApi.checkEligibility(productId),
    enabled: isAuthenticated && !!productId,
  })
}

export function useVoteReview() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ reviewId, helpful }: { reviewId: number; helpful: boolean }) =>
      reviewApi.voteReview(reviewId, helpful),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] })
      toast.success('Đã ghi nhận phản hồi của bạn!')
    },
    onError: (err: any) => {
      const msg = err.response?.data?.message || 'Có lỗi xảy ra khi thực hiện vote'
      toast.error(msg)
    },
  })
}
