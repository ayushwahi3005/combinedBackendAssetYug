package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AssetFile;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AssetFileRepository extends MongoRepository<AssetFile, String> {
  public List<AssetFile> findByAssetId(String assetId);
}
