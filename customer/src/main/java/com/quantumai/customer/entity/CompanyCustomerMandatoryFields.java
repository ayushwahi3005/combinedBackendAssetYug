package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyCustomerMandatoryFields {

	@Id
	private String id;
	private String name;
	private boolean mandatory;
	private String email;
	private String type;
	private String companyId;
}
