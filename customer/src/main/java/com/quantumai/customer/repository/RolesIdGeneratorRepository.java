package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.RolesIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RolesIdGeneratorRepository extends MongoRepository<RolesIdGenerator,String> {

    Optional<RolesIdGenerator> findByCompanyId(Long companyId);
}
