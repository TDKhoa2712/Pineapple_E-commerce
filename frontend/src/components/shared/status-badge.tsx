import { cn } from "@/lib/utils";
import type {
  OrderStatus,
  FarmStatus,
  UserStatus,
  BatchStatus,
  PaymentStatus,
  ProductStatus,
} from "@/types";

type StatusType =
  | OrderStatus
  | FarmStatus
  | UserStatus
  | BatchStatus
  | PaymentStatus
  | ProductStatus;

const statusConfig: Record<
  string,
  { label: string; className: string }
> = {
  // Order
  PENDING: { label: "Chờ xác nhận", className: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
  CONFIRMED: { label: "Đã xác nhận", className: "bg-blue-500/15 text-blue-400 border-blue-500/30" },
  PROCESSING: { label: "Đang xử lý", className: "bg-violet-500/15 text-violet-400 border-violet-500/30" },
  SHIPPING: { label: "Đang giao", className: "bg-cyan-500/15 text-cyan-400 border-cyan-500/30" },
  DELIVERED: { label: "Đã giao", className: "bg-pine-500/15 text-pine-400 border-pine-500/30" },
  REFUND_REQUESTED: { label: "Yêu cầu hoàn", className: "bg-orange-500/15 text-orange-400 border-orange-500/30" },
  REFUNDED: { label: "Đã hoàn tiền", className: "bg-slate-500/15 text-slate-400 border-slate-500/30" },
  RETURNED: { label: "Đã hoàn hàng", className: "bg-slate-500/15 text-slate-400 border-slate-500/30" },
  CANCELLED: { label: "Đã hủy", className: "bg-red-500/15 text-red-400 border-red-500/30" },
  // Farm
  PENDING_APPROVAL: { label: "Chờ duyệt", className: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
  PENDING_DEACTIVATION: { label: "Chờ vô hiệu hóa", className: "bg-orange-500/15 text-orange-400 border-orange-500/30" },
  PENDING_REACTIVATION: { label: "Chờ kích hoạt lại", className: "bg-orange-500/15 text-orange-400 border-orange-500/30" },
  ACTIVE: { label: "Hoạt động", className: "bg-pine-500/15 text-pine-400 border-pine-500/30" },
  INACTIVE: { label: "Ngừng hoạt động", className: "bg-slate-500/15 text-slate-400 border-slate-500/30" },
  REJECTED: { label: "Từ chối", className: "bg-red-500/15 text-red-400 border-red-500/30" },
  // User
  BANNED: { label: "Bị khóa", className: "bg-red-500/15 text-red-400 border-red-500/30" },
  // Batch
  AVAILABLE: { label: "Còn hàng", className: "bg-pine-500/15 text-pine-400 border-pine-500/30" },
  SOLD_OUT: { label: "Hết hàng", className: "bg-slate-500/15 text-slate-400 border-slate-500/30" },
  EXPIRED: { label: "Hết hạn", className: "bg-red-500/15 text-red-400 border-red-500/30" },
  // (legacy) some old FE used DEPLETED; keep mapping to avoid UI crash
  DEPLETED: { label: "Hết hàng", className: "bg-slate-500/15 text-slate-400 border-slate-500/30" },
  // Payment
  UNPAID: { label: "Chưa thanh toán", className: "bg-amber-500/15 text-amber-400 border-amber-500/30" },
  PAID: { label: "Đã thanh toán", className: "bg-pine-500/15 text-pine-400 border-pine-500/30" },
  FAILED: { label: "Thất bại", className: "bg-red-500/15 text-red-400 border-red-500/30" },
  // Product
  OUT_OF_STOCK: { label: "Hết hàng", className: "bg-red-500/15 text-red-400 border-red-500/30" },
};

interface StatusBadgeProps {
  status: StatusType;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = statusConfig[status] ?? {
    label: status,
    className: "bg-slate-500/15 text-slate-400 border-slate-500/30",
  };

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium",
        config.className,
        className
      )}
    >
      {config.label}
    </span>
  );
}