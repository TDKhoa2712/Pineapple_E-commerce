'use client'

import { useState, useEffect } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import { Minus, Plus, Trash2, ShoppingBag, ArrowRight } from 'lucide-react'
import { useCart, useUpdateCartItem, useRemoveCartItem } from '@/hooks'
import { useCartStore } from '@/stores/cart-store'
import { Button, Skeleton, EmptyState } from '@/components/ui'
import { formatPrice } from '@/lib/utils'
import { useAuthStore } from '@/stores/auth-store'

interface QuantityInputProps {
  value: number
  max: number
  onChange: (val: number) => void
}

function QuantityInput({ value, max, onChange }: QuantityInputProps) {
  const [localVal, setLocalVal] = useState<string>(value.toString())

  useEffect(() => {
    setLocalVal(value.toString())
  }, [value])

  const handleBlurOrSubmit = () => {
    let parsed = parseInt(localVal)
    if (isNaN(parsed) || parsed < 1) {
      parsed = 1
    } else if (parsed > max) {
      parsed = max
    }
    setLocalVal(parsed.toString())
    if (parsed !== value) {
      onChange(parsed)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.currentTarget.blur()
    }
  }

  return (
    <input
      type="number"
      value={localVal}
      onChange={(e) => setLocalVal(e.target.value)}
      onBlur={handleBlurOrSubmit}
      onKeyDown={handleKeyDown}
      className="w-8 text-center text-sm font-semibold bg-transparent border-none focus:outline-none focus:ring-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
    />
  )
}

export default function CartPage() {
  const { isAuthenticated } = useAuthStore()
  const { cart } = useCartStore()
  const { isLoading } = useCart()
  const updateItem = useUpdateCartItem()
  const removeItem = useRemoveCartItem()

  if (!isAuthenticated) {
    return (
      <div className="container-main py-20">
        <EmptyState icon="🛒" title="Đăng nhập để xem giỏ hàng"
          action={<Button href="/login">Đăng nhập ngay</Button>} />
      </div>
    )
  }

  if (isLoading) return <CartSkeleton />

  const items = cart?.items ?? []

  if (items.length === 0) {
    return (
      <div className="container-main py-20">
        <EmptyState icon="🛒" title="Giỏ hàng trống"
          description="Hãy thêm vài sản phẩm tươi ngon vào giỏ nào!"
          action={<Button href="/products" variant="gold"><ShoppingBag className="w-4 h-4" />Khám phá sản phẩm</Button>} />
      </div>
    )
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10">
        <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-8"
          style={{ fontFamily: 'var(--font-display)' }}>
          Giỏ hàng ({items.length} sản phẩm)
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Items */}
          <div className="lg:col-span-2 space-y-3">
            <AnimatePresence>
              {items.map((item) => (
                <motion.div key={item.id}
                  layout initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: -20, height: 0 }}
                  className="bg-white rounded-2xl border border-[var(--color-border)] p-4 flex items-center gap-4">
                  {/* Image */}
                  <div className="relative w-20 h-20 rounded-xl overflow-hidden flex-shrink-0 bg-[var(--color-brown-50)]">
                    <Image src={item.productThumbnail || '/placeholder.jpg'} alt={item.productName}
                      fill className="object-cover" sizes="80px" />
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <Link href={`/products/${item.productSlug}`}
                      className="font-semibold text-sm text-[var(--color-brown-900)] hover:text-[var(--color-gold-600)] line-clamp-2">
                      {item.productName}
                    </Link>
                    <p className="text-[var(--color-orange-500)] font-bold mt-1 flex items-baseline gap-0.5">
                      <span>{formatPrice(item.unitPrice)}</span>
                      {item.productUnit && (
                        <span className="text-xs font-normal text-[var(--color-text-muted)] ml-0.5">
                          / {item.productUnit}
                        </span>
                      )}
                    </p>
                    {item.productStatus === 'OUT_OF_STOCK' && (
                      <p className="text-xs text-red-500 mt-1">Sản phẩm hết hàng</p>
                    )}
                  </div>

                  {/* Quantity */}
                  <div className="flex items-center gap-1 bg-[var(--color-brown-50)] rounded-xl border border-[var(--color-border)] p-1">
                    <button onClick={() => {
                      if (item.quantity === 1) removeItem.mutate(item.id)
                      else updateItem.mutate({ cartItemId: item.id, quantity: item.quantity - 1 })
                    }}
                      className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-white transition-colors">
                      <Minus className="w-3.5 h-3.5" />
                    </button>
                    <QuantityInput
                      value={item.quantity}
                      max={item.availableStock}
                      onChange={(newQty) => updateItem.mutate({ cartItemId: item.id, quantity: newQty })}
                    />
                    <button onClick={() => updateItem.mutate({ cartItemId: item.id, quantity: item.quantity + 1 })}
                      disabled={item.quantity >= item.availableStock}
                      className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-white transition-colors disabled:opacity-40">
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Subtotal + remove */}
                  <div className="text-right flex-shrink-0">
                    <p className="font-bold text-[var(--color-brown-900)] text-sm">
                      {formatPrice(item.subtotal)}
                    </p>
                    <button onClick={() => removeItem.mutate(item.id)}
                      className="text-[var(--color-text-subtle)] hover:text-red-500 mt-1 transition-colors">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>

          {/* Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 sticky top-[calc(var(--nav-height)+1rem)]">
              <h2 className="font-bold text-[var(--color-brown-900)] text-lg mb-4"
                style={{ fontFamily: 'var(--font-display)' }}>
                Tóm tắt đơn hàng
              </h2>

              <div className="space-y-3 mb-4">
                {items.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <span className="text-[var(--color-text-muted)] line-clamp-1 flex-1 mr-2">
                      {item.productName} ×{item.quantity}
                    </span>
                    <span className="font-medium flex-shrink-0">{formatPrice(item.subtotal)}</span>
                  </div>
                ))}
              </div>

              <hr className="border-[var(--color-border)] my-4" />

              <div className="flex justify-between text-sm mb-2">
                <span className="text-[var(--color-text-muted)]">Tạm tính</span>
                <span>{formatPrice(cart?.totalAmount ?? 0)}</span>
              </div>
              <div className="flex justify-between text-sm mb-4">
                <span className="text-[var(--color-text-muted)]">Vận chuyển</span>
                <span className="text-[var(--color-green-600)] font-semibold">Miễn phí</span>
              </div>

              <hr className="border-[var(--color-border)] mb-4" />

              <div className="flex justify-between font-bold mb-6">
                <span className="text-[var(--color-brown-900)]">Tổng cộng</span>
                <span className="text-xl text-[var(--color-orange-500)]">
                  {formatPrice(cart?.totalAmount ?? 0)}
                </span>
              </div>

              <Button href="/checkout" fullWidth size="lg" variant="gold">
                Tiến hành thanh toán
                <ArrowRight className="w-4 h-4" />
              </Button>

              <p className="text-center text-xs text-[var(--color-text-subtle)] mt-3">
                🔒 Thông tin được bảo mật
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function CartSkeleton() {
  return (
    <div className="container-main py-10">
      <Skeleton className="h-8 w-48 mb-8" />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-2xl p-4 flex gap-4">
              <Skeleton className="w-20 h-20 rounded-xl flex-shrink-0" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-4 w-1/3" />
              </div>
            </div>
          ))}
        </div>
        <Skeleton className="h-64 rounded-2xl" />
      </div>
    </div>
  )
}