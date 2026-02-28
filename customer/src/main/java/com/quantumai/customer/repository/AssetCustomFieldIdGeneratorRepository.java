package com.quantumai.customer.repository;

import com.quantumai.customer.entity.IdGenerator.AssetCategoryIdGenerator;
import com.quantumai.customer.entity.IdGenerator.AssetCustomFieldIdGenerator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AssetCustomFieldIdGeneratorRepository extends MongoRepository<AssetCustomFieldIdGenerator, String>, CompanyScopedRepository {
    Optional<AssetCustomFieldIdGenerator> findByCompanyId(Long companyId);

    boolean existsByCompanyId(Long companyId);

    void deleteByCompanyId(Long companyId);
}
