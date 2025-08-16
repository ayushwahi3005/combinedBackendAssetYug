package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Subscription;
import java.util.List;
import java.util.Optional;

import com.quantumai.customer.entity.SubscriptionEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
  List<Subscription> findAll();

  List<Subscription> findByCompanyId(Long companyId);
  Optional<Subscription> findByStripeSubscriptionId(String id);

  Optional<Subscription> findByCompanyIdAndStatus(Long companyId, SubscriptionEnum status);
}
