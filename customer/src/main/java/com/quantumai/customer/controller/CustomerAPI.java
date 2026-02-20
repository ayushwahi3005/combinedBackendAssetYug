package com.quantumai.customer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.service.ActiveSessionService;
import com.quantumai.customer.service.CustomerService;
import com.quantumai.customer.service.TrialService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@RestController
@RequestMapping("/customer")
@Slf4j
public class CustomerAPI {

  @Autowired private CustomerService customerService;
  @Autowired private ActiveSessionService activeSessionService;
  @Autowired private TrialService trialService;

  // ─── Public endpoints (no auth needed) ───────────────────────────────────
  // These are called before login so cannot require authentication

  @GetMapping(value = "/working")
  public ResponseEntity<String> working() throws Exception {
    return ResponseEntity.ok("Working OK");
  }

  @GetMapping(value = "/checkUserName/{email}")

  public ResponseEntity<Boolean> checkUserName(@PathVariable String email) throws Exception {
    return ResponseEntity.ok(customerService.checkCustomer(email));
  }

  @PostMapping(value = "/addCustomer")
  public ResponseEntity<BaseResponseDTO> addCustomer(@RequestBody CustomerDTO customerDTO)
          throws Exception {
    return ResponseEntity.ok(customerService.addCustomer(customerDTO));
  }

  @PostMapping(value = "/authenticate/{deviceId}")
  public ResponseEntity<AuthenticationResponseDTO> authenticate(
          @RequestBody AuthenticationRequestDTO authenticationRequestDTO,
          @PathVariable String deviceId) throws Exception {
    return ResponseEntity.ok(customerService.authenticate(authenticationRequestDTO, deviceId));
  }

  @PostMapping(value = "/getLoginToken")
  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(@RequestBody JsonNode jsonNode)
          throws Exception {
    String email = jsonNode.get("email").asText();
    String password = jsonNode.get("password").asText();
    String deviceId = jsonNode.get("deviceId").asText();
    return ResponseEntity.ok(customerService.getLoginToken(email, password, deviceId));
  }

  @PostMapping(value = "/sentResetOTP")
  public void sentResetOTP(@RequestBody String email) throws NoEmailFoundException {
    customerService.sentResetOTP(email);
  }

  @PostMapping(value = "/updatePassword/{email}")
  @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
  public void updatePassword(@RequestBody JsonNode obj,@PathVariable String email)

          throws OTPException, FirebaseAuthException, NoEmailFoundException {

    String otp = obj.get("otp").asText();
    String password = obj.get("password").asText();
    customerService.updatePassword(email, otp, password);
  }

  // ─── Authenticated endpoints (same person check only) ────────────────────

  @GetMapping(value = "/get/{email}")
  @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
  public CustomerDTO getCustomer(@PathVariable String email) throws Exception {
    return customerService.getCustomer(email);
  }

  @GetMapping(value = "/getsubscription/{email}")
  @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
  public CustomerSubscribedDTO getCustomerSubscribed(@PathVariable String email) throws Exception {
    return customerService.getCustomerSubscription(email);
  }

  @PostMapping(value = "/isSameBrowserAndDevice")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public ResponseEntity<Boolean> isSameBrowser(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String deviceId = jsonNode.get("deviceId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(activeSessionService.isSameBrowserAndDevice(userId, deviceId, userAgent));
  }

  @PostMapping(value = "/isSameDevice")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public ResponseEntity<Boolean> isSameDevice(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String mobileId = jsonNode.get("mobileId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(activeSessionService.isSameMobile(userId, mobileId, userAgent));
  }

  @PostMapping(value = "/addLoggedIn")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void addLoggedIn(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String deviceId = jsonNode.get("deviceId").asText();
      String userAgent = jsonNode.get("userAgent").asText();
      activeSessionService.createOrUpdateSession(userId, null, userAgent, deviceId);
    }
  }

  @PostMapping(value = "/addLoggedInMobile")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void addLoggedInMobile(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String mobileId = jsonNode.get("mobileId").asText();
      String userAgent = jsonNode.get("userAgent").asText();
      activeSessionService.createOrUpdateSessionMobile(userId, null, userAgent, mobileId);
    }
  }

  @DeleteMapping(value = "/removeSession/{userId}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void removeSession(@PathVariable String userId) throws Exception {
    activeSessionService.removeSession(userId);
  }

  // ─── Company-scoped endpoints (same company check) ────────────────────────

  @PostMapping(value = "/addCompanyInformation")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyInformation.companyId)")
  public ResponseEntity<CompanyInformation> addCompanyInformation(
          @RequestBody CompanyInformation companyInformation) throws Exception {
    customerService.addCompanyInformation(companyInformation);
    return ResponseEntity.ok(companyInformation);
  }

  @PostMapping(value = "/updateCompanyInformation")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyInformation.companyId)")
  public ResponseEntity<CompanyInformation> updateCompanyInformation(
          @RequestBody CompanyInformation companyInformation) throws Exception {
    log.info("Updating company information: {}", companyInformation);
    customerService.addCompanyInformation(companyInformation);
    return ResponseEntity.ok(companyInformation);
  }

  @GetMapping(value = "/getCompanyInformation/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<CompanyInformation> getCompanyInformation(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.getcompanyInformation(companyId));
  }

  @GetMapping(value = "/getCompanyId/{email}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public ResponseEntity<CompanyIdDTO> getCompanyId(@PathVariable String email) throws Exception {
    return ResponseEntity.ok(customerService.getCompanyId(email));
  }

  @PostMapping(value = "/addUser")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #customerDTO.companyId)")
  public ResponseEntity<BaseResponseDTO> addUser(@RequestBody CustomerDTO customerDTO)
          throws Exception {
    return ResponseEntity.ok(customerService.addUsers(customerDTO));
  }

  @GetMapping(value = "/getRegisteredUsers/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<List<String>> getRegisteredUsers(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.activeUsers(companyId));
  }

  @GetMapping(value = "/accountInfo/{customerEmail}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public ResponseEntity<AccountLockInfoDTO> getAccountInfo(@PathVariable String customerEmail)
          throws Exception {
    return ResponseEntity.ok(customerService.getAccountInfo(customerEmail));
  }

  @PostMapping(value = "/accountInfo/update")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void updateAccountInfo(@RequestBody AccountLockInfoDTO accountLockInfoDTO) throws Exception {
    customerService.updateAccountInfo(accountLockInfoDTO);
  }

  @DeleteMapping(value = "/deleteAccount/{companyId}/{email}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public void deleteUser(@PathVariable Long companyId, @PathVariable String email)
          throws Exception {
    customerService.deleteUser(companyId, email);
  }

  // ─── Role & Permission ────────────────────────────────────────────────────

  @PostMapping(value = "/roleAndPermission/add")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #customRoleDTO.companyId)")
  public void addRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {
    customerService.addRoleAndPermission(customRoleDTO);
  }

  @PutMapping(value = "/roleAndPermission/update")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #customRoleDTO.companyId)")
  public void updateRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {
    customerService.addRoleAndPermission(customRoleDTO);
  }

  @GetMapping(value = "/roleAndPermission/get/{companyId}/{name}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<CustomRoleDTO> getRoleAndPermissionByName(
          @PathVariable Long companyId, @PathVariable String name) throws Exception {
    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  @GetMapping(value = "/roleAndPermission/get/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<List<CustomRoleDTO>> getRoleAndPermission(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.getRoleAndPermission(companyId));
  }

  @DeleteMapping(value = "/roleAndPermission/{id}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void deleteRoleAndPermission(@PathVariable String id) throws Exception {
    customerService.deleteRoleAndPermission(id);
  }

  @GetMapping(value = "/countByRole/{companyId}/{roleName}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<CountNameByRole> countByRole(
          @PathVariable Long companyId, @PathVariable String roleName) throws Exception {
    return ResponseEntity.ok(customerService.countByRoleName(roleName, companyId));
  }

  @GetMapping(value = "/roleAndPermissionByName/get/{companyId}/{name}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<CustomRoleDTO> roleAndPermissionByName(
          @PathVariable Long companyId, @PathVariable String name) throws Exception {
    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  // ─── Location & Bin ───────────────────────────────────────────────────────

  @PostMapping(value = "/addlocation")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #location.companyId)")
  public ResponseEntity<Location> addLocation(@RequestBody Location location)
          throws LocationAlreadyPresentException {
    return ResponseEntity.ok(customerService.addLocation(location));
  }

  @PutMapping(value = "/addlocation")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #location.companyId)")
  public ResponseEntity<Location> updateLocation(@RequestBody Location location) {
    return ResponseEntity.ok(customerService.updateLocation(location));
  }

  @GetMapping(value = "/getAllLocation/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<List<Location>> getAllLocation(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllLocation(companyId));
  }

  @DeleteMapping(value = "/deleteLocation/{id}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void deleteLocation(@PathVariable String id) throws LocationDeletionException {
    customerService.deleteLocation(id);
  }

  @PostMapping(value = "/addbin")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #bindDto.companyId)")
  public ResponseEntity<Bin> addBin(@RequestBody BinDTO bindDto) throws BinAlreadyPresentException {
    return ResponseEntity.ok(customerService.addBin(bindDto));
  }

  @PutMapping(value = "/addbin")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #bindDto.companyId)")
  public ResponseEntity<Bin> updateBin(@RequestBody BinDTO bindDto) {
    return ResponseEntity.ok(customerService.updateBin(bindDto));
  }

  @GetMapping(value = "/getAllBin/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<List<BinDTO>> getAllBin(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllBin(companyId));
  }

  @GetMapping("/locations-with-bins/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<List<LocationWithBinsDTO>> getLocationsWithBins(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getLocationsWithBins(companyId));
  }

  @DeleteMapping(value = "/deleteBin/{id}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void deleteBin(@PathVariable String id) throws LocationDeletionException {
    customerService.deleteBin(id);
  }

  // ─── Import History ───────────────────────────────────────────────────────

  @PostMapping(value = "/addImportHistory")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void addImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.addImportHistory(importHistory);
  }

  @PostMapping(value = "/getAllImportHistory/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public Page<ImportHistoryDTO> getImportHistory(
          @PathVariable Long companyId,
          @RequestParam(defaultValue = "0", required = false) int pageNumber,
          @RequestParam(defaultValue = "10", required = false) int pageSize,
          @RequestBody(required = false) JsonNode filter) {
    log.info("Filter received for import history: {}", filter);
    if (filter != null && filter.has("startDate") && filter.has("endDate")) {
      return customerService.getImportHistoryListWithDateFilter(companyId, pageNumber, pageSize,
              filter.get("startDate").asText(), filter.get("endDate").asText());
    }
    return customerService.getImportHistoryList(companyId, pageNumber, pageSize);
  }

  @PutMapping(value = "/updateImportHistory")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void updateImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.updateImportHistory(importHistory);
  }

  // ─── Card & Stripe ────────────────────────────────────────────────────────

  @PostMapping(value = "/addCardDetails")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void addCardDetails(@RequestBody CustomerStripeDetails customerStripeDetails) {
    customerService.addCardDetails(customerStripeDetails);
  }

  @GetMapping(value = "/getCardDetails/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public CustomerStripeDetails getCardDetails(@PathVariable Long companyId) {
    return customerService.getCardDetails(companyId);
  }

  @DeleteMapping(value = "/deleteCardDetails/{id}")
  @PreAuthorize("@appSecurity.isAuthenticated(authentication)")
  public void deleteCardDetails(@PathVariable String id) {
    customerService.deleteCardDetails(id);
  }

  // ─── Trial ────────────────────────────────────────────────────────────────

  @GetMapping(value = "/trial-status-details/{companyId}")
  @PreAuthorize("@appSecurity.isSameCompany(authentication, #companyId)")
  public ResponseEntity<TrialStatus> getTrialStatusDetails(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(trialService.getTrialDetails(companyId));
  }

  @GetMapping(value = "/trial-status/{email}")
  @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
  public ResponseEntity<Map<String, Object>> getTrialStatus(@PathVariable String email) {
    Map<String, Object> response = new HashMap<>();
    boolean isTrialActive = trialService.isTrialActive(email);
    boolean isTrialExpired = trialService.isTrialExpired(email);
    response.put("isTrialActive", isTrialActive);
    response.put("isTrialExpired", isTrialExpired);
    if (!isTrialExpired && isTrialActive) {
      trialService.getTrialStatus(email).ifPresent(trial -> {
        response.put("trialEndDate", trial.getTrialEndDate());
        response.put("daysRemaining", java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDateTime.now(), trial.getTrialEndDate()));
      });
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/activate-subscription/{email}")
  @PreAuthorize("@appSecurity.isEmailSame(authentication,#email)")
  public ResponseEntity<Map<String, String>> activateSubscription(@PathVariable String email) {
    trialService.activatePaidSubscription(email);
    Map<String, String> response = new HashMap<>();
    response.put("message", "Subscription activated successfully");
    response.put("status", "success");
    return ResponseEntity.ok(response);
  }
}