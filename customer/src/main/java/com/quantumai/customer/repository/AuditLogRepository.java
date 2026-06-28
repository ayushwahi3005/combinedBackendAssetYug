package com.quantumai.customer.repository;

import com.quantumai.customer.entity.AuditLog;
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends MongoRepository<AuditLog, String>, CompanyScopedRepository {

    Page<AuditLog> findByCompanyId(Long companyId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndModule(Long companyId, AuditModule module, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndModuleAndAction(Long companyId, AuditModule module, AuditAction action, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndPerformedByEmail(Long companyId, String performedByEmail, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndTimestampBetween(Long companyId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<AuditLog> findByCompanyIdAndEntityId(Long companyId, String entityId);

    Optional<AuditLog> findTopByCompanyIdOrderByAuditIdDesc(Long companyId);

    void deleteByCompanyId(Long companyId);
}
