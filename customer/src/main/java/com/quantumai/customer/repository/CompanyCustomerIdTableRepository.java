package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomerIdTable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;



public interface CompanyCustomerIdTableRepository extends MongoRepository<CompanyCustomerIdTable,String> {
	public Optional<CompanyCustomerIdTable> findByCompanyId(String id);
}
