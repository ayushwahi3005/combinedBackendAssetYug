package com.quantumai.customer.security;

import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.repository.AdminRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
public class JwtAdminAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  @Autowired private AdminRepository adminRepository;

  @Value("${allowed-origins}")
  private String allowedOrigins;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // TODO Auto-generated method stub
    HttpServletResponse res = (HttpServletResponse) response;
    HttpServletRequest req = (HttpServletRequest) request;
//    response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
    response.setHeader("Access-Control-Allow-Origin", allowedOrigins);

    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader(
        "Access-Control-Allow-Methods",
        "ACL, CANCELUPLOAD, CHECKIN, CHECKOUT, COPY, DELETE, GET, HEAD, LOCK, MKCALENDAR, MKCOL, MOVE, OPTIONS, POST, PROPFIND, PROPPATCH, PUT, REPORT, SEARCH, UNCHECKOUT, UNLOCK, UPDATE, VERSION-CONTROL");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader(
        "Access-Control-Allow-Headers",
        "Origin, X-Requested-With, Content-Type, Accept, Key, Authorization");

    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
      res.setStatus(HttpServletResponse.SC_OK);
    } else {
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
      filterChain.doFilter(request, response);
    }
  }
}
