package com.quantumai.customer.controller;

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

    @PostMapping("/global")
    @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
    public void globalNotification(@RequestBody Notification notification){
        notificationService.broadcastNotification(notification);
    }
    @PostMapping("/company/{companyId}")
    @PreAuthorize("@appSecurity.isSameCompany(authentication,#companyId)")
    public void companyNotification(@RequestBody Notification notification, @PathVariable Long companyId){
        notificationService.sendNotificationToCompany(companyId,notification);
    }
    @GetMapping("/user/{email}")
    @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
    public List<UserNotification> userNotification(@PathVariable String email){
        return notificationService.userNotificationDTO(email);
    }
    @PostMapping("/user/{email}")
    @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
    public ResponseEntity<String> updateUserNotification(@RequestBody List<UserNotification> notificationList,@PathVariable String email){
         notificationService.updateUserNotificationDTO(notificationList);
         return ResponseEntity.ok("Successfully Notification Updated");
    }

}
