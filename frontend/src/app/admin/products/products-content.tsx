"use client";

import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import {
  Search,
  Filter,
  Plus,
  X,
  Pencil,
  Trash2,
  PackageSearch,
  Eye,
  Star,
  Leaf,
  ArrowUpDown,
  ArrowUp,
  ArrowDown,
  Loader2,
} from "lucide-react";
import { uploadApi } from "@/services/api";
import {
  adminProductService,
  adminCategoryService,
} from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { formatCurrency } from "@/lib/utils";
import type { ProductSummaryResponse, ProductStatus, CategoryResponse } from "@/types";
import Image from "next/image";

// ─── Product form schema ─────────────────────────────────────
const productSchema = z.object({
  name: z.string().min(1, "Tên sản phẩm là bắt buộc").max(200),
  description: z.string().optional(),
  price: z.coerce.number().positive("Giá phải > 0"),
  discountPrice: z.coerce.number().min(0).optional(),
  weight: z.coerce.number().min(0).max(999999.99, "Khối lượng tối đa là 999,999.99g").optional(),
  calories: z.coerce.number().min(0).max(9999.99, "Calories tối đa là 9,999.99 kcal").optional(),
  brand: z.string().optional(),
  origin: z.string().optional(),
  isOrganic: z.boolean().default(false),
  thumbnail: z.string().min(1, "Vui lòng chọn một ảnh làm thumbnail"),
  categoryId: z.coerce.number().positive("Chọn danh mục"),
  status: z.enum(["ACTIVE", "PENDING_DEACTIVATION", "INACTIVE", "OUT_OF_STOCK"]).optional(),
  imageUrls: z.array(z.string()).default([]),
});
type ProductForm = z.infer<typeof productSchema>;

const STATUS_FILTERS = [
  { label: "Tất cả", value: "" },
  { label: "Đang bán", value: "ACTIVE" },
  { label: "Tạm ẩn", value: "INACTIVE" },
  { label: "Hết hàng", value: "OUT_OF_STOCK" },
];

// ─── Build flat category options from tree ───────────────────
function flattenCategories(
  cats: CategoryResponse[],
  depth = 0
): { id: number; name: string; prefix: string }[] {
  const result: { id: number; name: string; prefix: string }[] = [];
  for (const cat of cats) {
    result.push({ id: cat.id, name: cat.name, prefix: "  ".repeat(depth) });
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children, depth + 1));
    }
  }
  return result;
}

export function ProductsContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 20,
  });
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");
  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState<ProductSummaryResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ProductSummaryResponse | null>(null);
  const [detailTarget, setDetailTarget] = useState<ProductSummaryResponse | null>(null);

  // Fetch products
  const { data, isLoading } = useQuery({
    queryKey: queryKeys.products({
      keyword,
      status: statusFilter || undefined,
      page: pagination.pageIndex,
      size: pagination.pageSize,
      sortBy,
      sortDirection,
    }),
    queryFn: () =>
      adminProductService.getAll({
        keyword: keyword || undefined,
        status: (statusFilter || undefined) as ProductStatus | undefined,
        page: pagination.pageIndex,
        size: pagination.pageSize,
        sortBy,
        sortDirection,
      }),
  });

  // Fetch categories for form
  const { data: categoryTree } = useQuery({
    queryKey: queryKeys.categoriesTree,
    queryFn: () => adminCategoryService.getTree(),
  });
  const categoryOptions = categoryTree ? flattenCategories(categoryTree) : [];

  // Fetch product detail when viewing
  const { data: productDetail, isLoading: detailLoading } = useQuery({
    queryKey: queryKeys.product(detailTarget?.id ?? 0),
    queryFn: () => adminProductService.getById(detailTarget!.id),
    enabled: !!detailTarget,
  });

  // Fetch product detail when editing (to get imageUrls)
  const { data: editDetail } = useQuery({
    queryKey: ["admin-product-detail", editTarget?.id ?? 0],
    queryFn: () => adminProductService.getById(editTarget!.id),
    enabled: !!editTarget,
  });

  const createMutation = useMutation({
    mutationFn: (payload: ProductForm) =>
      adminProductService.create({
        name: payload.name,
        description: payload.description,
        price: payload.price,
        discountPrice: payload.discountPrice || undefined,
        weight: payload.weight || undefined,
        calories: payload.calories || undefined,
        brand: payload.brand || undefined,
        origin: payload.origin || undefined,
        isOrganic: payload.isOrganic,
        thumbnail: payload.thumbnail,
        categoryId: payload.categoryId,
        imageUrls: payload.imageUrls,
      }),
    onSuccess: () => {
      toast.success("Tạo sản phẩm thành công");
      qc.invalidateQueries({ queryKey: ["products"] });
      setShowForm(false);
      reset();
    },
    onError: (err: unknown) => {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message || "Tạo sản phẩm thất bại");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ProductForm }) =>
      adminProductService.update(id, {
        name: payload.name,
        description: payload.description,
        price: payload.price,
        discountPrice: payload.discountPrice || undefined,
        weight: payload.weight || undefined,
        calories: payload.calories || undefined,
        brand: payload.brand || undefined,
        origin: payload.origin || undefined,
        isOrganic: payload.isOrganic,
        thumbnail: payload.thumbnail,
        categoryId: payload.categoryId,
        status: payload.status,
        imageUrls: payload.imageUrls,
      }),
    onSuccess: () => {
      toast.success("Cập nhật sản phẩm thành công");
      qc.invalidateQueries({ queryKey: ["products"] });
      setShowForm(false);
      setEditTarget(null);
      reset();
    },
    onError: (err: unknown) => {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message || "Cập nhật thất bại");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminProductService.delete(id),
    onSuccess: () => {
      toast.success("Đã xóa sản phẩm");
      qc.invalidateQueries({ queryKey: ["products"] });
      setDeleteTarget(null);
    },
    onError: (err: unknown) => {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message || "Xóa sản phẩm thất bại");
    },
  });

  const [uploadingImages, setUploadingImages] = useState(false);

  const handleMultipleImagesUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setUploadingImages(true);

    const currentUrls = watch("imageUrls") || [];
    const newUrls: string[] = [];

    try {
      const promises = Array.from(files).map(async (file) => {
        const res = await uploadApi.uploadImage(file, "PRODUCT");
        return res.data.url;
      });

      const urls = await Promise.all(promises);
      newUrls.push(...urls);

      const updatedUrls = [...currentUrls, ...newUrls];
      setValue("imageUrls", updatedUrls);

      // Auto select the first uploaded image as thumbnail if none is currently selected
      if (!watch("thumbnail")) {
        setValue("thumbnail", updatedUrls[0]);
      }

      toast.success(`Đã tải lên thành công ${urls.length} hình ảnh!`);
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message || "Tải ảnh thất bại");
    } finally {
      setUploadingImages(false);
      e.target.value = "";
    }
  };

  const handleRemoveImage = (urlToRemove: string) => {
    const currentUrls = watch("imageUrls") || [];
    const updatedUrls = currentUrls.filter((url) => url !== urlToRemove);
    setValue("imageUrls", updatedUrls);

    // If removed image was the thumbnail, pick a new one from the remaining list
    if (watch("thumbnail") === urlToRemove) {
      setValue("thumbnail", updatedUrls.length > 0 ? updatedUrls[0] : "");
    }
  };

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ProductForm>({
    resolver: zodResolver(productSchema),
    defaultValues: { isOrganic: false },
  });

  const isOrganic = watch("isOrganic");

  const openCreate = () => {
    reset({ isOrganic: false, imageUrls: [], thumbnail: "" });
    setEditTarget(null);
    setShowForm(true);
  };

  const openEdit = (prod: ProductSummaryResponse) => {
    setEditTarget(prod);
    setShowForm(true);
  };

  useEffect(() => {
    if (editDetail && editTarget) {
      setValue("name", editDetail.name);
      setValue("description", editDetail.description ?? "");
      setValue("price", editDetail.price);
      setValue("discountPrice", editDetail.discountPrice ?? 0);
      setValue("weight", editDetail.weight ?? 0);
      setValue("calories", editDetail.calories ?? 0);
      setValue("brand", editDetail.brand ?? "");
      setValue("origin", editDetail.origin ?? "");
      setValue("isOrganic", editDetail.isOrganic);
      setValue("thumbnail", editDetail.thumbnail);
      setValue("categoryId", editDetail.categoryId);
      setValue("status", editDetail.status);
      setValue("imageUrls", editDetail.imageUrls ?? []);
    }
  }, [editDetail, editTarget, setValue]);

  const onSubmit = (form: ProductForm) => {
    if (editTarget) {
      updateMutation.mutate({ id: editTarget.id, payload: form });
    } else {
      createMutation.mutate(form);
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  const handleSort = (field: string) => {
    if (sortBy === field) {
      setSortDirection((prev) => (prev === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("desc");
    }
    setPagination((p) => ({ ...p, pageIndex: 0 }));
  };

  // Columns
  const columns: ColumnDef<ProductSummaryResponse, unknown>[] = [
    {
      id: "product",
      header: () => (
        <button
          onClick={() => handleSort("name")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Sản phẩm
          {sortBy === "name" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <Image
            src={row.original.thumbnail}
            alt={row.original.name}
            width={40}
            height={40}
            className="h-10 w-10 rounded-lg object-cover border border-slate-700 bg-slate-800 shrink-0"
          />
          <div className="min-w-0">
            <p className="truncate text-xs font-semibold text-slate-200">
              {row.original.name}
            </p>
            <div className="flex items-center gap-1 mt-0.5">
              {row.original.isOrganic && (
                <span className="inline-flex items-center gap-0.5 rounded-full bg-pine-500/10 px-1.5 py-0.5 text-[10px] font-medium text-pine-400">
                  <Leaf className="h-2.5 w-2.5" /> Organic
                </span>
              )}
              <span className="text-[10px] text-slate-500">{row.original.categoryName}</span>
            </div>
          </div>
        </div>
      ),
    },
    {
      id: "price",
      header: () => (
        <button
          onClick={() => handleSort("price")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Giá
          {sortBy === "price" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div>
          {row.original.discountPrice ? (
            <>
              <p className="text-xs font-bold text-pine-400">
                {formatCurrency(row.original.discountPrice)}
              </p>
              <p className="text-xs text-slate-500 line-through">
                {formatCurrency(row.original.price)}
              </p>
            </>
          ) : (
            <p className="text-xs font-semibold text-slate-200">
              {formatCurrency(row.original.price)}
            </p>
          )}
        </div>
      ),
    },
    {
      accessorKey: "totalStock",
      header: "Tồn kho",
      cell: ({ getValue }) => {
        const stock = getValue<number>();
        return (
          <span
            className={`text-xs font-semibold ${
              stock === 0
                ? "text-red-400"
                : stock < 10
                ? "text-amber-400"
                : "text-slate-300"
            }`}
          >
            {stock.toLocaleString("vi-VN")}
          </span>
        );
      },
    },
    {
      id: "rating",
      header: "Đánh giá",
      cell: ({ row }) => (
        <div className="flex items-center gap-1">
          <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
          <span className="text-xs text-slate-300">
            {row.original.averageRating?.toFixed(1) ?? "—"}
          </span>
          {row.original.reviewCount != null && (
            <span className="text-xs text-slate-500">
              ({row.original.reviewCount})
            </span>
          )}
        </div>
      ),
    },
    {
      accessorKey: "status",
      header: "Trạng thái",
      cell: ({ getValue }) => <StatusBadge status={getValue<ProductStatus>()} />,
    },
    {
      id: "actions",
      header: "Hành động",
      cell: ({ row }) => (
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setDetailTarget(row.original)}
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200 transition-colors"
            title="Xem chi tiết"
          >
            <Eye className="h-3.5 w-3.5" />
          </button>
          <button
            onClick={() => openEdit(row.original)}
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-500/10 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
            title="Chỉnh sửa"
          >
            <Pencil className="h-3.5 w-3.5" />
          </button>
          <button
            onClick={() => setDeleteTarget(row.original)}
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
            title="Xóa"
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
            <PackageSearch className="h-6 w-6 text-pine-400" />
            Quản lý sản phẩm
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {data?.totalElements ?? 0} sản phẩm trong hệ thống
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-xl bg-pine-500 px-4 py-2 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 transition-colors"
        >
          <Plus className="h-4 w-4" /> Thêm sản phẩm
        </button>
      </motion.div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm tên sản phẩm..."
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPagination((p) => ({ ...p, pageIndex: 0 }));
            }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>
        <div className="flex items-center gap-2">
          <Filter className="h-4 w-4 text-slate-500" />
          <div className="flex gap-1.5">
            {STATUS_FILTERS.map((s) => (
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

      {/* Table */}
      <DataTable
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
      />

      {/* Product Detail Modal */}
      <AnimatePresence>
        {detailTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setDetailTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl overflow-y-auto max-h-[90vh]"
            >
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <h2 className="font-display text-lg font-semibold text-slate-100">
                  Chi tiết sản phẩm
                </h2>
                <button
                  onClick={() => setDetailTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              {detailLoading ? (
                <div className="space-y-4 py-8">
                  <div className="h-6 animate-pulse rounded bg-slate-800 w-1/3" />
                  <div className="h-32 animate-pulse rounded bg-slate-800 w-full" />
                </div>
              ) : !productDetail ? (
                <p className="py-8 text-center text-sm text-slate-500">Không tải được chi tiết</p>
              ) : (
                <div className="mt-4 space-y-5">
                  {/* Images */}
                  <div className="flex gap-3 overflow-x-auto pb-2">
                    <Image
                      src={productDetail.thumbnail}
                      alt={productDetail.name}
                      width={96}
                      height={96}
                      className="h-24 w-24 shrink-0 rounded-xl object-cover border border-slate-700"
                    />
                    {productDetail.imageUrls.map((url, i) => (
                      <Image
                        key={i}
                        src={url}
                        alt={`img-${i}`}
                        width={96}
                        height={96}
                        className="h-24 w-24 shrink-0 rounded-xl object-cover border border-slate-700"
                      />
                    ))}
                  </div>

                  {/* Info grid */}
                  <div className="grid grid-cols-2 gap-4 rounded-xl bg-slate-800/40 p-4 text-xs">
                    <div>
                      <p className="text-slate-500">Tên sản phẩm</p>
                      <p className="mt-1 font-semibold text-slate-100">{productDetail.name}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Danh mục</p>
                      <p className="mt-1 font-medium text-slate-300">{productDetail.categoryName}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Giá gốc</p>
                      <p className="mt-1 font-semibold text-slate-200">{formatCurrency(productDetail.price)}</p>
                    </div>
                    <div>
                      <p className="text-slate-500">Giá khuyến mãi</p>
                      <p className="mt-1 font-semibold text-pine-400">
                        {productDetail.discountPrice ? formatCurrency(productDetail.discountPrice) : "—"}
                      </p>
                    </div>
                    <div>
                      <p className="text-slate-500">Tồn kho</p>
                      <p className="mt-1 font-bold text-cyan-400">
                        {productDetail.totalStock.toLocaleString("vi-VN")}
                      </p>
                    </div>
                    <div>
                      <p className="text-slate-500">Trạng thái</p>
                      <div className="mt-1">
                        <StatusBadge status={productDetail.status} />
                      </div>
                    </div>
                    {productDetail.origin && (
                      <div>
                        <p className="text-slate-500">Xuất xứ</p>
                        <p className="mt-1 text-slate-300">{productDetail.origin}</p>
                      </div>
                    )}
                    {productDetail.weight && (
                      <div>
                        <p className="text-slate-500">Khối lượng</p>
                        <p className="mt-1 text-slate-300">{productDetail.weight}g</p>
                      </div>
                    )}
                    {productDetail.calories && (
                      <div>
                        <p className="text-slate-500">Calories</p>
                        <p className="mt-1 text-slate-300">{productDetail.calories} kcal</p>
                      </div>
                    )}
                    <div className="col-span-2">
                      <p className="text-slate-500">Organic</p>
                      <p className="mt-1">
                        {productDetail.isOrganic ? (
                          <span className="inline-flex items-center gap-1 text-pine-400 font-medium">
                            <Leaf className="h-3.5 w-3.5" /> Có (Organic)
                          </span>
                        ) : (
                          <span className="text-slate-400">Không</span>
                        )}
                      </p>
                    </div>
                  </div>

                  {/* Description */}
                  {productDetail.description && (
                    <div>
                      <p className="text-xs font-semibold text-slate-400 mb-1">Mô tả</p>
                      <p className="text-xs text-slate-300 leading-relaxed">
                        {productDetail.description}
                      </p>
                    </div>
                  )}

                  {/* Actions */}
                  <div className="flex justify-end gap-3 border-t border-slate-800 pt-4">
                    <button
                      onClick={() => {
                        setDetailTarget(null);
                        openEdit(detailTarget!);
                      }}
                      className="flex items-center gap-2 rounded-lg bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                    >
                      <Pencil className="h-3.5 w-3.5" /> Chỉnh sửa
                    </button>
                    <button
                      onClick={() => setDetailTarget(null)}
                      className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                    >
                      Đóng
                    </button>
                  </div>
                </div>
              )}
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Create/Edit form drawer */}
      <AnimatePresence>
        {showForm && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-end bg-black/60 backdrop-blur-sm"
            onClick={() => { setShowForm(false); setEditTarget(null); reset(); }}
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
                  {editTarget ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm mới"}
                </h2>
                <button
                  onClick={() => { setShowForm(false); setEditTarget(null); reset(); }}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-4">
                {/* Name */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Tên sản phẩm *</label>
                  <input
                    {...register("name")}
                    placeholder="VD: Táo Envy Mỹ"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                  {errors.name && (
                    <p className="mt-1 text-xs text-red-400">{errors.name.message}</p>
                  )}
                </div>

                {/* Category */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Danh mục *</label>
                  <select
                    {...register("categoryId")}
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  >
                    <option value="">— Chọn danh mục —</option>
                    {categoryOptions.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.prefix}{c.name}
                      </option>
                    ))}
                  </select>
                  {errors.categoryId && (
                    <p className="mt-1 text-xs text-red-400">{errors.categoryId.message}</p>
                  )}
                </div>

                {/* Prices */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Giá bán (VND) *</label>
                    <input
                      type="number"
                      {...register("price")}
                      placeholder="50000"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.price && (
                      <p className="mt-1 text-xs text-red-400">{errors.price.message}</p>
                    )}
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Giá khuyến mãi</label>
                    <input
                      type="number"
                      {...register("discountPrice")}
                      placeholder="40000"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                </div>

                {/* Product Images & Thumbnail Selection */}
                <div className="space-y-2">
                  <label className="text-xs font-medium text-slate-400">
                    Hình ảnh sản phẩm * (Tải lên hàng loạt & chọn 1 ảnh làm ảnh đại diện)
                  </label>
                  
                  {/* Image Grid */}
                  <div className="grid grid-cols-4 gap-2">
                    {(watch("imageUrls") || []).map((url, index) => {
                      const isThumbnail = watch("thumbnail") === url;
                      return (
                        <div
                          key={url + index}
                          className={`relative aspect-square rounded-lg overflow-hidden border bg-slate-800 group transition-all ${
                            isThumbnail ? "border-pine-500 ring-2 ring-pine-500/20" : "border-slate-700 hover:border-slate-500"
                          }`}
                        >
                          <Image
                            src={url}
                            alt={`Preview ${index}`}
                            fill
                            className="object-cover"
                          />
                          
                          {/* Thumbnail Indicator/Button */}
                          <button
                            type="button"
                            onClick={() => setValue("thumbnail", url)}
                            className={`absolute inset-x-0 bottom-0 text-[9px] py-1 text-center font-medium transition-all ${
                              isThumbnail
                                ? "bg-pine-500 text-white opacity-100"
                                : "bg-black/60 text-slate-300 opacity-0 group-hover:opacity-100 hover:bg-pine-600/80 hover:text-white"
                            }`}
                          >
                            {isThumbnail ? "Ảnh bìa" : "Chọn làm bìa"}
                          </button>

                          {/* Delete Button */}
                          <button
                            type="button"
                            onClick={() => handleRemoveImage(url)}
                            className="absolute right-1 top-1 rounded-full bg-red-500/80 p-0.5 text-white hover:bg-red-600 opacity-0 group-hover:opacity-100 transition-opacity"
                            title="Xóa ảnh này"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </div>
                      );
                    })}

                    {/* Upload button inside grid */}
                    <label className="flex aspect-square cursor-pointer flex-col items-center justify-center rounded-lg border border-dashed border-slate-700 bg-slate-800 hover:bg-slate-700/50 hover:border-slate-600 transition">
                      {uploadingImages ? (
                        <div className="flex flex-col items-center gap-1 p-1 text-center">
                          <Loader2 className="h-4 w-4 animate-spin text-pine-400" />
                          <span className="text-[9px] text-slate-500">Đang tải...</span>
                        </div>
                      ) : (
                        <div className="flex flex-col items-center gap-1 p-1 text-center">
                          <Plus className="h-4 w-4 text-slate-400" />
                          <span className="text-[9px] text-slate-400 font-medium">Thêm ảnh</span>
                        </div>
                      )}
                      <input
                        type="file"
                        multiple
                        accept="image/*"
                        className="hidden"
                        onChange={handleMultipleImagesUpload}
                        disabled={uploadingImages}
                      />
                    </label>
                  </div>

                  {errors.thumbnail && (
                    <p className="mt-1 text-xs text-red-400">{errors.thumbnail.message}</p>
                  )}
                </div>

                {/* Origin & Weight */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Xuất xứ</label>
                    <input
                      {...register("origin")}
                      placeholder="Việt Nam"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Khối lượng (g)</label>
                    <input
                      type="number"
                      {...register("weight")}
                      placeholder="500"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.weight && (
                      <p className="mt-1 text-xs text-red-400">{errors.weight.message}</p>
                    )}
                  </div>
                </div>

                {/* Brand & Calories */}
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-slate-400">Thương hiệu</label>
                    <input
                      {...register("brand")}
                      placeholder="Tên thương hiệu"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-slate-400">Calories (kcal)</label>
                    <input
                      type="number"
                      {...register("calories")}
                      placeholder="100"
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    />
                    {errors.calories && (
                      <p className="mt-1 text-xs text-red-400">{errors.calories.message}</p>
                    )}
                  </div>
                </div>

                {/* Status (only when editing) */}
                {editTarget && (
                  <div>
                    <label className="text-xs font-medium text-slate-400">Trạng thái</label>
                    <select
                      {...register("status")}
                      className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                    >
                      <option value="ACTIVE">Đang bán (ACTIVE)</option>
                      <option value="INACTIVE">Tạm ẩn (INACTIVE)</option>
                      <option value="OUT_OF_STOCK">Hết hàng (OUT_OF_STOCK)</option>
                    </select>
                  </div>
                )}

                {/* Description */}
                <div>
                  <label className="text-xs font-medium text-slate-400">Mô tả</label>
                  <textarea
                    {...register("description")}
                    rows={3}
                    placeholder="Mô tả chi tiết sản phẩm..."
                    className="mt-1 w-full rounded-lg border border-slate-700 bg-slate-800 p-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                </div>

                {/* Organic toggle */}
                <div>
                  <label className="flex items-center gap-3 cursor-pointer">
                    <div
                      onClick={() => setValue("isOrganic", !isOrganic)}
                      className={`relative h-6 w-11 rounded-full transition-colors ${
                        isOrganic ? "bg-pine-500" : "bg-slate-700"
                      }`}
                    >
                      <div
                        className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform ${
                          isOrganic ? "translate-x-5" : "translate-x-0.5"
                        }`}
                      />
                    </div>
                    <span className="text-sm text-slate-300">
                      Sản phẩm hữu cơ (Organic)
                    </span>
                    {isOrganic && <Leaf className="h-4 w-4 text-pine-400" />}
                  </label>
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={isPending}
                    className="w-full rounded-xl bg-pine-500 py-2.5 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 disabled:opacity-50 transition-colors"
                  >
                    {isPending
                      ? "Đang xử lý..."
                      : editTarget
                      ? "Lưu thay đổi"
                      : "Tạo sản phẩm"}
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Delete confirm modal */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setDeleteTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 10 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm rounded-2xl border border-slate-700 bg-slate-900 p-6 shadow-2xl"
            >
              <div className="flex items-start gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-red-500/10">
                  <Trash2 className="h-5 w-5 text-red-400" />
                </div>
                <div>
                  <h3 className="font-display text-base font-semibold text-slate-100">
                    Xóa sản phẩm
                  </h3>
                  <p className="mt-1 text-sm text-slate-400">
                    Sản phẩm{" "}
                    <span className="font-semibold text-slate-200">
                      {deleteTarget.name}
                    </span>{" "}
                    sẽ bị ẩn khỏi hệ thống (soft-delete). Bạn có muốn tiếp tục?
                  </p>
                </div>
              </div>
              <div className="mt-6 flex justify-end gap-3">
                <button
                  onClick={() => setDeleteTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  onClick={() => deleteMutation.mutate(deleteTarget.id)}
                  disabled={deleteMutation.isPending}
                  className="rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-600 disabled:opacity-50"
                >
                  {deleteMutation.isPending ? "Đang xóa..." : "Xóa sản phẩm"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}