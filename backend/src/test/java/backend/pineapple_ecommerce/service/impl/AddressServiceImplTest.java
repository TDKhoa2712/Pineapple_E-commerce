package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.modules.address.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.modules.address.dto.response.AddressResponse;
import backend.pineapple_ecommerce.modules.address.models.Address;
import backend.pineapple_ecommerce.modules.address.service.AddressServiceImpl;
import backend.pineapple_ecommerce.modules.user.models.User;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import backend.pineapple_ecommerce.common.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.common.exception.UnauthorizedException;
import backend.pineapple_ecommerce.modules.address.mapper.AddressMapper;
import backend.pineapple_ecommerce.modules.address.repository.AddressRepository;
import backend.pineapple_ecommerce.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressServiceImpl")
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository    userRepository;
    @Mock private AddressMapper     addressMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    // ── Fixtures ──────────────────────────────────────────────────────

    private static final Long USER_ID    = 1L;
    private static final Long ADDRESS_ID = 10L;

    private User    user;
    private Address address;

    @BeforeEach
    void setUp() {
        user = User.builder().id(USER_ID).email("user@example.com").build();

        address = Address.builder()
                .id(ADDRESS_ID)
                .user(user)
                .receiverName("Nguyen Van A")
                .phone("0901234567")
                .province("Hà Nội")
                .district("Hoàn Kiếm")
                .ward("Tràng Tiền")
                .detail("1 Đinh Tiên Hoàng")
                .isDefault(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // getMyAddresses
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyAddresses()")
    class GetMyAddresses {

        @Test
        @DisplayName("trả về danh sách địa chỉ của user")
        void givenUserId_shouldReturnMappedList() {
            AddressResponse resp = AddressResponse.builder().build();
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(address));
            when(addressMapper.toResponse(address)).thenReturn(resp);

            List<AddressResponse> result = addressService.getMyAddresses(USER_ID);

            assertThat(result).hasSize(1).containsExactly(resp);
        }

        @Test
        @DisplayName("user không có địa chỉ nào → trả về danh sách rỗng")
        void givenNoAddresses_shouldReturnEmptyList() {
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of());

            assertThat(addressService.getMyAddresses(USER_ID)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // addAddress
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAddress()")
    class AddAddress {

        private CreateAddressRequest buildRequest(Boolean isDefault) {
            CreateAddressRequest req = new CreateAddressRequest();
            req.setReceiverName("Nguyen Van A");
            req.setPhone("0901234567");
            req.setProvince("Hà Nội");
            req.setDistrict("Hoàn Kiếm");
            req.setWard("Tràng Tiền");
            req.setDetail("1 Đinh Tiên Hoàng");
            req.setIsDefault(isDefault);
            return req;
        }

        @Test
        @DisplayName("thêm địa chỉ thành công → trả về AddressResponse")
        void givenValidRequest_shouldReturnAddressResponse() {
            CreateAddressRequest req = buildRequest(false);
            AddressResponse expected = AddressResponse.builder().build();

            when(addressRepository.countByUserId(USER_ID)).thenReturn(1);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(addressMapper.toEntity(req)).thenReturn(address);
            when(addressRepository.save(address)).thenReturn(address);
            when(addressMapper.toResponse(address)).thenReturn(expected);

            AddressResponse result = addressService.addAddress(USER_ID, req);

            assertThat(result).isSameAs(expected);
        }

        @Test
        @DisplayName("địa chỉ đầu tiên → tự động đặt làm mặc định")
        void givenFirstAddress_shouldBeSetAsDefault() {
            CreateAddressRequest req = buildRequest(false);

            when(addressRepository.countByUserId(USER_ID)).thenReturn(0);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(addressMapper.toEntity(req)).thenReturn(address);
            when(addressRepository.save(address)).thenReturn(address);
            when(addressMapper.toResponse(address)).thenReturn(AddressResponse.builder().build());

            addressService.addAddress(USER_ID, req);

            assertThat(address.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("isDefault = true → bỏ mặc định các địa chỉ cũ")
        void givenIsDefaultTrue_shouldUnsetOtherDefaults() {
            CreateAddressRequest req = buildRequest(true);
            Address oldDefault = Address.builder()
                    .id(99L).user(user).isDefault(true).build();

            when(addressRepository.countByUserId(USER_ID)).thenReturn(1);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(oldDefault));
            when(addressMapper.toEntity(req)).thenReturn(address);
            when(addressRepository.save(any())).thenReturn(address);
            when(addressMapper.toResponse(address)).thenReturn(AddressResponse.builder().build());

            addressService.addAddress(USER_ID, req);

            assertThat(oldDefault.getIsDefault()).isFalse();
            verify(addressRepository, atLeastOnce()).save(oldDefault);
        }

        @Test
        @DisplayName("đã đủ 5 địa chỉ → ném BusinessException")
        void givenMaxAddressesReached_shouldThrowBusinessException() {
            CreateAddressRequest req = buildRequest(false);
            when(addressRepository.countByUserId(USER_ID)).thenReturn(5);

            assertThatThrownBy(() -> addressService.addAddress(USER_ID, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("tối đa");
        }

        @Test
        @DisplayName("user không tồn tại → ném ResourceNotFoundException")
        void givenUnknownUser_shouldThrowResourceNotFoundException() {
            CreateAddressRequest req = buildRequest(false);
            when(addressRepository.countByUserId(USER_ID)).thenReturn(0);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.addAddress(USER_ID, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // setDefault
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setDefault()")
    class SetDefault {

        @Test
        @DisplayName("đặt địa chỉ làm mặc định thành công")
        void givenOwnedAddress_shouldSetDefaultAndReturn() {
            Address oldDefault = Address.builder()
                    .id(99L).user(user).isDefault(true).build();
            AddressResponse expected = AddressResponse.builder().build();

            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of(oldDefault, address));
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
            when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(addressMapper.toResponse(address)).thenReturn(expected);

            AddressResponse result = addressService.setDefault(USER_ID, ADDRESS_ID);

            assertThat(result).isSameAs(expected);
            assertThat(address.getIsDefault()).isTrue();
            assertThat(oldDefault.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("địa chỉ không thuộc user → ném UnauthorizedException")
        void givenAddressOfOtherUser_shouldThrowUnauthorizedException() {
            User otherUser = User.builder().id(99L).build();
            Address foreignAddress = Address.builder()
                    .id(ADDRESS_ID).user(otherUser).isDefault(false).build();

            when(addressRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(foreignAddress));

            assertThatThrownBy(() -> addressService.setDefault(USER_ID, ADDRESS_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // deleteAddress
    // ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAddress()")
    class DeleteAddress {

        @Test
        @DisplayName("xoá địa chỉ không phải mặc định → thành công")
        void givenNonDefaultAddress_shouldDelete() {
            address.setIsDefault(false);
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));

            addressService.deleteAddress(USER_ID, ADDRESS_ID);

            verify(addressRepository).delete(address);
        }

        @Test
        @DisplayName("xoá địa chỉ mặc định khi còn địa chỉ khác → ném BusinessException")
        void givenDefaultAddressWithOthersExisting_shouldThrow() {
            address.setIsDefault(true);
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
            when(addressRepository.countByUserId(USER_ID)).thenReturn(2);

            assertThatThrownBy(() -> addressService.deleteAddress(USER_ID, ADDRESS_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mặc định");
        }

        @Test
        @DisplayName("xoá địa chỉ mặc định duy nhất → cho phép xoá")
        void givenOnlyDefaultAddress_shouldAllowDelete() {
            address.setIsDefault(true);
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(address));
            when(addressRepository.countByUserId(USER_ID)).thenReturn(1);

            addressService.deleteAddress(USER_ID, ADDRESS_ID);

            verify(addressRepository).delete(address);
        }

        @Test
        @DisplayName("địa chỉ không tồn tại → ném ResourceNotFoundException")
        void givenMissingAddress_shouldThrow() {
            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> addressService.deleteAddress(USER_ID, ADDRESS_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("địa chỉ thuộc user khác → ném UnauthorizedException")
        void givenForeignAddress_shouldThrowUnauthorizedException() {
            User other = User.builder().id(99L).build();
            Address foreignAddr = Address.builder()
                    .id(ADDRESS_ID).user(other).isDefault(false).build();

            when(addressRepository.findById(ADDRESS_ID)).thenReturn(Optional.of(foreignAddr));

            assertThatThrownBy(() -> addressService.deleteAddress(USER_ID, ADDRESS_ID))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }
}