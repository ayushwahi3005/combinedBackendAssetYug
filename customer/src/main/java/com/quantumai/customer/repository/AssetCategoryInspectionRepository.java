package com.quantumai.customer.repository;


import com.quantumai.customer.entity.AssetCategoryInspection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AssetCategoryInspectionRepository extends MongoRepository<AssetCategoryInspection, String> {

    public Optional<AssetCategoryInspection> findByName(String name);
    public Optional<AssetCategoryInspection> findByCategoryId(String categoryId);
    public List<AssetCategoryInspection> findByCompanyId(String companyId);
}
