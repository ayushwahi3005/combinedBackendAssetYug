package com.quantumai.customer.config;

import com.quantumai.customer.filter.TrialExpirationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Autowired
    private TrialExpirationFilter trialExpirationFilter;

    @Bean
    public FilterRegistrationBean<TrialExpirationFilter> trialExpirationFilterRegistration() {
        FilterRegistrationBean<TrialExpirationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(trialExpirationFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2); // Set order after authentication filter
        registration.setName("trialExpirationFilter");
        return registration;
    }
}