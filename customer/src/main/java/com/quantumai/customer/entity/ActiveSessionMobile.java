package com.quantumai.customer.entity;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class ActiveSessionMobile {

  @Id private String sessionId;
  private String userId;
  private String mobileId;
  private String userAgent;
  private LocalDateTime lastActivityTime;
}
