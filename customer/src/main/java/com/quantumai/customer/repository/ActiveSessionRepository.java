package com.quantumai.customer.repository;

import com.quantumai.customer.entity.ActiveSession;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ActiveSessionRepository extends MongoRepository<ActiveSession, String> {

  Optional<ActiveSession> findByUserId(String userId);

  void deleteBySessionId(String sessionId);

  void deleteByUserId(String userId);

//  public void deleteByCompanyId(Long companyId);
}
