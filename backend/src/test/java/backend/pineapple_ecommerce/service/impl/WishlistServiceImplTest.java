package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.dto.response.WishlistResponse;
import backend.pineapple_ecommerce.entity.Product;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.entity.Wishlist;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.mapper.WishlistMapper;
import backend.pineapple_ecommerce.repository.ProductRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistServiceImplTest")
class WishlistServiceImplTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private UserRepository     userRepository;
    @Mock private ProductRepository  productRepository;
    @Mock private WishlistMapper     wishlistMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    // ── Fixtures ──────────────────────────────────────────────────────

    private static final Long USER_ID    = 1L;
    private static final Long PRODUCT_ID = 10L;

    private User    user;
    private Product product;

    @BeforeEach
    void setUp() {
        user    = User.builder().id(USER_ID).email("user@example.com").build();
        product = Product.builder().id(PRODUCT_ID).name("Dứa mật vàng").build();
    }

    // ─────────────────────────────────────────────────────────────────
    // getMyWishlist
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyWishlist()")
    class GetMyWishlist {

        @Test
        @DisplayName("trả về PageResponse ánh xạ từ repository")
        void givenUserId_shouldReturnPageResponse() {
            Wishlist wishlist = Wishlist.builder().user(user).product(product).build();
            WishlistResponse mappedResp = WishlistResponse.builder().build();

            Page<Wishlist> page = new PageImpl<>(List.of(wishlist));
            when(wishlistRepository.findByUserId(
                    eq(USER_ID), any(PageRequest.class))).thenReturn(page);
            when(wishlistMapper.toResponse(wishlist)).thenReturn(mappedResp);

            PageResponse<WishlistResponse> result =
                    wishlistService.getMyWishlist(USER_ID, 0, 10);

            assertThat(result.getContent()).containsExactly(mappedResp);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("wishlist rỗng → trả về page rỗng")
        void givenEmptyWishlist_shouldReturnEmptyPage() {
            Page<Wishlist> emptyPage = new PageImpl<>(List.of());
            when(wishlistRepository.findByUserId(eq(USER_ID), any())).thenReturn(emptyPage);

            PageResponse<WishlistResponse> result =
                    wishlistService.getMyWishlist(USER_ID, 0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("query dùng sort theo createdAt descending")
        void shouldQueryWithCorrectSort() {
            Page<Wishlist> page = new PageImpl<>(List.of());
            when(wishlistRepository.findByUserId(any(), any())).thenReturn(page);

            wishlistService.getMyWishlist(USER_ID, 0, 5);

            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(wishlistRepository).findByUserId(eq(USER_ID), captor.capture());

            PageRequest pr = captor.getValue();
            assertThat(pr.getPageNumber()).isZero();
            assertThat(pr.getPageSize()).isEqualTo(5);
            assertThat(pr.getSort().getOrderFor("createdAt"))
                    .isNotNull()
                    .satisfies(order -> assertThat(order.getDirection())
                            .isEqualTo(Sort.Direction.DESC));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // toggleWishlist
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleWishlist()")
    class ToggleWishlist {

        @Test
        @DisplayName("chưa có trong wishlist → thêm vào và trả về true")
        void givenNotInWishlist_shouldAddAndReturnTrue() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));

            boolean result = wishlistService.toggleWishlist(USER_ID, PRODUCT_ID);

            assertThat(result).isTrue();
            verify(wishlistRepository).save(any(Wishlist.class));
        }

        @Test
        @DisplayName("đã có trong wishlist → xoá và trả về false")
        void givenAlreadyInWishlist_shouldRemoveAndReturnFalse() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(true);

            boolean result = wishlistService.toggleWishlist(USER_ID, PRODUCT_ID);

            assertThat(result).isFalse();
            verify(wishlistRepository).deleteByUserIdAndProductId(USER_ID, PRODUCT_ID);
            verify(wishlistRepository, never()).save(any());
        }

        @Test
        @DisplayName("thêm vào: Wishlist được lưu với đúng user và product")
        void givenToggleOn_shouldSaveCorrectWishlist() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            ArgumentCaptor<Wishlist> captor = ArgumentCaptor.forClass(Wishlist.class);
            when(wishlistRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            wishlistService.toggleWishlist(USER_ID, PRODUCT_ID);

            Wishlist saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getProduct()).isEqualTo(product);
        }

        @Test
        @DisplayName("user không tồn tại → ném ResourceNotFoundException")
        void givenUnknownUser_shouldThrow() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.toggleWishlist(USER_ID, PRODUCT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("product không tồn tại → ném ResourceNotFoundException")
        void givenUnknownProduct_shouldThrow() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.toggleWishlist(USER_ID, PRODUCT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // isInWishlist
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isInWishlist()")
    class IsInWishlist {

        @Test
        @DisplayName("sản phẩm có trong wishlist → true")
        void givenProductInWishlist_shouldReturnTrue() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(true);
            assertThat(wishlistService.isInWishlist(USER_ID, PRODUCT_ID)).isTrue();
        }

        @Test
        @DisplayName("sản phẩm không có trong wishlist → false")
        void givenProductNotInWishlist_shouldReturnFalse() {
            when(wishlistRepository.existsByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(false);
            assertThat(wishlistService.isInWishlist(USER_ID, PRODUCT_ID)).isFalse();
        }
    }
}