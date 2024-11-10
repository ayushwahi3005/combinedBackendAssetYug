package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Subscription;
import com.quantumai.customer.entity.SubscriptionEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription,String> {
            List<Subscription> findAll();
            Optional<Subscription> findByCompanyId(String companyId);
}
