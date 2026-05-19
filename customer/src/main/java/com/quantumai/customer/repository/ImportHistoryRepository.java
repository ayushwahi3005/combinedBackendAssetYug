package com.quantumai.customer.repository;

import com.quantumai.customer.dto.ImportHistoryDTO;
import com.quantumai.customer.entity.ImportHistory;
import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ImportHistoryRepository extends MongoRepository<ImportHistory, String> , CompanyScopedRepository{

  Page<ImportHistoryDTO> findByCompanyId(Long companyId, Pageable pageable);

  Page<ImportHistoryDTO> findByCompanyIdAndDateBetween(
          Long companyId,
          LocalDateTime startDate,
          LocalDateTime endDate,
          Pageable pageable
  );

  Optional<ImportHistory> findTopByCompanyIdAndStatusAndRecordTypeOrderByDateDesc(Long companyId, String status, ImportHistoryRecordType recordType);



  public void deleteByCompanyId(Long companyId);
}
