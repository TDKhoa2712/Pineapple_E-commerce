package backend.pineapple_ecommerce.infrastructure.vnpay;

import backend.pineapple_ecommerce.modules.payment.dto.response.VNPayQueryResult;
import backend.pineapple_ecommerce.modules.payment.models.Payment;

public interface VNPayQueryService {
    VNPayQueryResult queryTransaction(Payment payment);
}
