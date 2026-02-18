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

  @GetMapping(value = "/trial-status-details/{companyId}")
  public ResponseEntity<TrialStatus> getTrialStatus(@PathVariable Long companyId) throws Exception {
    return ResponseEntity.ok(trialService.getTrialDetails(companyId));
  }

  @GetMapping(value = "/working")
  public ResponseEntity<String> working() throws Exception {
    System.out.println("working");
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
      @RequestBody AuthenticationRequestDTO authenticationRequestDTO, @PathVariable String deviceId)
      throws Exception {

    return ResponseEntity.ok(customerService.authenticate(authenticationRequestDTO, deviceId));
  }

  //  @GetMapping(value = "/getLoginToken/{email}/{deviceId}")
  //  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(
  //      @PathVariable String email, @PathVariable String deviceId) throws Exception {
  //
  //    return ResponseEntity.ok(customerService.getLoginToken(email, deviceId));
  //  }
  @PostMapping(value = "/getLoginToken")
  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(@RequestBody JsonNode jsonNode)
      throws Exception {

    String email = jsonNode.get("email").asText();
    String password = jsonNode.get("password").asText();
    String deviceId = jsonNode.get("deviceId").asText();

    return ResponseEntity.ok(customerService.getLoginToken(email, password, deviceId));
  }

  @GetMapping(value = "/get/{email}")
  public CustomerDTO getCustomer(@PathVariable String email) throws Exception {

    return customerService.getCustomer(email);
  }

  @GetMapping(value = "/getsubscription/{email}")
  public CustomerSubscribedDTO getCustomerSubscribed(@PathVariable String email) throws Exception {
    return customerService.getCustomerSubscription(email);
  }

  @PostMapping(value = "/isSameBrowserAndDevice")
  public ResponseEntity<Boolean> isSameBrowser(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String deviceId = jsonNode.get("deviceId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(
        activeSessionService.isSameBrowserAndDevice(userId, deviceId, userAgent));
  }

  @PostMapping(value = "/isSameDevice")
  public ResponseEntity<Boolean> isSameDevice(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String mobileId = jsonNode.get("mobileId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(activeSessionService.isSameMobile(userId, mobileId, userAgent));
  }

  @PostMapping(value = "/addLoggedIn")
  public void addLoggedIn(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String deviceId = jsonNode.get("deviceId").asText();
      String userAgent = jsonNode.get("userAgent").asText();

      activeSessionService.createOrUpdateSession(userId, null, userAgent, deviceId);
    }
  }

  @PostMapping(value = "/addLoggedInMobile")
  public void addLoggedInMobile(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String mobileId = jsonNode.get("mobileId").asText();
      String userAgent = jsonNode.get("userAgent").asText();

      activeSessionService.createOrUpdateSessionMobile(userId, null, userAgent, mobileId);
    }
  }

  @DeleteMapping(value = "/removeSession/{userId}")
  public void removeSession(@PathVariable String userId) throws Exception {

    activeSessionService.removeSession(userId);
  }

  @PostMapping(value = "/addCompanyInformation")
  public ResponseEntity<CompanyInformation> addCompanyInformation(
      @RequestBody CompanyInformation companyInformation) throws Exception {
    customerService.addCompanyInformation(companyInformation);

    return ResponseEntity.ok(companyInformation);
  }

  @PostMapping(value = "/updateCompanyInformation")
  public ResponseEntity<CompanyInformation> updateCompanyInformation(
      @RequestBody CompanyInformation companyInformation) throws Exception {
    log.info("Updating company information for company info: {}", companyInformation);
    customerService.addCompanyInformation(companyInformation);

    return ResponseEntity.ok(companyInformation);
  }

  @GetMapping(value = "/getCompanyInformation/{companyId}")
  public ResponseEntity<CompanyInformation> getCompanyInformation(@PathVariable Long companyId)
      throws Exception {
    return ResponseEntity.ok(customerService.getcompanyInformation(companyId));
  }

  @GetMapping(value = "/getCompanyId/{email}")
  public ResponseEntity<CompanyIdDTO> getCompanyId(@PathVariable String email) throws Exception {
    return ResponseEntity.ok(customerService.getCompanyId(email));
  }

  @PostMapping(value = "/addUser")
  public ResponseEntity<BaseResponseDTO> addUser(@RequestBody CustomerDTO customerDTO)
      throws Exception {

    return ResponseEntity.ok(customerService.addUsers(customerDTO));
  }

  @GetMapping(value = "/getRegisteredUsers/{companyId}")
  public ResponseEntity<List<String>> getRegisteredUsers(@PathVariable Long companyId)
      throws Exception {
    return ResponseEntity.ok(customerService.activeUsers(companyId));
  }

  @GetMapping(value = "/accountInfo/{customerEmail}")
  public ResponseEntity<AccountLockInfoDTO> getAccountInfo(@PathVariable String customerEmail)
      throws Exception {
    return ResponseEntity.ok(customerService.getAccountInfo(customerEmail));
  }

  @PostMapping(value = "/accountInfo/update")
  public void addUser(@RequestBody AccountLockInfoDTO AccountLockInfoDTO) throws Exception {

    customerService.updateAccountInfo(AccountLockInfoDTO);
  }

  @DeleteMapping(value = "/deleteAccount/{companyId}/{email}")
  public void deleteUser(@PathVariable Long companyId, @PathVariable String email)
      throws Exception {

    customerService.deleteUser(companyId, email);
  }

  @PostMapping(value = "/roleAndPermission/add")
  public void addRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {

    customerService.addRoleAndPermission(customRoleDTO);
  }

  @PutMapping(value = "/roleAndPermission/update")
  public void updateRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {

    customerService.addRoleAndPermission(customRoleDTO);
  }

  @GetMapping(value = "/roleAndPermission/get/{companyId}/{name}")
  public ResponseEntity<CustomRoleDTO> getRoleAndPermissionByName(
      @PathVariable Long companyId, @PathVariable String name) throws Exception {

    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  @GetMapping(value = "/roleAndPermission/get/{companyId}")
  public ResponseEntity<List<CustomRoleDTO>> getRoleAndPermission(@PathVariable Long companyId)
      throws Exception {

    return ResponseEntity.ok(customerService.getRoleAndPermission(companyId));
  }

  @DeleteMapping(value = "/roleAndPermission/{id}")
  public void deleteRoleAndPermission(@PathVariable String id) throws Exception {

    customerService.deleteRoleAndPermission(id);
  }

  @GetMapping(value = "/countByRole/{companyId}/{roleName}")
  public ResponseEntity<CountNameByRole> countByRole(
      @PathVariable Long companyId, @PathVariable String roleName) throws Exception {
//    Long count = customerService.countByRoleName(roleName, companyId);
//    System.out.println("count->" + count + " " + roleName);
    return ResponseEntity.ok(customerService.countByRoleName(roleName, companyId));
  }

  @GetMapping(value = "/roleAndPermissionByName/get/{companyId}/{name}")
  public ResponseEntity<CustomRoleDTO> roleAndPermissionByName(
      @PathVariable Long companyId, @PathVariable String name) throws Exception {

    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  @PostMapping(value = "/addlocation")
  public ResponseEntity<Location> addLocation(@RequestBody Location location) throws LocationAlreadyPresentException {
    return ResponseEntity.ok(customerService.addLocation(location));
  }

  @PutMapping(value = "/addlocation")
  public ResponseEntity<Location> updateLocation(@RequestBody Location location)  {
    return ResponseEntity.ok(customerService.updateLocation(location));
  }
  

  @GetMapping(value = "/getAllLocation/{companyId}")
  public ResponseEntity<List<Location>> getAllLocation(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllLocation(companyId));
  }

  @DeleteMapping(value = "/deleteLocation/{id}")
  public void deleteLocation(@PathVariable String id) throws LocationDeletionException {
    customerService.deleteLocation(id);
  }

  // -------------------------------------------------------
  @PostMapping(value = "/addbin")
  public ResponseEntity<Bin> addBin(@RequestBody BinDTO bindDto) throws BinAlreadyPresentException {
    System.out.println(bindDto.toString());
    return ResponseEntity.ok(customerService.addBin(bindDto));
  }
  @PutMapping(value = "/addbin")
  public ResponseEntity<Bin> updateBin(@RequestBody BinDTO bindDto) {
    System.out.println(bindDto.toString());
    return ResponseEntity.ok(customerService.updateBin(bindDto));
  }

  @GetMapping(value = "/getAllBin/{companyId}")
  public ResponseEntity<List<BinDTO>> getAllBin(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllBin(companyId));
  }

  @GetMapping("/locations-with-bins/{companyId}")
  public ResponseEntity<List<LocationWithBinsDTO>> getLocationsWithBins(
      @PathVariable Long companyId) {
    // Fetch all locations, and for each, fetch bins, map into DTO
    return ResponseEntity.ok(customerService.getLocationsWithBins(companyId));
  }

  @DeleteMapping(value = "/deleteBin/{id}")
  public void deleteBin(@PathVariable String id) throws LocationDeletionException {
    customerService.deleteBin(id);
  }

  @PostMapping(value = "/addImportHistory")
  public void addImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.addImportHistory(importHistory);
  }

  @PostMapping(value = "/getAllImportHistory/{companyId}")
  public Page<ImportHistoryDTO> getImportHistory(
      @PathVariable Long companyId,
      @RequestParam(defaultValue = "0", required = false) int pageNumber,
      @RequestParam(defaultValue = "10", required = false) int pageSize,
      @RequestBody(required = false) JsonNode filter) {

    //		try {
    //			Thread.sleep(5000);
    //		} catch (InterruptedException e) {
    //			e.printStackTrace(); // Handle the exception if needed
    //		}
   log.info("Filter received for import history: {}", filter);

    if(filter!=null&&filter.has("startDate") && filter.has("endDate")) {
      return customerService.getImportHistoryListWithDateFilter(companyId, pageNumber, pageSize,
              filter.get("startDate").asText(), filter.get("endDate").asText());
    }

    return customerService.getImportHistoryList(companyId, pageNumber, pageSize);
  }


  @PutMapping(value = "/updateImportHistory")
  public void updateImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.updateImportHistory(importHistory);
  }

  @PostMapping(value = "/sentResetOTP")
  public void sentResetOTP(@RequestBody String email) throws NoEmailFoundException {
    customerService.sentResetOTP(email);
  }

  @PostMapping(value = "/updatePassword")
  public void updatePassword(@RequestBody JsonNode obj)
      throws OTPException, FirebaseAuthException, NoEmailFoundException {
    String email = obj.get("email").asText();
    String otp = obj.get("otp").asText();
    String password = obj.get("password").asText();
    customerService.updatePassword(email, otp, password);
  }

  @PostMapping(value = "/addCardDetails")
  public void addCardDetails(@RequestBody CustomerStripeDetails customerStripeDetails) {

    customerService.addCardDetails(customerStripeDetails);
  }

  @GetMapping(value = "/getCardDetails/{companyId}")
  public CustomerStripeDetails getCardDetails(@PathVariable Long companyId) {

    return customerService.getCardDetails(companyId);
  }

  @DeleteMapping(value = "/deleteCardDetails/{id}")
  public void deleteCardDetails(@PathVariable String id) {

    customerService.deleteCardDetails(id);
  }

  @GetMapping(value = "/trial-status/{email}")
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
  public ResponseEntity<Map<String, String>> activateSubscription(@PathVariable String email) {
    trialService.activatePaidSubscription(email);
    
    Map<String, String> response = new HashMap<>();
    response.put("message", "Subscription activated successfully");
    response.put("status", "success");
    
    return ResponseEntity.ok(response);
  }
}
