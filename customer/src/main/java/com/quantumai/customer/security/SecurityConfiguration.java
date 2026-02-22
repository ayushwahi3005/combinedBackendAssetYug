package com.quantumai.customer.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

  private final JwtAuthenticationFilter jwtAuthFilter;
  private final JwtAdminAuthenticationFilter jwtAdminAuthFilter;

  @Autowired
  @Qualifier("customerAuthProvider")
  private AuthenticationProvider customerAuthenticationProvider;

  @Autowired
  @Qualifier("adminAuthProvider")
  private AuthenticationProvider adminAuthenticationProvider;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf()
            .disable()
            .authorizeHttpRequests()

            // Public endpoints for customer paths
            .requestMatchers(
                    "/customer/addCustomer",
                    "/customer/getLoginToken/**",
                    "/customer/addCompanyInformation",
                    "/customer/getCompanyId/**",
                    "/customer/addUser",
                    "/customer/accountInfo/**",
                    "/customer/working/**",
                    "/users/invite/getUser/**",
                    "/customer/authenticate/**",
                    "/customer/addLoggedIn/**",
                    "/customer/isSameDevice/**",
                    "/customer/addLoggedInMobile/**",
                    "/customer/isSameBrowserAndDevice/**",
                    "/customer/removeSession/*",
                    "/customer/checkUserName/*",
                    "/customer/sentResetOTP",
                    "/customer/updatePassword/**",
                    "/assetyug-notifications/**",
                     "/invitation/**",
                     "/subscription/subscription-valid/**",
                    "/topic/**",
                    "/stripe/*",
                    "/app/**")
            .permitAll()

            // Public endpoints for admin paths
            .requestMatchers("/admin/authenticate/**", "/admin/login/**", "/admin/send-otp/**")
            .permitAll()

            // Protected paths for customers
            .requestMatchers(
                    "/customer/**",
                    "/assets/**",
                    "/users/**",
                    "/companycustomer/**",
                    "/payment/**",
                    "/subscription/**")
            .authenticated()

            // Protected paths for admins
            .requestMatchers("/admin/**")
            .authenticated()
            .anyRequest()
            .authenticated() // Default rule to require authentication for any other request
            .and()

            // Stateless session management
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()

            // Configure different authentication providers and filters based on path
            .authenticationProvider(customerAuthenticationProvider)
            .authenticationProvider(adminAuthenticationProvider)

            // Add JWT filters based on the role paths
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAdminAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }


  // @Bean
  // public SessionRegistry sessionRegistry() {
  //   return new SessionRegistryImpl();
  // }
}