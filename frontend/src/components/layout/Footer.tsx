import Link from 'next/link'
import { Leaf, Mail, Phone, MapPin, Facebook, Instagram, Youtube } from 'lucide-react'

const FOOTER_LINKS = {
  shop: [
    { label: 'Tất cả sản phẩm', href: '/products' },
    { label: 'Hàng hữu cơ', href: '/products?isOrganic=true' },
    { label: 'Trang trại', href: '/farms' },
    { label: 'Ưu đãi hôm nay', href: '/products?sort=newest' },
  ],
  support: [
    { label: 'Trung tâm hỗ trợ', href: '#' },
    { label: 'Chính sách đổi trả', href: '#' },
    { label: 'Theo dõi đơn hàng', href: '/orders' },
    { label: 'Vận chuyển & Giao hàng', href: '#' },
  ],
  company: [
    { label: 'Về chúng tôi', href: '#' },
    { label: 'Câu chuyện thương hiệu', href: '#' },
    { label: 'Tuyển dụng', href: '#' },
    { label: 'Blog nông nghiệp', href: '#' },
  ],
}

export function Footer() {
  return (
    <footer className="bg-[var(--color-brown-950)] text-[var(--color-brown-200)]">
      {/* Main footer */}
      <div className="container-main py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-12">
          {/* Brand */}
          <div className="lg:col-span-2">
            <Link href="/" className="flex items-center gap-2 mb-4">
              <span className="text-2xl">🍍</span>
              <span className="font-display text-xl font-bold text-[var(--color-cream)]">
                Pineapple
              </span>
            </Link>
            <p className="text-sm leading-relaxed mb-6 text-[var(--color-brown-300)]">
              Nông sản hữu cơ tươi sạch, được tuyển chọn kỹ lưỡng từ những trang trại uy tín.
              Mang thiên nhiên đến bàn ăn của bạn mỗi ngày.
            </p>
            <div className="flex items-center gap-2 mb-2 text-sm">
              <MapPin className="w-4 h-4 text-[var(--color-gold-500)] flex-shrink-0" />
              <span>123 Đường Nông Nghiệp, Quận 1, TP.HCM</span>
            </div>
            <div className="flex items-center gap-2 mb-2 text-sm">
              <Phone className="w-4 h-4 text-[var(--color-gold-500)] flex-shrink-0" />
              <span>1800 PINE (7463)</span>
            </div>
            <div className="flex items-center gap-2 text-sm">
              <Mail className="w-4 h-4 text-[var(--color-gold-500)] flex-shrink-0" />
              <span>hello@pineapple.vn</span>
            </div>

            {/* Social */}
            <div className="flex items-center gap-3 mt-6">
              {[
                { icon: <Facebook className="w-4 h-4" />, label: 'Facebook' },
                { icon: <Instagram className="w-4 h-4" />, label: 'Instagram' },
                { icon: <Youtube className="w-4 h-4" />, label: 'Youtube' },
              ].map(({ icon, label }) => (
                <button
                  key={label}
                  aria-label={label}
                  className="w-9 h-9 rounded-lg bg-white/10 flex items-center justify-center hover:bg-[var(--color-gold-500)] hover:text-[var(--color-brown-900)] transition-colors"
                >
                  {icon}
                </button>
              ))}
            </div>
          </div>

          {/* Links */}
          <FooterLinkGroup title="Mua sắm" links={FOOTER_LINKS.shop} />
          <FooterLinkGroup title="Hỗ trợ" links={FOOTER_LINKS.support} />
          <FooterLinkGroup title="Công ty" links={FOOTER_LINKS.company} />
        </div>

        {/* Organic badge strip */}
        <div className="mt-12 pt-8 border-t border-white/10">
          <div className="flex flex-wrap items-center gap-4">
            {['100% Hữu cơ', 'Giao hàng hôm nay', 'Hoàn tiền 7 ngày', 'Hỗ trợ 24/7'].map(
              (badge) => (
                <div
                  key={badge}
                  className="flex items-center gap-1.5 text-xs font-medium text-[var(--color-green-400)]"
                >
                  <Leaf className="w-3.5 h-3.5" />
                  {badge}
                </div>
              )
            )}
          </div>
        </div>
      </div>

      {/* Bottom bar */}
      <div className="border-t border-white/10">
        <div className="container-main py-4 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-[var(--color-brown-400)]">
          <p>© 2026 Pineapple E-Commerce. Bảo lưu mọi quyền.</p>
          <div className="flex items-center gap-4">
            <Link href="#" className="hover:text-[var(--color-cream)] transition-colors">Chính sách bảo mật</Link>
            <Link href="#" className="hover:text-[var(--color-cream)] transition-colors">Điều khoản sử dụng</Link>
          </div>
        </div>
      </div>
    </footer>
  )
}

function FooterLinkGroup({
  title,
  links,
}: {
  title: string
  links: { label: string; href: string }[]
}) {
  return (
    <div>
      <h3 className="text-sm font-semibold text-[var(--color-cream)] uppercase tracking-wider mb-4">
        {title}
      </h3>
      <ul className="space-y-2.5">
        {links.map(({ label, href }) => (
          <li key={label}>
            <Link
              href={href}
              className="text-sm text-[var(--color-brown-300)] hover:text-[var(--color-cream)] transition-colors"
            >
              {label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}