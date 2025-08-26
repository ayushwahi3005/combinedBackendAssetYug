package com.quantumai.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionGrowthDTO {
    private LocalDate date;
    private long newSubscriptions;
    private long cancelledSubscriptions;
    private long netGrowth;
}
