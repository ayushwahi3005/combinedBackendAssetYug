package com.quantumai.customer.entity;

import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CustomerImportColumnMapping {

  @Id private String id;
  private Long companyId;
  private String name;
  private ImportHistoryRecordType recordType;
  private Map<String, String> columnMappings;
  private String createdBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
