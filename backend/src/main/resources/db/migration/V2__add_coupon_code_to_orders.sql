-- =============================================
-- V2: Thêm coupon_code vào bảng orders
-- Mục đích: Lưu mã giảm giá đã sử dụng cho đơn hàng
--           để hiển thị trong OrderResponse.couponCode (FE fix)
-- =============================================

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS coupon_code VARCHAR(50);

-- Optional: Add index for lookup by coupon code
CREATE INDEX IF NOT EXISTS idx_orders_coupon_code ON orders (coupon_code)
    WHERE coupon_code IS NOT NULL;
