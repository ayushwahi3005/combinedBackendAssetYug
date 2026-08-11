package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class UserExtraFieldsDTO {

  private String id;
  private String email;
  private String name;
  private String value;
  private String userId;
  private String type;
  private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
