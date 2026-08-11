package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Location {

  @Id
  private String id;
  private Long locationId;
  private Long companyId;
  private String name;
  private String parentLocation;
  private String parentLocationName;
  private String address;
  private String apartment;
  private String city;
  private String state;
  private String country;
  private StatusEnum status;
  private String zipCode;

  private String createdBy;
  private String lastUpdatedBy;
}
