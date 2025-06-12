package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyCustomerExtraFields {

  @Id private String id;

  private String name;
  private String value;
  private String companyCustomerId;
  private String type;
  private Long companyId;
}
