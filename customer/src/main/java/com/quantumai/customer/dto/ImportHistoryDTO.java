package com.quantumai.customer.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImportHistoryDTO {

  private String id;
  private String fileName;
  private LocalDateTime date;
  private String status;
  private Long complete;
  private String recordType;
  private String message;
  private String executedBy;
  private Long companyId;
  private Boolean hasErrorReport;
  private String errorReportFileName;

  private String createdBy;
  private String lastUpdatedBy;
}
