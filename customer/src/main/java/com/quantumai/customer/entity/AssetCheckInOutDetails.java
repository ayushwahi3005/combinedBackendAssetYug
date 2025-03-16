package com.quantumai.customer.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AssetCheckInOutDetails {
  private String status;
  private LocalDate date;
  private String employee;
  private String notes;
  private String location;
  private String companyId;
  private LocalDateTime updateTime;
}
