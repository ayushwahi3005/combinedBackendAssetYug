package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.CompanyCustomerIdTable;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyCustomerIdTableRepository
    extends MongoRepository<CompanyCustomerIdTable, String> {
  public Optional<CompanyCustomerIdTable> findByCompanyId(Long id);
}
