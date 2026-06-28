package com.quantumai.customer.controller;

import com.quantumai.customer.dto.AuditLogFilterDTO;
import com.quantumai.customer.dto.PaginatedResultDTO;
import com.quantumai.customer.entity.AuditLog;
import com.quantumai.customer.entity.enums.AuditModule;
import com.quantumai.customer.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/audit")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@Tag(name = "Audit", description = "Audit Log API — track all changes across every module")
@RequiredArgsConstructor
public class AuditLogAPI {

    private final AuditService auditService;

    /**
     * Main audit log query endpoint.
     * Supports combined filters: module + action + email + date range + entityId.
     * Sort by timestamp ASC or DESC.
     *
     * POST /audit/logs/{companyId}
     * Body: AuditLogFilterDTO
     */
    @Operation(
        summary = "Get audit logs (filtered + paginated)",
        description = """
            Returns paginated audit logs for a company.
            All body fields are optional — combine any number of filters freely.
            
            Available modules: ASSET, ASSET_CATEGORY, ASSET_INSPECTION, ASSET_INSPECTION_INSTANCE,
            ASSET_CUSTOM_FIELD, ASSET_CHECK_IN_OUT, COMPANY_CUSTOMER, COMPANY_CUSTOMER_CATEGORY,
            COMPANY_CUSTOMER_CUSTOM_FIELD, USER, ROLE, LOCATION, BIN, ASSET_QR, SUBSCRIPTION,
            PAYMENT, NOTIFICATION, IMPORT, ADMIN
            
            Available actions: CREATE, UPDATE, DELETE, VIEW, EXPORT, IMPORT, LOGIN, LOGOUT,
            ACTIVATE, DEACTIVATE
            """
    )
    @PostMapping("/logs/{companyId}")
    @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
    public PaginatedResultDTO<AuditLog> getAuditLogs(
            @PathVariable Long companyId,
            @RequestBody AuditLogFilterDTO filter) {
        filter.setCompanyId(companyId);
        return auditService.getAuditLogs(filter);
    }

    /**
     * Quick shortcut: get logs for a specific module, sorted by timestamp.
     *
     * GET /audit/module/{companyId}?module=ASSET&sortDirection=DESC&page=0&size=20
     */
    @Operation(
        summary = "Get logs by module",
        description = "Shortcut to fetch all logs for a specific module, sorted newest or oldest first."
    )
    @GetMapping("/module/{companyId}")
    @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
    public PaginatedResultDTO<AuditLog> getLogsByModule(
            @PathVariable Long companyId,
            @Parameter(description = "Module name e.g. ASSET, USER, COMPANY_CUSTOMER, ROLE, LOCATION, BIN, ASSET_QR, ASSET_CUSTOM_FIELD, ASSET_INSPECTION")
            @RequestParam AuditModule module,
            @Parameter(description = "ASC for oldest first, DESC for newest first (default: DESC)")
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AuditLogFilterDTO filter = new AuditLogFilterDTO();
        filter.setCompanyId(companyId);
        filter.setModule(module);
        filter.setSortDirection(sortDirection);
        filter.setPageNumber(page);
        filter.setPageSize(size);
        return auditService.getAuditLogs(filter);
    }

    /**
     * Full history for one specific entity (e.g. one asset, one user, one location).
     * Sorted chronologically (oldest first).
     *
     * GET /audit/trail/{companyId}/{entityId}
     */
    @Operation(
        summary = "Get entity audit trail",
        description = "Full chronological audit history (CREATE → UPDATEs → DELETE) for a single entity using its business ID."
    )
    @GetMapping("/trail/{companyId}/{entityId}")
    @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
    public List<AuditLog> getEntityAuditTrail(
            @PathVariable Long companyId,
            @Parameter(description = "Business ID of the entity (e.g. assetId, companyCustomerId, userId, locationId)")
            @PathVariable String entityId) {
        return auditService.getEntityAuditTrail(companyId, entityId);
    }

    /**
     * Returns the list of all available audit modules — useful for populating a filter dropdown in the UI.
     *
     * GET /audit/modules
     */
    @Operation(
        summary = "Get all available audit modules",
        description = "Returns all module names that can be used as filters. Use these values in the 'module' field."
    )
    @GetMapping("/modules")
    public List<String> getModules() {
        return Arrays.stream(AuditModule.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
