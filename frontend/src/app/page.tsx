import { redirect } from 'next/navigation'

// Root URL → user-facing homepage
// Admin panel is accessible at /admin/dashboard
export default function RootPage() {
  redirect('/products')
}