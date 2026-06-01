import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

/**
 * Hàm giải mã JWT payload mà không cần thư viện bên ngoài (an toàn cho Next.js Edge Runtime).
 */
function decodeJwt(token: string) {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null

    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch {
    return null
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Bảo vệ tất cả các tuyến đường bắt đầu bằng /admin
  if (pathname.startsWith('/admin')) {
    const accessToken = request.cookies.get('access_token')?.value

    // Trường hợp 1: Không có access token trong cookies -> Chưa đăng nhập
    if (!accessToken) {
      const loginUrl = new URL('/login', request.url)
      loginUrl.searchParams.set('redirect', pathname)
      return NextResponse.redirect(loginUrl)
    }

    // Trường hợp 2: Có access token -> Giải mã và kiểm tra vai trò ROLE_ADMIN
    const payload = decodeJwt(accessToken)
    const roles: string[] = payload?.roles || []

    if (!roles.includes('ROLE_ADMIN')) {
      // Nếu không có quyền ADMIN -> Chuyển hướng ra ngoài (về trang đăng nhập hoặc trang báo lỗi)
      const loginUrl = new URL('/login', request.url)
      loginUrl.searchParams.set('redirect', pathname)
      return NextResponse.redirect(loginUrl)
    }
  }

  return NextResponse.next()
}

// Cấu hình matcher cho middleware chạy trên các route admin
export const config = {
  matcher: ['/admin/:path*'],
}
