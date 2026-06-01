"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import {
  LayoutDashboard,
  ShoppingCart,
  Users,
  Store,
  Tag,
  Star,
  Package,
  ChevronLeft,
  Leaf,
  LogOut,
  FolderTree,
  PackageSearch,
  Home,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useUIStore } from "@/stores/ui-store";
import { useAuthStore } from "@/stores/auth-store";
import { authService } from "@/services/auth.service";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { ROUTES } from "@/lib/routes";

const navItems = [
  {
    label: "Tổng quan",
    href: ROUTES.ADMIN.DASHBOARD,
    icon: LayoutDashboard,
  },
  {
    label: "Đơn hàng",
    href: ROUTES.ADMIN.ORDERS,
    icon: ShoppingCart,
  },
  {
    label: "Người dùng",
    href: ROUTES.ADMIN.USERS,
    icon: Users,
  },
  {
    label: "Nông trại",
    href: ROUTES.ADMIN.FARMS,
    icon: Store,
  },
  {
    label: "Sản phẩm",
    href: ROUTES.ADMIN.PRODUCTS,
    icon: PackageSearch,
  },
  {
    label: "Danh mục",
    href: ROUTES.ADMIN.CATEGORIES,
    icon: FolderTree,
  },
  {
    label: "Mã giảm giá",
    href: ROUTES.ADMIN.COUPONS,
    icon: Tag,
  },
  {
    label: "Đánh giá",
    href: ROUTES.ADMIN.REVIEWS,
    icon: Star,
  },
  {
    label: "Kho hàng",
    href: ROUTES.ADMIN.INVENTORY,
    icon: Package,
  },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const { sidebarOpen, toggleSidebar, setAdminProfileOpen } = useUIStore();
  const { logout, user } = useAuthStore();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await authService.logout();
    } catch {
      // ignore
    } finally {
      logout();
      router.push(ROUTES.LOGIN);
      toast.success("Đã đăng xuất");
    }
  };

  return (
    <motion.aside
      animate={{ width: sidebarOpen ? 260 : 72 }}
      transition={{ type: "spring", stiffness: 300, damping: 30 }}
      className="relative flex h-full flex-col border-r border-slate-800 bg-slate-900"
    >
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-slate-800 px-4">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl gradient-pine shadow-lg shadow-pine-500/20">
          <Leaf className="h-5 w-5 text-white" />
        </div>
        <AnimatePresence>
          {sidebarOpen && (
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -10 }}
              transition={{ duration: 0.15 }}
              className="flex flex-col"
            >
              <span className="font-display text-sm font-bold text-white">
                Pineapple
              </span>
              <span className="text-xs text-pine-400">Admin Panel</span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4 scrollbar-thin">
        <ul className="space-y-1 px-2">
          {navItems.map((item) => {
            const isActive =
              pathname === item.href ||
              (item.href !== "/admin/dashboard" && pathname.startsWith(item.href));
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={cn(
                    "group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200",
                    isActive
                      ? "bg-pine-500/10 text-pine-400 shadow-sm"
                      : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
                  )}
                >
                  <item.icon
                    className={cn(
                      "h-5 w-5 shrink-0 transition-colors",
                      isActive ? "text-pine-400" : "text-slate-500 group-hover:text-slate-300"
                    )}
                  />
                  <AnimatePresence>
                    {sidebarOpen && (
                      <motion.span
                        initial={{ opacity: 0, x: -8 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -8 }}
                        transition={{ duration: 0.12 }}
                        className="truncate"
                      >
                        {item.label}
                      </motion.span>
                    )}
                  </AnimatePresence>
                  {isActive && (
                    <motion.div
                      layoutId="activeIndicator"
                      className="ml-auto h-1.5 w-1.5 shrink-0 rounded-full bg-pine-400"
                    />
                  )}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Footer */}
      <div className="border-t border-slate-800 p-3">
        {sidebarOpen && user && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            onClick={() => setAdminProfileOpen(true)}
            className="mb-2 cursor-pointer rounded-lg bg-slate-800 px-3 py-2 transition hover:bg-slate-700/80 active:scale-[0.98]"
            title="Xem hồ sơ cá nhân"
          >
            <p className="truncate text-xs font-medium text-slate-200">
              {user.fullName}
            </p>
            <p className="truncate text-xs text-slate-500">{user.email}</p>
          </motion.div>
        )}
        <Link
          href="/"
          className={cn(
            "mb-1 flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-slate-400 transition-all hover:bg-slate-800 hover:text-slate-200",
            !sidebarOpen && "justify-center"
          )}
        >
          <Home className="h-5 w-5 shrink-0" />
          {sidebarOpen && <span>Về trang chủ</span>}
        </Link>
        <button
          onClick={handleLogout}
          className={cn(
            "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm text-slate-400 transition-all hover:bg-red-500/10 hover:text-red-400",
            !sidebarOpen && "justify-center"
          )}
        >
          <LogOut className="h-5 w-5 shrink-0" />
          {sidebarOpen && <span>Đăng xuất</span>}
        </button>
      </div>

      {/* Collapse button */}
      <button
        onClick={toggleSidebar}
        className="absolute -right-3 top-20 flex h-6 w-6 items-center justify-center rounded-full border border-slate-700 bg-slate-900 text-slate-400 shadow-md transition-colors hover:bg-slate-800 hover:text-slate-200"
      >
        <motion.div animate={{ rotate: sidebarOpen ? 0 : 180 }}>
          <ChevronLeft className="h-3.5 w-3.5" />
        </motion.div>
      </button>
    </motion.aside>
  );
}