package com.quantumai.customer.repository;

import com.quantumai.customer.entity.UserExtraFieldName;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserExtraFieldNameRepository extends MongoRepository<UserExtraFieldName, String> , CompanyScopedRepository{
  public UserExtraFieldName findByNameIgnoreCaseAndCompanyId(String name, Long companyId);


  public List<UserExtraFieldName> findByCompanyId(Long companyId);

  public void deleteByCompanyId(Long companyId);
}
