package com.quantumai.customer.service;

import com.quantumai.customer.entity.CustomRole;
import com.quantumai.customer.entity.CustomRoleType;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("appSecurity")
@RequiredArgsConstructor
@Slf4j
public class AppSecurityService {

    private final UsersRepository usersRepository;

    // ─── Internal helper ──────────────────────────────────────────────────────

    private boolean check(Authentication auth, Long companyId, String module, CustomRoleType required) {
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

        // 🔒 Company ownership check
        if (companyId != null && !companyId.equals(user.getCompanyId())) {
            log.warn("Company mismatch! User {} belongs to company {} but requested company {}",
                    email, user.getCompanyId(), companyId);
            return false;
        }

        // 🔒 Dynamically resolve the module role level
        CustomRoleType userModuleRole = resolveModuleRole(user.getRole(), module);
        if (userModuleRole == null) {
            log.warn("Unknown module '{}' for user {}", module, email);
            return false;
        }

        int userLevel = userModuleRole.ordinal();
        int reqLevel  = required.ordinal();

        if (reqLevel > userLevel) {
            log.warn("Insufficient role for user {} on module {}. Required: {}, has: {}",
                    email, module, required, userModuleRole);
            return false;
        }

        return true;
    }

    public boolean isSameCompany(Authentication auth, Long companyId) {
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

        // ✅ Only check company match — no role check
        if (companyId != null && !companyId.equals(usersOptional.get().getCompanyId())) {
            log.warn("Company mismatch for user {}", email);
            return false;
        }

        return true;
    }

    // For endpoints with no companyId — just verify authenticated
    public boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated();
    }
    public boolean isEmailSame(Authentication auth, String email) {
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("Unauthenticated access attempt");
            return false;
        }

        String myEmail = auth.getName();
        Optional<Users> usersOptional = usersRepository.findByEmail(email);

        if(!email.equals(myEmail)){
            log.info("Email mismatch : {} {}",email,myEmail);
            return false;
        }

        return true;
    }

    // ─── Resolve which field to check based on module name ───────────────────

    private CustomRoleType resolveModuleRole(CustomRole role, String module) {
        return switch (module.toLowerCase()) {
            case "assets"             -> role.getAssets();
            case "customers"          -> role.getCustomers();
            case "workorders"         -> role.getWorkOrders();
            case "users"              -> role.getUsers();
            case "roleandpermissions" -> role.getRoleAndPermissions();
            case "imports"            -> role.getImports();
            case "category"           -> role.getCategory();
            case "inventory"          -> role.getInventory();
            case "inspections", "inspection" -> {
                CustomRoleType inspectionsRole = role.getInspections();
                yield inspectionsRole != null ? inspectionsRole : role.getAssets();
            }
            default -> null;
        };
    }

    // ─── Public API — with companyId (most controllers) ──────────────────────

    public boolean canView(Authentication auth, Long companyId, String module) {
        return check(auth, companyId, module, CustomRoleType.view);
    }

    public boolean canCreate(Authentication auth, Long companyId, String module) {
        return check(auth, companyId, module, CustomRoleType.create);
    }

    public boolean canEdit(Authentication auth, Long companyId, String module) {
        return check(auth, companyId, module, CustomRoleType.edit);
    }

    public boolean canDelete(Authentication auth, Long companyId, String module) {
        return check(auth, companyId, module, CustomRoleType.full);
    }

    // ─── Public API — without companyId (asset/record level endpoints) ────────

    public boolean canViewAny(Authentication auth, String module) {
        return check(auth, null, module, CustomRoleType.view);
    }

    public boolean canCreateAny(Authentication auth, String module) {
        return check(auth, null, module, CustomRoleType.create);
    }

    public boolean canEditAny(Authentication auth, String module) {
        return check(auth, null, module, CustomRoleType.edit);
    }

    public boolean canDeleteAny(Authentication auth, String module) {
        return check(auth, null, module, CustomRoleType.full);
    }
}