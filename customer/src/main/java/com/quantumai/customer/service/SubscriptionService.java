package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Subscription;
import java.util.List;

public interface SubscriptionService {
  public void addSubscription(SubscriptionDTO subscriptionDTO);

  public void updateSubscription(SubscriptionDTO subscriptionDTO);

  public void isExpired();

  public void addPayment(Payment payment);

  public void updatePayment(Payment payment);

  public List<Payment> getAllPayment(String companyId);

  public void addPlan(Plans plans);

  public void updatePlan(Plans plans);

  public void deletePlan(String id);

  public Plans getPlan(String id);

  public List<Plans> getAllPlan();

  public Subscription getCurrentSubscription(String companyId);
}
