package com.quantumai.customer.repository;


import com.quantumai.customer.entity.CompanyCustomerExtraFieldName;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CompanyCustomerExtraFieldNameRepository extends MongoRepository<CompanyCustomerExtraFieldName,String>{
	public CompanyCustomerExtraFieldName findByName(String name);
	public List<CompanyCustomerExtraFieldName> findByCompanyId(String companyId);
	public CompanyCustomerExtraFieldName findByNameAndCompanyId(String name, String companyId);
}
