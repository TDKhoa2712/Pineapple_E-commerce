'use client'

import { useState, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { SlidersHorizontal, Search, Leaf } from 'lucide-react'
import { useProducts, useCategories, useAddToCart } from '@/hooks'
import {
  Button,
  ProductCard,
  ProductCardSkeleton,
  Pagination,
  SectionHeader,
  EmptyState,
} from '@/components/ui'
import type { ProductSearchParams, ProductResponse } from '@/types'
import { cn } from '@/lib/utils'

const SORT_OPTIONS = [
  { value: '', label: 'Mới nhất' },
  { value: 'price_asc', label: 'Giá tăng dần' },
  { value: 'price_desc', label: 'Giá giảm dần' },
  { value: 'rating', label: 'Đánh giá cao nhất' },
  { value: 'sold', label: 'Bán chạy nhất' },
]

function HomeContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [page, setPage] = useState(0)

  const keyword = searchParams.get('keyword') || ''
  const categoryId = searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined
  const isOrganic = searchParams.get('isOrganic') === 'true' ? true : undefined
  const sort = (searchParams.get('sort') as ProductSearchParams['sort']) || undefined

  const params: ProductSearchParams = { page, size: 12, keyword: keyword || undefined, categoryId, isOrganic, sort }
  const { data: productsRes, isLoading: productsLoading } = useProducts(params)
  const { data: categoriesRes } = useCategories()
  const addToCart = useAddToCart()

  const products = productsRes?.data?.content ?? []
  const totalPages = productsRes?.data?.totalPages ?? 0
  const totalElements = productsRes?.data?.totalElements ?? 0
  const categories = categoriesRes?.data ?? []

  const updateFilter = (key: string, value: string | null) => {
    const next = new URLSearchParams(searchParams.toString())
    if (value) next.set(key, value)
    else next.delete(key)
    router.push(`/?${next.toString()}`)
    setPage(0)
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      {/* Hero banner */}
      {!keyword && !categoryId && !isOrganic && page === 0 && (
        <HeroBanner />
      )}

      {/* Main content */}
      <div className="container-main py-10">
        {/* Category pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-4 mb-8 no-scrollbar">
          <button
            onClick={() => updateFilter('categoryId', null)}
            className={cn(
              'flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
              !categoryId
                ? 'bg-[var(--color-brown-900)] text-[var(--color-cream)] border-[var(--color-brown-900)]'
                : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]'
            )}
          >
            Tất cả
          </button>
          <button
            onClick={() => updateFilter('isOrganic', isOrganic ? null : 'true')}
            className={cn(
              'flex-shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium border transition-all',
              isOrganic
                ? 'bg-[var(--color-green-600)] text-white border-[var(--color-green-600)]'
                : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-green-400)]'
            )}
          >
            <Leaf className="w-3.5 h-3.5" />
            Hữu cơ
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => updateFilter('categoryId', categoryId === cat.id ? null : String(cat.id))}
              className={cn(
                'flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
                categoryId === cat.id
                  ? 'bg-[var(--color-brown-900)] text-[var(--color-cream)] border-[var(--color-brown-900)]'
                  : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]'
              )}
            >
              {cat.icon && <span className="mr-1">{cat.icon}</span>}
              {cat.name}
            </button>
          ))}
        </div>

        {/* Toolbar */}
        <div className="flex items-center justify-between mb-6 gap-4">
          <div>
            {keyword ? (
              <div className="flex items-center gap-2">
                <Search className="w-4 h-4 text-[var(--color-text-muted)]" />
                <span className="text-sm font-medium text-[var(--color-brown-900)]">
                  Kết quả cho &ldquo;<strong>{keyword}</strong>&rdquo;
                </span>
                <span className="text-xs text-[var(--color-text-subtle)]">
                  ({totalElements} sản phẩm)
                </span>
                <button
                  onClick={() => updateFilter('keyword', null)}
                  className="text-xs text-red-500 hover:text-red-600 ml-1"
                >
                  ✕
                </button>
              </div>
            ) : (
              <SectionHeader
                title={
                  categoryId
                    ? (categories.find((c) => c.id === categoryId)?.name ?? 'Sản phẩm')
                    : 'Tất cả sản phẩm'
                }
                subtitle={`${totalElements} sản phẩm`}
              />
            )}
          </div>

          {/* Sort */}
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="w-4 h-4 text-[var(--color-text-muted)]" />
            <select
              value={sort || ''}
              onChange={(e) => updateFilter('sort', e.target.value || null)}
              className="h-9 px-3 pr-8 bg-white border border-[var(--color-border)] rounded-xl text-sm text-[var(--color-text)] focus:outline-none focus:border-[var(--color-gold-500)] appearance-none cursor-pointer"
            >
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Grid */}
        {productsLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-5">
            {Array.from({ length: 12 }).map((_, i) => (
              <ProductCardSkeleton key={i} />
            ))}
          </div>
        ) : products.length === 0 ? (
          <EmptyState
            icon="🔍"
            title="Không tìm thấy sản phẩm"
            description="Hãy thử tìm kiếm với từ khóa khác hoặc xem tất cả sản phẩm."
            action={
              <Button onClick={() => router.push('/')} variant="secondary">
                Xem tất cả
              </Button>
            }
          />
        ) : (
          <motion.div
            className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-5"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.3 }}
          >
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                onAddToCart={(p) => addToCart.mutate({ productId: p.id, quantity: 1 })}
                addingToCart={addToCart.isPending}
              />
            ))}
          </motion.div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </div>
  )
}

// ─── Hero Banner ──────────────────────────────────────────────────────────────
function HeroBanner() {
  const router = useRouter()
  const [search, setSearch] = useState('')

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (search.trim()) {
      router.push(`/?keyword=${encodeURIComponent(search.trim())}`)
    }
  }

  return (
    <div className="gradient-hero texture-overlay relative overflow-hidden">
      <div className="container-main py-20 md:py-28 relative z-10">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="max-w-2xl"
        >
          <div className="badge-organic mb-4 text-sm inline-flex">
            🌿 100% Hữu cơ & Tươi sạch
          </div>
          <h1
            className="text-4xl md:text-5xl lg:text-6xl font-bold text-white mb-4 leading-tight"
            style={{ fontFamily: 'var(--font-display)' }}
          >
            Nông sản tươi sạch
            <br />
            <span className="text-[var(--color-gold-400)]">từ trang trại</span>
          </h1>
          <p className="text-[var(--color-brown-200)] text-lg mb-8 leading-relaxed">
            Được tuyển chọn kỹ lưỡng từ những trang trại hữu cơ uy tín. Giao tận nơi, tươi ngon mỗi ngày.
          </p>

          <form onSubmit={handleSearch} className="flex gap-3">
            <div className="flex-1 relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-[var(--color-brown-400)]" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm rau, củ, quả hữu cơ..."
                className="w-full h-13 pl-12 pr-4 bg-white/95 backdrop-blur rounded-2xl text-[var(--color-text)] placeholder:text-[var(--color-text-subtle)] focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-400)] text-sm"
                style={{ height: '52px' }}
              />
            </div>
            <Button type="submit" variant="gold" size="lg" className="flex-shrink-0">
              Tìm kiếm
            </Button>
          </form>

          {/* Stats */}
          <div className="flex items-center gap-8 mt-10">
            {[
              { value: '200+', label: 'Sản phẩm' },
              { value: '50+', label: 'Trang trại' },
              { value: '10K+', label: 'Khách hàng' },
            ].map(({ value, label }) => (
              <div key={label}>
                <p className="text-2xl font-bold text-white" style={{ fontFamily: 'var(--font-display)' }}>
                  {value}
                </p>
                <p className="text-xs text-[var(--color-brown-300)] mt-0.5">{label}</p>
              </div>
            ))}
          </div>
        </motion.div>
      </div>

      {/* Decorative circles */}
      <div className="absolute -right-20 -top-20 w-80 h-80 rounded-full bg-white/5" />
      <div className="absolute -right-10 -bottom-10 w-60 h-60 rounded-full bg-[var(--color-gold-500)]/10" />
    </div>
  )
}

export default function HomePage() {
  return (
    <Suspense fallback={
      <div className="bg-[var(--color-cream)] min-h-screen flex items-center justify-center">
        <p className="text-[var(--color-text-muted)] text-sm">Đang tải trang chủ...</p>
      </div>
    }>
      <HomeContent />
    </Suspense>
  )
}