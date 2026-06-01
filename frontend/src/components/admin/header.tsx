"use client";

import { Bell, Moon, Sun, Menu } from "lucide-react";
import { useTheme } from "next-themes";
import { useUIStore } from "@/stores/ui-store";
import { useAuthStore } from "@/stores/auth-store";
import { getInitials } from "@/lib/utils";

export function AdminHeader() {
  const { theme, setTheme } = useTheme();
  const { toggleSidebar, setAdminProfileOpen } = useUIStore();
  const { user } = useAuthStore();

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-800 bg-slate-900/50 px-6 backdrop-blur-sm">
      <div className="flex items-center gap-4">
        <button
          onClick={toggleSidebar}
          className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-800 hover:text-slate-200 lg:hidden"
        >
          <Menu className="h-5 w-5" />
        </button>
        <div>
          <p className="text-xs text-slate-500">
            {new Date().toLocaleDateString("vi-VN", {
              weekday: "long",
              day: "numeric",
              month: "long",
              year: "numeric",
            })}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {/* Theme toggle */}
        <button
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-800 hover:text-slate-200"
        >
          {theme === "dark" ? (
            <Sun className="h-5 w-5" />
          ) : (
            <Moon className="h-5 w-5" />
          )}
        </button>

        {/* Notifications */}
        <button className="relative rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-800 hover:text-slate-200">
          <Bell className="h-5 w-5" />
          <span className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-pine-500" />
        </button>

        {/* Avatar */}
        {user && (
          <button
            onClick={() => setAdminProfileOpen(true)}
            className="ml-2 flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-pine-500 to-pine-700 text-sm font-semibold text-white shadow-lg shadow-pine-500/20 cursor-pointer transition hover:scale-105 active:scale-95 focus:outline-none"
            title="Xem hồ sơ cá nhân"
          >
            {getInitials(user.fullName)}
          </button>
        )}
      </div>
    </header>
  );
}