package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class CompanyCustomerExtraFieldsDTO {
  private String id;

  private String name;
  private String value;
  private String companyCustomerId;
  private String type;
  private Long companyId;
}
