import { z } from 'zod'
import { PHONE_REGEX } from '@/lib/utils'

// ─── Auth schemas ─────────────────────────────────────────────────────────────
export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
})

export const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Họ tên ít nhất 2 ký tự').max(100, 'Tối đa 100 ký tự'),
    email: z.string().email('Email không hợp lệ'),
    phone: z
      .string()
      .regex(PHONE_REGEX, 'Số điện thoại không hợp lệ (VD: 0901234567)')
      .optional()
      .or(z.literal('')),
    password: z
      .string()
      .min(8, 'Mật khẩu ít nhất 8 ký tự')
      .max(100, 'Tối đa 100 ký tự'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

export const verifyEmailSchema = z.object({
  email: z.string().email(),
  otp: z.string().regex(/^\d{6}$/, 'OTP phải gồm 6 chữ số'),
})

export const forgotPasswordSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
})

export const resetPasswordSchema = z
  .object({
    email: z.string().email(),
    otp: z.string().regex(/^\d{6}$/, 'OTP phải gồm 6 chữ số'),
    newPassword: z.string().min(8, 'Mật khẩu ít nhất 8 ký tự').max(100),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Nhập mật khẩu hiện tại'),
    newPassword: z.string().min(8, 'Mật khẩu ít nhất 8 ký tự').max(100),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

// ─── Profile schemas ──────────────────────────────────────────────────────────
export const updateProfileSchema = z.object({
  fullName: z.string().min(2, 'Họ tên ít nhất 2 ký tự').max(100),
  phone: z
    .string()
    .regex(PHONE_REGEX, 'Số điện thoại không hợp lệ')
    .optional()
    .or(z.literal('')),
})

// ─── Address schemas ──────────────────────────────────────────────────────────
export const addressSchema = z.object({
  receiverName: z.string().min(2, 'Tên người nhận ít nhất 2 ký tự').max(100),
  phone: z.string().regex(PHONE_REGEX, 'Số điện thoại không hợp lệ'),
  province: z.string().min(1, 'Chọn tỉnh/thành phố'),
  district: z.string().min(1, 'Chọn quận/huyện'),
  ward: z.string().min(1, 'Chọn phường/xã'),
  detail: z.string().min(5, 'Địa chỉ chi tiết ít nhất 5 ký tự').max(200),
  isDefault: z.boolean().optional().default(false),
})

// ─── Checkout schemas ─────────────────────────────────────────────────────────
// NOTE: addressId is NOT part of the form — it's managed via state (activeAddressId)
// Validation of addressId is done manually in onSubmit before calling the API
export const checkoutSchema = z.object({
  paymentMethod: z.enum(['COD', 'VNPAY']),
  couponCode: z.string().optional(),
  note: z.string().max(500).optional(),
})

// ─── Review schemas ───────────────────────────────────────────────────────────
export const reviewSchema = z.object({
  productId: z.number(),
  orderId: z.number(),
  rating: z.number().min(1, 'Vui lòng chọn số sao').max(5),
  comment: z.string().max(1000).optional(),
})

// ─── Types from schemas ───────────────────────────────────────────────────────
export type LoginFormData = z.infer<typeof loginSchema>
export type RegisterFormData = z.infer<typeof registerSchema>
export type VerifyEmailFormData = z.infer<typeof verifyEmailSchema>
export type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>
export type ResetPasswordFormData = z.infer<typeof resetPasswordSchema>
export type ChangePasswordFormData = z.infer<typeof changePasswordSchema>
export type UpdateProfileFormData = z.infer<typeof updateProfileSchema>
export type AddressFormData = z.infer<typeof addressSchema>
export type CheckoutFormData = z.infer<typeof checkoutSchema>
export type ReviewFormData = z.infer<typeof reviewSchema>

// Helper type for checkout (includes runtime-managed addressId)
export type CheckoutSubmitData = CheckoutFormData & { addressId: number }