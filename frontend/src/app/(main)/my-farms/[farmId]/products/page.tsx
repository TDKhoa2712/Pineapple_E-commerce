'use client'

import React, { useState, useEffect, useRef } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Image from 'next/image'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { motion, AnimatePresence } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import {
  Search, Plus, X, Pencil, Trash2, Eye, Leaf, ArrowLeft,
  Tractor, Calendar, Package, Layers, Activity, AlertTriangle,
  Upload, HelpCircle, Save, CheckCircle, ArrowUpDown, ChevronRight,
  TrendingUp, RefreshCw
} from 'lucide-react'

import {
  Button, Input, Badge, StarRating
} from '@/components/ui'
import {
  useMyProducts, useFarmBatches, useAddBatch, useAdjustBatch,
  useBatchAdjustments, useFarm, useCategories, useResubmitBatch
} from '@/hooks'
import { uploadApi } from '@/services/api'
import { adminProductService, adminCategoryService } from '@/services/admin.service'
import { formatCurrency, formatPrice } from '@/lib/utils'
import type {
  ProductResponse, ProductStatus, FarmStatus, CategoryNode,
  InventoryBatchResponse, BatchStatus
} from '@/types'

// ─── Product schema for Farmer ─────────────────────────────
const productSchema = z.object({
  name: z.string().min(1, 'Tên sản phẩm là bắt buộc').max(200),
  description: z.string().optional(),
  price: z.coerce.number().positive('Giá phải lớn hơn 0'),
  discountPrice: z.coerce.number().min(0).optional(),
  weight: z.coerce.number().min(0).max(999999.99, 'Khối lượng tối đa 999,999.99g').optional(),
  calories: z.coerce.number().min(0).max(9999.99, 'Calories tối đa 9,999.99 kcal').optional(),
  brand: z.string().optional(),
  origin: z.string().optional(),
  isOrganic: z.boolean().default(false),
  thumbnail: z.string().min(1, 'Vui lòng chọn hoặc tải lên một ảnh làm thumbnail'),
  categoryId: z.coerce.number().positive('Vui lòng chọn danh mục'),
  imageUrls: z.array(z.string()).default([]),
})
type ProductForm = z.infer<typeof productSchema>

// Helper to flatten categories for select options
function flattenCategories(
  cats: CategoryNode[],
  depth = 0
): { id: number; name: string; prefix: string }[] {
  const result: { id: number; name: string; prefix: string }[] = []
  for (const cat of cats) {
    result.push({ id: cat.id, name: cat.name, prefix: '  '.repeat(depth) })
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children, depth + 1))
    }
  }
  return result
}

export default function FarmerPortalPage() {
  const params = useParams()
  const router = useRouter()
  const qc = useQueryClient()
  const farmId = Number(params.farmId)

  const [activeTab, setActiveTab] = useState<'products' | 'batches'>('products')

  // Product tab states
  const [prodKeyword, setProdKeyword] = useState('')
  const [prodStatusFilter, setProdStatusFilter] = useState('')
  const [productPage, setProductPage] = useState(0)

  // Modals / Selection states
  const [showProductForm, setShowProductForm] = useState(false)
  const [editProductTarget, setEditProductTarget] = useState<any | null>(null)
  const [deleteProductTarget, setDeleteProductTarget] = useState<any | null>(null)
  const [detailProductTarget, setDetailProductTarget] = useState<any | null>(null)

  // Batch tab states
  const [batchKeyword, setBatchKeyword] = useState('')
  const [batchPage, setBatchPage] = useState(0)

  // Batch modals states
  const [showAddBatchModal, setShowAddBatchModal] = useState(false)
  const [showAdjustModal, setShowAdjustModal] = useState(false)
  const [showHistoryModal, setShowHistoryModal] = useState(false)
  const [selectedBatch, setSelectedBatch] = useState<InventoryBatchResponse | null>(null)

  // Adjustment form states
  const [adjustQty, setAdjustQty] = useState<number>(0)
  const [adjustReason, setAdjustReason] = useState('')

  // File Upload states
  const [uploadingImages, setUploadingImages] = useState(false)

  // Queries
  const { data: farmRes } = useFarm(farmId)
  const farm = farmRes?.data

  const { data: productsPageRes, isLoading: productsLoading } = useMyProducts({
    page: productPage,
    size: 12,
    keyword: prodKeyword || undefined,
    status: prodStatusFilter || undefined,
    sortBy: 'createdAt',
    sortDirection: 'desc'
  })
  const farmerProducts = productsPageRes?.data?.content ?? []

  const { data: batchesPageRes, isLoading: batchesLoading } = useFarmBatches(farmId, {
    page: batchPage,
    size: 10,
    keyword: batchKeyword || undefined,
    sortBy: 'createdAt',
    sortDirection: 'desc'
  })
  const batches = batchesPageRes?.data?.content ?? []

  const { data: categoryTree } = useCategories()
  const categoryOptions = categoryTree?.data ? flattenCategories(categoryTree.data) : []

  // Batch Adjustments log query
  const { data: adjustmentsRes, isLoading: historyLoading } = useBatchAdjustments(selectedBatch?.id ?? 0)
  const adjustments = adjustmentsRes ?? []

  // Mutations
  const createProductMutation = useMutation({
    mutationFn: (payload: ProductForm) => adminProductService.create(payload),
    onSuccess: () => {
      toast.success('Đã gửi sản phẩm mới thành công. Vui lòng chờ Admin duyệt.')
      qc.invalidateQueries({ queryKey: ['my-products'] })
      setShowProductForm(false)
      reset()
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Tạo sản phẩm thất bại')
    }
  })

  const updateProductMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ProductForm }) =>
      adminProductService.update(id, payload),
    onSuccess: () => {
      toast.success('Đã cập nhật sản phẩm. Sản phẩm đang chờ Admin duyệt lại.')
      qc.invalidateQueries({ queryKey: ['my-products'] })
      setShowProductForm(false)
      setEditProductTarget(null)
      reset()
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Cập nhật thất bại')
    }
  })

  const deleteProductMutation = useMutation({
    mutationFn: (id: number) => adminProductService.delete(id),
    onSuccess: () => {
      toast.success('Đã xoá sản phẩm')
      qc.invalidateQueries({ queryKey: ['my-products'] })
      setDeleteProductTarget(null)
    },
    onError: (err: any) => {
      toast.error(err?.response?.data?.message || 'Xoá sản phẩm thất bại')
    }
  })

  const addBatchMutation = useAddBatch()
  const adjustBatchMutation = useAdjustBatch()
  const resubmitBatchMutation = useResubmitBatch()

  // Form handling
  const {
    register, handleSubmit, reset, setValue, watch, formState: { errors }
  } = useForm<ProductForm>({
    resolver: zodResolver(productSchema),
    defaultValues: { isOrganic: false, imageUrls: [], thumbnail: '' }
  })

  // Set values when editing
  useEffect(() => {
    if (editProductTarget) {
      reset({
        name: editProductTarget.name,
        description: editProductTarget.description ?? '',
        price: editProductTarget.price,
        discountPrice: editProductTarget.discountPrice ?? 0,
        weight: editProductTarget.weight ?? 0,
        calories: editProductTarget.calories ?? 0,
        brand: editProductTarget.brand ?? '',
        origin: editProductTarget.origin ?? '',
        isOrganic: editProductTarget.isOrganic,
        thumbnail: editProductTarget.thumbnail,
        categoryId: editProductTarget.categoryId ?? 0,
        imageUrls: editProductTarget.imageUrls ?? []
      })
    }
  }, [editProductTarget, reset])

  // Batch form state
  const [newBatchProduct, setNewBatchProduct] = useState<number>(0)
  const [newBatchCode, setNewBatchCode] = useState('')
  const [newBatchQty, setNewBatchQty] = useState<number>(0)
  const [newBatchHarvestDate, setNewBatchHarvestDate] = useState('')
  const [newBatchExpiryDate, setNewBatchExpiryDate] = useState('')
  const [newBatchSweetness, setNewBatchSweetness] = useState<number>(0)

  const handleCreateProduct = (data: ProductForm) => {
    if (editProductTarget) {
      updateProductMutation.mutate({ id: editProductTarget.id, payload: data })
    } else {
      createProductMutation.mutate(data)
    }
  }

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (!files || files.length === 0) return

    setUploadingImages(true)
    const currentUrls = watch('imageUrls') || []
    const newUrls: string[] = []

    try {
      const promises = Array.from(files).map(async (file) => {
        const res = await uploadApi.uploadImage(file, 'PRODUCT')
        return res.data.url
      })
      const urls = await Promise.all(promises)
      newUrls.push(...urls)

      const updatedUrls = [...currentUrls, ...newUrls]
      setValue('imageUrls', updatedUrls)

      if (!watch('thumbnail')) {
        setValue('thumbnail', updatedUrls[0])
      }
      toast.success(`Đã tải lên ${urls.length} ảnh thành công!`)
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Tải ảnh thất bại')
    } finally {
      setUploadingImages(false)
      e.target.value = ''
    }
  }

  const handleRemoveImage = (urlToRemove: string) => {
    const currentUrls = watch('imageUrls') || []
    const updatedUrls = currentUrls.filter((url) => url !== urlToRemove)
    setValue('imageUrls', updatedUrls)

    if (watch('thumbnail') === urlToRemove) {
      setValue('thumbnail', updatedUrls.length > 0 ? updatedUrls[0] : '')
    }
  }

  const submitBatchForm = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newBatchProduct || !newBatchCode || newBatchQty <= 0 || !newBatchHarvestDate || !newBatchExpiryDate) {
      toast.error('Vui lòng điền đầy đủ các thông tin bắt buộc')
      return
    }

    addBatchMutation.mutate({
      productId: Number(newBatchProduct),
      farmId: farmId,
      batchCode: newBatchCode,
      quantity: Number(newBatchQty),
      harvestDate: newBatchHarvestDate,
      expiryDate: newBatchExpiryDate,
      sweetnessLevel: newBatchSweetness || undefined
    }, {
      onSuccess: () => {
        setShowAddBatchModal(false)
        // Reset form
        setNewBatchProduct(0)
        setNewBatchCode('')
        setNewBatchQty(0)
        setNewBatchHarvestDate('')
        setNewBatchExpiryDate('')
        setNewBatchSweetness(0)
      }
    })
  }

  const submitAdjustment = (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedBatch || adjustQty === 0 || !adjustReason.trim()) {
      toast.error('Vui lòng điền số lượng điều chỉnh và lý do')
      return
    }

    adjustBatchMutation.mutate({
      batchId: selectedBatch.id,
      payload: {
        adjustmentQty: adjustQty,
        reason: adjustReason
      }
    }, {
      onSuccess: () => {
        setShowAdjustModal(false)
        setAdjustQty(0)
        setAdjustReason('')
        setSelectedBatch(null)
      }
    })
  }

  // Calculate some simple dashboard stats
  const totalProductsCount = productsPageRes?.data?.totalElements ?? 0
  const activeProductsCount = farmerProducts.filter(p => p.status === 'ACTIVE').length
  const pendingProductsCount = farmerProducts.filter(p => p.status === 'INACTIVE').length
  const outOfStockProductsCount = farmerProducts.filter(p => p.status === 'OUT_OF_STOCK').length

  return (
    <div className="bg-[var(--color-cream)] min-h-screen pb-20">
      {/* Upper Banner */}
      <div className="bg-[var(--color-green-700)] relative overflow-hidden text-white">
        <div className="container-main py-10 relative z-10">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="space-y-2">
              <button
                onClick={() => router.push('/my-farms')}
                className="flex items-center gap-2 text-xs text-[var(--color-green-100)] hover:underline mb-2"
              >
                <ArrowLeft className="w-3.5 h-3.5" /> Quay lại danh sách nông trại
              </button>
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-white/10 flex items-center justify-center text-2xl">
                  🚜
                </div>
                <div>
                  <h1 className="text-2xl md:text-3xl font-bold tracking-tight" style={{ fontFamily: 'var(--font-display)' }}>
                    {farm?.name || 'Đang tải thông tin...'}
                  </h1>
                  <p className="text-sm text-[var(--color-green-100)] flex items-center gap-1">
                    📍 {farm?.location || 'Không rõ địa chỉ'}
                  </p>
                </div>
              </div>
            </div>

            {/* Quick Stats */}
            <div className="flex flex-wrap gap-4">
              <div className="bg-white/10 backdrop-blur-md rounded-xl p-3 border border-white/10 min-w-[100px]">
                <p className="text-[10px] uppercase font-semibold text-[var(--color-green-200)]">Sản phẩm</p>
                <p className="text-xl font-bold mt-1">{totalProductsCount}</p>
              </div>
              <div className="bg-white/10 backdrop-blur-md rounded-xl p-3 border border-white/10 min-w-[100px]">
                <p className="text-[10px] uppercase font-semibold text-[var(--color-green-200)]">Chờ duyệt</p>
                <p className="text-xl font-bold mt-1 text-amber-300">{pendingProductsCount}</p>
              </div>
              <div className="bg-white/10 backdrop-blur-md rounded-xl p-3 border border-white/10 min-w-[100px]">
                <p className="text-[10px] uppercase font-semibold text-[var(--color-green-200)]">Lô hàng</p>
                <p className="text-xl font-bold mt-1">{batchesPageRes?.data?.totalElements ?? 0}</p>
              </div>
            </div>
          </div>
        </div>
        <div className="absolute -right-20 -top-20 w-80 h-80 rounded-full bg-white/5" />
      </div>

      <div className="container-main mt-8">
        {/* Navigation Tabs */}
        <div className="flex border-b border-[var(--color-border)] mb-8 gap-6">
          <button
            onClick={() => setActiveTab('products')}
            className={`pb-4 text-sm font-semibold relative transition-colors ${
              activeTab === 'products'
                ? 'text-[var(--color-brown-900)]'
                : 'text-[var(--color-text-muted)] hover:text-[var(--color-text)]'
            }`}
          >
            <span className="flex items-center gap-2">
              <Package className="w-4 h-4" /> Danh sách sản phẩm
            </span>
            {activeTab === 'products' && (
              <motion.div layoutId="activeTabIndicator" className="absolute bottom-0 inset-x-0 h-0.5 bg-[var(--color-brown-900)]" />
            )}
          </button>
          <button
            onClick={() => setActiveTab('batches')}
            className={`pb-4 text-sm font-semibold relative transition-colors ${
              activeTab === 'batches'
                ? 'text-[var(--color-brown-900)]'
                : 'text-[var(--color-text-muted)] hover:text-[var(--color-text)]'
            }`}
          >
            <span className="flex items-center gap-2">
              <Layers className="w-4 h-4" /> Lô hàng & Tồn kho
            </span>
            {activeTab === 'batches' && (
              <motion.div layoutId="activeTabIndicator" className="absolute bottom-0 inset-x-0 h-0.5 bg-[var(--color-brown-900)]" />
            )}
          </button>
        </div>

        {/* Tab 1: Products */}
        {activeTab === 'products' && (
          <div className="space-y-6">
            {/* Toolbar */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white rounded-2xl border border-[var(--color-border)] p-4 shadow-sm">
              <div className="relative w-full sm:max-w-xs">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-muted)]" />
                <input
                  type="text"
                  placeholder="Tìm kiếm sản phẩm..."
                  value={prodKeyword}
                  onChange={(e) => {
                    setProdKeyword(e.target.value)
                    setProductPage(0)
                  }}
                  className="h-10 w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-cream)]/30 pl-10 pr-3 text-sm placeholder-[var(--color-text-subtle)] outline-none focus:border-[var(--color-gold-500)] transition"
                />
              </div>

              <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
                <select
                  value={prodStatusFilter}
                  onChange={(e) => {
                    setProdStatusFilter(e.target.value)
                    setProductPage(0)
                  }}
                  className="h-10 px-3 rounded-xl border border-[var(--color-border)] bg-white text-sm focus:outline-none"
                >
                  <option value="">Tất cả trạng thái</option>
                  <option value="ACTIVE">Đang bán</option>
                  <option value="INACTIVE">Chờ duyệt / Tạm ẩn</option>
                  <option value="OUT_OF_STOCK">Hết hàng</option>
                </select>

                <Button onClick={() => { setEditProductTarget(null); setShowProductForm(true) }} className="gap-2 shrink-0">
                  <Plus className="w-4 h-4" /> Thêm sản phẩm
                </Button>
              </div>
            </div>

            {/* List */}
            {productsLoading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                {[1, 2, 3, 4].map((i) => (
                  <div key={i} className="bg-white rounded-2xl border border-[var(--color-border)] p-4 space-y-4 animate-pulse">
                    <div className="aspect-[4/3] rounded-xl bg-slate-100" />
                    <div className="h-4 bg-slate-100 rounded w-3/4" />
                    <div className="h-3 bg-slate-100 rounded w-1/2" />
                    <div className="h-6 bg-slate-100 rounded w-1/3" />
                  </div>
                ))}
              </div>
            ) : farmerProducts.length === 0 ? (
              <div className="text-center py-20 bg-white rounded-2xl border border-[var(--color-border)]">
                <div className="text-6xl mb-4">🌾</div>
                <h3 className="text-lg font-bold text-[var(--color-brown-900)] mb-1" style={{ fontFamily: 'var(--font-display)' }}>
                  Không tìm thấy sản phẩm nào
                </h3>
                <p className="text-xs text-[var(--color-text-muted)] mb-6 max-w-xs mx-auto">
                  Bạn chưa đăng ký sản phẩm nào hoặc không khớp với bộ lọc. Hãy tạo sản phẩm mới.
                </p>
                <Button onClick={() => setShowProductForm(true)} className="gap-2">
                  <Plus className="w-4 h-4" /> Tạo sản phẩm đầu tiên
                </Button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                  {farmerProducts.map((product) => {
                    const isPending = product.status === 'INACTIVE'
                    return (
                      <motion.div
                        key={product.id}
                        whileHover={{ y: -4 }}
                        className="bg-white rounded-2xl overflow-hidden border border-[var(--color-border)] shadow-xs flex flex-col"
                      >
                        {/* Image */}
                        <div className="relative aspect-[4/3] bg-slate-50 shrink-0">
                          <Image
                            src={product.thumbnail || '/placeholder-product.jpg'}
                            alt={product.name}
                            fill
                            className="object-cover"
                          />
                          <div className="absolute top-2.5 left-2.5 flex flex-col gap-1.5">
                            {product.isOrganic && (
                              <Badge variant="organic" size="sm">🌿 Hữu cơ</Badge>
                            )}
                            {isPending ? (
                              <Badge variant="gold" size="sm">⏳ Chờ duyệt</Badge>
                            ) : product.status === 'OUT_OF_STOCK' ? (
                              <Badge variant="danger" size="sm">Hết hàng</Badge>
                            ) : (
                              <Badge variant="success" size="sm">Đang bán</Badge>
                            )}
                          </div>
                        </div>

                        {/* Details */}
                        <div className="p-4 flex-1 flex flex-col justify-between">
                          <div>
                            <p className="text-[10px] text-[var(--color-text-muted)] mb-1">{product.categoryName}</p>
                            <h4 className="font-bold text-sm text-[var(--color-brown-900)] line-clamp-2 leading-snug">
                              {product.name}
                            </h4>
                            <div className="flex items-center gap-1.5 mt-2">
                              <span className="text-sm font-bold text-[var(--color-green-700)]">
                                {formatPrice(product.discountPrice ?? product.price)}
                              </span>
                              {product.discountPrice && (
                                <span className="text-[10px] line-through text-[var(--color-text-muted)]">
                                  {formatPrice(product.price)}
                                </span>
                              )}
                            </div>
                            <p className="text-[10px] text-[var(--color-text-muted)] mt-1">
                              Tồn kho khả dụng: <span className="font-bold text-[var(--color-text)]">{product.totalStock}</span>
                            </p>
                          </div>

                          {/* Action row */}
                          <div className="flex items-center justify-end gap-1.5 pt-3 border-t border-[var(--color-border)] mt-4">
                            <button
                              onClick={() => setDetailProductTarget(product)}
                              className="p-1.5 rounded-lg text-[var(--color-text-muted)] hover:bg-[var(--color-brown-50)] hover:text-[var(--color-text)] transition"
                              title="Xem chi tiết"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => setEditProductTarget(product)}
                              className="p-1.5 rounded-lg text-blue-500 hover:bg-blue-50 transition"
                              title="Sửa thông tin"
                            >
                              <Pencil className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => setDeleteProductTarget(product)}
                              className="p-1.5 rounded-lg text-red-500 hover:bg-red-50 transition"
                              title="Xoá"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </div>
                      </motion.div>
                    )
                  })}
                </div>

                {/* Pagination */}
                {productsPageRes?.data && productsPageRes.data.totalPages > 1 && (
                  <div className="flex justify-center mt-8 gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={productPage === 0}
                      onClick={() => setProductPage(p => p - 1)}
                    >
                      ← Trước
                    </Button>
                    <span className="text-xs flex items-center px-4 font-semibold text-[var(--color-text)]">
                      Trang {productPage + 1} / {productsPageRes.data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={productPage >= productsPageRes.data.totalPages - 1}
                      onClick={() => setProductPage(p => p + 1)}
                    >
                      Sau →
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Tab 2: Inventory Batches */}
        {activeTab === 'batches' && (
          <div className="space-y-6">
            {/* Toolbar */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white rounded-2xl border border-[var(--color-border)] p-4 shadow-sm">
              <div className="relative w-full sm:max-w-xs">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--color-text-muted)]" />
                <input
                  type="text"
                  placeholder="Tìm mã lô hoặc tên sản phẩm..."
                  value={batchKeyword}
                  onChange={(e) => {
                    setBatchKeyword(e.target.value)
                    setBatchPage(0)
                  }}
                  className="h-10 w-full rounded-xl border border-[var(--color-border)] bg-[var(--color-cream)]/30 pl-10 pr-3 text-sm placeholder-[var(--color-text-subtle)] outline-none focus:border-[var(--color-gold-500)] transition"
                />
              </div>

              <div className="flex items-center gap-3 w-full sm:w-auto justify-end">
                <Button onClick={() => setShowAddBatchModal(true)} className="gap-2 shrink-0 bg-[var(--color-green-700)] hover:bg-[var(--color-green-800)] text-white border-0">
                  <Plus className="w-4 h-4" /> Nhập lô hàng mới
                </Button>
              </div>
            </div>

            {/* Batches Table */}
            {batchesLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-14 bg-white border border-[var(--color-border)] rounded-xl animate-pulse" />
                ))}
              </div>
            ) : batches.length === 0 ? (
              <div className="text-center py-20 bg-white rounded-2xl border border-[var(--color-border)]">
                <div className="text-6xl mb-4">📦</div>
                <h3 className="text-lg font-bold text-[var(--color-brown-900)] mb-1" style={{ fontFamily: 'var(--font-display)' }}>
                  Chưa có lô hàng nào
                </h3>
                <p className="text-xs text-[var(--color-text-muted)] mb-6 max-w-xs mx-auto">
                  Hãy nhập lô hàng đầu tiên để thêm số lượng tồn kho khả dụng cho sản phẩm của bạn.
                </p>
                <Button onClick={() => setShowAddBatchModal(true)} className="gap-2">
                  <Plus className="w-4 h-4" /> Nhập lô hàng
                </Button>
              </div>
            ) : (
              <>
                <div className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden shadow-sm">
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm text-left text-[var(--color-text)]">
                      <thead className="text-[10px] uppercase font-bold text-[var(--color-text-muted)] border-b border-[var(--color-border)] bg-[var(--color-cream)]/20">
                        <tr>
                          <th className="px-6 py-4">Mã lô</th>
                          <th className="px-6 py-4">Sản phẩm</th>
                          <th className="px-6 py-4 text-center">Ban đầu</th>
                          <th className="px-6 py-4 text-center">Còn lại</th>
                          <th className="px-6 py-4">Ngày thu hoạch</th>
                          <th className="px-6 py-4">Hạn sử dụng</th>
                          <th className="px-6 py-4">Trạng thái</th>
                          <th className="px-6 py-4 text-right">Thao tác</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[var(--color-border)]">
                        {batches.map((batch) => {
                          const status = batch.status
                          const isPending = status === 'PENDING_APPROVAL'
                          const isRejected = status === 'REJECTED'
                          const isAvailable = status === 'AVAILABLE'
                          const isSoldOut = status === 'SOLD_OUT' || batch.remainingQuantity === 0
                          const isExpired = status === 'EXPIRED'
                          return (
                            <tr key={batch.id} className="hover:bg-[var(--color-cream)]/10 transition">
                              <td className="px-6 py-4 font-mono font-bold text-xs text-[var(--color-brown-900)]">
                                {batch.batchCode}
                              </td>
                              <td className="px-6 py-4 font-medium text-[var(--color-brown-900)]">
                                {batch.productName}
                              </td>
                              <td className="px-6 py-4 text-center">{batch.quantity.toLocaleString('vi-VN')}</td>
                              <td className="px-6 py-4 text-center font-bold">
                                <span className={isSoldOut ? 'text-red-500' : batch.remainingQuantity < 15 ? 'text-amber-500' : ''}>
                                  {batch.remainingQuantity.toLocaleString('vi-VN')}
                                </span>
                              </td>
                              <td className="px-6 py-4 text-xs text-[var(--color-text-muted)]">{batch.harvestDate}</td>
                              <td className="px-6 py-4 text-xs font-medium">
                                <span className={isExpired ? 'text-red-500' : ''}>{batch.expiryDate}</span>
                              </td>
                              <td className="px-6 py-4">
                                {isPending ? (
                                  <Badge variant="warning" size="sm">Chờ duyệt</Badge>
                                ) : isRejected ? (
                                  <Badge variant="danger" size="sm">Từ chối</Badge>
                                ) : isExpired ? (
                                  <Badge variant="danger" size="sm">Hết hạn</Badge>
                                ) : isSoldOut ? (
                                  <Badge variant="muted" size="sm">Hết hàng</Badge>
                                ) : isAvailable ? (
                                  <Badge variant="success" size="sm">Đã duyệt</Badge>
                                ) : (
                                  <Badge variant="muted" size="sm">{status}</Badge>
                                )}
                                {isRejected && batch.rejectionReason && (
                                  <div className="mt-2 text-[10px] text-red-600">
                                    <span className="font-semibold">Lý do:</span> {batch.rejectionReason}
                                  </div>
                                )}
                              </td>
                              <td className="px-6 py-4 text-right">
                                <div className="flex items-center justify-end gap-2">
                                  <Button
                                    variant="outline"
                                    size="xs"
                                    disabled={!isAvailable}
                                    onClick={() => { setSelectedBatch(batch); setAdjustQty(0); setAdjustReason(''); setShowAdjustModal(true) }}
                                  >
                                    Điều chỉnh
                                  </Button>
                                  {isRejected && (
                                    <Button
                                      size="xs"
                                      onClick={() => resubmitBatchMutation.mutate({ batchId: batch.id })}
                                      loading={resubmitBatchMutation.isPending}
                                      className="bg-[var(--color-gold-500)] hover:bg-[var(--color-gold-600)] text-[var(--color-brown-900)] border-0"
                                    >
                                      Gửi duyệt lại
                                    </Button>
                                  )}
                                  <Button
                                    variant="secondary"
                                    size="xs"
                                    onClick={() => { setSelectedBatch(batch); setShowHistoryModal(true) }}
                                    className="gap-1"
                                  >
                                    Lịch sử
                                  </Button>
                                </div>
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* Pagination */}
                {batchesPageRes?.data && batchesPageRes.data.totalPages > 1 && (
                  <div className="flex justify-center mt-8 gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={batchPage === 0}
                      onClick={() => setBatchPage(p => p - 1)}
                    >
                      ← Trước
                    </Button>
                    <span className="text-xs flex items-center px-4 font-semibold text-[var(--color-text)]">
                      Trang {batchPage + 1} / {batchesPageRes.data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={batchPage >= batchesPageRes.data.totalPages - 1}
                      onClick={() => setBatchPage(p => p + 1)}
                    >
                      Sau →
                    </Button>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>

      {/* ─── MODAL: PRODUCT DETAILS ─────────────────── */}
      <AnimatePresence>
        {detailProductTarget && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setDetailProductTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-2xl bg-white rounded-2xl shadow-2xl overflow-hidden max-h-[90vh] flex flex-col"
            >
              {/* Header */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)] bg-[var(--color-cream)]/20">
                <h3 className="font-bold text-[var(--color-brown-900)] font-display">Chi tiết sản phẩm</h3>
                <button onClick={() => setDetailProductTarget(null)} className="p-1 rounded-lg text-[var(--color-text-muted)] hover:bg-slate-100">
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Scrollable Content */}
              <div className="p-6 space-y-6 overflow-y-auto flex-1">
                {/* Images */}
                <div className="flex gap-3 overflow-x-auto pb-2">
                  <div className="relative w-28 h-28 shrink-0 rounded-xl overflow-hidden border border-[var(--color-border)]">
                    <Image src={detailProductTarget.thumbnail || '/placeholder-product.jpg'} alt={detailProductTarget.name} fill className="object-cover" />
                  </div>
                  {detailProductTarget.imageUrls?.map((url: string, i: number) => (
                    <div key={i} className="relative w-28 h-28 shrink-0 rounded-xl overflow-hidden border border-[var(--color-border)]">
                      <Image src={url} alt={`img-${i}`} fill className="object-cover" />
                    </div>
                  ))}
                </div>

                {/* Specs Grid */}
                <div className="grid grid-cols-2 gap-4 bg-[var(--color-cream)]/20 rounded-2xl p-5 text-sm">
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Tên sản phẩm</p>
                    <p className="font-bold mt-1 text-[var(--color-brown-900)]">{detailProductTarget.name}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Danh mục</p>
                    <p className="font-semibold mt-1 text-[var(--color-text)]">{detailProductTarget.categoryName}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Giá bán công bố</p>
                    <p className="font-bold mt-1 text-[var(--color-text)]">{formatPrice(detailProductTarget.price)}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Giá khuyến mãi</p>
                    <p className="font-bold mt-1 text-[var(--color-green-700)]">
                      {detailProductTarget.discountPrice ? formatPrice(detailProductTarget.discountPrice) : '—'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Tổng lượng trong kho</p>
                    <p className="font-bold mt-1 text-[var(--color-brown-900)]">{detailProductTarget.totalStock}</p>
                  </div>
                  <div>
                    <p className="text-xs text-[var(--color-text-muted)]">Xuất xứ</p>
                    <p className="font-semibold mt-1 text-[var(--color-text)]">{detailProductTarget.origin || 'Việt Nam'}</p>
                  </div>
                  {detailProductTarget.weight && (
                    <div>
                      <p className="text-xs text-[var(--color-text-muted)]">Khối lượng đóng gói</p>
                      <p className="font-semibold mt-1 text-[var(--color-text)]">{detailProductTarget.weight}g</p>
                    </div>
                  )}
                  {detailProductTarget.calories && (
                    <div>
                      <p className="text-xs text-[var(--color-text-muted)]">Năng lượng dinh dưỡng</p>
                      <p className="font-semibold mt-1 text-[var(--color-text)]">{detailProductTarget.calories} kcal</p>
                    </div>
                  )}
                  <div className="col-span-2">
                    <p className="text-xs text-[var(--color-text-muted)]">Phương pháp canh tác</p>
                    <p className="font-bold mt-1 text-[var(--color-green-700)]">
                      {detailProductTarget.isOrganic ? '🌿 Hữu cơ (Organic)' : 'Tiêu chuẩn an toàn'}
                    </p>
                  </div>
                </div>

                {/* Description */}
                <div>
                  <p className="text-xs font-semibold text-[var(--color-text-muted)] uppercase mb-2">Mô tả sản phẩm</p>
                  <p className="text-sm leading-relaxed text-[var(--color-text)] bg-slate-50 p-4 rounded-xl border border-slate-100">
                    {detailProductTarget.description || 'Chưa cung cấp mô tả chi tiết cho sản phẩm.'}
                  </p>
                </div>
              </div>

              {/* Footer */}
              <div className="flex justify-end gap-3 px-6 py-4 border-t border-[var(--color-border)] bg-slate-50">
                <Button variant="secondary" onClick={() => setDetailProductTarget(null)}>Đóng</Button>
                <Button onClick={() => { setDetailProductTarget(null); setEditProductTarget(detailProductTarget); setShowProductForm(true) }} className="gap-2">
                  <Pencil className="w-4 h-4" /> Chỉnh sửa
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ─── MODAL: PRODUCT CREATE/EDIT (DRAWER) ────── */}
      <AnimatePresence>
        {showProductForm && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-end bg-black/50 backdrop-blur-sm"
            onClick={() => { setShowProductForm(false); setEditProductTarget(null); reset() }}
          >
            <motion.div
              initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }}
              transition={{ type: 'spring', stiffness: 300, damping: 30 }}
              onClick={(e) => e.stopPropagation()}
              className="h-full w-full max-w-lg overflow-y-auto border-l border-[var(--color-border)] bg-white p-6 shadow-2xl flex flex-col justify-between"
            >
              {/* Drawer Header */}
              <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-4 mb-6">
                <h2 className="text-lg font-bold text-[var(--color-brown-900)] font-display">
                  {editProductTarget ? '✏️ Sửa sản phẩm' : '🌱 Thêm sản phẩm mới'}
                </h2>
                <button
                  onClick={() => { setShowProductForm(false); setEditProductTarget(null); reset() }}
                  className="p-1 rounded-lg text-[var(--color-text-muted)] hover:bg-slate-100"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Form Content */}
              <form onSubmit={handleSubmit(handleCreateProduct)} className="space-y-4 flex-1 overflow-y-auto pr-1">
                <Input label="Tên sản phẩm *" error={errors.name?.message} {...register('name')} placeholder="VD: Dứa Mật Cầu Đúc" />

                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Danh mục *</label>
                  <select
                    {...register('categoryId')}
                    className="h-11 w-full rounded-xl border border-[var(--color-border)] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                  >
                    <option value="">— Chọn danh mục sản phẩm —</option>
                    {categoryOptions.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.prefix}{c.name}
                      </option>
                    ))}
                  </select>
                  {errors.categoryId && <p className="text-xs text-red-500">{errors.categoryId.message}</p>}
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Input label="Giá bán (VND) *" type="number" error={errors.price?.message} {...register('price')} placeholder="50000" />
                  <Input label="Giá khuyến mãi (VND)" type="number" {...register('discountPrice')} placeholder="45000" />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Input label="Khối lượng đóng gói (g)" type="number" {...register('weight')} placeholder="1000" />
                  <Input label="Calories (kcal)" type="number" {...register('calories')} placeholder="50" />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Input label="Xuất xứ" {...register('origin')} placeholder="Tiền Giang" />
                  <Input label="Thương hiệu" {...register('brand')} placeholder="Pineapple Farm" />
                </div>

                <div className="flex items-center gap-2 py-2">
                  <input type="checkbox" id="isOrganic" {...register('isOrganic')} className="rounded text-[var(--color-green-700)] focus:ring-[var(--color-green-700)] h-4 w-4" />
                  <label htmlFor="isOrganic" className="text-sm font-semibold text-[var(--color-brown-900)] flex items-center gap-1.5 cursor-pointer">
                    <Leaf className="w-4 h-4 text-[var(--color-green-600)]" /> Sản phẩm Hữu cơ (Organic)
                  </label>
                </div>

                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Mô tả sản phẩm</label>
                  <textarea
                    {...register('description')}
                    rows={4}
                    placeholder="Mô tả chi tiết sản phẩm, phương pháp chăm sóc..."
                    className="w-full rounded-xl border border-[var(--color-border)] bg-white px-3 py-2.5 text-sm placeholder-[var(--color-text-subtle)] outline-none focus:border-[var(--color-gold-500)] resize-none"
                  />
                </div>

                {/* Upload Section */}
                <div className="space-y-3 pt-2">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">
                    Hình ảnh sản phẩm * (Tải lên hàng loạt, nhấp vào ảnh để chọn làm thumbnail)
                  </label>

                  <div className="grid grid-cols-4 gap-2">
                    {(watch('imageUrls') || []).map((url, i) => {
                      const isThumbnail = watch('thumbnail') === url
                      return (
                        <div
                          key={url + i}
                          onClick={() => setValue('thumbnail', url)}
                          className={`relative aspect-square rounded-xl overflow-hidden border-2 cursor-pointer transition ${
                            isThumbnail ? 'border-[var(--color-green-700)] ring-2 ring-[var(--color-green-700)]/20' : 'border-[var(--color-border)] hover:border-slate-400'
                          }`}
                        >
                          <Image src={url} alt={`preview-${i}`} fill className="object-cover" />
                          <button
                            type="button"
                            onClick={(e) => { e.stopPropagation(); handleRemoveImage(url) }}
                            className="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-black"
                          >
                            <X className="w-3 h-3" />
                          </button>
                          {isThumbnail && (
                            <span className="absolute bottom-0 inset-x-0 bg-[var(--color-green-700)] text-white text-[9px] font-bold text-center py-0.5">
                              Ảnh bìa
                            </span>
                          )}
                        </div>
                      )
                    })}

                    <label className={`aspect-square rounded-xl border-2 border-dashed border-[var(--color-border)] hover:border-[var(--color-green-600)] bg-slate-50 flex flex-col items-center justify-center cursor-pointer transition ${uploadingImages ? 'opacity-50 pointer-events-none' : ''}`}>
                      <Upload className="w-5 h-5 text-[var(--color-text-muted)] mb-1" />
                      <span className="text-[10px] text-[var(--color-text-muted)]">Upload</span>
                      <input type="file" accept="image/*" multiple className="sr-only" onChange={handleImageUpload} />
                    </label>
                  </div>
                  {errors.thumbnail && <p className="text-xs text-red-500">{errors.thumbnail.message}</p>}
                </div>

                <div className="rounded-xl bg-amber-50 border border-amber-200 p-4 text-xs text-amber-700 flex items-start gap-2 mt-4">
                  <AlertTriangle className="w-4 h-4 mt-0.5 shrink-0" />
                  <p>Khi Farmer chỉnh sửa hoặc thêm sản phẩm, trạng thái sẽ tự động đổi về <strong>Chờ duyệt</strong> và ẩn khỏi cửa hàng công khai cho đến khi Admin phê duyệt.</p>
                </div>

                <div className="flex gap-3 pt-6 border-t border-[var(--color-border)]">
                  <Button type="button" variant="secondary" className="flex-1" onClick={() => { setShowProductForm(false); setEditProductTarget(null); reset() }}>
                    Hủy
                  </Button>
                  <Button type="submit" loading={createProductMutation.isPending || updateProductMutation.isPending} className="flex-1 gap-2">
                    <Save className="w-4 h-4" /> Lưu thông tin
                  </Button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ─── MODAL: PRODUCT DELETE CONFIRMATION ────── */}
      <AnimatePresence>
        {deleteProductTarget && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setDeleteProductTarget(null)}
          >
            <motion.div
              initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm bg-white rounded-2xl shadow-2xl p-6"
            >
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center text-red-500">
                  <Trash2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-[var(--color-brown-900)]">Xoá sản phẩm?</h3>
                  <p className="text-xs text-[var(--color-text-muted)]">Ẩn vĩnh viễn khỏi cửa hàng</p>
                </div>
              </div>
              <p className="text-sm text-[var(--color-text-muted)] mb-6">
                Bạn có chắc chắn muốn xoá sản phẩm <span className="font-semibold text-[var(--color-brown-900)]">{deleteProductTarget.name}</span>?
              </p>
              <div className="flex gap-3">
                <Button variant="secondary" className="flex-1" onClick={() => setDeleteProductTarget(null)}>Hủy</Button>
                <button
                  onClick={() => deleteProductMutation.mutate(deleteProductTarget.id)}
                  disabled={deleteProductMutation.isPending}
                  className="flex-1 rounded-xl bg-red-500 hover:bg-red-600 text-white font-medium text-sm py-2 disabled:opacity-50 transition"
                >
                  {deleteProductMutation.isPending ? 'Đang xoá...' : 'Xoá sản phẩm'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ─── MODAL: ADD BATCH ───────────────────────── */}
      <AnimatePresence>
        {showAddBatchModal && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setShowAddBatchModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden"
            >
              <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)]">
                <h3 className="font-bold text-[var(--color-brown-900)] font-display">📦 Nhập lô hàng mới</h3>
                <button onClick={() => setShowAddBatchModal(false)} className="p-1 rounded-lg text-[var(--color-text-muted)] hover:bg-slate-100">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={submitBatchForm} className="p-6 space-y-4">
                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Chọn sản phẩm *</label>
                  <select
                    value={newBatchProduct}
                    onChange={(e) => setNewBatchProduct(Number(e.target.value))}
                    className="h-11 w-full rounded-xl border border-[var(--color-border)] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                  >
                    <option value="">— Chọn sản phẩm trong lô —</option>
                    {farmerProducts.map((p) => (
                      <option key={p.id} value={p.id}>{p.name}</option>
                    ))}
                  </select>
                </div>

                <Input
                  label="Mã lô hàng (Batch Code) *"
                  value={newBatchCode}
                  onChange={(e) => setNewBatchCode(e.target.value)}
                  placeholder="VD: BATCH-MIT-001"
                />

                <Input
                  label="Số lượng nhập hàng *"
                  type="number"
                  value={newBatchQty || ''}
                  onChange={(e) => setNewBatchQty(Number(e.target.value))}
                  placeholder="VD: 50"
                />

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Ngày thu hoạch *</label>
                    <input
                      type="date"
                      value={newBatchHarvestDate}
                      onChange={(e) => setNewBatchHarvestDate(e.target.value)}
                      className="w-full h-11 px-4 border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                    />
                  </div>
                  <div className="space-y-1">
                    <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Ngày hết hạn *</label>
                    <input
                      type="date"
                      value={newBatchExpiryDate}
                      onChange={(e) => setNewBatchExpiryDate(e.target.value)}
                      className="w-full h-11 px-4 border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                    />
                  </div>
                </div>

                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Độ ngọt / Chất lượng (1-5)</label>
                  <select
                    value={newBatchSweetness}
                    onChange={(e) => setNewBatchSweetness(Number(e.target.value))}
                    className="h-11 w-full rounded-xl border border-[var(--color-border)] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                  >
                    <option value="0">Không xác định</option>
                    <option value="1">1 sao (Kém ngọt)</option>
                    <option value="2">2 sao (Vừa ngọt)</option>
                    <option value="3">3 sao (Khá ngon)</option>
                    <option value="4">4 sao (Rất ngọt)</option>
                    <option value="5">5 sao (Thượng hạng)</option>
                  </select>
                </div>

                <div className="flex gap-3 pt-4 border-t border-[var(--color-border)]">
                  <Button type="button" variant="secondary" className="flex-1" onClick={() => setShowAddBatchModal(false)}>Hủy</Button>
                  <Button type="submit" loading={addBatchMutation.isPending} className="flex-1">Nhập lô hàng</Button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ─── MODAL: ADJUST BATCH ────────────────────── */}
      <AnimatePresence>
        {showAdjustModal && selectedBatch && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setShowAdjustModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm bg-white rounded-2xl shadow-2xl overflow-hidden"
            >
              <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)]">
                <h3 className="font-bold text-[var(--color-brown-900)] font-display">⚙️ Điều chỉnh tồn kho</h3>
                <button onClick={() => setShowAdjustModal(false)} className="p-1 rounded-lg text-[var(--color-text-muted)] hover:bg-slate-100">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={submitAdjustment} className="p-6 space-y-4">
                <div className="bg-[var(--color-cream)]/20 p-4 rounded-xl border border-[var(--color-border)] text-xs space-y-1">
                  <p className="text-[var(--color-text-muted)]">Mã lô hàng: <span className="font-mono font-bold text-[var(--color-brown-900)]">{selectedBatch.batchCode}</span></p>
                  <p className="text-[var(--color-text-muted)]">Sản phẩm: <span className="font-bold text-[var(--color-brown-900)]">{selectedBatch.productName}</span></p>
                  <p className="text-[var(--color-text-muted)]">Tồn kho hiện tại: <span className="font-bold text-green-700">{selectedBatch.remainingQuantity}</span></p>
                </div>

                <Input
                  label="Số lượng thay đổi (+/-) *"
                  type="number"
                  value={adjustQty || ''}
                  onChange={(e) => setAdjustQty(Number(e.target.value))}
                  placeholder="Tăng nhập: +10 | Hao hụt: -5"
                  hint="Sử dụng số dương để thêm và số âm để giảm tồn kho."
                />

                <div className="space-y-1">
                  <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase">Lý do điều chỉnh *</label>
                  <input
                    type="text"
                    value={adjustReason}
                    onChange={(e) => setAdjustReason(e.target.value)}
                    placeholder="VD: Khấu hao thất thoát, Khách trả..."
                    className="w-full h-11 px-4 border border-[var(--color-border)] rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)]"
                  />
                </div>

                <div className="flex gap-3 pt-4 border-t border-[var(--color-border)]">
                  <Button type="button" variant="secondary" className="flex-1" onClick={() => setShowAdjustModal(false)}>Hủy</Button>
                  <Button type="submit" loading={adjustBatchMutation.isPending} className="flex-1">Xác nhận</Button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ─── MODAL: BATCH ADJUSTMENTS HISTORY ────────── */}
      <AnimatePresence>
        {showHistoryModal && selectedBatch && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => { setShowHistoryModal(false); setSelectedBatch(null) }}
          >
            <motion.div
              initial={{ scale: 0.95, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-xl bg-white rounded-2xl shadow-2xl overflow-hidden max-h-[80vh] flex flex-col"
            >
              <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)] bg-[var(--color-cream)]/20">
                <div>
                  <h3 className="font-bold text-[var(--color-brown-900)] font-display">📜 Lịch sử điều chỉnh lô</h3>
                  <p className="text-[10px] text-[var(--color-text-muted)] font-mono mt-0.5">Lô hàng: {selectedBatch.batchCode}</p>
                </div>
                <button onClick={() => { setShowHistoryModal(false); setSelectedBatch(null) }} className="p-1 rounded-lg text-[var(--color-text-muted)] hover:bg-slate-100">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="p-6 overflow-y-auto flex-1">
                {historyLoading ? (
                  <div className="space-y-3 py-6">
                    <div className="h-6 bg-slate-100 rounded animate-pulse" />
                    <div className="h-6 bg-slate-100 rounded animate-pulse" />
                  </div>
                ) : adjustments.length === 0 ? (
                  <div className="text-center py-10 text-[var(--color-text-muted)] text-sm">
                    Lô hàng này chưa ghi nhận bất kỳ sự điều chỉnh stock thủ công nào.
                  </div>
                ) : (
                  <div className="relative border-l border-[var(--color-border)] ml-3 pl-6 space-y-6 py-2">
                    {adjustments.map((adj) => {
                      const isPositive = adj.adjustmentQty > 0
                      return (
                        <div key={adj.id} className="relative">
                          {/* Dot */}
                          <div className={`absolute -left-[30px] top-1.5 w-3 h-3 rounded-full border-2 border-white ${isPositive ? 'bg-green-500' : 'bg-red-500'}`} />
                          
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-xs">
                              <span className={`font-bold ${isPositive ? 'text-green-600' : 'text-red-500'}`}>
                                {isPositive ? '+' : ''}{adj.adjustmentQty} sản phẩm
                              </span>
                              <span className="text-[10px] text-[var(--color-text-muted)]">
                                {new Date(adj.createdAt).toLocaleString('vi-VN')}
                              </span>
                            </div>
                            <p className="text-sm font-semibold text-[var(--color-brown-900)]">{adj.reason}</p>
                            <p className="text-[10px] text-[var(--color-text-muted)]">
                              Tồn kho: {adj.qtyBefore} → {adj.qtyAfter} | Thực hiện bởi: <span className="font-semibold text-[var(--color-text)]">{adj.adjustedByName}</span>
                            </p>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>

              <div className="px-6 py-4 bg-slate-50 border-t border-[var(--color-border)] flex justify-end">
                <Button variant="secondary" onClick={() => { setShowHistoryModal(false); setSelectedBatch(null) }}>Đóng</Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
