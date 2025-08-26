package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Subscription;
import java.util.List;

import com.quantumai.customer.dto.MrrDTO;
import com.quantumai.customer.dto.RevenueTrendDTO;
import com.quantumai.customer.dto.SubscriptionAnalyticsDTO;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import java.time.LocalDate;
import java.util.List;

public interface SubscriptionService {
  public void addSubscription(SubscriptionDTO subscriptionDTO);

  public void updateSubscription(SubscriptionDTO subscriptionDTO);

  public void isExpired();

  public void addPayment(Payment payment);

  public void updatePayment(Payment payment);

  public List<Payment> getAllPayment(Long companyId);

  public void addPlan(Plans plans);

  public void updatePlan(Plans plans);

  public void deletePlan(String id);

  public Plans getPlan(String id);

  public List<Plans> getAllPlan();

  public Subscription getCurrentSubscription(Long companyId);

  public List<Subscription> getAllSubscription(Long companyId);

  public void deleteUpcomingSubscription(Long companyId, String companyName, String email)
      throws Exception;

  public void startUpcomingSubscription(Long companyId, String companyName, String email)
      throws Exception;

    /**
     * Returns subscription growth analytics between start and end dates (inclusive).
     */
    List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end);
    
    /**
     * Returns detailed subscription analytics for the given time period
     */
    List<SubscriptionAnalyticsDTO> getSubscriptionAnalytics(LocalDate start, LocalDate end, SubscriptionAnalyticsDTO.TimePeriod period);
    
    /**
     * Calculates the churn rate between two dates as a percentage
     */
    double calculateChurnRate(LocalDate start, LocalDate end);
    
    /**
     * Returns revenue trends over time for the specified period
     */
    List<RevenueTrendDTO> getRevenueTrends(LocalDate start, LocalDate end, SubscriptionAnalyticsDTO.TimePeriod period);
    
    /**
     * Returns Monthly Recurring Revenue (MRR) trend data
     */
    List<MrrDTO> getMrrTrend(LocalDate start, LocalDate end);

}
