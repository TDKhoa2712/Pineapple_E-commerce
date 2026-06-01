'use client'

import { useEffect, useMemo, useState, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { SlidersHorizontal, Leaf } from 'lucide-react'
import { useProducts, useCategories, useAddToCart } from '@/hooks'
import {
  ProductCard, ProductCardSkeleton, Pagination, SectionHeader, EmptyState, Button
} from '@/components/ui'
import type { ProductSearchParams } from '@/types'
import { cn } from '@/lib/utils'

const SORT_OPTIONS = [
  { value: '', label: 'Mới nhất' },
  { value: 'price_asc', label: 'Giá tăng dần' },
  { value: 'price_desc', label: 'Giá giảm dần' },
  { value: 'rating', label: 'Đánh giá cao' },
  { value: 'sold', label: 'Bán chạy' },
]

function ProductsContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [page, setPage] = useState(0)

  const keyword = searchParams.get('keyword') || ''
  const categoryId = searchParams.get('categoryId') ? Number(searchParams.get('categoryId')) : undefined
  const isOrganic = searchParams.get('isOrganic') === 'true' ? true : undefined
  const sort = (searchParams.get('sort') as ProductSearchParams['sort']) || undefined

  const [expandedCategoryId, setExpandedCategoryId] = useState<number | null>(null)
  const params: ProductSearchParams = { page, size: 16, keyword: keyword || undefined, categoryId, isOrganic, sort }
  const { data: productsRes, isLoading } = useProducts(params)
  const { data: categoriesRes } = useCategories()
  const addToCart = useAddToCart()

  const products = productsRes?.data?.content ?? []
  const totalPages = productsRes?.data?.totalPages ?? 0
  const totalElements = productsRes?.data?.totalElements ?? 0
  const categories = useMemo(() => categoriesRes?.data ?? [], [categoriesRes?.data])

  const activeTopCategory = categories.find((cat) =>
    cat.id === categoryId || cat.children?.some((child) => child.id === categoryId)
  )
  const expandedCategory = expandedCategoryId
    ? categories.find((cat) => cat.id === expandedCategoryId)
    : activeTopCategory

  useEffect(() => {
    if (activeTopCategory) {
      setExpandedCategoryId(activeTopCategory.id)
    } else {
      setExpandedCategoryId(null)
    }
  }, [activeTopCategory, categoryId, categories])

  const updateFilter = (key: string, value: string | null) => {
    const next = new URLSearchParams(searchParams.toString())
    if (value) next.set(key, value); else next.delete(key)
    router.push(`/products?${next.toString()}`)
    setPage(0)
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10">
        {/* Categories + filter row */}
        <div className="flex items-center gap-2 overflow-x-auto pb-4 mb-6 no-scrollbar">
          <button onClick={() => updateFilter('categoryId', null)}
            className={cn('flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
              !categoryId ? 'bg-[var(--color-brown-900)] text-white border-[var(--color-brown-900)]'
              : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]')}>
            Tất cả
          </button>
          <button onClick={() => updateFilter('isOrganic', isOrganic ? null : 'true')}
            className={cn('flex-shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-medium border transition-all',
              isOrganic ? 'bg-[var(--color-green-600)] text-white border-[var(--color-green-600)]'
              : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-green-400)]')}>
            <Leaf className="w-3.5 h-3.5" /> Hữu cơ
          </button>
          {categories.map((cat) => {
            const isActive = categoryId === cat.id || cat.children?.some((child) => child.id === categoryId)
            return (
              <button key={cat.id}
                onClick={() => {
                  setExpandedCategoryId(cat.children?.length ? cat.id : null)
                  updateFilter('categoryId', String(cat.id))
                }}
                className={cn('flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
                  isActive
                    ? 'bg-[var(--color-brown-900)] text-white border-[var(--color-brown-900)]'
                    : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]')}>
                {cat.icon} {cat.name}
              </button>
            )
          })}
        </div>

        {expandedCategory?.children?.length ? (
          <div className="flex items-center gap-2 overflow-x-auto pb-4 mb-6 no-scrollbar">
            <button onClick={() => updateFilter('categoryId', String(expandedCategory.id))}
              className={cn('flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
                categoryId === expandedCategory.id
                  ? 'bg-[var(--color-brown-900)] text-white border-[var(--color-brown-900)]'
                  : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]')}>
              Tất cả {expandedCategory.name}
            </button>
            {expandedCategory.children.map((child) => (
              <button key={child.id}
                onClick={() => updateFilter('categoryId', String(child.id))}
                className={cn('flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium border transition-all',
                  categoryId === child.id
                    ? 'bg-[var(--color-brown-900)] text-white border-[var(--color-brown-900)]'
                    : 'bg-white text-[var(--color-text-muted)] border-[var(--color-border)] hover:border-[var(--color-brown-300)]')}>
                {child.name}
              </button>
            ))}
          </div>
        ) : null}

        {/* Toolbar */}
        <div className="flex items-center justify-between mb-6 gap-4 flex-wrap">
          <SectionHeader
            title={categoryId ? (categories.find((c) => c.id === categoryId)?.name ?? 'Sản phẩm') : 'Tất cả sản phẩm'}
            subtitle={`${totalElements} sản phẩm`} />
          <div className="flex items-center gap-2">
            <SlidersHorizontal className="w-4 h-4 text-[var(--color-text-muted)]" />
            <select value={sort || ''} onChange={(e) => updateFilter('sort', e.target.value || null)}
              className="h-9 px-3 pr-8 bg-white border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:border-[var(--color-gold-500)] appearance-none cursor-pointer">
              {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </div>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            {Array.from({ length: 16 }).map((_, i) => <ProductCardSkeleton key={i} />)}
          </div>
        ) : products.length === 0 ? (
          <EmptyState icon="🔍" title="Không tìm thấy sản phẩm"
            action={<Button onClick={() => router.push('/products')} variant="secondary">Xem tất cả</Button>} />
        ) : (
          <motion.div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            {products.map((product) => (
              <ProductCard key={product.id} product={product}
                onAddToCart={(p) => addToCart.mutate({ productId: p.id, quantity: 1 })}
                addingToCart={addToCart.isPending} />
            ))}
          </motion.div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </div>
  )
}

export default function ProductsPage() {
  return (
    <Suspense fallback={
      <div className="bg-[var(--color-cream)] min-h-screen flex items-center justify-center">
        <p className="text-[var(--color-text-muted)] text-sm">Đang tải danh sách sản phẩm...</p>
      </div>
    }>
      <ProductsContent />
    </Suspense>
  )
}