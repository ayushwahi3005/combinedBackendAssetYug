package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionDTO;
import com.quantumai.customer.entity.Payment;

public interface SubscriptionService {
    public void addSubscription(SubscriptionDTO subscriptionDTO);
    public void updateSubscription(SubscriptionDTO subscriptionDTO);
    public void isExpired();
    public void addPayment(Payment payment);
    public void updatePayment(Payment payment);
}
