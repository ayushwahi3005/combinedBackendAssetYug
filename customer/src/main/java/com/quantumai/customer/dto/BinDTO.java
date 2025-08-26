package com.quantumai.customer.dto;

import com.quantumai.customer.entity.StatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinDTO {

  String id;
  String locationId;
  String locationName;
  String binNumber;
  private StatusEnum status;
  Long companyId;
}
