package com.quantumai.customer.repository;


import com.quantumai.customer.entity.IdGenerator.CompanyCustomerCategoryIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompanyCustomerCategoryIdGeneratorRepository extends MongoRepository<CompanyCustomerCategoryIdGenerator,String> {
    Optional<CompanyCustomerCategoryIdGenerator> findByCompanyId(Long companyId);
}
