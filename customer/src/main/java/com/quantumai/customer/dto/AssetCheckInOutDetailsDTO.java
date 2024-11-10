package com.quantumai.customer.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetCheckInOutDetailsDTO {
	private String status;
	private LocalDate date;
	private String employee;
	private String notes;
	private String location;
	private String companyId;
}
