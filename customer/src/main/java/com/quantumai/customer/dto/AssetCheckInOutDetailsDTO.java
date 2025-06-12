package com.quantumai.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AssetCheckInOutDetailsDTO {
  private String status;
  private LocalDate date;
  private String employee;
  private String notes;
  private String location;
  private Long companyId;
  private LocalDateTime updateTime;
}
