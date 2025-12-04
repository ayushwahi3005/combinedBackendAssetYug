package com.quantumai.customer.entity;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
@AllArgsConstructor
@NoArgsConstructor
public class Subscription {

  @Id private String id;
  private Long companyId;
  private SubscriptionEnum status;
  private String plan;
  private Integer person;
  private LocalDate subscriptionDate;
  private LocalDate expiryDate;
  private SubscriptionPlan subscriptionPlan;
  private Double amount;
  private String stripeSubscriptionId;
  private String stripeCustomerId;
  private String subscriptionName;


}
