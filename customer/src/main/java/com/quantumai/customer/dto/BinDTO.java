package com.quantumai.customer.dto;

import com.quantumai.customer.entity.StatusEnum;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BinDTO {

  String id;
  private Long binId;
  String locationId;
  String locationName;
  String binNumber;
  private StatusEnum status;
  Long companyId;
  private String createdBy;
  private String lastUpdatedBy;

  public BinDTO(String id, Long binId, String locationId, String locationName,
                String binNumber, StatusEnum status, Long companyId) {
    this.id = id;
    this.binId = binId;
    this.locationId = locationId;
    this.locationName = locationName;
    this.binNumber = binNumber;
    this.status = status;
    this.companyId = companyId;
  }
}
