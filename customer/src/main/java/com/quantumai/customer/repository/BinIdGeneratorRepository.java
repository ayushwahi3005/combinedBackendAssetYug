package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.BinIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BinIdGeneratorRepository extends MongoRepository<BinIdGenerator,String>, CompanyScopedRepository {

    Optional<BinIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
