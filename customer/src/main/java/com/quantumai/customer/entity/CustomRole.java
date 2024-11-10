package com.quantumai.customer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class CustomRole {
	
	@Id
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
