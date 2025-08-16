package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CustomerStripeDetails;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerStripeDetailsRepository
    extends MongoRepository<CustomerStripeDetails, String> {
  Optional<CustomerStripeDetails> findByCompanyId(Long companyId);
  Optional<CustomerStripeDetails> findByCustomerId(String customerId);

  Optional<CustomerStripeDetails> findByPaymentMethodId(String paymentMethodId);
}
