package com.quantumai.customer.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class CompanyCustomerIdTable {

	@Id
	private String id;
	private int tableId;
	private String companyId;
	
	public void updateId() {
		this.tableId+=1;
	}
}
