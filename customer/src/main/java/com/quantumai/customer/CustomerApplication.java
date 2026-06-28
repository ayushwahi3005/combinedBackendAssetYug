package com.quantumai.customer;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@Slf4j
public class CustomerApplication {

  public static void main(String[] args) {
    SpringApplication.run(CustomerApplication.class, args);

    try {
      FirebaseOptions options =
          new FirebaseOptions.Builder()
              .setCredentials(
                  GoogleCredentials.fromStream(
                      new ClassPathResource("upkeep.json").getInputStream()))
              .setDatabaseUrl("https://upkeep-22aee.firebaseio.com")
              .build();
      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
