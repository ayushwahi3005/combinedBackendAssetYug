package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.AdminResetPassword;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.entity.Admin;
import com.quantumai.customer.entity.Customer;
import com.quantumai.customer.entity.Plans;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.exception.TheMailException;
import com.quantumai.customer.exception.UserEmailAlreadyVerifiedException;
import com.quantumai.customer.exception.UserException;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.security.JwtService;
import com.quantumai.customer.service.AdminService;
import com.quantumai.customer.service.SubscriptionService;
import com.quantumai.customer.service.UserService;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@Slf4j
@Tag(name = "Admin", description = "Admin Management API")
public class AdminAPI {

    @Autowired
    private UserService userService;

  @Autowired private UsersRepository usersRepository;

    @Operation(summary = "Resend User Verification", description = "Endpoint to resend user verification")
    @PostMapping("/resend-verification/{companyId}/{email}")
    public ResponseEntity<String> resendUserVerification(@PathVariable Long companyId, @PathVariable String email) {
        try {
            userService.resendVerificationEmail(email, companyId);
            return ResponseEntity.ok("Verification email resent.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to resend verification: " + e.getMessage());
        }
    }

  @Autowired private AdminService adminService;

  @Autowired JwtService jwtService;

  @Autowired private SubscriptionService subscriptionService;

  @Operation(summary = "Login", description = "Endpoint to login")
  @PostMapping("/login")
  public AuthenticationResponseDTO login(@RequestBody Admin admin) throws Exception {

    //        System.out.println("--->"+null);

    return adminService.login(admin, null);
  }

  @Operation(summary = "Reset Password", description = "Endpoint to reset password")
  @PostMapping("/resetPassword")
  public void resetPassword(@RequestBody AdminResetPassword adminResetPassword) throws Exception {

    adminService.updatePassword(adminResetPassword);
  }

  @Operation(summary = "Send Otp", description = "Endpoint to send otp")
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

  @Operation(summary = "Validate Otp", description = "Endpoint to validate otp")
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

  @Operation(summary = "Authenticate", description = "Endpoint to authenticate")
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

  @Operation(summary = "Get Login Token", description = "Endpoint to get login token")
  @GetMapping(value = "/getLoginToken/{email}/{deviceId}")
  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(
      @PathVariable String email, @PathVariable String deviceId) throws Exception {

    return ResponseEntity.ok(adminService.getLoginToken(email, deviceId));
  }

  @Operation(summary = "Add Plan", description = "Endpoint to add plan")
  @PostMapping("/addPlan")
  public void addPlan(@RequestBody Plans plan) {
    //        System.out.println("----------->"+plan);
    subscriptionService.addPlan(plan);
  }

  @Operation(summary = "Update Plan", description = "Endpoint to update plan")
  @PutMapping("/updatePlan")
  public void updatePlan(@RequestBody Plans plan) {
    //        System.out.println("----------->"+plan);
    subscriptionService.updatePlan(plan);
  }

  @Operation(summary = "Delete Plan", description = "Endpoint to delete plan")
  @DeleteMapping("/deletePlan/{id}")
  public void deletePlan(@PathVariable String id) {
    //        System.out.println("----------->"+id);
    subscriptionService.deletePlan(id);
  }

  @Operation(summary = "Get Plan", description = "Endpoint to get plan")
  @GetMapping("/getPlan/{id}")
  public Plans getPlan(@PathVariable String id) {
    //        System.out.println("----------->"+id);
    return subscriptionService.getPlan(id);
  }

  @Operation(summary = "Get All Plan", description = "Endpoint to get all plan")
  @GetMapping("/getAllPlan")
  public List<Plans> getAllPlan() {
    //        System.out.println("----------->"+id);
    return subscriptionService.getAllPlan();
  }
  @Operation(summary = "Get All Customers", description = "Endpoint to get all customers")
  @GetMapping(value = "/customers")
  public ResponseEntity<List<Users>> getAllCustomers() {
//    System.out.println("working");
    return ResponseEntity.ok(usersRepository.findAll());
  }
  @Operation(summary = "Resend Firebase Verification Email", description = "Endpoint to resend firebase verification email")
  @PostMapping(value = "/resend-email-firebase-verification/{companyId}/{email}")
  public ResponseEntity<Map<String, String>> resendFirebaseVerificationEmail(
          @PathVariable Long companyId,
          @PathVariable String email) throws UserException, TheMailException, FirebaseAuthException, UserEmailAlreadyVerifiedException {

    userService.resendFirebaseVerificationEmail(email, companyId);

    return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Firebase verification email has been resent successfully"
    ));
  }
}
