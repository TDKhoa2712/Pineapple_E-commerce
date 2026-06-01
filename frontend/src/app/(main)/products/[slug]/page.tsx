'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Image from 'next/image'
import Link from 'next/link'
import { motion, AnimatePresence } from 'framer-motion'
import { Heart, ShoppingCart, Minus, Plus, Star, ChevronRight, Leaf, Package, ThumbsUp, ThumbsDown } from 'lucide-react'
import { toast } from 'sonner'
import { useProduct, useProductReviews, useProductRating, useAddToCart, useToggleWishlist, useWishlistCheck, useRelatedProducts, useReviewEligibility, useVoteReview } from '@/hooks'
import { useAuthStore } from '@/stores/auth-store'
import { Button, Badge, StarRating, Skeleton, EmptyState, ProductCard, ProductCardSkeleton } from '@/components/ui'
import { ReviewModal } from '@/components/shared/ReviewModal'
import { formatPrice, calcDiscountPercent, relativeTime, getInitials } from '@/lib/utils'

export default function ProductDetailPage() {
  const { slug } = useParams<{ slug: string }>()
  const router = useRouter()
  const { isAuthenticated, user } = useAuthStore()
  const [qty, setQty] = useState(1)
  const [activeImg, setActiveImg] = useState(0)
  const [showReviewModal, setShowReviewModal] = useState(false)

  const { data: productRes, isLoading } = useProduct(slug)
  const product = productRes?.data

  const { data: reviewsRes } = useProductReviews(product?.id ?? 0)
  const { data: ratingRes } = useProductRating(product?.id ?? 0)
  const addToCart = useAddToCart()
  const toggleWishlist = useToggleWishlist()
  const { data: wishlistCheck } = useWishlistCheck(product?.id ?? 0)
  const { data: relatedProductsRes, isLoading: relatedLoading } = useRelatedProducts(product?.id ?? 0)

  const isWishlisted = wishlistCheck?.data ?? false
  const reviews = reviewsRes?.data?.content ?? []
  const rating = ratingRes?.data
  const { data: eligibilityRes } = useReviewEligibility(product?.id ?? 0)
  const isEligibleToReview = eligibilityRes?.data?.eligible ?? false
  const relatedProducts = relatedProductsRes?.data ?? []
  const isOutOfStock = product ? (product.status === 'OUT_OF_STOCK' || (product.totalStock ?? 0) <= 0) : true

  const voteReview = useVoteReview()

  const handleVote = (reviewId: number, helpful: boolean) => {
    if (!isAuthenticated) {
      toast.error('Vui lòng đăng nhập để bình chọn đánh giá')
      router.push('/login?redirect=/products/' + slug)
      return
    }
    voteReview.mutate({ reviewId, helpful })
  }

  const handleAddToCart = () => {
    if (!isAuthenticated) { router.push('/login?redirect=/products/' + slug); return }
    addToCart.mutate({ productId: product!.id, quantity: qty })
  }

  const handleToggleWishlist = () => {
    if (!isAuthenticated) { router.push('/login?redirect=/products/' + slug); return }
    toggleWishlist.mutate(product!.id)
  }

  if (isLoading) return <ProductDetailSkeleton />

  if (!product) return (
    <div className="container-main py-20">
      <EmptyState icon="🔍" title="Không tìm thấy sản phẩm"
        action={<Button href="/products">Quay lại</Button>} />
    </div>
  )

  const images = [product.thumbnail, ...(product.imageUrls ?? [])].filter(Boolean)
  const discountPct = product.discountPrice ? calcDiscountPercent(product.price, product.discountPrice) : 0

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      {/* Breadcrumb */}
      <div className="bg-white border-b border-[var(--color-border)]">
        <div className="container-main py-3 flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
          <Link href="/" className="hover:text-[var(--color-brown-900)]">Trang chủ</Link>
          <ChevronRight className="w-3 h-3" />
          <Link href="/products" className="hover:text-[var(--color-brown-900)]">Sản phẩm</Link>
          <ChevronRight className="w-3 h-3" />
          <span className="text-[var(--color-brown-900)] font-medium truncate max-w-[200px]">{product.name}</span>
        </div>
      </div>

      <div className="container-main py-10">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 mb-16">
          {/* Gallery */}
          <div>
            <div className="relative aspect-square rounded-2xl overflow-hidden bg-white border border-[var(--color-border)] mb-3">
              <Image src={images[activeImg] || '/placeholder.jpg'} alt={product.name}
                fill className="object-cover" sizes="(max-width: 1024px) 100vw, 50vw" priority />
              {discountPct > 0 && (
                <div className="absolute top-4 left-4">
                  <Badge variant="danger" size="lg">-{discountPct}%</Badge>
                </div>
              )}
              {product.isOrganic && (
                <div className="absolute top-4 right-4">
                  <Badge variant="organic" size="lg">🌿 Hữu cơ</Badge>
                </div>
              )}
            </div>
            {images.length > 1 && (
              <div className="flex gap-2">
                {images.map((img, i) => (
                  <button key={i} onClick={() => setActiveImg(i)}
                    className={`relative w-16 h-16 rounded-lg overflow-hidden border-2 transition-all ${i === activeImg ? 'border-[var(--color-gold-500)]' : 'border-[var(--color-border)] opacity-60 hover:opacity-100'}`}>
                    <Image src={img} alt="" fill className="object-cover" sizes="64px" />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Info */}
          <div>
            <p className="text-sm text-[var(--color-text-muted)] mb-1">
              <Link href={`/categories/${product.categoryId}`} className="hover:text-[var(--color-gold-600)]">
                {product.categoryName}
              </Link>
            </p>

            <h1 className="text-2xl md:text-3xl font-bold text-[var(--color-brown-900)] mb-3 leading-tight"
              style={{ fontFamily: 'var(--font-display)' }}>
              {product.name}
            </h1>

            {rating && (
              <div className="flex items-center gap-3 mb-4">
                <StarRating rating={rating.averageRating ?? 0} showCount count={rating.totalReviews ?? 0} />
                <span className="text-sm text-[var(--color-text-muted)]">
                  {(rating.averageRating ?? 0).toFixed(1)} / 5 — {rating.totalReviews ?? 0} đánh giá
                </span>
              </div>
            )}

            {/* Price */}
            <div className="flex items-baseline gap-3 mb-6">
              <span className="text-3xl font-black text-[var(--color-orange-500)]">
                {formatPrice(product.discountPrice ?? product.price)}
              </span>
              {product.discountPrice && (
                <span className="text-lg text-[var(--color-text-subtle)] line-through">
                  {formatPrice(product.price)}
                </span>
              )}
              <span className="text-sm text-[var(--color-text-muted)]">/ {product.unit}</span>
            </div>

            {/* Stock */}
            <div className="flex items-center gap-2 mb-6">
              <Package className="w-4 h-4 text-[var(--color-text-muted)]" />
              <span className="text-sm">
                {(product.totalStock ?? 0) > 0 ? (
                  <span className="text-[var(--color-green-600)] font-medium">
                    Còn {product.totalStock} {product.unit}
                  </span>
                ) : (
                  <span className="text-red-500 font-medium">Hết hàng</span>
                )}
              </span>
              {(product.soldCount ?? 0) > 0 && (
                <span className="text-[var(--color-text-subtle)] text-sm ml-2">
                  · Đã bán {product.soldCount}
                </span>
              )}
            </div>

            {product.farmName && (
              <div className="flex items-center gap-2 mb-4 p-3 bg-[var(--color-green-50)] rounded-xl border border-[var(--color-green-100)]">
                <span className="text-xl">🌾</span>
                <div>
                  <p className="text-xs text-[var(--color-text-muted)]">Trang trại</p>
                  <Link href={`/farms/${product.farmId}`}
                    className="text-sm font-semibold text-[var(--color-green-700)] hover:underline">
                    {product.farmName}
                  </Link>
                </div>
              </div>
            )}

            {/* Quantity + CTA */}
            <div className="flex items-center gap-4 mb-6">
              {!isOutOfStock && (
                <div className="flex items-center gap-1 bg-[var(--color-brown-50)] rounded-xl border border-[var(--color-border)] p-1">
                  <button onClick={() => setQty((q) => Math.max(1, q - 1))}
                    className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white transition-colors">
                    <Minus className="w-4 h-4" />
                  </button>
                  <input
                    type="number"
                    value={qty === 0 ? '' : qty}
                    onChange={(e) => {
                      const val = e.target.value
                      if (val === '') {
                        setQty(0)
                        return
                      }
                      const parsed = parseInt(val)
                      if (!isNaN(parsed)) {
                        setQty(Math.min(product.totalStock ?? 0, Math.max(1, parsed)))
                      }
                    }}
                    onBlur={() => {
                      if (qty < 1) {
                        setQty(1)
                      }
                    }}
                    className="w-10 text-center font-semibold text-[var(--color-brown-900)] bg-transparent border-none focus:outline-none focus:ring-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                  />
                  <button onClick={() => setQty((q) => Math.min(product.totalStock ?? 0, q + 1))}
                    className="w-8 h-8 flex items-center justify-center rounded-lg hover:bg-white transition-colors">
                    <Plus className="w-4 h-4" />
                  </button>
                </div>
              )}

              <Button variant="gold" size="lg" className="flex-1" onClick={handleAddToCart}
                loading={addToCart.isPending} disabled={isOutOfStock}>
                <ShoppingCart className="w-5 h-5" />
                {isOutOfStock ? 'Hết hàng' : 'Thêm vào giỏ'}
              </Button>

              <button onClick={handleToggleWishlist}
                className={`w-12 h-12 rounded-xl border flex items-center justify-center transition-all
                  ${isWishlisted
                    ? 'border-red-200 bg-red-50 text-red-500'
                    : 'border-[var(--color-border)] bg-white text-[var(--color-text-muted)] hover:text-red-500 hover:border-red-200'}`}>
                <Heart className="w-5 h-5" fill={isWishlisted ? 'currentColor' : 'none'} />
              </button>
            </div>

            {/* Tags */}
            <div className="flex flex-wrap gap-2">
              {product.isOrganic && <Badge variant="organic"><Leaf className="w-3 h-3" />Chứng nhận hữu cơ</Badge>}
              <Badge variant="default">Giao hỏa tốc</Badge>
              <Badge variant="default">Hoàn tiền 7 ngày</Badge>
            </div>
          </div>
        </div>

        {/* Description + Reviews */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <h2 className="text-xl font-bold text-[var(--color-brown-900)] mb-4"
              style={{ fontFamily: 'var(--font-display)' }}>
              Mô tả sản phẩm
            </h2>
            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6">
              <p className="text-sm text-[var(--color-text)] leading-relaxed whitespace-pre-wrap">
                {product.description || 'Chưa có mô tả.'}
              </p>
            </div>
          </div>

          {/* Rating summary */}
          {rating && (
            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-6 h-fit">
              <h3 className="font-bold text-[var(--color-brown-900)] mb-4">Đánh giá</h3>
              <div className="text-center mb-4">
                <p className="text-5xl font-black text-[var(--color-brown-900)]" style={{ fontFamily: 'var(--font-display)' }}>
                  {(rating.averageRating ?? 0).toFixed(1)}
                </p>
                <StarRating rating={rating.averageRating ?? 0} size={20} />
                <p className="text-sm text-[var(--color-text-muted)] mt-1">{rating.totalReviews ?? 0} đánh giá</p>
              </div>
              {Object.entries(rating.distribution ?? {}).reverse().map(([stars, count]) => (
                <div key={stars} className="flex items-center gap-2 mb-1.5">
                  <span className="text-xs text-[var(--color-text-muted)] w-4">{stars}</span>
                  <Star className="w-3 h-3 text-[var(--color-gold-500)] fill-[var(--color-gold-500)]" />
                  <div className="flex-1 h-2 bg-[var(--color-brown-100)] rounded-full overflow-hidden">
                    <div className="h-full bg-[var(--color-gold-500)] rounded-full"
                      style={{ width: rating.totalReviews ? `${(Number(count) / rating.totalReviews) * 100}%` : '0%' }} />
                  </div>
                  <span className="text-xs text-[var(--color-text-muted)] w-6 text-right">{count}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Reviews section */}
        <div className="mt-16 border-t border-[var(--color-border)] pt-12">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-bold text-[var(--color-brown-900)]"
              style={{ fontFamily: 'var(--font-display)' }}>
              Nhận xét từ khách hàng
            </h2>
            {isAuthenticated && isEligibleToReview && (
              <Button variant="gold" size="sm" onClick={() => setShowReviewModal(true)}>
                Viết đánh giá
              </Button>
            )}
          </div>

          {reviews.length === 0 ? (
            <div className="bg-white rounded-2xl border border-[var(--color-border)] p-8 text-center">
              <p className="text-sm text-[var(--color-text-muted)]">Chưa có nhận xét nào cho sản phẩm này.</p>
              {isAuthenticated ? (
                isEligibleToReview && (
                  <Button variant="gold" size="sm" className="mt-4" onClick={() => setShowReviewModal(true)}>
                    Viết đánh giá đầu tiên
                  </Button>
                )
              ) : (
                <p className="text-xs text-[var(--color-text-subtle)] mt-2">
                  Bạn cần <Link href={`/login?redirect=/products/${slug}`} className="text-[var(--color-gold-600)] hover:underline">đăng nhập</Link> để đánh giá sản phẩm.
                </p>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              {reviews.map((review) => (
                <div key={review.id} className="bg-white rounded-2xl border border-[var(--color-border)] p-5">
                  <div className="flex items-start gap-3">
                    <div className="w-9 h-9 rounded-full bg-[var(--color-gold-500)] flex items-center justify-center text-[var(--color-brown-900)] font-bold text-sm flex-shrink-0">
                      {review.userAvatar ? (
                        <Image src={review.userAvatar} alt={review.userFullName} width={36} height={36} className="rounded-full" />
                      ) : getInitials(review.userFullName)}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center justify-between">
                        <p className="font-semibold text-sm text-[var(--color-brown-900)]">{review.userFullName}</p>
                        <span className="text-xs text-[var(--color-text-subtle)]">{relativeTime(review.createdAt)}</span>
                      </div>
                      <StarRating rating={review.rating} size={12} />
                      {review.comment && (
                        <p className="text-sm text-[var(--color-text)] mt-2 leading-relaxed">{review.comment}</p>
                      )}
                      {review.imageUrls && review.imageUrls.length > 0 && (
                        <div className="mt-3 flex gap-2 flex-wrap">
                          {review.imageUrls.map((url, index) => (
                            <a key={index} href={url} target="_blank" rel="noopener noreferrer" className="relative w-16 h-16 rounded-xl overflow-hidden border border-[var(--color-border)] bg-[var(--color-brown-50)] hover:scale-105 transition-transform">
                              <Image src={url} alt="" fill className="object-cover" sizes="64px" />
                            </a>
                          ))}
                        </div>
                      )}

                      {/* Nút Like/Dislike cho đánh giá */}
                      <div className="mt-4 flex items-center gap-3">
                        <button
                          onClick={() => handleVote(review.id, true)}
                          disabled={user?.id === review.userId}
                          className={`flex items-center gap-1.5 text-xs font-semibold py-1.5 px-3 rounded-xl border transition-all duration-200 cursor-pointer
                            ${user?.id === review.userId
                              ? 'opacity-40 cursor-not-allowed border-gray-100 bg-gray-50 text-[var(--color-text-subtle)]'
                              : review.userVote === true
                                ? 'border-[var(--color-gold-500)] bg-[var(--color-gold-50)] text-[var(--color-gold-600)] scale-[1.02]'
                                : 'border-[var(--color-border)] bg-white text-[var(--color-text-muted)] hover:border-[var(--color-gold-400)] hover:text-[var(--color-gold-600)] hover:bg-[var(--color-gold-50)]/10'
                            }`}
                          title={user?.id === review.userId ? 'Bạn không thể bình chọn đánh giá của chính mình' : 'Hữu ích'}
                        >
                          <ThumbsUp className={`w-3.5 h-3.5 transition-transform duration-200 ${review.userVote === true ? 'fill-current scale-110 text-[var(--color-gold-600)]' : ''}`} />
                          <span>Hữu ích ({review.helpfulCount ?? 0})</span>
                        </button>

                        <button
                          onClick={() => handleVote(review.id, false)}
                          disabled={user?.id === review.userId}
                          className={`flex items-center gap-1.5 text-xs font-semibold py-1.5 px-3 rounded-xl border transition-all duration-200 cursor-pointer
                            ${user?.id === review.userId
                              ? 'opacity-40 cursor-not-allowed border-gray-100 bg-gray-50 text-[var(--color-text-subtle)]'
                              : review.userVote === false
                                ? 'border-red-300 bg-red-50 text-red-600 scale-[1.02]'
                                : 'border-[var(--color-border)] bg-white text-[var(--color-text-muted)] hover:border-red-300 hover:text-red-600 hover:bg-red-50/10'
                            }`}
                          title={user?.id === review.userId ? 'Bạn không thể bình chọn đánh giá của chính mình' : 'Không hữu ích'}
                        >
                          <ThumbsDown className={`w-3.5 h-3.5 transition-transform duration-200 ${review.userVote === false ? 'fill-current scale-110 text-red-600' : ''}`} />
                          <span>Không hữu ích ({review.unhelpfulCount ?? 0})</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <AnimatePresence>
          {showReviewModal && (
            <ReviewModal
              product={{
                id: product.id,
                name: product.name,
                thumbnail: product.thumbnail,
              }}
              onClose={() => setShowReviewModal(false)}
            />
          )}
        </AnimatePresence>

        {/* Related Products */}
        <div className="mt-16 border-t border-[var(--color-border)] pt-12">
          <h2 className="text-2xl font-bold text-[var(--color-brown-900)] mb-6"
            style={{ fontFamily: 'var(--font-display)' }}>
            Sản phẩm tương tự
          </h2>
          
          {relatedLoading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-5">
              {Array.from({ length: 4 }).map((_, i) => (
                <ProductCardSkeleton key={i} />
              ))}
            </div>
          ) : relatedProducts.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">Chưa có sản phẩm tương tự.</p>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 md:gap-5">
              {relatedProducts.slice(0, 4).map((item) => (
                <ProductCard
                  key={item.id}
                  product={item}
                  onAddToCart={(p) => addToCart.mutate({ productId: p.id, quantity: 1 })}
                  addingToCart={addToCart.isPending}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function ProductDetailSkeleton() {
  return (
    <div className="container-main py-10">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <Skeleton className="aspect-square rounded-2xl" />
        <div className="space-y-4">
          <Skeleton className="h-4 w-24" />
          <Skeleton className="h-8 w-3/4" />
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-10 w-40" />
          <Skeleton className="h-12 w-full" />
        </div>
      </div>
    </div>
  )
}