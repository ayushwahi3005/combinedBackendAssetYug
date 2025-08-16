package com.quantumai.customer.entity;


import lombok.Data;
import java.util.List;

@Data
public class InspectionTemplateResult {

    private String inspectionName;
    private List<InspectionStepValues> stepValues;
}
