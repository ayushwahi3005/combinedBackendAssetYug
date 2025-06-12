package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Bin;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BinRepository extends MongoRepository<Bin, String> {

  List<Bin> findByCompanyId(Long id);
}
