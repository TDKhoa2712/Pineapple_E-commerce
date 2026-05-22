package backend.pineapple_ecommerce.modules.address.service;

import backend.pineapple_ecommerce.modules.address.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.modules.address.dto.response.AddressResponse;

import java.util.List;

/**
 * Quản lý sổ địa chỉ giao hàng của người dùng.
 * Mỗi user có tối đa 5 địa chỉ; có duy nhất 1 địa chỉ mặc định.
 */
public interface AddressService {

    /** Lấy danh sách địa chỉ của user. */
    List<AddressResponse> getMyAddresses(Long userId);

    /** Thêm địa chỉ mới. Nếu isDefault = true thì bỏ default các địa chỉ cũ. */
    AddressResponse addAddress(Long userId, CreateAddressRequest request);

    /** Cập nhật địa chỉ. Chỉ chủ sở hữu mới được sửa. */
    AddressResponse updateAddress(Long userId, Long addressId, CreateAddressRequest request);

    /**
     * Đặt địa chỉ làm mặc định.
     * Tự động bỏ mặc định của các địa chỉ còn lại trong 1 transaction.
     */
    AddressResponse setDefault(Long userId, Long addressId);

    /**
     * Xoá địa chỉ.
     * Ném BusinessException nếu địa chỉ đang được dùng bởi đơn hàng đang xử lý.
     */
    void deleteAddress(Long userId, Long addressId);
}
