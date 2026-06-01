'use client'

import { useState } from 'react'
import Image from 'next/image'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { MapPin, Package, Search } from 'lucide-react'
import { useFarms } from '@/hooks'
import { Button, Skeleton, EmptyState, Pagination } from '@/components/ui'

export default function FarmsPage() {
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const { data: farmsRes, isLoading } = useFarms({ page, size: 12 })

  const farms = farmsRes?.data?.content ?? []
  const totalPages = farmsRes?.data?.totalPages ?? 0

  const filtered = keyword
    ? farms.filter((f) => f.name.toLowerCase().includes(keyword.toLowerCase()) || f.location.toLowerCase().includes(keyword.toLowerCase()))
    : farms

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      {/* Hero */}
      <div className="bg-[var(--color-green-700)] relative overflow-hidden">
        <div className="container-main py-16 relative z-10">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
            <p className="badge-organic mb-3 text-sm">🌾 Trang trại đối tác</p>
            <h1 className="text-4xl md:text-5xl font-bold text-white mb-3"
              style={{ fontFamily: 'var(--font-display)' }}>
              Những trang trại<br />
              <span className="text-[var(--color-gold-400)]">uy tín nhất</span>
            </h1>
            <p className="text-[var(--color-green-100)] text-lg max-w-xl">
              Tất cả đối tác của Pineapple đều được kiểm tra chất lượng và đạt chứng nhận hữu cơ.
            </p>
          </motion.div>
        </div>
        <div className="absolute -right-20 -top-20 w-80 h-80 rounded-full bg-white/5" />
        <div className="absolute -left-10 -bottom-10 w-60 h-60 rounded-full bg-[var(--color-gold-500)]/10" />
      </div>

      <div className="container-main py-10">
        {/* Search */}
        <div className="relative max-w-md mb-8">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-subtle)]" />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="Tìm theo tên hoặc tỉnh thành..."
            className="w-full h-11 pl-10 pr-4 bg-white border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:border-[var(--color-gold-500)]"
          />
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <Skeleton key={i} className="h-64 rounded-2xl" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState icon="🌾" title="Không tìm thấy trang trại"
            action={<Button onClick={() => setKeyword('')} variant="secondary">Xoá bộ lọc</Button>} />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {filtered.map((farm, i) => (
              <motion.div key={farm.id}
                initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                className="group bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden hover:shadow-[var(--shadow-md)] transition-shadow">
                <div className="relative h-44 overflow-hidden bg-[var(--color-green-50)]">
                  {farm.imageUrl ? (
                    <Image src={farm.imageUrl} alt={farm.name} fill
                      className="object-cover group-hover:scale-105 transition-transform duration-500"
                      sizes="(max-width:640px) 100vw, (max-width:1024px) 50vw, 33vw" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-6xl">🌾</div>
                  )}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent" />
                  <div className="absolute bottom-3 left-3 right-3">
                    <div className="flex items-center gap-1.5 text-xs text-white/90">
                      <MapPin className="w-3 h-3" />
                      {farm.location}
                    </div>
                  </div>
                </div>
                <div className="p-5">
                  <h3 className="font-bold text-[var(--color-brown-900)] mb-1 text-lg"
                    style={{ fontFamily: 'var(--font-display)' }}>
                    {farm.name}
                  </h3>
                  {farm.description && (
                    <p className="text-sm text-[var(--color-text-muted)] line-clamp-2 mb-3">
                      {farm.description}
                    </p>
                  )}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 text-xs text-[var(--color-text-muted)]">
                      <Package className="w-3.5 h-3.5" />
                      Sản phẩm sạch
                    </div>
                    <Link href={`/farms/${farm.id}`}>
                      <Button variant="secondary" size="sm">Xem trang trại →</Button>
                    </Link>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </div>
  )
}