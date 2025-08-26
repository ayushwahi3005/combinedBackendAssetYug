package com.quantumai.customer.service;

import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import com.quantumai.customer.repository.SubscriptionRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionAnalyticsService {
    @Autowired
    private SubscriptionRepositoryImpl subscriptionRepositoryImpl;

    public List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end) {
        return subscriptionRepositoryImpl.getSubscriptionGrowth(start, end);
    }
}
