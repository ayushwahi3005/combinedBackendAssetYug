package com.quantumai.customer.security;

import com.quantumai.customer.entity.ActiveSession;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.repository.ActiveSessionRepository;
import com.quantumai.customer.repository.AdminRepository;
import com.quantumai.customer.repository.CustomerRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private ActiveSessionRepository activeSessionRepository;

  @Autowired private AdminRepository adminRepository;

  @Value("${application.security.jwt.expiration}")
  private Long expirationTime;

//  public JwtAuthenticationFilter(@Value("${application.security.jwt.expiration}") String expirationTime) {
//    this.expirationTime = expirationTime;
//  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
    HttpServletResponse res = (HttpServletResponse) response;
    HttpServletRequest req = (HttpServletRequest) request;
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader(
        "Access-Control-Allow-Methods",
        "ACL, CANCELUPLOAD, CHECKIN, CHECKOUT, COPY, DELETE, GET, HEAD, LOCK, MKCALENDAR, MKCOL, MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT, SEARCH, UNCHECKOUT, UNLOCK, UPDATE, VERSION-CONTROL");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader(
        "Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Key, Authorization,Device-ID");

    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      res.setStatus(HttpServletResponse.SC_OK);
    } else {
      String requestURI = request.getRequestURI();

      final String authHeader = request.getHeader("Authorization");
      System.out.println("------------------------>" + authHeader);
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

          Optional<Admin> customer = adminRepository.findByEmail(userEmail);
          //    		      UserDetails userDetails =
          // this.userDetailsService.loadUserByUsername(customer.get());
          if (jwtService.isTokenValid(jwt, customer.get())) {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    customer.get(), null, customer.get().getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          }
        }
      } else {
        System.out.println("Customer HIT ====>");
        System.out.println(
            userEmail + " " + SecurityContextHolder.getContext().getAuthentication());
        //				if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() !=
        // null){
        //					String deviceId = jwtService.extractClaim(jwt,myclaims -> myclaims.get("deviceId",
        // String.class));
        //					String currentDeviceId = extractDeviceIdFromRequest(request);
        //					if (!deviceId.equals(currentDeviceId)) {
        //						System.out.println(deviceId+"-"+currentDeviceId);
        //						response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //						response.getWriter().write("Token is invalid for the current device.");
        //						return;
        //					}
        //				}
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          String deviceId =
              jwtService.extractClaim(jwt, myclaims -> myclaims.get("deviceId", String.class));
          //					String currentDeviceId = extractDeviceIdFromRequest(request);

          Claims claims = jwtService.extractAllClaims(jwt);
          System.out.println("All Claims: " + claims);
          //					System.out.println(deviceId+" "+currentDeviceId);
          Optional<Customer> customer = customerRepository.findByEmail(userEmail);

          Optional<ActiveSession> activeSessionOptional =
              activeSessionRepository.findByUserId(userEmail);
          String currentDeviceId = activeSessionOptional.get().getDeviceId();

          //    		      UserDetails userDetails =
          // this.userDetailsService.loadUserByUsername(customer.get());
          System.out.println("Device Id=="+deviceId + "  - " + currentDeviceId+" "+(Objects.equals(deviceId, currentDeviceId)));

          if (!deviceId.equals(currentDeviceId)||activeSessionOptional.get().getLastActivityTime().isBefore(LocalDateTime.now().minusSeconds(expirationTime))) {
            System.out.println(deviceId + "-" + currentDeviceId);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token is invalid for the current device.");
            return;
          }

          if (jwtService.isTokenValid(jwt, customer.get())) {
            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    customer.get(), null, customer.get().getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          }
        }
      }

      filterChain.doFilter(request, response);
    }
  }

  private String extractDeviceIdFromRequest(HttpServletRequest request) {
    return request.getHeader("device-id"); // Example: Retrieve device ID from a custom header
  }
}
