'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import Image from 'next/image'
import Link from 'next/link'
import { MapPin, Package, User, ChevronRight } from 'lucide-react'
import { useFarm, useFarmProducts, useAddToCart } from '@/hooks'
import { ProductCard, ProductCardSkeleton, Pagination, Skeleton, EmptyState, Button } from '@/components/ui'

export default function FarmDetailPage() {
  const { farmId } = useParams<{ farmId: string }>()
  const [page, setPage] = useState(0)

  const { data: farmRes, isLoading: farmLoading } = useFarm(Number(farmId))
  const { data: productsRes, isLoading: productsLoading } = useFarmProducts(Number(farmId), { page, size: 12 })
  const addToCart = useAddToCart()

  const farm = farmRes?.data
  const products = productsRes?.data?.content ?? []
  const totalPages = productsRes?.data?.totalPages ?? 0

  if (farmLoading) return (
    <div className="container-main py-10 space-y-6">
      <Skeleton className="h-60 rounded-2xl" />
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {[1,2,3,4].map((i) => <ProductCardSkeleton key={i} />)}
      </div>
    </div>
  )

  if (!farm) return (
    <div className="container-main py-20">
      <EmptyState icon="🌾" title="Không tìm thấy trang trại"
        action={<Button href="/farms">Quay lại</Button>} />
    </div>
  )

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      {/* Breadcrumb */}
      <div className="bg-white border-b border-[var(--color-border)]">
        <div className="container-main py-3 flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
          <Link href="/" className="hover:text-[var(--color-brown-900)]">Trang chủ</Link>
          <ChevronRight className="w-3 h-3" />
          <Link href="/farms" className="hover:text-[var(--color-brown-900)]">Trang trại</Link>
          <ChevronRight className="w-3 h-3" />
          <span className="text-[var(--color-brown-900)] font-medium">{farm.name}</span>
        </div>
      </div>

      {/* Farm hero */}
      <div className="relative h-56 md:h-72 overflow-hidden bg-[var(--color-green-700)]">
        {farm.imageUrl && (
          <Image src={farm.imageUrl} alt={farm.name} fill
            className="object-cover opacity-50"
            sizes="100vw" priority />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-[var(--color-brown-950)]/80 to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 container-main pb-8">
          <p className="badge-organic text-sm mb-2">🌿 Trang trại hữu cơ</p>
          <h1 className="text-3xl md:text-4xl font-bold text-white"
            style={{ fontFamily: 'var(--font-display)' }}>
            {farm.name}
          </h1>
          <div className="flex flex-wrap items-center gap-4 mt-2 text-sm text-white/80">
            <span className="flex items-center gap-1.5">
              <MapPin className="w-4 h-4" /> {farm.location}
            </span>
            <span className="flex items-center gap-1.5">
              <User className="w-4 h-4" /> {farm.ownerName}
            </span>
            <span className="flex items-center gap-1.5">
              <Package className="w-4 h-4" /> {productsRes?.data?.totalElements ?? 0} sản phẩm
            </span>
          </div>
        </div>
      </div>

      <div className="container-main py-10">
        {farm.description && (
          <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 mb-8">
            <h2 className="font-bold text-[var(--color-brown-900)] mb-3"
              style={{ fontFamily: 'var(--font-display)' }}>Giới thiệu trang trại</h2>
            <p className="text-sm text-[var(--color-text)] leading-relaxed whitespace-pre-wrap">{farm.description}</p>
          </div>
        )}

        <h2 className="text-2xl font-bold text-[var(--color-brown-900)] mb-6"
          style={{ fontFamily: 'var(--font-display)' }}>
          Sản phẩm từ trang trại
        </h2>

        {productsLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {[1,2,3,4].map((i) => <ProductCardSkeleton key={i} />)}
          </div>
        ) : products.length === 0 ? (
          <EmptyState icon="🥦" title="Trang trại chưa có sản phẩm" />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {products.map((product) => (
              <ProductCard key={product.id} product={product}
                onAddToCart={(p) => addToCart.mutate({ productId: p.id, quantity: 1 })}
                addingToCart={addToCart.isPending} />
            ))}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </div>
  )
}