package com.quantumai.customer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.quantumai.customer.service.SubscriptionService;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@SpringBootApplication
// @EnableTransactionManagement
// @EnableWebMvc // Ensure Web MVC is enabled for global CORS configuration
public class CustomerApplication {

  public static void main(String[] args) {

    SpringApplication.run(CustomerApplication.class, args);

    String json = Files.readString(Paths.get("upkeep.json"));
// FOR PROD
        json = json.replace("${FIREBASE_PROJECT_ID}", System.getenv("FIREBASE_PROJECT_ID"))
                   .replace("${FIREBASE_PRIVATE_KEY_ID}", System.getenv("FIREBASE_PRIVATE_KEY_ID"))
                   .replace("${FIREBASE_PRIVATE_KEY}", System.getenv("FIREBASE_PRIVATE_KEY").replace("\\n", "\n"))
                   .replace("${FIREBASE_CLIENT_EMAIL}", System.getenv("FIREBASE_CLIENT_EMAIL"))
                   .replace("${FIREBASE_CLIENT_ID}", System.getenv("FIREBASE_CLIENT_ID"))
                   .replace("${FIREBASE_CLIENT_X509_CERT_URL}", System.getenv("FIREBASE_CLIENT_X509_CERT_URL"));

        try (FileOutputStream out = new FileOutputStream("upkeep_resolved.json")) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
       
    try (FileInputStream serviceAccount = new FileInputStream("upkeep_resolved.json")) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://upkeep-22aee.firebaseio.com") // change if needed
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
      e.printStackTrace();
    }

     //////////

     /////Local
    //   try {
    //   FirebaseOptions options =
    //       new FirebaseOptions.Builder()
    //           .setCredentials(
    //               GoogleCredentials.fromStream(
    //                   new ClassPathResource("upkeep.json").getInputStream()))
    //           .setDatabaseUrl("https://upkeep-22aee.firebaseio.com")
    //           .build();
    //   if (FirebaseApp.getApps().isEmpty()) { // <--- check with  this line
    //     FirebaseApp.initializeApp(options);
    //   }
    // } catch (IOException e) {
    //   e.printStackTrace();
    // }
  }

  @Autowired SubscriptionService subscriptionService;

  //    @Bean
  //    public WebMvcConfigurer corsConfigurer() {
  //        return new WebMvcConfigurer() {
  //            @Override
  //            public void addCorsMappings(CorsRegistry registry) {
  //                registry.addMapping("/**")
  //                        .allowedOrigins("http://localhost:4200") // Allow requests from this
  // origin
  //                        .allowedMethods("GET", "POST", "PUT", "DELETE") // Allow specific HTTP
  // methods
  //                        .allowedHeaders("*") // Allow all headers
  //                        .exposedHeaders("*") // Expose additional headers if needed
  //                        .allowCredentials(true); // Allow sending credentials (e.g., cookies)
  //            }
  //        };
  //    }
  //    @Bean
  //    CorsConfigurationSource corsConfigurationSource() {
  //        UrlBasedCorsConfigurationSource source = new
  //                UrlBasedCorsConfigurationSource();
  //        source.registerCorsConfiguration("/**", new
  // CorsConfiguration().applyPermitDefaultValues());
  //        return source;
  //    }
  //    @Bean
  //    WebMvcConfigurer corsConfigurer() {
  //	        return new WebMvcConfigurer() {
  //	            @Override
  //	            public void addCorsMappings(CorsRegistry registry) {
  //	            	registry.addMapping("/**")
  //	                .allowedOrigins("**")
  //
  //	                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
  //	                .allowedHeaders( "Authorization","Content-Type", "Date", "Total-Count",
  // "loginInfo","jwt_token","Device-ID","device-id")
  //                    .exposedHeaders("*","Content-Type", "Date", "Total-Count", "loginInfo",
  // "jwt_token","Authorization","Device-ID","device-id")
  //	                .allowCredentials(true)
  //	                .maxAge(3600);
  //	            }
  //	        };
  //	    }

  @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
  public void updateAllSubscriptionExpiry() {
    System.out.println("updateAllSubscriptionExpiryTriggered->" + LocalDateTime.now());
    subscriptionService.isExpired();
  }
  //  @Bean
  //  public PlatformTransactionManager transactionManager(MongoDatabaseFactory
  // mongoDatabaseFactory){
  //    return  new MongoTransactionManager(mongoDatabaseFactory);
  //  }
}
