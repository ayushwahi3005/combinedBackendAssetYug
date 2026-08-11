package com.quantumai.customer.entity;

import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
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
  private String createdBy;
  private String lastUpdatedBy;

  public Subscription(String id, Long companyId, SubscriptionEnum status, String plan, Integer person,
                      LocalDate subscriptionDate, LocalDate expiryDate, SubscriptionPlan subscriptionPlan,
                      Double amount, String stripeSubscriptionId, String stripeCustomerId, String subscriptionName) {
    this.id = id;
    this.companyId = companyId;
    this.status = status;
    this.plan = plan;
    this.person = person;
    this.subscriptionDate = subscriptionDate;
    this.expiryDate = expiryDate;
    this.subscriptionPlan = subscriptionPlan;
    this.amount = amount;
    this.stripeSubscriptionId = stripeSubscriptionId;
    this.stripeCustomerId = stripeCustomerId;
    this.subscriptionName = subscriptionName;
  }
}
