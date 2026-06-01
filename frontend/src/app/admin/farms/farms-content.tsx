"use client";

import { useState, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import {
  CheckCircle, XCircle, X, Search, ArrowUpDown, ArrowUp, ArrowDown,
  Eye, PowerOff, Power, Trash2, Package, ImageIcon,
} from "lucide-react";
import Image from "next/image";
import { adminFarmService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { formatDate } from "@/lib/utils";
import type { FarmResponse, FarmStatus, ProductSummaryResponse } from "@/types";

const rejectSchema = z.object({
  reason: z.string().min(10, "Lý do phải ít nhất 10 ký tự"),
});
type RejectForm = z.infer<typeof rejectSchema>;

const FARM_STATUSES = [
  { label: "Tất cả", value: "" },
  { label: "Chờ duyệt", value: "PENDING_APPROVAL" },
  { label: "Chờ vô hiệu hóa", value: "PENDING_DEACTIVATION" },
  { label: "Chờ kích hoạt lại", value: "PENDING_REACTIVATION" },
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Từ chối", value: "REJECTED" },
  { label: "Ngừng hoạt động", value: "INACTIVE" },
];

export function FarmsContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [statusFilter, setStatusFilter] = useState("PENDING_APPROVAL");
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Modal states
  const [rejectTarget, setRejectTarget] = useState<FarmResponse | null>(null);
  const [detailTarget, setDetailTarget] = useState<FarmResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<FarmResponse | null>(null);
  const [detailTab, setDetailTab] = useState<"info" | "products">("info");
  const [detailPage, setDetailPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.farms({
      page: pagination.pageIndex,
      size: pagination.pageSize,
      status: statusFilter || undefined,
      keyword: keyword || undefined,
      sortBy,
      sortDirection,
    }),
    queryFn: () =>
      adminFarmService.getAll({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        status: statusFilter || undefined,
        keyword: keyword || undefined,
        sortBy,
        sortDirection,
      }),
  });

  // Products for detail modal
  const { data: productsData, isLoading: productsLoading } = useQuery({
    queryKey: ["admin-farm-products", detailTarget?.id, detailPage],
    queryFn: () => adminFarmService.getProducts(detailTarget!.id, { page: detailPage, size: 8 }),
    enabled: !!detailTarget && detailTab === "products",
  });

  const approve = useMutation({
    mutationFn: (farmId: number) => adminFarmService.approve(farmId),
    onSuccess: () => { toast.success("Đã phê duyệt nông trại"); qc.invalidateQueries({ queryKey: ["farms"] }); },
    onError: () => toast.error("Thao tác thất bại"),
  });

  const reject = useMutation({
    mutationFn: ({ farmId, reason }: { farmId: number; reason: string }) =>
      adminFarmService.reject(farmId, { reason }),
    onSuccess: () => {
      toast.success("Đã từ chối nông trại");
      qc.invalidateQueries({ queryKey: ["farms"] });
      setRejectTarget(null);
    },
    onError: () => toast.error("Thao tác thất bại"),
  });

  const activate = useMutation({
    mutationFn: (farmId: number) => adminFarmService.activate(farmId),
    onSuccess: () => { toast.success("Đã kích hoạt nông trại"); qc.invalidateQueries({ queryKey: ["farms"] }); },
    onError: (e: { response?: { data?: { message?: string } } }) =>
      toast.error(e.response?.data?.message || "Thao tác thất bại"),
  });

  const deactivate = useMutation({
    mutationFn: (farmId: number) => adminFarmService.deactivate(farmId),
    onSuccess: () => { toast.success("Đã vô hiệu hóa nông trại"); qc.invalidateQueries({ queryKey: ["farms"] }); },
    onError: (e: { response?: { data?: { message?: string } } }) =>
      toast.error(e.response?.data?.message || "Thao tác thất bại"),
  });

  const deleteFarm = useMutation({
    mutationFn: (farmId: number) => adminFarmService.delete(farmId),
    onSuccess: () => {
      toast.success("Đã xóa nông trại");
      qc.invalidateQueries({ queryKey: ["farms"] });
      setDeleteTarget(null);
    },
    onError: () => toast.error("Không thể xóa nông trại"),
  });

  const { register, handleSubmit, reset, formState: { errors } } =
    useForm<RejectForm>({ resolver: zodResolver(rejectSchema) });

  const onRejectSubmit = (form: RejectForm) => {
    if (!rejectTarget) return;
    reject.mutate({ farmId: rejectTarget.id, reason: form.reason });
    reset();
  };

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("desc");
    }
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  const openDetail = (farm: FarmResponse) => {
    setDetailTarget(farm);
    setDetailTab("info");
    setDetailPage(0);
  };

  const columns: ColumnDef<FarmResponse, unknown>[] = [
    {
      id: "image",
      header: "Ảnh",
      cell: ({ row }) => (
        <div className="w-10 h-10 rounded-lg overflow-hidden bg-slate-800 flex items-center justify-center shrink-0">
          {row.original.imageUrl ? (
            <Image src={row.original.imageUrl} alt={row.original.name}
              width={40} height={40} className="object-cover w-full h-full" />
          ) : (
            <ImageIcon className="h-4 w-4 text-slate-600" />
          )}
        </div>
      ),
    },
    {
      id: "farm",
      header: () => (
        <button onClick={() => handleSort("name")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500">
          Nông trại
          {sortBy === "name" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div>
          <p className="text-xs font-semibold text-slate-200">{row.original.name}</p>
          <p className="text-xs text-slate-500">{row.original.location}</p>
        </div>
      ),
    },
    {
      id: "owner",
      header: "Chủ sở hữu",
      cell: ({ row }) => <span className="text-xs text-slate-400">{row.original.ownerName}</span>,
    },
    {
      accessorKey: "status",
      header: () => (
        <button onClick={() => handleSort("status")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500">
          Trạng thái
          {sortBy === "status" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div className="space-y-1">
          <StatusBadge status={row.original.status as FarmStatus} />
          {row.original.status === "REJECTED" && row.original.rejectionReason && (
            <p className="text-[10px] text-red-400 max-w-[140px] truncate" title={row.original.rejectionReason}>
              ↳ {row.original.rejectionReason}
            </p>
          )}
        </div>
      ),
    },
    {
      accessorKey: "createdAt",
      header: () => (
        <button onClick={() => handleSort("createdAt")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500">
          Ngày đăng ký
          {sortBy === "createdAt" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-500">{formatDate(getValue<string>())}</span>
      ),
    },
    {
      id: "actions",
      header: "Hành động",
      cell: ({ row }) => {
        const farm = row.original;
        return (
          <div className="flex items-center gap-1.5 flex-wrap">
            {/* View Detail */}
            <button onClick={() => openDetail(farm)}
              className="flex items-center gap-1 rounded-lg bg-blue-500/10 px-2 py-1.5 text-xs font-medium text-blue-400 hover:bg-blue-500/20 transition-colors">
              <Eye className="h-3.5 w-3.5" /> Chi tiết
            </button>

            {/* Pending: Approve + Reject */}
            {farm.status === "PENDING_APPROVAL" && (
              <>
                <button onClick={() => approve.mutate(farm.id)} disabled={approve.isPending}
                  className="flex items-center gap-1 rounded-lg bg-pine-500/10 px-2 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 disabled:opacity-50 transition-colors">
                  <CheckCircle className="h-3.5 w-3.5" /> Duyệt
                </button>
                <button onClick={() => setRejectTarget(farm)}
                  className="flex items-center gap-1 rounded-lg bg-red-500/10 px-2 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/20 transition-colors">
                  <XCircle className="h-3.5 w-3.5" /> Từ chối
                </button>
              </>
            )}

            {/* Pending Deactivation: Approve Deactivation + Reject/Keep Active */}
            {farm.status === "PENDING_DEACTIVATION" && (
              <>
                <button onClick={() => deactivate.mutate(farm.id)} disabled={deactivate.isPending}
                  className="flex items-center gap-1 rounded-lg bg-amber-500/10 px-2 py-1.5 text-xs font-medium text-amber-400 hover:bg-amber-500/20 disabled:opacity-50 transition-colors">
                  <CheckCircle className="h-3.5 w-3.5" /> Duyệt ngừng HĐ
                </button>
                <button onClick={() => activate.mutate(farm.id)} disabled={activate.isPending}
                  className="flex items-center gap-1 rounded-lg bg-pine-500/10 px-2 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 disabled:opacity-50 transition-colors">
                  <Power className="h-3.5 w-3.5" /> Từ chối ngừng HĐ
                </button>
              </>
            )}

            {/* Pending Reactivation: Approve Reactivation + Reject/Keep Inactive */}
            {farm.status === "PENDING_REACTIVATION" && (
              <>
                <button onClick={() => activate.mutate(farm.id)} disabled={activate.isPending}
                  className="flex items-center gap-1 rounded-lg bg-pine-500/10 px-2 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 disabled:opacity-50 transition-colors">
                  <CheckCircle className="h-3.5 w-3.5" /> Duyệt kích hoạt
                </button>
                <button onClick={() => deactivate.mutate(farm.id)} disabled={deactivate.isPending}
                  className="flex items-center gap-1 rounded-lg bg-red-500/10 px-2 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/20 disabled:opacity-50 transition-colors">
                  <PowerOff className="h-3.5 w-3.5" /> Từ chối kích hoạt
                </button>
              </>
            )}

            {/* Active: Deactivate */}
            {farm.status === "ACTIVE" && (
              <button onClick={() => deactivate.mutate(farm.id)} disabled={deactivate.isPending}
                className="flex items-center gap-1 rounded-lg bg-amber-500/10 px-2 py-1.5 text-xs font-medium text-amber-400 hover:bg-amber-500/20 disabled:opacity-50 transition-colors">
                <PowerOff className="h-3.5 w-3.5" /> Vô hiệu
              </button>
            )}

            {/* Inactive or Rejected: Activate */}
            {(farm.status === "INACTIVE" || farm.status === "REJECTED") && (
              <button onClick={() => activate.mutate(farm.id)} disabled={activate.isPending}
                className="flex items-center gap-1 rounded-lg bg-pine-500/10 px-2 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 disabled:opacity-50 transition-colors">
                <Power className="h-3.5 w-3.5" /> Kích hoạt
              </button>
            )}

            {/* Delete */}
            <button onClick={() => setDeleteTarget(farm)}
              className="flex items-center gap-1 rounded-lg bg-red-500/10 px-2 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/20 transition-colors">
              <Trash2 className="h-3.5 w-3.5" />
            </button>
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-2xl font-bold text-slate-100">Quản lý nông trại</h1>
        <p className="mt-1 text-sm text-slate-500">{data?.totalElements ?? 0} nông trại</p>
      </motion.div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input type="text" placeholder="Tìm tên nông trại, chủ sở hữu..."
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2" />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {FARM_STATUSES.map((s) => (
            <button key={s.value}
              onClick={() => { setStatusFilter(s.value); setPagination((p) => ({ ...p, pageIndex: 0 })); }}
              className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${statusFilter === s.value ? "bg-pine-500 text-white" : "bg-slate-800 text-slate-400 hover:bg-slate-700"}`}>
              {s.label}
            </button>
          ))}
        </div>
      </div>

      <DataTable data={data?.content ?? []} columns={columns}
        pageCount={data?.totalPages ?? 0} pagination={pagination}
        onPaginationChange={setPagination} isLoading={isLoading} />

      {/* ── Reject Modal ─────────────────────────────── */}
      <AnimatePresence>
        {rejectTarget && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setRejectTarget(null)}>
            <motion.div initial={{ scale: 0.95, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl">
              <div className="flex items-center justify-between">
                <h2 className="font-display text-lg font-semibold text-slate-100">Từ chối nông trại</h2>
                <button onClick={() => setRejectTarget(null)} className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800">
                  <X className="h-4 w-4" />
                </button>
              </div>
              <p className="mt-2 text-sm text-slate-400">
                Từ chối <span className="font-medium text-slate-200">{rejectTarget.name}</span>
              </p>
              <form onSubmit={handleSubmit(onRejectSubmit)} className="mt-4 space-y-4">
                <div>
                  <label className="text-xs font-medium text-slate-400">Lý do từ chối *</label>
                  <textarea {...register("reason")} rows={4} placeholder="Nhập lý do từ chối..."
                    className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 p-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-red-500/50 transition focus:border-red-500 focus:ring-2" />
                  {errors.reason && <p className="mt-1 text-xs text-red-400">{errors.reason.message}</p>}
                </div>
                <div className="flex justify-end gap-3">
                  <button type="button" onClick={() => setRejectTarget(null)}
                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800">Hủy</button>
                  <button type="submit" disabled={reject.isPending}
                    className="rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-600 disabled:opacity-50">
                    {reject.isPending ? "Đang xử lý..." : "Xác nhận từ chối"}
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Detail Modal ─────────────────────────────── */}
      <AnimatePresence>
        {detailTarget && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
            onClick={() => setDetailTarget(null)}>
            <motion.div initial={{ scale: 0.95, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 shadow-2xl overflow-hidden">
              {/* Header with farm image */}
              <div className="relative h-40 bg-slate-800">
                {detailTarget.imageUrl ? (
                  <Image src={detailTarget.imageUrl} alt={detailTarget.name}
                    fill className="object-cover opacity-70" />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-5xl">🌾</div>
                )}
                <div className="absolute inset-0 bg-gradient-to-t from-slate-900 to-transparent" />
                <button onClick={() => setDetailTarget(null)}
                  className="absolute top-3 right-3 rounded-lg bg-black/40 p-1.5 text-white hover:bg-black/60">
                  <X className="h-4 w-4" />
                </button>
                <div className="absolute bottom-4 left-6">
                  <h2 className="font-display text-xl font-bold text-white">{detailTarget.name}</h2>
                  <p className="text-sm text-slate-300">{detailTarget.location}</p>
                </div>
                <div className="absolute bottom-4 right-6">
                  <StatusBadge status={detailTarget.status as FarmStatus} />
                </div>
              </div>

              {/* Tabs */}
              <div className="flex border-b border-slate-800">
                {(["info", "products"] as const).map((tab) => (
                  <button key={tab} onClick={() => setDetailTab(tab)}
                    className={`px-5 py-3 text-sm font-medium transition-colors ${detailTab === tab ? "border-b-2 border-pine-500 text-pine-400" : "text-slate-500 hover:text-slate-300"}`}>
                    {tab === "info" ? "Thông tin" : <span className="flex items-center gap-1.5"><Package className="h-3.5 w-3.5" />Sản phẩm</span>}
                  </button>
                ))}
              </div>

              {/* Tab content */}
              <div className="p-6 max-h-80 overflow-y-auto">
                {detailTab === "info" ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-slate-500 uppercase tracking-wide">Chủ sở hữu</p>
                        <p className="text-sm text-slate-200 mt-1">{detailTarget.ownerName}</p>
                      </div>
                      <div>
                        <p className="text-xs text-slate-500 uppercase tracking-wide">Ngày đăng ký</p>
                        <p className="text-sm text-slate-200 mt-1">{formatDate(detailTarget.createdAt)}</p>
                      </div>
                    </div>
                    {detailTarget.description && (
                      <div>
                        <p className="text-xs text-slate-500 uppercase tracking-wide">Mô tả</p>
                        <p className="text-sm text-slate-300 mt-1 leading-relaxed whitespace-pre-wrap">{detailTarget.description}</p>
                      </div>
                    )}
                    {detailTarget.certificate && (
                      <div>
                        <p className="text-xs text-slate-500 uppercase tracking-wide">Chứng nhận</p>
                        <p className="text-sm text-slate-300 mt-1">{detailTarget.certificate}</p>
                      </div>
                    )}
                    {detailTarget.status === "REJECTED" && detailTarget.rejectionReason && (
                      <div className="rounded-lg border border-red-500/30 bg-red-500/10 p-4">
                        <p className="text-xs font-medium text-red-400 uppercase tracking-wide mb-1">Lý do từ chối</p>
                        <p className="text-sm text-red-300">{detailTarget.rejectionReason}</p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div>
                    {productsLoading ? (
                      <div className="space-y-2">
                        {[1, 2, 3].map((i) => (
                          <div key={i} className="h-12 rounded-lg bg-slate-800 animate-pulse" />
                        ))}
                      </div>
                    ) : !productsData?.content?.length ? (
                      <div className="text-center py-8 text-slate-500">
                        <Package className="h-8 w-8 mx-auto mb-2 opacity-30" />
                        <p className="text-sm">Nông trại chưa có sản phẩm</p>
                      </div>
                    ) : (
                      <div className="space-y-2">
                        {productsData.content.map((product: ProductSummaryResponse) => (
                          <div key={product.id} className="flex items-center gap-3 rounded-lg bg-slate-800/50 p-3">
                            <div className="w-9 h-9 rounded-lg overflow-hidden bg-slate-700 shrink-0">
                              <Image src={product.thumbnail} alt={product.name}
                                width={36} height={36} className="object-cover w-full h-full" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-xs font-medium text-slate-200 truncate">{product.name}</p>
                              <p className="text-xs text-slate-500">{product.totalStock} đơn vị còn</p>
                            </div>
                            <p className="text-xs font-semibold text-pine-400 shrink-0">
                              {product.effectivePrice.toLocaleString("vi-VN")}₫
                            </p>
                          </div>
                        ))}
                        {/* Pagination */}
                        {productsData.totalPages > 1 && (
                          <div className="flex justify-center gap-2 mt-3">
                            <button disabled={detailPage === 0}
                              onClick={() => setDetailPage((p) => p - 1)}
                              className="rounded-lg px-3 py-1.5 text-xs text-slate-400 hover:bg-slate-800 disabled:opacity-30">← Trước</button>
                            <span className="px-3 py-1.5 text-xs text-slate-500">
                              {detailPage + 1}/{productsData.totalPages}
                            </span>
                            <button disabled={detailPage + 1 >= productsData.totalPages}
                              onClick={() => setDetailPage((p) => p + 1)}
                              className="rounded-lg px-3 py-1.5 text-xs text-slate-400 hover:bg-slate-800 disabled:opacity-30">Sau →</button>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="border-t border-slate-800 px-6 py-4 flex justify-end gap-3">
                <button onClick={() => setDetailTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800">Đóng</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Delete Confirm Modal ──────────────────────── */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setDeleteTarget(null)}>
            <motion.div initial={{ scale: 0.95, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl">
              <div className="flex items-center gap-3 mb-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-500/10">
                  <Trash2 className="h-5 w-5 text-red-400" />
                </div>
                <div>
                  <h2 className="font-display text-base font-semibold text-slate-100">Xóa nông trại</h2>
                  <p className="text-xs text-slate-500">Hành động này không thể hoàn tác</p>
                </div>
              </div>
              <p className="text-sm text-slate-400 mb-6">
                Bạn có chắc muốn xóa <span className="font-medium text-slate-200">{deleteTarget.name}</span>?
              </p>
              <div className="flex justify-end gap-3">
                <button onClick={() => setDeleteTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800">Hủy</button>
                <button onClick={() => deleteFarm.mutate(deleteTarget.id)} disabled={deleteFarm.isPending}
                  className="rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-600 disabled:opacity-50">
                  {deleteFarm.isPending ? "Đang xóa..." : "Xóa"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}