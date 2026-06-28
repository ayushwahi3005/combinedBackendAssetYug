package com.quantumai.customer.service;

import com.quantumai.customer.dto.AuditLogFilterDTO;
import com.quantumai.customer.dto.PaginatedResultDTO;
import com.quantumai.customer.entity.AuditLog;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import com.quantumai.customer.repository.AuditLogRepository;
import com.quantumai.customer.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UsersRepository usersRepository;
    private final MongoTemplate mongoTemplate;

    // ─── Public API ───────────────────────────────────────────────────────────

    @Override
    public void logCreate(AuditModule module, String entityId, String entityName,
                          Long companyId, Map<String, Object> snapshot) {
        String email = resolveCurrentUserEmail();
        String ip    = resolveClientIp();
        persist(module, AuditAction.CREATE, entityId, entityName, companyId,
                "Created " + friendlyName(module) + ": " + entityName,
                snapshot, email, ip);
    }

    @Override
    public void logUpdate(AuditModule module, String entityId, String entityName,
                          Long companyId, Map<String, Object> changes) {
        String email = resolveCurrentUserEmail();
        String ip    = resolveClientIp();
        persist(module, AuditAction.UPDATE, entityId, entityName, companyId,
                "Updated " + friendlyName(module) + ": " + entityName,
                changes, email, ip);
    }

    @Override
    public void logDelete(AuditModule module, String entityId, String entityName,
                          Long companyId, Map<String, Object> snapshot) {
        String email = resolveCurrentUserEmail();
        String ip    = resolveClientIp();
        persist(module, AuditAction.DELETE, entityId, entityName, companyId,
                "Deleted " + friendlyName(module) + ": " + entityName,
                snapshot, email, ip);
    }

    @Override
    public void log(AuditModule module, AuditAction action, String entityId, String entityName,
                    Long companyId, String description, Map<String, Object> changes) {
        String email = resolveCurrentUserEmail();
        String ip    = resolveClientIp();
        persist(module, action, entityId, entityName, companyId, description, changes, email, ip);
    }

    @Override
    public void logUpdateWithComparison(AuditModule module, String entityId, String entityName,
                                        Long companyId, Object beforeEntity, Object afterEntity) {
        Map<String, Object> changes = AuditChangeCalculator.computeChanges(beforeEntity, afterEntity);
        String email = resolveCurrentUserEmail();
        String ip    = resolveClientIp();
        persist(module, AuditAction.UPDATE, entityId, entityName, companyId,
                "Updated " + friendlyName(module) + ": " + entityName,
                changes, email, ip);
    }

    // ─── Querying ─────────────────────────────────────────────────────────────

    @Override
    public PaginatedResultDTO<AuditLog> getAuditLogs(AuditLogFilterDTO filter) {
        // Build criteria from all provided filter fields (combined with AND)
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("companyId").is(filter.getCompanyId()));

        if (filter.getModule() != null) {
            criteriaList.add(Criteria.where("module").is(filter.getModule()));
        }
        if (filter.getAction() != null) {
            criteriaList.add(Criteria.where("action").is(filter.getAction()));
        }
        if (filter.getPerformedByEmail() != null && !filter.getPerformedByEmail().isBlank()) {
            criteriaList.add(Criteria.where("performedByEmail").is(filter.getPerformedByEmail()));
        }
        if (filter.getEntityId() != null && !filter.getEntityId().isBlank()) {
            criteriaList.add(Criteria.where("entityId").is(filter.getEntityId()));
        }
        if (filter.getFromTimestamp() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(filter.getFromTimestamp()));
        }
        if (filter.getToTimestamp() != null) {
            criteriaList.add(Criteria.where("timestamp").lte(filter.getToTimestamp()));
        }

        Criteria combined = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(combined);

        // Sort direction
        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        query.with(Sort.by(direction, "timestamp"));

        // Count before pagination
        long total = mongoTemplate.count(query, AuditLog.class);

        // Apply pagination
        query.skip((long) filter.getPageNumber() * filter.getPageSize());
        query.limit(filter.getPageSize());

        List<AuditLog> results = mongoTemplate.find(query, AuditLog.class);
        return new PaginatedResultDTO<>(results, total);
    }

    @Override
    public List<AuditLog> getEntityAuditTrail(Long companyId, String entityId) {
        Query query = new Query(
                Criteria.where("companyId").is(companyId)
                        .and("entityId").is(entityId))
                .with(Sort.by(Sort.Direction.ASC, "timestamp"));
        return mongoTemplate.find(query, AuditLog.class);
    }

    // ─── Core persist ─────────────────────────────────────────────────────────

    private void persist(AuditModule module, AuditAction action,
                         String entityId, String entityName,
                         Long companyId, String description,
                         Map<String, Object> changes,
                         String performedByEmail, String ipAddress) {
        try {
            String performedByName   = null;
            String performedByUserId = null;

            if (performedByEmail != null) {
                Optional<Users> userOpt = usersRepository.findByEmail(performedByEmail);
                if (userOpt.isPresent()) {
                    Users user = userOpt.get();
                    String first = trimToNull(user.getFirstName());
                    performedByName   = first != null
                            ? first + " " + user.getLastName()
                            : performedByEmail;
                    performedByUserId = user.getId();
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .auditId(nextAuditId(companyId))
                    .companyId(companyId)
                    .module(module)
                    .action(action)
                    .entityId(entityId)
                    .entityName(entityName)
                    .performedByEmail(performedByEmail)
                    .performedByName(performedByName)
                    .performedByUserId(performedByUserId)
                    .ipAddress(ipAddress)
                    .description(description)
                    .changes(changes)
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            log.error("Failed to persist audit log for module={} action={} entityId={}: {}",
                    module, action, entityId, e.getMessage(), e);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String resolveCurrentUserEmail() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve current user: {}", e.getMessage());
        }
        return null;
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Could not resolve client IP: {}", e.getMessage());
        }
        return null;
    }

    private Long nextAuditId(Long companyId) {
        return auditLogRepository.findTopByCompanyIdOrderByAuditIdDesc(companyId)
                .map(last -> last.getAuditId() != null ? last.getAuditId() + 1 : 1L)
                .orElse(1L);
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String friendlyName(AuditModule module) {
        return module.name().toLowerCase().replace("_", " ");
    }
}
