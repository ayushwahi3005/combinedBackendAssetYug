package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class AssetCategory {

  @Id private String id;
  private Long assetCategoryId;
  private String name;
  private String status;
  private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
