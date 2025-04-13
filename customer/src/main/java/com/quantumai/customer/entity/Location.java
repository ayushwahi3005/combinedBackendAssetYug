package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Location {

  @Id private String id;
  private String companyId;
  private String name;
  private String parentLocation;
  private String address;
  private String apartment;
  private String city;
  private String state;
  private StatusEnum status;
  private Integer zipCode;
}
