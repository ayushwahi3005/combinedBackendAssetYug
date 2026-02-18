package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Bin {
  @Id 
  String id;
  private Long binId;
  
  @DBRef
  private Location locationId;  // Changed from ObjectId to Location
  
  String binNumber;
  private StatusEnum status;
  Long companyId;
}
