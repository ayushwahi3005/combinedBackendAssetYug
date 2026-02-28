package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.CompanyCustomerExtraFieldIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CompanyCustomerExtraFieldIdGeneratorRepository extends MongoRepository<CompanyCustomerExtraFieldIdGenerator, String>, CompanyScopedRepository {
    Optional<CompanyCustomerExtraFieldIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
