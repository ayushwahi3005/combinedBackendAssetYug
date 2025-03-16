package com.quantumai.customer.security;

import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.repository.AdminRepository;
import com.quantumai.customer.repository.CustomerRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Data
public class ApplicationConfig {

  private final CustomerRepository repository;
  private final AdminRepository adminRepository;

  @Bean
  public UserDetailsService userDetailsService() {
    return new UserDetailsService() {

      @Override
      public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO Auto-generated method stub
        Optional<Customer> customer = repository.findByEmail(username);
        return customer.orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
      }
    };
  }

  @Bean
  public UserDetailsService adminDetailsService() {
    return new UserDetailsService() {

      @Override
      public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // TODO Auto-generated method stub
        Optional<Admin> admin = adminRepository.findByEmail(username);
        return admin.orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
      }
    };
  }

  @Bean
  @Qualifier("customerAuthProvider")
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  @Qualifier("adminAuthProvider")
  @Primary
  public AuthenticationProvider adminAuthenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(adminDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  //	@Bean
  //	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
  //		AuthenticationManagerBuilder authenticationManagerBuilder =
  //				http.getSharedObject(AuthenticationManagerBuilder.class);
  //
  //		// Configure multiple authentication providers
  //		authenticationManagerBuilder
  //				.authenticationProvider(adminAuthenticationProvider())
  //				.authenticationProvider(authenticationProvider());
  //
  //		return authenticationManagerBuilder.build();
  //	}
  @Bean
  public ProviderManager authenticationManager() {
    List<AuthenticationProvider> providers = new ArrayList<>();
    providers.add(authenticationProvider());
    providers.add(adminAuthenticationProvider());
    return new ProviderManager(providers);
  }

  //	@Bean
  //	public AuthenticationManager authManager(AuthenticationConfiguration authConfig) throws
  // Exception {
  //		return authConfig.getAuthenticationManager();
  //	}

  //	@Bean
  //	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws
  // Exception {
  //		return config.getAuthenticationManager();
  //	}

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
