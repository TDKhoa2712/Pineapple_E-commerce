package backend.pineapple_ecommerce.service.impl;

import backend.pineapple_ecommerce.dto.request.CreateFarmRequest;
import backend.pineapple_ecommerce.dto.response.FarmResponse;
import backend.pineapple_ecommerce.dto.response.PageResponse;
import backend.pineapple_ecommerce.entity.Farm;
import backend.pineapple_ecommerce.entity.User;
import backend.pineapple_ecommerce.exception.ResourceNotFoundException;
import backend.pineapple_ecommerce.exception.UnauthorizedException;
import backend.pineapple_ecommerce.mapper.FarmMapper;
import backend.pineapple_ecommerce.repository.FarmRepository;
import backend.pineapple_ecommerce.repository.UserRepository;
import backend.pineapple_ecommerce.service.FarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmServiceImpl implements FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final FarmMapper     farmMapper;

    @Override
    @Transactional
    public FarmResponse createFarm(Long ownerId, CreateFarmRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        Farm farm = farmMapper.toEntity(request);
        farm.setOwner(owner);

        Farm saved = farmRepository.save(farm);
        log.info("Farm created: id={}, owner={}", saved.getId(), ownerId);
        return farmMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long farmId) {
        return farmMapper.toResponse(findById(farmId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FarmResponse> getAllFarms(int page, int size) {
        var result = farmRepository
                .findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(farmMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmResponse> getMyFarms(Long ownerId) {
        return farmRepository.findByOwnerId(ownerId).stream()
                .map(farmMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public FarmResponse updateFarm(Long farmId, Long requesterId, CreateFarmRequest request) {
        Farm farm = findById(farmId);
        verifyOwnership(farm, requesterId);

        farmMapper.updateFromRequest(request, farm);
        Farm saved = farmRepository.save(farm);
        log.info("Farm updated: id={}", farmId);
        return farmMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteFarm(Long farmId, Long requesterId) {
        Farm farm = findById(farmId);
        verifyOwnership(farm, requesterId);
        farmRepository.delete(farm);
        log.info("Farm deleted: id={}", farmId);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private Farm findById(Long farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", farmId));
    }

    /**
     * Kiểm tra quyền sở hữu.
     * Admin bypass bằng @PreAuthorize ở tầng Controller — ở đây chỉ check owner.
     */
    private void verifyOwnership(Farm farm, Long requesterId) {
        if (!farm.getOwner().getId().equals(requesterId)) {
            throw new UnauthorizedException("Bạn không có quyền thao tác trang trại này");
        }
    }
}
