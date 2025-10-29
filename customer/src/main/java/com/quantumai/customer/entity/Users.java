package com.quantumai.customer.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Data
public class Users {

  @Id private String id;
  private LocalDateTime lastLogin;
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
