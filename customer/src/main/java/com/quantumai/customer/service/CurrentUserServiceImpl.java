package com.quantumai.customer.service;

import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

  private final UsersRepository usersRepository;
  private final CustomerRepository customerRepository;

  @Override
  public String getCurrentUserDisplayName() {
    String email = resolveCurrentUserEmail();
    if (email == null || email.isBlank()) {
      return null;
    }

    Optional<Users> userOpt = usersRepository.findByEmail(email);
    if (userOpt.isPresent()) {
      return formatName(userOpt.get().getFirstName(), userOpt.get().getLastName(), email);
    }

    Optional<Customer> customerOpt = customerRepository.findByEmail(email);
    if (customerOpt.isPresent()) {
      Customer customer = customerOpt.get();
      return formatName(customer.getFirstName(), customer.getLastName(), email);
    }

    return email;
  }

  private String resolveCurrentUserEmail() {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated()
              && !"anonymousUser".equals(auth.getPrincipal())) {
        return auth.getName();
      }
    } catch (Exception e) {
      log.warn("Could not resolve current user email: {}", e.getMessage());
    }
    return null;
  }

  private String formatName(String firstName, String lastName, String fallbackEmail) {
    String first = trimToNull(firstName);
    if (first != null) {
      String last = lastName != null ? lastName.trim() : "";
      return last.isEmpty() ? first : first + " " + last;
    }
    return fallbackEmail;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
