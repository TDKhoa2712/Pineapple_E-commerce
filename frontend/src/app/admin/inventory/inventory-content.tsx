"use client";

import { useState, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import Link from "next/link";
import {
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { format, subMonths } from "date-fns";
import {
  CalendarDays,
  Package,
  TrendingDown,
  Archive,
  X,
  Plus,
  AlertTriangle,
  RefreshCw,
  BarChart3,
  History,
  Search,
  ArrowUp,
  ArrowDown,
  ArrowUpDown,
} from "lucide-react";
import { toast } from "sonner";
import {
  adminInventoryService,
  adminProductService,
  adminFarmService,
} from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { formatDate, truncate } from "@/lib/utils";
import type { InventoryBatchResponse, BatchStatus } from "@/types";

type TabKey = "batches" | "expiring" | "summary" | "report";

const TABS: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: "batches", label: "Lô hàng", icon: Package },
  { key: "expiring", label: "Sắp hết hạn", icon: AlertTriangle },
  { key: "summary", label: "Tổng hợp tồn", icon: BarChart3 },
  { key: "report", label: "Báo cáo", icon: CalendarDays },
];

export function InventoryContent() {
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabKey>("batches");
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [summaryPagination, setSummaryPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 });
  const [adjustTarget, setAdjustTarget] = useState<InventoryBatchResponse | null>(null);
  const [adjustQty, setAdjustQty] = useState<number>(0);
  const [adjustReason, setAdjustReason] = useState<string>("");
  const [historyTarget, setHistoryTarget] = useState<InventoryBatchResponse | null>(null);
  const [showAddBatch, setShowAddBatch] = useState(false);
  const [expiringDays, setExpiringDays] = useState(7);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [rejectTarget, setRejectTarget] = useState<InventoryBatchResponse | null>(null);
  const [rejectReason, setRejectReason] = useState<string>("");

  // Summary states
  const [summaryKeyword, setSummaryKeyword] = useState("");
  const [summarySortBy, setSummarySortBy] = useState("totalStock");
  const [summarySortDirection, setSummarySortDirection] = useState<"asc" | "desc">("desc");

  // Report states
  const [reportKeyword, setReportKeyword] = useState("");
  const [reportGroupBy, setReportGroupBy] = useState<"day" | "week" | "month">("day");

  const handleSummarySort = (field: string) => {
    if (summarySortBy === field) {
      setSummarySortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSummarySortBy(field);
      setSummarySortDirection("desc");
    }
    setSummaryPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  // Search and Sort states
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Add batch form state
  const [addBatchForm, setAddBatchForm] = useState({
    productId: "",
    farmId: "",
    batchCode: "",
    quantity: "",
    harvestDate: "",
    expiryDate: "",
    sweetnessLevel: "",
  });

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("desc");
    }
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  // ─── Queries ────────────────────────────────────────────────

  // Fetch products (for dropdown selector)
  const { data: allProducts } = useQuery({
    queryKey: ["admin-all-products-for-select"],
    queryFn: () => adminProductService.getAll({ page: 0, size: 1000 }),
  });

  // Fetch farms (for dropdown selector)
  const { data: allFarms } = useQuery({
    queryKey: ["admin-all-farms-for-select"],
    queryFn: () => adminFarmService.getAll({ page: 0, size: 1000 }),
  });

  const { data: batches, isLoading: batchesLoading } = useQuery({
    queryKey: queryKeys.inventory({
      page: pagination.pageIndex,
      size: pagination.pageSize,
      keyword,
      sortBy,
      sortDirection,
      status: statusFilter,
    }),
    queryFn: () =>
      adminInventoryService.getBatches({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        keyword: keyword || undefined,
        sortBy,
        sortDirection,
        status: statusFilter || undefined,
      }),
    enabled: activeTab === "batches",
  });

  const { data: expiringSoon, isLoading: expiringLoading } = useQuery({
    queryKey: queryKeys.inventoryExpiring(expiringDays),
    queryFn: () => adminInventoryService.getExpiringSoon(expiringDays),
    enabled: activeTab === "expiring",
  });

  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: queryKeys.inventorySummary({
      page: summaryPagination.pageIndex,
      size: summaryPagination.pageSize,
      keyword: summaryKeyword,
      sortBy: summarySortBy,
      sortDirection: summarySortDirection,
    }),
    queryFn: () =>
      adminInventoryService.getSummary({
        page: summaryPagination.pageIndex,
        size: summaryPagination.pageSize,
        keyword: summaryKeyword || undefined,
        sortBy: summarySortBy,
        sortDirection: summarySortDirection,
      }),
    enabled: activeTab === "summary",
  });

  const [dateRange, setDateRange] = useState({
    from: format(subMonths(new Date(), 1), "yyyy-MM-dd"),
    to: format(new Date(), "yyyy-MM-dd"),
  });

  const { data: report, isLoading: reportLoading } = useQuery({
    queryKey: queryKeys.inventoryReport({ ...dateRange, groupBy: reportGroupBy }),
    queryFn: () => adminInventoryService.getReport({ ...dateRange, groupBy: reportGroupBy }),
    enabled: activeTab === "report" && !!dateRange.from && !!dateRange.to,
  });

  const { data: adjustments, isLoading: adjustmentsLoading } = useQuery({
    queryKey: queryKeys.inventoryAdjustments(historyTarget?.id ?? 0),
    queryFn: () => adminInventoryService.getAdjustments(historyTarget!.id),
    enabled: !!historyTarget,
  });

  // Client-side report filtering and exporting
  const filteredReportDetails = useMemo(() => {
    if (!report?.details) return [];
    return report.details.filter((d) =>
      d.productName.toLowerCase().includes(reportKeyword.toLowerCase()) ||
      d.productId.toString().includes(reportKeyword)
    );
  }, [report?.details, reportKeyword]);

  const handleExportCSV = () => {
    if (!report || !report.details.length) return;
    
    const headers = [
      "ID sản phẩm",
      "Tên sản phẩm",
      "Số lô nhập",
      "Số lượng nhập",
      "Số lượng bán",
      "Số lô hết hạn",
      "Số lượng hết hạn",
      "Tồn kho hiện tại",
      "Nhập sớm nhất",
      "Nhập muộn nhất"
    ];
    
    const rows = report.details.map(d => [
      d.productId,
      `"${d.productName.replace(/"/g, '""')}"`,
      d.batchesImported,
      d.quantityImported,
      d.quantitySold,
      d.batchesExpired,
      d.quantityExpired,
      d.currentStock,
      d.earliestImport || "-",
      d.latestImport || "-"
    ]);
    
    const csvContent = "\uFEFF" + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
    
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `bao-cao-kho-hang_${dateRange.from}_to_${dateRange.to}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // ─── Mutations ──────────────────────────────────────────────

  const adjustStock = useMutation({
    mutationFn: ({ id, qty, reason }: { id: number; qty: number; reason: string }) =>
      adminInventoryService.adjustBatch(id, { adjustmentQty: qty, reason }),
    onSuccess: () => {
      toast.success("Điều chỉnh tồn kho thành công");
      qc.invalidateQueries({ queryKey: ["inventory"] });
      setAdjustTarget(null);
      setAdjustQty(0);
      setAdjustReason("");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Điều chỉnh thất bại");
    },
  });

  const addBatch = useMutation({
    mutationFn: () =>
      adminInventoryService.addBatch({
        productId: parseInt(addBatchForm.productId),
        farmId: addBatchForm.farmId ? parseInt(addBatchForm.farmId) : undefined,
        batchCode: addBatchForm.batchCode,
        quantity: parseInt(addBatchForm.quantity),
        harvestDate: addBatchForm.harvestDate,
        expiryDate: addBatchForm.expiryDate,
        sweetnessLevel: addBatchForm.sweetnessLevel
          ? parseFloat(addBatchForm.sweetnessLevel)
          : undefined,
      }),
    onSuccess: () => {
      toast.success("Nhập lô hàng thành công");
      qc.invalidateQueries({ queryKey: ["inventory"] });
      setShowAddBatch(false);
      setAddBatchForm({
        productId: "", farmId: "", batchCode: "", quantity: "",
        harvestDate: "", expiryDate: "", sweetnessLevel: "",
      });
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Nhập lô hàng thất bại");
    },
  });

  const markExpiredMutation = useMutation({
    mutationFn: () => adminInventoryService.markExpired(),
    onSuccess: (data) => {
      toast.success(`Đã đánh dấu ${data?.markedCount ?? 0} lô hết hạn`);
      qc.invalidateQueries({ queryKey: ["inventory"] });
    },
    onError: () => toast.error("Thao tác thất bại"),
  });

  const approveBatchMutation = useMutation({
    mutationFn: (batchId: number) => adminInventoryService.approveBatch(batchId),
    onSuccess: () => {
      toast.success("Đã duyệt lô hàng");
      qc.invalidateQueries({ queryKey: ["inventory"] });
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Duyệt lô hàng thất bại");
    },
  });

  const rejectBatchMutation = useMutation({
    mutationFn: ({ batchId, reason }: { batchId: number; reason: string }) =>
      adminInventoryService.rejectBatch(batchId, { reason }),
    onSuccess: () => {
      toast.success("Đã từ chối lô hàng");
      qc.invalidateQueries({ queryKey: ["inventory"] });
      setRejectTarget(null);
      setRejectReason("");
    },
    onError: (error: any) => {
      toast.error(error?.response?.data?.message || "Từ chối lô hàng thất bại");
    },
  });

  // ─── Column definitions ──────────────────────────────────────

  const batchColumns: ColumnDef<InventoryBatchResponse, unknown>[] = [
    {
      accessorKey: "batchCode",
      header: () => (
        <button
          onClick={() => handleSort("batchCode")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Mã lô
          {sortBy === "batchCode" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => (
        <span className="font-mono text-xs font-semibold text-cyan-400">
          {getValue<string>()}
        </span>
      ),
    },
    {
      accessorKey: "productName",
      header: "Sản phẩm",
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-300">{getValue<string>()}</span>
      ),
    },
    {
      accessorKey: "farmName",
      header: "Nông trại",
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-400">{getValue<string>()}</span>
      ),
    },
    {
      id: "qty",
      header: () => (
        <button
          onClick={() => handleSort("remainingQuantity")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Tồn / Tổng
          {sortBy === "remainingQuantity" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <div className="h-1.5 w-16 overflow-hidden rounded-full bg-slate-700">
            <div
              className="h-full rounded-full bg-pine-500 transition-all"
              style={{
                width: `${Math.min(100, (row.original.remainingQuantity / row.original.quantity) * 100)}%`,
              }}
            />
          </div>
          <span className="text-xs text-slate-400">
            {row.original.remainingQuantity}/{row.original.quantity}
          </span>
        </div>
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
      cell: ({ getValue }) => <StatusBadge status={getValue<BatchStatus>()} />,
    },
    {
      accessorKey: "rejectionReason",
      header: "Lý do",
      cell: ({ row }) => {
        const reason = row.original.rejectionReason;
        if (!reason) return <span className="text-xs text-slate-600">—</span>;
        return <span className="text-xs text-slate-400">{reason}</span>;
      },
    },
    {
      accessorKey: "harvestDate",
      header: () => (
        <button
          onClick={() => handleSort("harvestDate")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Thu hoạch
          {sortBy === "harvestDate" ? (
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
      cell: ({ row }) => (
        <div className="flex gap-2">
          {row.original.status === "PENDING_APPROVAL" && (
            <>
              <button
                onClick={() => approveBatchMutation.mutate(row.original.id)}
                disabled={approveBatchMutation.isPending}
                className="rounded-lg bg-pine-500/10 px-2.5 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 disabled:opacity-50 transition-colors"
              >
                Duyệt
              </button>
              <button
                onClick={() => {
                  setRejectTarget(row.original);
                  setRejectReason("");
                }}
                className="rounded-lg bg-red-500/10 px-2.5 py-1.5 text-xs font-medium text-red-400 hover:bg-red-500/20 transition-colors"
              >
                Từ chối
              </button>
            </>
          )}
          <button
            onClick={() => {
              setAdjustTarget(row.original);
              setAdjustQty(0);
              setAdjustReason("");
            }}
            className="rounded-lg bg-pine-500/10 px-2.5 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20 transition-colors"
          >
            Điều chỉnh
          </button>
          <button
            onClick={() => setHistoryTarget(row.original)}
            className="rounded-lg bg-cyan-500/10 px-2.5 py-1.5 text-xs font-medium text-cyan-400 hover:bg-cyan-500/20 transition-colors"
          >
            Lịch sử
          </button>
        </div>
      ),
    },
  ];

  const chartData = report?.details.slice(0, 10).map((d) => ({
    name: truncate(d.productName, 15),
    imported: d.quantityImported,
    sold: d.quantitySold,
    current: d.currentStock,
  }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-wrap items-center justify-between gap-4"
      >
        <div>
          <h1 className="font-display text-2xl font-bold text-slate-100">
            Kho hàng & Tồn kho
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            Theo dõi nhập xuất tồn và tình trạng lô hàng
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => markExpiredMutation.mutate()}
            disabled={markExpiredMutation.isPending}
            className="flex items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs font-medium text-amber-400 hover:bg-amber-500/20 disabled:opacity-50 transition-colors"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${markExpiredMutation.isPending ? "animate-spin" : ""}`} />
            Đánh dấu hết hạn
          </button>
          <button
            onClick={() => setShowAddBatch(true)}
            className="flex items-center gap-2 rounded-xl bg-pine-500 px-3 py-2 text-xs font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" /> Nhập lô hàng
          </button>
        </div>
      </motion.div>

      {/* Tabs */}
      <div className="flex gap-1 rounded-xl border border-slate-800 bg-slate-900 p-1">
        {TABS.map((tab) => {
          const Icon = tab.icon;
          return (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-3 py-2 text-xs font-medium transition-colors ${
                activeTab === tab.key
                  ? "bg-pine-500 text-white shadow-sm"
                  : "text-slate-400 hover:bg-slate-800 hover:text-slate-200"
              }`}
            >
              <Icon className="h-3.5 w-3.5" />
              <span className="hidden sm:block">{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* ── Tab: Batches ── */}
      {activeTab === "batches" && (
        <div className="space-y-4">
          {/* Filters */}
          <div className="flex flex-wrap gap-3">
            <div className="relative flex-1 min-w-[200px]">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="Tìm kiếm theo mã lô, tên sản phẩm hoặc nông trại..."
                value={keyword}
                onChange={(e) => {
                  setKeyword(e.target.value);
                  setPagination((p) => ({ ...p, pageIndex: 0 }));
                }}
                className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
              />
            </div>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPagination((p) => ({ ...p, pageIndex: 0 }));
              }}
              className="h-9 rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PENDING_APPROVAL">Chờ duyệt</option>
              <option value="REJECTED">Từ chối</option>
              <option value="AVAILABLE">Còn hàng</option>
              <option value="SOLD_OUT">Hết hàng</option>
              <option value="EXPIRED">Hết hạn</option>
            </select>
          </div>

          <DataTable
            data={batches?.content ?? []}
            columns={batchColumns}
            pageCount={batches?.totalPages ?? 0}
            pagination={pagination}
            onPaginationChange={setPagination}
            isLoading={batchesLoading}
          />
        </div>
      )}

      {/* ── Tab: Expiring Soon ── */}
      {activeTab === "expiring" && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 rounded-xl border border-slate-800 bg-slate-900 p-4">
            <AlertTriangle className="h-4 w-4 text-amber-400" />
            <span className="text-sm text-slate-400">Hiển thị lô hàng hết hạn trong</span>
            <select
              value={expiringDays}
              onChange={(e) => setExpiringDays(parseInt(e.target.value))}
              className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
            >
              <option value={3}>3 ngày</option>
              <option value={7}>7 ngày</option>
              <option value={14}>14 ngày</option>
              <option value={30}>30 ngày</option>
            </select>
          </div>

          {expiringLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="h-12 animate-pulse rounded-lg bg-slate-800" />
              ))}
            </div>
          ) : !expiringSoon || expiringSoon.length === 0 ? (
            <div className="rounded-xl border border-slate-800 bg-slate-900 py-12 text-center">
              <Package className="mx-auto h-10 w-10 text-slate-600 mb-3" />
              <p className="text-sm text-slate-500">
                Không có lô hàng nào sắp hết hạn trong {expiringDays} ngày tới
              </p>
            </div>
          ) : (
            <div className="overflow-hidden rounded-xl border border-slate-800">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-800 bg-slate-800/40 text-slate-400">
                    <th className="px-4 py-2.5 font-medium">Mã lô</th>
                    <th className="px-4 py-2.5 font-medium">Sản phẩm</th>
                    <th className="px-4 py-2.5 font-medium">Nông trại</th>
                    <th className="px-4 py-2.5 font-medium text-center">Còn lại</th>
                    <th className="px-4 py-2.5 font-medium">Hết hạn</th>
                    <th className="px-4 py-2.5 font-medium">Trạng thái</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {expiringSoon.map((batch) => (
                    <motion.tr
                      key={batch.id}
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      className="text-slate-300 hover:bg-slate-800/30 transition-colors"
                    >
                      <td className="px-4 py-2.5 font-mono font-semibold text-amber-400">
                        {batch.batchCode}
                      </td>
                      <td className="px-4 py-2.5">{truncate(batch.productName, 30)}</td>
                      <td className="px-4 py-2.5 text-slate-400">{batch.farmName}</td>
                      <td className="px-4 py-2.5 text-center font-semibold">
                        {batch.remainingQuantity}
                      </td>
                      <td className="px-4 py-2.5 text-red-400 font-medium">
                        {formatDate(batch.expiryDate)}
                      </td>
                      <td className="px-4 py-2.5">
                        <StatusBadge status={batch.status} />
                      </td>
                    </motion.tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── Tab: Summary ── */}
      {activeTab === "summary" && (
        <div className="space-y-4">
          {/* Filters for Summary */}
          <div className="flex flex-wrap gap-3">
            <div className="relative flex-1 min-w-[200px]">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                placeholder="Tìm kiếm sản phẩm theo tên..."
                value={summaryKeyword}
                onChange={(e) => {
                  setSummaryKeyword(e.target.value);
                  setSummaryPagination((p) => ({ ...p, pageIndex: 0 }));
                }}
                className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
              />
            </div>
          </div>

          {summaryLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="h-12 animate-pulse rounded-lg bg-slate-800" />
              ))}
            </div>
          ) : !summary || summary.content.length === 0 ? (
            <div className="rounded-xl border border-slate-800 bg-slate-900 py-12 text-center">
              <Archive className="mx-auto h-10 w-10 text-slate-600 mb-3" />
              <p className="text-sm text-slate-500">Không có dữ liệu tồn kho</p>
            </div>
          ) : (
            <>
              <div className="overflow-hidden rounded-xl border border-slate-800">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 bg-slate-800/40 text-slate-400">
                      <th className="px-4 py-2.5 font-medium">
                        <button
                          onClick={() => handleSummarySort("productId")}
                          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
                        >
                          ID
                          {summarySortBy === "productId" ? (
                            summarySortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
                          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
                        </button>
                      </th>
                      <th className="px-4 py-2.5 font-medium">
                        <button
                          onClick={() => handleSummarySort("productName")}
                          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
                        >
                          Sản phẩm
                          {summarySortBy === "productName" ? (
                            summarySortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
                          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
                        </button>
                      </th>
                      <th className="px-4 py-2.5 font-medium text-center">
                        <button
                          onClick={() => handleSummarySort("batchCount")}
                          className="mx-auto flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
                        >
                          Số lô
                          {summarySortBy === "batchCount" ? (
                            summarySortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
                          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
                        </button>
                      </th>
                      <th className="px-4 py-2.5 font-medium text-right">
                        <button
                          onClick={() => handleSummarySort("totalStock")}
                          className="ml-auto flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
                        >
                          Tổng tồn kho
                          {summarySortBy === "totalStock" ? (
                            summarySortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
                          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
                        </button>
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {summary.content.map((item) => (
                      <tr key={item.productId} className="text-slate-300 hover:bg-slate-800/30 transition-colors">
                        <td className="px-4 py-2.5 font-mono text-slate-500">
                          #{item.productId}
                        </td>
                        <td className="px-4 py-2.5 font-medium text-slate-200">
                          {item.productName}
                        </td>
                        <td className="px-4 py-2.5 text-center text-cyan-400 font-semibold">
                          {item.batchCount}
                        </td>
                        <td className="px-4 py-2.5 text-right font-bold text-pine-400">
                          {item.totalStock.toLocaleString("vi-VN")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {/* Pagination */}
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-500">
                  {summary.totalElements} sản phẩm
                </span>
                <div className="flex gap-2">
                  <button
                    disabled={summaryPagination.pageIndex === 0}
                    onClick={() => setSummaryPagination((p) => ({ ...p, pageIndex: p.pageIndex - 1 }))}
                    className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-400 hover:bg-slate-800 disabled:opacity-40"
                  >
                    ← Trước
                  </button>
                  <button
                    disabled={summaryPagination.pageIndex >= summary.totalPages - 1}
                    onClick={() => setSummaryPagination((p) => ({ ...p, pageIndex: p.pageIndex + 1 }))}
                    className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-400 hover:bg-slate-800 disabled:opacity-40"
                  >
                    Sau →
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* ── Tab: Report ── */}
      {activeTab === "report" && (
        <div className="space-y-6">
          {/* Date range picker */}
          <div className="flex flex-wrap items-center gap-3 rounded-xl border border-slate-800 bg-slate-900 p-4">
            <CalendarDays className="h-4 w-4 text-slate-500" />
            <span className="text-sm text-slate-400">Kỳ báo cáo:</span>
            <input
              type="date"
              value={dateRange.from}
              onChange={(e) => setDateRange((p) => ({ ...p, from: e.target.value }))}
              className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
            />
            <span className="text-slate-600">→</span>
            <input
              type="date"
              value={dateRange.to}
              onChange={(e) => setDateRange((p) => ({ ...p, to: e.target.value }))}
              className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
            />
            <span className="text-sm text-slate-400 sm:ml-4">Xem theo:</span>
            <select
              value={reportGroupBy}
              onChange={(e) => setReportGroupBy(e.target.value as "day" | "week" | "month")}
              className="h-8 rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
            >
              <option value="day">Ngày</option>
              <option value="week">Tuần</option>
              <option value="month">Tháng</option>
            </select>
          </div>

          {/* Summary cards */}
          {report && (
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              {[
                {
                  label: "Tổng nhập",
                  value: report.summary.totalQuantityImported.toLocaleString("vi-VN"),
                  icon: Package,
                  color: "text-cyan-400",
                },
                {
                  label: "Đã bán",
                  value: report.summary.totalQuantitySold.toLocaleString("vi-VN"),
                  icon: TrendingDown,
                  color: "text-pine-400",
                },
                {
                  label: "Hết hạn",
                  value: report.summary.totalQuantityExpired.toLocaleString("vi-VN"),
                  icon: Archive,
                  color: "text-red-400",
                },
                {
                  label: "Còn tồn",
                  value: report.summary.currentAvailableStock.toLocaleString("vi-VN"),
                  icon: Package,
                  color: "text-amber-400",
                },
              ].map((card, i) => (
                <motion.div
                  key={card.label}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="rounded-xl border border-slate-800 bg-slate-900 p-4"
                >
                  <div className="flex items-center gap-2">
                    <card.icon className={`h-4 w-4 ${card.color}`} />
                    <span className="text-xs text-slate-500">{card.label}</span>
                  </div>
                  <p className={`mt-2 font-display text-xl font-bold ${card.color}`}>
                    {card.value}
                  </p>
                </motion.div>
              ))}
            </div>
          )}

          {/* Timeline Chart */}
          {report?.timeline && report.timeline.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="rounded-2xl border border-slate-800 bg-slate-900 p-5"
            >
              <div className="flex flex-wrap items-center justify-between gap-4">
                <h2 className="font-display text-base font-semibold text-slate-200">
                  Xu hướng Nhập / Xuất kho (Số lượng)
                </h2>
                <span className="text-xs text-slate-500">
                  Lịch sử theo {reportGroupBy === "day" ? "ngày" : reportGroupBy === "week" ? "tuần" : "tháng"}
                </span>
              </div>
              {reportLoading ? (
                <div className="mt-4 h-64 animate-pulse rounded-lg bg-slate-800" />
              ) : (
                <div className="mt-4 h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={report.timeline}>
                      <defs>
                        <linearGradient id="colorImported" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#22d3ee" stopOpacity={0.2}/>
                          <stop offset="95%" stopColor="#22d3ee" stopOpacity={0}/>
                        </linearGradient>
                        <linearGradient id="colorSold" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#4ade80" stopOpacity={0.2}/>
                          <stop offset="95%" stopColor="#4ade80" stopOpacity={0}/>
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis
                        dataKey="label"
                        tick={{ fill: "#475569", fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fill: "#475569", fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: "#1e293b",
                          border: "1px solid #334155",
                          borderRadius: "10px",
                          color: "#e2e8f0",
                          fontSize: "11px",
                        }}
                      />
                      <Legend
                        wrapperStyle={{ fontSize: "11px", color: "#94a3b8", paddingTop: "8px" }}
                      />
                      <Area
                        type="monotone"
                        dataKey="quantityImported"
                        name="Số lượng nhập"
                        stroke="#22d3ee"
                        fillOpacity={1}
                        fill="url(#colorImported)"
                      />
                      <Area
                        type="monotone"
                        dataKey="quantitySold"
                        name="Số lượng xuất (bán)"
                        stroke="#4ade80"
                        fillOpacity={1}
                        fill="url(#colorSold)"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              )}
            </motion.div>
          )}

          {/* Bar chart */}
          {(chartData?.length ?? 0) > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="rounded-2xl border border-slate-800 bg-slate-900 p-5"
            >
              <h2 className="font-display text-base font-semibold text-slate-200">
                Top 10 sản phẩm — nhập / bán / tồn
              </h2>
              {reportLoading ? (
                <div className="mt-4 h-52 animate-pulse rounded-lg bg-slate-800" />
              ) : (
                <div className="mt-4 h-52">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chartData} barGap={2}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis
                        dataKey="name"
                        tick={{ fill: "#475569", fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fill: "#475569", fontSize: 10 }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: "#1e293b",
                          border: "1px solid #334155",
                          borderRadius: "10px",
                          color: "#e2e8f0",
                          fontSize: "11px",
                        }}
                      />
                      <Legend
                        wrapperStyle={{ fontSize: "11px", color: "#94a3b8", paddingTop: "8px" }}
                      />
                      <Bar dataKey="imported" name="Nhập" fill="#22d3ee" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="sold" name="Bán" fill="#4ade80" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="current" name="Tồn" fill="#fbbf24" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}
            </motion.div>
          )}

          {/* Details Table & CSV export */}
          {report && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="space-y-4"
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="relative flex-1 min-w-[200px] max-w-md">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                  <input
                    type="text"
                    placeholder="Tìm kiếm sản phẩm trong báo cáo..."
                    value={reportKeyword}
                    onChange={(e) => setReportKeyword(e.target.value)}
                    className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                </div>
                <button
                  onClick={handleExportCSV}
                  disabled={!filteredReportDetails.length}
                  className="flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-4 py-2 text-xs font-medium text-slate-300 hover:bg-slate-700 disabled:opacity-50 transition-colors"
                >
                  Xuất báo cáo (CSV)
                </button>
              </div>

              {filteredReportDetails.length === 0 ? (
                <div className="rounded-xl border border-slate-800 bg-slate-900 py-12 text-center">
                  <Package className="mx-auto h-10 w-10 text-slate-600 mb-3" />
                  <p className="text-sm text-slate-500">Không tìm thấy sản phẩm phù hợp</p>
                </div>
              ) : (
                <div className="overflow-hidden rounded-xl border border-slate-800">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 bg-slate-800/40 text-slate-400">
                        <th className="px-4 py-2.5 font-medium">ID</th>
                        <th className="px-4 py-2.5 font-medium">Sản phẩm</th>
                        <th className="px-4 py-2.5 font-medium text-center">Số lô nhập</th>
                        <th className="px-4 py-2.5 font-medium text-right">Số lượng nhập</th>
                        <th className="px-4 py-2.5 font-medium text-right">Đã bán</th>
                        <th className="px-4 py-2.5 font-medium text-center">Lô hết hạn</th>
                        <th className="px-4 py-2.5 font-medium text-right">SL hết hạn</th>
                        <th className="px-4 py-2.5 font-medium text-right">Tồn hiện tại</th>
                        <th className="px-4 py-2.5 font-medium text-center">Nhập sớm nhất</th>
                        <th className="px-4 py-2.5 font-medium text-center">Nhập muộn nhất</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {filteredReportDetails.map((item) => (
                        <tr key={item.productId} className="text-slate-300 hover:bg-slate-800/30 transition-colors">
                          <td className="px-4 py-2.5 font-mono text-slate-500">#{item.productId}</td>
                          <td className="px-4 py-2.5 font-medium text-slate-200">{item.productName}</td>
                          <td className="px-4 py-2.5 text-center text-cyan-400 font-semibold">{item.batchesImported}</td>
                          <td className="px-4 py-2.5 text-right text-slate-300 font-medium">{item.quantityImported.toLocaleString("vi-VN")}</td>
                          <td className="px-4 py-2.5 text-right text-pine-400 font-semibold">{item.quantitySold.toLocaleString("vi-VN")}</td>
                          <td className="px-4 py-2.5 text-center text-amber-500">{item.batchesExpired}</td>
                          <td className="px-4 py-2.5 text-right text-red-400 font-medium">{item.quantityExpired.toLocaleString("vi-VN")}</td>
                          <td className="px-4 py-2.5 text-right text-amber-400 font-bold">{item.currentStock.toLocaleString("vi-VN")}</td>
                          <td className="px-4 py-2.5 text-center text-slate-500">{item.earliestImport ? formatDate(item.earliestImport.toString()) : "-"}</td>
                          <td className="px-4 py-2.5 text-center text-slate-500">{item.latestImport ? formatDate(item.latestImport.toString()) : "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </motion.div>
          )}
        </div>
      )}

      {/* ── Modal: Adjust stock ── */}
      <AnimatePresence>
        {adjustTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setAdjustTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="font-display text-lg font-semibold text-slate-100">
                  Điều chỉnh lô:{" "}
                  <span className="font-mono text-cyan-400">{adjustTarget.batchCode}</span>
                </h2>
                <button
                  onClick={() => setAdjustTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 space-y-4">
                <div>
                  <p className="text-xs text-slate-400">
                    Sản phẩm: <span className="text-slate-200">{adjustTarget.productName}</span>
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    Số lượng hiện tại:{" "}
                    <span className="font-semibold text-slate-200">
                      {adjustTarget.remainingQuantity} / {adjustTarget.quantity}
                    </span>
                  </p>
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Số lượng thay đổi * (Số dương = cộng, Số âm = trừ)
                  </label>
                  <input
                    type="number"
                    value={adjustQty || ""}
                    onChange={(e) => setAdjustQty(parseInt(e.target.value) || 0)}
                    placeholder="VD: 50 hoặc -20"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Lý do điều chỉnh *
                  </label>
                  <textarea
                    value={adjustReason}
                    onChange={(e) => setAdjustReason(e.target.value)}
                    rows={3}
                    placeholder="Nhập lý do điều chỉnh..."
                    className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 p-3 text-sm text-slate-200 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                </div>
              </div>

              <div className="mt-6 flex justify-end gap-3">
                <button
                  onClick={() => setAdjustTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  onClick={() =>
                    adjustStock.mutate({
                      id: adjustTarget.id,
                      qty: adjustQty,
                      reason: adjustReason,
                    })
                  }
                  disabled={adjustStock.isPending || !adjustQty || !adjustReason.trim()}
                  className="rounded-lg bg-pine-500 px-4 py-2 text-sm font-medium text-white hover:bg-pine-600 disabled:opacity-50"
                >
                  {adjustStock.isPending ? "Đang xử lý..." : "Xác nhận"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Modal: Adjustment history ── */}
      <AnimatePresence>
        {historyTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setHistoryTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl animate-in fade-in duration-200"
            >
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="font-display text-lg font-semibold text-slate-100 flex items-center gap-2">
                  <History className="h-5 w-5 text-cyan-400" />
                  Lịch sử điều chỉnh: <span className="font-mono text-cyan-400">{historyTarget.batchCode}</span>
                </h2>
                <button
                  onClick={() => setHistoryTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 max-h-[350px] overflow-y-auto pr-1 scrollbar-thin">
                {adjustmentsLoading ? (
                  <div className="space-y-2 py-4">
                    <div className="h-8 animate-pulse rounded-lg bg-slate-800" />
                    <div className="h-8 animate-pulse rounded-lg bg-slate-800" />
                    <div className="h-8 animate-pulse rounded-lg bg-slate-800" />
                  </div>
                ) : !adjustments || adjustments.length === 0 ? (
                  <div className="py-12 text-center text-sm text-slate-500">
                    Lô hàng này chưa được điều chỉnh kho thủ công lần nào.
                  </div>
                ) : (
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400">
                        <th className="pb-2 font-medium">Người chỉnh</th>
                        <th className="pb-2 font-medium text-center">Thay đổi</th>
                        <th className="pb-2 font-medium text-center">Tồn trước/sau</th>
                        <th className="pb-2 font-medium">Lý do</th>
                        <th className="pb-2 font-medium text-right">Ngày thực hiện</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800 text-slate-300">
                      {adjustments.map((log) => (
                        <tr key={log.id}>
                          <td className="py-2.5 font-medium">{log.adjustedByName}</td>
                          <td className="py-2.5 text-center font-bold">
                            <span className={log.adjustmentQty > 0 ? "text-pine-400" : "text-red-400"}>
                              {log.adjustmentQty > 0 ? `+${log.adjustmentQty}` : log.adjustmentQty}
                            </span>
                          </td>
                          <td className="py-2.5 text-center text-slate-400">
                            {log.qtyBefore} &rarr; {log.qtyAfter}
                          </td>
                          <td className="py-2.5 text-slate-300 max-w-[200px] break-words">
                            {log.reason}
                          </td>
                          <td className="py-2.5 text-right text-slate-500">
                            {formatDate(log.createdAt)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              <div className="mt-6 flex justify-end">
                <button
                  onClick={() => setHistoryTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Đóng
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Drawer: Add batch ── */}
      <AnimatePresence>
        {showAddBatch && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-end bg-black/60 backdrop-blur-sm"
            onClick={() => setShowAddBatch(false)}
          >
            <motion.div
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", stiffness: 300, damping: 30 }}
              onClick={(e) => e.stopPropagation()}
              className="h-full w-full max-w-md overflow-y-auto border-l border-slate-700 bg-slate-900 p-6"
            >
              <div className="flex items-center justify-between">
                <h2 className="font-display text-lg font-semibold text-slate-100">
                  Nhập lô hàng mới
                </h2>
                <button
                  onClick={() => setShowAddBatch(false)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-6 space-y-4">
                {/* Product Selection */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Sản phẩm *</label>
                  <select
                    value={addBatchForm.productId}
                    onChange={(e) =>
                      setAddBatchForm((p) => ({ ...p, productId: e.target.value }))
                    }
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  >
                    <option value="">— Chọn sản phẩm —</option>
                    {allProducts?.content?.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name} (ID: {p.id})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Farm Selection */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Nông trại *</label>
                  <select
                    value={addBatchForm.farmId}
                    onChange={(e) =>
                      setAddBatchForm((p) => ({ ...p, farmId: e.target.value }))
                    }
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  >
                    <option value="">— Chọn nông trại —</option>
                    {allFarms?.content?.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.name} (ID: {f.id})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Warning note about adding product first */}
                <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4 text-xs space-y-2">
                  <div className="flex items-start gap-2 text-slate-400">
                    <AlertTriangle className="h-4 w-4 shrink-0 text-amber-500 mt-0.5" />
                    <span>
                      <strong>Lưu ý:</strong> Nếu sản phẩm bạn cần nhập chưa có trong danh sách, bạn phải thêm sản phẩm mới vào hệ thống trước khi nhập lô.
                    </span>
                  </div>
                  <div className="pl-6">
                    <Link
                      href="/admin/products"
                      className="inline-flex items-center gap-1 text-pine-400 hover:text-pine-300 font-medium transition-colors"
                    >
                      Quản lý sản phẩm →
                    </Link>
                  </div>
                </div>

                {/* Batch code */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Mã lô hàng *</label>
                  <input
                    value={addBatchForm.batchCode}
                    onChange={(e) =>
                      setAddBatchForm((p) => ({ ...p, batchCode: e.target.value }))
                    }
                    placeholder="VD: BATCH-2026-001"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  />
                </div>

                {/* Quantity */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Số lượng *</label>
                  <input
                    type="number"
                    value={addBatchForm.quantity}
                    onChange={(e) =>
                      setAddBatchForm((p) => ({ ...p, quantity: e.target.value }))
                    }
                    placeholder="VD: 500"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  />
                </div>

                {/* Harvest & Expiry dates */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Ngày thu hoạch *</label>
                    <input
                      type="date"
                      value={addBatchForm.harvestDate}
                      onChange={(e) =>
                        setAddBatchForm((p) => ({ ...p, harvestDate: e.target.value }))
                      }
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Ngày hết hạn *</label>
                    <input
                      type="date"
                      value={addBatchForm.expiryDate}
                      onChange={(e) =>
                        setAddBatchForm((p) => ({ ...p, expiryDate: e.target.value }))
                      }
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-xs text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                </div>

                {/* Sweetness level */}
                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Độ ngọt (tùy chọn, thang 1-10)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    step="0.1"
                    value={addBatchForm.sweetnessLevel}
                    onChange={(e) =>
                      setAddBatchForm((p) => ({ ...p, sweetnessLevel: e.target.value }))
                    }
                    placeholder="VD: 8.5"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  />
                </div>

                <button
                  onClick={() => addBatch.mutate()}
                  disabled={
                    addBatch.isPending ||
                    !addBatchForm.productId ||
                    !addBatchForm.batchCode ||
                    !addBatchForm.quantity ||
                    !addBatchForm.harvestDate ||
                    !addBatchForm.expiryDate
                  }
                  className="mt-2 w-full rounded-xl bg-pine-500 py-2.5 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 disabled:opacity-50 transition-colors"
                >
                  {addBatch.isPending ? "Đang nhập..." : "Nhập lô hàng"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Reject modal */}
      <AnimatePresence>
        {!!rejectTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
            onClick={() => setRejectTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 12 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md rounded-2xl border border-slate-800 bg-slate-950 p-5 shadow-2xl"
            >
              <div className="flex items-center justify-between">
                <h3 className="font-display text-lg font-bold text-slate-100">Từ chối lô hàng</h3>
                <button
                  onClick={() => setRejectTarget(null)}
                  className="rounded-lg p-1 text-slate-400 hover:bg-slate-800"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="mt-3 space-y-2 text-sm text-slate-300">
                <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-3 text-xs">
                  <div>
                    Mã lô:{" "}
                    <span className="font-mono font-semibold text-cyan-300">
                      {rejectTarget.batchCode}
                    </span>
                  </div>
                  <div>
                    Sản phẩm:{" "}
                    <span className="font-semibold text-slate-100">
                      {rejectTarget.productName}
                    </span>
                  </div>
                  <div>
                    Nông trại:{" "}
                    <span className="text-slate-200">{rejectTarget.farmName ?? "—"}</span>
                  </div>
                </div>

                <label className="text-xs font-semibold uppercase text-slate-500">Lý do từ chối</label>
                <input
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Nhập lý do (tối đa 500 ký tự)"
                  className="h-10 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100 outline-none focus:border-red-500 focus:ring-2 focus:ring-red-500/30"
                />
              </div>

              <div className="mt-4 flex justify-end gap-2">
                <button
                  onClick={() => setRejectTarget(null)}
                  className="rounded-xl border border-slate-800 bg-slate-900 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  onClick={() => {
                    if (!rejectReason.trim()) {
                      toast.error("Vui lòng nhập lý do từ chối");
                      return;
                    }
                    rejectBatchMutation.mutate({
                      batchId: rejectTarget.id,
                      reason: rejectReason.trim(),
                    });
                  }}
                  disabled={rejectBatchMutation.isPending}
                  className="rounded-xl bg-red-600 px-3 py-2 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                >
                  Xác nhận từ chối
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}