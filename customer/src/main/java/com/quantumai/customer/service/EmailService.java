package com.quantumai.customer.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    public void sendEmail(String email, String subject, String body,String firstName) {
        // Implementation for sending email
        log.info("Sending email to: {}, subject: {}", email, subject);
        // Actual email sending logic would go here
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);


            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText("Hi "+firstName+",\n" +
                    "\n" +
                    body+
                    "\n" +
                    "\n" +
                    "Best regards,\n" +
                    "Asset Yug Team");


            emailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in sending mail: " + e);
        }
    }

}
