package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.InvoicePrimaryKeyTable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvoicePrimaryKeyTableRepository extends MongoRepository<InvoicePrimaryKeyTable, String> {
}
