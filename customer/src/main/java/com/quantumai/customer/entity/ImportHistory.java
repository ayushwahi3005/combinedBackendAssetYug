package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class ImportHistory {

  @Id private String id;
  private String fileName;
  private String date;
  private String status;
  private Long complete;
  private String recordType;
  private String message;
  private String executedBy;
  private Long companyId;
}
