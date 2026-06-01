'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { addressApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import type { AddressRequest } from '@/types'
import { queryKeys } from './query-keys'

export function useAddresses() {
  const { isAuthenticated } = useAuthStore()
  return useQuery({
    queryKey: queryKeys.addresses,
    queryFn: () => addressApi.getAll(),
    enabled: isAuthenticated,
  })
}

export function useCreateAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: AddressRequest) => addressApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.addresses })
      toast.success('Đã thêm địa chỉ mới')
    },
  })
}

export function useUpdateAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<AddressRequest> }) =>
      addressApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.addresses })
      toast.success('Đã cập nhật địa chỉ')
    },
  })
}

export function useDeleteAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => addressApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.addresses })
      toast.success('Đã xoá địa chỉ')
    },
  })
}

export function useSetDefaultAddress() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => addressApi.setDefault(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.addresses })
      toast.success('Đã đặt làm địa chỉ mặc định')
    },
  })
}
