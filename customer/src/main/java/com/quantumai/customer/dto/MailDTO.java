package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class MailDTO {

  private String firstName;
  private String lastName;
  private String phoneNumber;
  private String message;
  private String email;
  private String role;
}
