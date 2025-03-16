package com.quantumai.customer.controller;

import com.quantumai.customer.dto.AdminResetPassword;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.security.JwtService;
import com.quantumai.customer.service.AdminService;
import com.quantumai.customer.service.SubscriptionService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "**")
@Slf4j
public class AdminAPI {

  @Autowired private AdminService adminService;

  @Autowired JwtService jwtService;

  @Autowired private SubscriptionService subscriptionService;

  @PostMapping("/login")
  public AuthenticationResponseDTO login(@RequestBody Admin admin) throws Exception {

    //        System.out.println("--->"+null);

    return adminService.login(admin, null);
  }

  @PostMapping("/resetPassword")
  public void resetPassword(@RequestBody AdminResetPassword adminResetPassword) throws Exception {

    adminService.updatePassword(adminResetPassword);
  }

  @PostMapping("/send-otp")
  public ResponseEntity<String> sendOtp(@RequestBody String email) throws Exception {
    //        String email = request.get("email");

    //        try{
    //            Thread.sleep(5000);
    //        }
    //        catch (InterruptedException e) {
    //            Thread.currentThread().interrupt(); // Restore interrupted status
    //            System.out.println("Thread was interrupted.");
    //        }
    adminService.generateOtp(email);

    return ResponseEntity.ok("OTP sent to email");
  }

  @PostMapping("/validate-otp")
  public ResponseEntity<Boolean> validateOtp(@RequestBody Map<String, String> request) {
    String email = request.get("email");
    String otp = request.get("otp");
    boolean isValid = adminService.validateOtp(email, otp);
    if (isValid) {
      adminService.clearOtp(email);
    }
    return ResponseEntity.ok(isValid);
  }

  @PostMapping(value = "/authenticate/{deviceId}")
  public ResponseEntity<AuthenticationResponseDTO> authenticate(
      @RequestBody AuthenticationRequestDTO authenticationRequestDTO, @PathVariable String deviceId)
      throws Exception {

    log.info(
        "Email and Password {} {}",
        authenticationRequestDTO.getEmail(),
        authenticationRequestDTO.getPassword());
    return ResponseEntity.ok(adminService.authenticate(authenticationRequestDTO, deviceId));
  }

  @GetMapping(value = "/getLoginToken/{email}/{deviceId}")
  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(
      @PathVariable String email, @PathVariable String deviceId) throws Exception {

    return ResponseEntity.ok(adminService.getLoginToken(email, deviceId));
  }

  @PostMapping("/addPlan")
  public void addPlan(@RequestBody Plans plan) {
    //        System.out.println("----------->"+plan);
    subscriptionService.addPlan(plan);
  }

  @PutMapping("/updatePlan")
  public void updatePlan(@RequestBody Plans plan) {
    //        System.out.println("----------->"+plan);
    subscriptionService.updatePlan(plan);
  }

  @DeleteMapping("/deletePlan/{id}")
  public void deletePlan(@PathVariable String id) {
    //        System.out.println("----------->"+id);
    subscriptionService.deletePlan(id);
  }

  @GetMapping("/getPlan/{id}")
  public Plans getPlan(@PathVariable String id) {
    //        System.out.println("----------->"+id);
    return subscriptionService.getPlan(id);
  }

  @GetMapping("/getAllPlan")
  public List<Plans> getAllPlan() {
    //        System.out.println("----------->"+id);
    return subscriptionService.getAllPlan();
  }
}
