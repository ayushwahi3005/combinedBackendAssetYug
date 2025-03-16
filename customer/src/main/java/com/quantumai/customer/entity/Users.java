package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Users {

  @Id private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String title;
  private String companyId;
  private String mobileNumber;
  private String password;
  private String role;
}
