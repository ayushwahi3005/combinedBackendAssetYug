package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.CompanyCustomerCategoryIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerCategoryIdGeneratorRepository extends MongoRepository<CompanyCustomerCategoryIdGenerator,String> {
}
