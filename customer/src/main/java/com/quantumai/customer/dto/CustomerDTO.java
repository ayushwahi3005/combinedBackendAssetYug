package com.quantumai.customer.dto;

import com.quantumai.customer.entity.Role;

import lombok.Data;

@Data
public class CustomerDTO {
	
	private String id;
	private String firstName;
	private String lastName;
	private String email;
	private String companyName;
	private String mobileNumber;
	private String password;
	private String companyId;
	private String role;
	
	
	
}
