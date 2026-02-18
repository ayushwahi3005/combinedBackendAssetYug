package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.UserIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserIdGeneratorRepository extends MongoRepository<UserIdGenerator, String> {

    Optional<UserIdGenerator> findByCompanyId(Long companyId);
}
