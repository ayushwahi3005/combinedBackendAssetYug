package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class CategoryDTO {

  private String id;
  private Long companyCustomerCategoryId;
  private Long assetCategoryId;
  private String name;
  private String status;
  private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
