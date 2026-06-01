'use client'

import { useState, useEffect } from 'react'
import { toast } from 'sonner'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion, AnimatePresence } from 'framer-motion'
import { Plus, Edit2, Trash2, Star } from 'lucide-react'
import { useAddresses, useCreateAddress, useUpdateAddress, useDeleteAddress, useSetDefaultAddress, useProvinces, useDistricts, useWards } from '@/hooks'
import { addressSchema, type AddressFormData } from '@/lib/validations'
import { Button, Input, Badge, Skeleton, EmptyState } from '@/components/ui'
import type { AddressResponse } from '@/types'

export default function AddressesPage() {
  const [editing, setEditing] = useState<AddressResponse | null>(null)
  const [showForm, setShowForm] = useState(false)

  const [selectedProvinceId, setSelectedProvinceId] = useState<string | null>(null)
  const [selectedDistrictId, setSelectedDistrictId] = useState<string | null>(null)
  const [selectedWardCode, setSelectedWardCode] = useState<string | null>(null)

  const { data: provincesRes } = useProvinces()
  const provinces = provincesRes?.data ?? []

  const { data: districtsRes } = useDistricts(selectedProvinceId)
  const districts = districtsRes?.data ?? []

  const { data: wardsRes } = useWards(selectedDistrictId)
  const wards = wardsRes?.data ?? []

  const { data: addrRes, isLoading } = useAddresses()
  const createAddress = useCreateAddress()
  const updateAddress = useUpdateAddress()
  const deleteAddress = useDeleteAddress()
  const setDefault = useSetDefaultAddress()

  const addresses = addrRes?.data ?? []

  // Auto-resolve GHN ID from names when editing
  useEffect(() => {
    if (!editing || !showForm) return;

    if (provinces.length > 0 && !selectedProvinceId) {
      const match = provinces.find((p: any) => p.name === editing.province);
      if (match) {
        setSelectedProvinceId(match.id);
      }
    }
  }, [provinces, editing, showForm, selectedProvinceId]);

  useEffect(() => {
    if (!editing || !showForm || !selectedProvinceId) return;

    if (districts.length > 0 && !selectedDistrictId) {
      const match = districts.find((d: any) => d.name === editing.district);
      if (match) {
        setSelectedDistrictId(match.id);
      }
    }
  }, [districts, editing, showForm, selectedProvinceId, selectedDistrictId]);

  useEffect(() => {
    if (!editing || !showForm || !selectedDistrictId) return;

    if (wards.length > 0 && !selectedWardCode) {
      const match = wards.find((w: any) => w.name === editing.ward);
      if (match) {
        setSelectedWardCode(match.id);
      }
    }
  }, [wards, editing, showForm, selectedDistrictId, selectedWardCode]);

  const selectClassName = "w-full h-11 px-4 bg-white border border-[var(--color-border)] rounded-xl text-sm text-[var(--color-text)] focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)] focus:ring-offset-0 transition-all duration-150 disabled:opacity-50 disabled:cursor-not-allowed";

  const { register, handleSubmit, reset, setValue, formState: { errors, isSubmitting } } =
    useForm<AddressFormData>({ resolver: zodResolver(addressSchema) })

  const openCreate = () => {
    setEditing(null)
    reset()
    setSelectedProvinceId(null)
    setSelectedDistrictId(null)
    setSelectedWardCode(null)
    setShowForm(true)
  }

  const openEdit = (addr: AddressResponse) => {
    setEditing(addr)
    setValue('receiverName', addr.receiverName)
    setValue('phone', addr.phone)
    setValue('province', addr.province)
    setValue('district', addr.district)
    setValue('ward', addr.ward)
    setValue('detail', addr.detail)
    setValue('isDefault', addr.isDefault)

    const ghnMeta = addr.carrierMetadata?.GHN
    if (ghnMeta) {
      setSelectedProvinceId(ghnMeta.provinceId || null)
      setSelectedDistrictId(ghnMeta.districtId || null)
      setSelectedWardCode(ghnMeta.wardCode || null)
    } else {
      setSelectedProvinceId(null)
      setSelectedDistrictId(null)
      setSelectedWardCode(null)
    }

    setShowForm(true)
  }

  const onSubmit = async (data: AddressFormData) => {
    if (!selectedProvinceId || !selectedDistrictId || !selectedWardCode) {
      toast.error('Vui lòng chọn đầy đủ Tỉnh/Thành, Quận/Huyện, Phường/Xã từ danh sách đối tác vận chuyển.')
      return
    }

    const payload = {
      ...data,
      carrierMetadata: {
        GHN: {
          provinceId: selectedProvinceId,
          districtId: selectedDistrictId,
          wardCode: selectedWardCode,
        }
      }
    }

    try {
      if (editing) {
        await updateAddress.mutateAsync({ id: editing.id, data: payload })
      } else {
        await createAddress.mutateAsync(payload)
      }
      setShowForm(false)
      reset()
      setEditing(null)
      setSelectedProvinceId(null)
      setSelectedDistrictId(null)
      setSelectedWardCode(null)
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Lưu địa chỉ thất bại. Vui lòng thử lại.')
    }
  }

  return (
    <div className="bg-[var(--color-cream)] min-h-screen">
      <div className="container-main py-10 max-w-3xl">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-[var(--color-brown-900)]"
            style={{ fontFamily: 'var(--font-display)' }}>
            Địa chỉ của tôi
          </h1>
          <Button variant="gold" size="sm" onClick={openCreate}>
            <Plus className="w-4 h-4" />
            Thêm địa chỉ
          </Button>
        </div>

        {/* Form */}
        <AnimatePresence>
          {showForm && (
            <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }}
              className="bg-white rounded-2xl border border-[var(--color-gold-500)] p-6 mb-6">
              <h2 className="font-bold text-[var(--color-brown-900)] mb-4">
                {editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới'}
              </h2>
              <form onSubmit={handleSubmit(onSubmit)}>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input label="Tên người nhận" error={errors.receiverName?.message} {...register('receiverName')} />
                  <Input label="Số điện thoại" error={errors.phone?.message} {...register('phone')} />
                  
                  <div className="space-y-1">
                    <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                      Tỉnh / Thành phố
                    </label>
                    <select
                      value={selectedProvinceId || ""}
                      onChange={(e) => {
                        const id = e.target.value;
                        const name = provinces.find(p => p.id === id)?.name || "";
                        setSelectedProvinceId(id || null);
                        setSelectedDistrictId(null);
                        setSelectedWardCode(null);
                        setValue('province', name);
                        setValue('district', '');
                        setValue('ward', '');
                      }}
                      className={selectClassName}
                    >
                      <option value="">Chọn Tỉnh / Thành phố</option>
                      {provinces.map((p) => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                    </select>
                    {errors.province?.message && <p className="text-xs text-red-500">{errors.province.message}</p>}
                  </div>

                  <div className="space-y-1">
                    <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                      Quận / Huyện
                    </label>
                    <select
                      value={selectedDistrictId || ""}
                      disabled={!selectedProvinceId}
                      onChange={(e) => {
                        const id = e.target.value;
                        const name = districts.find(d => d.id === id)?.name || "";
                        setSelectedDistrictId(id || null);
                        setSelectedWardCode(null);
                        setValue('district', name);
                        setValue('ward', '');
                      }}
                      className={selectClassName}
                    >
                      <option value="">Chọn Quận / Huyện</option>
                      {districts.map((d) => (
                        <option key={d.id} value={d.id}>{d.name}</option>
                      ))}
                    </select>
                    {errors.district?.message && <p className="text-xs text-red-500">{errors.district.message}</p>}
                  </div>

                  <div className="space-y-1">
                    <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                      Phường / Xã
                    </label>
                    <select
                      value={selectedWardCode || ""}
                      disabled={!selectedDistrictId}
                      onChange={(e) => {
                        const id = e.target.value;
                        const name = wards.find(w => w.id === id)?.name || "";
                        setSelectedWardCode(id || null);
                        setValue('ward', name);
                      }}
                      className={selectClassName}
                    >
                      <option value="">Chọn Phường / Xã</option>
                      {wards.map((w) => (
                        <option key={w.id} value={w.id}>{w.name}</option>
                      ))}
                    </select>
                    {errors.ward?.message && <p className="text-xs text-red-500">{errors.ward.message}</p>}
                  </div>

                  <Input label="Địa chỉ chi tiết" className="sm:col-span-2" error={errors.detail?.message} {...register('detail')} />
                </div>
                <label className="flex items-center gap-2 mt-4 text-sm text-[var(--color-text-muted)] cursor-pointer">
                  <input type="checkbox" {...register('isDefault')} className="rounded" />
                  Đặt làm địa chỉ mặc định
                </label>
                <div className="flex gap-3 mt-4">
                  <Button type="submit" loading={isSubmitting || createAddress.isPending || updateAddress.isPending}>
                    {editing ? 'Cập nhật' : 'Lưu địa chỉ'}
                  </Button>
                  <Button type="button" variant="secondary" onClick={() => { setShowForm(false); setEditing(null) }}>
                    Huỷ
                  </Button>
                </div>
              </form>
            </motion.div>
          )}
        </AnimatePresence>

        {/* List */}
        {isLoading ? (
          <div className="space-y-4">{[1, 2].map((i) => <Skeleton key={i} className="h-28 rounded-2xl" />)}</div>
        ) : addresses.length === 0 ? (
          <EmptyState icon="📍" title="Chưa có địa chỉ nào"
            action={<Button onClick={openCreate}><Plus className="w-4 h-4" />Thêm địa chỉ</Button>} />
        ) : (
          <div className="space-y-3">
            {addresses.map((addr) => (
              <div key={addr.id} className="bg-white rounded-2xl border border-[var(--color-border)] p-5">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <p className="font-bold text-sm text-[var(--color-brown-900)]">{addr.receiverName}</p>
                      <span className="text-[var(--color-text-muted)] text-sm">|</span>
                      <p className="text-sm text-[var(--color-text-muted)]">{addr.phone}</p>
                      {addr.isDefault && <Badge variant="organic" size="sm">Mặc định</Badge>}
                    </div>
                    <p className="text-sm text-[var(--color-text-muted)]">
                      {addr.detail}, {addr.ward}, {addr.district}, {addr.province}
                    </p>
                  </div>
                  <div className="flex items-center gap-1 flex-shrink-0">
                    {!addr.isDefault && (
                      <Button size="xs" variant="ghost" onClick={() => setDefault.mutate(addr.id)} title="Đặt mặc định">
                        <Star className="w-3.5 h-3.5" />
                      </Button>
                    )}
                    <Button size="xs" variant="ghost" onClick={() => openEdit(addr)}>
                      <Edit2 className="w-3.5 h-3.5" />
                    </Button>
                    {!addr.isDefault && (
                      <Button size="xs" variant="ghost" className="hover:text-red-500"
                        onClick={() => { if (confirm('Xoá địa chỉ này?')) deleteAddress.mutate(addr.id) }}>
                        <Trash2 className="w-3.5 h-3.5" />
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}