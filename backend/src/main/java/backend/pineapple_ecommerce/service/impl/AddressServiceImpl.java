package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateAddressRequest;
import backend.pineapple_ecommerce.dto.response.AddressResponse;
import backend.pineapple_ecommerce.entity.Address;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.exception.BusinessException;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.AddressMapper;
import backend.pineapple_ecommerce.repository.AddressRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 5;

    private final AddressRepository addressRepository;
    private final UserRepository    userRepository;
    private final AddressMapper     addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, CreateAddressRequest request) {
        // Giới hạn số địa chỉ
        int count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BusinessException(
                    "Mỗi tài khoản chỉ được tối đa " + MAX_ADDRESSES_PER_USER + " địa chỉ");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Nếu isDefault = true → bỏ default tất cả địa chỉ cũ trong cùng transaction
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unsetAllDefaults(userId);
        }

        // Nếu đây là địa chỉ đầu tiên → tự động làm default
        boolean isFirst = count == 0;
        Address address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setIsDefault(isFirst || Boolean.TRUE.equals(request.getIsDefault()));

        Address saved = addressRepository.save(address);
        log.info("Address added: userId={}, addressId={}", userId, saved.getId());
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, CreateAddressRequest request) {
        Address address = findAddressAndVerifyOwner(addressId, userId);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            unsetAllDefaults(userId);
        }

        addressMapper.updateFromRequest(request, address);

        // isDefault cần gán riêng vì mapper ignore nó khi update
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        // Bỏ default tất cả → set default cho địa chỉ được chọn (trong 1 transaction)
        unsetAllDefaults(userId);

        Address address = findAddressAndVerifyOwner(addressId, userId);
        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        log.info("Default address set: userId={}, addressId={}", userId, addressId);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = findAddressAndVerifyOwner(addressId, userId);

        // Không cho xoá địa chỉ mặc định nếu còn địa chỉ khác (UX tốt hơn)
        if (Boolean.TRUE.equals(address.getIsDefault())
                && addressRepository.countByUserId(userId) > 1) {
            throw new BusinessException(
                    "Không thể xoá địa chỉ mặc định. Hãy đặt địa chỉ khác làm mặc định trước");
        }

        addressRepository.delete(address);
        log.info("Address deleted: userId={}, addressId={}", userId, addressId);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Address findAddressAndVerifyOwner(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        if (!address.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Địa chỉ không thuộc về tài khoản của bạn");
        }
        return address;
    }

    /**
     * Bỏ isDefault của tất cả địa chỉ thuộc userId.
     * Phải gọi trong transaction để đảm bảo consistency.
     */
    private void unsetAllDefaults(Long userId) {
        List<Address> defaults = addressRepository.findByUserId(userId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .toList();

        defaults.forEach(a -> {
            a.setIsDefault(false);
            addressRepository.save(a);
        });
    }
}
