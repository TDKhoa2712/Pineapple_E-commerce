'use client'

import { useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { Heart, ShoppingCart } from 'lucide-react'
import { motion } from 'framer-motion'
import { useWishlist, useToggleWishlist, useAddToCart } from '@/hooks'
import { Button, Badge, Pagination, Skeleton, EmptyState } from '@/components/ui'
import { formatPrice, calcDiscountPercent } from '@/lib/utils'

export default function WishlistPage() {
  const [page, setPage] = useState(0)
  const { data: wishlistRes, isLoading } = useWishlist({ page })
  const toggleWishlist = useToggleWishlist()
  const addToCart = useAddToCart()

  const items = wishlistRes?.data?.content ?? []
  const totalPages = wishlistRes?.data?.totalPages ?? 0

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10">
        <h1 className="text-3xl font-bold text-[var(--color-brown-900)] mb-8"
          style={{ fontFamily: 'var(--font-display)' }}>
          Yêu thích ({wishlistRes?.data?.totalElements ?? 0} sản phẩm)
        </h1>

        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {[1,2,3,4].map((i) => <Skeleton key={i} className="h-64 rounded-2xl" />)}
          </div>
        ) : items.length === 0 ? (
          <EmptyState icon="❤️" title="Danh sách yêu thích trống"
            description="Hãy thêm những sản phẩm bạn thích vào đây!"
            action={<Button href="/products" variant="gold"><ShoppingCart className="w-4 h-4" />Khám phá sản phẩm</Button>} />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {items.map((item) => {
              const discountPct = item.productDiscountPrice
                ? calcDiscountPercent(item.productPrice, item.productDiscountPrice) : 0
              return (
                <motion.div key={item.id} layout
                  className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden group hover:shadow-[var(--shadow-md)] transition-shadow">
                  <div className="relative aspect-square overflow-hidden bg-[var(--color-brown-50)]">
                    <Link href={`/products/${item.productSlug}`}>
                      <Image src={item.productThumbnail || '/placeholder.jpg'} alt={item.productName}
                        fill className="object-cover group-hover:scale-105 transition-transform duration-500" sizes="25vw" />
                    </Link>
                    {discountPct > 0 && (
                      <div className="absolute top-2 left-2">
                        <Badge variant="danger" size="sm">-{discountPct}%</Badge>
                      </div>
                    )}
                    <button
                      onClick={(e) => {
                        e.preventDefault()
                        e.stopPropagation()
                        toggleWishlist.mutate(item.productId)
                      }}
                      disabled={toggleWishlist.isPending}
                      className="absolute top-2 right-2 w-8 h-8 bg-white rounded-full shadow flex items-center justify-center text-red-500 hover:scale-110 transition-transform disabled:opacity-50 z-10"
                    >
                      <Heart className="w-4 h-4 fill-current" />
                    </button>
                  </div>
                  <div className="p-4">
                    <Link href={`/products/${item.productSlug}`}
                      className="text-sm font-semibold text-[var(--color-brown-900)] hover:text-[var(--color-gold-600)] line-clamp-2 mb-2 block">
                      {item.productName}
                    </Link>
                    <div className="flex items-center justify-between gap-2">
                      <div>
                        <p className="text-[var(--color-orange-500)] font-bold text-sm">
                          {formatPrice(item.productDiscountPrice ?? item.productPrice)}
                        </p>
                        {item.productDiscountPrice && (
                          <p className="text-xs text-[var(--color-text-subtle)] line-through">
                            {formatPrice(item.productPrice)}
                          </p>
                        )}
                      </div>
                      <Button size="xs" variant="gold"
                        onClick={() => addToCart.mutate({ productId: item.productId, quantity: 1 })}
                        disabled={item.productStatus === 'OUT_OF_STOCK'}>
                        <ShoppingCart className="w-3.5 h-3.5" />
                      </Button>
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