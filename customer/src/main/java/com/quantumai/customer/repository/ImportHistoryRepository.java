package com.quantumai.customer.repository;

import com.quantumai.customer.dto.ImportHistoryDTO;
import com.quantumai.customer.entity.ImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImportHistoryRepository extends MongoRepository<ImportHistory, String> {

  Page<ImportHistoryDTO> findByCompanyId(String companyId, Pageable pageable);
}
