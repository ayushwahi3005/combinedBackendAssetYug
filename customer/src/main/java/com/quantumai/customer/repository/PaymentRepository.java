package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Payment;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
  List<Payment> findByCompanyId(Long id);
}
