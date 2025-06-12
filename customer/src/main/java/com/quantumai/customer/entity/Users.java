package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Data
public class Users {

  @Id private String id;
  private String firstName;
  private String lastName;
  private String email;
  private String title;
  private Long companyId;
  private String mobileNumber;
  private String password;
  private StatusEnum status;
  @DBRef private CustomRole role;
}
