package backend.pineapple_ecommerce.modules.wishlist.repository;

import backend.pineapple_ecommerce.modules.wishlist.models.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    @Query(value = "SELECT wl FROM Wishlist wl LEFT JOIN FETCH wl.product WHERE wl.user.id = :userId",
           countQuery = "SELECT count(wl) FROM Wishlist wl WHERE wl.user.id = :userId")
    Page<Wishlist> findByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
