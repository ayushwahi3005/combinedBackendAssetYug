package com.quantumai.customer.entity;

import java.time.LocalDate;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Subscription {

  @Id private String id;
  private String companyId;
  private SubscriptionEnum status;
  private String plan;
  private Integer person;
  private LocalDate subscriptionDate;
  private LocalDate expiryDate;
  private SubscriptionPlan subscriptionPlan;
  private Double amount;
}
