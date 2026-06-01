package backend.pineapple_ecommerce.modules.farm.repository;

import backend.pineapple_ecommerce.modules.farm.models.Farm;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long>, JpaSpecificationExecutor<Farm> {

    /** Farmer lấy farm của mình — bao gồm INACTIVE/REJECTED nhưng loại trừ deleted */
    @Query("SELECT f FROM Farm f LEFT JOIN FETCH f.owner WHERE f.owner.id = :ownerId AND f.isDeleted = false")
    List<Farm> findByOwnerIdAndIsDeletedFalse(@Param("ownerId") Long ownerId);


    /** Lấy farm chưa bị xoá theo ID (public/Farmer dùng) */
    Optional<Farm> findByIdAndIsDeletedFalse(Long id);

    /** Compat: giữ lại cho FarmServiceImpl cũ */
    List<Farm> findByOwnerId(Long ownerId);

    boolean existsByOwnerIdAndStatusAndIsDeletedFalse(Long ownerId, FarmStatus status);

    long countByStatus(FarmStatus status);
}
