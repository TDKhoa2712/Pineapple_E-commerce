"use client";

import { useState, useEffect, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { type ColumnDef, type PaginationState } from "@tanstack/react-table";
import { toast } from "sonner";
import { Search, Lock, Unlock, Shield, Key, X, ArrowUpDown, ArrowUp, ArrowDown, Eye, Globe, Calendar, Mail, Phone, Trash2, Plus, Star, Edit2 } from "lucide-react";
import { adminUserService } from "@/services/admin.service";
import { queryKeys } from "@/lib/query-keys";
import { DataTable } from "@/components/shared/data-table";
import { StatusBadge } from "@/components/shared/status-badge";
import { formatDate, getInitials, formatCurrency } from "@/lib/utils";
import type { UserResponse, UserStatus, RoleName, AddressRequest, LocationItem } from "@/types";
import { useProvinces, useDistricts, useWards } from "@/hooks";
import { addressSchema } from "@/lib/validations";
import Image from "next/image";

export function UsersContent() {
  const qc = useQueryClient();
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 20,
  });
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Modals state
  const [viewingUser, setViewingUser] = useState<UserResponse | null>(null);
  const [activeTab, setActiveTab] = useState<"info" | "addresses" | "wishlist">("info");
  const [roleTarget, setRoleTarget] = useState<UserResponse | null>(null);
  const [selectedRole, setSelectedRole] = useState<RoleName>("ROLE_USER");
  const [resetPasswordTarget, setResetPasswordTarget] = useState<UserResponse | null>(null);
  const [newPassword, setNewPassword] = useState("");

  // User edit form state
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [editFormData, setEditFormData] = useState({
    fullName: "",
    phone: "",
  });
  const [selectedAvatarFile, setSelectedAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);

  // Address form state
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [editingAddressId, setEditingAddressId] = useState<number | null>(null);
  const [addressData, setAddressData] = useState({
    receiverName: "",
    phone: "",
    province: "",
    district: "",
    ward: "",
    detail: "",
    isDefault: false,
  });

  const [selectedProvinceId, setSelectedProvinceId] = useState<string | null>(null);
  const [selectedDistrictId, setSelectedDistrictId] = useState<string | null>(null);
  const [selectedWardCode, setSelectedWardCode] = useState<string | null>(null);

  const { data: provincesRes } = useProvinces();
  const provinces = useMemo(() => provincesRes?.data ?? [], [provincesRes]);

  const { data: districtsRes } = useDistricts(selectedProvinceId);
  const districts = useMemo(() => districtsRes?.data ?? [], [districtsRes]);

  const { data: wardsRes } = useWards(selectedDistrictId);
  const wards = useMemo(() => wardsRes?.data ?? [], [wardsRes]);

  // Auto-resolve GHN ID from names when editing
  useEffect(() => {
    if (!editingAddressId || !viewingUser || !showAddressForm) return;

    if (provinces.length > 0 && !selectedProvinceId) {
      const match = provinces.find((p: LocationItem) => p.name === addressData.province);
      if (match) {
        setSelectedProvinceId(match.id);
      }
    }
  }, [provinces, editingAddressId, showAddressForm, addressData.province, selectedProvinceId, viewingUser]);

  useEffect(() => {
    if (!editingAddressId || !viewingUser || !showAddressForm || !selectedProvinceId) return;

    if (districts.length > 0 && !selectedDistrictId) {
      const match = districts.find((d: LocationItem) => d.name === addressData.district);
      if (match) {
        setSelectedDistrictId(match.id);
      }
    }
  }, [districts, editingAddressId, showAddressForm, addressData.district, selectedProvinceId, selectedDistrictId, viewingUser]);

  useEffect(() => {
    if (!editingAddressId || !viewingUser || !showAddressForm || !selectedDistrictId) return;

    if (wards.length > 0 && !selectedWardCode) {
      const match = wards.find((w: LocationItem) => w.name === addressData.ward);
      if (match) {
        setSelectedWardCode(match.id);
      }
    }
  }, [wards, editingAddressId, showAddressForm, addressData.ward, selectedDistrictId, selectedWardCode, viewingUser]);

  const resetAddressForm = () => {
    setAddressData({
      receiverName: "",
      phone: "",
      province: "",
      district: "",
      ward: "",
      detail: "",
      isDefault: false,
    });
    setEditingAddressId(null);
    setSelectedProvinceId(null);
    setSelectedDistrictId(null);
    setSelectedWardCode(null);
  };

  const resetEditForm = () => {
    setEditingUserId(null);
    setEditFormData({ fullName: "", phone: "" });
    setSelectedAvatarFile(null);
    setAvatarPreview(null);
  };

  const handleViewUser = (user: UserResponse) => {
    setViewingUser(user);
    setActiveTab("info");
  };

  // Fetch viewed user's addresses
  const { data: userAddresses, isLoading: addressesLoading } = useQuery({
    queryKey: ["user-addresses", viewingUser?.id],
    queryFn: () => adminUserService.getAddresses(viewingUser!.id),
    enabled: !!viewingUser && activeTab === "addresses",
  });

  // Fetch viewed user's wishlist
  const { data: userWishlist, isLoading: wishlistLoading } = useQuery({
    queryKey: ["user-wishlist", viewingUser?.id],
    queryFn: () => adminUserService.getWishlist(viewingUser!.id, { page: 0, size: 50 }),
    enabled: !!viewingUser && activeTab === "wishlist",
  });

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.users({
      page: pagination.pageIndex,
      size: pagination.pageSize,
      keyword,
      status: statusFilter || undefined,
      role: roleFilter || undefined,
      sortBy,
      sortDirection,
    }),
    queryFn: () =>
      adminUserService.getAll({
        page: pagination.pageIndex,
        size: pagination.pageSize,
        keyword: keyword || undefined,
        status: statusFilter || undefined,
        role: roleFilter || undefined,
        sortBy,
        sortDirection,
      }),
  });

  const toggleStatus = useMutation({
    mutationFn: ({ userId, current }: { userId: number; current: UserStatus }) =>
      adminUserService.updateStatus(userId, {
        status: current === "ACTIVE" ? "BANNED" : "ACTIVE",
      }),
    onSuccess: (updated) => {
      toast.success(
        updated.status === "BANNED" ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản"
      );
      qc.invalidateQueries({ queryKey: ["users"] });
      setViewingUser((prev) => (prev && prev.id === updated.id ? { ...prev, status: updated.status } : prev));
    },
    onError: () => toast.error("Cập nhật thất bại"),
  });

  const updateRoles = useMutation({
    mutationFn: ({ userId, roles }: { userId: number; roles: RoleName[] }) =>
      adminUserService.updateRoles(userId, { roles }),
    onSuccess: () => {
      toast.success("Cập nhật quyền thành công");
      qc.invalidateQueries({ queryKey: ["users"] });
      setRoleTarget(null);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      const msg = err.response?.data?.message || "Cập nhật quyền thất bại";
      toast.error(msg);
    },
  });

  const resetPassword = useMutation({
    mutationFn: ({ userId, password }: { userId: number; password: string }) =>
      adminUserService.resetPassword(userId, { newPassword: password }),
    onSuccess: () => {
      toast.success("Đặt lại mật khẩu thành công");
      setResetPasswordTarget(null);
      setNewPassword("");
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      const msg = err.response?.data?.message || "Đặt lại mật khẩu thất bại";
      toast.error(msg);
    },
  });

  // Mutations for Address
  const deleteAddressMutation = useMutation({
    mutationFn: (addressId: number) =>
      adminUserService.deleteAddress(viewingUser!.id, addressId),
    onSuccess: () => {
      toast.success("Đã xóa địa chỉ thành công");
      qc.invalidateQueries({ queryKey: ["user-addresses", viewingUser?.id] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Xóa địa chỉ thất bại");
    },
  });

  const setDefaultAddressMutation = useMutation({
    mutationFn: (addressId: number) =>
      adminUserService.setDefaultAddress(viewingUser!.id, addressId),
    onSuccess: () => {
      toast.success("Đã đặt làm địa chỉ mặc định");
      qc.invalidateQueries({ queryKey: ["user-addresses", viewingUser?.id] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Đặt mặc định thất bại");
    },
  });

  const addAddressMutation = useMutation({
    mutationFn: (payload: AddressRequest) =>
      adminUserService.addAddress(viewingUser!.id, payload),
    onSuccess: () => {
      toast.success("Đã thêm địa chỉ mới");
      qc.invalidateQueries({ queryKey: ["user-addresses", viewingUser?.id] });
      setShowAddressForm(false);
      resetAddressForm();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Thêm địa chỉ thất bại");
    },
  });

  const updateAddressMutation = useMutation({
    mutationFn: ({ addressId, payload }: { addressId: number; payload: AddressRequest }) =>
      adminUserService.updateAddress(viewingUser!.id, addressId, payload),
    onSuccess: () => {
      toast.success("Đã cập nhật địa chỉ thành công");
      qc.invalidateQueries({ queryKey: ["user-addresses", viewingUser?.id] });
      setShowAddressForm(false);
      resetAddressForm();
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Cập nhật thất bại");
    },
  });

  const deleteWishlistItemMutation = useMutation({
    mutationFn: (productId: number) =>
      adminUserService.deleteWishlistItem(viewingUser!.id, productId),
    onSuccess: () => {
      toast.success("Đã xóa sản phẩm khỏi yêu thích");
      qc.invalidateQueries({ queryKey: ["user-wishlist", viewingUser?.id] });
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Xóa sản phẩm thất bại");
    },
  });

  const updateUserInfo = useMutation({
    mutationFn: ({ userId, fullName, phone }: { userId: number; fullName: string; phone?: string }) =>
      adminUserService.updateUserInfo(userId, { fullName, phone }),
    onSuccess: (updated) => {
      toast.success("Cập nhật thông tin người dùng thành công");
      qc.invalidateQueries({ queryKey: ["users"] });
      setViewingUser((prev) => (prev && prev.id === updated.id ? updated : prev));
      setEditingUserId(null);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      const msg = err.response?.data?.message || "Cập nhật thông tin thất bại";
      toast.error(msg);
    },
  });

  const uploadUserAvatar = useMutation({
    mutationFn: ({ userId, file }: { userId: number; file: File }) =>
      adminUserService.uploadUserAvatar(userId, file),
    onSuccess: (updated) => {
      toast.success("Cập nhật avatar thành công");
      qc.invalidateQueries({ queryKey: ["users"] });
      setViewingUser((prev) => (prev && prev.id === updated.id ? updated : prev));
      setSelectedAvatarFile(null);
      setAvatarPreview(null);
    },
    onError: (error: unknown) => {
      const err = error as { response?: { data?: { message?: string } } };
      const msg = err.response?.data?.message || "Cập nhật avatar thất bại";
      toast.error(msg);
    },
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

  const columns: ColumnDef<UserResponse, unknown>[] = [
    {
      id: "user",
      header: () => (
        <button
          onClick={() => handleSort("fullName")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Người dùng
          {sortBy === "fullName" ? (
            sortDirection === "asc" ? <ArrowUp className="h-3.5 w-3.5 text-pine-400" /> : <ArrowDown className="h-3.5 w-3.5 text-pine-400" />
          ) : <ArrowUpDown className="h-3.5 w-3.5 text-slate-600" />}
        </button>
      ),
      cell: ({ row }) => (
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-pine-600 to-pine-800 text-xs font-semibold text-white">
            {getInitials(row.original.fullName)}
          </div>
          <div>
            <p className="text-xs font-medium text-slate-200">
              {row.original.fullName}
            </p>
            <p className="text-xs text-slate-500">{row.original.email}</p>
          </div>
        </div>
      ),
    },
    {
      accessorKey: "phone",
      header: "SĐT",
      cell: ({ getValue }) => (
        <span className="text-xs text-slate-400">{getValue<string>() ?? "—"}</span>
      ),
    },
    {
      accessorKey: "roles",
      header: "Quyền",
      cell: ({ getValue }) => (
        <div className="flex flex-wrap gap-1">
          {(getValue<string[]>() ?? []).map((r) => (
            <span
              key={r}
              className="rounded-full bg-violet-500/15 px-2 py-0.5 text-xs font-medium text-violet-400"
            >
              {r.replace("ROLE_", "")}
            </span>
          ))}
        </div>
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
      cell: ({ getValue }) => <StatusBadge status={getValue<UserStatus>()} />,
    },
    {
      accessorKey: "createdAt",
      header: () => (
        <button
          onClick={() => handleSort("createdAt")}
          className="flex items-center gap-1 hover:text-slate-200 text-left font-semibold uppercase text-slate-500"
        >
          Ngày tham gia
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
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleViewUser(row.original)}
            className="flex items-center gap-1.5 rounded-lg bg-pine-500/10 px-2.5 py-1.5 text-xs font-medium text-pine-400 hover:bg-pine-500/20"
          >
            <Eye className="h-3.5 w-3.5" /> Xem
          </button>

          <button
            onClick={() =>
              toggleStatus.mutate({
                userId: row.original.id,
                current: row.original.status,
              })
            }
            disabled={toggleStatus.isPending}
            className={`flex items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
              row.original.status === "ACTIVE"
                ? "bg-red-500/10 text-red-400 hover:bg-red-500/20"
                : "bg-pine-500/10 text-pine-400 hover:bg-pine-500/20"
            }`}
          >
            {row.original.status === "ACTIVE" ? (
              <>
                <Lock className="h-3.5 w-3.5" /> Khóa
              </>
            ) : (
              <>
                <Unlock className="h-3.5 w-3.5" /> Kích hoạt
              </>
            )}
          </button>

          <button
            onClick={() => {
              setRoleTarget(row.original);
              const highestRole = row.original.roles.includes("ROLE_ADMIN")
                ? "ROLE_ADMIN"
                : row.original.roles.includes("ROLE_FARMER")
                ? "ROLE_FARMER"
                : "ROLE_USER";
              setSelectedRole(highestRole);
            }}
            className="flex items-center gap-1.5 rounded-lg bg-violet-500/10 px-2.5 py-1.5 text-xs font-medium text-violet-400 hover:bg-violet-500/20"
          >
            <Shield className="h-3.5 w-3.5" /> Phân quyền
          </button>

          <button
            onClick={() => setResetPasswordTarget(row.original)}
            className="flex items-center gap-1.5 rounded-lg bg-amber-500/10 px-2.5 py-1.5 text-xs font-medium text-amber-400 hover:bg-amber-500/20"
          >
            <Key className="h-3.5 w-3.5" /> Reset Pass
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}>
        <h1 className="font-display text-2xl font-bold text-slate-100">
          Quản lý người dùng
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          {data?.totalElements ?? 0} người dùng
        </p>
      </motion.div>

      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px] max-w-sm">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Tìm tên, email..."
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setPagination((p) => ({ ...p, pageIndex: 0 }));
            }}
            className="h-9 w-full rounded-lg border border-slate-700 bg-slate-800 pl-9 pr-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
          />
        </div>

        {/* Status Filter */}
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value);
            setPagination((p) => ({ ...p, pageIndex: 0 }));
          }}
          className="h-9 rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Hoạt động</option>
          <option value="INACTIVE">Ngừng hoạt động</option>
          <option value="BANNED">Bị khóa</option>
        </select>

        {/* Role Filter */}
        <select
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value);
            setPagination((p) => ({ ...p, pageIndex: 0 }));
          }}
          className="h-9 rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 outline-none focus:border-pine-500"
        >
          <option value="">Tất cả vai trò</option>
          <option value="ROLE_USER">USER (Khách hàng)</option>
          <option value="ROLE_FARMER">FARMER (Nông dân)</option>
          <option value="ROLE_ADMIN">ADMIN (Quản trị viên)</option>
        </select>
      </div>

      <DataTable
        data={data?.content ?? []}
        columns={columns}
        pageCount={data?.totalPages ?? 0}
        pagination={pagination}
        onPaginationChange={setPagination}
        isLoading={isLoading}
        onRowDoubleClick={(user) => handleViewUser(user)}
      />

      {/* User Details Modal */}
      <AnimatePresence>
        {viewingUser && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
            onClick={() => setViewingUser(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 15 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 15 }}
              onClick={(e) => e.stopPropagation()}
              className="relative w-full max-w-lg overflow-hidden rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl"
            >
              {/* Decorative top background */}
              <div className="absolute inset-x-0 top-0 h-32 bg-gradient-to-r from-pine-600/20 to-violet-600/20 blur-2xl" />
              
              {/* Close button */}
              <button
                onClick={() => setViewingUser(null)}
                className="absolute right-4 top-4 z-10 rounded-lg p-1.5 text-slate-400 hover:bg-slate-800/80 hover:text-slate-200 transition-colors"
              >
                <X className="h-5 w-5" />
              </button>

              <div className="relative px-6 pt-8 pb-6">
                {/* Header */}
                <div className="flex flex-col items-center border-b border-slate-800/60 pb-6 text-center">
                  {/* Avatar display with edit overlay */}
                  <div className="relative group">
                    <div className="relative flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-pine-600 to-pine-800 text-2xl font-bold text-white shadow-xl shadow-pine-500/20 overflow-hidden">
                      {viewingUser.avatar ? (
                        <Image src={viewingUser.avatar} alt={viewingUser.fullName} width={80} height={80} className="h-full w-full object-cover" />
                      ) : (
                        getInitials(viewingUser.fullName)
                      )}
                    </div>
                    {editingUserId === viewingUser.id && (
                      <label className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-full cursor-pointer opacity-0 group-hover:opacity-100 transition-opacity">
                        <input
                          type="file"
                          accept="image/*"
                          className="hidden"
                          onChange={(e) => {
                            const file = e.target.files?.[0];
                            if (file) {
                              setSelectedAvatarFile(file);
                              const reader = new FileReader();
                              reader.onload = (event) => {
                                setAvatarPreview(event.target?.result as string);
                              };
                              reader.readAsDataURL(file);
                            }
                          }}
                        />
                        <div className="flex items-center justify-center text-white text-xs">
                          <span>Chọn ảnh</span>
                        </div>
                      </label>
                    )}
                  </div>

                  {/* Avatar preview if selected */}
                  {avatarPreview && (
                    <p className="mt-2 text-xs text-pine-400">Ảnh được chọn</p>
                  )}

                  <h2 className="mt-4 font-display text-xl font-bold text-slate-100">
                    {viewingUser.fullName}
                  </h2>
                  <div className="mt-2 flex flex-wrap justify-center gap-1.5">
                    {viewingUser.roles.map((role) => (
                      <span
                        key={role}
                        className="inline-flex items-center gap-1 rounded-full bg-violet-500/10 px-2.5 py-0.5 text-xs font-semibold text-violet-400 border border-violet-500/20"
                      >
                        <Shield className="h-3 w-3" />
                        {role.replace("ROLE_", "")}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Tabs bar */}
                <div className="mt-5 flex border-b border-slate-800/60 mb-4">
                  <button
                    onClick={() => setActiveTab("info")}
                    className={`flex-1 pb-2.5 text-center text-xs font-semibold border-b-2 transition-colors focus:outline-none ${
                      activeTab === "info"
                        ? "border-pine-500 text-pine-400"
                        : "border-transparent text-slate-400 hover:text-slate-200"
                    }`}
                  >
                    Thông tin cơ bản
                  </button>
                  <button
                    onClick={() => setActiveTab("addresses")}
                    className={`flex-1 pb-2.5 text-center text-xs font-semibold border-b-2 transition-colors focus:outline-none ${
                      activeTab === "addresses"
                        ? "border-pine-500 text-pine-400"
                        : "border-transparent text-slate-400 hover:text-slate-200"
                    }`}
                  >
                    Sổ địa chỉ
                  </button>
                  <button
                    onClick={() => setActiveTab("wishlist")}
                    className={`flex-1 pb-2.5 text-center text-xs font-semibold border-b-2 transition-colors focus:outline-none ${
                      activeTab === "wishlist"
                        ? "border-pine-500 text-pine-400"
                        : "border-transparent text-slate-400 hover:text-slate-200"
                    }`}
                  >
                    Sản phẩm yêu thích
                  </button>
                </div>

                {/* Tab content 1: Basic Info */}
                {activeTab === "info" && (
                  <>
                    {editingUserId === viewingUser.id ? (
                      // Edit mode
                      <form
                        onSubmit={(e) => {
                          e.preventDefault();
                          if (!editFormData.fullName.trim()) {
                            toast.error("Họ tên không được để trống");
                            return;
                          }
                          const requestData: { userId: number; fullName: string; phone?: string } = {
                            userId: viewingUser.id,
                            fullName: editFormData.fullName,
                          };
                          // Only include phone if it has a value
                          if (editFormData.phone?.trim()) {
                            requestData.phone = editFormData.phone;
                          }
                          updateUserInfo.mutate(requestData);
                          // Upload avatar if selected
                          if (selectedAvatarFile) {
                            setTimeout(() => {
                              uploadUserAvatar.mutate({
                                userId: viewingUser.id,
                                file: selectedAvatarFile,
                              });
                            }, 500); // Delay slightly to let info update first
                          }
                        }}
                        className="mt-4 space-y-4"
                      >
                        <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-3">
                          Chỉnh sửa thông tin
                        </h3>

                        {/* Full Name */}
                        <div>
                          <label className="text-xs font-semibold uppercase tracking-wider text-slate-400">Họ tên</label>
                          <input
                            type="text"
                            value={editFormData.fullName}
                            onChange={(e) => setEditFormData({ ...editFormData, fullName: e.target.value })}
                            maxLength={100}
                            className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-sm text-slate-200 placeholder-slate-500 outline-none focus:border-pine-500 transition"
                            placeholder="Nhập họ tên"
                          />
                        </div>

                        {/* Phone */}
                        <div>
                          <label className="text-xs font-semibold uppercase tracking-wider text-slate-400">Số điện thoại</label>
                          <input
                            type="tel"
                            value={editFormData.phone}
                            onChange={(e) => setEditFormData({ ...editFormData, phone: e.target.value })}
                            pattern="^(0[3|5|7|8|9])+([0-9]{8})$"
                            className="mt-1.5 w-full rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-sm text-slate-200 placeholder-slate-500 outline-none focus:border-pine-500 transition"
                            placeholder="0912345678"
                          />
                          <p className="mt-1 text-xs text-slate-500">Định dạng: 0xxxxxxxxx</p>
                        </div>

                        {/* Avatar Upload */}
                        <div>
                          <label className="text-xs font-semibold uppercase tracking-wider text-slate-400">Avatar</label>
                          <div className="mt-1.5 relative">
                            <input
                              type="file"
                              accept="image/*"
                              className="hidden"
                              id="avatar-input"
                              onChange={(e) => {
                                const file = e.target.files?.[0];
                                if (file) {
                                  if (file.size > 5 * 1024 * 1024) {
                                    toast.error("Kích thước ảnh không vượt quá 5MB");
                                    return;
                                  }
                                  setSelectedAvatarFile(file);
                                  const reader = new FileReader();
                                  reader.onload = (event) => {
                                    setAvatarPreview(event.target?.result as string);
                                  };
                                  reader.readAsDataURL(file);
                                }
                              }}
                            />
                            <label
                              htmlFor="avatar-input"
                              className="block text-center w-full rounded-lg border border-dashed border-slate-700 bg-slate-800/50 px-3.5 py-4 text-sm text-slate-400 hover:border-pine-500 hover:bg-slate-800 hover:text-slate-200 cursor-pointer transition"
                            >
                              {selectedAvatarFile ? (
                                <>
                                  <p className="font-medium">{selectedAvatarFile.name}</p>
                                  <p className="text-xs mt-1">Nhấp để thay đổi ảnh</p>
                                </>
                              ) : (
                                <>
                                  <p>Chọn ảnh để tải lên</p>
                                  <p className="text-xs mt-1">Tối đa 5MB, định dạng: JPG, PNG, WebP</p>
                                </>
                              )}
                            </label>
                          </div>
                        </div>

                        {/* Form Actions */}
                        <div className="flex gap-2 pt-2">
                          <button
                            type="submit"
                            disabled={updateUserInfo.isPending || uploadUserAvatar.isPending}
                            className="flex-1 rounded-lg bg-pine-600 px-3.5 py-2 text-xs font-semibold text-white hover:bg-pine-700 disabled:opacity-50 transition"
                          >
                            {updateUserInfo.isPending ? "Đang lưu..." : "Lưu thay đổi"}
                          </button>
                          <button
                            type="button"
                            onClick={() => resetEditForm()}
                            className="flex-1 rounded-lg border border-slate-700 bg-slate-800 px-3.5 py-2 text-xs font-semibold text-slate-200 hover:bg-slate-700 transition"
                          >
                            Hủy
                          </button>
                        </div>
                      </form>
                    ) : (
                      // View mode
                      <>
                        <div className="mt-4 space-y-4">
                          <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                            Thông tin chi tiết
                          </h3>

                          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                            {/* User ID */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5">
                              <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Mã khách hàng</p>
                              <p className="mt-1 font-mono text-sm font-semibold text-slate-200">#{viewingUser.id}</p>
                            </div>

                            {/* Status badge */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5">
                              <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Trạng thái tài khoản</p>
                              <div className="mt-1.5 flex">
                                <StatusBadge status={viewingUser.status} />
                              </div>
                            </div>

                            {/* Email */}
                            <div className="col-span-1 sm:col-span-2 rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                              <Mail className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                              <div>
                                <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Địa chỉ Email</p>
                                <p className="mt-0.5 text-sm font-medium text-slate-200 break-all">{viewingUser.email}</p>
                              </div>
                            </div>

                            {/* Phone */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                              <Phone className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                              <div>
                                <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Số điện thoại</p>
                                <p className="mt-0.5 text-sm font-medium text-slate-200">{viewingUser.phone ?? "Chưa cập nhật"}</p>
                              </div>
                            </div>

                            {/* Provider */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                              <Globe className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                              <div>
                                <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Đăng nhập bằng</p>
                                <span className="mt-1 inline-block rounded-md bg-slate-800 px-2 py-0.5 text-xs font-semibold text-slate-300">
                                  {viewingUser.provider ?? "LOCAL"}
                                </span>
                              </div>
                            </div>

                            {/* Created Date */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                              <Calendar className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                              <div>
                                <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Ngày tham gia</p>
                                <p className="mt-0.5 text-xs font-medium text-slate-300">{formatDate(viewingUser.createdAt)}</p>
                              </div>
                            </div>

                            {/* Updated Date */}
                            <div className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex items-start gap-3">
                              <Calendar className="mt-0.5 h-4.5 w-4.5 text-slate-500 shrink-0" />
                              <div>
                                <p className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Cập nhật gần nhất</p>
                                <p className="mt-0.5 text-xs font-medium text-slate-300">{formatDate(viewingUser.updatedAt)}</p>
                              </div>
                            </div>
                          </div>
                        </div>
                      </>
                    )}

                    {/* Actions Section */}
                    <div className="mt-6 border-t border-slate-800/60 pt-4">
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500 mb-3">
                        Hành động khả dụng
                      </h3>
                      <div className="flex flex-wrap gap-2.5">
                        {editingUserId === viewingUser.id ? (
                          // Upload avatar button in edit mode
                          <button
                            type="button"
                            onClick={() => {
                              const input = document.getElementById("avatar-input") as HTMLInputElement;
                              input?.click();
                            }}
                            disabled={uploadUserAvatar.isPending}
                            className="flex flex-1 min-w-[120px] items-center justify-center gap-1.5 rounded-lg bg-blue-500/10 px-3.5 py-2 text-xs font-semibold text-blue-400 hover:bg-blue-500/20 border border-blue-500/20 transition disabled:opacity-50"
                          >
                            {uploadUserAvatar.isPending ? (
                              <>Đang tải...</>
                            ) : (
                              <>
                                <Edit2 className="h-3.5 w-3.5" /> Chọn ảnh
                              </>
                            )}
                          </button>
                        ) : (
                          <>
                            {/* Edit Button */}
                            <button
                              onClick={() => {
                                setEditingUserId(viewingUser.id);
                                setEditFormData({
                                  fullName: viewingUser.fullName,
                                  phone: viewingUser.phone || "",
                                });
                              }}
                              className="flex flex-1 min-w-[120px] items-center justify-center gap-1.5 rounded-lg bg-blue-500/10 px-3.5 py-2 text-xs font-semibold text-blue-400 hover:bg-blue-500/20 border border-blue-500/20 transition"
                            >
                              <Edit2 className="h-3.5 w-3.5" /> Chỉnh sửa
                            </button>

                            {/* Status Toggle Action */}
                            <button
                              onClick={() => {
                                toggleStatus.mutate({
                                  userId: viewingUser.id,
                                  current: viewingUser.status,
                                });
                              }}
                              disabled={toggleStatus.isPending}
                              className={`flex flex-1 min-w-[120px] items-center justify-center gap-1.5 rounded-lg px-3.5 py-2 text-xs font-semibold transition-colors disabled:opacity-50 ${
                                viewingUser.status === "ACTIVE"
                                  ? "bg-red-500/10 text-red-400 hover:bg-red-500/20 border border-red-500/20"
                                  : "bg-pine-500/10 text-pine-400 hover:bg-pine-500/20 border border-pine-500/20"
                              }`}
                            >
                              {viewingUser.status === "ACTIVE" ? (
                                <>
                                  <Lock className="h-3.5 w-3.5" /> Khóa tài khoản
                                </>
                              ) : (
                                <>
                                  <Unlock className="h-3.5 w-3.5" /> Kích hoạt tài khoản
                                </>
                              )}
                            </button>

                            {/* Role Phân Quyền Action */}
                            <button
                              onClick={() => {
                                setRoleTarget(viewingUser);
                                const highestRole = viewingUser.roles.includes("ROLE_ADMIN")
                                  ? "ROLE_ADMIN"
                                  : viewingUser.roles.includes("ROLE_FARMER")
                                  ? "ROLE_FARMER"
                                  : "ROLE_USER";
                                setSelectedRole(highestRole);
                                setViewingUser(null); // Close detail modal
                              }}
                              className="flex flex-1 min-w-[120px] items-center justify-center gap-1.5 rounded-lg bg-violet-500/10 px-3.5 py-2 text-xs font-semibold text-violet-400 hover:bg-violet-500/20 border border-violet-500/20 transition"
                            >
                              <Shield className="h-3.5 w-3.5" /> Phân quyền
                            </button>

                            {/* Reset Password Action */}
                            <button
                              onClick={() => {
                                setResetPasswordTarget(viewingUser);
                                setViewingUser(null); // Close detail modal
                              }}
                              className="flex flex-1 min-w-[120px] items-center justify-center gap-1.5 rounded-lg bg-amber-500/10 px-3.5 py-2 text-xs font-semibold text-amber-400 hover:bg-amber-500/20 border border-amber-500/20 transition"
                            >
                              <Key className="h-3.5 w-3.5" /> Reset Pass
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </>
                )}

                {/* Tab content 2: Sổ địa chỉ */}
                {activeTab === "addresses" && (
                  <div className="mt-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                        Danh sách địa chỉ giao hàng
                      </h3>
                      {!showAddressForm && (
                        <button
                          onClick={() => {
                            resetAddressForm();
                            setShowAddressForm(true);
                          }}
                          className="flex items-center gap-1 text-[11px] font-bold text-pine-400 hover:text-pine-300 transition-colors"
                        >
                          <Plus className="h-3.5 w-3.5" /> Thêm địa chỉ
                        </button>
                      )}
                    </div>

                    {showAddressForm ? (
                      <form
                        onSubmit={(e) => {
                          e.preventDefault();
                          
                          // Zod schema validation
                          const validation = addressSchema.safeParse({
                            receiverName: addressData.receiverName,
                            phone: addressData.phone,
                            province: addressData.province,
                            district: addressData.district,
                            ward: addressData.ward,
                            detail: addressData.detail,
                            isDefault: addressData.isDefault,
                          });
                          
                          if (!validation.success) {
                            const errorMsg = validation.error.errors[0]?.message || "Thông tin không hợp lệ";
                            toast.error(errorMsg);
                            return;
                          }

                          // Carrier metadata validation (require selection of carrier IDs)
                          if (!selectedProvinceId || !selectedDistrictId || !selectedWardCode) {
                            toast.error("Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện, Phường/Xã từ danh sách.");
                            return;
                          }

                          const payload = {
                            ...addressData,
                            carrierMetadata: {
                              GHN: {
                                provinceId: selectedProvinceId,
                                districtId: selectedDistrictId,
                                wardCode: selectedWardCode,
                              }
                            }
                          };

                          if (editingAddressId) {
                            updateAddressMutation.mutate({ addressId: editingAddressId, payload });
                          } else {
                            addAddressMutation.mutate(payload);
                          }
                        }}
                        className="mt-2 space-y-3 rounded-xl border border-slate-800 bg-slate-950/40 p-4 text-xs"
                      >
                        <h4 className="font-bold text-slate-200">
                          {editingAddressId ? "Cập nhật địa chỉ" : "Thêm địa chỉ mới"}
                        </h4>
                        <div className="grid grid-cols-2 gap-2.5">
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Tên người nhận</label>
                            <input
                              type="text"
                              required
                              value={addressData.receiverName}
                              onChange={(e) => setAddressData({ ...addressData, receiverName: e.target.value })}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-800 px-2 text-slate-200 outline-none focus:border-pine-500"
                            />
                          </div>
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Số điện thoại</label>
                            <input
                              type="text"
                              required
                              value={addressData.phone}
                              onChange={(e) => setAddressData({ ...addressData, phone: e.target.value })}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-800 px-2 text-slate-200 outline-none focus:border-pine-500"
                            />
                          </div>
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Tỉnh / Thành phố</label>
                            <select
                              value={selectedProvinceId || ""}
                              onChange={(e) => {
                                const id = e.target.value;
                                const name = provinces.find((p: LocationItem) => p.id === id)?.name || "";
                                setSelectedProvinceId(id || null);
                                setSelectedDistrictId(null);
                                setSelectedWardCode(null);
                                setAddressData({ ...addressData, province: name, district: "", ward: "" });
                              }}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-850 px-2 text-slate-200 outline-none focus:border-pine-500 text-xs"
                            >
                              <option value="">Chọn Tỉnh / Thành phố</option>
                              {provinces.map((p: LocationItem) => (
                                <option key={p.id} value={p.id}>{p.name}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Quận / Huyện</label>
                            <select
                              value={selectedDistrictId || ""}
                              disabled={!selectedProvinceId}
                              onChange={(e) => {
                                const id = e.target.value;
                                const name = districts.find((d: LocationItem) => d.id === id)?.name || "";
                                setSelectedDistrictId(id || null);
                                setSelectedWardCode(null);
                                setAddressData({ ...addressData, district: name, ward: "" });
                              }}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-850 px-2 text-slate-200 outline-none focus:border-pine-500 text-xs disabled:opacity-50"
                            >
                              <option value="">Chọn Quận / Huyện</option>
                              {districts.map((d: LocationItem) => (
                                <option key={d.id} value={d.id}>{d.name}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Phường / Xã</label>
                            <select
                              value={selectedWardCode || ""}
                              disabled={!selectedDistrictId}
                              onChange={(e) => {
                                const id = e.target.value;
                                const name = wards.find((w: LocationItem) => w.id === id)?.name || "";
                                setSelectedWardCode(id || null);
                                setAddressData({ ...addressData, ward: name });
                              }}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-850 px-2 text-slate-200 outline-none focus:border-pine-500 text-xs disabled:opacity-50"
                            >
                              <option value="">Chọn Phường / Xã</option>
                              {wards.map((w: LocationItem) => (
                                <option key={w.id} value={w.id}>{w.name}</option>
                              ))}
                            </select>
                          </div>
                          <div>
                            <label className="text-[10px] text-slate-500 font-semibold uppercase">Địa chỉ chi tiết</label>
                            <input
                              type="text"
                              required
                              value={addressData.detail}
                              onChange={(e) => setAddressData({ ...addressData, detail: e.target.value })}
                              className="mt-1 h-8 w-full rounded-md border border-slate-700 bg-slate-800 px-2 text-slate-200 outline-none focus:border-pine-500"
                            />
                          </div>
                        </div>
                        <label className="flex items-center gap-2 mt-2 text-slate-400 cursor-pointer">
                          <input
                            type="checkbox"
                            checked={addressData.isDefault}
                            onChange={(e) => setAddressData({ ...addressData, isDefault: e.target.checked })}
                            className="rounded bg-slate-800 border-slate-700 text-pine-500 focus:ring-pine-500"
                          />
                          Đặt làm địa chỉ mặc định
                        </label>
                        <div className="flex gap-2 justify-end mt-3">
                          <button
                            type="button"
                            onClick={() => setShowAddressForm(false)}
                            className="rounded bg-slate-800 px-3 py-1.5 text-slate-400 hover:bg-slate-700 transition"
                          >
                            Hủy
                          </button>
                          <button
                            type="submit"
                            disabled={addAddressMutation.isPending || updateAddressMutation.isPending}
                            className="rounded bg-pine-500 px-3 py-1.5 text-white hover:bg-pine-600 disabled:opacity-50 transition"
                          >
                            Lưu
                          </button>
                        </div>
                      </form>
                    ) : addressesLoading ? (
                      <div className="py-8 space-y-2.5">
                        <div className="h-14 w-full animate-pulse rounded-xl bg-slate-800/40" />
                        <div className="h-14 w-full animate-pulse rounded-xl bg-slate-800/40" />
                      </div>
                    ) : !userAddresses || userAddresses.length === 0 ? (
                      <p className="py-12 text-center text-xs text-slate-400">Người dùng chưa cập nhật địa chỉ giao hàng nào.</p>
                    ) : (
                      <div className="space-y-2.5 max-h-[280px] overflow-y-auto pr-1 scrollbar-thin">
                        {userAddresses.map((addr) => (
                          <div
                            key={addr.id}
                            className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-3.5 flex flex-col gap-1 text-xs"
                          >
                            <div className="flex items-start justify-between">
                              <div className="flex flex-col gap-0.5">
                                <div className="flex items-center gap-2">
                                  <span className="font-semibold text-slate-200">{addr.receiverName}</span>
                                  <span className="text-slate-400 font-mono">{addr.phone}</span>
                                </div>
                                <p className="text-slate-300 leading-relaxed mt-1">
                                  {addr.detail}, {addr.ward}, {addr.district}, {addr.province}
                                </p>
                              </div>
                              <div className="flex items-center gap-1.5 shrink-0 ml-2">
                                {!addr.isDefault && (
                                  <button
                                    onClick={() => setDefaultAddressMutation.mutate(addr.id)}
                                    disabled={setDefaultAddressMutation.isPending}
                                    title="Đặt làm mặc định"
                                    className="p-1 rounded text-slate-500 hover:bg-slate-800 hover:text-amber-400 transition-colors"
                                  >
                                    <Star className="h-3.5 w-3.5" />
                                  </button>
                                )}
                                <button
                                  onClick={() => {
                                    setAddressData({
                                      receiverName: addr.receiverName,
                                      phone: addr.phone,
                                      province: addr.province,
                                      district: addr.district,
                                      ward: addr.ward,
                                      detail: addr.detail,
                                      isDefault: addr.isDefault,
                                    });
                                    setEditingAddressId(addr.id);
                                    
                                    const ghnMeta = addr.carrierMetadata?.GHN;
                                    if (ghnMeta) {
                                      setSelectedProvinceId(ghnMeta.provinceId || null);
                                      setSelectedDistrictId(ghnMeta.districtId || null);
                                      setSelectedWardCode(ghnMeta.wardCode || null);
                                    } else {
                                      setSelectedProvinceId(null);
                                      setSelectedDistrictId(null);
                                      setSelectedWardCode(null);
                                    }

                                    setShowAddressForm(true);
                                  }}
                                  title="Chỉnh sửa"
                                  className="p-1 rounded text-slate-500 hover:bg-slate-800 hover:text-pine-400 transition-colors"
                                >
                                  <Edit2 className="h-3.5 w-3.5" />
                                </button>
                                {!addr.isDefault && (
                                  <button
                                    onClick={() => {
                                      if (confirm("Bạn có chắc chắn muốn xóa địa chỉ này?")) {
                                        deleteAddressMutation.mutate(addr.id);
                                      }
                                    }}
                                    disabled={deleteAddressMutation.isPending}
                                    title="Xóa địa chỉ"
                                    className="p-1 rounded text-slate-500 hover:bg-slate-800 hover:text-red-400 transition-colors"
                                  >
                                    <Trash2 className="h-3.5 w-3.5" />
                                  </button>
                                )}
                              </div>
                            </div>
                            {addr.isDefault && (
                              <span className="mt-1 self-start inline-block rounded bg-pine-500/15 border border-pine-500/20 px-1.5 py-0.5 text-[10px] font-semibold text-pine-400">
                                Mặc định
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {/* Tab content 3: Sản phẩm yêu thích */}
                {activeTab === "wishlist" && (
                  <div className="mt-4 space-y-3">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                      Sản phẩm yêu thích (Wishlist)
                    </h3>
                    {wishlistLoading ? (
                      <div className="py-8 space-y-2.5">
                        <div className="h-12 w-full animate-pulse rounded-xl bg-slate-800/40" />
                        <div className="h-12 w-full animate-pulse rounded-xl bg-slate-800/40" />
                      </div>
                    ) : !userWishlist || !userWishlist.content || userWishlist.content.length === 0 ? (
                      <p className="py-12 text-center text-xs text-slate-400">Danh sách yêu thích trống.</p>
                    ) : (
                      <div className="space-y-2.5 max-h-[280px] overflow-y-auto pr-1 scrollbar-thin">
                        {userWishlist.content.map((item) => (
                          <div
                            key={item.id}
                            className="rounded-xl border border-slate-800/50 bg-slate-950/40 p-2.5 flex items-center justify-between gap-3 text-xs"
                          >
                            <div className="flex items-center gap-3">
                              <Image
                                src={item.productThumbnail}
                                alt={item.productName}
                                width={40}
                                height={40}
                                className="h-10 w-10 rounded-lg object-cover bg-slate-800 border border-slate-800 shrink-0"
                              />
                              <div>
                                <h4 className="font-semibold text-slate-200 line-clamp-1">{item.productName}</h4>
                                <p className="text-[10px] text-slate-500 mt-0.5">ID: #{item.productId}</p>
                              </div>
                            </div>
                            <div className="flex items-center gap-3 shrink-0">
                              <span className="font-bold text-pine-400">
                                {formatCurrency(item.productDiscountPrice ?? item.productPrice)}
                              </span>
                              <button
                                onClick={() => {
                                  if (confirm(`Xóa ${item.productName} khỏi danh sách yêu thích?`)) {
                                    deleteWishlistItemMutation.mutate(item.productId);
                                  }
                                }}
                                disabled={deleteWishlistItemMutation.isPending}
                                title="Xóa khỏi danh sách yêu thích"
                                className="p-1 rounded text-slate-500 hover:bg-slate-800 hover:text-red-400 transition-colors"
                              >
                                <Trash2 className="h-3.5 w-3.5" />
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {/* Footer close */}
                <div className="mt-6 flex justify-end border-t border-slate-800/60 pt-4">
                  <button
                    onClick={() => setViewingUser(null)}
                    className="rounded-lg border border-slate-700 px-4.5 py-2 text-sm font-medium text-slate-400 hover:bg-slate-800 transition"
                  >
                    Đóng
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Role Management Modal */}
      <AnimatePresence>
        {roleTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setRoleTarget(null)}
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
                  Phân quyền vai trò
                </h2>
                <button
                  onClick={() => setRoleTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 space-y-4">
                <div>
                  <p className="text-xs text-slate-400">
                    Người dùng: <span className="text-slate-200 font-medium">{roleTarget.fullName}</span>
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    Email: <span className="text-slate-200 font-mono">{roleTarget.email}</span>
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between rounded-lg bg-slate-800/40 p-3">
                    <label className="flex items-center gap-3 cursor-pointer w-full">
                      <input
                        type="radio"
                        name="user-role"
                        checked={selectedRole === "ROLE_USER"}
                        onChange={() => setSelectedRole("ROLE_USER")}
                        className="h-4 w-4 border-slate-700 bg-slate-800 text-pine-500 focus:ring-pine-500/50"
                      />
                      <div>
                        <p className="text-xs font-semibold text-slate-200">USER (Khách hàng)</p>
                        <p className="text-[10px] text-slate-500">Quyền mua sắm cơ bản của mọi tài khoản</p>
                      </div>
                    </label>
                  </div>

                  <div className="flex items-center justify-between rounded-lg bg-slate-800/40 p-3">
                    <label className="flex items-center gap-3 cursor-pointer w-full">
                      <input
                        type="radio"
                        name="user-role"
                        checked={selectedRole === "ROLE_FARMER"}
                        onChange={() => setSelectedRole("ROLE_FARMER")}
                        className="h-4 w-4 border-slate-700 bg-slate-800 text-pine-500 focus:ring-pine-500/50"
                      />
                      <div>
                        <p className="text-xs font-semibold text-slate-200">FARMER (Nông dân)</p>
                        <p className="text-[10px] text-slate-500">Quyền quản lý nông trại và bán sản phẩm</p>
                      </div>
                    </label>
                  </div>

                  <div className="flex items-center justify-between rounded-lg bg-slate-800/40 p-3">
                    <label className="flex items-center gap-3 cursor-pointer w-full">
                      <input
                        type="radio"
                        name="user-role"
                        checked={selectedRole === "ROLE_ADMIN"}
                        onChange={() => setSelectedRole("ROLE_ADMIN")}
                        className="h-4 w-4 border-slate-700 bg-slate-800 text-pine-500 focus:ring-pine-500/50"
                      />
                      <div>
                        <p className="text-xs font-semibold text-slate-200">ADMIN (Quản trị viên)</p>
                        <p className="text-[10px] text-slate-500">Quyền quản trị toàn bộ hệ thống</p>
                      </div>
                    </label>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex justify-end gap-3 border-t border-slate-800 pt-4">
                <button
                  onClick={() => setRoleTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  onClick={() =>
                    updateRoles.mutate({
                      userId: roleTarget.id,
                      roles: [selectedRole],
                    })
                  }
                  disabled={updateRoles.isPending}
                  className="rounded-lg bg-pine-500 px-4 py-2 text-sm font-medium text-white hover:bg-pine-600 disabled:opacity-50"
                >
                  {updateRoles.isPending ? "Đang xử lý..." : "Lưu thay đổi"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Reset Password Modal */}
      <AnimatePresence>
        {resetPasswordTarget && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
            onClick={() => setResetPasswordTarget(null)}
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
                  Đặt lại mật khẩu
                </h2>
                <button
                  onClick={() => setResetPasswordTarget(null)}
                  className="rounded-lg p-1.5 text-slate-500 hover:bg-slate-800"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <div className="mt-4 space-y-4">
                <div>
                  <p className="text-xs text-slate-400">
                    Đặt mật khẩu mới cho: <span className="text-slate-200 font-medium">{resetPasswordTarget.fullName}</span>
                  </p>
                  <p className="text-xs text-slate-400 mt-1">
                    Email: <span className="text-slate-200 font-mono">{resetPasswordTarget.email}</span>
                  </p>
                </div>

                <div>
                  <label className="text-xs font-medium text-slate-400">Mật khẩu mới *</label>
                  <div className="flex gap-2 mt-1">
                    <input
                      type="text"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Nhập mật khẩu mới..."
                      className="h-9 flex-1 rounded-lg border border-slate-700 bg-slate-800 px-3 text-sm text-slate-200 placeholder-slate-500 outline-none ring-pine-500/50 transition focus:border-pine-500 focus:ring-2"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        const chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$";
                        let generated = "";
                        for (let i = 0; i < 10; i++) {
                          generated += chars.charAt(Math.floor(Math.random() * chars.length));
                        }
                        setNewPassword(generated);
                      }}
                      className="rounded-lg border border-slate-700 px-3 text-xs font-medium text-slate-300 hover:bg-slate-800 transition-colors"
                    >
                      Ngẫu nhiên
                    </button>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex justify-end gap-3 border-t border-slate-800 pt-4">
                <button
                  onClick={() => setResetPasswordTarget(null)}
                  className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-400 hover:bg-slate-800"
                >
                  Hủy
                </button>
                <button
                  onClick={() =>
                    resetPassword.mutate({
                      userId: resetPasswordTarget.id,
                      password: newPassword,
                    })
                  }
                  disabled={resetPassword.isPending || !newPassword.trim() || newPassword.length < 6}
                  className="rounded-lg bg-pine-500 px-4 py-2 text-sm font-medium text-white hover:bg-pine-600 disabled:opacity-50"
                >
                  {resetPassword.isPending ? "Đang xử lý..." : "Xác nhận"}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}