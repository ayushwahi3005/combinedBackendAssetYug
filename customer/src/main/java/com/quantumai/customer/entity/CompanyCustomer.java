package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class CompanyCustomer {

  @Id private String id;
  private Integer companyCustomerId;
  private String name;
  private Long companyId;
  private String category;
  private String status;
  private String phone;
  private String email;
  private String address;
  private String apartment;
  private String city;
  private String state;
  private Integer zipCode;
  private String updatedAt;
}
