package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.AssetCategoryIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetCategoryIdGeneratorRepository extends MongoRepository<AssetCategoryIdGenerator,String> {
}
