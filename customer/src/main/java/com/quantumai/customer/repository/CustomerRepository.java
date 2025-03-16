package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String> {

  Boolean existsByEmail(String email);

  Optional<Customer> findByEmail(String email);

  List<Customer> findByCompanyId(String companyId);

  Optional<Customer> findByEmailAndCompanyId(String email, String companyId);

  Long countByRoleAndCompanyId(String role, String id);
}
