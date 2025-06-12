package com.quantumai.customer.dto;

import com.quantumai.customer.entity.StatusEnum;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class BinDTO {

  String id;
  ObjectId locationId;
  String locationName;
  String binNumber;
  private StatusEnum status;
  Long companyId;
}
