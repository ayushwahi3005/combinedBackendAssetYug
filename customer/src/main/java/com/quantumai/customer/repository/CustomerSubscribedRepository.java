package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CustomerSubscribed;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerSubscribedRepository extends MongoRepository<CustomerSubscribed, String> {

  Optional<CustomerSubscribed> findById(String email);
}
