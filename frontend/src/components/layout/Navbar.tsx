'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Search, ShoppingCart, Heart, User, Menu, X,
  ChevronDown, LogOut, Package, MapPin, Settings, Leaf
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { useCartStore } from '@/stores/cart-store'
import { useLogout } from '@/hooks'
import { cn, getInitials } from '@/lib/utils'
import Image from 'next/image'
import { ROUTES } from '@/lib/routes'

export function Navbar() {
  const pathname = usePathname()
  const router = useRouter()
  const [searchValue, setSearchValue] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)

  const { user, isAuthenticated } = useAuthStore()
  const { itemCount } = useCartStore()
  const logoutMutation = useLogout()

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 10)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  // Close menus on route change
  useEffect(() => {
    setMenuOpen(false)
    setUserMenuOpen(false)
  }, [pathname])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (searchValue.trim()) {
      router.push(`${ROUTES.PRODUCTS}?keyword=${encodeURIComponent(searchValue.trim())}`)
    }
  }

  return (
    <>
      <header
        className={cn(
          'fixed top-0 left-0 right-0 z-[200] transition-all duration-300',
          scrolled
            ? 'bg-white/95 backdrop-blur-md shadow-sm border-b border-[var(--color-border)]'
            : 'bg-white border-b border-[var(--color-border)]'
        )}
        style={{ height: 'var(--nav-height)' }}
      >
        <div className="container-main h-full flex items-center gap-4">
          {/* Logo */}
          <Link href={ROUTES.HOME} className="flex items-center gap-2 flex-shrink-0 group">
            <span className="text-2xl">🍍</span>
            <span
              className="font-display text-xl font-bold text-[var(--color-brown-900)] tracking-tight"
              style={{ fontFamily: 'var(--font-display)' }}
            >
              Pineapple
            </span>
          </Link>

          {/* Category nav — desktop */}
          <nav className="hidden lg:flex items-center gap-1 ml-4">
            <Link
              href={ROUTES.PRODUCTS}
              className={cn(
                'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                pathname === ROUTES.PRODUCTS
                  ? 'text-[var(--color-gold-600)] bg-[var(--color-brown-50)]'
                  : 'text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-brown-50)]'
              )}
            >
              Sản phẩm
            </Link>
            <Link
              href={ROUTES.FARMS}
              className={cn(
                'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                pathname.startsWith(ROUTES.FARMS)
                  ? 'text-[var(--color-gold-600)] bg-[var(--color-brown-50)]'
                  : 'text-[var(--color-text-muted)] hover:text-[var(--color-text)] hover:bg-[var(--color-brown-50)]'
              )}
            >
              Trang trại
            </Link>
          </nav>

          {/* Search — desktop */}
          <form
            onSubmit={handleSearch}
            className="hidden md:flex items-center flex-1 max-w-sm ml-auto"
          >
            <div className="relative w-full">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-subtle)]" />
              <input
                type="search"
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
                placeholder="Tìm nông sản..."
                className="w-full h-9 pl-9 pr-4 bg-[var(--color-brown-50)] border border-[var(--color-border)] rounded-full text-sm focus:outline-none focus:border-[var(--color-gold-500)] focus:bg-white transition-all"
              />
            </div>
          </form>

          {/* Right actions */}
          <div className="flex items-center gap-1 ml-auto md:ml-0">
            {/* Wishlist */}
            {isAuthenticated && (
              <Link
                href={ROUTES.WISHLIST}
                className="p-2 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-brown-900)] hover:bg-[var(--color-brown-50)] transition-colors"
              >
                <Heart className="w-5 h-5" />
              </Link>
            )}

            {/* Cart */}
            <Link
              href={ROUTES.CART}
              className="relative p-2 rounded-lg text-[var(--color-text-muted)] hover:text-[var(--color-brown-900)] hover:bg-[var(--color-brown-50)] transition-colors"
            >
              <ShoppingCart className="w-5 h-5" />
              {itemCount > 0 && (
                <motion.span
                  initial={{ scale: 0 }}
                  animate={{ scale: 1 }}
                  className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-[var(--color-orange-500)] text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1"
                >
                  {itemCount > 99 ? '99+' : itemCount}
                </motion.span>
              )}
            </Link>

            {/* User menu */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setUserMenuOpen((v) => !v)}
                  className="flex items-center gap-1.5 pl-1 pr-2 py-1 rounded-lg hover:bg-[var(--color-brown-50)] transition-colors"
                >
                  <div className="w-8 h-8 rounded-full overflow-hidden bg-[var(--color-gold-500)] flex items-center justify-center flex-shrink-0">
                    {user?.avatar ? (
                      <Image
                        src={user.avatar}
                        alt={user.fullName}
                        width={32}
                        height={32}
                        className="object-cover w-full h-full"
                      />
                    ) : (
                      <span className="text-[var(--color-brown-900)] text-xs font-bold">
                        {getInitials(user?.fullName ?? 'U')}
                      </span>
                    )}
                  </div>
                  <ChevronDown className="w-3.5 h-3.5 text-[var(--color-text-subtle)] hidden sm:block" />
                </button>

                <AnimatePresence>
                  {userMenuOpen && (
                    <>
                      <div
                        className="fixed inset-0 z-[300]"
                        onClick={() => setUserMenuOpen(false)}
                      />
                      <motion.div
                        initial={{ opacity: 0, y: 8, scale: 0.96 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, y: 8, scale: 0.96 }}
                        transition={{ duration: 0.15 }}
                        className="absolute right-0 top-full mt-2 w-52 bg-white rounded-xl shadow-[var(--shadow-lg)] border border-[var(--color-border)] overflow-hidden z-[400]"
                      >
                        <div className="px-4 py-3 bg-[var(--color-brown-50)] border-b border-[var(--color-border)]">
                          <p className="text-sm font-semibold text-[var(--color-brown-900)] truncate">
                            {user?.fullName}
                          </p>
                          <p className="text-xs text-[var(--color-text-subtle)] truncate mt-0.5">
                            {user?.email}
                          </p>
                        </div>
                        <div className="p-1">
                          <UserMenuItem href={ROUTES.PROFILE} icon={<User className="w-4 h-4" />} label="Thông tin cá nhân" />
                          <UserMenuItem href={ROUTES.ORDERS} icon={<Package className="w-4 h-4" />} label="Đơn hàng của tôi" />
                          <UserMenuItem href={ROUTES.WISHLIST} icon={<Heart className="w-4 h-4" />} label="Danh sách yêu thích" />
                          <UserMenuItem href={ROUTES.ADDRESSES} icon={<MapPin className="w-4 h-4" />} label="Địa chỉ của tôi" />
                          {user?.roles?.includes('ROLE_FARMER') && (
                            <UserMenuItem href={ROUTES.FARMER.FARMS} icon={<Leaf className="w-4 h-4" />} label="Trang trại của tôi" />
                          )}
                          {user?.roles?.includes('ROLE_ADMIN') && (
                            <UserMenuItem href={ROUTES.ADMIN.DASHBOARD} icon={<Settings className="w-4 h-4" />} label="Quản trị" />
                          )}
                        </div>
                        <div className="p-1 border-t border-[var(--color-border)]">
                          <button
                            onClick={() => logoutMutation.mutate()}
                            className="w-full flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-red-600 hover:bg-red-50 transition-colors"
                          >
                            <LogOut className="w-4 h-4" />
                            Đăng xuất
                          </button>
                        </div>
                      </motion.div>
                    </>
                  )}
                </AnimatePresence>
              </div>
            ) : (
              <Link
                href={ROUTES.LOGIN}
                className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 bg-[var(--color-brown-900)] text-[var(--color-cream)] rounded-lg text-sm font-medium hover:bg-[var(--color-brown-800)] transition-colors"
              >
                Đăng nhập
              </Link>
            )}

            {/* Mobile menu toggle */}
            <button
              className="lg:hidden p-2 rounded-lg text-[var(--color-text-muted)] hover:bg-[var(--color-brown-50)]"
              onClick={() => setMenuOpen((v) => !v)}
            >
              {menuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </header>

      {/* Mobile menu */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="fixed top-[var(--nav-height)] left-0 right-0 z-[190] bg-white border-b border-[var(--color-border)] shadow-lg lg:hidden"
          >
            <div className="container-main py-4 space-y-1">
              <form onSubmit={handleSearch} className="mb-4">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--color-text-subtle)]" />
                  <input
                    type="search"
                    value={searchValue}
                    onChange={(e) => setSearchValue(e.target.value)}
                    placeholder="Tìm nông sản..."
                    className="w-full h-10 pl-9 pr-4 bg-[var(--color-brown-50)] border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:border-[var(--color-gold-500)]"
                  />
                </div>
              </form>
              <MobileNavItem href={ROUTES.PRODUCTS} label="Sản phẩm" />
              <MobileNavItem href={ROUTES.FARMS} label="Trang trại" />
              {!isAuthenticated && (
                <div className="pt-2 border-t border-[var(--color-border)]">
                  <Link
                    href={ROUTES.LOGIN}
                    className="block w-full text-center py-2.5 bg-[var(--color-brown-900)] text-white rounded-xl text-sm font-medium"
                  >
                    Đăng nhập
                  </Link>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  )
}

function UserMenuItem({ href, icon, label }: { href: string; icon: React.ReactNode; label: string }) {
  return (
    <Link
      href={href}
      className="flex items-center gap-3 px-3 py-2 rounded-lg text-sm text-[var(--color-text)] hover:bg-[var(--color-brown-50)] transition-colors"
    >
      <span className="text-[var(--color-text-muted)]">{icon}</span>
      {label}
    </Link>
  )
}

function MobileNavItem({ href, label }: { href: string; label: string }) {
  const pathname = usePathname()
  return (
    <Link
      href={href}
      className={cn(
        'block px-4 py-2.5 rounded-xl text-sm font-medium transition-colors',
        pathname === href
          ? 'bg-[var(--color-brown-50)] text-[var(--color-gold-600)]'
          : 'text-[var(--color-text-muted)] hover:bg-[var(--color-brown-50)] hover:text-[var(--color-text)]'
      )}
    >
      {label}
    </Link>
  )
}