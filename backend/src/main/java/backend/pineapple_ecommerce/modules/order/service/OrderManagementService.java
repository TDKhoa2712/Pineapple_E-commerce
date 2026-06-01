package backend.pineapple_ecommerce.modules.order.service;

import backend.pineapple_ecommerce.modules.order.dto.request.BulkOrderStatusRequest;

/**
 * Service chuyên xử lý các nghiệp vụ quản trị, bulk operation
 */
public interface OrderManagementService {

    int bulkUpdateStatus(BulkOrderStatusRequest request);
}