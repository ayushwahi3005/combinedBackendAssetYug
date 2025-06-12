package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CustomerStripeDetails {
  @Id private String id;
  private String firstName;
  private String lastName;
  private String paymentMethodId;
  private String customerId;
  private Long companyId;
  private String email;
}
