"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { toast } from "sonner";
import {
  EyeOff,
  Eye,
  Star,
  Search,
  Filter,
  X,
  MessageSquare,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Trash2,
  Info,
  XCircle,
} from "lucide-react";
import { adminReviewService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { formatDateTime, getInitials, truncate } from "@/lib/utils";
import type { ReviewResponse } from "@/types";
import Image from "next/image";

function StarRating({ value }: { value: number }) {
  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          className={`h-3 w-3 ${i < value ? "fill-amber-400 text-amber-400" : "text-slate-600"}`}
        />
      ))}
      <span className="ml-1 text-xs text-slate-400">{value}</span>
    </div>
  );
}

const RATING_OPTIONS = [
  { label: "Tất cả sao", value: "" },
  { label: "5 ★", value: "5" },
  { label: "4 ★", value: "4" },
  { label: "3 ★", value: "3" },
  { label: "2 ★", value: "2" },
  { label: "1 ★", value: "1" },
];

export function ReviewsContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 20,
  });
  const [inspectingReview, setInspectingReview] = useState<ReviewResponse | null>(null);

  // Filters
  const [keyword, setKeyword] = useState("");
  const [ratingFilter, setRatingFilter] = useState("");
  const [hiddenFilter, setHiddenFilter] = useState<"all" | "visible" | "hidden">("all");
  const [productIdFilter, setProductIdFilter] = useState("");
  const [userIdFilter, setUserIdFilter] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Derived params for query
  const queryParams = {
    page: pagination.pageIndex,
    size: pagination.pageSize,
    keyword: keyword || undefined,
    rating: ratingFilter ? parseInt(ratingFilter) : undefined,
    productId: productIdFilter ? parseInt(productIdFilter) : undefined,
    userId: userIdFilter ? parseInt(userIdFilter) : undefined,
    sortBy,
    sortDirection,
  };

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.reviews(queryParams),
    queryFn: () => adminReviewService.getAll(queryParams),
  });

  // Client-side filter for isHidden since BE /admin/all doesn't support it as param
  const filteredContent = data?.content.filter((r) => {
    if (hiddenFilter === "visible") return !r.isHidden;
    if (hiddenFilter === "hidden") return r.isHidden;
    return true;
  }) ?? [];

  const hideReview = useMutation({
    mutationFn: (reviewId: number) => adminReviewService.hide(reviewId),
    onSuccess: () => {
      toast.success("Đã cập nhật trạng thái hiển thị");
      qc.invalidateQueries({ queryKey: ["reviews"] });
    },
    onError: () => toast.error("Thao tác thất bại"),
  });

  const deleteReview = useMutation({
    mutationFn: (reviewId: number) => adminReviewService.delete(reviewId),
    onSuccess: () => {
      toast.success("Đã xóa đánh giá thành công");
      qc.invalidateQueries({ queryKey: ["reviews"] });
    },
    onError: () => toast.error("Xóa đánh giá thất bại"),
  });

  const resetFilters = () => {
    setKeyword("");
    setRatingFilter("");
    setHiddenFilter("all");
    setProductIdFilter("");
    setUserIdFilter("");
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  const hasActiveFilters =
    keyword || ratingFilter || hiddenFilter !== "all" || productIdFilter || userIdFilter;

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("desc");
    }
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  const columns: ColumnDef<ReviewResponse, unknown>[] = [
    {
      id: "reviewer",
      header: "Người đánh giá",
      cell: ({ row }) => (
        <div className="flex items-center gap-2.5">
          {row.original.userAvatar ? (
            <Image
              src={row.original.userAvatar}
              alt={row.original.userFullName}
              width={28}
              height={28}
              className="h-7 w-7 shrink-0 rounded-full object-cover border border-slate-700"
            />
          ) : (
            <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-violet-600 to-violet-800 text-xs font-semibold text-white">
              {getInitials(row.original.userFullName)}
            </div>
          )}
          <div className="min-w-0">
            <p className="truncate text-xs font-medium text-slate-200">
              {row.original.userFullName}
            </p>
            <p className="text-[10px] text-slate-500 font-mono">
              UID #{row.original.userId}
            </p>
          </div>
        </div>
      ),
    },
    {
      id: "product",
      header: "Sản phẩm",
      cell: ({ row }) => (
        <div className="min-w-0">
          <p className="truncate text-xs font-medium text-slate-300">
            {row.original.productName ?? "—"}
          </p>
          <p className="text-[10px] text-slate-500 font-mono">
            PID #{row.original.productId}
          </p>
        </div>
      ),
    },
    {
      accessorKey: "rating",
      header: () => (
        <button
          onClick={() => handleSort("rating")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Sao
          {sortBy === "rating" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ getValue }) => <StarRating value={getValue<number>()} />,
    },
    {
      accessorKey: "comment",
      header: "Nội dung",
      cell: ({ row }) => (
        <div>
          <p className="text-xs text-slate-400 leading-relaxed">
            {truncate(row.original.comment, 100)}
          </p>
          {row.original.imageUrls.length > 0 && (
            <div className="mt-1 flex gap-1">
              {row.original.imageUrls.slice(0, 3).map((url, i) => (
                <Image
                  key={i}
                  src={url}
                  alt=""
                  width={32}
                  height={32}
                  className="h-8 w-8 rounded-lg object-cover border border-slate-700"
                />
              ))}
            </div>
          )}
        </div>
      ),
    },
    {
      id: "helpful",
      header: "Hữu ích",
      cell: ({ row }) => (
        <span className="text-xs text-slate-400">
          👍 {row.original.helpfulCount} · 👎 {row.original.unhelpfulCount}
        </span>
      ),
    },
    {
      accessorKey: "isHidden",
      header: "Trạng thái",
      cell: ({ getValue }) => (
        <span
          className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${
            getValue<boolean>()
              ? "border-red-500/30 bg-red-500/10 text-red-400"
              : "border-pine-500/30 bg-pine-500/10 text-pine-400"
          }`}
        >
          {getValue<boolean>() ? "Đã ẩn" : "Hiển thị"}
        </span>
      ),
    },
    {
      accessorKey: "createdAt",
      header: () => (
        <button
          onClick={() => handleSort("createdAt")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Ngày đăng
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
      cell: ({ row }) => (
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setInspectingReview(row.original)}
            className="flex items-center gap-1 rounded-lg bg-slate-700/50 text-slate-300 hover:bg-slate-700 hover:text-slate-100 px-2 py-1.5 text-xs font-medium transition-colors cursor-pointer"
            title="Xem chi tiết"
          >
            <Info className="h-3.5 w-3.5" />
          </button>
          
          <button
            onClick={() => hideReview.mutate(row.original.id)}
            disabled={hideReview.isPending}
            className={`flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 cursor-pointer ${
              row.original.isHidden
                ? "bg-pine-500/10 text-pine-400 hover:bg-pine-500/20"
                : "bg-amber-500/10 text-amber-400 hover:bg-amber-500/20"
            }`}
            title={row.original.isHidden ? "Hiện đánh giá" : "Ẩn đánh giá"}
          >
            {row.original.isHidden ? (
              <Eye className="h-3.5 w-3.5" />
            ) : (
              <EyeOff className="h-3.5 w-3.5" />
            )}
          </button>

          <button
            onClick={() => {
              if (
                confirm(
                  `Bạn có chắc chắn muốn xóa vĩnh viễn đánh giá của ${row.original.userFullName}?`
                )
              ) {
                deleteReview.mutate(row.original.id);
              }
            }}
            disabled={deleteReview.isPending}
            className="flex items-center gap-1 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 px-2 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 cursor-pointer"
            title="Xóa đánh giá"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-wrap items-center justify-between gap-4"
      >
        <div>
          <h1 className="font-display text-2xl font-bold text-slate-100 flex items-center gap-2">
            <MessageSquare className="h-6 w-6 text-pine-400" />
            Quản lý đánh giá
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {data?.totalElements ?? 0} đánh giá
            {filteredContent.length !== (data?.content.length ?? 0) && (
              <> · hiển thị {filteredContent.length}</>
            )}
          </p>
        </div>
      </motion.div>

      {/* ── Filter bar ── */}
      <div className="flex flex-wrap gap-3">
        {/* Keyword search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm tên người dùng, sản phẩm, nội dung..."
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPagination((p) => ({ ...p, pageIndex: 0 }));
            }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>

        {/* Product ID filter */}
        <div className="w-28 shrink-0">
          <input
            type="number"
            placeholder="Mã SP (PID)"
            value={productIdFilter}
            onChange={(e) => {
              setProductIdFilter(e.target.value);
              setPagination((p) => ({ ...p, pageIndex: 0 }));
            }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>

        {/* User ID filter */}
        <div className="w-28 shrink-0">
          <input
            type="number"
            placeholder="Mã ND (UID)"
            value={userIdFilter}
            onChange={(e) => {
              setUserIdFilter(e.target.value);
              setPagination((p) => ({ ...p, pageIndex: 0 }));
            }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>

        {/* Rating filter */}
        <div className="flex items-center gap-1.5">
          <Filter className="h-4 w-4 shrink-0 text-slate-500" />
          <div className="flex gap-1">
            {RATING_OPTIONS.map((r) => (
              <button
                key={r.value}
                onClick={() => {
                  setRatingFilter(r.value);
                  setPagination((p) => ({ ...p, pageIndex: 0 }));
                }}
                className={`rounded-full px-2.5 py-1 text-xs font-medium transition-colors ${
                  ratingFilter === r.value
                    ? "bg-amber-500 text-white"
                    : "bg-slate-800 text-slate-400 hover:bg-slate-700"
                }`}
              >
                {r.label}
              </button>
            ))}
          </div>
        </div>

        {/* Visible/Hidden filter */}
        <div className="flex items-center gap-1 rounded-xl border border-slate-700 bg-slate-800 p-1">
          {(["all", "visible", "hidden"] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setHiddenFilter(mode)}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
                hiddenFilter === mode
                  ? mode === "hidden"
                    ? "bg-red-500 text-white"
                    : "bg-pine-500 text-white"
                  : "text-slate-400 hover:text-slate-200"
              }`}
            >
              {mode === "all" ? "Tất cả" : mode === "visible" ? "Hiển thị" : "Đã ẩn"}
            </button>
          ))}
        </div>

        {/* Reset filters */}
        {hasActiveFilters && (
          <button
            onClick={resetFilters}
            className="flex items-center gap-1.5 rounded-lg border border-slate-700 px-3 py-1.5 text-xs font-medium text-slate-400 hover:bg-slate-800 hover:text-slate-200 transition-colors"
          >
            <X className="h-3.5 w-3.5" />
            Xoá bộ lọc
          </button>
        )}
      </div>

      {/* Table */}
      <DataTable
        data={filteredContent}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
      />

      <AnimatePresence>
        {inspectingReview && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setInspectingReview(null)}
              className="absolute inset-0 bg-black/75 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              className="relative w-full max-w-xl bg-slate-900 rounded-3xl overflow-hidden shadow-2xl border border-slate-800 p-6 z-10 text-slate-100"
            >
              <button
                onClick={() => setInspectingReview(null)}
                className="absolute top-4 right-4 text-slate-400 hover:text-slate-200 transition-colors cursor-pointer"
              >
                <XCircle className="w-6 h-6" />
              </button>

              <h3 className="text-lg font-bold font-display text-slate-100 mb-6 flex items-center gap-2">
                <MessageSquare className="h-5 w-5 text-pine-400" />
                Chi tiết đánh giá
              </h3>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                <div className="p-3 bg-slate-800/50 rounded-2xl border border-slate-800">
                  <p className="text-[10px] uppercase font-semibold text-slate-500 mb-1">Người đánh giá</p>
                  <p className="font-semibold text-sm text-slate-200">{inspectingReview.userFullName}</p>
                  <p className="text-xs text-slate-500 font-mono">UID: #{inspectingReview.userId}</p>
                </div>
                <div className="p-3 bg-slate-800/50 rounded-2xl border border-slate-800">
                  <p className="text-[10px] uppercase font-semibold text-slate-500 mb-1">Sản phẩm</p>
                  <p className="font-semibold text-sm text-slate-200">{inspectingReview.productName ?? "—"}</p>
                  <p className="text-xs text-slate-500 font-mono">PID: #{inspectingReview.productId}</p>
                </div>
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between p-3 bg-slate-800/30 rounded-xl border border-slate-800/50">
                  <span className="text-xs text-slate-400">Điểm đánh giá</span>
                  <StarRating value={inspectingReview.rating} />
                </div>

                <div className="flex items-center justify-between p-3 bg-slate-800/30 rounded-xl border border-slate-800/50">
                  <span className="text-xs text-slate-400">Trạng thái hiển thị</span>
                  <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                    inspectingReview.isHidden
                      ? "bg-red-500/10 text-red-400 border border-red-500/20"
                      : "bg-pine-500/10 text-pine-400 border border-pine-500/20"
                  }`}>
                    {inspectingReview.isHidden ? "Đã ẩn" : "Đang hiển thị"}
                  </span>
                </div>

                <div className="p-4 bg-slate-800/50 rounded-2xl border border-slate-800 space-y-2">
                  <p className="text-[10px] uppercase font-semibold text-slate-500">Nội dung nhận xét</p>
                  <p className="text-sm text-slate-300 leading-relaxed whitespace-pre-wrap">
                    {inspectingReview.comment || "(Không có nội dung bình luận)"}
                  </p>
                </div>

                {inspectingReview.imageUrls && inspectingReview.imageUrls.length > 0 && (
                  <div className="space-y-2">
                    <p className="text-[10px] uppercase font-semibold text-slate-500">Hình ảnh kèm theo ({inspectingReview.imageUrls.length})</p>
                    <div className="flex flex-wrap gap-2">
                      {inspectingReview.imageUrls.map((url, i) => (
                        <a key={i} href={url} target="_blank" rel="noopener noreferrer" className="relative w-20 h-20 rounded-xl overflow-hidden border border-slate-800 bg-slate-950 hover:scale-105 transition-transform">
                          <Image src={url} alt="" fill className="object-cover" />
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-slate-800">
                <button
                  onClick={() => {
                    hideReview.mutate(inspectingReview.id, {
                      onSuccess: () => {
                        setInspectingReview((prev) => prev ? { ...prev, isHidden: !prev.isHidden } : null);
                      }
                    });
                  }}
                  className={`rounded-xl px-4 py-2 text-xs font-semibold transition-colors cursor-pointer ${
                    inspectingReview.isHidden
                      ? "bg-pine-600 text-white hover:bg-pine-500"
                      : "bg-red-600/20 text-red-400 hover:bg-red-600/35 border border-red-500/25"
                  }`}
                >
                  {inspectingReview.isHidden ? "Hiện nhận xét" : "Ẩn nhận xét"}
                </button>
                <button
                  onClick={() => setInspectingReview(null)}
                  className="bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl px-4 py-2 text-xs font-semibold transition-colors cursor-pointer"
                >
                  Đóng
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}