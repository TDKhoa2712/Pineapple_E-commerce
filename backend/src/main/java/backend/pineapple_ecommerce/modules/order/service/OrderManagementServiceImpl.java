package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.modules.order.dto.request.BulkOrderStatusRequest;
import backend.pineapple_ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManagementServiceImpl implements OrderManagementService {

    private final OrderService orderService;   // Inject interface là đúng

    @Override
    public int bulkUpdateStatus(BulkOrderStatusRequest request) {
        if (request.getOrderIds() == null || request.getOrderIds().isEmpty()) {
            return 0;
        }
        if (request.getOrderIds().size() > 100) {
            throw new BusinessException("Không thể cập nhật cùng lúc quá 100 đơn hàng");
        }

        AtomicInteger successCount = new AtomicInteger(0);

        request.getOrderIds().forEach(orderId -> {
            try {
                orderService.updateOrderStatus(orderId, request.getNewStatus());
                successCount.incrementAndGet();
            } catch (Exception e) {
                log.warn("Bulk update skipped orderId={}: {}", orderId, e.getMessage());
            }
        });

        log.info("Bulk status update: {}/{} orders updated to {}",
                successCount.get(), request.getOrderIds().size(), request.getNewStatus());

        return successCount.get();
    }
}