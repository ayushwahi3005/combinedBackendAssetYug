package com.quantumai.customer.repository;


import com.quantumai.customer.entity.ActiveSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ActiveSessionRepository extends MongoRepository<ActiveSession,String> {

    Optional<ActiveSession> findByUserId(String userId);

    void deleteBySessionId(String sessionId);

    void deleteByUserId(String userId);
}
