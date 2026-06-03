'use client'

import { useState } from 'react'
import Image from 'next/image'
import { motion, AnimatePresence } from 'framer-motion'
import { Star, XCircle, Loader2, Camera, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useCreateReview } from '@/hooks'
import { uploadApi } from '@/services/api'
import { Button } from '@/components/ui'

interface ReviewModalProps {
  product: {
    id: number
    name: string
    thumbnail: string
  }
  orderId?: number // Optional since direct review from product detail page does not have orderId
  onClose: () => void
}

export function ReviewModal({ product, orderId, onClose }: ReviewModalProps) {
  const [rating, setRating] = useState(5)
  const [hoverRating, setHoverRating] = useState<number | null>(null)
  const [comment, setComment] = useState('')
  const [images, setImages] = useState<{ url: string; publicId: string }[]>([])
  const [dragActive, setDragActive] = useState(false)
  const [uploading, setUploading] = useState(false)
  const createReview = useCreateReview()

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    const files = e.dataTransfer.files
    if (files && files.length > 0) {
      await uploadFiles(files)
    }
  }

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (files && files.length > 0) {
      await uploadFiles(files)
    }
  }

  const uploadFiles = async (files: FileList) => {
    const availableSlots = 5 - images.length
    if (availableSlots <= 0) {
      toast.error('Đã đạt giới hạn tối đa 5 hình ảnh')
      return
    }

    setUploading(true)
    try {
      const filesToUpload = Array.from(files).slice(0, availableSlots)
      for (const file of filesToUpload) {
        if (!file.type.startsWith('image/')) {
          toast.error(`File "${file.name}" không phải là ảnh`)
          continue
        }
        if (file.size > 5 * 1024 * 1024) {
          toast.error(`Ảnh "${file.name}" quá lớn (tối đa 5MB)`)
          continue
        }
        const res = await uploadApi.uploadImage(file, 'REVIEW')
        setImages((prev) => [...prev, { url: res.data.url, publicId: res.data.publicId }])
      }
      toast.success('Đã tải ảnh lên thành công!')
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      toast.error(error.response?.data?.message || 'Tải ảnh lên thất bại')
    } finally {
      setUploading(false)
    }
  }

  const handleDeleteImage = (indexToRemove: number) => {
    setImages((prev) => prev.filter((_, i) => i !== indexToRemove))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (comment.length < 10) {
      toast.error('Bình luận phải có ít nhất 10 ký tự')
      return
    }

    createReview.mutate(
      {
        productId: product.id,
        orderId: orderId || 0,
        rating,
        comment,
        images,
      },
      {
        onSuccess: () => {
          onClose()
        },
        onError: (err: unknown) => {
          const error = err as { response?: { data?: { message?: string } } };
          toast.error(error.response?.data?.message || 'Gửi đánh giá thất bại')
        },
      }
    )
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop overlay */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="absolute inset-0 bg-black/60 backdrop-blur-sm"
      />

      {/* Modal Content */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        className="relative w-full max-w-lg bg-white rounded-3xl overflow-hidden shadow-2xl border border-[var(--color-border)] p-6 z-10"
      >
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-[var(--color-text-muted)] hover:text-[var(--color-text)] transition-colors cursor-pointer"
        >
          <XCircle className="w-6 h-6" />
        </button>

        <h3
          className="text-xl font-bold text-[var(--color-brown-900)] mb-4 pr-8"
          style={{ fontFamily: 'var(--font-display)' }}
        >
          Viết đánh giá sản phẩm
        </h3>

        <div className="flex items-center gap-3 mb-5 p-3 bg-[var(--color-brown-50)] rounded-2xl border border-[var(--color-border)]">
          <div className="relative w-12 h-12 rounded-xl overflow-hidden flex-shrink-0 bg-white border border-[var(--color-border)]">
            <Image
              src={product.thumbnail || '/placeholder-product.jpg'}
              alt={product.name}
              fill
              className="object-cover"
              sizes="48px"
            />
          </div>
          <p className="font-semibold text-sm text-[var(--color-brown-900)] line-clamp-2">
            {product.name}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Star Selection */}
          <div className="flex flex-col items-center justify-center p-4 bg-[var(--color-brown-50)] rounded-2xl border border-[var(--color-border)]">
            <p className="text-xs font-semibold text-[var(--color-text-muted)] uppercase mb-2">
              Đánh giá của bạn
            </p>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((star) => {
                const isFilled = hoverRating !== null ? star <= hoverRating : star <= rating
                return (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    onMouseEnter={() => setHoverRating(star)}
                    onMouseLeave={() => setHoverRating(null)}
                    className="transition-transform hover:scale-125 focus:outline-none cursor-pointer"
                  >
                    <Star
                      size={32}
                      className={`transition-colors duration-150 ${
                        isFilled
                          ? 'fill-[var(--color-gold-500)] text-[var(--color-gold-500)]'
                          : 'fill-transparent text-[var(--color-brown-200)]'
                      }`}
                    />
                  </button>
                )
              })}
            </div>
            <p className="text-xs text-[var(--color-text-muted)] mt-2 font-semibold">
              {rating === 5
                ? '⭐ Rất tốt'
                : rating === 4
                ? '⭐ Tốt'
                : rating === 3
                ? '⭐ Bình thường'
                : rating === 2
                ? '⭐ Không tốt'
                : '⭐ Rất kém'}
            </p>
          </div>

          {/* Comment input */}
          <div className="space-y-1">
            <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
              Nhận xét sản phẩm
            </label>
            <textarea
              required
              rows={4}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder="Chia sẻ trải nghiệm của bạn về sản phẩm này nhé (tối thiểu 10 ký tự)..."
              className="w-full p-4 bg-white border border-[var(--color-border)] rounded-2xl text-sm text-[var(--color-text)] placeholder:text-[var(--color-text-subtle)] focus:outline-none focus:ring-2 focus:ring-[var(--color-gold-500)] transition-all resize-none"
            />
            <p className="text-right text-[10px] text-[var(--color-text-subtle)] font-medium">
              {comment.length} / 1000 ký tự (tối thiểu 10)
            </p>
          </div>

          {/* Image upload section */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-semibold text-[var(--color-text-muted)] uppercase tracking-wide">
                Hình ảnh thực tế ({images.length}/5)
              </label>
              <span className="text-[10px] text-[var(--color-text-subtle)] font-medium">Tối đa 5MB/ảnh</span>
            </div>

            {/* Drag & Drop Area */}
            {images.length < 5 && (
              <div
                onDragEnter={handleDrag}
                onDragOver={handleDrag}
                onDragLeave={handleDrag}
                onDrop={handleDrop}
                className={`relative border-2 border-dashed rounded-2xl p-6 transition-all duration-200 text-center flex flex-col items-center justify-center gap-2 group cursor-pointer
                  ${dragActive 
                    ? 'border-[var(--color-gold-500)] bg-[var(--color-brown-50)] scale-[1.01]' 
                    : 'border-[var(--color-border)] hover:border-[var(--color-gold-400)] hover:bg-[var(--color-brown-50)]/30'
                  }`}
              >
                <input 
                  type="file" 
                  accept="image/*" 
                  multiple 
                  onChange={handleFileChange}
                  className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
                  disabled={uploading}
                />
                
                <div className="w-10 h-10 rounded-full bg-[var(--color-brown-50)] group-hover:bg-white flex items-center justify-center text-[var(--color-gold-500)] transition-colors">
                  <Camera className="w-5 h-5" />
                </div>
                <div>
                  <p className="text-sm font-semibold text-[var(--color-brown-900)]">
                    Kéo thả hoặc Click để tải ảnh
                  </p>
                  <p className="text-xs text-[var(--color-text-subtle)] mt-0.5">
                    Hỗ trợ định dạng JPG, PNG, WEBP (Tối đa 5 ảnh)
                  </p>
                </div>
              </div>
            )}

            {/* Thumbnail Preview Grid */}
            {(images.length > 0 || uploading) && (
              <div className="grid grid-cols-5 gap-2 mt-3">
                <AnimatePresence>
                  {images.map((img, i) => (
                    <motion.div
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.8 }}
                      key={img.publicId || i}
                      className="relative aspect-square rounded-xl overflow-hidden border border-[var(--color-border)] bg-[var(--color-brown-50)] group"
                    >
                      <Image src={img.url} alt="" fill className="object-cover" sizes="80px" />
                      <button
                        type="button"
                        onClick={() => handleDeleteImage(i)}
                        className="absolute inset-0 bg-black/60 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer z-10"
                      >
                        <Trash2 className="w-4 h-4 text-white hover:scale-110 transition-transform" />
                      </button>
                    </motion.div>
                  ))}
                </AnimatePresence>

                {/* Uploading Placeholder Card */}
                {uploading && (
                  <div className="relative aspect-square rounded-xl overflow-hidden border-2 border-dashed border-[var(--color-gold-400)] bg-[var(--color-brown-50)]/50 flex flex-col items-center justify-center animate-pulse">
                    <Loader2 className="w-4 h-4 animate-spin text-[var(--color-gold-500)]" />
                    <span className="text-[9px] font-semibold text-[var(--color-gold-600)] mt-1.5 uppercase tracking-wide">
                      Uploading
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* CTAs */}
          <div className="flex gap-3 pt-2">
            <Button type="submit" variant="gold" className="flex-1" loading={createReview.isPending}>
              Gửi đánh giá
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={onClose}
              disabled={createReview.isPending}
            >
              Huỷ
            </Button>
          </div>
        </form>
      </motion.div>
    </div>
  )
}
