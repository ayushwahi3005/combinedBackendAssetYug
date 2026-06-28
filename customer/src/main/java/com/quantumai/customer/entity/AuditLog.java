package com.quantumai.customer.entity;

import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    /** Sequential numeric ID per company for easy referencing */
    private Long auditId;

    /** Company the action was performed under */
    @Indexed
    private Long companyId;

    /** Module in which the action was performed */
    @Indexed
    private AuditModule module;

    /** Type of action (CREATE, UPDATE, DELETE, etc.) */
    private AuditAction action;

    /** MongoDB document ID of the affected entity */
    private String entityId;

    /** Human-readable name / title of the affected entity (e.g. asset name, user email) */
    private String entityName;

    /** Email of the user who performed the action */
    @Indexed
    private String performedByEmail;

    /** Full name of the user who performed the action */
    private String performedByName;

    /** Database ID of the user who performed the action */
    private String performedByUserId;

    /** IP address from which the request was made */
    private String ipAddress;

    /** Optional human-readable description of what happened */
    private String description;

    /**
     * Key-value map capturing field-level changes.
     * For updates: { "fieldName": { "old": "oldValue", "new": "newValue" } }
     * For creates/deletes: snapshot of relevant fields.
     */
    private Map<String, Object> changes;

    /** Timestamp when the action occurred */
    @Indexed
    private LocalDateTime timestamp;
}
