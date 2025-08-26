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
public class SubscriptionAnalyticsDTO {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private TimePeriod timePeriod;
    private int newSubscriptions;
    private int cancelledSubscriptions;
    private int activeSubscriptions;
    private double churnRate; // Percentage of lost subscribers
    private BigDecimal revenue; // Total revenue for the period
    private BigDecimal mrr; // Monthly Recurring Revenue
    private BigDecimal arr; // Annual Recurring Revenue (MRR * 12)
    
    public enum TimePeriod {
        DAILY,
        WEEKLY,
        MONTHLY,
        QUARTERLY,
        YEARLY
    }
}
