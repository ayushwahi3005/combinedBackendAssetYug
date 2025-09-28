package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Subscription;
import java.util.List;
import java.util.Optional;

import com.quantumai.customer.entity.SubscriptionEnum;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.quantumai.customer.dto.SubscriptionGrowthDTO;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriptionRepository extends MongoRepository<Subscription, String>, SubscriptionRepositoryCustom {
  List<Subscription> findAll();

  List<Subscription> findByCompanyId(Long companyId);
  List<Subscription> findByStripeSubscriptionId(String id);

  Optional<Subscription> findByCompanyIdAndStatus(Long companyId, SubscriptionEnum status);
    
  /**
   * Retrieves subscription growth data between the specified dates.
   * @param start The start date (inclusive)
   * @param end The end date (inclusive)
   * @return A list of SubscriptionGrowthDTO containing growth metrics
   */
  List<SubscriptionGrowthDTO> getSubscriptionGrowth(LocalDate start, LocalDate end);
}
