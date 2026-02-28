package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.LocationIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LocationIdGeneratorRepository extends MongoRepository<LocationIdGenerator,String>, CompanyScopedRepository {

    Optional<LocationIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
