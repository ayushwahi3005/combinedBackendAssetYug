package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyPrimaryKeyTable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyPrimaryKeyTableRepository
    extends MongoRepository<CompanyPrimaryKeyTable, String> {}
