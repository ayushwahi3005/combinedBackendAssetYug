package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerMandatoryFields;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;





public interface CompanyCustomerMandatoryFieldsRepository extends MongoRepository<CompanyCustomerMandatoryFields,String> {
	public Optional<CompanyCustomerMandatoryFields> findByNameAndCompanyId(String name,String companyId);
	public List<CompanyCustomerMandatoryFields> findByCompanyId(String companyId);

}
