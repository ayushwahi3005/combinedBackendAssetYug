package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CustomerImportColumnMapping;
import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerImportColumnMappingRepository
    extends MongoRepository<CustomerImportColumnMapping, String> {

  List<CustomerImportColumnMapping> findByCompanyIdAndRecordTypeOrderByUpdatedAtDesc(
      Long companyId, ImportHistoryRecordType recordType);

  Optional<CustomerImportColumnMapping> findByIdAndCompanyId(String id, Long companyId);
}
