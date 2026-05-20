package backend.pineapple_ecommerce.service;

import backend.pineapple_ecommerce.dto.response.VNPayQueryResult;
import backend.pineapple_ecommerce.entity.Payment;

public interface VNPayQueryService {
    VNPayQueryResult queryTransaction(Payment payment);
}
