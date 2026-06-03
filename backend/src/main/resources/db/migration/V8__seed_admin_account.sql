-- =============================================================
-- V8: Seed default admin account
-- Password: Admin@123456 (BCrypt hash — đổi ngay sau khi deploy)
-- =============================================================

-- 1. Đảm bảo tất cả roles đã tồn tại (idempotent)
INSERT INTO roles (name)
VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_FARMER')
ON CONFLICT (name) DO NOTHING;

-- 2. Tạo tài khoản admin mặc định nếu chưa có
--    password: Admin@123456
INSERT INTO users (
    full_name,
    email,
    password,
    phone,
    status,
    provider,
    email_verified,
    created_at,
    updated_at
)
SELECT
    'System Admin',
    'admin@pineapple.vn',
    '$2a$12$TwEAZMqaBbdBTEZoHidxiuCf1K1LU5CXNixqfN5zD.j7UMSt3cAmi',
    '0900000000',
    'ACTIVE',
    'LOCAL',
    true,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@pineapple.vn'
);

-- 3. Gán role ROLE_ADMIN cho tài khoản admin vừa tạo
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.email = 'admin@pineapple.vn'
  AND r.name   = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );
