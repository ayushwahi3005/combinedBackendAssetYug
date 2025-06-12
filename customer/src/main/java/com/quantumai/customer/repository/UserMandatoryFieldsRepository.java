package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserMandatoryFields;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMandatoryFieldsRepository
    extends MongoRepository<UserMandatoryFields, String> {
  public Optional<UserMandatoryFields> findByNameIgnoreCaseAndCompanyId(
      String name, Long companyId);

  public List<UserMandatoryFields> findByCompanyId(Long companyId);
}
