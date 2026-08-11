package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class UserExtraFieldNameDTO {

  private String id;
  private String name;
  private String type;
  private String email;
  private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
