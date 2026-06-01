'use client'

import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import Image from 'next/image'
import Link from 'next/link'
import { Camera, User, Phone, MapPin, Lock, Package, Heart, Tractor } from 'lucide-react'
import { updateProfileSchema, type UpdateProfileFormData } from '@/lib/validations'
import { useAuthStore } from '@/stores/auth-store'
import { useUpdateProfile, useUploadAvatar } from '@/hooks'
import { Button, Input, Badge } from '@/components/ui'
import { getInitials } from '@/lib/utils'

export default function ProfilePage() {
  const { user } = useAuthStore()
  const updateProfile = useUpdateProfile()
  const uploadAvatar = useUploadAvatar()
  const fileRef = useRef<HTMLInputElement>(null)

  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<UpdateProfileFormData>({
      resolver: zodResolver(updateProfileSchema),
      values: { fullName: user?.fullName ?? '', phone: user?.phone ?? '' },
    })

  const onSubmit = async (data: UpdateProfileFormData) => {
    await updateProfile.mutateAsync(data)
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    await uploadAvatar.mutateAsync(file)
  }

  const navLinks = [
    { href: '/profile', icon: <User className="w-4 h-4" />, label: 'Thông tin cá nhân', active: true },
    { href: '/profile/addresses', icon: <MapPin className="w-4 h-4" />, label: 'Địa chỉ của tôi' },
    { href: '/profile/change-password', icon: <Lock className="w-4 h-4" />, label: 'Đổi mật khẩu' },
    { href: '/orders', icon: <Package className="w-4 h-4" />, label: 'Đơn hàng' },
    { href: '/wishlist', icon: <Heart className="w-4 h-4" />, label: 'Yêu thích' },
    { href: '/my-farms', icon: <Tractor className="w-4 h-4" />, label: 'Nông trại của tôi' },
  ]

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10">
        <div className="grid grid-cols-1 md:grid-cols-[240px_1fr] gap-8">
          {/* Sidebar nav */}
          <div className="space-y-1">
            {/* Avatar */}
            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-4 text-center">
              <div className="relative inline-block mb-3">
                <div className="w-20 h-20 rounded-full overflow-hidden bg-[var(--color-gold-500)] flex items-center justify-center mx-auto">
                  {user?.avatar ? (
                    <Image src={user.avatar} alt={user.fullName} width={80} height={80} className="object-cover" />
                  ) : (
                    <span className="text-2xl font-bold text-[var(--color-brown-900)]">
                      {getInitials(user?.fullName ?? 'U')}
                    </span>
                  )}
                </div>
                <button onClick={() => fileRef.current?.click()}
                  className="absolute bottom-0 right-0 w-7 h-7 bg-[var(--color-brown-900)] text-white rounded-full flex items-center justify-center hover:bg-[var(--color-brown-700)] transition-colors">
                  <Camera className="w-3.5 h-3.5" />
                </button>
                <input ref={fileRef} type="file" accept="image/*" className="sr-only" onChange={handleAvatarChange} />
              </div>
              <p className="font-bold text-[var(--color-brown-900)] text-sm">{user?.fullName}</p>
              <p className="text-xs text-[var(--color-text-muted)] truncate">{user?.email}</p>
              {user?.roles?.map((role) => (
                <Badge key={role} variant={role === 'ROLE_ADMIN' ? 'danger' : role === 'ROLE_FARMER' ? 'organic' : 'default'} size="sm" className="mt-2">
                  {role === 'ROLE_ADMIN' ? 'Admin' : role === 'ROLE_FARMER' ? 'Farmer' : 'Khách hàng'}
                </Badge>
              ))}
            </div>

            {navLinks.map((link) => (
              <Link key={link.href} href={link.href}
                className={`flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors
                  ${link.active
                    ? 'bg-[var(--color-brown-900)] text-white'
                    : 'text-[var(--color-text-muted)] hover:bg-[var(--color-brown-50)] hover:text-[var(--color-text)]'}`}>
                {link.icon}
                {link.label}
              </Link>
            ))}
          </div>

          {/* Main content */}
          <div className="bg-white rounded-2xl border border-[var(--color-border)] p-8">
            <h1 className="text-2xl font-bold text-[var(--color-brown-900)] mb-6"
              style={{ fontFamily: 'var(--font-display)' }}>
              Thông tin cá nhân
            </h1>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 max-w-md">
              <Input label="Email" type="email" value={user?.email ?? ''} disabled
                hint="Email không thể thay đổi" />
              <Input label="Họ và tên" error={errors.fullName?.message}
                leftIcon={<User className="w-4 h-4" />}
                {...register('fullName')} />
              <Input label="Số điện thoại" error={errors.phone?.message}
                leftIcon={<Phone className="w-4 h-4" />}
                {...register('phone')} />

              <div>
                <p className="text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide mb-1">
                  Đăng nhập qua
                </p>
                <p className="text-sm capitalize font-medium">{user?.provider?.toLowerCase()}</p>
              </div>

              <Button type="submit" loading={isSubmitting || updateProfile.isPending}>
                Lưu thay đổi
              </Button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}