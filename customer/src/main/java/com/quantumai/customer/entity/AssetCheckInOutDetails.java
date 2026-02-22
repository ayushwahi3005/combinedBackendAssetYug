package com.quantumai.customer.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AssetCheckInOutDetails {
  private String status;
  private LocalDateTime date;
  private String employee;
  private String notes;
  private String location;
  private Long companyId;
  private LocalDateTime updateTime;
  private String userLongitude;
  private String userLatitude;
  private String ipAddress;
  private String userLocation;

  // Overloaded setters to accept LocalDate or LocalDateTime
  public void setDate(LocalDate localDate) {
    this.date = localDate == null ? null : localDate.atStartOfDay();
  }

  public void setDate(LocalDateTime localDateTime) {
    this.date = localDateTime;
  }
}
