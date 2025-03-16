package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class AssetQR {

  @Id private String id;
  private String type;
  private String custom;
  private String optional;
  private String companyId;
}
