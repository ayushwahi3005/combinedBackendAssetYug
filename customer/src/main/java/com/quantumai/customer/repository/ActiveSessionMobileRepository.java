package com.quantumai.customer.repository;

import com.quantumai.customer.entity.ActiveSessionMobile;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActiveSessionMobileRepository
    extends MongoRepository<ActiveSessionMobile, String> {

  Optional<ActiveSessionMobile> findByUserId(String userId);

  void deleteBySessionId(String sessionId);

  void deleteByUserId(String userId);

//  public void deleteByCompanyId(Long companyId);
}
