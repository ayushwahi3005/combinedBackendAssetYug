package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyInformation {

  @Id private Long id;
  private String customerEmail;
  private String companyName;
  private String comapanyLogo;
  private String country;
  //	private String accountId;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String zipCode;
  private String phoneNo;
  private String website;

  private String createdBy;
  private String lastUpdatedBy;
}
