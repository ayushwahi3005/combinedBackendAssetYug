package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class UserExtraFields {
  @Id private String id;
  private String email;
  private String name;
  private String value;
  private String userId;
  private String type;
  private Long companyId;
}
