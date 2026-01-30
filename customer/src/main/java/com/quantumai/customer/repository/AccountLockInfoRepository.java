package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AccountLockInfo;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountLockInfoRepository extends MongoRepository<AccountLockInfo, String> {

  public Optional<AccountLockInfo> findByCustomerEmail(String email);

//  public void deleteByCompanyId(Long companyId);
}
