package com.quantumai.customer.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document
public class CompanyInformation {
	
	@Id
	private String id;
	private String customerEmail;
	private String companyName;
	private String comapanyLogo;
//	private String accountId;
	private String address1;
	private String address2;
	private String city;
	private String state;
	private String zipCode;
	private String phoneNo;
	private String website;
	
}
