package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Location;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocationRepository extends MongoRepository<Location, String> {

  List<Location> findByCompanyId(String id);
}
