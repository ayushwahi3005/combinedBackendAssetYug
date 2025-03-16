package com.quantumai.customer.dto;

import com.quantumai.customer.entity.SubscriptionEnum;
import com.quantumai.customer.entity.SubscriptionPlan;
import java.time.LocalDate;
import lombok.Data;

@Data
public class SubscriptionDTO {

  private String id;
  private String companyId;
  private SubscriptionEnum status;
  private String plan;
  private Integer person;
  private LocalDate subscriptionDate;
  private LocalDate expiryDate;
  private SubscriptionPlan subscriptionPlan;
  private Double amount;
}
