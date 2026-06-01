package backend.pineapple_ecommerce.common.enums;

/**
 * Trang thai cua trang trai trong marketplace.
 */
public enum FarmStatus {
    /** Farm moi tao hoac vua cap nhat, cho Admin duyet. */
    PENDING_APPROVAL,

    /** Farm dang xin phep ngung hoat dong, cho Admin duyet. */
    PENDING_DEACTIVATION,

    /** Farm dang xin phep hoat dong lai, cho Admin duyet. */
    PENDING_REACTIVATION,

    /** Farm da duoc duyet va dang hien thi public. */
    ACTIVE,

    /** Farm tam ngung hoat dong, khong duoc thao tac san pham/ton kho. */
    INACTIVE,

    /** Farm bi tu choi, co rejectionReason. */
    REJECTED
}
