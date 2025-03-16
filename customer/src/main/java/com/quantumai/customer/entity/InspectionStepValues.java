package com.quantumai.customer.entity;

import lombok.Data;

@Data
public class InspectionStepValues {

    private String id;
    private String name;
    private String inspectionStepId;
    private InspectionStepType type;
    private String value;
}
