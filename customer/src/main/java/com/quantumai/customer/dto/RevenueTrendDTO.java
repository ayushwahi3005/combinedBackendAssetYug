package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTrendDTO {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private SubscriptionAnalyticsDTO.TimePeriod timePeriod;
    private BigDecimal totalRevenue;
    private BigDecimal recurringRevenue;
    private BigDecimal oneTimeRevenue;
    private int totalTransactions;
}
