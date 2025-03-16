package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByCompanyId(String id);
}
