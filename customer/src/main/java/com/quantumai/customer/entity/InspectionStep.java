package com.quantumai.customer.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class InspectionStep {

    private String id;
    private long stepNumber;
    private String name;
    private InspectionStepType type;


}
