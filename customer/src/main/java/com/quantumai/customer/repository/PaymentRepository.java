package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Payment;
import com.quantumai.customer.entity.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface PaymentRepository  extends MongoRepository<Payment,String> {

}
