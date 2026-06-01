"use client";

import { useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { useQuery } from "@tanstack/react-query";
import { authService } from "@/services/auth.service";
import { queryKeys } from "@/lib/query-keys";
import { ROUTES } from "@/lib/routes";

export function AdminGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { setUser, logout } = useAuthStore();

  const { data: user, isLoading, isError } = useQuery({
    queryKey: queryKeys.me,
    queryFn: authService.getMe,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  useEffect(() => {
    if (isError) {
      logout();
      router.replace(`${ROUTES.LOGIN}?redirect=${encodeURIComponent(pathname)}`);
      return;
    }
    if (!isLoading && user) {
      if (!user.roles.includes("ROLE_ADMIN")) {
        router.replace(`${ROUTES.LOGIN}?redirect=${encodeURIComponent(pathname)}`);
        return;
      }
      setUser(user);
    }
  }, [user, isLoading, isError, router, pathname, setUser, logout]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-950">
        <div className="flex flex-col items-center gap-4">
          <div className="h-12 w-12 animate-spin rounded-full border-4 border-pine-500 border-t-transparent" />
          <p className="text-sm text-slate-400 font-body">Đang xác thực...</p>
        </div>
      </div>
    );
  }

  if (!user || !user.roles.includes("ROLE_ADMIN")) {
    return null;
  }

  return <>{children}</>;
}