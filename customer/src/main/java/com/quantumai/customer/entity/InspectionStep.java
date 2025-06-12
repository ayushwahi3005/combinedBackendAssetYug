package com.quantumai.customer.entity;

import lombok.Data;

@Data
public class InspectionStep {

  private String id;
  private long stepNumber;
  private String name;
  private InspectionStepType type;
}
