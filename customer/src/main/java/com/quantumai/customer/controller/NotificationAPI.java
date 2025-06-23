package com.quantumai.customer.controller;

import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/")
    public void globalNotification(@RequestBody Notification notification){
        notificationService.broadcastNotification(notification);
    }
    @PostMapping("/{companyId}")
    public void companyNotification(@RequestBody Notification notification, @PathVariable Long companyId){
        notificationService.sendNotificationToCompany(companyId,notification);
    }

}
