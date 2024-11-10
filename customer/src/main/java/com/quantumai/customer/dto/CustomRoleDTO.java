package com.quantumai.customer.dto;

import com.quantumai.customer.entity.CustomRoleType;

import lombok.Data;

@Data
public class CustomRoleDTO {
	
	private String id;
	private String name;
	private String status;
	private CustomRoleType assets;
	private CustomRoleType customers;
	private CustomRoleType workOrders;
	private CustomRoleType users;
	private CustomRoleType roleAndPermissions;
	private CustomRoleType imports;
	private CustomRoleType category;
	private CustomRoleType inventory;
	private String companyId;
}
