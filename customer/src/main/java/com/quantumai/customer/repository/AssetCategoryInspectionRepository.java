package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetCategoryInspection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetCategoryInspectionRepository
    extends MongoRepository<AssetCategoryInspection, String>, CompanyScopedRepository {

  public Optional<AssetCategoryInspection> findByName(String name);

  public Optional<AssetCategoryInspection> findByCategoryId(String categoryId);

  public List<AssetCategoryInspection> findByCompanyId(Long companyId);

  @Query("{ 'companyId': ?0, '$or': [ { 'categoryName': { $regex: ?1, $options: 'i' } }, { 'categoryName': { $regex: '^none$', $options: 'i' } } ] }")
  List<AssetCategoryInspection> findByCompanyIdAndCategoryNameIgnoreCase(Long companyId, String category);

  public void deleteByCompanyId(Long companyId);

//List<AssetCategoryInspection> findByCompanyIdAndCategoryNameIgnoreCase(Long companyId, String category);
}
