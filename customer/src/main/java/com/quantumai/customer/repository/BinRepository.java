package com.quantumai.customer.repository;


import com.quantumai.customer.entity.Bin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BinRepository extends MongoRepository<Bin,String> {

    List<Bin> findByCompanyId(String id);
}
