package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyCustomerCategory {

  @Id private String id;
  private String name;
  private String status;
  private Long companyId;
}
