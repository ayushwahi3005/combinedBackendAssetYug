package com.quantumai.customer.repository;

import com.quantumai.customer.entity.Location;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LocationRepository extends MongoRepository<Location,String> {

    List<Location> findByCompanyId(String id);
}
