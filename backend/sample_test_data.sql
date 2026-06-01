-- ====================================================================
-- SCRIPT TẠO MẪU DỮ LIỆU ĐỂ TEST HỆ THỐNG PINEAPPLE E-COMMERCE
-- Hệ quản trị cơ sở dữ liệu: PostgreSQL
-- ====================================================================

-- Hướng dẫn sử dụng:
-- Bạn có thể chạy toàn bộ file SQL này để nạp dữ liệu.
-- Nếu bạn muốn làm sạch (xóa hết) dữ liệu cũ trước khi nạp mới, hãy bỏ comment dòng TRUNCATE bên dưới.

-- WARNING: Dòng lệnh dưới đây sẽ xoá sạch dữ liệu các bảng liên quan.
TRUNCATE TABLE user_roles, roles, users, addresses, categories, products, product_images, farms, inventory_batches, stock_adjustments, carts, cart_items, coupons, coupon_usages, reviews, review_images, review_votes, shipments, payments, orders, order_items, wishlists RESTART IDENTITY CASCADE;

-- ==========================================
-- 1. Đảm bảo có các ROLE hệ thống
-- ==========================================
INSERT INTO roles (name) VALUES 
('ROLE_ADMIN'),
('ROLE_FARMER'),
('ROLE_USER')
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- 2. Nạp dữ liệu USERS mẫu
-- Tất cả mật khẩu của các tài khoản dưới đây đều là: "password"
-- (Được mã hoá bằng BCrypt: $2a$10$ByI5qaMldT.sR72aE.F1GOe0.t9E.o3fF8p6q8z4N5t0w6gM5l7rS)
-- ==========================================
INSERT INTO users (id, email, password, full_name, phone, provider, status, email_verified, created_at, updated_at) VALUES
(1, 'admin@pineapple.vn', '$2a$10$ByI5qaMldT.sR72aE.F1GOe0.t9E.o3fF8p6q8z4N5t0w6gM5l7rS', 'Quản trị viên Pineapple', '0901234567', 'LOCAL', 'ACTIVE', true, NOW() - INTERVAL '30 days', NOW()),
(2, 'farmer@pineapple.vn', '$2a$10$ByI5qaMldT.sR72aE.F1GOe0.t9E.o3fF8p6q8z4N5t0w6gM5l7rS', 'Nông Dân Tiền Giang', '0907654321', 'LOCAL', 'ACTIVE', true, NOW() - INTERVAL '25 days', NOW()),
(3, 'user@pineapple.vn', '$2a$10$ByI5qaMldT.sR72aE.F1GOe0.t9E.o3fF8p6q8z4N5t0w6gM5l7rS', 'Nguyễn Văn Khách Hàng', '0912345678', 'LOCAL', 'ACTIVE', true, NOW() - INTERVAL '20 days', NOW()),
(4, 'farmer_lamdong@pineapple.vn', '$2a$10$ByI5qaMldT.sR72aE.F1GOe0.t9E.o3fF8p6q8z4N5t0w6gM5l7rS', 'Nông Dân Đà Lạt', '0933445566', 'LOCAL', 'ACTIVE', true, NOW() - INTERVAL '15 days', NOW())
ON CONFLICT (email) DO NOTHING;

-- Đồng bộ sequence của bảng users nếu cần thiết
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE(max(id), 1)) FROM users;

-- ==========================================
-- 3. Gán quyền (ROLE) cho các USERS
-- ==========================================
INSERT INTO user_roles (role_id, user_id) VALUES 
((SELECT id FROM roles WHERE name = 'ROLE_ADMIN'), 1),
((SELECT id FROM roles WHERE name = 'ROLE_FARMER'), 2),
((SELECT id FROM roles WHERE name = 'ROLE_USER'), 3),
((SELECT id FROM roles WHERE name = 'ROLE_FARMER'), 4)
ON CONFLICT DO NOTHING;

-- ==========================================
-- 4. Địa chỉ (ADDRESSES) mẫu cho khách hàng (user_id = 3)
-- ==========================================
INSERT INTO addresses (id, user_id, receiver_name, phone, province, district, ward, detail, is_default, created_at, updated_at) VALUES
(1, 3, 'Nguyễn Văn Khách Hàng', '0912345678', 'Thành phố Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', 'Số 123 Lê Lợi, P. Bến Nghé', true, NOW() - INTERVAL '20 days', NOW()),
(2, 3, 'Nguyễn Văn Khách Hàng (Văn phòng)', '0988776655', 'Thành phố Hồ Chí Minh', 'Quận Bình Thạnh', 'Phường 22', 'Tòa nhà Landmark 81, Tầng 15', false, NOW() - INTERVAL '10 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('addresses', 'id'), COALESCE(max(id), 1)) FROM addresses;

-- ==========================================
-- 5. Danh mục sản phẩm (CATEGORIES)
-- ==========================================
INSERT INTO categories (id, name, slug, parent_id, image, created_at, updated_at) VALUES
(1, 'Trái Cây Sạch', 'trai-cay-sach', NULL, 'https://images.unsplash.com/photo-1619546813926-a78fa6372cd2?w=500', NOW() - INTERVAL '30 days', NOW()),
(2, 'Trái Cây Việt Nam', 'trai-cay-viet-nam', 1, 'https://images.unsplash.com/photo-1528825871115-3581a5387919?w=500', NOW() - INTERVAL '29 days', NOW()),
(3, 'Trái Cây Nhập Khẩu', 'trai-cay-nhap-khau', 1, 'https://images.unsplash.com/photo-1610832958506-ee56336191d1?w=500', NOW() - INTERVAL '29 days', NOW()),
(4, 'Rau Củ Tươi', 'rau-cu-tuoi', NULL, 'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500', NOW() - INTERVAL '30 days', NOW()),
(5, 'Rau Lá Xanh', 'rau-la-xanh', 4, 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500', NOW() - INTERVAL '29 days', NOW()),
(6, 'Củ Quả Sạch', 'cu-qua-sach', 4, 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500', NOW() - INTERVAL '29 days', NOW()),
(7, 'Nấm & Thảo Mộc', 'nam-thao-moc', NULL, 'https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500', NOW() - INTERVAL '30 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('categories', 'id'), COALESCE(max(id), 1)) FROM categories;

-- ==========================================
-- 6. Nông trại (FARMS)
-- ==========================================
INSERT INTO farms (id, owner_id, name, location, description, status, image_url, is_deleted, created_at, updated_at) VALUES
(1, 2, 'Nông Trại Dứa Miền Tây', 'Huyện Tân Phước, Tỉnh Tiền Giang', 'Nông trại chuyên canh các giống dứa chất lượng cao như Queen, MD2 đạt tiêu chuẩn VietGAP.', 'ACTIVE', 'https://images.unsplash.com/photo-1592417817098-8f3d6eb19675?w=500', false, NOW() - INTERVAL '25 days', NOW()),
(2, 4, 'Đà Lạt Eco Farm', 'Thành phố Đà Lạt, Tỉnh Lâm Đồng', 'Nông trại sản xuất rau củ quả hữu cơ theo hướng công nghệ cao, nhà màng hiện đại.', 'ACTIVE', 'https://images.unsplash.com/photo-1500937386664-56d1dfef3854?w=500', false, NOW() - INTERVAL '15 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('farms', 'id'), COALESCE(max(id), 1)) FROM farms;

-- ==========================================
-- 7. Sản phẩm (PRODUCTS)
-- ==========================================
INSERT INTO products (id, category_id, name, slug, price, discount_price, is_organic, weight, calories, status, brand, origin, thumbnail, description, created_by, created_at, updated_at) VALUES
-- Trái cây Việt Nam (category_id = 2)
(1, 2, 'Dứa Mật MD2 Tiền Giang', 'dua-mat-md2-tien-giang', 45000.00, 39000.00, true, 1.20, 60.00, 'ACTIVE', 'Nông Sản Miền Tây', 'Tiền Giang, Việt Nam', 'https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500', 'Dứa mật MD2 trái lớn, vỏ mỏng, mắt dứa nông, vị ngọt đậm đà và mọng nước. Đạt chuẩn VietGAP cực kỳ an toàn cho sức khỏe.', 2, NOW() - INTERVAL '20 days', NOW()),
(2, 2, 'Dứa Queen Tân Phước', 'dua-queen-tan-phuoc', 35000.00, NULL, true, 1.00, 50.00, 'ACTIVE', 'Nông Sản Miền Tây', 'Tiền Giang, Việt Nam', 'https://images.unsplash.com/photo-1587883012610-e3df17d41270?w=500', 'Dứa Queen thơm nức tiếng vùng Tân Phước, thịt quả vàng xuộm, giòn ngọt, vị chua ngọt hài hoà thích hợp ăn tươi hoặc làm nước ép.', 2, NOW() - INTERVAL '20 days', NOW()),
(3, 2, 'Xoài Cát Hòa Lộc Hạng A', 'xoai-cat-hoa-loc-hang-a', 95000.00, 85000.00, true, 0.50, 75.00, 'ACTIVE', 'Hợp Tác Xã Hòa Lộc', 'Tiền Giang, Việt Nam', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=500', 'Xoài cát Hòa Lộc nổi tiếng thơm ngon, thịt quả mịn không xơ, vị ngọt thanh tao, mùi thơm đặc trưng quyến rũ.', 2, NOW() - INTERVAL '18 days', NOW()),

-- Trái cây nhập khẩu (category_id = 3)
(4, 3, 'Táo Rockit New Zealand (Hộp 4 quả)', 'tao-rockit-new-zealand', 129000.00, 115000.00, false, 0.35, 80.00, 'ACTIVE', 'Rockit', 'New Zealand', 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500', 'Táo Rockit nhỏ xinh, đóng trong ống nhựa tiện lợi. Vỏ màu đỏ hồng, thịt giòn ngọt đậm đà, hương thơm dịu mát.', 1, NOW() - INTERVAL '15 days', NOW()),
(5, 3, 'Việt Quất Tươi Peru (Hộp 125g)', 'viet-quat-tuoi-peru', 89000.00, NULL, false, 0.125, 45.00, 'ACTIVE', 'Driscolls', 'Peru', 'https://images.unsplash.com/photo-1498557850523-fd3d118b962e?w=500', 'Trái việt quất tươi nhập khẩu từ Peru quả to tròn, ngọt mát nhẹ, giàu chất chống oxy hóa tốt cho não bộ và thị lực.', 1, NOW() - INTERVAL '15 days', NOW()),

-- Rau lá xanh (category_id = 5)
(6, 5, 'Cải Bó Xôi Hữu Cơ Đà Lạt', 'cai-bo-xoi-huu-co-da-lat', 28000.00, 24000.00, true, 0.30, 23.00, 'ACTIVE', 'Đà Lạt Eco Farm', 'Lâm Đồng, Việt Nam', 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500', 'Cải bó xôi (rau chân vịt) trồng hữu cơ tại Đà Lạt, giàu sắt và vitamin. Thích hợp nấu canh hoặc làm các món xào.', 4, NOW() - INTERVAL '14 days', NOW()),
(7, 5, 'Xà Lách Thuỷ Canh Lollo Rossa', 'xa-lach-thuy-canh-lollo-rossa', 32000.00, NULL, true, 0.25, 15.00, 'ACTIVE', 'Đà Lạt Eco Farm', 'Lâm Đồng, Việt Nam', 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500', 'Xà lách Lollo Rossa lá xoăn đỏ đẹp mắt, trồng thuỷ canh sạch hoàn toàn, giòn mát ngọt dịu, thích hợp làm các món salad trộn.', 4, NOW() - INTERVAL '14 days', NOW()),

-- Củ quả sạch (category_id = 6)
(8, 6, 'Cà Rốt Đà Lạt Baby', 'ca-rot-da-lat-baby', 25000.00, NULL, true, 0.50, 41.00, 'ACTIVE', 'Đà Lạt Eco Farm', 'Lâm Đồng, Việt Nam', 'https://images.unsplash.com/photo-1444796893965-25a061807300?w=500', 'Cà rốt baby giòn ngọt, vỏ mỏng nhiều nước, thích hợp cho trẻ ăn dặm hoặc làm nước ép thanh lọc cơ thể.', 4, NOW() - INTERVAL '12 days', NOW()),
(9, 6, 'Khoai Tây Vàng Đà Lạt', 'khoai-tay-vang-da-lat', 35000.00, 31000.00, true, 1.00, 77.00, 'ACTIVE', 'Đà Lạt Eco Farm', 'Lâm Đồng, Việt Nam', 'https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=500', 'Khoai tây vàng Đà Lạt củ tròn đều, ruột vàng ươm, dẻo thơm, nhiều tinh bột thích hợp chiên, hầm canh.', 4, NOW() - INTERVAL '12 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('products', 'id'), COALESCE(max(id), 1)) FROM products;

-- ==========================================
-- 8. Ảnh chi tiết sản phẩm (PRODUCT_IMAGES)
-- ==========================================
INSERT INTO product_images (id, product_id, sort_order, image_url) VALUES
(1, 1, 1, 'https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500'),
(2, 1, 2, 'https://images.unsplash.com/photo-1618220179428-22790b461013?w=500'),
(3, 2, 1, 'https://images.unsplash.com/photo-1587883012610-e3df17d41270?w=500'),
(4, 3, 1, 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=500'),
(5, 6, 1, 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('product_images', 'id'), COALESCE(max(id), 1)) FROM product_images;

-- ==========================================
-- 9. Lô hàng tồn kho (INVENTORY_BATCHES)
-- Cần có tồn kho khả dụng thì khách mới add to cart được!
-- ==========================================
INSERT INTO inventory_batches (id, product_id, farm_id, batch_code, quantity, remaining_quantity, harvest_date, expiry_date, sweetness_level, status, note, created_at, updated_at) VALUES
(1, 1, 1, 'BATCH-MD2-001', 200, 150, CURRENT_DATE - 3, CURRENT_DATE + 7, 13.50, 'AVAILABLE', 'Lô dứa mật MD2 đạt chuẩn độ đường cao.', NOW() - INTERVAL '3 days', NOW()),
(2, 2, 1, 'BATCH-QUE-001', 150, 120, CURRENT_DATE - 2, CURRENT_DATE + 8, 12.00, 'AVAILABLE', 'Lô dứa Queen chín vàng thơm ngát.', NOW() - INTERVAL '2 days', NOW()),
(3, 3, 1, 'BATCH-XCL-001', 100, 80, CURRENT_DATE - 4, CURRENT_DATE + 5, 15.00, 'AVAILABLE', 'Xoài cát Hoà Lộc chín cây.', NOW() - INTERVAL '4 days', NOW()),
(4, 4, NULL, 'BATCH-TRK-001', 50, 45, CURRENT_DATE - 10, CURRENT_DATE + 20, NULL, 'AVAILABLE', 'Táo Rockit nhập khẩu đợt tháng 5.', NOW() - INTERVAL '10 days', NOW()),
(5, 5, NULL, 'BATCH-VQP-001', 80, 80, CURRENT_DATE - 7, CURRENT_DATE + 14, NULL, 'AVAILABLE', 'Việt quất Peru.', NOW() - INTERVAL '7 days', NOW()),
(6, 6, 2, 'BATCH-CBX-001', 120, 110, CURRENT_DATE - 1, CURRENT_DATE + 4, NULL, 'AVAILABLE', 'Cải bó xôi hái sớm mai.', NOW() - INTERVAL '1 days', NOW()),
(7, 7, 2, 'BATCH-XLL-001', 100, 95, CURRENT_DATE - 1, CURRENT_DATE + 4, NULL, 'AVAILABLE', 'Xà lách thuỷ canh nguyên rễ.', NOW() - INTERVAL '1 days', NOW()),
(8, 8, 2, 'BATCH-CRB-001', 150, 130, CURRENT_DATE - 2, CURRENT_DATE + 10, NULL, 'AVAILABLE', 'Cà rốt baby ngọt giòn sạch đất.', NOW() - INTERVAL '2 days', NOW()),
(9, 9, 2, 'BATCH-KTV-001', 300, 280, CURRENT_DATE - 5, CURRENT_DATE + 25, NULL, 'AVAILABLE', 'Khoai tây vàng vụ mới Đà Lạt.', NOW() - INTERVAL '5 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('inventory_batches', 'id'), COALESCE(max(id), 1)) FROM inventory_batches;

-- ==========================================
-- 10. Lịch sử điều chỉnh kho (STOCK_ADJUSTMENTS)
-- ==========================================
INSERT INTO stock_adjustments (id, batch_id, adjusted_by, qty_before, adjustment_qty, qty_after, reason, created_at, updated_at) VALUES
(1, 1, 2, 200, -50, 150, 'Xuất bán sỉ cho cửa hàng liên kết', NOW() - INTERVAL '2 days', NOW()),
(2, 2, 2, 150, -30, 120, 'Xuất mẫu dùng thử tại hội chợ', NOW() - INTERVAL '1 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('stock_adjustments', 'id'), COALESCE(max(id), 1)) FROM stock_adjustments;

-- ==========================================
-- 11. Giỏ hàng mẫu (CARTS) cho user_id = 3
-- ==========================================
INSERT INTO carts (id, user_id, created_at, updated_at) VALUES
(1, 3, NOW() - INTERVAL '10 days', NOW())
ON CONFLICT (user_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('carts', 'id'), COALESCE(max(id), 1)) FROM carts;

-- ==========================================
-- 12. Sản phẩm trong giỏ (CART_ITEMS)
-- ==========================================
INSERT INTO cart_items (id, cart_id, product_id, quantity) VALUES
(1, 1, 1, 2), -- 2 Dứa Mật MD2
(2, 1, 6, 3)  -- 3 Cải Bó Xôi
ON CONFLICT (cart_id, product_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('cart_items', 'id'), COALESCE(max(id), 1)) FROM cart_items;

-- ==========================================
-- 13. Mã giảm giá (COUPONS)
-- ==========================================
INSERT INTO coupons (id, code, type, value, min_order_value, max_discount_amount, total_limit, per_user_limit, used_count, is_active, start_date, expiry_date, created_by, created_at, updated_at) VALUES
(1, 'PINEAPPLE50', 'FIXED_AMOUNT', 50000.00, 200000.00, NULL, 100, 1, 5, true, NOW() - INTERVAL '5 days', NOW() + INTERVAL '30 days', 1, NOW() - INTERVAL '5 days', NOW()),
(2, 'FREESHIP20', 'FIXED_AMOUNT', 20000.00, 100000.00, NULL, 500, 2, 12, true, NOW() - INTERVAL '5 days', NOW() + INTERVAL '30 days', 1, NOW() - INTERVAL '5 days', NOW()),
(3, 'ANCHOISACH10', 'PERCENTAGE', 10.00, 150000.00, 30000.00, 200, 1, 0, true, NOW() - INTERVAL '1 days', NOW() + INTERVAL '15 days', 1, NOW() - INTERVAL '1 days', NOW())
ON CONFLICT (code) DO NOTHING;

SELECT setval(pg_get_serial_sequence('coupons', 'id'), COALESCE(max(id), 1)) FROM coupons;

-- ==========================================
-- 14. Thiết lập áp dụng Coupon cho danh mục
-- ==========================================
-- ANCHOISACH10 (id = 3) áp dụng cho danh mục Rau Củ Tươi (id = 4)
INSERT INTO coupon_applicable_categories (coupon_id, category_id) VALUES
(3, 4)
ON CONFLICT DO NOTHING;

-- ==========================================
-- 15. Đơn hàng mẫu (ORDERS & ORDER_ITEMS)
-- ==========================================
-- Đơn hàng đã hoàn thành (Delivered, đã trả tiền bằng COD)
INSERT INTO orders (id, user_id, address_id, subtotal, discount_amount, shipping_fee, total_amount, payment_method, payment_status, status, note, shipping_address, created_at, updated_at) VALUES
(1, 3, 1, 107000.00, 20000.00, 25000.00, 112000.00, 'COD', 'PAID', 'DELIVERED', 'Giao giờ hành chính', 'Nguyễn Văn Khách Hàng - 0912345678 - Thành phố Hồ Chí Minh, Quận 1, Phường Bến Nghé, Số 123 Lê Lợi, P. Bến Nghé', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days');

INSERT INTO order_items (id, order_id, product_id, batch_id, batch_code, product_name, product_thumbnail, quantity, unit_price, subtotal) VALUES
(1, 1, 1, 1, 'BATCH-MD2-001', 'Dứa Mật MD2 Tiền Giang', 'https://images.unsplash.com/photo-1550258987-190a2d41a8ba?w=500', 1, 39000.00, 39000.00),
(2, 1, 6, 6, 'BATCH-CBX-001', 'Cải Bó Xôi Hữu Cơ Đà Lạt', 'https://images.unsplash.com/photo-1576045057995-568f588f82fb?w=500', 2, 24000.00, 48000.00),
(3, 1, 8, 8, 'BATCH-CRB-001', 'Cà Rốt Đà Lạt Baby', 'https://images.unsplash.com/photo-1444796893965-25a061807300?w=500', 1, 20000.00, 20000.00);

-- Đơn hàng đang chờ thanh toán qua VNPAY
INSERT INTO orders (id, user_id, address_id, subtotal, discount_amount, shipping_fee, total_amount, payment_method, payment_status, status, note, shipping_address, created_at, updated_at) VALUES
(2, 3, 1, 230000.00, 50000.00, 30000.00, 210000.00, 'VNPAY', 'UNPAID', 'PENDING', 'Xin cảm ơn', 'Nguyễn Văn Khách Hàng - 0912345678 - Thành phố Hồ Chí Minh, Quận 1, Phường Bến Nghé, Số 123 Lê Lợi, P. Bến Nghé', NOW() - INTERVAL '1 hours', NOW());

INSERT INTO order_items (id, order_id, product_id, batch_id, batch_code, product_name, product_thumbnail, quantity, unit_price, subtotal) VALUES
(4, 2, 4, 4, 'BATCH-TRK-001', 'Táo Rockit New Zealand (Hộp 4 quả)', 'https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=500', 2, 115000.00, 230000.00);

SELECT setval(pg_get_serial_sequence('orders', 'id'), COALESCE(max(id), 1)) FROM orders;
SELECT setval(pg_get_serial_sequence('order_items', 'id'), COALESCE(max(id), 1)) FROM order_items;

-- ==========================================
-- 16. Lịch sử sử dụng Coupon (COUPON_USAGES)
-- ==========================================
INSERT INTO coupon_usages (id, coupon_id, user_id, order_id, discount_applied, used_at) VALUES
(1, 2, 3, 1, 20000.00, NOW() - INTERVAL '5 days'),
(2, 1, 3, 2, 50000.00, NOW() - INTERVAL '1 hours')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('coupon_usages', 'id'), COALESCE(max(id), 1)) FROM coupon_usages;

-- ==========================================
-- 17. Thanh toán mẫu (PAYMENTS)
-- ==========================================
INSERT INTO payments (id, order_id, amount, provider, status, transaction_code, paid_at, created_at, updated_at) VALUES
(1, 1, 112000.00, 'COD', 'PAID', 'TXN-COD-998811', NOW() - INTERVAL '4 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days')
ON CONFLICT (order_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('payments', 'id'), COALESCE(max(id), 1)) FROM payments;

-- ==========================================
-- 18. Vận đơn giao hàng mẫu (SHIPMENTS)
-- ==========================================
INSERT INTO shipments (id, order_id, carrier_code, current_status, shipping_fee, insurance_fee, total_fee, client_order_code, external_order_code, raw_status, created_at, updated_at) VALUES
(1, 1, 'GHN', 'DELIVERED', 25000.00, 0.00, 25000.00, 'PE-ORD-0001', 'GHN-100200300', 'delivered', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days')
ON CONFLICT (order_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('shipments', 'id'), COALESCE(max(id), 1)) FROM shipments;

-- ==========================================
-- 19. Đánh giá (REVIEWS & REVIEW_VOTES)
-- ==========================================
INSERT INTO reviews (id, product_id, user_id, rating, comment, helpful_count, unhelpful_count, is_hidden, created_at, updated_at) VALUES
(1, 1, 3, 5, 'Dứa rất ngọt, nhiều mật, quả tươi ngon. Rất đáng tiền!', 5, 0, false, NOW() - INTERVAL '3 days', NOW()),
(2, 6, 3, 4, 'Rau cải xanh tươi, không dập nát. Giao hàng nhanh.', 2, 0, false, NOW() - INTERVAL '3 days', NOW())
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('reviews', 'id'), COALESCE(max(id), 1)) FROM reviews;

INSERT INTO review_votes (id, review_id, user_id, is_helpful, created_at, updated_at) VALUES
(1, 1, 1, true, NOW() - INTERVAL '2 days', NOW()),
(2, 2, 1, true, NOW() - INTERVAL '2 days', NOW())
ON CONFLICT (review_id, user_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('review_votes', 'id'), COALESCE(max(id), 1)) FROM review_votes;

-- ==========================================
-- 20. Danh sách yêu thích (WISHLISTS)
-- ==========================================
INSERT INTO wishlists (id, user_id, product_id, created_at, updated_at) VALUES
(1, 3, 3, NOW() - INTERVAL '5 days', NOW())
ON CONFLICT (user_id, product_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('wishlists', 'id'), COALESCE(max(id), 1)) FROM wishlists;
