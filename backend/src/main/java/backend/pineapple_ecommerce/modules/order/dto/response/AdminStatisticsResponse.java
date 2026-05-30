package backend.pineapple_ecommerce.modules.order.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatisticsResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalUsers;
    private long pendingFarms;
    
    private double revenueChangePercentage;
    private double orderChangePercentage;
    
    private List<MonthlyRevenue> monthlyRevenueList;
    private Map<String, Long> orderStatusDistribution;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyRevenue {
        private String month;
        private BigDecimal revenue;
        private long orderCount;
    }
}
