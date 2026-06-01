'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { farmApi } from '@/services/api'
import { adminInventoryService } from '@/services/admin.service'
import { useAuthStore } from '@/stores/auth-store'
import type { ProductSearchParams } from '@/types'
import { queryKeys } from './query-keys'

export function useFarms(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: queryKeys.farms(params),
    queryFn: () => farmApi.getAll(params),
  })
}

export function useFarm(id: number) {
  return useQuery({
    queryKey: queryKeys.farm(id),
    queryFn: () => farmApi.getById(id),
    enabled: !!id,
  })
}

export function useFarmProducts(farmId: number, params?: ProductSearchParams) {
  return useQuery({
    queryKey: queryKeys.farmProducts(farmId, params),
    queryFn: () => farmApi.getFarmProducts(farmId, params),
    enabled: !!farmId,
  })
}

// ─── Farmer Hooks ───────────────────────────────────────────────
export function useMyFarms() {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: ['my-farms'],
    queryFn: () => farmApi.getMyFarms(),
    enabled: isAuthenticated,
  })
}

export function useCreateFarm() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: import('@/types').CreateFarmRequest) => farmApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-farms'] })
      toast.success('Đã gửi đăng ký nông trại. Vui lòng chờ Admin duyệt.')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Không thể tạo nông trại')
    },
  })
}

export function useUpdateFarm() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: import('@/types').CreateFarmRequest }) =>
      farmApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-farms'] })
      toast.success('Đã cập nhật thông tin nông trại')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Cập nhật thất bại')
    },
  })
}

export function useDeleteFarm() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => farmApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-farms'] })
      toast.success('Đã xóa nông trại')
    },
    onError: () => toast.error('Không thể xóa nông trại'),
  })
}

export function useUploadFarmImage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, file }: { id: number; file: File }) => farmApi.uploadImage(id, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-farms'] })
      toast.success('Đã cập nhật ảnh nông trại')
    },
    onError: () => toast.error('Upload ảnh thất bại'),
  })
}

export function useFarmBatches(
  farmId: number,
  params?: { keyword?: string; page?: number; size?: number; sortBy?: string; sortDirection?: string }
) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: ['farm-batches', farmId, params],
    queryFn: () => farmApi.getFarmBatches(farmId, params),
    enabled: isAuthenticated && !!farmId,
  })
}

export function useAddBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: import('@/types').CreateInventoryBatchRequest) =>
      adminInventoryService.addBatch(payload),
    onSuccess: (_, variables) => {
      if (variables.farmId) {
        queryClient.invalidateQueries({ queryKey: ['farm-batches', variables.farmId] })
      }
      toast.success('Đã thêm lô hàng mới thành công')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Thêm lô hàng thất bại')
    },
  })
}

export function useAdjustBatch() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ batchId, payload }: { batchId: number; payload: { adjustmentQty: number; reason: string } }) =>
      adminInventoryService.adjustBatch(batchId, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['farm-batches'] })
      queryClient.invalidateQueries({ queryKey: ['batch-adjustments', variables.batchId] })
      toast.success('Điều chỉnh tồn kho thành công')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Điều chỉnh thất bại')
    },
  })
}

export function useBatchAdjustments(batchId: number) {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: ['batch-adjustments', batchId],
    queryFn: () => adminInventoryService.getAdjustments(batchId),
    enabled: isAuthenticated && !!batchId,
  })
}

export function useResubmitBatch() {
  const queryClient = useQueryClient()
  const { isAuthenticated } = useAuthStore()
  return useMutation({
    mutationFn: ({ batchId }: { batchId: number }) => farmApi.resubmitBatch(batchId),
    onSuccess: (res) => {
      // refresh farm batches list (all farms)
      queryClient.invalidateQueries({ queryKey: ['farm-batches'] })
      toast.success(res?.message || 'Đã gửi yêu cầu duyệt lại')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Gửi yêu cầu duyệt lại thất bại')
    },
    onMutate: () => {
      if (!isAuthenticated) {
        toast.error('Vui lòng đăng nhập')
      }
    },
  })
}
