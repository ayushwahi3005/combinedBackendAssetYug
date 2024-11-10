package com.quantumai.customer.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetCheckInOutDetails {
	private String status;
	private LocalDate date;
	private String employee;
	private String notes;
	private String location;
	private String companyId;
}
