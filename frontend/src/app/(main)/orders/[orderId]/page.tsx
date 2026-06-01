'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Image from 'next/image'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import { ChevronRight, MapPin, CreditCard, Package, Truck, CheckCircle, XCircle, Clock } from 'lucide-react'
import { useOrder, useCancelOrder } from '@/hooks'
import { Badge, Button, Skeleton, EmptyState } from '@/components/ui'
import { ReviewModal } from '@/components/shared/ReviewModal'
import { formatPrice, formatDateTime, ORDER_STATUS_LABELS, ORDER_STATUS_COLORS, slugify } from '@/lib/utils'
import type { OrderStatus } from '@/types'

const ORDER_STEPS: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING', 'DELIVERED']

export default function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const router = useRouter()
  const { data: orderRes, isLoading } = useOrder(Number(orderId))
  const cancelOrder = useCancelOrder()
  const [reviewProduct, setReviewProduct] = useState<any>(null)

  const order = orderRes?.data

  if (isLoading) return (
    <div className="container-main py-10 max-w-3xl space-y-4">
      {[1, 2, 3].map((i) => <Skeleton key={i} className="h-32 rounded-2xl" />)}
    </div>
  )

  if (!order) return (
    <div className="container-main py-20"><EmptyState icon="📦" title="Không tìm thấy đơn hàng" action={<Button href="/orders">Xem đơn hàng</Button>} /></div>
  )

  const statusColor = ORDER_STATUS_COLORS[order.status as OrderStatus]
  const isTerminal = ['CANCELLED', 'REFUNDED', 'RETURNED'].includes(order.status)
  const currentStep = ORDER_STEPS.indexOf(order.status as OrderStatus)

  const addressParts = order.shippingAddress ? order.shippingAddress.split(' - ') : []
  const receiverInfo = addressParts.length >= 2 ? `${addressParts[0]} — ${addressParts[1]}` : (order.shippingAddress || '')
  const addressDetail = addressParts.length >= 3 ? addressParts.slice(2).join(' - ') : ''

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="bg-white border-b border-[var(--color-border)]">
        <div className="container-main py-3 flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
          <Link href="/orders" className="hover:text-[var(--color-brown-900)]">Đơn hàng</Link>
          <ChevronRight className="w-3 h-3" />
          <span className="text-[var(--color-brown-900)] font-mono font-medium">#{order.id}</span>
        </div>
      </div>

      <div className="container-main py-10 max-w-3xl">
        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-[var(--color-brown-900)]"
              style={{ fontFamily: 'var(--font-display)' }}>
              Đơn hàng #{order.id}
            </h1>
            <p className="text-sm text-[var(--color-text-muted)] mt-1">{formatDateTime(order.createdAt)}</p>
          </div>
          <Badge className={`${statusColor.bg} ${statusColor.text} border-0`} size="lg">
            {ORDER_STATUS_LABELS[order.status as OrderStatus]}
          </Badge>
        </div>

        {/* Progress stepper (for active orders) */}
        {!isTerminal && (
          <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-5">
            <div className="flex items-center">
              {ORDER_STEPS.map((step, i) => {
                const isActive = i <= currentStep
                const isCurrent = i === currentStep
                const icons = [<Clock key="c" />, <CheckCircle key="ch" />, <Package key="p" />, <Truck key="t" />, <CheckCircle key="ch2" />]
                return (
                  <div key={step} className="flex items-center flex-1 last:flex-none">
                    <div className={`flex flex-col items-center flex-shrink-0`}>
                      <div className={`w-9 h-9 rounded-full flex items-center justify-center transition-all
                        ${isCurrent ? 'bg-[var(--color-gold-500)] text-white scale-110 shadow-md' :
                          isActive ? 'bg-[var(--color-green-600)] text-white' :
                          'bg-[var(--color-brown-100)] text-[var(--color-text-subtle)]'}`}>
                        <span className="w-4 h-4">{icons[i]}</span>
                      </div>
                      <p className={`text-[10px] mt-1 font-medium text-center max-w-[60px]
                        ${isActive ? 'text-[var(--color-brown-900)]' : 'text-[var(--color-text-subtle)]'}`}>
                        {ORDER_STATUS_LABELS[step]}
                      </p>
                    </div>
                    {i < ORDER_STEPS.length - 1 && (
                      <div className={`flex-1 h-0.5 mx-2 transition-colors
                        ${i < currentStep ? 'bg-[var(--color-green-400)]' : 'bg-[var(--color-brown-100)]'}`} />
                    )}
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* Items */}
        <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-5">
          <h2 className="font-bold text-[var(--color-brown-900)] mb-4">Sản phẩm</h2>
          <div className="space-y-4">
            {order.items.map((item) => (
              <div key={item.id} className="flex items-center gap-4">
                <div className="relative w-14 h-14 rounded-xl overflow-hidden flex-shrink-0 bg-[var(--color-brown-50)]">
                  <Image src={item.productThumbnail} alt={item.productName} fill className="object-cover" sizes="56px" />
                </div>
                <div className="flex-1">
                  <Link href={`/products/${slugify(item.productName)}`}
                    className="text-sm font-medium text-[var(--color-brown-900)] hover:text-[var(--color-gold-600)] line-clamp-2">
                    {item.productName}
                  </Link>
                  <p className="text-xs text-[var(--color-text-muted)] mb-1">
                    {formatPrice(item.unitPrice)} × {item.quantity}
                  </p>
                  {order.status === 'DELIVERED' && (
                    <button
                      onClick={() => setReviewProduct(item)}
                      className="text-xs font-semibold text-[var(--color-gold-600)] hover:text-[var(--color-gold-500)] underline decoration-dotted transition-colors cursor-pointer"
                    >
                      Viết đánh giá
                    </button>
                  )}
                </div>
                <p className="font-bold text-[var(--color-brown-900)] text-sm">{formatPrice(item.subtotal)}</p>
              </div>
            ))}
          </div>

          <hr className="border-[var(--color-border)] my-4" />

          <div className="space-y-2 text-sm">
            <div className="flex justify-between text-[var(--color-text-muted)]">
              <span>Tạm tính</span><span>{formatPrice(order.subtotal)}</span>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-[var(--color-green-600)]">
                <span>Giảm giá {order.couponCode && `(${order.couponCode})`}</span>
                <span>-{formatPrice(order.discountAmount)}</span>
              </div>
            )}
            <div className="flex justify-between text-[var(--color-green-600)]">
              <span>Vận chuyển</span><span className="font-semibold">Miễn phí</span>
            </div>
            <div className="flex justify-between font-bold text-base pt-2 border-t border-[var(--color-border)]">
              <span className="text-[var(--color-brown-900)]">Tổng cộng</span>
              <span className="text-[var(--color-orange-500)] text-lg">{formatPrice(order.totalAmount)}</span>
            </div>
          </div>
        </div>

        {/* Shipping address */}
        <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-5">
          <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-3">
            <MapPin className="w-4 h-4 text-[var(--color-gold-500)]" /> Địa chỉ giao hàng
          </h2>
          <p className="font-semibold text-sm text-[var(--color-brown-900)]">
            {receiverInfo}
          </p>
          {addressDetail && (
            <p className="text-sm text-[var(--color-text-muted)] mt-1">{addressDetail}</p>
          )}
        </div>

        {/* Payment */}
        <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-5">
          <h2 className="font-bold text-[var(--color-brown-900)] flex items-center gap-2 mb-3">
            <CreditCard className="w-4 h-4 text-[var(--color-gold-500)]" /> Thanh toán
          </h2>
          <div className="flex items-center justify-between text-sm">
            <span className="text-[var(--color-text-muted)]">Phương thức</span>
            <span className="font-medium">{order.paymentMethod === 'COD' ? 'Tiền mặt (COD)' : 'VNPay'}</span>
          </div>
          <div className="flex items-center justify-between text-sm mt-2">
            <span className="text-[var(--color-text-muted)]">Trạng thái</span>
            <Badge variant={order.paymentStatus === 'PAID' ? 'success' : order.paymentStatus === 'FAILED' ? 'danger' : 'warning'}>
              {order.paymentStatus === 'PAID' ? 'Đã thanh toán' : order.paymentStatus === 'FAILED' ? 'Thất bại' : 'Chờ thanh toán'}
            </Badge>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          {order.status === 'PENDING' && (
            <Button variant="danger" onClick={() => {
              if (confirm('Bạn có chắc muốn huỷ đơn hàng này?')) cancelOrder.mutate(order.id)
            }} loading={cancelOrder.isPending}>
              <XCircle className="w-4 h-4" />
              Huỷ đơn hàng
            </Button>
          )}
          <Button variant="secondary" href="/orders">← Đơn hàng khác</Button>
        </div>
      </div>

      <AnimatePresence>
        {reviewProduct && (
          <ReviewModal
            product={{
              id: reviewProduct.productId,
              name: reviewProduct.productName,
              thumbnail: reviewProduct.productThumbnail,
            }}
            orderId={order.id}
            onClose={() => setReviewProduct(null)}
          />
        )}
      </AnimatePresence>
    </div>
  )
}