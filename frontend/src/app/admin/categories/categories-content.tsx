"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import {
  Plus,
  X,
  Pencil,
  Trash2,
  FolderTree,
  ChevronRight,
  ChevronDown,
  Tag,
  Search,
  Upload,
  Loader2,
} from "lucide-react";
import { uploadApi } from "@/services/api";
import { adminCategoryService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import type { CategoryResponse } from "@/types";

const categorySchema = z.object({
  name: z.string().min(1, "Tên danh mục là bắt buộc").max(100),
  slug: z.string().optional(),
  image: z.string().url("URL không hợp lệ").optional().or(z.literal("")),
  parentId: z.coerce.number().optional(),
});
type CategoryForm = z.infer<typeof categorySchema>;

// ─── Recursive tree node ────────────────────────────────────
function CategoryNode({
  category,
  depth = 0,
  onEdit,
  onDelete,
}: {
  category: CategoryResponse;
  depth?: number;
  onEdit: (cat: CategoryResponse) => void;
  onDelete: (cat: CategoryResponse) => void;
}) {
  const [expanded, setExpanded] = useState(depth === 0);
  const hasChildren = (category.children?.length ?? 0) > 0;

  return (
    <div>
      <motion.div
        initial={{ opacity: 0, x: -8 }}
        animate={{ opacity: 1, x: 0 }}
        className={`group flex items-center gap-3 rounded-lg px-3 py-2.5 transition-colors hover:bg-slate-800/50 ${
          depth > 0 ? "ml-6 border-l border-slate-800 pl-4" : ""
        }`}
      >
        {/* Expand/collapse */}
        <button
          onClick={() => setExpanded((p) => !p)}
          className="flex h-5 w-5 shrink-0 items-center justify-center rounded text-slate-500 hover:text-slate-300"
        >
          {hasChildren ? (
            expanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )
          ) : (
            <Tag className="h-3.5 w-3.5" />
          )}
        </button>

        {/* Category info */}
        <div className="flex flex-1 items-center gap-3 min-w-0">
          {category.image && (
            <img
              src={category.image}
              alt={category.name}
              className="h-7 w-7 rounded-lg object-cover border border-slate-700 shrink-0"
            />
          )}
          <div className="min-w-0">
            <p className="truncate text-sm font-medium text-slate-200">
              {category.name}
            </p>
            <p className="truncate text-xs text-slate-500 font-mono">/{category.slug}</p>
          </div>
          {(category.children?.length ?? 0) > 0 && (
            <span className="shrink-0 rounded-full bg-slate-800 px-2 py-0.5 text-xs text-slate-400">
              {category.children!.length} con
            </span>
          )}
        </div>

        {/* Actions */}
        <div className="flex shrink-0 items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <button
            onClick={() => onEdit(category)}
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-500/10 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
            title="Chỉnh sửa"
          >
            <Pencil className="h-3.5 w-3.5" />
          </button>
          <button
            onClick={() => onDelete(category)}
            className="flex h-7 w-7 items-center justify-center rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
            title="Xóa"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </motion.div>

      {/* Children */}
      <AnimatePresence>
        {expanded && hasChildren && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
          >
            {category.children!.map((child) => (
              <CategoryNode
                key={child.id}
                category={child}
                depth={depth + 1}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Main component ─────────────────────────────────────────
export function CategoriesContent() {
  const qc = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState<CategoryResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CategoryResponse | null>(null);
  const [search, setSearch] = useState("");
  const [viewMode, setViewMode] = useState<"tree" | "flat">("tree");

  // Fetch tree
  const { data: tree, isLoading: treeLoading } = useQuery({
    queryKey: queryKeys.categoriesTree,
    queryFn: () => adminCategoryService.getTree(),
    enabled: viewMode === "tree",
  });

  // Fetch flat list
  const { data: flatList, isLoading: flatLoading } = useQuery({
    queryKey: queryKeys.categories,
    queryFn: () => adminCategoryService.getAll(),
  });

  const isLoading = viewMode === "tree" ? treeLoading : flatLoading;

  // Create mutation
  const createMutation = useMutation({
    mutationFn: (payload: CategoryForm) =>
      adminCategoryService.create({
        name: payload.name,
        slug: payload.slug || undefined,
        image: payload.image || undefined,
        parentId: payload.parentId || undefined,
      }),
    onSuccess: () => {
      toast.success("Tạo danh mục thành công");
      qc.invalidateQueries({ queryKey: ["categories"] });
      setShowForm(false);
      reset();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Tạo danh mục thất bại");
    },
  });

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: CategoryForm }) =>
      adminCategoryService.update(id, {
        name: payload.name,
        slug: payload.slug || undefined,
        image: payload.image || undefined,
        parentId: payload.parentId || undefined,
      }),
    onSuccess: () => {
      toast.success("Cập nhật danh mục thành công");
      qc.invalidateQueries({ queryKey: ["categories"] });
      setEditTarget(null);
      reset();
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Cập nhật thất bại");
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminCategoryService.delete(id),
    onSuccess: () => {
      toast.success("Đã xóa danh mục");
      qc.invalidateQueries({ queryKey: ["categories"] });
      setDeleteTarget(null);
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || "Xóa thất bại");
    },
  });

  const [uploadingImage, setUploadingImage] = useState(false);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingImage(true);
    try {
      const res = await uploadApi.uploadImage(file, "CATEGORY");
      setValue("image", res.data.url);
      toast.success("Tải ảnh lên thành công!");
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "Tải ảnh lên thất bại");
    } finally {
      setUploadingImage(false);
    }
  };

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CategoryForm>({ resolver: zodResolver(categorySchema) });

  const openCreate = () => {
    reset();
    setEditTarget(null);
    setShowForm(true);
  };

  const openEdit = (cat: CategoryResponse) => {
    setEditTarget(cat);
    setValue("name", cat.name);
    setValue("slug", cat.slug);
    setValue("image", cat.image ?? "");
    setValue("parentId", cat.parentId);
    setShowForm(true);
  };

  const onSubmit = (form: CategoryForm) => {
    if (editTarget) {
      updateMutation.mutate({ id: editTarget.id, payload: form });
    } else {
      createMutation.mutate(form);
    }
  };

  // Filter flat list
  const filteredFlat = flatList?.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    c.slug.toLowerCase().includes(search.toLowerCase())
  );

  const isPending = createMutation.isPending || updateMutation.isPending;

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
            <FolderTree className="h-6 w-6 text-pine-400" />
            Quản lý danh mục
          </h1>
          <p className="mt-1 text-sm text-slate-500">
            {flatList?.length ?? 0} danh mục trong hệ thống
          </p>
        </div>
        <button
          onClick={openCreate}
          className="flex items-center gap-2 rounded-xl bg-pine-500 px-4 py-2 text-sm font-medium text-white shadow-lg shadow-pine-500/20 hover:bg-pine-600 transition-colors"
        >
          <Plus className="h-4 w-4" /> Tạo danh mục
        </button>
      </motion.div>

      {/* Controls */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Search */}
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm danh mục..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>
        {/* View toggle */}
        <div className="flex items-center gap-1 rounded-xl border border-slate-700 bg-slate-800 p-1">
          <button
            onClick={() => setViewMode("tree")}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
              viewMode === "tree"
                ? "bg-pine-500 text-white"
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            Dạng cây
          </button>
          <button
            onClick={() => setViewMode("flat")}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${
              viewMode === "flat"
                ? "bg-pine-500 text-white"
                : "text-slate-400 hover:text-slate-200"
            }`}
          >
            Danh sách
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="rounded-2xl border border-slate-800 bg-slate-900">
        {isLoading ? (
          <div className="space-y-2 p-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-10 animate-pulse rounded-lg bg-slate-800" />
            ))}
          </div>
        ) : viewMode === "tree" ? (
          <div className="p-2">
            {(tree?.length ?? 0) === 0 ? (
              <p className="py-10 text-center text-sm text-slate-500">
                Chưa có danh mục nào
              </p>
            ) : (
              tree!.map((cat) => (
                <CategoryNode
                  key={cat.id}
                  category={cat}
                  onEdit={openEdit}
                  onDelete={setDeleteTarget}
                />
              ))
            )}
          </div>
        ) : (
          <div className="divide-y divide-slate-800">
            {(filteredFlat?.length ?? 0) === 0 ? (
              <p className="py-10 text-center text-sm text-slate-500">
                Không tìm thấy danh mục
              </p>
            ) : (
              filteredFlat!.map((cat) => (
                <motion.div
                  key={cat.id}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  className="group flex items-center justify-between px-4 py-3 hover:bg-slate-800/30 transition-colors"
                >
                  <div className="flex items-center gap-3">
                    {cat.image && (
                      <img
                        src={cat.image}
                        alt={cat.name}
                        className="h-8 w-8 rounded-lg object-cover border border-slate-700"
                      />
                    )}
                    <div>
                      <p className="text-sm font-medium text-slate-200">{cat.name}</p>
                      <p className="text-xs font-mono text-slate-500">/{cat.slug}</p>
                    </div>
                    {cat.parentName && (
                      <span className="rounded-full border border-slate-700 bg-slate-800 px-2 py-0.5 text-xs text-slate-400">
                        ← {cat.parentName}
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => openEdit(cat)}
                      className="flex h-7 w-7 items-center justify-center rounded-lg bg-cyan-500/10 text-cyan-400 hover:bg-cyan-500/20 transition-colors"
                    >
                      <Pencil className="h-3.5 w-3.5" />
                    </button>
                    <button
                      onClick={() => setDeleteTarget(cat)}
                      className="flex h-7 w-7 items-center justify-center rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 transition-colors"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </motion.div>
              ))
            )}
          </div>
        )}
      </div>

      {/* Create/Edit modal */}
      <AnimatePresence>
        {showForm && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => { setShowForm(false); setEditTarget(null); reset(); }}
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
                  {editTarget ? "Chỉnh sửa danh mục" : "Tạo danh mục mới"}
                </h2>
                <button
                  onClick={() => { setShowForm(false); setEditTarget(null); reset(); }}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <form onSubmit={handleSubmit(onSubmit)} className="mt-4 space-y-4">
                {/* Name */}
                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Tên danh mục *
                  </label>
                  <input
                    {...register("name")}
                    placeholder="VD: Rau củ quả"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                  {errors.name && (
                    <p className="mt-1 text-xs text-red-400">{errors.name.message}</p>
                  )}
                </div>

                {/* Slug */}
                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Slug (URL-friendly, tùy chọn)
                  </label>
                  <input
                    {...register("slug")}
                    placeholder="VD: rau-cu-qua (tự động nếu để trống)"
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                  />
                </div>

                 {/* Image URL / Upload */}
                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Hình ảnh danh mục (tùy chọn)
                  </label>
                  <div className="mt-1.5 flex items-center gap-4">
                    {watch("image") && (
                      <div className="relative h-16 w-16 overflow-hidden rounded-xl border border-slate-700 bg-slate-800">
                        <img
                          src={watch("image")}
                          alt="Category preview"
                          className="h-full w-full object-cover"
                        />
                        <button
                          type="button"
                          onClick={() => setValue("image", "")}
                          className="absolute -right-1 -top-1 rounded-full bg-red-500 p-0.5 text-white hover:bg-red-600"
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    )}
                    <label className="flex h-16 flex-1 cursor-pointer flex-col items-center justify-center rounded-xl border border-dashed border-slate-700 bg-slate-800 hover:bg-slate-700/50 hover:border-slate-600 transition">
                      {uploadingImage ? (
                        <div className="flex flex-col items-center gap-1">
                          <Loader2 className="h-4 w-4 animate-spin text-pine-400" />
                          <span className="text-[10px] text-slate-500">Đang tải lên...</span>
                        </div>
                      ) : (
                        <div className="flex flex-col items-center gap-1">
                          <Upload className="h-4 w-4 text-slate-400" />
                          <span className="text-[10px] text-slate-400 font-medium">Chọn file ảnh</span>
                          <span className="text-[8px] text-slate-500">PNG, JPG, WEBP lên đến 5MB</span>
                        </div>
                      )}
                      <input
                        type="file"
                        accept="image/*"
                        className="hidden"
                        onChange={handleImageUpload}
                        disabled={uploadingImage}
                      />
                    </label>
                  </div>
                  <input type="hidden" {...register("image")} />
                  {errors.image && (
                    <p className="mt-1 text-xs text-red-400">{errors.image.message}</p>
                  )}
                </div>

                {/* Parent category */}
                <div>
                  <label className="text-xs font-medium text-slate-400">
                    Danh mục cha (tùy chọn)
                  </label>
                  <select
                    {...register("parentId")}
                    className="mt-1 h-9 w-full rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
                  >
                    <option value="">— Không có cha (danh mục gốc) —</option>
                    {flatList
                      ?.filter((c) => !editTarget || c.id !== editTarget.id)
                      .map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name} ({c.slug})
                        </option>
                      ))}
                  </select>
                </div>

                <div className="flex justify-end gap-3 border-t border-slate-800 pt-4">
                  <button
                    type="button"
                    onClick={() => { setShowForm(false); setEditTarget(null); reset(); }}
                    className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                  >
                    Hủy
                  </button>
                  <button
                    type="submit"
                    disabled={isPending}
                    className="rounded-lg bg-pine-500 px-4 py-2 text-sm font-medium text-white hover:bg-pine-600 disabled:opacity-50"
                  >
                    {isPending
                      ? "Đang xử lý..."
                      : editTarget
                      ? "Lưu thay đổi"
                      : "Tạo danh mục"}
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
                    Xác nhận xóa
                  </h3>
                  <p className="mt-1 text-sm text-slate-400">
                    Bạn có chắc muốn xóa danh mục{" "}
                    <span className="font-semibold text-slate-200">
                      {deleteTarget.name}
                    </span>
                    ? Hành động này không thể hoàn tác.
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
                  {deleteMutation.isPending ? "Đang xóa..." : "Xóa danh mục"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}