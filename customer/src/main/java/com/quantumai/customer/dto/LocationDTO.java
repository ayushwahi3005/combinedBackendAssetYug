package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class LocationDTO {
  private String id;
  private Long companyId;
  private String name;
  private String parentLocation;
  private String parentLocationName;
  private String address;
  private String apartment;
  private String city;
  private String country;
  private String state;
  private String status;
  private String zipCode;
}
