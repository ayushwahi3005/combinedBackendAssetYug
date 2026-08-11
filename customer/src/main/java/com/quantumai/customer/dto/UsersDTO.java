package com.quantumai.customer.dto;

import com.quantumai.customer.entity.StatusEnum;
import com.quantumai.customer.entity.UserStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsersDTO {
  private String id;
  private Long userId;
  private LocalDateTime lastLogin;
  private String firstName;
  private String lastName;
  private String email;
  private Long companyId;
  private String mobileNumber;
  private String password;
  private UserStatusEnum status;
  private String title;
  private CustomRoleDTO role;

  private String createdBy;
  private String lastUpdatedBy;
}
