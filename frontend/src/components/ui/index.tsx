'use client'

import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'
import { Loader2, Star, Heart } from 'lucide-react'
import Link from 'next/link'
import Image from 'next/image'
import { motion } from 'framer-motion'
import { useToggleWishlist, useWishlistCheck } from '@/hooks'
import { useAuthStore } from '@/stores/auth-store'
import { formatPrice, calcDiscountPercent } from '@/lib/utils'
import type { ProductResponse } from '@/types'

// ─── Button ───────────────────────────────────────────────────────────────────
const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-gold-500)] focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed select-none',
  {
    variants: {
      variant: {
        primary:
          'bg-[var(--color-brown-900)] text-[var(--color-cream)] hover:bg-[var(--color-brown-800)] active:scale-[0.98] shadow-sm',
        secondary:
          'bg-[var(--color-brown-50)] text-[var(--color-brown-900)] hover:bg-[var(--color-brown-100)] border border-[var(--color-border)] active:scale-[0.98]',
        gold:
          'bg-[var(--color-gold-500)] text-[var(--color-brown-900)] hover:bg-[var(--color-gold-400)] active:scale-[0.98] shadow-sm font-semibold',
        ghost:
          'text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-brown-50)]',
        danger:
          'bg-red-500 text-white hover:bg-red-600 active:scale-[0.98] shadow-sm',
        outline:
          'border border-[var(--color-border)] text-[var(--color-text)] hover:bg-[var(--color-brown-50)] active:scale-[0.98]',
      },
      size: {
        xs: 'h-7 px-2.5 text-xs rounded-lg',
        sm: 'h-8 px-3 text-sm rounded-lg',
        md: 'h-10 px-4 text-sm',
        lg: 'h-12 px-6 text-base',
        xl: 'h-14 px-8 text-base',
        icon: 'h-9 w-9',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
)

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  loading?: boolean
  asChild?: boolean
  href?: string
  fullWidth?: boolean
}

export function Button({
  className,
  variant,
  size,
  loading,
  href,
  fullWidth,
  children,
  disabled,
  ...props
}: ButtonProps) {
  const classes = cn(buttonVariants({ variant, size }), fullWidth && 'w-full', className)

  if (href) {
    return (
      <Link href={href} className={classes}>
        {children}
      </Link>
    )
  }

  return (
    <button className={classes} disabled={disabled || loading} {...props}>
      {loading && <Loader2 className="w-4 h-4 animate-spin" />}
      {children}
    </button>
  )
}

// ─── Badge ────────────────────────────────────────────────────────────────────
const badgeVariants = cva(
  'inline-flex items-center gap-1 font-semibold rounded-full border',
  {
    variants: {
      variant: {
        default:  'bg-[var(--color-brown-50)] text-[var(--color-brown-700)] border-[var(--color-border)]',
        organic:  'bg-[var(--color-green-50)] text-[var(--color-green-600)] border-[var(--color-green-100)]',
        gold:     'bg-amber-50 text-amber-700 border-amber-200',
        success:  'bg-green-50 text-green-700 border-green-200',
        danger:   'bg-red-50 text-red-700 border-red-200',
        info:     'bg-blue-50 text-blue-700 border-blue-200',
        warning:  'bg-orange-50 text-orange-700 border-orange-200',
        purple:   'bg-purple-50 text-purple-700 border-purple-200',
        muted:    'bg-gray-50 text-gray-600 border-gray-200',
      },
      size: {
        sm: 'text-[10px] px-2 py-0.5',
        md: 'text-xs px-2.5 py-1',
        lg: 'text-sm px-3 py-1',
      },
    },
    defaultVariants: { variant: 'default', size: 'md' },
  }
)

export function Badge({
  className,
  variant,
  size,
  children,
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & VariantProps<typeof badgeVariants>) {
  return (
    <span className={cn(badgeVariants({ variant, size }), className)} {...props}>
      {children}
    </span>
  )
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────
export function Skeleton({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('skeleton', className)} {...props} />
}

export function ProductCardSkeleton() {
  return (
    <div className="bg-white rounded-2xl overflow-hidden border border-[var(--color-border)]">
      <Skeleton className="aspect-[4/3] w-full" />
      <div className="p-4 space-y-3">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-3 w-1/2" />
        <div className="flex items-center justify-between">
          <Skeleton className="h-5 w-24" />
          <Skeleton className="h-8 w-8 rounded-lg" />
        </div>
      </div>
    </div>
  )
}

// ─── StarRating ───────────────────────────────────────────────────────────────
export function StarRating({
  rating,
  max = 5,
  size = 16,
  showCount,
  count,
}: {
  rating: number
  max?: number
  size?: number
  showCount?: boolean
  count?: number
}) {
  return (
    <div className="flex items-center gap-1">
      <div className="flex">
        {Array.from({ length: max }).map((_, i) => (
          <Star
            key={i}
            size={size}
            className={cn(
              'transition-colors',
              i < Math.floor(rating)
                ? 'fill-[var(--color-gold-500)] text-[var(--color-gold-500)]'
                : i < rating
                ? 'fill-[var(--color-gold-300)] text-[var(--color-gold-300)]'
                : 'fill-[var(--color-brown-100)] text-[var(--color-brown-100)]'
            )}
          />
        ))}
      </div>
      {showCount && count !== undefined && (
        <span className="text-xs text-[var(--color-text-subtle)] ml-1">({count})</span>
      )}
    </div>
  )
}

// ─── ProductCard ──────────────────────────────────────────────────────────────
interface ProductCardProps {
  product: ProductResponse
  onAddToCart?: (product: ProductResponse) => void
  addingToCart?: boolean
}

export function ProductCard({ product, onAddToCart, addingToCart }: ProductCardProps) {
  const { isAuthenticated } = useAuthStore()
  const { data: wishlistCheckRes } = useWishlistCheck(product.id)
  const toggleWishlist = useToggleWishlist()

  const isWishlisted = wishlistCheckRes?.data ?? false
  const discountPct = product.discountPrice
    ? calcDiscountPercent(product.price, product.discountPrice)
    : 0

  return (
    <motion.div
      whileHover={{ y: -4 }}
      transition={{ duration: 0.2 }}
      className="group bg-white rounded-2xl overflow-hidden border border-[var(--color-border)] shadow-[var(--shadow-xs)] hover:shadow-[var(--shadow-md)] transition-shadow"
    >
      {/* Image */}
      <div className="relative aspect-[4/3] overflow-hidden bg-[var(--color-brown-50)]">
        <Link href={`/products/${product.slug}`}>
          <Image
            src={product.thumbnail || '/placeholder-product.jpg'}
            alt={product.name}
            fill
            className="object-cover group-hover:scale-105 transition-transform duration-500"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
          />
        </Link>

        {/* Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5">
          {product.isOrganic && (
            <Badge variant="organic" size="sm">
              🌿 Hữu cơ
            </Badge>
          )}
          {discountPct > 0 && (
            <Badge variant="danger" size="sm">
              -{discountPct}%
            </Badge>
          )}
          {product.status === 'OUT_OF_STOCK' && (
            <Badge variant="muted" size="sm">
              Hết hàng
            </Badge>
          )}
        </div>

        {/* Wishlist button */}
        {isAuthenticated && (
          <button
            onClick={(e) => {
              e.preventDefault()
              toggleWishlist.mutate(product.id)
            }}
            className={cn(
              'absolute top-3 right-3 w-8 h-8 rounded-full bg-white shadow-sm flex items-center justify-center transition-all z-10',
              isWishlisted
                ? 'text-red-500 opacity-100'
                : 'opacity-0 group-hover:opacity-100 translate-y-1 group-hover:translate-y-0 text-[var(--color-text-muted)] hover:text-red-500'
            )}
          >
            <Heart
              className="w-4 h-4 transition-all"
              fill={isWishlisted ? 'currentColor' : 'none'}
            />
          </button>
        )}
      </div>

      {/* Content */}
      <div className="p-4">
        <div className="mb-2">
          <p className="text-xs text-[var(--color-text-subtle)] mb-1">{product.categoryName}</p>
          <Link href={`/products/${product.slug}`}>
            <h3 className="font-semibold text-[var(--color-brown-900)] text-sm leading-snug line-clamp-2 hover:text-[var(--color-gold-600)] transition-colors">
              {product.name}
            </h3>
          </Link>
        </div>

        {product.averageRating ? (
          <div className="mb-3">
            <StarRating
              rating={product.averageRating}
              size={12}
              showCount
              count={product.reviewCount}
            />
          </div>
        ) : null}

        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="price-main text-base flex items-baseline gap-0.5">
              <span>{formatPrice(product.discountPrice ?? product.price)}</span>
              {product.unit && (
                <span className="text-xs font-normal text-[var(--color-text-muted)] ml-0.5">
                  / {product.unit}
                </span>
              )}
            </p>
            {product.discountPrice && (
              <p className="price-original text-xs">
                {formatPrice(product.price)}
              </p>
            )}
          </div>

          {onAddToCart && (
            <Button
              size="sm"
              variant="gold"
              onClick={() => onAddToCart(product)}
              loading={addingToCart}
              disabled={product.status === 'OUT_OF_STOCK'}
              className="flex-shrink-0"
            >
              {product.status === 'OUT_OF_STOCK' ? 'Hết' : '+'}
            </Button>
          )}
        </div>

        {product.farmName && (
          <p className="text-xs text-[var(--color-text-subtle)] mt-2 flex items-center gap-1">
            🌾 {product.farmName}
          </p>
        )}
      </div>
    </motion.div>
  )
}

// ─── Empty State ──────────────────────────────────────────────────────────────
export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: string
  title: string
  description?: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      {icon && <span className="text-5xl mb-4">{icon}</span>}
      <h3 className="text-lg font-semibold text-[var(--color-brown-900)] mb-2">{title}</h3>
      {description && (
        <p className="text-sm text-[var(--color-text-muted)] max-w-sm">{description}</p>
      )}
      {action && <div className="mt-6">{action}</div>}
    </div>
  )
}

// ─── Section Header ───────────────────────────────────────────────────────────
export function SectionHeader({
  title,
  subtitle,
  action,
}: {
  title: string
  subtitle?: string
  action?: React.ReactNode
}) {
  return (
    <div className="flex items-end justify-between mb-8">
      <div>
        <h2
          className="text-2xl md:text-3xl font-bold text-[var(--color-brown-900)]"
          style={{ fontFamily: 'var(--font-display)' }}
        >
          {title}
        </h2>
        {subtitle && (
          <p className="text-sm text-[var(--color-text-muted)] mt-1">{subtitle}</p>
        )}
      </div>
      {action && <div>{action}</div>}
    </div>
  )
}

// ─── Pagination ───────────────────────────────────────────────────────────────
export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null

  const pages = Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
    if (totalPages <= 7) return i
    if (page < 4) return i
    if (page >= totalPages - 4) return totalPages - 7 + i
    return page - 3 + i
  })

  return (
    <div className="flex items-center justify-center gap-1.5 mt-10">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
      >
        ← Trước
      </Button>

      {pages.map((p) => (
        <Button
          key={p}
          variant={p === page ? 'primary' : 'outline'}
          size="sm"
          onClick={() => onChange(p)}
          className="min-w-[36px]"
        >
          {p + 1}
        </Button>
      ))}

      <Button
        variant="outline"
        size="sm"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        Sau →
      </Button>
    </div>
  )
}

// ─── Input ────────────────────────────────────────────────────────────────────
export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  leftIcon?: React.ReactNode
}

export function Input({ label, error, hint, leftIcon, className, ...props }: InputProps) {
  return (
    <div className="space-y-1">
      {label && (
        <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
          {label}
        </label>
      )}
      <div className="relative">
        {leftIcon && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-subtle)]">
            {leftIcon}
          </span>
        )}
        <input
          className={cn(
            'w-full h-11 px-4 bg-white border rounded-xl text-sm text-[var(--color-text)] placeholder:text-[var(--color-text-subtle)]',
            'focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)] focus:ring-offset-0',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            'transition-all duration-150',
            error
              ? 'border-red-400 focus:ring-red-400'
              : 'border-[var(--color-border)] focus:border-[var(--color-gold-500)]',
            leftIcon && 'pl-10',
            className
          )}
          {...props}
        />
      </div>
      {error && <p className="text-xs text-red-500">{error}</p>}
      {hint && !error && <p className="text-xs text-[var(--color-text-subtle)]">{hint}</p>}
    </div>
  )
}

// ─── Divider ──────────────────────────────────────────────────────────────────
export function Divider({ label }: { label?: string }) {
  if (!label) {
    return <hr className="border-[var(--color-border)]" />
  }
  return (
    <div className="relative flex items-center gap-3">
      <div className="flex-1 border-t border-[var(--color-border)]" />
      <span className="text-xs text-[var(--color-text-subtle)] font-medium bg-white px-2">
        {label}
      </span>
      <div className="flex-1 border-t border-[var(--color-border)]" />
    </div>
  )
}