package backend.pineapple_ecommerce.modules.address.controller;

import backend.pineapple_ecommerce.modules.address.service.AddressService;
import backend.pineapple_ecommerce.modules.address.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.modules.address.dto.response.AddressResponse;
import backend.pineapple_ecommerce.common.dto.response.ApiResponse;
import backend.pineapple_ecommerce.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Addresses", description = "Quản lý địa chỉ giao hàng")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;
    private final UserService    userService;

    @Operation(summary = "Lấy danh sách địa chỉ của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(addressService.getMyAddresses(userId)));
    }

    @Operation(summary = "Thêm địa chỉ mới")
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody CreateAddressRequest request) {

        Long userId = userService.getCurrentUserId();
        AddressResponse response = addressService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thêm địa chỉ thành công"));
    }

    @Operation(summary = "Cập nhật địa chỉ")
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody CreateAddressRequest request) {

        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                addressService.updateAddress(userId, addressId, request), "Cập nhật địa chỉ thành công"));
    }

    @Operation(summary = "Đặt địa chỉ làm mặc định")
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long addressId) {
        Long userId = userService.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                addressService.setDefault(userId, addressId), "Đã đặt làm địa chỉ mặc định"));
    }

    @Operation(summary = "Xoá địa chỉ")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long addressId) {
        Long userId = userService.getCurrentUserId();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã xoá địa chỉ"));
    }
}