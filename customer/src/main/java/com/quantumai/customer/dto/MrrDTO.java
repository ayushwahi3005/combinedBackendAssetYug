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
public class MrrDTO {
    private LocalDate date;
    private BigDecimal mrrAmount;
    private int activeSubscriptions;
    private BigDecimal averageRevenuePerUser;
}
