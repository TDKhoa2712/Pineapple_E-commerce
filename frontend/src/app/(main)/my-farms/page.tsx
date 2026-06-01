'use client'

import { useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { motion, AnimatePresence } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import {
  Plus, Edit2, Trash2, Upload, X, AlertCircle, CheckCircle,
  Clock, XCircle, MapPin, FileText, Award, ImageIcon, ChevronRight,
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import {
  useMyFarms, useCreateFarm, useUpdateFarm, useDeleteFarm, useUploadFarmImage,
} from '@/hooks'
import { Button } from '@/components/ui'
import { StatusBadge } from '@/components/shared/status-badge'
import type { FarmResponse, FarmStatus, CreateFarmRequest } from '@/types'

const farmSchema = z.object({
  name: z.string().min(2, 'Tên trang trại ít nhất 2 ký tự').max(200),
  location: z.string().max(300).optional(),
  description: z.string().optional(),
  certificate: z.string().max(500).optional(),
})
type FarmForm = z.infer<typeof farmSchema>

const DEFAULT_STATUS_INFO = {
  icon: <AlertCircle className="w-4 h-4" />,
  color: 'text-slate-600',
  bg: 'bg-slate-50 border-slate-200',
  text: 'Trạng thái trang trại tạm thời.',
}

const STATUS_INFO: Record<FarmStatus, { icon: React.ReactNode; color: string; bg: string; text: string }> = {
  PENDING_APPROVAL: {
    icon: <Clock className="w-4 h-4" />,
    color: 'text-amber-600',
    bg: 'bg-amber-50 border-amber-200',
    text: 'Đang chờ Admin xét duyệt. Chúng tôi sẽ thông báo qua email khi có kết quả.',
  },
  ACTIVE: {
    icon: <CheckCircle className="w-4 h-4" />,
    color: 'text-green-600',
    bg: 'bg-green-50 border-green-200',
    text: 'Nông trại đang hoạt động và hiển thị công khai trên hệ thống.',
  },
  REJECTED: {
    icon: <XCircle className="w-4 h-4" />,
    color: 'text-red-600',
    bg: 'bg-red-50 border-red-200',
    text: '',
  },
  INACTIVE: {
    icon: <AlertCircle className="w-4 h-4" />,
    color: 'text-slate-600',
    bg: 'bg-slate-50 border-slate-200',
    text: 'Nông trại đang tạm ngưng hoạt động.',
  },
  PENDING_DEACTIVATION: {
    icon: <Clock className="w-4 h-4" />,
    color: 'text-amber-600',
    bg: 'bg-amber-50 border-amber-200',
    text: 'Bạn đã gửi yêu cầu ngừng hoạt động. Farm vẫn hoạt động cho đến khi Admin duyệt.',
  },
  PENDING_REACTIVATION: {
    icon: <Clock className="w-4 h-4" />,
    color: 'text-amber-600',
    bg: 'bg-amber-50 border-amber-200',
    text: 'Farm đang chờ kích hoạt lại bởi Admin.',
  },
}

export default function MyFarmsPage() {
  const router = useRouter()
  const { user } = useAuthStore()
  const isFarmer = user?.roles?.some((r) => r === 'ROLE_FARMER' || r === 'ROLE_ADMIN')

  const { data: farmsRes, isLoading } = useMyFarms()
  const createFarm = useCreateFarm()
  const updateFarm = useUpdateFarm()
  const deleteFarm = useDeleteFarm()
  const uploadImage = useUploadFarmImage()

  const [formOpen, setFormOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<FarmResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<FarmResponse | null>(null)
  const [uploadTarget, setUploadTarget] = useState<FarmResponse | null>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const farms = farmsRes?.data ?? []

  const {
    register, handleSubmit, reset, setValue,
    formState: { errors, isSubmitting },
  } = useForm<FarmForm>({ resolver: zodResolver(farmSchema) })

  const openCreate = () => {
    setEditTarget(null)
    reset({ name: '', location: '', description: '', certificate: '' })
    setFormOpen(true)
  }

  const openEdit = (farm: FarmResponse) => {
    setEditTarget(farm)
    reset({
      name: farm.name,
      location: farm.location ?? '',
      description: farm.description ?? '',
      certificate: farm.certificate ?? '',
    })
    setFormOpen(true)
  }

  const onSubmit = async (data: FarmForm) => {
    const payload: CreateFarmRequest = {
      name: data.name,
      location: data.location || undefined,
      description: data.description || undefined,
      certificate: data.certificate || undefined,
    }
    if (editTarget) {
      await updateFarm.mutateAsync({ id: editTarget.id, data: payload })
    } else {
      await createFarm.mutateAsync(payload)
    }
    setFormOpen(false)
    reset()
  }

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file || !uploadTarget) return
    await uploadImage.mutateAsync({ id: uploadTarget.id, file })
    setUploadTarget(null)
    e.target.value = ''
  }



  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      {/* Header */}
      <div className="bg-[var(--color-green-700)] relative overflow-hidden">
        <div className="container-main py-12 relative z-10">
          <div className="flex items-end justify-between gap-4">
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
              <p className="text-[var(--color-green-100)] text-sm mb-2 flex items-center gap-2">
                <span onClick={() => router.push('/profile')} className="hover:underline cursor-pointer">Tài khoản</span>
                <ChevronRight className="w-3 h-3" />
                <span>Nông trại của tôi</span>
              </p>
              <h1 className="text-3xl font-bold text-white" style={{ fontFamily: 'var(--font-display)' }}>
                🌾 Nông trại của tôi
              </h1>
              <p className="text-[var(--color-green-100)] text-sm mt-1">
                {farms.length} nông trại đã đăng ký
              </p>
            </motion.div>
            <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }}>
              <Button onClick={openCreate} className="gap-2">
                <Plus className="w-4 h-4" /> Đăng ký nông trại mới
              </Button>
            </motion.div>
          </div>
        </div>
        <div className="absolute -right-20 -top-20 w-80 h-80 rounded-full bg-white/5" />
      </div>

      <div className="container-main py-10">
        {isLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {[1, 2].map((i) => (
              <div key={i} className="h-48 rounded-2xl bg-white border border-[var(--color-border)] animate-pulse" />
            ))}
          </div>
        ) : farms.length === 0 ? (
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
            className="text-center py-20 bg-white rounded-2xl border border-[var(--color-border)]">
            <div className="text-6xl mb-4">🌱</div>
            <h2 className="text-xl font-bold text-[var(--color-brown-900)] mb-2"
              style={{ fontFamily: 'var(--font-display)' }}>
              Chưa có nông trại nào
            </h2>
            <p className="text-sm text-[var(--color-text-muted)] mb-6 max-w-sm mx-auto">
              Đăng ký nông trại của bạn để bắt đầu bán sản phẩm trên Pineapple.
            </p>
            <Button onClick={openCreate} className="gap-2">
              <Plus className="w-4 h-4" /> Đăng ký ngay
            </Button>
          </motion.div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {farms.map((farm, i) => {
              const statusInfo = STATUS_INFO[farm.status as FarmStatus] ?? DEFAULT_STATUS_INFO
              return (
                <motion.div key={farm.id}
                  initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.06 }}
                  className="bg-white rounded-2xl border border-[var(--color-border)] overflow-hidden shadow-sm hover:shadow-md transition-shadow">
                  {/* Farm image */}
                  <div className="relative h-36 bg-[var(--color-green-50)] group">
                    {farm.imageUrl ? (
                      <Image src={farm.imageUrl} alt={farm.name} fill className="object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-4xl">🌾</div>
                    )}
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors" />
                    <button
                      onClick={() => { setUploadTarget(farm); fileRef.current?.click() }}
                      className="absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex items-center gap-1.5 bg-white/90 backdrop-blur-sm rounded-lg px-2.5 py-1.5 text-xs font-medium text-[var(--color-brown-900)] shadow">
                      <Upload className="w-3 h-3" /> Đổi ảnh
                    </button>
                    <div className="absolute top-2 right-2">
                      <StatusBadge status={farm.status as FarmStatus} />
                    </div>
                  </div>

                  <div className="p-5">
                    <h3 className="font-bold text-[var(--color-brown-900)] text-lg mb-0.5"
                      style={{ fontFamily: 'var(--font-display)' }}>
                      {farm.name}
                    </h3>
                    {farm.location && (
                      <p className="flex items-center gap-1 text-xs text-[var(--color-text-muted)] mb-3">
                        <MapPin className="w-3 h-3" /> {farm.location}
                      </p>
                    )}

                    {/* Status banner */}
                    <div className={`flex items-start gap-2 rounded-lg border p-3 mb-4 text-xs ${statusInfo.bg} ${statusInfo.color}`}>
                      <span className="mt-0.5 shrink-0">{statusInfo.icon}</span>
                      <span>
                        {farm.status === 'REJECTED'
                          ? <>Nông trại bị từ chối. {farm.rejectionReason && <><br /><strong>Lý do:</strong> {farm.rejectionReason}</>}</>
                          : statusInfo.text}
                      </span>
                    </div>

                    {/* Details row */}
                    <div className="flex flex-wrap gap-3 mb-4 text-xs text-[var(--color-text-muted)]">
                      {farm.certificate && (
                        <span className="flex items-center gap-1">
                          <Award className="w-3 h-3 text-[var(--color-gold-500)]" /> {farm.certificate}
                        </span>
                      )}
                      {farm.description && (
                        <span className="flex items-center gap-1 line-clamp-1 max-w-[200px]">
                          <FileText className="w-3 h-3 shrink-0" /> {farm.description}
                        </span>
                      )}
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-2 pt-3 border-t border-[var(--color-border)]">
                      {farm.status === 'ACTIVE' ? (
                        <>
                          <Button size="sm" className="gap-1.5 flex-1 bg-[var(--color-green-700)] hover:bg-[var(--color-green-600)] text-white"
                            onClick={() => router.push(`/my-farms/${farm.id}/products`)}>
                            Quản lý sản phẩm
                          </Button>
                          <Button variant="secondary" size="sm" className="gap-1.5 px-2" onClick={() => openEdit(farm)}>
                            <Edit2 className="w-3.5 h-3.5" />
                          </Button>
                          <Button variant="secondary" size="sm" className="gap-1.5 px-2"
                            onClick={() => router.push(`/farms/${farm.id}`)}>
                            Xem
                          </Button>
                        </>
                      ) : (
                        <Button variant="secondary" size="sm" className="gap-1.5 flex-1" onClick={() => openEdit(farm)}>
                          <Edit2 className="w-3.5 h-3.5" /> Chỉnh sửa
                        </Button>
                      )}
                      <button onClick={() => setDeleteTarget(farm)}
                        className="p-2 rounded-lg text-red-400 hover:bg-red-50 transition-colors shrink-0">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}
      </div>

      {/* Hidden file input for image upload */}
      <input ref={fileRef} type="file" accept="image/*" className="sr-only" onChange={handleImageChange} />

      {/* ── Farm Form Modal ────────────────────────── */}
      <AnimatePresence>
        {formOpen && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setFormOpen(false)}>
            <motion.div initial={{ scale: 0.95, y: 12 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 12 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-lg bg-white rounded-2xl shadow-2xl overflow-hidden">
              {/* Modal header */}
              <div className="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)]">
                <h2 className="text-lg font-bold text-[var(--color-brown-900)]"
                  style={{ fontFamily: 'var(--font-display)' }}>
                  {editTarget ? '✏️ Chỉnh sửa nông trại' : '🌱 Đăng ký nông trại mới'}
                </h2>
                <button onClick={() => setFormOpen(false)} className="p-1.5 rounded-lg text-[var(--color-text-muted)] hover:bg-[var(--color-cream)]">
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-5">
                {/* Name */}
                <div>
                  <label className="block text-sm font-medium text-[var(--color-brown-900)] mb-1.5">
                    Tên nông trại <span className="text-red-500">*</span>
                  </label>
                  <input {...register('name')}
                    placeholder="VD: Nông trại hữu cơ Đà Lạt"
                    className="w-full h-10 px-3 rounded-xl border border-[var(--color-border)] bg-white text-sm focus:outline-none focus:border-[var(--color-gold-500)] transition" />
                  {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name.message}</p>}
                </div>

                {/* Location */}
                <div>
                  <label className="block text-sm font-medium text-[var(--color-brown-900)] mb-1.5">
                    <MapPin className="w-3.5 h-3.5 inline mr-1" />Địa chỉ / Tỉnh thành
                  </label>
                  <input {...register('location')}
                    placeholder="VD: Đà Lạt, Lâm Đồng"
                    className="w-full h-10 px-3 rounded-xl border border-[var(--color-border)] bg-white text-sm focus:outline-none focus:border-[var(--color-gold-500)] transition" />
                </div>

                {/* Certificate */}
                <div>
                  <label className="block text-sm font-medium text-[var(--color-brown-900)] mb-1.5">
                    <Award className="w-3.5 h-3.5 inline mr-1" />Chứng nhận (tùy chọn)
                  </label>
                  <input {...register('certificate')}
                    placeholder="VD: VietGAP, Organic Vietnam..."
                    className="w-full h-10 px-3 rounded-xl border border-[var(--color-border)] bg-white text-sm focus:outline-none focus:border-[var(--color-gold-500)] transition" />
                </div>

                {/* Description */}
                <div>
                  <label className="block text-sm font-medium text-[var(--color-brown-900)] mb-1.5">
                    <FileText className="w-3.5 h-3.5 inline mr-1" />Mô tả
                  </label>
                  <textarea {...register('description')} rows={3}
                    placeholder="Giới thiệu về nông trại, phương pháp canh tác, sản phẩm chủ lực..."
                    className="w-full px-3 py-2.5 rounded-xl border border-[var(--color-border)] bg-white text-sm focus:outline-none focus:border-[var(--color-gold-500)] transition resize-none" />
                </div>

                {!editTarget && (
                  <div className="rounded-xl bg-amber-50 border border-amber-200 p-4 text-xs text-amber-700 flex items-start gap-2">
                    <Clock className="w-4 h-4 mt-0.5 shrink-0" />
                    <p>Sau khi gửi, nông trại sẽ ở trạng thái <strong>Chờ duyệt</strong>. Admin sẽ xem xét và phê duyệt trong vòng 1-3 ngày làm việc.</p>
                  </div>
                )}

                <div className="flex gap-3 pt-2">
                  <Button type="button" variant="secondary" className="flex-1" onClick={() => setFormOpen(false)}>
                    Hủy
                  </Button>
                  <Button type="submit" loading={isSubmitting || createFarm.isPending || updateFarm.isPending} className="flex-1">
                    {editTarget ? 'Lưu thay đổi' : 'Gửi đăng ký'}
                  </Button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ── Delete Confirm Modal ───────────────────── */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4"
            onClick={() => setDeleteTarget(null)}>
            <motion.div initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}
              onClick={(e) => e.stopPropagation()}
              className="w-full max-w-sm bg-white rounded-2xl shadow-2xl p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                  <Trash2 className="w-5 h-5 text-red-500" />
                </div>
                <div>
                  <h2 className="font-bold text-[var(--color-brown-900)]">Xóa nông trại?</h2>
                  <p className="text-xs text-[var(--color-text-muted)]">Hành động này không thể hoàn tác</p>
                </div>
              </div>
              <p className="text-sm text-[var(--color-text-muted)] mb-6">
                Bạn có chắc muốn xóa <span className="font-medium text-[var(--color-brown-900)]">{deleteTarget.name}</span>?
              </p>
              <div className="flex gap-3">
                <Button variant="secondary" className="flex-1" onClick={() => setDeleteTarget(null)}>Hủy</Button>
                <button onClick={() => { deleteFarm.mutate(deleteTarget.id); setDeleteTarget(null) }}
                  disabled={deleteFarm.isPending}
                  className="flex-1 rounded-xl bg-red-500 text-white text-sm font-medium py-2 hover:bg-red-600 disabled:opacity-50 transition">
                  {deleteFarm.isPending ? 'Đang xóa...' : 'Xóa nông trại'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
