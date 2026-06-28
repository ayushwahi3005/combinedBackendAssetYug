package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.quantumai.customer.dto.PaginatedNotificationDTO;
import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.entity.UserNotification;
import com.quantumai.customer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
@Tag(name = "Notification", description = "Notification Management API")
public class NotificationAPI {

    private final SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    NotificationService notificationService;

    public NotificationAPI(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

//    @Scheduled(fixedRate = 10000)
//    public void sendD

//    @PostMapping("/notify")
//    public ResponseEntity<String> notificationSend(@RequestBody String message){
//        simpMessagingTemplate.convertAndSend("/topic/notifications",message);
//        return ResponseEntity.ok("Notification Sent");
//    }

    @Operation(summary = "Global Notification", description = "Endpoint to global notification")
    @PostMapping("/global")
    @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
    public void globalNotification(@RequestBody Notification notification){
        notificationService.broadcastNotification(notification);
    }

    @Operation(summary = "Get Paginated User Notifications", description = "Endpoint to get user notifications with pagination (lazy loading) - 10 per page")
    @GetMapping("/user/{email}/paginated")
    @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
    public ResponseEntity<PaginatedNotificationDTO> getPaginatedUserNotifications(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize) {
        PaginatedNotificationDTO paginatedNotifications = notificationService.getPaginatedNotifications(
                email, pageNumber, pageSize);
        return ResponseEntity.ok(paginatedNotifications);
    }

    @Operation(summary = "Company Notification", description = "Endpoint to company notification")
    @PostMapping("/company/{companyId}")
    @PreAuthorize("@appSecurity.isSameCompany(authentication,#companyId)")
    public void companyNotification(@RequestBody Notification notification, @PathVariable Long companyId){
        notificationService.sendNotificationToCompany(companyId,notification);
    }
    @Operation(summary = "User Notification", description = "Endpoint to user notification")
    @GetMapping("/user/{email}")
    @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
    public List<UserNotification> userNotification(@PathVariable String email){
        return notificationService.userNotificationDTO(email);
    }
    @Operation(summary = "Update User Notification", description = "Endpoint to update user notification")
    @PostMapping("/user/{email}")
    @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
    public ResponseEntity<String> updateUserNotification(@RequestBody List<UserNotification> notificationList,@PathVariable String email){
         notificationService.updateUserNotificationDTO(notificationList);
         return ResponseEntity.ok("Successfully Notification Updated");
    }

}
