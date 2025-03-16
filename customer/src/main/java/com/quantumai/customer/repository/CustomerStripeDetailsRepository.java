package com.quantumai.customer.repository;


import com.quantumai.customer.entity.CustomerStripeDetails;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerStripeDetailsRepository extends MongoRepository<CustomerStripeDetails, String> {
    Optional<CustomerStripeDetails> findByCompanyId(String companyId);
    Optional<CustomerStripeDetails> findByPaymentMethodId(String paymentMethodId);
}
