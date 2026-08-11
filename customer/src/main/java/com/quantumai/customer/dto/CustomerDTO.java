package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class CustomerDTO {

  private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String companyName;
  private String mobileNumber;
  private String password;
  private Long companyId;
  private String role;

  private String createdBy;
  private String lastUpdatedBy;
}
