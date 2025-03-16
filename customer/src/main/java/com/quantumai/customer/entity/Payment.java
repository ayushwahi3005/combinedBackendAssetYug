package com.quantumai.customer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Payment {

  @Id private String id;

  private Double amount;

  private String currency;

  private PaymentStatus paymentStatus;

  private String cardholderName;

  private LocalDateTime transactionDate;

  private PaymentType paymentType; // e.g., CREDIT_CARD, DEBIT_CARD

  private String description;

  private String companyId;
}
