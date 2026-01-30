package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyInformation;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyInformationRepository extends MongoRepository<CompanyInformation, Long> {
  Optional<CompanyInformation> findByCustomerEmail(String email);

//  public void deleteByCompanyId(Long companyId);
}
