package com.quantumai.customer.repository;

import com.quantumai.customer.entity.BlacklistedEmail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistedEmailRepository extends MongoRepository<BlacklistedEmail, String> {
    Optional<BlacklistedEmail> findByEmail(String email);
    boolean existsByEmail(String email);
}
