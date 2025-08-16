package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Bin;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BinRepository extends MongoRepository<Bin, String> {

  List<Bin> findByCompanyId(Long id);
  Optional<Bin> findByCompanyIdAndBinNumberIgnoreCase(Long companyId, String binNumber);
}
