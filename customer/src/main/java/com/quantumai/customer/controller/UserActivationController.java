package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.service.UserActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-activation")
@Tag(name = "UserActivation", description = "UserActivation Management API")
public class UserActivationController {

    private final UserActivationService userActivationService;

    @Autowired
    public UserActivationController(UserActivationService userActivationService) {
        this.userActivationService = userActivationService;
    }

    @Operation(summary = "Activate User", description = "Endpoint to activate user")
    @PostMapping("/activate/{userId}")
    public ResponseEntity<?> activateUser(@PathVariable String userId) {
        boolean activated = userActivationService.activateUser(userId);
        if (activated) {
            return ResponseEntity.ok().body("User activated successfully");
        } else {
            return ResponseEntity.badRequest().body("Cannot activate user - subscription limit reached or user not found");
        }
    }

    @Operation(summary = "Deactivate User", description = "Endpoint to deactivate user")
    @PostMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivateUser(@PathVariable String userId) {
        boolean deactivated = userActivationService.deactivateUser(userId);
        if (deactivated) {
            return ResponseEntity.ok().body("User deactivated successfully");
        } else {
            return ResponseEntity.badRequest().body("User was already inactive or not found");
        }
    }

    @Operation(summary = "Can Activate New User", description = "Endpoint to can activate new user")
    @GetMapping("/can-activate/{companyId}")
    public ResponseEntity<Boolean> canActivateNewUser(@PathVariable Long companyId) {
        boolean canActivate = userActivationService.canActivateNewUser(companyId);
        return ResponseEntity.ok(canActivate);
    }
}
