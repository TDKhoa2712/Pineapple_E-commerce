
Tài liệu này tổng hợp toàn bộ thông tin chi tiết về cấu trúc hệ thống, API endpoints, định dạng dữ liệu (payload), luồng bảo mật (Authentication/OAuth2), tích hợp thanh toán (VNPay), giao hàng (GHN/Carriers) và quản lý giỏ hàng để bạn có thể tiến hành xây dựng ứng dụng Frontend một cách nhanh chóng và chính xác nhất.

---

## 1. Tổng Quan Hệ Thống & Cấu Hình

*   **Địa chỉ Backend (Local):** `http://localhost:8080` (Mặc định cấu hình cổng 8080).
*   **Đường dẫn gốc API (Base API Path):** `/api/v1`
*   **Cấu hình CORS:** Chỉ cho phép nhận credentials (cookies) từ địa chỉ nguồn được cấu hình trong `FRONTEND_URL` (mặc định là `http://localhost:3000`).
*   **Swagger API Docs (Local):** `http://localhost:8080/swagger-ui.html` hoặc `http://localhost:8080/v3/api-docs` (truy cập để xem tài liệu tương tác).

---

## 2. Định Dạng Phản Hồi Chuẩn (API Response Wrapper)

Tất cả phản hồi từ Backend đều được bọc trong một cấu trúc chung để Frontend dễ dàng xử lý.

### 2.1. Phản hồi thành công (Success Response)
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": { ... }, // Đối tượng hoặc Mảng chứa dữ liệu thực tế
  "timestamp": "2026-05-30T06:11:04"
}
```

### 2.2. Phản hồi lỗi (Error Response)
```json
{
  "success": false,
  "message": "Mật khẩu không hợp lệ",
  "errors": { ... }, // Mảng lỗi validation hoặc thông tin chi tiết (nếu có)
  "timestamp": "2026-05-30T06:11:04"
}
```

### 2.3. Phản hồi phân trang (PageResponse)
Đối với các API lấy danh sách (sản phẩm, đơn hàng, đánh giá...), dữ liệu trong trường `"data"` sẽ có định dạng phân trang chuẩn sau:
```json
{
  "content": [ ... ], // Danh sách item thực tế của trang hiện tại
  "page": 0,          // Chỉ mục trang hiện tại (bắt đầu từ 0)
  "size": 20,         // Số lượng item trên một trang
  "totalElements": 100, // Tổng số lượng item trong toàn bộ hệ thống
  "totalPages": 5,    // Tổng số trang
  "last": false       // Đã là trang cuối cùng hay chưa (true/false)
}
```

---

## 3. Xác Thực & Bảo Mật (Authentication & Security)

Hệ thống sử dụng cơ chế bảo mật **JWT** kết hợp **HttpOnly Cookie** chống tấn công XSS.

### 3.1. Luồng Đăng ký & Xác thực Email (Local Register)
1.  **Đăng ký tài khoản:** Frontend gửi thông tin đăng ký đến `POST /api/v1/auth/register`.
2.  **Nhận mã OTP:** Backend sẽ tạo tài khoản ở trạng thái chưa xác thực, gửi OTP về email người dùng và trả về phản hồi:
    *   Trường `"emailVerified": false`
    *   Trường `"accessToken"` và `"refreshToken"` sẽ là `null`.
3.  **Xác thực Email:** Frontend hiển thị màn hình nhập OTP 6 chữ số. Gửi request đến `POST /api/v1/auth/verify-email`.
    *   Sau khi xác thực thành công, API trả về thông tin đăng nhập hoàn chỉnh chứa `accessToken` trực tiếp trong body.

### 3.2. Cấu Trúc JWT & Quản Lý Token
*   **AccessToken:**
    *   Thời hạn hiệu lực: **15 phút** (`900000 ms`).
    *   Phương thức truyền: Đính kèm trong HTTP Header: `Authorization: Bearer <accessToken>`.
*   **RefreshToken:**
    *   Thời hạn hiệu lực: **7 ngày** (`604800000 ms`).
    *   **Lưu trữ bảo mật:** Khi đăng nhập thành công qua OAuth2 hoặc gọi API làm mới (`POST /api/v1/auth/refresh`), Backend sẽ tự động lưu `refreshToken` vào trình duyệt dưới dạng một **HttpOnly, Secure, SameSite=Lax Cookie** (được phân quyền trên path `/api/v1/auth`).
    *   *Lưu ý cho Local Login:* Khi gọi `/login`, Refresh Token ban đầu được trả về trong response body. Frontend có thể lưu trữ hoặc kích hoạt ngay luồng `/refresh` để Backend tự động chuyển token đó sang dạng HttpOnly cookie.

### 3.3. Làm Mới Access Token (Token Rotation)
Khi `accessToken` hết hạn (lỗi `401 Unauthorized`), Frontend cần gọi API:
*   `POST /api/v1/auth/refresh`
*   **Yêu cầu Axios/Fetch:** Phải bật cờ `withCredentials: true` (hoặc credentials: 'include') để trình duyệt tự động đính kèm cookie `refresh_token`.
*   Backend nhận diện cookie, kiểm tra, thực hiện cơ chế xoay vòng (rotation): cấp lại `accessToken` mới trong body và đè một `refresh_token` cookie mới.

### 3.4. Đăng Nhập Mạng Xã Hội (OAuth2 Flow)
Quy trình trao đổi mã bảo mật để chống lộ Token trong lịch sử trình duyệt:
1.  Frontend hướng người dùng redirect đến đường dẫn khởi chạy đăng nhập của Backend:
    *   Google: `http://localhost:8080/oauth2/authorization/google?redirect_uri=http://localhost:3000/oauth2/callback`
    *   Facebook: `http://localhost:8080/oauth2/authorization/facebook?redirect_uri=http://localhost:3000/oauth2/callback`
2.  Sau khi đăng nhập thành công ở cổng Google/Facebook, Backend sẽ redirect người dùng trở lại Frontend:
    *   URL redirect: `http://localhost:3000/oauth2/callback?code=<exchange_code>`
3.  Frontend đọc tham số `code` từ URL và gửi ngay một request ngầm đến Backend để đổi Token thật:
    *   `POST /api/v1/auth/oauth2/exchange`
    *   Payload: `{ "code": "<exchange_code>" }`
    *   Kết quả trả về: `accessToken` trong body và `refreshToken` tự động đặt trong HttpOnly Cookie.

---

## 4. Đặc Tả Các Endpoint Chi Tiết Theo Module

### 4.1. Địa chỉ (Address) — `/api/v1/addresses`
Quản lý địa chỉ giao hàng của người dùng.
*   **GET** `/api/v1/addresses/` : Lấy danh sách địa chỉ của user hiện tại.
*   **POST** `/api/v1/addresses/` : Thêm địa chỉ mới.
    *   *Payload:*
        ```json
        {
          "receiverName": "Nguyễn Văn A",
          "phone": "0901234567",
          "province": "Thành phố Hồ Chí Minh",
          "district": "Quận 1",
          "ward": "Phường Bến Nghé",
          "detail": "123 Lê Lợi",
          "isDefault": true,
          "carrierMetadata": {
            "GHN": {
              "provinceId": "202",
              "districtId": "1442",
              "wardCode": "20308"
            }
          }
        }
        ```
*   **PUT** `/api/v1/addresses/{addressId}` : Cập nhật địa chỉ.
*   **PATCH** `/api/v1/addresses/{addressId}/default` : Đặt địa chỉ này làm mặc định.
*   **DELETE** `/api/v1/addresses/{addressId}` : Xoá địa chỉ.

### 4.2. Xác Thực (Authentication) — `/api/v1/auth`
*   **POST** `/api/v1/auth/register` : Đăng ký tài khoản.
    *   *Payload:* `{ "email": "...", "password": "...", "fullName": "...", "phone": "..." }`
*   **POST** `/api/v1/auth/verify-email` : Xác thực OTP kích hoạt tài khoản.
    *   *Payload:* `{ "email": "...", "otp": "123456" }`
*   **POST** `/api/v1/auth/resend-verification` : Gửi lại mã kích hoạt.
    *   *Payload:* `{ "email": "..." }`
*   **POST** `/api/v1/auth/login` : Đăng nhập local.
    *   *Payload:* `{ "email": "...", "password": "..." }`
*   **POST** `/api/v1/auth/refresh` : Làm mới Token (yêu cầu gửi kèm cookie).
*   **POST** `/api/v1/auth/logout` : Đăng xuất (thu hồi token, xoá cookie).
*   **GET** `/api/v1/auth/me` : Lấy thông tin user hiện tại (AccessToken bắt buộc).
*   **Mật khẩu:**
    *   `POST /api/v1/auth/password-reset/initiate` : Yêu cầu reset (Gửi OTP qua email). Body: `{ "email": "..." }`
    *   `POST /api/v1/auth/password-reset/confirm` : Xác nhận đặt lại mật khẩu mới. Body: `{ "email": "...", "otp": "...", "newPassword": "..." }`

### 4.3. Giỏ Hàng (Cart) — `/api/v1/cart`
Cơ chế đồng bộ thông minh giúp tối ưu hóa giỏ hàng khách và giỏ hàng thật.
*   **GET** `/api/v1/cart/` : Lấy giỏ hàng chi tiết của người dùng.
*   **GET** `/api/v1/cart/count` : Trả về số lượng loại sản phẩm trong giỏ (hiển thị badge trên Header).
*   **POST** `/api/v1/cart/items` : Thêm sản phẩm vào giỏ.
    *   *Payload:* `{ "productId": 101, "quantity": 2 }`
*   **PUT** `/api/v1/cart/items/{cartItemId}` : Cập nhật số lượng của item trong giỏ. (Truyền `quantity: 0` để xoá item).
    *   *Payload:* `{ "quantity": 3 }`
*   **DELETE** `/api/v1/cart/items/{cartItemId}` : Xoá sản phẩm khỏi giỏ.
*   **DELETE** `/api/v1/cart/` : Xoá sạch giỏ hàng.
*   **POST** `/api/v1/cart/validate` : Kiểm tra xem tồn kho có đủ đáp ứng số lượng trong giỏ hàng hiện tại trước khi chuyển sang bước checkout hay không.
*   **POST** `/api/v1/cart/merge` : **SMART MERGE** — Gộp giỏ hàng khách (`localStorage`) vào giỏ hàng thật sau khi user đăng nhập.
    *   *Payload:*
        ```json
        {
          "items": [
            { "productId": 101, "quantity": 2 },
            { "productId": 102, "quantity": 1 }
          ]
        }
        ```
    *   *Ứng xử hệ thống:* Nếu sản phẩm hết hàng hoặc không còn kích hoạt sẽ được ghi vào danh sách `skippedItems` kèm lý do để hiển thị thông báo cho người dùng.

### 4.4. Danh Mục (Category) — `/api/v1/categories`
*   **GET** `/api/v1/categories/tree` : Lấy cây danh mục đa cấp (root + children) để dựng menu đa cấp nhanh.
*   **GET** `/api/v1/categories/` : Danh sách phẳng tất cả danh mục.
*   **GET** `/api/v1/categories/{id}` : Lấy theo ID.
*   **GET** `/api/v1/categories/slug/{slug}` : Lấy theo slug (đáp ứng URL thân thiện SEO).

### 4.5. Sản Phẩm (Product) — `/api/v1/products`
*   **GET** `/api/v1/products/` : Tìm kiếm & lọc sản phẩm nâng cao (Phân trang mặc định `page=0`, `size=20`).
    *   *Query Params hỗ trợ:*
        *   `keyword` (tìm kiếm theo tên)
        *   `categoryId` (lọc theo danh mục)
        *   `farmId` (lọc theo trang trại cung cấp)
        *   `inStock` (true/false)
        *   `minPrice` / `maxPrice` (lọc khoảng giá)
        *   `isOrganic` (true/false)
        *   `sortBy` (tiêu chí sắp xếp: `newest`, `price_asc`, `price_desc`)
*   **GET** `/api/v1/products/{id}` : Chi tiết sản phẩm theo ID.
*   **GET** `/api/v1/products/slug/{slug}` : Chi tiết sản phẩm theo Slug (Thân thiện SEO).
*   **GET** `/api/v1/products/{id}/related` : Lấy danh sách sản phẩm liên quan (cùng danh mục).
*   **GET** `/api/v1/products/{id}/stock` : Lấy tồn kho khả dụng hiện tại.

### 4.6. Vận Chuyển (Shipping) — `/api/v1/shipping`
Hỗ trợ tích hợp đa đơn vị vận chuyển (GHN - Giao Hàng Nhanh làm mặc định).
*   **GET** `/api/v1/shipping/carriers` : Lấy danh sách hãng vận chuyển được hỗ trợ (Ví dụ: `["GHN", "GHTK"]`).
*   **GET** `/api/v1/shipping/provinces` : Danh sách Tỉnh/Thành phố.
*   **GET** `/api/v1/shipping/districts?provinceId=...` : Danh sách Quận/Huyện dựa vào `provinceId`.
*   **GET** `/api/v1/shipping/wards?districtId=...` : Danh sách Phường/Xã dựa vào `districtId`.
*   **POST** `/api/v1/shipping/calculate-fee` : Tính phí giao hàng trước khi đặt hàng.
    *   *Payload:*
        ```json
        {
          "toDistrictId": "1442",
          "toWardCode": "20308",
          "weight": 500, // Khối lượng gram
          "length": 20, "width": 20, "height": 10,
          "insuranceValue": 150000, // Giá trị đơn hàng để mua bảo hiểm vận chuyển
          "serviceTypeId": "2", // GHN: 2 là dịch vụ E-commerce (chuẩn)
          "coupon": "FREESHIP" // Mã coupon ship nếu có
        }
        ```
*   **GET** `/api/v1/shipping/orders/{orderId}/tracking` : Theo dõi hành trình giao hàng thực tế của đơn. Trả về thông tin mã vận vận đơn gốc (`externalOrderCode`) và nhật ký lịch trình bằng tiếng Việt.

### 4.7. Đơn Hàng (Order) — `/api/v1/orders`
*   **POST** `/api/v1/orders/` : Tạo đơn hàng từ giỏ hàng hiện tại.
    *   *Payload:*
        ```json
        {
          "addressId": 12, // ID địa chỉ giao nhận của user
          "paymentMethod": "VNPAY", // COD | VNPAY | BANK_TRANSFER
          "note": "Giao giờ hành chính",
          "couponCode": "GIAM20K"
        }
        ```
*   **GET** `/api/v1/orders/my` : Lịch sử đơn hàng của tôi. Hỗ trợ query param `status` để lọc đơn (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED).
*   **GET** `/api/v1/orders/{orderId}` : Lấy thông tin chi tiết của một đơn hàng cụ thể.
*   **POST** `/api/v1/orders/{orderId}/cancel` : Huỷ đơn hàng (Chỉ được phép khi đơn ở trạng thái `PENDING` - Chờ thanh toán/Chờ xác nhận).

### 4.8. Cổng Thanh Toán (Payment) — `/api/v1/payments`
Tích hợp VNPay an toàn tuyệt đối.
*   **POST** `/api/v1/payments/{orderId}/initiate` : Khởi tạo giao dịch thanh toán.
    *   *Kết quả trả về:* Chứa `paymentUrl` (URL dẫn đến cổng thanh toán VNPay). Frontend redirect trình duyệt của người dùng đến link này.
*   **GET** `/api/v1/payments/order/{orderId}` : Lấy thông tin & trạng thái thanh toán từ Database.
    *   *Lưu ý bảo mật:* Khi người dùng thanh toán xong, VNPay sẽ redirect trình duyệt về Frontend tại đường dẫn: `/payment/result?status=success&txnRef=...`. **Frontend tuyệt đối không dùng giá trị status trên URL để hiển thị thông báo thành công**. Thay vào đó, hãy lấy `orderId` và gọi API `GET /api/v1/payments/order/{orderId}` để kiểm tra trạng thái thanh toán thực tế đã lưu trong DB (do IPN cập nhật ngầm an toàn).

### 4.9. Đánh Giá (Review) — `/api/v1/reviews`
*   **GET** `/api/v1/reviews/product/{productId}` : Xem danh sách đánh giá của sản phẩm (Phân trang).
*   **GET** `/api/v1/reviews/product/{productId}/rating` : Lấy điểm đánh giá trung bình.
*   **POST** `/api/v1/reviews/` : Đăng đánh giá mới.
    *   *Điều kiện:* User phải đăng nhập, đã mua sản phẩm này và đơn hàng phải ở trạng thái `DELIVERED`.
    *   *Payload:* `{ "productId": 101, "rating": 5, "comment": "Sản phẩm tươi ngon!" }`
*   **PUT** `/api/v1/reviews/{reviewId}` : Sửa đánh giá.
*   **DELETE** `/api/v1/reviews/{reviewId}` : Xoá đánh giá của mình.

### 4.10. Danh Sách Yêu Thích (Wishlist) — `/api/v1/wishlist`
*   **GET** `/api/v1/wishlist/` : Lấy danh sách sản phẩm yêu thích (Phân trang).
*   **POST** `/api/v1/wishlist/{productId}` : Toggle trạng thái yêu thích (Gửi lần 1: Thêm vào, Gửi lần 2: Xoá ra). Trả về boolean `true`/`false`.
*   **GET** `/api/v1/wishlist/{productId}/check` : Check xem sản phẩm này đã được yêu thích chưa để tô đỏ icon trái tim.

### 4.11. Quản Lý Trang Trại (Farmer Portal) — `/api/v1/farms` & `/api/v1/inventory`
Dành cho người dùng có vai trò `FARMER` hoặc `ADMIN`.
*   **POST** `/api/v1/farms/` : Đăng ký mở trang trại mới.
    *   *Payload:* `{ "name": "...", "location": "...", "description": "...", "certificate": "..." }` (Trạng thái ban đầu sẽ là `PENDING_APPROVAL`, chờ Admin phê duyệt).
*   **GET** `/api/v1/farms/my` : Danh sách trang trại thuộc quản lý của tôi.
*   **POST** `/api/v1/farms/{farmId}/image` : Upload ảnh đại diện trang trại.
*   **Inventory (Quản lý kho hàng):**
    *   `POST /api/v1/inventory/batches` : Nhập lô hàng mới cho sản phẩm (Chứa thông tin số lượng, ngày sản xuất, ngày hết hạn).
    *   `POST /api/v1/inventory/batches/{batchId}/adjust` : Điều chỉnh số lượng lô hàng do hư hỏng, hao hụt kèm lý do.

---

## 5. Hướng Dẫn Triển Khai Trên Frontend (Best Practices)

### 5.1. Cấu hình Axios Client chung
```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  withCredentials: true, // Quan trọng: Bắt buộc để tự động gửi/nhận HttpOnly Cookie refresh_token
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor đính kèm Access Token vào request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor xử lý tự động Refresh Token khi gặp lỗi 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        // Thực hiện gọi refresh ngầm, refresh_token cookie sẽ tự gửi đi
        const res = await axios.post(
          'http://localhost:8080/api/v1/auth/refresh',
          {},
          { withCredentials: true }
        );
        const newAccessToken = res.data.data.accessToken;
        localStorage.setItem('accessToken', newAccessToken);
        
        // Cập nhật token mới vào request bị lỗi ban đầu và chạy lại
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh token cũng hết hạn -> Yêu cầu đăng nhập lại
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

### 5.2. File Upload lên Cloudinary qua Backend
Backend cung cấp các API upload ảnh trung gian. Bạn không nên upload trực tiếp từ Frontend lên Cloudinary để bảo mật credentials:
*   Frontend gửi request dạng `multipart/form-data` chứa file ảnh đến:
    *   Avatar: `POST /api/v1/users/me/avatar`
    *   Sản phẩm phụ: `POST /api/v1/products/{productId}/images`
    *   Thumbnail: `POST /api/v1/products/{productId}/thumbnail`
*   Backend nhận file, tải lên Cloudinary và tự động lưu URL vào Database, sau đó trả về thông tin ảnh đã upload (`url`, `publicId`).

---

Chúc bạn xây dựng giao diện ứng dụng thành công và mượt mà! Nếu gặp bất kỳ vấn đề gì về định dạng dữ liệu DTO hoặc lỗi luồng tích hợp, hãy xem log chi tiết tại console và kiểm tra thông tin trên trang Swagger.
