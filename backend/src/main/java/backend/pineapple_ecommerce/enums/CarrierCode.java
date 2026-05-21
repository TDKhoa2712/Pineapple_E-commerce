package backend.pineapple_ecommerce.enums;

public enum CarrierCode {

    GHN("Giao Hàng Nhanh"),
    GHTK("Giao Hàng Tiết Kiệm"),
    VIETTEL_POST("Viettel Post"),
    J_AND_T("J&T Express"),
    BEST_EXPRESS("BEST Express");

    private final String displayName;

    CarrierCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}