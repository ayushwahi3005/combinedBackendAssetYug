package com.quantumai.customer.controller;

import com.quantumai.customer.service.UserActivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-activation")
public class UserActivationController {

    private final UserActivationService userActivationService;

    @Autowired
    public UserActivationController(UserActivationService userActivationService) {
        this.userActivationService = userActivationService;
    }

    @PostMapping("/activate/{userId}")
    public ResponseEntity<?> activateUser(@PathVariable String userId) {
        boolean activated = userActivationService.activateUser(userId);
        if (activated) {
            return ResponseEntity.ok().body("User activated successfully");
        } else {
            return ResponseEntity.badRequest().body("Cannot activate user - subscription limit reached or user not found");
        }
    }

    @PostMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivateUser(@PathVariable String userId) {
        boolean deactivated = userActivationService.deactivateUser(userId);
        if (deactivated) {
            return ResponseEntity.ok().body("User deactivated successfully");
        } else {
            return ResponseEntity.badRequest().body("User was already inactive or not found");
        }
    }

    @GetMapping("/can-activate/{companyId}")
    public ResponseEntity<Boolean> canActivateNewUser(@PathVariable Long companyId) {
        boolean canActivate = userActivationService.canActivateNewUser(companyId);
        return ResponseEntity.ok(canActivate);
    }
}
