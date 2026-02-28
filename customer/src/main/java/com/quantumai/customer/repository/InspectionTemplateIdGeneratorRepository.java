package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.InspectionTemplateIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InspectionTemplateIdGeneratorRepository extends MongoRepository<InspectionTemplateIdGenerator,String>, CompanyScopedRepository {

    Optional<InspectionTemplateIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
