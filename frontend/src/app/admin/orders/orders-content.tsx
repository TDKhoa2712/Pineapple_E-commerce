"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { toast } from "sonner";
import { Search, Filter, Download, FileDown, Eye, X, ArrowUpDown, ArrowUp, ArrowDown, CreditCard, Truck, RefreshCw, AlertTriangle, Calendar, MapPin, ClipboardList, CheckCircle } from "lucide-react";
import { adminOrderService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { formatCurrency, formatDateTime } from "@/lib/utils";
import type { OrderResponse, OrderStatus } from "@/types";

const ORDER_STATUSES: { label: string; value: string }[] = [
  { label: "Tất cả", value: "" },
  { label: "Chờ xác nhận", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
  { label: "Đang xử lý", value: "PROCESSING" },
  { label: "Đang giao", value: "SHIPPING" },
  { label: "Đã giao", value: "DELIVERED" },
  { label: "Yêu cầu hoàn tiền", value: "REFUND_REQUESTED" },
  { label: "Đã hoàn tiền", value: "REFUNDED" },
  { label: "Đã trả hàng", value: "RETURNED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  PENDING: "CONFIRMED",
  CONFIRMED: "PROCESSING",
  PROCESSING: "SHIPPING",
  SHIPPING: "DELIVERED",
  REFUND_REQUESTED: "REFUNDED",
};

const ALLOWED_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PROCESSING", "CANCELLED"],
  PROCESSING: ["SHIPPING", "CANCELLED"],
  SHIPPING: ["DELIVERED", "CANCELLED"],
  DELIVERED: ["REFUND_REQUESTED", "RETURNED"],
  REFUND_REQUESTED: ["REFUNDED", "RETURNED"],
  CANCELLED: ["REFUND_REQUESTED", "REFUNDED"],
  REFUNDED: [],
  RETURNED: [],
};

const STATUS_TRANSITION_ACTIONS: Record<
  OrderStatus,
  { label: string; className: string }
> = {
  PENDING: { label: "Chờ xác nhận", className: "bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700" },
  CONFIRMED: { label: "Xác nhận đơn", className: "bg-blue-600/20 text-blue-400 border border-blue-500/30 hover:bg-blue-600/30" },
  PROCESSING: { label: "Chuẩn bị hàng", className: "bg-amber-600/20 text-amber-400 border border-amber-500/30 hover:bg-amber-600/30" },
  SHIPPING: { label: "Giao hàng", className: "bg-indigo-600/20 text-indigo-400 border border-indigo-500/30 hover:bg-indigo-600/30" },
  DELIVERED: { label: "Đã giao hàng", className: "bg-green-600/20 text-green-400 border border-green-500/30 hover:bg-green-600/30" },
  CANCELLED: { label: "Hủy đơn hàng", className: "bg-red-600/20 text-red-400 border border-red-500/30 hover:bg-red-600/30" },
  REFUND_REQUESTED: { label: "Yêu cầu hoàn tiền", className: "bg-amber-500/20 text-amber-400 border border-amber-500/30 hover:bg-amber-500/30" },
  REFUNDED: { label: "Đã hoàn tiền", className: "bg-violet-600/20 text-violet-400 border border-violet-500/30 hover:bg-violet-600/30" },
  RETURNED: { label: "Trả hàng", className: "bg-rose-600/20 text-rose-400 border border-rose-500/30 hover:bg-rose-600/30" },
};

export function OrdersContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 20,
  });
  const [statusFilter, setStatusFilter] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Order Details Modal state
  const [detailOrderId, setDetailOrderId] = useState<number | null>(null);
  const [selectedCarrier, setSelectedCarrier] = useState<"GHN" | "GHTK">("GHN");

  useEffect(() => {
    if (detailOrderId) {
      setSelectedCarrier("GHN");
    }
  }, [detailOrderId]);

  // Bulk selection state
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [bulkStatus, setBulkStatus] = useState<OrderStatus | "">("");

  // Export state
  const [isExporting, setIsExporting] = useState(false);

  // Fetch orders
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.orders({
      page: pagination.pageIndex,
      size: pagination.pageSize,
      status: statusFilter,
      keyword,
      sortBy,
      sortDirection,
    }),
    queryFn: () =>
      adminOrderService.getAll({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        status: statusFilter || undefined,
        keyword: keyword || undefined,
        sortBy,
        sortDirection,
      }),
  });

  // Fetch order details
  const { data: orderDetail, isLoading: detailLoading } = useQuery({
    queryKey: ["order-details", detailOrderId],
    queryFn: () => adminOrderService.getById(detailOrderId!),
    enabled: !!detailOrderId,
  });

  // Fetch payment details
  const { data: paymentDetail, isLoading: paymentLoading } = useQuery({
    queryKey: ["order-payment", detailOrderId],
    queryFn: () => adminOrderService.getPayment(detailOrderId!),
    enabled: !!detailOrderId,
  });

  // Fetch shipping tracking details
  const { data: shippingTracking, isLoading: shippingLoading } = useQuery({
    queryKey: ["order-shipping", detailOrderId],
    queryFn: () => adminOrderService.getShippingTracking(detailOrderId!),
    enabled: !!detailOrderId,
    retry: false, // Don't retry if shipment doesn't exist yet
  });

  // Mutation: Create shipment
  const createShipmentMutation = useMutation({
    mutationFn: ({ orderId, carrier }: { orderId: number; carrier: string }) =>
      adminOrderService.createShipment(orderId, carrier),
    onSuccess: (res) => {
      toast.success(`Tạo vận đơn ${res.carrierName} thành công! Mã vận đơn: ${res.externalOrderCode}`);
      qc.invalidateQueries({ queryKey: ["orders"] });
      qc.invalidateQueries({ queryKey: ["order-details", detailOrderId] });
      qc.invalidateQueries({ queryKey: ["order-shipping", detailOrderId] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Tạo vận đơn thất bại";
      toast.error(msg);
    },
  });

  // Mutation: Cancel shipment
  const cancelShipmentMutation = useMutation({
    mutationFn: (orderId: number) => adminOrderService.cancelShipment(orderId),
    onSuccess: () => {
      toast.success("Đã hủy vận đơn giao hàng thành công!");
      qc.invalidateQueries({ queryKey: ["orders"] });
      qc.invalidateQueries({ queryKey: ["order-details", detailOrderId] });
      qc.invalidateQueries({ queryKey: ["order-shipping", detailOrderId] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Hủy vận đơn thất bại";
      toast.error(msg);
    },
  });

  // Mutation: Sync shipment
  const syncShipmentMutation = useMutation({
    mutationFn: (orderId: number) => adminOrderService.syncShipment(orderId),
    onSuccess: (res) => {
      toast.success(`Đồng bộ trạng thái vận đơn thành công: ${res.currentStatusLabel}`);
      qc.invalidateQueries({ queryKey: ["orders"] });
      qc.invalidateQueries({ queryKey: ["order-details", detailOrderId] });
      qc.invalidateQueries({ queryKey: ["order-shipping", detailOrderId] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Đồng bộ thất bại";
      toast.error(msg);
    },
  });

  // Clear selection when filter or page changes
  useEffect(() => {
    setSelectedIds([]);
  }, [pagination.pageIndex, statusFilter, keyword]);

  const updateStatus = useMutation({
    mutationFn: ({ id, status }: { id: number; status: OrderStatus }) =>
      adminOrderService.updateStatus(id, { status }),
    onSuccess: () => {
      toast.success("Cập nhật trạng thái thành công");
      qc.invalidateQueries({ queryKey: ["orders"] });
      // Invalidate specific order details too
      if (detailOrderId) {
        qc.invalidateQueries({ queryKey: ["order-details", detailOrderId] });
      }
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Cập nhật thất bại";
      toast.error(msg);
      console.error("Order status update failed:", error);
    },
  });

  const bulkUpdateStatus = useMutation({
    mutationFn: () =>
      adminOrderService.bulkUpdateStatus({
        orderIds: selectedIds,
        newStatus: bulkStatus as OrderStatus,
      }),
    onSuccess: (updatedCount) => {
      toast.success(`Cập nhật thành công ${updatedCount} đơn hàng`);
      setSelectedIds([]);
      setBulkStatus("");
      qc.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || "Cập nhật đồng loạt thất bại";
      toast.error(msg);
    },
  });

  const handleExport = async (format: "csv" | "excel") => {
    try {
      setIsExporting(true);
      const res = await adminOrderService.exportOrders({
        status: statusFilter || undefined,
        format,
      });

      const blob = new Blob([res], {
        type: format === "excel"
          ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
          : "text/csv; charset=utf-8",
      });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute(
        "download",
        `orders_export_${Date.now()}.${format === "excel" ? "xlsx" : "csv"}`
      );
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success(`Xuất báo cáo ${format.toUpperCase()} thành công`);
    } catch (err) {
      console.error("Export error:", err);
      toast.error("Xuất báo cáo thất bại");
    } finally {
      setIsExporting(false);
    }
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

  const columns: ColumnDef<OrderResponse, unknown>[] = [
    {
      id: "select",
      header: () => {
        const allSelected =
          data?.content &&
          data.content.length > 0 &&
          data.content.every((order) => selectedIds.includes(order.id));
        return (
          <input
            type="checkbox"
            checked={allSelected || false}
            onChange={() => {
              if (allSelected) {
                const pageIds = (data?.content ?? []).map((o) => o.id);
                setSelectedIds((prev) => prev.filter((id) => !pageIds.includes(id)));
              } else {
                const pageIds = (data?.content ?? []).map((o) => o.id);
                setSelectedIds((prev) => Array.from(new Set([...prev, ...pageIds])));
              }
            }}
            className="rounded border-slate-700 bg-slate-800 text-pine-500 focus:ring-pine-500/50"
          />
        );
      },
      cell: ({ row }) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(row.original.id)}
          onChange={() => {
            const id = row.original.id;
            setSelectedIds((prev) =>
              prev.includes(id) ? prev.filter((oid) => oid !== id) : [...prev, id]
            );
          }}
          className="rounded border-slate-700 bg-slate-800 text-pine-500 focus:ring-pine-500/50"
        />
      ),
    },
    {
      accessorKey: "id",
      header: () => (
        <button
          onClick={() => handleSort("id")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Mã ĐH
          {sortBy === "id" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <button
          onClick={() => setDetailOrderId(getValue<number>())}
          className="font-mono text-xs font-semibold text-pine-400 hover:underline"
        >
          #{getValue<number>()}
        </button>
      ),
    },
    {
      id: "customer",
      header: "Khách hàng",
      cell: ({ row }) => (
        <div>
          <p className="text-xs font-medium text-slate-200">
            {row.original.userFullName ?? "—"}
          </p>
          <p className="text-xs text-slate-500">{row.original.userEmail ?? ""}</p>
        </div>
      ),
    },
    {
      accessorKey: "totalAmount",
      header: () => (
        <button
          onClick={() => handleSort("totalAmount")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Tổng tiền
          {sortBy === "totalAmount" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <span className="font-semibold text-slate-200">
          {formatCurrency(getValue<number>())}
        </span>
      ),
    },
    {
      accessorKey: "paymentMethod",
      header: "Thanh toán",
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-400">{getValue<string>()}</span>
      ),
    },
    {
      accessorKey: "status",
      header: () => (
        <button
          onClick={() => handleSort("status")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Trạng thái
          {sortBy === "status" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => <StatusBadge status={getValue<OrderStatus>()} />,
    },
    {
      accessorKey: "createdAt",
      header: () => (
        <button
          onClick={() => handleSort("createdAt")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Ngày đặt
          {sortBy === "createdAt" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-500">
          {formatDateTime(getValue<string>())}
        </span>
      ),
    },
    {
      id: "actions",
      header: "Hành động",
      cell: ({ row }) => {
        const currentStatus = row.original.status;
        const nextStatus = NEXT_STATUS[currentStatus];
        const canCancel = ALLOWED_TRANSITIONS[currentStatus]?.includes("CANCELLED");
        const canRefund = ALLOWED_TRANSITIONS[currentStatus]?.includes("REFUNDED");
        const canRequestRefund = ALLOWED_TRANSITIONS[currentStatus]?.includes("REFUND_REQUESTED");

        return (
          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => setDetailOrderId(row.original.id)}
              className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200 transition-colors"
              title="Xem chi tiết"
            >
              <Eye className="h-4 w-4" />
            </button>
            {nextStatus && (
              <button
                onClick={() =>
                  updateStatus.mutate({ id: row.original.id, status: nextStatus })
                }
                disabled={updateStatus.isPending}
                className="rounded-lg bg-pine-500/10 px-2.5 py-1 text-xs font-semibold text-pine-400 transition-colors hover:bg-pine-500/20 disabled:opacity-50"
              >
                → {nextStatus}
              </button>
            )}
            {canCancel && (
              <button
                onClick={() => {
                  if (confirm(`Bạn có chắc chắn muốn hủy đơn hàng #${row.original.id} không?`)) {
                    updateStatus.mutate({ id: row.original.id, status: "CANCELLED" });
                  }
                }}
                disabled={updateStatus.isPending}
                className="rounded-lg bg-red-500/10 px-2.5 py-1 text-xs font-semibold text-red-400 hover:bg-red-500/20 disabled:opacity-50 transition-colors"
              >
                Hủy đơn
              </button>
            )}
            {currentStatus === "CANCELLED" && (
              <>
                {canRequestRefund && (
                  <button
                    onClick={() =>
                      updateStatus.mutate({ id: row.original.id, status: "REFUND_REQUESTED" })
                    }
                    disabled={updateStatus.isPending}
                    className="rounded-lg bg-amber-500/10 px-2 py-1 text-xs font-semibold text-amber-400 transition-colors hover:bg-amber-500/20 disabled:opacity-50"
                  >
                    Yêu cầu HT
                  </button>
                )}
                {canRefund && (
                  <button
                    onClick={() =>
                      updateStatus.mutate({ id: row.original.id, status: "REFUNDED" })
                    }
                    disabled={updateStatus.isPending}
                    className="rounded-lg bg-violet-500/10 px-2 py-1 text-xs font-semibold text-violet-400 transition-colors hover:bg-violet-500/20 disabled:opacity-50"
                  >
                    Hoàn tiền
                  </button>
                )}
              </>
            )}
            {!nextStatus && !canCancel && currentStatus !== "CANCELLED" && (
              <span className="text-xs text-slate-600">—</span>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="space-y-6">
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-wrap items-center justify-between gap-4"
      >
        <div>
          <h1 className="font-display text-2xl font-bold text-slate-100">
            Quản lý đơn hàng
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {data?.totalElements ?? 0} đơn hàng trong hệ thống
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => handleExport("csv")}
            disabled={isExporting || isLoading}
            className="flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-xs font-medium text-slate-300 hover:bg-slate-800 disabled:opacity-50"
          >
            <Download className="h-4 w-4" /> Xuất CSV
          </button>
          <button
            onClick={() => handleExport("excel")}
            disabled={isExporting || isLoading}
            className="flex items-center gap-2 rounded-xl bg-pine-500 px-3 py-2 text-xs font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 disabled:opacity-50"
          >
            <FileDown className="h-4 w-4" /> Xuất Excel
          </button>
        </div>
      </motion.div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm khách hàng, mã đơn..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-slate-500" />
          <div className="flex gap-1.5">
            {ORDER_STATUSES.map((s) => (
              <button
                key={s.value}
                onClick={() => {
                  setStatusFilter(s.value);
                  setPagination((p) => ({ ...p, pageIndex: 0 }));
                }}
                className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                  statusFilter === s.value
                    ? "bg-pine-500 text-white"
                    : "bg-slate-800 text-slate-400 hover:bg-slate-700"
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Bulk actions */}
      <AnimatePresence>
        {selectedIds.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-pine-500/30 bg-pine-950/20 px-4 py-3"
          >
            <span className="text-xs font-medium text-slate-300">
              Đã chọn <strong className="text-pine-400 font-semibold">{selectedIds.length}</strong> đơn hàng
            </span>
            <div className="flex items-center gap-3">
              <select
                value={bulkStatus}
                onChange={(e) => setBulkStatus(e.target.value as OrderStatus)}
                className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
              >
                <option value="">-- Thay đổi trạng thái --</option>
                <option value="CONFIRMED">Đã xác nhận</option>
                <option value="PROCESSING">Đang xử lý</option>
                <option value="SHIPPING">Đang giao hàng</option>
                <option value="DELIVERED">Đã giao hàng</option>
                <option value="REFUND_REQUESTED">Yêu cầu hoàn tiền</option>
                <option value="REFUNDED">Đã hoàn tiền</option>
                <option value="CANCELLED">Hủy đơn hàng</option>
              </select>
              <button
                onClick={() => {
                  if (!bulkStatus) return;
                  bulkUpdateStatus.mutate();
                }}
                disabled={bulkUpdateStatus.isPending || !bulkStatus}
                className="rounded-lg bg-pine-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-pine-600 disabled:opacity-50"
              >
                Cập nhật
              </button>
              <button
                onClick={() => setSelectedIds([])}
                className="text-xs text-slate-400 hover:text-slate-200 transition-colors"
              >
                Hủy chọn
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <DataTable
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
      />

      {/* Order Details Modal */}
      <AnimatePresence>
        {detailOrderId && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setDetailOrderId(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl overflow-y-auto max-h-[90vh] scrollbar-thin"
            >
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="font-display text-lg font-semibold text-slate-100 flex items-center gap-2">
                  <span>Chi tiết đơn hàng:</span>
                  <span className="font-mono text-pine-400">#{detailOrderId}</span>
                </h2>
                <button
                  onClick={() => setDetailOrderId(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {detailLoading ? (
                <div className="space-y-4 py-8">
                  <div className="h-6 animate-pulse rounded bg-slate-800 w-1/3" />
                  <div className="h-20 animate-pulse rounded bg-slate-800 w-full" />
                  <div className="h-32 animate-pulse rounded bg-slate-800 w-full" />
                </div>
              ) : !orderDetail ? (
                <div className="py-8 text-center text-sm text-slate-500">
                  Không thể tìm thấy chi tiết đơn hàng.
                </div>
              ) : (
                <div className="mt-4 space-y-6">
                  {/* Order Overview info */}
                  <div className="grid grid-cols-2 gap-4 rounded-xl bg-slate-800/40 p-4 text-xs">
                    <div>
                      <p className="text-slate-500">Khách hàng</p>
                      <p className="mt-1 font-semibold text-slate-200">{orderDetail.userFullName ?? "—"}</p>
                      <p className="text-slate-400">{orderDetail.userEmail ?? ""}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Thời gian đặt</p>
                      <p className="mt-1 font-semibold text-slate-200">{formatDateTime(orderDetail.createdAt)}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Trạng thái thanh toán</p>
                      <span
                        className={`inline-flex mt-1 rounded-full border px-2 py-0.5 font-medium ${
                          orderDetail.paymentStatus === "PAID"
                            ? "border-pine-500/30 bg-pine-500/10 text-pine-400"
                            : "border-amber-500/30 bg-amber-500/10 text-amber-400"
                        }`}
                      >
                        {orderDetail.paymentStatus} · {orderDetail.paymentMethod}
                      </span>
                    </div>
                    <div>
                      <p className="text-slate-500">Trạng thái đơn hàng</p>
                      <div className="mt-1">
                        <StatusBadge status={orderDetail.status} />
                      </div>
                    </div>
                  </div>

                  {/* Payment transaction details */}
                  {paymentLoading ? (
                    <div className="h-10 w-full animate-pulse rounded-xl bg-slate-800/40" />
                  ) : paymentDetail ? (
                    <div className="rounded-xl border border-slate-800 bg-slate-950/20 p-4 text-xs space-y-2">
                      <div className="flex items-center gap-1.5 font-semibold text-slate-200">
                        <CreditCard className="h-4 w-4 text-pine-400" />
                        <span>Chi tiết giao dịch thanh toán</span>
                      </div>
                      <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-[11px] text-slate-400 mt-2">
                        <div>
                          <p>Phương thức:</p>
                          <p className="font-semibold text-slate-300 mt-0.5">{paymentDetail.method}</p>
                        </div>
                        <div>
                          <p>Mã giao dịch:</p>
                          <p className="font-mono font-semibold text-slate-300 mt-0.5">{paymentDetail.transactionRef ?? "—"}</p>
                        </div>
                        <div>
                          <p>Số tiền:</p>
                          <p className="font-semibold text-pine-400 mt-0.5">{formatCurrency(paymentDetail.amount)}</p>
                        </div>
                        <div>
                          <p>Trạng thái GD:</p>
                          <span className={`inline-block mt-0.5 rounded px-1.5 py-0.25 font-bold ${
                            paymentDetail.status === "PAID"
                              ? "bg-pine-500/10 text-pine-400"
                              : paymentDetail.status === "REFUNDED"
                              ? "bg-violet-500/10 text-violet-400"
                              : "bg-red-500/10 text-red-400"
                          }`}>
                            {paymentDetail.status}
                          </span>
                        </div>
                        {paymentDetail.paidAt && (
                          <div className="col-span-2">
                            <p>Thời gian thanh toán:</p>
                            <p className="font-semibold text-slate-300 mt-0.5">{formatDateTime(paymentDetail.paidAt)}</p>
                          </div>
                        )}
                      </div>
                    </div>
                  ) : null}

                  {/* Shipping info */}
                  <div className="space-y-1 text-xs">
                    <p className="text-slate-500 font-medium">Địa chỉ giao hàng</p>
                    <p className="text-slate-300 font-semibold">{orderDetail.shippingAddress}</p>
                    {orderDetail.note && (
                      <div className="mt-2 rounded-lg bg-slate-800/20 border border-slate-800 p-2.5">
                        <p className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Ghi chú</p>
                        <p className="text-slate-400 mt-0.5">{orderDetail.note}</p>
                      </div>
                    )}
                  </div>

                  {/* Shipping shipment details */}
                  <div className="border-t border-slate-800/60 pt-4 space-y-3">
                    <div className="flex items-center gap-1.5 font-semibold text-slate-200 text-xs">
                      <Truck className="h-4 w-4 text-pine-400" />
                      <span>Chi tiết vận đơn giao nhận</span>
                    </div>

                    {shippingLoading ? (
                      <div className="h-20 w-full animate-pulse rounded-xl bg-slate-800/40" />
                    ) : !shippingTracking ? (
                      /* No Shipment Exists Yet */
                      <div className="rounded-xl border border-slate-800 bg-slate-950/20 p-4 text-xs space-y-3">
                        <p className="text-slate-400 text-[11px]">Đơn hàng này chưa được tạo vận đơn giao hàng trên các đơn vị vận chuyển.</p>
                        
                        {(orderDetail.status === "CONFIRMED" || orderDetail.status === "PROCESSING") && (
                          <div className="flex flex-wrap items-center gap-3 mt-2 border-t border-slate-800/60 pt-3">
                            <div className="flex items-center gap-2">
                              <span className="text-slate-500 text-[10px] uppercase font-bold tracking-wider">Chọn ĐVVC:</span>
                              <select
                                value={selectedCarrier}
                                onChange={(e) => setSelectedCarrier(e.target.value as "GHN" | "GHTK")}
                                className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-2 text-xs text-slate-200 outline-none focus:border-pine-500"
                              >
                                <option value="GHN">GHN (Giao Hàng Nhanh)</option>
                                <option value="GHTK">GHTK (Giao Hàng Tiết Kiệm)</option>
                              </select>
                            </div>
                            <button
                              onClick={() => {
                                createShipmentMutation.mutate({
                                  orderId: orderDetail.id,
                                  carrier: selectedCarrier,
                                });
                              }}
                              disabled={createShipmentMutation.isPending}
                              className="rounded-lg bg-pine-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-pine-600 active:scale-[0.98] transition disabled:opacity-50"
                            >
                              {createShipmentMutation.isPending ? "Đang tạo..." : "Tạo vận đơn"}
                            </button>
                          </div>
                        )}
                      </div>
                    ) : (
                      /* Shipment Exists */
                      <div className="space-y-3">
                        <div className="rounded-xl border border-slate-800 bg-slate-950/20 p-4 text-xs space-y-2">
                          <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-[11px] text-slate-400">
                            <div>
                              <p>Đơn vị vận chuyển:</p>
                              <p className="font-semibold text-slate-300 mt-0.5">{shippingTracking.carrierName}</p>
                            </div>
                            <div>
                              <p>Mã vận đơn:</p>
                              <p className="font-mono font-semibold text-pine-400 mt-0.5">{shippingTracking.externalOrderCode}</p>
                            </div>
                            <div>
                              <p>Trạng thái vận đơn:</p>
                              <span className="inline-block mt-0.5 rounded bg-pine-500/10 border border-pine-500/20 px-2 py-0.5 text-xs font-semibold text-pine-400">
                                {shippingTracking.currentStatusLabel}
                              </span>
                            </div>
                            {shippingTracking.expectedDeliveryTime && (
                              <div>
                                <p>Ngày giao dự kiến:</p>
                                <p className="font-semibold text-slate-300 mt-0.5">{formatDateTime(shippingTracking.expectedDeliveryTime)}</p>
                              </div>
                            )}
                            <div>
                              <p>Phí ship gốc:</p>
                              <p className="font-semibold text-slate-300 mt-0.5">{formatCurrency(shippingTracking.totalFee)}</p>
                            </div>
                            {shippingTracking.lastSyncAt && (
                              <div>
                                <p>Đồng bộ lần cuối:</p>
                                <p className="font-semibold text-slate-500 mt-0.5">{formatDateTime(shippingTracking.lastSyncAt)}</p>
                              </div>
                            )}
                          </div>

                          <div className="flex flex-wrap gap-2.5 mt-4 pt-3 border-t border-slate-800/60">
                            <button
                              onClick={() => syncShipmentMutation.mutate(orderDetail.id)}
                              disabled={syncShipmentMutation.isPending}
                              className="flex items-center gap-1.5 rounded-lg border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs font-medium text-slate-300 hover:bg-slate-700 disabled:opacity-50"
                            >
                              <RefreshCw className={`h-3.5 w-3.5 ${syncShipmentMutation.isPending ? "animate-spin" : ""}`} />
                              <span>Đồng bộ trạng thái</span>
                            </button>

                            {(shippingTracking.currentStatus === "PENDING_PICKUP" || shippingTracking.currentStatus === "PICKING_UP") && (
                              <button
                                onClick={() => {
                                  if (confirm("Bạn có chắc chắn muốn hủy vận đơn giao hàng này không?")) {
                                    cancelShipmentMutation.mutate(orderDetail.id);
                                  }
                                }}
                                disabled={cancelShipmentMutation.isPending}
                                className="flex items-center gap-1.5 rounded-lg bg-red-500/10 border border-red-500/20 px-3 py-1.5 text-xs font-semibold text-red-400 hover:bg-red-500/20 disabled:opacity-50 transition"
                              >
                                <X className="h-3.5 w-3.5" />
                                <span>Hủy vận đơn</span>
                              </button>
                            )}
                          </div>
                        </div>

                        {/* Status history log */}
                        {shippingTracking.statusHistory && shippingTracking.statusHistory.length > 0 && (
                          <div className="rounded-xl border border-slate-800 bg-slate-950/10 p-4 space-y-2">
                            <p className="text-[10px] uppercase font-bold tracking-wider text-slate-500">Hành trình chi tiết</p>
                            <div className="relative border-l border-slate-800 pl-4 ml-1.5 space-y-3 mt-2 text-xs">
                              {shippingTracking.statusHistory.map((logItem, idx) => (
                                <div key={idx} className="relative">
                                  <div className="absolute -left-[21px] top-1.5 h-2 w-2 rounded-full bg-pine-500 border border-slate-900" />
                                  <p className="font-semibold text-slate-300">{logItem.statusLabel}</p>
                                  <p className="text-[10px] text-slate-500 mt-0.5">{formatDateTime(logItem.updatedAt)}</p>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Order items */}
                  <div>
                    <p className="text-xs font-semibold text-slate-400 mb-2">Sản phẩm đã mua</p>
                    <div className="overflow-hidden rounded-xl border border-slate-800">
                      <table className="w-full text-left text-xs">
                        <thead>
                          <tr className="border-b border-slate-800 bg-slate-800/40 text-slate-400">
                            <th className="px-4 py-2 font-medium">Sản phẩm</th>
                            <th className="px-4 py-2 font-medium text-center">SL</th>
                            <th className="px-4 py-2 font-medium text-right">Đơn giá</th>
                            <th className="px-4 py-2 font-medium text-right">Thành tiền</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-800 text-slate-300">
                          {orderDetail.items.map((item) => (
                            <tr key={item.id}>
                              <td className="px-4 py-2.5 flex items-center gap-2.5">
                                <img
                                  src={item.productThumbnail}
                                  alt={item.productName}
                                  className="h-8 w-8 rounded-lg object-cover bg-slate-800 border border-slate-700"
                                />
                                <span className="font-medium text-slate-200">{item.productName}</span>
                              </td>
                              <td className="px-4 py-2.5 text-center font-medium">
                                {item.quantity} {item.productUnit}
                              </td>
                              <td className="px-4 py-2.5 text-right font-medium">
                                {formatCurrency(item.unitPrice)}
                                {item.productUnit && (
                                  <span className="text-[10px] text-slate-500 ml-0.5">
                                    / {item.productUnit}
                                  </span>
                                )}
                              </td>
                              <td className="px-4 py-2.5 text-right font-semibold text-slate-100">
                                {formatCurrency(item.subtotal)}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>

                  {/* Order pricing summary */}
                  <div className="border-t border-slate-800 pt-4 flex flex-col items-end gap-1.5 text-xs text-slate-400">
                    <div className="flex justify-between w-64">
                      <span>Tạm tính:</span>
                      <span className="font-medium text-slate-300">{formatCurrency(orderDetail.subtotal)}</span>
                    </div>
                    <div className="flex justify-between w-64">
                      <span>Phí vận chuyển:</span>
                      <span className="font-medium text-slate-300">+{formatCurrency(orderDetail.shippingFee)}</span>
                    </div>
                    {orderDetail.discountAmount > 0 && (
                      <div className="flex justify-between w-64 text-pine-400">
                        <span>Giảm giá:</span>
                        <span className="font-medium">-{formatCurrency(orderDetail.discountAmount)}</span>
                      </div>
                    )}
                    <div className="flex justify-between w-64 border-t border-slate-800 pt-2 text-sm">
                      <span className="font-semibold text-slate-200">Tổng cộng:</span>
                      <span className="font-bold text-pine-400">{formatCurrency(orderDetail.totalAmount)}</span>
                    </div>
                  </div>

                  {/* Actions for detail */}
                  {ALLOWED_TRANSITIONS[orderDetail.status] && ALLOWED_TRANSITIONS[orderDetail.status].length > 0 && (
                    <div className="border-t border-slate-800 pt-4 flex flex-col gap-2.5">
                      <span className="text-xs text-slate-500 font-medium">
                        Cập nhật trạng thái đơn hàng:
                      </span>
                      <div className="flex flex-wrap gap-2.5">
                        {ALLOWED_TRANSITIONS[orderDetail.status].map((nextStat) => {
                          const action = STATUS_TRANSITION_ACTIONS[nextStat];
                          return (
                            <button
                              key={nextStat}
                              onClick={() => {
                                if (nextStat === "CANCELLED") {
                                  if (!confirm(`Bạn có chắc chắn muốn hủy đơn hàng #${orderDetail.id} không?`)) {
                                    return;
                                  }
                                }
                                updateStatus.mutate({
                                  id: orderDetail.id,
                                  status: nextStat,
                                });
                              }}
                              disabled={updateStatus.isPending}
                              className={`rounded-lg px-4 py-2 text-xs font-semibold transition disabled:opacity-50 active:scale-[0.98] ${
                                action?.className || "bg-pine-500 text-white hover:bg-pine-600"
                              }`}
                            >
                              {updateStatus.isPending ? "Đang xử lý..." : action?.label || nextStat}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}