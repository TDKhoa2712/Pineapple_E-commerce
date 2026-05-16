package backend.pineapple_ecommerce.enums;

/**
 * Trạng thái của trang trại trong marketplace.
 *
 * <pre>
 * PENDING_APPROVAL → ACTIVE     (Admin duyệt)
 * PENDING_APPROVAL → REJECTED   (Admin từ chối)
 * ACTIVE           → INACTIVE   (Admin/Farmer tạm khoá)
 * INACTIVE         → ACTIVE     (Admin kích hoạt lại)
 * </pre>
 */
public enum FarmStatus {
    /** Farm mới tạo, chờ Admin duyệt — chưa public */
    PENDING_APPROVAL,

    /** Farm đã được duyệt — hiển thị cho public */
    ACTIVE,

    /** Farm bị tạm khoá (chủ động hoặc vi phạm) */
    INACTIVE,

    /** Farm bị từ chối — có rejectionReason */
    REJECTED
}
