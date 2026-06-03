'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion } from 'framer-motion'
import { MapPin, Plus, CreditCard, Banknote, Tag, FileText } from 'lucide-react'
import { toast } from 'sonner'
import Image from 'next/image'
import {
  useAddresses, useCreateAddress, useCart, useCreateOrder,
  useInitiatePayment, usePreviewCoupon, useProvinces, useDistricts, useWards,
  useCalculateShippingFee
} from '@/hooks'
import { useCartStore } from '@/stores/cart-store'
import { checkoutSchema, addressSchema, type CheckoutFormData, type AddressFormData } from '@/lib/validations'
import { Button, Input, EmptyState } from '@/components/ui'
import { formatPrice } from '@/lib/utils'
import type { AddressResponse } from '@/types'

export default function CheckoutPage() {
  const router = useRouter()
  const { cart } = useCartStore()
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [couponCode, setCouponCode] = useState('')
  const [discount, setDiscount] = useState(0)
  const [shippingFee, setShippingFee] = useState(0)
  const [isLoadingShippingFee, setIsLoadingShippingFee] = useState(false)

  const calculateShippingFee = useCalculateShippingFee()

  const [selectedProvinceId, setSelectedProvinceId] = useState<string | null>(null)
  const [selectedDistrictId, setSelectedDistrictId] = useState<string | null>(null)
  const [selectedWardCode, setSelectedWardCode] = useState<string | null>(null)

  const { data: provincesRes } = useProvinces()
  const provinces = provincesRes?.data ?? []

  const { data: districtsRes } = useDistricts(selectedProvinceId)
  const districts = districtsRes?.data ?? []

  const { data: wardsRes } = useWards(selectedDistrictId)
  const wards = wardsRes?.data ?? []

  const selectClassName = "w-full h-11 px-4 bg-white border border-[var(--color-border)] rounded-xl text-sm text-[var(--color-text)] focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)] focus:ring-offset-0 transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed";

  const { data: addressesRes } = useAddresses()
  const createAddress = useCreateAddress()
  const createOrder = useCreateOrder()
  const initiatePayment = useInitiatePayment()
  const previewCoupon = usePreviewCoupon()

  const addresses = addressesRes?.data ?? []
  const defaultAddr = addresses.find((a) => a.isDefault)
  const activeAddressId = selectedAddressId ?? defaultAddr?.id ?? null

  const { register, handleSubmit, watch, formState: { errors } } = useForm<CheckoutFormData>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: { paymentMethod: 'COD' }
  })
  const paymentMethod = watch('paymentMethod')

  const addrForm = useForm<AddressFormData>({ resolver: zodResolver(addressSchema) })
  const items = cart?.items ?? []
  const subtotal = cart?.totalAmount ?? 0
  const total = Math.max(0, subtotal - discount + shippingFee)

  useEffect(() => {
    if (subtotal >= 500000) {
      setShippingFee(0)
      return
    }

    if (!activeAddressId || addresses.length === 0) {
      setShippingFee(30000)
      return
    }

    const addr = addresses.find((a) => a.id === activeAddressId)
    const ghnMeta = addr?.carrierMetadata?.GHN
    if (!ghnMeta?.districtId || !ghnMeta?.wardCode) {
      setShippingFee(30000)
      return
    }

    const totalWeight = items.reduce((acc, item) => {
      const itemWeight = item.productWeight ?? 500
      return acc + (itemWeight * item.quantity)
    }, 0)

    setIsLoadingShippingFee(true)
    calculateShippingFee.mutate({
      data: {
        toDistrictId: String(ghnMeta.districtId),
        toWardCode: String(ghnMeta.wardCode),
        weight: totalWeight > 0 ? totalWeight : 500,
      }
    }, {
      onSuccess: (res) => {
        setIsLoadingShippingFee(false)
        if (res.success && res.data) {
          setShippingFee(res.data.totalFee)
        } else {
          setShippingFee(30000)
        }
      },
      onError: () => {
        setIsLoadingShippingFee(false)
        setShippingFee(30000)
      }
    })
  }, [activeAddressId, addresses, subtotal, items, calculateShippingFee])

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return
    try {
      const res = await previewCoupon.mutateAsync({ couponCode, cartTotal: subtotal })
      if (res.success && res.data) {
        setDiscount(res.data.discountAmount)
        toast.success(`Áp dụng thành công! Giảm ${formatPrice(res.data.discountAmount)}`)
      }
    } catch { /* error handled in hook */ }
  }

  const onAddAddress = async (data: AddressFormData) => {
    const payload = {
      ...data,
      carrierMetadata: selectedProvinceId && selectedDistrictId && selectedWardCode ? {
        GHN: {
          provinceId: selectedProvinceId,
          districtId: selectedDistrictId,
          wardCode: selectedWardCode,
        }
      } : undefined
    }
    const res = await createAddress.mutateAsync(payload)
    setSelectedAddressId(res.data.id)
    setShowAddressForm(false)
    addrForm.reset()
    setSelectedProvinceId(null)
    setSelectedDistrictId(null)
    setSelectedWardCode(null)
  }

  const toggleAddressForm = () => {
    setShowAddressForm((v) => {
      if (!v) {
        setSelectedProvinceId(null)
        setSelectedDistrictId(null)
        setSelectedWardCode(null)
        addrForm.reset()
      }
      return !v
    })
  }

  const onSubmit = async (data: CheckoutFormData) => {
    if (!activeAddressId) { toast.error('Vui lòng chọn địa chỉ giao hàng'); return }
    if (items.length === 0) { toast.error('Giỏ hàng trống'); return }

    try {
      const orderRes = await createOrder.mutateAsync({
        addressId: activeAddressId,
        paymentMethod: data.paymentMethod,
        couponCode: couponCode || undefined,
        note: data.note,
      })

      if (data.paymentMethod === 'VNPAY') {
        await initiatePayment.mutateAsync(orderRes.data.id)
        // initiatePayment redirects automatically
      } else {
        toast.success('Đặt hàng thành công!')
        router.push(`/orders/${orderRes.data.id}`)
      }
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } }
      toast.error(e?.response?.data?.message || 'Đặt hàng thất bại')
    }
  }

  if (items.length === 0) {
    return (
      <div className="container-main py-20">
        <EmptyState icon="🛒" title="Giỏ hàng trống" action={<Button href="/products">Mua sắm ngay</Button>} />
      </div>
    )
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10">
        <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-8"
          style={{ fontFamily: 'var(--font-display)' }}>
          Thanh toán
        </h1>

        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_380px] gap-8 items-start">
            {/* Left */}
            <div className="space-y-5">
              {/* Shipping address */}
              <section className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
                <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-4">
                  <MapPin className="w-5 h-5 text-[var(--color-gold-500)]" />
                  Địa chỉ giao hàng
                </h2>

                {addresses.length === 0 && !showAddressForm && (
                  <p className="text-sm text-[var(--color-text-muted)] mb-3">Chưa có địa chỉ. Hãy thêm mới.</p>
                )}

                <div className="space-y-3 mb-4">
                  {addresses.map((addr) => (
                    <AddressCard key={addr.id} addr={addr}
                      selected={activeAddressId === addr.id}
                      onSelect={() => setSelectedAddressId(addr.id)} />
                  ))}
                </div>

                <button type="button" onClick={toggleAddressForm}
                  className="flex items-center gap-2 text-sm font-semibold text-[var(--color-gold-600)] hover:text-[var(--color-gold-500)] transition-colors">
                  <Plus className="w-4 h-4" />
                  {showAddressForm ? 'Huỷ' : 'Thêm địa chỉ mới'}
                </button>

                {showAddressForm && (
                  <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
                    className="mt-4 p-4 bg-[var(--color-brown-50)] rounded-xl border border-dashed border-[var(--color-border)]">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <Input label="Tên người nhận" error={addrForm.formState.errors.receiverName?.message}
                        {...addrForm.register('receiverName')} />
                      <Input label="Số điện thoại" error={addrForm.formState.errors.phone?.message}
                        {...addrForm.register('phone')} />
                      
                      <div className="space-y-1">
                        <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                          Tỉnh / Thành phố
                        </label>
                        <select
                          value={selectedProvinceId || ""}
                          onChange={(e) => {
                            const id = e.target.value;
                            const name = provinces.find(p => p.id === id)?.name || "";
                            setSelectedProvinceId(id || null);
                            setSelectedDistrictId(null);
                            setSelectedWardCode(null);
                            addrForm.setValue('province', name);
                            addrForm.setValue('district', '');
                            addrForm.setValue('ward', '');
                          }}
                          className={selectClassName}
                        >
                          <option value="">Chọn Tỉnh / Thành phố</option>
                          {provinces.map((p) => (
                            <option key={p.id} value={p.id}>{p.name}</option>
                          ))}
                        </select>
                        {addrForm.formState.errors.province?.message && <p className="text-xs text-red-500">{addrForm.formState.errors.province.message}</p>}
                      </div>

                      <div className="space-y-1">
                        <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                          Quận / Huyện
                        </label>
                        <select
                          value={selectedDistrictId || ""}
                          disabled={!selectedProvinceId}
                          onChange={(e) => {
                            const id = e.target.value;
                            const name = districts.find(d => d.id === id)?.name || "";
                            setSelectedDistrictId(id || null);
                            setSelectedWardCode(null);
                            addrForm.setValue('district', name);
                            addrForm.setValue('ward', '');
                          }}
                          className={selectClassName}
                        >
                          <option value="">Chọn Quận / Huyện</option>
                          {districts.map((d) => (
                            <option key={d.id} value={d.id}>{d.name}</option>
                          ))}
                        </select>
                        {addrForm.formState.errors.district?.message && <p className="text-xs text-red-500">{addrForm.formState.errors.district.message}</p>}
                      </div>

                      <div className="space-y-1">
                        <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                          Phường / Xã
                        </label>
                        <select
                          value={selectedWardCode || ""}
                          disabled={!selectedDistrictId}
                          onChange={(e) => {
                            const id = e.target.value;
                            const name = wards.find(w => w.id === id)?.name || "";
                            setSelectedWardCode(id || null);
                            addrForm.setValue('ward', name);
                          }}
                          className={selectClassName}
                        >
                          <option value="">Chọn Phường / Xã</option>
                          {wards.map((w) => (
                            <option key={w.id} value={w.id}>{w.name}</option>
                          ))}
                        </select>
                        {addrForm.formState.errors.ward?.message && <p className="text-xs text-red-500">{addrForm.formState.errors.ward.message}</p>}
                      </div>

                      <Input label="Địa chỉ chi tiết" className="sm:col-span-2"
                        error={addrForm.formState.errors.detail?.message}
                        {...addrForm.register('detail')} />
                    </div>
                    <label className="flex items-center gap-2 mt-3 text-sm text-[var(--color-text-muted)] cursor-pointer">
                      <input type="checkbox" {...addrForm.register('isDefault')} className="rounded" />
                      Đặt làm địa chỉ mặc định
                    </label>
                    <Button type="button" size="sm" className="mt-3"
                      loading={createAddress.isPending}
                      onClick={addrForm.handleSubmit(onAddAddress)}>
                      Lưu địa chỉ
                    </Button>
                  </motion.div>
                )}
              </section>

              {/* Payment method */}
              <section className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
                <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-4">
                  <CreditCard className="w-5 h-5 text-[var(--color-gold-500)]" />
                  Phương thức thanh toán
                </h2>
                <div className="space-y-3">
                  {([
                    { value: 'COD', icon: <Banknote className="w-5 h-5 text-[var(--color-green-600)]" />, label: 'Thanh toán khi nhận hàng (COD)' },
                    { value: 'VNPAY', icon: <CreditCard className="w-5 h-5 text-blue-600" />, label: 'Thanh toán qua VNPay' },
                  ] as const).map((m) => (
                    <label key={m.value} className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all
                      ${paymentMethod === m.value
                        ? 'border-[var(--color-gold-500)] bg-amber-50'
                        : 'border-[var(--color-border)] hover:border-[var(--color-brown-200)]'}`}>
                      <input type="radio" value={m.value} {...register('paymentMethod')} className="sr-only" />
                      {m.icon}
                      <span className="text-sm font-medium">{m.label}</span>
                      <div className={`ml-auto w-5 h-5 rounded-full border-2 flex items-center justify-center transition-all
                        ${paymentMethod === m.value ? 'border-[var(--color-gold-500)]' : 'border-[var(--color-border)]'}`}>
                        {paymentMethod === m.value && <div className="w-2.5 h-2.5 rounded-full bg-[var(--color-gold-500)]" />}
                      </div>
                    </label>
                  ))}
                </div>
              </section>

              {/* Coupon */}
              <section className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
                <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-4">
                  <Tag className="w-5 h-5 text-[var(--color-gold-500)]" />
                  Mã giảm giá
                </h2>
                <div className="flex gap-2">
                  <input value={couponCode} onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                    placeholder="Nhập mã coupon..."
                    className="flex-1 h-10 px-4 bg-[var(--color-brown-50)] border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:border-[var(--color-gold-500)] uppercase" />
                  <Button type="button" variant="secondary" size="sm"
                    loading={previewCoupon.isPending} onClick={handleApplyCoupon}>
                    Áp dụng
                  </Button>
                </div>
                {discount > 0 && (
                  <p className="text-sm text-[var(--color-green-600)] font-semibold mt-2">
                    ✓ Giảm {formatPrice(discount)}
                  </p>
                )}
              </section>

              {/* Note */}
              <section className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
                <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-4">
                  <FileText className="w-5 h-5 text-[var(--color-gold-500)]" />
                  Ghi chú
                </h2>
                <textarea {...register('note')} rows={3} placeholder="Giao buổi sáng, để tại bảo vệ..."
                  className="w-full p-3 bg-[var(--color-brown-50)] border border-[var(--color-border)] rounded-xl text-sm resize-none focus:outline-none focus:border-[var(--color-gold-500)]" />
              </section>
            </div>

            {/* Right — order summary */}
            <div className="sticky top-[calc(var(--nav-height)+1rem)]">
              <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
                <h3 className="font-bold text-[var(--color-brown-900)] mb-4" style={{ fontFamily: 'var(--font-display)' }}>
                  Đơn hàng ({items.length} sản phẩm)
                </h3>

                <div className="space-y-3 max-h-72 overflow-y-auto mb-4">
                  {items.map((item) => (
                    <div key={item.id} className="flex items-center gap-3">
                      <div className="relative w-12 h-12 rounded-lg overflow-hidden flex-shrink-0 bg-[var(--color-brown-50)]">
                        <Image src={item.productThumbnail} alt={item.productName} fill className="object-cover" sizes="48px" />
                        <span className="absolute -top-1.5 -right-1.5 w-5 h-5 bg-[var(--color-brown-900)] text-white text-[10px] font-bold rounded-full flex items-center justify-center">
                          {item.quantity}
                        </span>
                      </div>
                      <span className="flex-1 text-xs text-[var(--color-text)] line-clamp-2">{item.productName}</span>
                      <span className="text-sm font-semibold flex-shrink-0">{formatPrice(item.subtotal)}</span>
                    </div>
                  ))}
                </div>

                <hr className="border-[var(--color-border)] mb-4" />

                <div className="space-y-2 text-sm mb-4">
                  <div className="flex justify-between text-[var(--color-text-muted)]">
                    <span>Tạm tính</span><span>{formatPrice(subtotal)}</span>
                  </div>
                  {discount > 0 && (
                    <div className="flex justify-between text-[var(--color-green-600)]">
                      <span>Giảm giá</span><span>-{formatPrice(discount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-[var(--color-text)]">
                    <span className="text-[var(--color-text-muted)]">Vận chuyển</span>
                    <span className={shippingFee === 0 ? 'text-[var(--color-green-600)] font-semibold' : 'font-semibold'}>
                      {isLoadingShippingFee ? 'Đang tính...' : (shippingFee === 0 ? 'Miễn phí' : formatPrice(shippingFee))}
                    </span>
                  </div>
                </div>

                <hr className="border-[var(--color-border)] mb-4" />

                <div className="flex justify-between font-bold mb-6">
                  <span className="text-[var(--color-brown-900)]">Tổng cộng</span>
                  <span className="text-xl text-[var(--color-orange-500)]">{formatPrice(total)}</span>
                </div>

                <Button type="submit" fullWidth size="lg" variant="gold"
                  loading={createOrder.isPending || initiatePayment.isPending}>
                  {paymentMethod === 'VNPAY' ? '💳 Thanh toán VNPay' : '✓ Đặt hàng ngay'}
                </Button>

                <p className="text-center text-xs text-[var(--color-text-subtle)] mt-3">🔒 Thông tin thanh toán được bảo mật</p>
              </div>
            </div>
          </div>
        </form>
      </div>
    </div>
  )
}

function AddressCard({ addr, selected, onSelect }: { addr: AddressResponse; selected: boolean; onSelect: () => void }) {
  return (
    <label className={`flex items-start gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all
      ${selected ? 'border-[var(--color-gold-500)] bg-amber-50' : 'border-[var(--color-border)] hover:border-[var(--color-brown-200)]'}`}>
      <input type="radio" checked={selected} onChange={onSelect} className="sr-only" />
      <div className="flex-1">
        <div className="flex items-center gap-2 mb-1">
          <span className="font-semibold text-sm text-[var(--color-brown-900)]">{addr.receiverName}</span>
          <span className="text-sm text-[var(--color-text-muted)]">{addr.phone}</span>
          {addr.isDefault && <span className="badge-organic text-[10px]">Mặc định</span>}
        </div>
        <p className="text-xs text-[var(--color-text-muted)]">
          {addr.detail}, {addr.ward}, {addr.district}, {addr.province}
        </p>
      </div>
      <div className={`w-5 h-5 rounded-full border-2 flex-shrink-0 mt-0.5 flex items-center justify-center
        ${selected ? 'border-[var(--color-gold-500)]' : 'border-[var(--color-border)]'}`}>
        {selected && <div className="w-2.5 h-2.5 rounded-full bg-[var(--color-gold-500)]" />}
      </div>
    </label>
  )
}