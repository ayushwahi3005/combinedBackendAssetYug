package com.quantumai.customer.service;


import com.quantumai.customer.entity.CustomRoleType;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Security service used in @PreAuthorize SpEL expressions.
 *
 * Every method here is callable via:
 *   @PreAuthorize("@assetSecurity.canView(authentication, #companyId)")
 *
 * Two checks are combined:
 *   1. ROLE check    — does the user's role allow this action (view/create/edit/full)?
 *   2. COMPANY check — does the companyId in the request match the user's own companyId?
 *
 * This prevents a user with a valid JWT from one company
 * accessing or mutating data belonging to another company.
 */
@Service("assetSecurity")
@RequiredArgsConstructor
@Slf4j
public class AssetSecurityService {

    private final UsersRepository usersRepository;

    // ─── Internal helper ──────────────────────────────────────────────────────

    private boolean check(Authentication auth, Long companyId, CustomRoleType required) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt");
            return false;
        }

        String email = auth.getName();
        Optional<Users> usersOptional = usersRepository.findByEmail(email);

        if (usersOptional.isEmpty()) {
            log.warn("No user found for email: {}", email);
            return false;
        }

        Users user = usersOptional.get();

        // 🔒 Company ownership check — user must belong to the same company
        if (companyId != null && !companyId.equals(user.getCompanyId())) {
            log.warn("Company mismatch! User {} belongs to company {} but requested company {}",
                    email, user.getCompanyId(), companyId);
            return false;
        }

        // 🔒 Role level check — user's asset role must be >= required level
        int userLevel  = user.getRole().getAssets().ordinal();
        int reqLevel   = required.ordinal();

        if (reqLevel > userLevel) {
            log.warn("Insufficient role for user {}. Required: {} ({}), has: {} ({})",
                    email, required, reqLevel,
                    user.getRole().getAssets(), userLevel);
            return false;
        }

        return true;
    }

    // ─── Public API used in @PreAuthorize ─────────────────────────────────────

    /** Read-only access to a company's assets */
    public boolean canView(Authentication auth, Long companyId) {
        return check(auth, companyId, CustomRoleType.view);
    }

    /** Create new assets / import / upload */
    public boolean canCreate(Authentication auth, Long companyId) {
        return check(auth, companyId, CustomRoleType.create);
    }

    /** Edit / update existing assets */
    public boolean canEdit(Authentication auth, Long companyId) {
        return check(auth, companyId, CustomRoleType.edit);
    }

    /** Delete / destructive operations */
    public boolean canDelete(Authentication auth, Long companyId) {
        return check(auth, companyId, CustomRoleType.full);
    }

    /**
     * For asset-level endpoints where companyId is not in the path
     * (e.g. /getAsset/{id}), we still verify the user is authenticated
     * and has at least view-level role. Company check is skipped here
     * because we only have the asset's internal ID — add a service-level
     * check if you need to verify asset ownership too.
     */
    public boolean canViewAny(Authentication auth) {
        return check(auth, null, CustomRoleType.view);
    }

    public boolean canCreateAny(Authentication auth) {
        return check(auth, null, CustomRoleType.create);
    }

    public boolean canEditAny(Authentication auth) {
        return check(auth, null, CustomRoleType.edit);
    }

    public boolean canDeleteAny(Authentication auth) {
        return check(auth, null, CustomRoleType.full);
    }
}
