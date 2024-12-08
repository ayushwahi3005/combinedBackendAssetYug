package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Plans;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PlansRepository extends MongoRepository<Plans,String> {
    List<Plans> findAll();
    Optional<Plans> getById(String id);

}
