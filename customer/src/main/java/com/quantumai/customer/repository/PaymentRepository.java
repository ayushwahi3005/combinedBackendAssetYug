package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Payment;
import java.util.List;
import java.util.Optional;

import com.quantumai.customer.entity.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
  List<Payment> findByCompanyId(Long id);
  Optional<Payment> findByPaymentIntentId(String id);
  List<Payment> findByCompanyIdAndPaymentStatusOrderByTransactionDateDesc(Long id, PaymentStatus status);
}
