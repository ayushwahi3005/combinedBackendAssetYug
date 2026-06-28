package com.quantumai.customer.service;

import com.quantumai.customer.dto.AuditLogFilterDTO;
import com.quantumai.customer.dto.PaginatedResultDTO;
import com.quantumai.customer.entity.AuditLog;
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;

import java.util.List;
import java.util.Map;

public interface AuditService {

    /**
     * Log a create action for any module.
     */
    void logCreate(AuditModule module, String entityId, String entityName,
                   Long companyId, Map<String, Object> snapshot);

    /**
     * Log an update action, capturing old vs new values.
     */
    void logUpdate(AuditModule module, String entityId, String entityName,
                   Long companyId, Map<String, Object> changes);

    /**
     * Log a delete action for any module.
     */
    void logDelete(AuditModule module, String entityId, String entityName,
                   Long companyId, Map<String, Object> snapshot);

    /**
     * Generic log method for any action.
     */
    void log(AuditModule module, AuditAction action, String entityId, String entityName,
             Long companyId, String description, Map<String, Object> changes);

    /**
     * Log update with detailed field-by-field comparison (before/after).
     * Automatically computes changes map with old/new values for each field.
     */
    void logUpdateWithComparison(AuditModule module, String entityId, String entityName,
                                 Long companyId, Object beforeEntity, Object afterEntity);

    /**
     * Retrieve paginated audit logs for a company with optional filtering.
     */
    PaginatedResultDTO<AuditLog> getAuditLogs(AuditLogFilterDTO filter);

    /**
     * Retrieve all audit trail for a specific entity.
     */
    List<AuditLog> getEntityAuditTrail(Long companyId, String entityId);
}
