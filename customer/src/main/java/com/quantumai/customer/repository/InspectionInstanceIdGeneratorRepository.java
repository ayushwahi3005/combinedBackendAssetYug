package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.InspectionInstanceIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InspectionInstanceIdGeneratorRepository extends MongoRepository<InspectionInstanceIdGenerator,String>, CompanyScopedRepository {

    Optional<InspectionInstanceIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
