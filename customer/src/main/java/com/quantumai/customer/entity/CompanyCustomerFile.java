package com.quantumai.customer.entity;

import lombok.Data;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyCustomerFile {

  @Id private String id;
  private String companyCustomerId;
  private String fileName;
  private byte[] file;
  private LocalDateTime uploadDateTime;
    private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
