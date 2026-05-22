package backend.pineapple_ecommerce.modules.farm.repository;

import backend.pineapple_ecommerce.modules.farm.models.Farm;
import backend.pineapple_ecommerce.common.enums.FarmStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    /** Farmer lấy farm của mình — bao gồm INACTIVE/REJECTED nhưng loại trừ deleted */
    List<Farm> findByOwnerIdAndIsDeletedFalse(Long ownerId);

    /** Public listing — chỉ ACTIVE, chưa bị xoá */
    Page<Farm> findByStatusAndIsDeletedFalse(FarmStatus status, Pageable pageable);

    /** Admin listing — tất cả status, chưa bị xoá */
    Page<Farm> findByIsDeletedFalse(Pageable pageable);

    /** Admin listing — lọc theo status cụ thể, chưa bị xoá */
    Page<Farm> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(FarmStatus status, Pageable pageable);

    /** Lấy farm chưa bị xoá theo ID (public/Farmer dùng) */
    Optional<Farm> findByIdAndIsDeletedFalse(Long id);

    /** Compat: giữ lại cho FarmServiceImpl cũ */
    List<Farm> findByOwnerId(Long ownerId);
}
