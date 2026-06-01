"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { Plus, X, Tag, ToggleLeft, ToggleRight, ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { adminCouponService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { formatCurrency, formatDate } from "@/lib/utils";
import type { CouponResponse } from "@/types";

const couponSchema = z.object({
  code: z.string().min(3, "Mã ít nhất 3 ký tự").toUpperCase(),
  type: z.enum(["PERCENTAGE", "FIXED_AMOUNT"]),
  value: z.coerce.number().positive("Giá trị phải > 0"),
  minOrderValue: z.coerce.number().min(0, "Không được nhỏ hơn 0").default(0),
  maxDiscountAmount: z.coerce.number().min(0).optional(),
  startDate: z.string().min(1, "Bắt buộc"),
  expiryDate: z.string().min(1, "Bắt buộc"),
  totalLimit: z.coerce.number({ invalid_type_error: "Bắt buộc" }).int().positive("Phải lớn hơn 0"),
  perUserLimit: z.coerce.number({ invalid_type_error: "Bắt buộc" }).int().positive("Phải lớn hơn 0").default(1),
});
type CouponForm = z.infer<typeof couponSchema>;

export function CouponsContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [showCreate, setShowCreate] = useState(false);
  const [usageTarget, setUsageTarget] = useState<CouponResponse | null>(null);
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  const { data: usageLogs, isLoading: usageLoading } = useQuery({
    queryKey: ["coupon-usage", usageTarget?.id],
    queryFn: () => adminCouponService.getUsage(usageTarget!.id),
    enabled: !!usageTarget,
  });

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.coupons({
      page: pagination.pageIndex,
      size: pagination.pageSize,
      sortBy,
      sortDirection,
    }),
    queryFn: () =>
      adminCouponService.getAll({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        sortBy,
        sortDirection,
      }),
  });

  const formatDateTimeLocal = (val: string) => {
    if (!val) return "";
    if (val.length === 16) {
      return `${val}:00`;
    }
    return val;
  };

  const createCoupon = useMutation({
    mutationFn: (payload: CouponForm) =>
      adminCouponService.create({
        ...payload,
        minOrderValue: payload.minOrderValue ?? 0,
        perUserLimit: payload.perUserLimit ?? 1,
        startDate: formatDateTimeLocal(payload.startDate),
        expiryDate: formatDateTimeLocal(payload.expiryDate),
        applicableProductIds: [],
        applicableCategoryIds: [],
      }),
    onSuccess: () => {
      toast.success("Tạo mã giảm giá thành công");
      qc.invalidateQueries({ queryKey: ["coupons"] });
      setShowCreate(false);
      reset();
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Tạo mã thất bại";
      toast.error(msg);
    },
  });

  const toggleActive = useMutation({
    mutationFn: ({
      id,
      isActive,
      totalLimit,
      perUserLimit,
    }: {
      id: number;
      isActive: boolean;
      totalLimit: number;
      perUserLimit: number;
    }) =>
      adminCouponService.update(id, {
        isActive: !isActive,
        totalLimit,
        perUserLimit,
      }),
    onSuccess: () => {
      toast.success("Cập nhật trạng thái thành công");
      qc.invalidateQueries({ queryKey: ["coupons"] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Cập nhật trạng thái thất bại";
      toast.error(msg);
    },
  });

  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<CouponForm>({
    resolver: zodResolver(couponSchema),
    defaultValues: { type: "PERCENTAGE", minOrderValue: 0, perUserLimit: 1 },
  });

  const couponType = watch("type");

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("desc");
    }
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  const columns: ColumnDef<CouponResponse, unknown>[] = [
    {
      accessorKey: "code",
      header: () => (
        <button
          onClick={() => handleSort("code")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Mã
          {sortBy === "code" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <div className="flex items-center gap-2">
          <Tag className="h-3.5 w-3.5 text-pine-400" />
          <span className="font-mono text-xs font-bold text-pine-300">
            {getValue<string>()}
          </span>
        </div>
      ),
    },
    {
      id: "discount",
      header: () => (
        <button
          onClick={() => handleSort("value")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Giảm giá
          {sortBy === "value" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <span className="text-xs font-semibold text-slate-200">
          {row.original.type === "PERCENTAGE"
            ? `${row.original.value}%`
            : formatCurrency(row.original.value)}
        </span>
      ),
    },
    {
      accessorKey: "usedCount",
      header: "Đã dùng",
      cell: ({ row }) => (
        <span className="text-xs text-slate-400">
          {row.original.usedCount} / {row.original.totalLimit ?? "∞"}
        </span>
      ),
    },
    {
      accessorKey: "expiryDate",
      header: () => (
        <button
          onClick={() => handleSort("expiryDate")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Hết hạn
          {sortBy === "expiryDate" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-500">{formatDate(getValue<string>())}</span>
      ),
    },
    {
      accessorKey: "isActive",
      header: () => (
        <button
          onClick={() => handleSort("isActive")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Kích hoạt
          {sortBy === "isActive" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <button
          onClick={() =>
            toggleActive.mutate({
              id: row.original.id,
              isActive: row.original.isActive,
              totalLimit: row.original.totalLimit ?? 100,
              perUserLimit: row.original.perUserLimit ?? 1,
            })
          }
          className={`flex items-center gap-1.5 text-xs font-medium ${
            row.original.isActive ? "text-pine-400" : "text-slate-500"
          }`}
        >
          {row.original.isActive ? (
            <ToggleRight className="h-5 w-5" />
          ) : (
            <ToggleLeft className="h-5 w-5" />
          )}
          {row.original.isActive ? "Đang hoạt động" : "Đã tắt"}
        </button>
      ),
    },
    {
      id: "actions",
      header: "Hành động",
      cell: ({ row }) => (
        <button
          onClick={() => setUsageTarget(row.original)}
          className="rounded-lg bg-cyan-500/10 px-2.5 py-1.5 text-xs font-medium text-cyan-400 hover:bg-cyan-500/20"
        >
          Lịch sử
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center justify-between"
      >
        <div>
          <h1 className="font-display text-2xl font-bold text-slate-100">Mã giảm giá</h1>
          <p className="mt-1 text-sm text-slate-500">{data?.totalElements ?? 0} mã</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 rounded-xl bg-pine-500 px-4 py-2 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600"
        >
          <Plus className="h-4 w-4" /> Tạo mã mới
        </button>
      </motion.div>

      <DataTable
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
      />

      {/* Create coupon drawer */}
      <AnimatePresence>
        {showCreate && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-end bg-black/60 backdrop-blur-sm"
            onClick={() => setShowCreate(false)}
          >
            <motion.div
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
              onClick={(e) => e.stopPropagation()}
              className="h-full w-full max-w-md overflow-y-auto border-l border-slate-700 bg-slate-900 p-6 scrollbar-thin"
            >
              <div className="flex items-center justify-between">
                <h2 className="font-display text-lg font-semibold text-slate-100">
                  Tạo mã giảm giá
                </h2>
                <button
                  onClick={() => setShowCreate(false)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <form
                onSubmit={handleSubmit((d) => createCoupon.mutate(d))}
                className="mt-6 space-y-4"
              >
                {[
                  { name: "code" as const, label: "Mã coupon *", placeholder: "VD: SUMMER20" },
                ].map(({ name, label, placeholder }) => (
                  <div key={name}>
                    <label className="text-xs font-medium text-slate-400">{label}</label>
                    <input
                      {...register(name)}
                      placeholder={placeholder}
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                    />
                    {errors[name] && (
                      <p className="mt-1 text-xs text-red-400">{errors[name]?.message}</p>
                    )}
                  </div>
                ))}

                <div>
                  <label className="text-xs font-medium text-slate-400">Loại *</label>
                  <select
                    {...register("type")}
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  >
                    <option value="PERCENTAGE">Phần trăm (%)</option>
                    <option value="FIXED_AMOUNT">Số tiền cố định (VND)</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Giá trị * {couponType === "PERCENTAGE" ? "(%)" : "(VND)"}
                  </label>
                  <input
                    type="number"
                    {...register("value")}
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                  {errors.value && (
                    <p className="mt-1 text-xs text-red-400">{errors.value.message}</p>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Đơn hàng tối thiểu</label>
                    <input
                      type="number"
                      {...register("minOrderValue")}
                      placeholder="0"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Giảm tối đa</label>
                    <input
                      type="number"
                      {...register("maxDiscountAmount")}
                      placeholder="Không giới hạn"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Ngày bắt đầu *</label>
                    <input
                      type="datetime-local"
                      {...register("startDate")}
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.startDate && (
                      <p className="mt-1 text-xs text-red-400">{errors.startDate.message}</p>
                    )}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Ngày hết hạn *</label>
                    <input
                      type="datetime-local"
                      {...register("expiryDate")}
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.expiryDate && (
                      <p className="mt-1 text-xs text-red-400">{errors.expiryDate.message}</p>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Giới hạn tổng *</label>
                    <input
                      type="number"
                      {...register("totalLimit")}
                      placeholder="VD: 100"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.totalLimit && (
                      <p className="mt-1 text-xs text-red-400">{errors.totalLimit.message}</p>
                    )}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Giới hạn / người *</label>
                    <input
                      type="number"
                      {...register("perUserLimit")}
                      placeholder="VD: 1"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.perUserLimit && (
                      <p className="mt-1 text-xs text-red-400">{errors.perUserLimit.message}</p>
                    )}
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={createCoupon.isPending}
                  className="mt-2 w-full rounded-xl bg-pine-500 py-2.5 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 disabled:opacity-50"
                >
                  {createCoupon.isPending ? "Đang tạo..." : "Tạo mã giảm giá"}
                </button>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Usage history modal */}
      <AnimatePresence>
        {usageTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setUsageTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="font-display text-lg font-semibold text-slate-100">
                  Lịch sử sử dụng: <span className="font-mono text-pine-400">{usageTarget.code}</span>
                </h2>
                <button
                  onClick={() => setUsageTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 max-h-[400px] overflow-y-auto pr-1">
                {usageLoading ? (
                  <div className="space-y-2 py-4">
                    <div className="h-8 animate-pulse rounded-lg bg-slate-800" />
                    <div className="h-8 animate-pulse rounded-lg bg-slate-800" />
                  </div>
                ) : !usageLogs || usageLogs.length === 0 ? (
                  <div className="py-8 text-center text-sm text-slate-500">
                    Mã giảm giá này chưa được sử dụng lần nào.
                  </div>
                ) : (
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400">
                        <th className="pb-2 font-medium">Khách hàng</th>
                        <th className="pb-2 font-medium">Đơn hàng</th>
                        <th className="pb-2 font-medium">Số tiền giảm</th>
                        <th className="pb-2 font-medium">Ngày áp dụng</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800 text-slate-300">
                      {usageLogs.map((log) => (
                        <tr key={log.id}>
                          <td className="py-2.5">{log.userEmail}</td>
                          <td className="py-2.5 font-mono text-pine-400">#{log.orderId}</td>
                          <td className="py-2.5 font-semibold text-slate-100">
                            {formatCurrency(log.discountApplied)}
                          </td>
                          <td className="py-2.5 text-slate-500">
                            {formatDate(log.usedAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              <div className="mt-6 flex justify-end">
                <button
                  onClick={() => setUsageTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Đóng
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}