package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class ImportHistoryDTO {

  private String id;
  private String fileName;
  private String date;
  private String status;
  private Long complete;
  private String recordType;
  private String message;
  private String executedBy;
  private Long companyId;
}
