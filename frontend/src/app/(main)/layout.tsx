import { Navbar } from '@/components/layout/Navbar'
import { Footer } from '@/components/layout/Footer'
import { AppBootstrap } from '@/components/layout/AppBootstrap'

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <AppBootstrap>
      <Navbar />
      <main className="min-h-screen pt-[var(--nav-height)]">{children}</main>
      <Footer />
    </AppBootstrap>
  )
}