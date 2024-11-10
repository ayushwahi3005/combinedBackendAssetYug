package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerExtraFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;





public interface CompanyCustomerExtraFieldsRepository extends MongoRepository<CompanyCustomerExtraFields,String> {
	public List<CompanyCustomerExtraFields>findByCompanyCustomerId(String workorderId);
	public List<CompanyCustomerExtraFields> findByCompanyId(String companyId);
	public List<CompanyCustomerExtraFields> findByName(String name);
	public CompanyCustomerExtraFields findByNameAndCompanyCustomerId(String name, String companyCustomerId);
     public CompanyCustomerExtraFields findByCompanyCustomerIdAndCompanyId(Integer companyCustomerId,String id);

}
