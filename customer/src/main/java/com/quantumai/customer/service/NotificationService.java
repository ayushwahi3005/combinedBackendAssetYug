package com.quantumai.customer.service;


import com.quantumai.customer.dto.UserNotificationDTO;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.NotificationRepository;
import com.quantumai.customer.repository.UserNotificationRepository;
import com.quantumai.customer.repository.UserNotificationRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    UserNotificationRepository userNotificationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    UserNotificationRepositoryImpl userNotificationRepositoryImpl;

    public void sendNotificationToCompany(Long companyId, Notification notification) {
        // Sends notification to a specific company
        notification.setNotificationType(NotificationType.COMPANY);
        List<Customer> customerList=customerRepository.findByCompanyId(companyId);
        Notification notification1=notificationRepository.save(notification);
        customerList.forEach((customer)->{
            UserNotification userNotification=new UserNotification(null,customer.getEmail(),notification1.getId(),customer.getCompanyId(),false,null,LocalDateTime.now(),notification1);
//            userNotificationList.add(userNotification);
            userNotificationRepository.save(userNotification);
            List<UserNotification> myList=userNotificationRepositoryImpl.findRecentNotificationsWithDetails(customer.getEmail());
            System.out.println("Sending Message"+myList);
            simpMessagingTemplate.convertAndSend("/topic/user/" + customer.getEmail() + "/notifications", myList);


        });
//        simpMessagingTemplate.convertAndSend("/topic/companies/" + companyId + "/notifications", notification);
//        simpMessagingTemplate.convertAndSend("/topic/notifications",notification);
    }

    public void broadcastNotification(Notification notification) {
        // Sends notification to all users globally
        notification.setNotificationType(NotificationType.GLOBAL);
       Notification notification1= notificationRepository.save(notification);
        List<Customer> customerList=customerRepository.findAll();
//        List<UserNotification> userNotificationList=new ArrayList<>();
        customerList.forEach((customer)->{
            UserNotification userNotification=new UserNotification(null,customer.getEmail(),notification1.getId(),customer.getCompanyId(),false,null,LocalDateTime.now(),notification1);
//            userNotificationList.add(userNotification);
            userNotificationRepository.save(userNotification);
            List<com.quantumai.customer.dto.UserNotificationDTO> myList=userNotificationRepository.findRecentNotificationsWithDetails(customer.getEmail(), LocalDateTime.now().minusDays(7),LocalDateTime.now().minusMonths(1));
            simpMessagingTemplate.convertAndSend("/topic/user/" + customer.getEmail() + "/notifications", myList);


        });
//        userNotificationRepository.saveAll(userNotificationList);



//        simpMessagingTemplate.convertAndSend("/topic/global/notifications", notification);
    }

//    public void sendNotificationToUser(String userId, NotificationDTO notificationDTO) {
//        // Sends notification to a specific user
//        simpMessagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", notificationDTO);
//    }


//    @Scheduled(fixedRate = 10000)
//    public void sendDummyNotification(){
//        System.out.println("Notification Triggered");
//        Notification notification =new Notification("111","hello","hello message", NotificationType.GLOBAL,"INFO",10001L,LocalDateTime.now(),LocalDateTime.now().plusDays(10));
//        sendNotificationToCompany(100003L, notification);
//    }
    public List<UserNotification> userNotificationDTO(String email){
        return userNotificationRepositoryImpl.findRecentNotificationsWithDetails(email);
    }
    public void updateUserNotificationDTO(List<UserNotification> userNotificationList){
        userNotificationList.forEach((userNotification -> {
            userNotification.setRead(true);
            userNotification.setReadAt(LocalDateTime.now());
        }));
        userNotificationRepository.saveAll(userNotificationList);
    }

    public void sendNotificationToUser(String userEmail, Notification notification) {
        // Sends notification to a specific user
        notification.setNotificationType(NotificationType.USER);
        Notification savedNotification = notificationRepository.save(notification);
        
        // Get customer details
        Customer customer = customerRepository.findByEmail(userEmail).orElse(null);
        if (customer != null) {
            UserNotification userNotification = new UserNotification(
                null, 
                userEmail, 
                savedNotification.getId(), 
                customer.getCompanyId(), 
                false, 
                null, 
                LocalDateTime.now(), 
                savedNotification
            );
            userNotificationRepository.save(userNotification);
            
            List<UserNotification> myList = userNotificationRepositoryImpl.findRecentNotificationsWithDetails(userEmail);
            simpMessagingTemplate.convertAndSend("/topic/user/" + userEmail + "/notifications", myList);
        }
    }
}
