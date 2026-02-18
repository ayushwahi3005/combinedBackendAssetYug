package com.quantumai.customer.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InspectionCompletedCountPerDayDTO {
    LocalDate date;
    Long count;
}
