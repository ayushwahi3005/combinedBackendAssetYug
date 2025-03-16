package com.quantumai.customer.dto;

import com.quantumai.customer.entity.StatusEnum;
import lombok.Data;

@Data
public class BinDTO {

  String id;
  String location;
  String binNumber;
  private StatusEnum status;
  String companyId;
}
