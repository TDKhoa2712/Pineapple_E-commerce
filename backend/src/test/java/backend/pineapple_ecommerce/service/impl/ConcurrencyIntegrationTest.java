package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.enums.BatchStatus;
import backend.pineapple_ecommerce.common.enums.CouponType;
import backend.pineapple_ecommerce.common.enums.ProductStatus;
import backend.pineapple_ecommerce.common.util.AppConstants;
import backend.pineapple_ecommerce.modules.category.models.Category;
import backend.pineapple_ecommerce.modules.category.repository.CategoryRepository;
import backend.pineapple_ecommerce.modules.coupon.models.Coupon;
import backend.pineapple_ecommerce.modules.coupon.repository.CouponRepository;
import backend.pineapple_ecommerce.modules.coupon.service.CouponService;
import backend.pineapple_ecommerce.modules.inventory.models.InventoryBatch;
import backend.pineapple_ecommerce.modules.inventory.repository.InventoryBatchRepository;
import backend.pineapple_ecommerce.modules.inventory.service.InventoryService;
import backend.pineapple_ecommerce.modules.product.models.Product;
import backend.pineapple_ecommerce.modules.product.repository.ProductRepository;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import backend.pineapple_ecommerce.modules.cart.models.CartItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Concurrency Integration Tests (Pessimistic Locking)")
public class ConcurrencyIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User testUser;
    private Category testCategory;
    private Product testProduct;
    private InventoryBatch testBatch;
    private Coupon testCoupon;

    @BeforeEach
    void setUp() {
        // Create Category with unique slug and name
        testCategory = Category.builder()
                .name("Test Concurrency Category " + UUID.randomUUID())
                .slug("test-concurrency-category-" + UUID.randomUUID())
                .build();
        testCategory = categoryRepository.save(testCategory);

        // Create Product with unique slug
        testProduct = Product.builder()
                .name("Test Concurrency Product")
                .slug("test-concurrency-product-" + UUID.randomUUID())
                .price(new BigDecimal("50000.00"))
                .status(ProductStatus.ACTIVE)
                .category(testCategory)
                .isOrganic(false)
                .build();
        testProduct = productRepository.save(testProduct);

        // Create User with unique email
        testUser = User.builder()
                .email("testconcurrency_" + UUID.randomUUID() + "@example.com")
                .fullName("Test Concurrency User")
                .emailVerified(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        // Delete in reverse order of dependencies to avoid foreign key violations
        if (testBatch != null) {
            try {
                inventoryBatchRepository.delete(testBatch);
            } catch (Exception e) {
                // Ignore if already deleted/absent
            }
        }
        if (testProduct != null) {
            try {
                productRepository.delete(testProduct);
            } catch (Exception e) {
                // Ignore
            }
        }
        if (testCategory != null) {
            try {
                categoryRepository.delete(testCategory);
            } catch (Exception e) {
                // Ignore
            }
        }
        if (testCoupon != null) {
            try {
                couponRepository.delete(testCoupon);
            } catch (Exception e) {
                // Ignore
            }
        }
        if (testUser != null) {
            try {
                userRepository.delete(testUser);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Test
    @DisplayName("Stock deduction FIFO concurrency test")
    void testStockDeductionConcurrency() throws InterruptedException {
        // Create a batch with 5 available items
        testBatch = InventoryBatch.builder()
                .product(testProduct)
                .batchCode("BATCH-CONC-" + UUID.randomUUID().toString().substring(0, 8))
                .quantity(5)
                .remainingQuantity(5)
                .status(BatchStatus.AVAILABLE)
                .harvestDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(10))
                .build();
        testBatch = inventoryBatchRepository.save(testBatch);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    // Each thread tries to deduct 1 unit from stock using the FIFO locking mechanism
                    inventoryService.deductStockFifo(testProduct.getId(), 1);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start concurrent threads
        finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Since batch has only 5 remaining quantities:
        // exactly 5 threads should successfully deduct 1 unit, and 5 threads should fail with BusinessException
        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(5);

        // Fetch updated batch state from db to assert remaining quantity is 0 and status is SOLD_OUT
        InventoryBatch updatedBatch = inventoryBatchRepository.findById(testBatch.getId()).orElseThrow();
        assertThat(updatedBatch.getRemainingQuantity()).isEqualTo(0);
        assertThat(updatedBatch.getStatus()).isEqualTo(BatchStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("Coupon usage limit validation concurrency test")
    void testCouponUsageConcurrency() throws InterruptedException {
        // Create Coupon with total limit 1
        LocalDateTime now = LocalDateTime.now(AppConstants.VN_ZONE);
        testCoupon = Coupon.builder()
                .code("CONC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .type(CouponType.PERCENTAGE)
                .value(new BigDecimal("10.00"))
                .minOrderValue(BigDecimal.ZERO)
                .totalLimit(1)
                .usedCount(0)
                .perUserLimit(10)
                .isActive(true)
                .startDate(now.minusDays(1))
                .expiryDate(now.plusDays(5))
                .build();
        testCoupon = couponRepository.save(testCoupon);

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CartItem> cartItems = new ArrayList<>(); // Empty cart items to bypass restrictions
        BigDecimal subtotal = new BigDecimal("100.00");

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    // Each thread tries to lock and apply the coupon
                    couponService.applyAndLock(testCoupon.getCode(), testUser.getId(), cartItems, subtotal);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start concurrent threads
        finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Since the coupon total limit is 1, exactly 1 thread should succeed and 9 threads should fail
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);

        // Fetch updated coupon state from db to assert usedCount is 1
        Coupon updatedCoupon = couponRepository.findById(testCoupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getUsedCount()).isEqualTo(1);
    }
}
