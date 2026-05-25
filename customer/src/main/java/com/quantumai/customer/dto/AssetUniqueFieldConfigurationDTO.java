package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AssetUniqueFieldConfigurationDTO {
  private String id;
  private Long companyId;
  private String fieldName;
  private Boolean isUnique;
  private String email;
  private String type; // STANDARD or EXTRA
  private String createdAt;
  private String updatedAt;
}
