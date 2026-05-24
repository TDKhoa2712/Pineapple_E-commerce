package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.modules.cart.models.Cart;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;
import backend.pineapple_ecommerce.modules.cart.service.CartService;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CouponPreviewRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.CreateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.request.UpdateCouponRequest;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponPreviewResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponResponse;
import backend.pineapple_ecommerce.modules.coupon.dto.response.CouponUsageResponse;
import backend.pineapple_ecommerce.modules.coupon.mapper.CouponMapper;
import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import backend.pineapple_ecommerce.modules.coupon.models.CouponUsage;
import backend.pineapple_ecommerce.modules.coupon.repository.CouponRepository;
import backend.pineapple_ecommerce.modules.coupon.repository.CouponUsageRepository;
import backend.pineapple_ecommerce.modules.coupon.service.CouponServiceImpl;
import backend.pineapple_ecommerce.modules.order.models.Order;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponServiceImpl")
class CouponServiceImplTest {

    @Mock private CouponRepository      couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;
    @Mock private CartService           cartService;
    @Mock private UserRepository        userRepository;
    @Mock private CouponMapper          couponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;

    private User user;
    private User admin;
    private Product product;
    private Category category;
    private CartItem cartItem;
    private Cart cart;
    private Coupon percentageCoupon;
    private Coupon fixedCoupon;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email("user@example.com").build();
        admin = User.builder().id(ADMIN_ID).email("admin@example.com").build();

        category = Category.builder().id(10L).name("Trai Cay").build();

        product = Product.builder()
                .id(20L)
                .name("Qua Dua")
                .price(new BigDecimal("100000"))
                .category(category)
                .build();

        cartItem = CartItem.builder()
                .id(30L)
                .product(product)
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(40L)
                .user(user)
                .items(new ArrayList<>(List.of(cartItem)))
                .build();

        percentageCoupon = Coupon.builder()
                .id(100L)
                .code("PINE20")
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("20"))
                .maxDiscountAmount(new BigDecimal("30000"))
                .minOrderValue(new BigDecimal("150000"))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .totalLimit(10)
                .usedCount(0)
                .perUserLimit(1)
                .isActive(true)
                .applicableProductIds(new HashSet<>())
                .applicableCategoryIds(new HashSet<>())
                .build();

        fixedCoupon = Coupon.builder()
                .id(101L)
                .code("PINE50K")
                .type(CouponType.FIXED_AMOUNT)
                .value(new BigDecimal("50000"))
                .minOrderValue(new BigDecimal("100000"))
                .startDate(LocalDateTime.now().minusDays(1))
                .expiryDate(LocalDateTime.now().plusDays(5))
                .totalLimit(5)
                .usedCount(2)
                .perUserLimit(1)
                .isActive(true)
                .applicableProductIds(new HashSet<>())
                .applicableCategoryIds(new HashSet<>())
                .build();
    }

    @Nested
    @DisplayName("previewCoupon()")
    class PreviewCoupon {

        @Test
        @DisplayName("xem trước coupon phần trăm thành công")
        void previewPercentageCouponSuccess() {
            CouponPreviewRequest req = new CouponPreviewRequest();
            req.setCouponCode("PINE20");
            req.setCartTotal(new BigDecimal("200000"));

            when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
            when(cartService.getCheckoutItems(USER_ID)).thenReturn(cart);
            when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), USER_ID)).thenReturn(0L);

            CouponPreviewResponse resp = couponService.previewCoupon(USER_ID, req);

            assertThat(resp.getCouponCode()).isEqualTo("PINE20");
            // subtotal = 2 * 100k = 200k. 20% = 40k. Max discount is 30k.
            assertThat(resp.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("30000"));
            assertThat(resp.getNewTotal()).isEqualByComparingTo(new BigDecimal("170000"));
        }

        @Test
        @DisplayName("xem trước coupon cố định thành công")
        void previewFixedCouponSuccess() {
            CouponPreviewRequest req = new CouponPreviewRequest();
            req.setCouponCode("PINE50K");
            req.setCartTotal(new BigDecimal("200000"));

            when(couponRepository.findByCodeIgnoreCase("PINE50K")).thenReturn(Optional.of(fixedCoupon));
            when(cartService.getCheckoutItems(USER_ID)).thenReturn(cart);
            when(couponUsageRepository.countByCouponIdAndUserId(fixedCoupon.getId(), USER_ID)).thenReturn(0L);

            CouponPreviewResponse resp = couponService.previewCoupon(USER_ID, req);

            assertThat(resp.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(resp.getNewTotal()).isEqualByComparingTo(new BigDecimal("150000"));
        }

        @Test
        @DisplayName("xem trước thất bại nếu đơn hàng chưa đủ minOrderValue")
        void previewMinOrderValueFail() {
            // total giỏ hàng là 200k, minOrderValue của percentageCoupon là 150k. 
            // Giảm số lượng giỏ hàng xuống 1 cái để subtotal = 100k < 150k
            cartItem.setQuantity(1);

            CouponPreviewRequest req = new CouponPreviewRequest();
            req.setCouponCode("PINE20");
            req.setCartTotal(new BigDecimal("100000"));

            when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
            when(cartService.getCheckoutItems(USER_ID)).thenReturn(cart);

            assertThatThrownBy(() -> couponService.previewCoupon(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Đơn hàng chưa đạt giá trị tối thiểu");
        }

        @Test
        @DisplayName("xem trước thất bại nếu sản phẩm không nằm trong diện áp dụng")
        void previewProductApplicabilityFail() {
            percentageCoupon.setApplicableProductIds(Set.of(999L)); // Coupon chỉ áp dụng cho sản phẩm 999

            CouponPreviewRequest req = new CouponPreviewRequest();
            req.setCouponCode("PINE20");
            req.setCartTotal(new BigDecimal("200000"));

            when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
            when(cartService.getCheckoutItems(USER_ID)).thenReturn(cart);

            assertThatThrownBy(() -> couponService.previewCoupon(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("không áp dụng cho các sản phẩm");
        }
    }

    @Nested
    @DisplayName("applyAndLock()")
    class ApplyAndLock {

        @Test
        @DisplayName("áp dụng và khoá coupon thành công")
        void applyAndLockSuccess() {
            when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
            when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), USER_ID)).thenReturn(0L);
            when(couponRepository.incrementUsedCount(percentageCoupon.getId())).thenReturn(1);

            BigDecimal discount = couponService.applyAndLock("PINE20", USER_ID, cart.getItems(), new BigDecimal("200000"));

            assertThat(discount).isEqualByComparingTo(new BigDecimal("30000"));
            verify(couponRepository).incrementUsedCount(percentageCoupon.getId());
        }

        @Test
        @DisplayName("ném ngoại lệ nếu mã đã hết lượt khi thực hiện lock")
        void applyAndLockExhausted() {
            when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
            when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), USER_ID)).thenReturn(0L);
            when(couponRepository.incrementUsedCount(percentageCoupon.getId())).thenReturn(0); // 0 rows affected => exhausted

            assertThatThrownBy(() -> couponService.applyAndLock("PINE20", USER_ID, cart.getItems(), new BigDecimal("200000")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("đã hết lượt sử dụng");
        }
    }

    @Nested
    @DisplayName("releaseCouponUsage()")
    class ReleaseCouponUsage {

        @Test
        @DisplayName("hoàn lại lượt sử dụng coupon và xóa log")
        void releaseCouponSuccess() {
            CouponUsage usage = CouponUsage.builder()
                    .id(500L)
                    .coupon(percentageCoupon)
                    .order(Order.builder().id(999L).build())
                    .user(user)
                    .build();

            when(couponUsageRepository.findAllByOrderId(999L)).thenReturn(List.of(usage));

            couponService.releaseCouponUsage(999L);

            verify(couponRepository).decrementUsedCount(percentageCoupon.getId());
            verify(couponUsageRepository).delete(usage);
        }
    }

    @Test
    @DisplayName("Mô phỏng concurrency bằng CountDownLatch và ExecutorService")
    void testConcurrentAccessSimulation() throws InterruptedException {
        // Giả sử ta chạy 5 luồng đặt hàng đồng thời cho 1 coupon còn đúng 1 lượt
        percentageCoupon.setTotalLimit(1);
        percentageCoupon.setUsedCount(0);

        when(couponRepository.findByCodeIgnoreCase("PINE20")).thenReturn(Optional.of(percentageCoupon));
        when(couponUsageRepository.countByCouponIdAndUserId(percentageCoupon.getId(), USER_ID)).thenReturn(0L);

        // Mô phỏng: Luồng đầu tiên gọi update thành công (return 1), các luồng sau lỗi (return 0)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        
        when(couponRepository.incrementUsedCount(percentageCoupon.getId())).thenAnswer(invocation -> {
            // Chỉ duy nhất một luồng có thể tăng thành công
            if (successCount.getAndIncrement() == 0) {
                return 1;
            } else {
                return 0;
            }
        });

        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Chờ tất cả xuất phát cùng lúc
                    couponService.applyAndLock("PINE20", USER_ID, cart.getItems(), new BigDecimal("200000"));
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Phát lệnh chạy đồng thời
        finishLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // Luồng đầu tiên update thành công, 4 luồng sau nhận lỗi "hết lượt sử dụng"
        assertThat(failCount.get()).isEqualTo(4);
        verify(couponRepository, times(5)).incrementUsedCount(percentageCoupon.getId());
    }
}
