package com.quantumai.customer.service;

import com.quantumai.customer.dto.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    
    public void sendNotificationToUser(String userId, Notification notification){
        simpMessagingTemplate.convertAndSend("/topic/notifications/"+userId,notification);
    }

    @Scheduled(fixedRate = 10000)
    public void sendDummyNotification(){
        Notification notification=new Notification("1234","1234","notification","alert",true, LocalDateTime.now());
        sendNotificationToUser("1234",notification);
    }
    
}
