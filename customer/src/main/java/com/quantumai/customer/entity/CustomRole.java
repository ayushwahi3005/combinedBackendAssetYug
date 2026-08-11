package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CustomRole {

  @Id private String id;
  private Long customRoleId;
  private String name;
  private String status;
  private RoleType type;
  private CustomRoleType assets;
  private CustomRoleType customers;
  private CustomRoleType workOrders;
  private CustomRoleType users;
  private CustomRoleType roleAndPermissions;
  private CustomRoleType imports;
  private CustomRoleType category;
  private CustomRoleType inventory;
  private CustomRoleType inspections;
  private Long companyId;

  private String createdBy;
  private String lastUpdatedBy;
}
