package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.AssetCategoryIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AssetCategoryIdGeneratorRepository extends MongoRepository<AssetCategoryIdGenerator,String> {
    Optional<AssetCategoryIdGenerator>  findByCompanyId(Long companyId);
}
