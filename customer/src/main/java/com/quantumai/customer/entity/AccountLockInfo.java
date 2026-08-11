package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AccountLockInfo {

  @Id private String id;
  private String customerEmail;
  private Boolean lockedStatus;
  private Integer incorrectAttemptCount;

  private String createdBy;
  private String lastUpdatedBy;
}
