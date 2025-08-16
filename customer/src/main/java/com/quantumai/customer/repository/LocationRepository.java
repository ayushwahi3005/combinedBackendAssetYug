package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Location;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocationRepository extends MongoRepository<Location, String> {

  List<Location> findByCompanyId(Long id);
  Optional<Location> findByCompanyIdAndName(Long id,String name);
}
