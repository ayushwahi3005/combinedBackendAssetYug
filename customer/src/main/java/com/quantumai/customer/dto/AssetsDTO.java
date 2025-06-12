package com.quantumai.customer.dto;

import lombok.Data;

@Data
public class AssetsDTO {

  private String email;
  private Integer assetId;
  private String id;
  private String name;
  private String serialNumber;
  private String category;
  private String customer;
  private String customerId;
  private String location;
  private String locationName;
  private String status;
  private String image;
  private Long companyId;
  private String updatedAt;
}
