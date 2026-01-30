package com.quantumai.customer.entity;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Payment {

  @Id private String id;

  private String paymentId;

  private Double amount;

  private String currency;

  private PaymentStatus paymentStatus;

  private String cardholderName;

  private LocalDateTime transactionDate;

  private PaymentType paymentType; // e.g., CREDIT_CARD, DEBIT_CARD

  private String description;

  private SubscriptionPlan planSelected;

  private Long companyId;

  private Integer person;

  private LocalDateTime startDate;

  private LocalDateTime endDate;

  private String paymentIntentId;
  private String chargeId;
  private String invoiceId;
}
