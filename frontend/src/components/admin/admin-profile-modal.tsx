"use client";

import { motion, AnimatePresence } from "framer-motion";
import { X, User, Mail, Phone, Shield, ShieldCheck, Calendar, Globe, Activity } from "lucide-react";
import { useAuthStore } from "@/stores/auth-store";
import { useUIStore } from "@/stores/ui-store";
import { formatDate, getInitials } from "@/lib/utils";
import { StatusBadge } from "@/components/shared/status-badge";

export function AdminProfileModal() {
  const { user } = useAuthStore();
  const { adminProfileOpen, setAdminProfileOpen } = useUIStore();

  if (!user) return null;

  return (
    <AnimatePresence>
      {adminProfileOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
          onClick={() => setAdminProfileOpen(false)}
        >
          <motion.div
            initial={{ scale: 0.95, y: 15 }}
            animate={{ scale: 1, y: 0 }}
            exit={{ scale: 0.95, y: 15 }}
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-lg overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl"
          >
            {/* Header / Decorative background */}
            <div className="absolute inset-x-0 top-0 h-32 bg-gradient-to-r from-pine-600/30 to-violet-600/30 blur-2xl" />
            
            {/* Close Button */}
            <button
              onClick={() => setAdminProfileOpen(false)}
              className="absolute right-4 top-4 z-10 rounded-lg p-1.5 text-slate-400 hover:bg-slate-800/80 hover:text-slate-200 transition-colors"
            >
              <X className="h-5 w-5" />
            </button>

            <div className="relative px-6 pt-8 pb-6">
              {/* Profile Header */}
              <div className="flex flex-col items-center border-b border-slate-800/60 pb-6 text-center">
                <div className="flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-pine-500 to-pine-700 text-2xl font-bold text-white shadow-xl shadow-pine-500/20">
                  {getInitials(user.fullName)}
                </div>
                <h2 className="mt-4 font-display text-xl font-bold text-slate-100">
                  {user.fullName}
                </h2>
                <div className="mt-2 flex flex-wrap justify-center gap-1.5">
                  {user.roles.map((role) => (
                    <span
                      key={role}
                      className="inline-flex items-center gap-1 rounded-full bg-violet-500/10 px-2.5 py-0.5 text-xs font-semibold text-violet-400 border border-violet-500/20"
                    >
                      <Shield className="h-3 w-3" />
                      {role.replace("ROLE_", "")}
                    </span>
                  ))}
                  <span className="inline-flex items-center gap-1 rounded-full bg-pine-500/15 px-2.5 py-0.5 text-xs font-semibold text-pine-400 border border-pine-500/10">
                    <Activity className="h-3 w-3" />
                    Admin Session
                  </span>
                </div>
              </div>

              {/* Profile Details */}
              <div className="mt-6 space-y-4">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Thông tin tài khoản
                </h3>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  {/* ID */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5">
                    <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Mã quản trị viên</p>
                    <p className="mt-1 font-mono text-sm font-semibold text-slate-200">#{user.id}</p>
                  </div>

                  {/* Trạng thái */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5">
                    <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Trạng thái tài khoản</p>
                    <div className="mt-1.5 flex">
                      <StatusBadge status={user.status} />
                    </div>
                  </div>

                  {/* Email */}
                  <div className="col-span-1 sm:col-span-2 rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                    <Mail className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                    <div>
                      <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Địa chỉ Email</p>
                      <p className="mt-0.5 text-sm font-medium text-slate-200 break-all">{user.email}</p>
                    </div>
                  </div>

                  {/* Phone */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                    <Phone className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                    <div>
                      <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Số điện thoại</p>
                      <p className="mt-0.5 text-sm font-medium text-slate-200">{user.phone ?? "Chưa cập nhật"}</p>
                    </div>
                  </div>

                  {/* Provider */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                    <Globe className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                    <div>
                      <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Phương thức đăng nhập</p>
                      <span className="mt-1 inline-block rounded-md bg-slate-800 px-2 py-0.5 text-xs font-semibold text-slate-300">
                        {user.provider ?? "LOCAL"}
                      </span>
                    </div>
                  </div>

                  {/* Created At */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                    <Calendar className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                    <div>
                      <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Ngày tham gia</p>
                      <p className="mt-0.5 text-xs font-medium text-slate-300">{formatDate(user.createdAt)}</p>
                    </div>
                  </div>

                  {/* Security / Role Info */}
                  <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                    <ShieldCheck className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                    <div>
                      <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Quyền truy cập</p>
                      <p className="mt-0.5 text-xs font-medium text-slate-300">Full Administrator</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Modal Actions */}
              <div className="mt-8 flex justify-end border-t border-slate-800/60 pt-4">
                <button
                  onClick={() => setAdminProfileOpen(false)}
                  className="rounded-lg bg-pine-500 px-5 py-2 text-sm font-semibold text-white hover:bg-pine-600 active:scale-[0.98] transition-all shadow-lg shadow-pine-500/25"
                >
                  Đóng
                </button>
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
