'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Lock } from 'lucide-react'
import { changePasswordSchema, type ChangePasswordFormData } from '@/lib/validations'
import { useChangePassword } from '@/hooks'
import { Button, Input } from '@/components/ui'

export default function ChangePasswordPage() {
  const changePassword = useChangePassword()

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<ChangePasswordFormData>({ resolver: zodResolver(changePasswordSchema) })

  const onSubmit = async (data: ChangePasswordFormData) => {
    await changePassword.mutateAsync({ currentPassword: data.currentPassword, newPassword: data.newPassword })
    reset()
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10 max-w-2xl">
        <div className="bg-white rounded-2xl border border-[var(--color-border)] p-8">
          <h1 className="text-2xl font-bold text-[var(--color-brown-900)] mb-6"
            style={{ fontFamily: 'var(--font-display)' }}>
            Đổi mật khẩu
          </h1>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 max-w-sm">
            <Input label="Mật khẩu hiện tại" type="password" placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.currentPassword?.message} {...register('currentPassword')} />
            <Input label="Mật khẩu mới" type="password" placeholder="Ít nhất 8 ký tự"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.newPassword?.message} {...register('newPassword')} />
            <Input label="Xác nhận mật khẩu mới" type="password" placeholder="Nhập lại"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.confirmPassword?.message} {...register('confirmPassword')} />
            <Button type="submit" loading={isSubmitting || changePassword.isPending}>
              Cập nhật mật khẩu
            </Button>
          </form>
        </div>
      </div>
    </div>
  )
}