"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { motion } from "framer-motion";
import { toast } from "sonner";
import { Eye, EyeOff, Leaf, Lock, Mail } from "lucide-react";
import { authService } from "@/services/auth.service";
import { useAuthStore } from "@/stores/auth-store";
import { tokenStorage } from "@/lib/api-client";

const loginSchema = z.object({
  email: z.string().email("Email không hợp lệ"),
  password: z.string().min(8, "Mật khẩu ít nhất 8 ký tự"),
});
type LoginForm = z.infer<typeof loginSchema>;

export function LoginForm() {
  const router = useRouter();
  const { setUser } = useAuthStore();
  const [showPw, setShowPw] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (form: LoginForm) => {
    try {
      const res = await authService.login(form);
      if (!res.success) {
        if (res.message && (res.message.includes('chưa được xác thực') || res.message.includes('xác thực email') || res.message.includes('chưa kích hoạt'))) {
          toast.error(res.message);
          router.push(`/verify-email?email=${encodeURIComponent(form.email)}`);
          return;
        }
        toast.error(res.message);
        return;
      }
      const { data } = res;
      if (!data.roles?.includes("ROLE_ADMIN")) {
        toast.error("Bạn không có quyền truy cập trang này");
        return;
      }
      if (data.accessToken) tokenStorage.set(data.accessToken);
      // Fetch full profile
      const user = await authService.getMe();
      setUser(user);
      toast.success(`Chào mừng, ${user.fullName}!`);
      router.push("/admin/dashboard");
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const msg = axiosErr?.response?.data?.message;
      if (msg && (msg.includes('chưa được xác thực') || msg.includes('xác thực email') || msg.includes('chưa kích hoạt'))) {
        toast.error(msg);
        router.push(`/verify-email?email=${encodeURIComponent(form.email)}`);
        return;
      }
      toast.error(msg ?? "Đăng nhập thất bại");
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="w-full max-w-md"
    >
      {/* Card */}
      <div className="relative overflow-hidden rounded-3xl border border-slate-800 bg-slate-900 p-8 shadow-2xl">
        {/* Decorative glow */}
        <div className="absolute -left-16 -top-16 h-48 w-48 rounded-full bg-pine-500/10 blur-3xl" />
        <div className="absolute -bottom-16 -right-16 h-48 w-48 rounded-full bg-pine-700/10 blur-3xl" />

        {/* Logo */}
        <div className="relative mb-8 flex flex-col items-center">
          <div className="mb-3 flex h-14 w-14 items-center justify-center rounded-2xl gradient-pine shadow-xl shadow-pine-500/30">
            <Leaf className="h-7 w-7 text-white" />
          </div>
          <h1 className="font-display text-2xl font-bold text-slate-100">
            Pineapple Admin
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Đăng nhập vào bảng điều khiển
          </p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="relative space-y-4">
          {/* Email */}
          <div>
            <label className="mb-1.5 block text-xs font-medium text-slate-400">
              Email
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
              <input
                type="email"
                {...register("email")}
                placeholder="Nhập email của bạn"
                autoComplete="email"
                className="h-10 w-full rounded-xl border border-slate-700 bg-slate-800 pl-9 pr-4 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/40 transition focus:border-pine-500 focus:ring-2"
              />
            </div>
            {errors.email && (
              <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>
            )}
          </div>

          {/* Password */}
          <div>
            <label className="mb-1.5 block text-xs font-medium text-slate-400">
              Mật khẩu
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
              <input
                type={showPw ? "text" : "password"}
                {...register("password")}
                placeholder="••••••••"
                autoComplete="current-password"
                className="h-10 w-full rounded-xl border border-slate-700 bg-slate-800 pl-9 pr-10 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/40 transition focus:border-pine-500 focus:ring-2"
              />
              <button
                type="button"
                onClick={() => setShowPw((v) => !v)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
              >
                {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {errors.password && (
              <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>
            )}
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="mt-2 flex h-10 w-full items-center justify-center rounded-xl gradient-pine text-sm font-semibold text-white shadow-lg shadow-pine-500/25 transition hover:opacity-90 disabled:opacity-50"
          >
            {isSubmitting ? (
              <span className="flex items-center gap-2">
                <span className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                Đang đăng nhập...
              </span>
            ) : (
              "Đăng nhập"
            )}
          </button>
        </form>
      </div>

      <p className="mt-4 text-center text-xs text-slate-600">
        © {new Date().getFullYear()} Pineapple E-Commerce. All rights reserved.
      </p>
    </motion.div>
  );
}