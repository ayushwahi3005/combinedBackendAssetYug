package com.quantumai.customer.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Bin {
  @Id String id;
  ObjectId locationId;
  String binNumber;
  private StatusEnum status;
  Long companyId;
}
