package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class CompanyCustomerDTO {

  private String id;
  private Integer companyCustomerId;
  private String name;
  private Long companyId;
  private String category;
  private String status;
  private String phone;
  private String email;
  private String address;
  private String apartment;
  private String city;
  private String state;
  private Integer zipCode;
  private String updatedAt;
}
