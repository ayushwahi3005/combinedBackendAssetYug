package com.quantumai.customer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.quantumai.customer.controller.NotificationAPI;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.service.NotificationService;
import com.quantumai.customer.service.SubscriptionService;
import java.io.IOException;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@EnableScheduling
@SpringBootApplication
@Slf4j
// @EnableTransactionManagement
// @EnableWebMvc // Ensure Web MVC is enabled for global CORS configuration
public class CustomerApplication {

  public static void main(String[] args) {

    SpringApplication.run(CustomerApplication.class, args);

// FOR PROD
//      try {
//          String json = Files.readString(Paths.get("upkeep.json"));
//
//
//          json = json.replace("${FIREBASE_PROJECT_ID}", System.getenv("FIREBASE_PROJECT_ID"))
//                  .replace("${FIREBASE_PRIVATE_KEY_ID}", System.getenv("FIREBASE_PRIVATE_KEY_ID"))
//                  .replace("${FIREBASE_PRIVATE_KEY}", System.getenv("FIREBASE_PRIVATE_KEY").replace("\\n", "\n"))
//                  .replace("${FIREBASE_CLIENT_EMAIL}", System.getenv("FIREBASE_CLIENT_EMAIL"))
//                  .replace("${FIREBASE_CLIENT_ID}", System.getenv("FIREBASE_CLIENT_ID"))
//                  .replace("${FIREBASE_CLIENT_X509_CERT_URL}", System.getenv("FIREBASE_CLIENT_X509_CERT_URL"));
//
//          try (FileOutputStream out = new FileOutputStream("upkeep_resolved.json")) {
//              out.write(json.getBytes(StandardCharsets.UTF_8));
//          } catch (IOException e) {
//
//          }
//      }
//      catch (IOException e){
//          System.out.println(e);
//      }
//
//    try (FileInputStream serviceAccount = new FileInputStream("upkeep_resolved.json")) {
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .setDatabaseUrl("https://upkeep-22aee.firebaseio.com") // change if needed
//                    .build();
//
//            if (FirebaseApp.getApps().isEmpty()) {
//                FirebaseApp.initializeApp(options);
//            }
//        } catch (IOException e) {
//      e.printStackTrace();
//    }

     //////////

     /////Local
       try {
       FirebaseOptions options =
           new FirebaseOptions.Builder()
               .setCredentials(
                   GoogleCredentials.fromStream(
                       new ClassPathResource("upkeep.json").getInputStream()))
               .setDatabaseUrl("https://upkeep-22aee.firebaseio.com")
               .build();
       if (FirebaseApp.getApps().isEmpty()) { // <--- check with  this line
         FirebaseApp.initializeApp(options);
       }
     } catch (IOException e) {
       e.printStackTrace();
     }
  }

  @Autowired SubscriptionService subscriptionService;
  @Autowired  private UsersRepository usersRepository;
  @Autowired  private SubscriptionRepository subscriptionRepository;
  @Autowired  private NotificationAPI notificationAPI;
  @Autowired  private NotificationService notificationService;

  @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
//   @Scheduled(cron = "0 * * * * ?")
  public void updateAllSubscriptionExpiry() {
    System.out.println("updateAllSubscriptionExpiryTriggered->" + LocalDateTime.now());
    subscriptionService.isExpired();
    List<Subscription> subscriptionList=subscriptionRepository.findByStatus(SubscriptionEnum.ACTIVE);
      subscriptionList.forEach((data)->{
          long activeUserCount = usersRepository.countByCompanyIdAndStatus(data.getCompanyId(), StatusEnum.active);
          if(activeUserCount>data.getPerson()){
              log.info("Notification Sent For Inactivate user or upgrade subscription to avoid account lock for company Id {}",data.getCompanyId());
              Notification notification=new Notification("","ALERT!!","Alert!! Please Inactivate user or upgrade subscription to avoid account lock",NotificationType.COMPANY,"", data.getCompanyId(), LocalDateTime.now(),null);
              notificationService.sendNotificationToAdmin(data.getCompanyId(),notification);
          }

      });
  }
  //  @Bean
  //  public PlatformTransactionManager transactionManager(MongoDatabaseFactory
  // mongoDatabaseFactory){
  //    return  new MongoTransactionManager(mongoDatabaseFactory);
  //  }
}
