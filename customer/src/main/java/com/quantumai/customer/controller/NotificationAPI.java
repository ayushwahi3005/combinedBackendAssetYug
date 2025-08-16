package com.quantumai.customer.controller;

import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.entity.UserNotification;
import com.quantumai.customer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notification")
//@CrossOrigin(
//        origins = {
//                "http://localhost:4200",
//                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
//        },
//        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
//)
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
    public void globalNotification(@RequestBody Notification notification){
        notificationService.broadcastNotification(notification);
    }
    @PostMapping("/company/{companyId}")
    public void companyNotification(@RequestBody Notification notification, @PathVariable Long companyId){
        notificationService.sendNotificationToCompany(companyId,notification);
    }
    @GetMapping("/user/{email}")
    public List<UserNotification> userNotification(@PathVariable String email){
        return notificationService.userNotificationDTO(email);
    }
    @PostMapping("/user/{email}")
    public ResponseEntity<String> updateUserNotification(@RequestBody List<UserNotification> notificationList,@PathVariable String email){
         notificationService.updateUserNotificationDTO(notificationList);
         return ResponseEntity.ok("Successfully Notification Updated");
    }

}
