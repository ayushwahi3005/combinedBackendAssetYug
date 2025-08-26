package com.quantumai.customer.repository;

import com.quantumai.customer.dto.MrrDTO;
import com.quantumai.customer.dto.RevenueTrendDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import com.quantumai.customer.entity.SubscriptionEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepositoryCustom {
    List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end);
    
    /**
     * Get comprehensive subscription analytics for a given time period
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @param period Time period for grouping (DAILY, WEEKLY, MONTHLY, QUARTERLY)
     * @return List of analytics data points
     */
    List<SubscriptionAnalyticsDTO> getSubscriptionAnalytics(
        LocalDate start, 
        LocalDate end, 
        SubscriptionAnalyticsDTO.TimePeriod period
    );
    
    /**
     * Calculate churn rate for a specific period
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return Churn rate as a percentage (0-100)
     */
    double calculateChurnRate(LocalDate start, LocalDate end);
    
    /**
     * Get revenue trends over time
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @param period Time period for grouping
     * @return List of revenue data points
     */
    List<RevenueTrendDTO> getRevenueTrends(
        LocalDate start,
        LocalDate end,
        SubscriptionAnalyticsDTO.TimePeriod period
    );
    
    /**
     * Get MRR (Monthly Recurring Revenue) trend
     * @param start Start date (inclusive)
     * @param end End date (inclusive)
     * @return List of MRR values over time
     */
    List<MrrDTO> getMrrTrend(LocalDate start, LocalDate end);
}
