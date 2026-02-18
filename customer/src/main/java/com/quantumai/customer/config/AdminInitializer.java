package com.quantumai.customer.config;

import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "ayushwahi3005@gmail.com";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_ADMIN_ROLE = "ADMIN";

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        // Lazy fetch PasswordEncoder from application context
        PasswordEncoder passwordEncoder = applicationContext.getBean(PasswordEncoder.class);

        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
            admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
            admin.setRole(DEFAULT_ADMIN_ROLE);
            adminRepository.save(admin);
            log.info("Default admin created with email: {}", DEFAULT_ADMIN_EMAIL);
        } else {
            log.info("Admin user(s) already exist. No default admin created.");
        }
    }
}
