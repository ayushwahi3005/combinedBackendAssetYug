package com.quantumai.customer.entity;

import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
public class ImportHistory {

  @Id private String id;
  private String fileName;
  private LocalDateTime date;
  private String status;
  private Long complete;
  private ImportHistoryRecordType recordType;
  private String message;
  private String executedBy;
  private Long companyId;
  private Boolean hasErrorReport;
  private String errorReportFileName;
  private byte[] errorReportFile;

  private String createdBy;
  private String lastUpdatedBy;
}
