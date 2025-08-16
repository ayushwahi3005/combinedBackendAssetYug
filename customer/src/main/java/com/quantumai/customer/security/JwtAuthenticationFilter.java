package com.quantumai.customer.security;

import com.quantumai.customer.entity.ActiveSession;
import com.quantumai.customer.entity.ActiveSessionMobile;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.repository.ActiveSessionMobileRepository;
import com.quantumai.customer.repository.ActiveSessionRepository;
import com.quantumai.customer.repository.AdminRepository;
import com.quantumai.customer.repository.CustomerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private ActiveSessionRepository activeSessionRepository;
  @Autowired private ActiveSessionMobileRepository activeSessionMobileRepository;

  @Autowired private AdminRepository adminRepository;

  @Value("${application.security.jwt.expiration}")
  private Long expirationTime;

  @Value("${allowed-origins}")
  private String allowedOrigins;

  //  public JwtAuthenticationFilter(@Value("${application.security.jwt.expiration}") String
  // expirationTime) {
  //    this.expirationTime = expirationTime;
  //  }
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    HttpServletResponse res = (HttpServletResponse) response;
    HttpServletRequest req = (HttpServletRequest) request;

    response.setHeader("Access-Control-Allow-Origin", allowedOrigins);
//    response.setHeader("Access-Control-Allow-Origin", "*");

    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader(
        "Access-Control-Allow-Methods",
        "ACL, CANCELUPLOAD, CHECKIN, CHECKOUT, COPY, DELETE, GET, HEAD, LOCK, MKCALENDAR, MKCOL, MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT, SEARCH, UNCHECKOUT, UNLOCK, UPDATE, VERSION-CONTROL");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader(
        "Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Key, Authorization,Device-ID,Mobile-ID,companyId");

    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      res.setStatus(HttpServletResponse.SC_OK);
      return;
    }

    String requestURI = request.getRequestURI();
    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    jwt = authHeader.substring(7);
    userEmail = jwtService.extractUserEmail(jwt);

    if (requestURI.startsWith("/admin")) {
      System.out.println("ADMIN HIT ====>");

      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        Optional<Admin> admin = adminRepository.findByEmail(userEmail);

        if (admin.isPresent() && jwtService.isTokenValid(jwt, admin.get())) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  admin.get(), null, admin.get().getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }

    } else {
      System.out.println("Customer HIT ====>");
      log.info("User Email: {}", userEmail);
      if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        String deviceId = request.getHeader("device-id");
        String mobileId = request.getHeader("mobile-id");

        Optional<Customer> customer = customerRepository.findByEmail(userEmail);
        Optional<ActiveSession> activeSessionOptional =
            activeSessionRepository.findByUserId(userEmail);
        Optional<ActiveSessionMobile> activeSessionMobileOptional =
            activeSessionMobileRepository.findByUserId(userEmail);

        String currentDeviceId = activeSessionOptional.map(ActiveSession::getDeviceId).orElse(null);
        String currentMobileId =
            activeSessionMobileOptional.map(ActiveSessionMobile::getMobileId).orElse(null);

//        log.info("deviceId: " + deviceId + " " + (deviceId == null));
//        log.info("mobileId: " + mobileId + " " + (mobileId == null));
        // 🔒 Reject if both headers are missing
        if (deviceId == null && mobileId == null) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Token is invalid: missing device or mobile ID.");

          return;
        }

        // 🔒 Reject if both device and mobile ID do not match stored sessions
        boolean deviceMismatch = currentDeviceId != null && !currentDeviceId.equals(deviceId);
        boolean mobileMismatch = currentMobileId != null && !currentMobileId.equals(mobileId);
        boolean sessionExpiredDevice =
            activeSessionOptional.isPresent()
                && activeSessionOptional
                    .get()
                    .getLastActivityTime()
                    .isBefore(LocalDateTime.now().minusSeconds(expirationTime));

        boolean sessionExpiredMobile =
            activeSessionMobileOptional.isPresent()
                && activeSessionMobileOptional
                    .get()
                    .getLastActivityTime()
                    .isBefore(LocalDateTime.now().minusSeconds(expirationTime));
//        log.info(
//            "DeviceMismatch: {} MobileMismatch: {} SessionExpired: {}",
//            deviceMismatch,
//            mobileMismatch,
//            sessionExpiredDevice);
        //      if ((deviceMismatch && mobileMismatch) || sessionExpired) {
        //        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //        response.getWriter().write("Token is invalid for the current device.");
        //        return;
        //      }
        if ((deviceId != null && !Objects.equals(currentDeviceId, deviceId))
            || (sessionExpiredDevice)) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Token is invalid for the current device.");
          return;
        }
        if ((mobileId != null && !Objects.equals(currentMobileId, mobileId))
            || (sessionExpiredMobile)) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Token is invalid for the current device.");
          return;
        }

        if (customer.isPresent() && jwtService.isTokenValid(jwt, customer.get())) {
          UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                  customer.get(), null, customer.get().getAuthorities());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        } else {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Unauthorized: Invalid token or user not found");
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }



  private String extractDeviceIdFromRequest(HttpServletRequest request) {
    return request.getHeader("device-id"); // Example: Retrieve device ID from a custom header
  }
}
