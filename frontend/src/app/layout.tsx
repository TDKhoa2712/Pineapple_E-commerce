import type { Metadata, Viewport } from 'next'
import { Playfair_Display, DM_Sans } from 'next/font/google'
import { Toaster } from 'sonner'
import { Providers } from '@/components/shared/providers'
import './globals.css'

const playfairDisplay = Playfair_Display({
  subsets: ['latin', 'vietnamese'],
  variable: '--font-display',
  display: 'swap',
  weight: ['400', '600', '700'],
  style: ['normal', 'italic'],
})

const dmSans = DM_Sans({
  subsets: ['latin', 'latin-ext'],
  variable: '--font-body',
  display: 'swap',
  weight: ['300', '400', '500', '600', '700'],
})

export const metadata: Metadata = {
  title: {
    default: 'Pineapple — Nông sản tươi sạch',
    template: '%s | Pineapple',
  },
  description: 'Nông sản hữu cơ, tươi sạch từ trang trại đến bàn ăn của bạn.',
  keywords: ['nông sản', 'organic', 'hữu cơ', 'tươi sạch', 'thực phẩm sạch'],
  authors: [{ name: 'Pineapple Team' }],
  metadataBase: new URL(
    process.env.NEXT_PUBLIC_SITE_URL ||
    (process.env.VERCEL_URL ? `https://${process.env.VERCEL_URL}` : 'http://localhost:3000')
  ),
  openGraph: {
    title: 'Pineapple — Nông sản tươi sạch',
    description: 'Nông sản hữu cơ, tươi sạch từ trang trại đến bàn ăn của bạn.',
    type: 'website',
    locale: 'vi_VN',
  },
}

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: '#4a2e1a',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="vi" className={`${playfairDisplay.variable} ${dmSans.variable}`} suppressHydrationWarning>
      <body suppressHydrationWarning>
        <Providers>
          {children}
          <Toaster
            richColors
            position="top-right"
            toastOptions={{
              style: {
                fontFamily: 'var(--font-body)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border)',
              },
              classNames: {
                toast: 'font-body',
                success: 'toast-success',
                error: 'toast-error',
              },
            }}
          />
        </Providers>
      </body>
    </html>
  )
}