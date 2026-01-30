package com.quantumai.customer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.quantumai.customer.controller.NotificationAPI;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.TrialStatusRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.service.EmailService;
import com.quantumai.customer.service.NotificationService;
import com.quantumai.customer.service.PurgeService;
import com.quantumai.customer.service.SubscriptionService;
import java.io.IOException;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;


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
  @Autowired private PurgeService purgeService;
  @Autowired private TrialStatusRepository trialStatusRepository;
  @Autowired private EmailService emailService;

    @Value("${purge.expiry.days}")
    private int purgeExpiryDays;

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

      log.info("Purge Service Called");
      purgeService.purgeOldData();
      expirySendNotification();



  }


  public void expirySendNotification() {
      log.info("Notification Sent For Inactivate user or upgrade subscription to avoid account lock for company Id ");
      trialStatusRepository.findByTrialEndDateBetween(LocalDateTime.now().minusDays(purgeExpiryDays), LocalDateTime.now())
              .forEach(trialStatus -> {
                  Optional<Subscription> data=subscriptionRepository.findByCompanyIdAndStatus(trialStatus.getCompanyId(),SubscriptionEnum.ACTIVE);
                  if(data.isEmpty()){
                      Long companyId = trialStatus.getCompanyId();
                      log.info("Trial Expiry Notification Sent For Company Id {}",companyId);
                      Notification notification=new Notification("","ALERT!!","Alert!! Your Trial Has Ended\n" +
                              "We’ll keep your data for the next "+purgeExpiryDays+" days.\n" +
                              "Upgrade now to continue accessing your account.",NotificationType.COMPANY,"", companyId, LocalDateTime.now(),null);
                      notificationService.sendNotificationToAdmin(companyId,notification);
                      emailToAdmin(companyId);
                  }


              });

  }
  public void emailToAdmin(Long companyId){
      List<Users> userList = usersRepository.findByCompanyId(companyId);
//      Optional<TrialStatus> trialStatusOptional=trialStatusRepository.findByCompanyId(companyId);
      userList=userList.stream().filter((user)->user.getRole().getName().equals("ADMIN")).toList();
        userList.forEach((user)->{
            emailService.sendEmail(user.getEmail(),"AssetYug - Trial Ended",
                    "The trial period for your account has ended. Please note that we will retain your data for the next "+purgeExpiryDays+" days. During this time, you may choose to upgrade your plan to continue accessing your data and services without interruption.\n" +
                    "\n" +
                    "If no action is taken within one week, the data associated with your trial account may be permanently deleted.\n" +
                    "\n" +
                    "If you have any questions or need assistance, feel free to reach out.",user.getFirstName());
        });
  }

}
