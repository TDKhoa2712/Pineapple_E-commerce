import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { formatDistanceToNow, format } from "date-fns";
import { vi } from "date-fns/locale";
import type { OrderStatus, ProductStatus } from "@/types";

// ─── Tailwind CSS merger ──────────────────────────────────────────────────────
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// ─── Price / Currency formatting ──────────────────────────────────────────────
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

export const formatPrice = formatCurrency;

export function formatNumber(value: number): string {
  return new Intl.NumberFormat("vi-VN").format(value);
}

export function calcDiscountPercent(original: number, discounted: number): number {
  if (!original) return 0;
  return Math.round(((original - discounted) / original) * 100);
}

// ─── Date formatting ──────────────────────────────────────────────────────────
export function relativeTime(dateStr: string): string {
  try {
    return formatDistanceToNow(new Date(dateStr), { addSuffix: true, locale: vi });
  } catch {
    return dateStr;
  }
}

export function formatDate(
  dateStr: string,
  optionsOrPattern?: Intl.DateTimeFormatOptions | string
): string {
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  if (typeof optionsOrPattern === "string") {
    try {
      return format(d, optionsOrPattern, { locale: vi });
    } catch {
      return dateStr;
    }
  }
  return d.toLocaleDateString("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    ...optionsOrPattern,
  });
}

export function formatDateTime(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    const pad = (n: number) => n.toString().padStart(2, "0");
    return `${pad(d.getHours())}:${pad(d.getMinutes())} - ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
  } catch {
    return dateStr;
  }
}

// ─── Initials & String Helpers ────────────────────────────────────────────────
export function getInitials(name: string): string {
  if (!name) return "";
  const words = name.trim().split(/\s+/);
  if (words.length > 1) {
    const firstWord = words[0];
    const lastWord = words[words.length - 1];
    return ((firstWord[0] || "") + (lastWord[0] || "")).toUpperCase();
  }
  const word = words[0] || "";
  return word.slice(0, 2).toUpperCase();
}

export function slugify(text: string): string {
  return text
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .trim();
}

export function truncate(str: string, maxLength: number): string {
  if (str.length <= maxLength) return str;
  return `${str.slice(0, maxLength)}...`;
}

// ─── Enums & Labels ───────────────────────────────────────────────────────────
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang xử lý",
  SHIPPING: "Đang giao hàng",
  DELIVERED: "Đã giao hàng",
  CANCELLED: "Đã huỷ",
  REFUND_REQUESTED: "Yêu cầu hoàn tiền",
  REFUNDED: "Đã hoàn tiền",
  RETURNED: "Đã trả hàng",
};

export const ORDER_STATUS_COLORS: Record<OrderStatus, { bg: string; text: string; border: string }> = {
  PENDING:          { bg: "bg-amber-50",   text: "text-amber-700",  border: "border-amber-200" },
  CONFIRMED:        { bg: "bg-blue-50",    text: "text-blue-700",   border: "border-blue-200"  },
  PROCESSING:       { bg: "bg-purple-50",  text: "text-purple-700", border: "border-purple-200"},
  SHIPPING:         { bg: "bg-cyan-50",    text: "text-cyan-700",   border: "border-cyan-200"  },
  DELIVERED:        { bg: "bg-green-50",   text: "text-green-700",  border: "border-green-200" },
  CANCELLED:        { bg: "bg-red-50",     text: "text-red-700",    border: "border-red-200"   },
  REFUND_REQUESTED: { bg: "bg-orange-50",  text: "text-orange-700", border: "border-orange-200"},
  REFUNDED:         { bg: "bg-gray-50",    text: "text-gray-700",   border: "border-gray-200"  },
  RETURNED:         { bg: "bg-gray-50",    text: "text-gray-700",   border: "border-gray-200"  },
};

export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = {
  ACTIVE: "Đang bán",
  PENDING_DEACTIVATION: "Chờ ngừng bán",
  INACTIVE: "Ngừng bán",
  OUT_OF_STOCK: "Hết hàng",
};

// ─── Regex patterns ───────────────────────────────────────────────────────────
export const PHONE_REGEX = /^(0[3|5|7|8|9])+([0-9]{8})$/;
export const CLOUDINARY_URL_REGEX = /^https:\/\/res\.cloudinary\.com\/.*/;