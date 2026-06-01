'use client'

import { useState } from 'react'
import Link from 'next/link'
import Image from 'next/image'
import { motion } from 'framer-motion'
import { Package } from 'lucide-react'
import { useOrders } from '@/hooks'
import { Badge, Pagination, Skeleton, EmptyState, Button } from '@/components/ui'
import { formatPrice, formatDateTime, ORDER_STATUS_LABELS, ORDER_STATUS_COLORS } from '@/lib/utils'
import type { OrderStatus } from '@/types'

const STATUS_TABS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả' },
  { value: 'PENDING', label: 'Chờ xác nhận' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'PROCESSING', label: 'Đang xử lý' },
  { value: 'SHIPPING', label: 'Đang giao' },
  { value: 'DELIVERED', label: 'Đã giao' },
  { value: 'CANCELLED', label: 'Đã huỷ' },
]

export default function OrdersPage() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')

  const { data: ordersRes, isLoading } = useOrders({
    page,
    size: 10,
    status: status || undefined,
  })

  const orders = ordersRes?.data?.content ?? []
  const totalPages = ordersRes?.data?.totalPages ?? 0

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10 max-w-4xl">
        <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-6"
          style={{ fontFamily: 'var(--font-display)' }}>
          Đơn hàng của tôi
        </h1>

        {/* Status tabs */}
        <div className="flex gap-1 overflow-x-auto pb-2 mb-6 no-scrollbar">
          {STATUS_TABS.map((tab) => (
            <button key={tab.value} onClick={() => { setStatus(tab.value); setPage(0) }}
              className={`flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition-all
                ${status === tab.value
                  ? 'bg-[var(--color-brown-900)] text-white'
                  : 'bg-white border border-[var(--color-border)] text-[var(--color-text-muted)] hover:border-[var(--color-brown-300)]'}`}>
              {tab.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => <Skeleton key={i} className="h-40 rounded-2xl" />)}
          </div>
        ) : orders.length === 0 ? (
          <EmptyState icon="📦" title="Chưa có đơn hàng nào"
            description="Hãy khám phá các sản phẩm tươi ngon và đặt hàng ngay!"
            action={<Button href="/products" variant="gold"><Package className="w-4 h-4" />Mua sắm ngay</Button>} />
        ) : (
          <div className="space-y-4">
            {orders.map((order) => {
              const statusColor = ORDER_STATUS_COLORS[order.status as OrderStatus]
              return (
                <motion.div key={order.id} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
                  className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden hover:shadow-[var(--shadow-sm)] transition-shadow">
                  {/* Header */}
                  <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)]">
                    <div>
                      <p className="text-xs text-[var(--color-text-muted)]">Mã đơn hàng</p>
                      <p className="font-bold text-[var(--color-brown-900)] font-mono text-sm">
                        #{order.id}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-xs text-[var(--color-text-muted)] mb-1">{formatDateTime(order.createdAt)}</p>
                      <Badge className={`${statusColor.bg} ${statusColor.text} border-0`}>
                        {ORDER_STATUS_LABELS[order.status as OrderStatus]}
                      </Badge>
                    </div>
                  </div>

                  {/* Items preview */}
                  <div className="px-6 py-4">
                    <div className="flex items-center gap-3 mb-3">
                      <div className="flex -space-x-2">
                        {order.items.slice(0, 3).map((item) => (
                          <div key={item.id} className="relative w-10 h-10 rounded-lg overflow-hidden border-2 border-white bg-[var(--color-brown-50)]">
                            <Image src={item.productThumbnail} alt={item.productName} fill className="object-cover" sizes="40px" />
                          </div>
                        ))}
                        {order.items.length > 3 && (
                          <div className="w-10 h-10 rounded-lg border-2 border-white bg-[var(--color-brown-100)] flex items-center justify-center text-xs font-bold text-[var(--color-text-muted)]">
                            +{order.items.length - 3}
                          </div>
                        )}
                      </div>
                      <div className="flex-1">
                        <p className="text-sm text-[var(--color-text)] line-clamp-1">
                          {order.items.map((i) => i.productName).join(', ')}
                        </p>
                        <p className="text-xs text-[var(--color-text-muted)]">{order.items.length} sản phẩm</p>
                      </div>
                    </div>

                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-xs text-[var(--color-text-muted)]">Tổng tiền</p>
                        <p className="font-bold text-[var(--color-orange-500)] text-lg">{formatPrice(order.totalAmount)}</p>
                      </div>
                      <Link href={`/orders/${order.id}`}>
                        <Button variant="secondary" size="sm">Xem chi tiết →</Button>
                      </Link>
                    </div>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </div>
  )
}