package com.quantumai.customer.repository;

import com.quantumai.customer.dto.ImportHistoryDTO;
import com.quantumai.customer.entity.ImportHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface ImportHistoryRepository extends MongoRepository<ImportHistory, String> , CompanyScopedRepository{

  Page<ImportHistoryDTO> findByCompanyId(Long companyId, Pageable pageable);

  Page<ImportHistoryDTO> findByCompanyIdAndDateBetween(
          Long companyId,
          LocalDateTime startDate,
          LocalDateTime endDate,
          Pageable pageable
  );


  public void deleteByCompanyId(Long companyId);
}
