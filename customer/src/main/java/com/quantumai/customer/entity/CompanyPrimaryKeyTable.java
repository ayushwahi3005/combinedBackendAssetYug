package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class CompanyPrimaryKeyTable {

  @Id private String id;
  private Long seq;
}
