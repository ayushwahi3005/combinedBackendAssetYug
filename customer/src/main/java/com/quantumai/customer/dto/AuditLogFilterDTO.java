package com.quantumai.customer.dto;

import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogFilterDTO {

    private Long companyId;

    /** Filter by module (e.g. ASSET, COMPANY_CUSTOMER, USER) */
    private AuditModule module;

    /** Filter by action (CREATE, UPDATE, DELETE, etc.) */
    private AuditAction action;

    /** Filter by who performed the action */
    private String performedByEmail;

    /** Filter from this timestamp (inclusive) */
    private LocalDateTime fromTimestamp;

    /** Filter to this timestamp (inclusive) */
    private LocalDateTime toTimestamp;

    /** Filter by specific entity id */
    private String entityId;

    /** Sort direction: "ASC" or "DESC" (default: DESC = newest first) */
    private String sortDirection = "DESC";

    private int pageNumber = 0;
    private int pageSize = 20;
}
