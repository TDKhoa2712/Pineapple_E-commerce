'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { userApi } from '@/services/api'
import { useAuthStore } from '@/stores/auth-store'
import { queryKeys } from './query-keys'

export function useUpdateProfile() {
  const { setUser } = useAuthStore()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: userApi.updateProfile,
    onSuccess: (res) => {
      setUser(res.data)
      queryClient.invalidateQueries({ queryKey: queryKeys.me })
      toast.success('Đã cập nhật thông tin')
    },
  })
}

export function useUploadAvatar() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (file: File) => userApi.uploadAvatar(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.me })
      toast.success('Đã cập nhật ảnh đại diện')
    },
  })
}

export function useChangePassword() {
  return useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      userApi.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      toast.success('Đã đổi mật khẩu thành công')
    },
    onError: (error: { response?: { data?: { message?: string } } }) => {
      toast.error(error.response?.data?.message || 'Đổi mật khẩu thất bại')
    },
  })
}
