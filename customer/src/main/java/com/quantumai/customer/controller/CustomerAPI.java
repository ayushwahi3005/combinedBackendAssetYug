package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.ImportHistoryRepository;
import com.quantumai.customer.repository.BinRepository;
import com.quantumai.customer.repository.CustomRoleRepository;
import com.quantumai.customer.repository.LocationRepository;
import com.quantumai.customer.service.ActiveSessionService;
import com.quantumai.customer.service.AuditService;
import com.quantumai.customer.service.CustomerService;
import com.quantumai.customer.service.TrialService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
@Tag(name = "Customer", description = "Customer Management API")
public class CustomerAPI {

  @Autowired private CustomerService customerService;
  @Autowired private ActiveSessionService activeSessionService;
  @Autowired private TrialService trialService;
  @Autowired private AuditService auditService;
  @Autowired private CustomRoleRepository customRoleRepository;
  @Autowired private LocationRepository locationRepository;
  @Autowired private BinRepository binRepository;
  @Autowired private ImportHistoryRepository importHistoryRepository;

  // ─── Public endpoints (no auth needed) ───────────────────────────────────
  // These are called before login so cannot require authentication

  @Operation(summary = "Working", description = "Endpoint to working")
  @GetMapping(value = "/working")
  public ResponseEntity<String> working() throws Exception {
    return ResponseEntity.ok("Working OK");
  }

  @Operation(summary = "Check User Name", description = "Endpoint to check user name")
  @GetMapping(value = "/checkUserName/{email}")

  public ResponseEntity<Boolean> checkUserName(@PathVariable String email) throws Exception {
    return ResponseEntity.ok(customerService.checkCustomer(email));
  }

  @Operation(summary = "Add Customer", description = "Endpoint to add customer")
  @PostMapping(value = "/addCustomer")
  public ResponseEntity<BaseResponseDTO> addCustomer(@RequestBody CustomerDTO customerDTO)
          throws Exception {
    return ResponseEntity.ok(customerService.addCustomer(customerDTO));
  }

  @Operation(summary = "Authenticate", description = "Endpoint to authenticate")
  @PostMapping(value = "/authenticate/{deviceId}")
  public ResponseEntity<AuthenticationResponseDTO> authenticate(
          @RequestBody AuthenticationRequestDTO authenticationRequestDTO,
          @PathVariable String deviceId) throws Exception {
    return ResponseEntity.ok(customerService.authenticate(authenticationRequestDTO, deviceId));
  }

  @Operation(summary = "Get Login Token", description = "Endpoint to get login token")
  @PostMapping(value = "/getLoginToken")
  public ResponseEntity<AuthenticationResponseDTO> getLoginToken(@RequestBody JsonNode jsonNode)
          throws Exception {
    String email = jsonNode.get("email").asText();
    String password = jsonNode.get("password").asText();
    String deviceId = jsonNode.get("deviceId").asText();
    return ResponseEntity.ok(customerService.getLoginToken(email, password, deviceId));
  }

  @Operation(summary = "Sent Reset Otp", description = "Endpoint to sent reset otp")
  @PostMapping(value = "/sentResetOTP")
  public void sentResetOTP(@RequestBody String email) throws NoEmailFoundException {
    customerService.sentResetOTP(email);
  }

  @Operation(summary = "Update Password", description = "Endpoint to update password")
  @PostMapping(value = "/updatePassword/{email}")

  public void updatePassword(@RequestBody JsonNode obj,@PathVariable String email)

          throws OTPException, FirebaseAuthException, NoEmailFoundException {

    String otp = obj.get("otp").asText();
    String password = obj.get("password").asText();
    customerService.updatePassword(email, otp, password);
  }

  // ─── Authenticated endpoints (same person check only) ────────────────────

  @Operation(summary = "Get Customer", description = "Endpoint to get customer")
  @GetMapping(value = "/get/{email}")

  public CustomerDTO getCustomer(@PathVariable String email) throws Exception {
    return customerService.getCustomer(email);
  }

  @Operation(summary = "Get Customer Subscribed", description = "Endpoint to get customer subscribed")
  @GetMapping(value = "/getsubscription/{email}")

  public CustomerSubscribedDTO getCustomerSubscribed(@PathVariable String email) throws Exception {
    return customerService.getCustomerSubscription(email);
  }

  @Operation(summary = "Is Same Browser", description = "Endpoint to is same browser")
  @PostMapping(value = "/isSameBrowserAndDevice")

  public ResponseEntity<Boolean> isSameBrowser(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String deviceId = jsonNode.get("deviceId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(activeSessionService.isSameBrowserAndDevice(userId, deviceId, userAgent));
  }

  @Operation(summary = "Is Same Device", description = "Endpoint to is same device")
  @PostMapping(value = "/isSameDevice")

  public ResponseEntity<Boolean> isSameDevice(@RequestBody JsonNode jsonNode) throws Exception {
    String userId = jsonNode.get("userId").asText();
    String mobileId = jsonNode.get("mobileId").asText();
    String userAgent = jsonNode.get("userAgent").asText();
    return ResponseEntity.ok(activeSessionService.isSameMobile(userId, mobileId, userAgent));
  }

  @Operation(summary = "Add Logged In", description = "Endpoint to add logged in")
  @PostMapping(value = "/addLoggedIn")

  public void addLoggedIn(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String deviceId = jsonNode.get("deviceId").asText();
      String userAgent = jsonNode.get("userAgent").asText();
      activeSessionService.createOrUpdateSession(userId, null, userAgent, deviceId);
    }
  }

  @Operation(summary = "Add Logged In Mobile", description = "Endpoint to add logged in mobile")
  @PostMapping(value = "/addLoggedInMobile")

  public void addLoggedInMobile(@RequestBody JsonNode jsonNode) throws Exception {
    if (jsonNode != null) {
      String userId = jsonNode.get("userId").asText();
      String mobileId = jsonNode.get("mobileId").asText();
      String userAgent = jsonNode.get("userAgent").asText();
      activeSessionService.createOrUpdateSessionMobile(userId, null, userAgent, mobileId);
    }
  }

  @Operation(summary = "Remove Session", description = "Endpoint to remove session")
  @DeleteMapping(value = "/removeSession/{userId}")

  public void removeSession(@PathVariable String userId) throws Exception {
    activeSessionService.removeSession(userId);
  }

  // ─── Company-scoped endpoints (same company check) ────────────────────────

  @Operation(summary = "Add Company Information", description = "Endpoint to add company information")
  @PostMapping(value = "/addCompanyInformation")

  public ResponseEntity<CompanyInformation> addCompanyInformation(
          @RequestBody CompanyInformation companyInformation) throws Exception {
    customerService.addCompanyInformation(companyInformation);
    if (companyInformation.getId() != null) {
      auditService.logCreate(AuditModule.COMPANY, String.valueOf(companyInformation.getId()),
              companyInformation.getCompanyName(), companyInformation.getId(),
              Map.of("companyName", companyInformation.getCompanyName() != null
                      ? companyInformation.getCompanyName() : ""));
    }
    return ResponseEntity.ok(companyInformation);
  }

  @Operation(summary = "Update Company Information", description = "Endpoint to update company information")
  @PostMapping(value = "/updateCompanyInformation")

  public ResponseEntity<CompanyInformation> updateCompanyInformation(
          @RequestBody CompanyInformation companyInformation) throws Exception {
    log.info("Updating company information: {}", companyInformation);
    CompanyInformation beforeState = null;
    if (companyInformation.getId() != null) {
      beforeState = customerService.getcompanyInformation(companyInformation.getId());
    }
    customerService.addCompanyInformation(companyInformation);
    CompanyInformation afterState = customerService.getcompanyInformation(companyInformation.getId());
    if (beforeState != null && afterState != null) {
      auditService.logUpdateWithComparison(AuditModule.COMPANY,
              String.valueOf(afterState.getId()), afterState.getCompanyName(),
              afterState.getId(), beforeState, afterState);
    }
    return ResponseEntity.ok(companyInformation);
  }

  @Operation(summary = "Get Company Information", description = "Endpoint to get company information")
  @GetMapping(value = "/getCompanyInformation/{companyId}")

  public ResponseEntity<CompanyInformation> getCompanyInformation(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.getcompanyInformation(companyId));
  }

  @Operation(summary = "Get Company Id", description = "Endpoint to get company id")
  @GetMapping(value = "/getCompanyId/{email}")

  public ResponseEntity<CompanyIdDTO> getCompanyId(@PathVariable String email) throws Exception {
    return ResponseEntity.ok(customerService.getCompanyId(email));
  }

  @Operation(summary = "Add User", description = "Endpoint to add user")
  @PostMapping(value = "/addUser")

  public ResponseEntity<BaseResponseDTO> addUser(@RequestBody CustomerDTO customerDTO)
          throws Exception {
    return ResponseEntity.ok(customerService.addUsers(customerDTO));
  }

  @Operation(summary = "Get Registered Users", description = "Endpoint to get registered users")
  @GetMapping(value = "/getRegisteredUsers/{companyId}")

  public ResponseEntity<List<String>> getRegisteredUsers(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.activeUsers(companyId));
  }

  @Operation(summary = "Get Account Info", description = "Endpoint to get account info")
  @GetMapping(value = "/accountInfo/{customerEmail}")

  public ResponseEntity<AccountLockInfoDTO> getAccountInfo(@PathVariable String customerEmail)
          throws Exception {
    return ResponseEntity.ok(customerService.getAccountInfo(customerEmail));
  }

  @Operation(summary = "Update Account Info", description = "Endpoint to update account info")
  @PostMapping(value = "/accountInfo/update")

  public void updateAccountInfo(@RequestBody AccountLockInfoDTO accountLockInfoDTO) throws Exception {
    customerService.updateAccountInfo(accountLockInfoDTO);
  }

  @Operation(summary = "Delete User", description = "Endpoint to delete user")
  @DeleteMapping(value = "/deleteAccount/{companyId}/{email}")

  public void deleteUser(@PathVariable Long companyId, @PathVariable String email)
          throws Exception {
    customerService.deleteUser(companyId, email);
  }

  // ─── Role & Permission ────────────────────────────────────────────────────

  @Operation(summary = "Add Role And Permission", description = "Endpoint to add role and permission")
  @PostMapping(value = "/roleAndPermission/add")
  public void addRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {
    customerService.addRoleAndPermission(customRoleDTO);
    auditService.logCreate(AuditModule.ROLE, String.valueOf(customRoleDTO.getCustomRoleId()),
            customRoleDTO.getName(), customRoleDTO.getCompanyId(),
            Map.of("name", String.valueOf(customRoleDTO.getName()),
                    "type", String.valueOf(customRoleDTO.getType())));
  }

  @Operation(summary = "Update Role And Permission", description = "Endpoint to update role and permission")
  @PutMapping(value = "/roleAndPermission/update")
  public void updateRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {
    CustomRole before = customRoleRepository.findById(customRoleDTO.getId()).orElse(null);
    customerService.addRoleAndPermission(customRoleDTO);
    if (before != null) {
      CustomRole after = customRoleRepository.findById(customRoleDTO.getId()).orElse(null);
      if (after != null) {
        auditService.logUpdateWithComparison(AuditModule.ROLE,
                String.valueOf(after.getCustomRoleId()), after.getName(),
                after.getCompanyId(), before, after);
      }
    }
  }

  @Operation(summary = "Get Role And Permission By Name", description = "Endpoint to get role and permission by name")
  @GetMapping(value = "/roleAndPermission/get/{companyId}/{name}")

  public ResponseEntity<CustomRoleDTO> getRoleAndPermissionByName(
          @PathVariable Long companyId, @PathVariable String name) throws Exception {
    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  @Operation(summary = "Get Role And Permission", description = "Endpoint to get role and permission")
  @GetMapping(value = "/roleAndPermission/get/{companyId}")

  public ResponseEntity<List<CustomRoleDTO>> getRoleAndPermission(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(customerService.getRoleAndPermission(companyId));
  }

  @Operation(summary = "Delete Role And Permission", description = "Endpoint to delete role and permission")
  @DeleteMapping(value = "/roleAndPermission/{id}")
  public void deleteRoleAndPermission(@PathVariable String id) throws Exception {
    customRoleRepository.findById(id).ifPresent(role ->
            auditService.logDelete(AuditModule.ROLE, String.valueOf(role.getCustomRoleId()),
                    role.getName(), role.getCompanyId(),
                    Map.of("name", String.valueOf(role.getName()),
                            "type", String.valueOf(role.getType()))));
    customerService.deleteRoleAndPermission(id);
  }

  @Operation(summary = "Count By Role", description = "Endpoint to count by role")
  @GetMapping(value = "/countByRole/{companyId}/{roleName}")

  public ResponseEntity<CountNameByRole> countByRole(
          @PathVariable Long companyId, @PathVariable String roleName) throws Exception {
    return ResponseEntity.ok(customerService.countByRoleName(roleName, companyId));
  }

  @Operation(summary = "Role And Permission By Name", description = "Endpoint to role and permission by name")
  @GetMapping(value = "/roleAndPermissionByName/get/{companyId}/{name}")

  public ResponseEntity<CustomRoleDTO> roleAndPermissionByName(
          @PathVariable Long companyId, @PathVariable String name) throws Exception {
    return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId, name));
  }

  // ─── Location & Bin ───────────────────────────────────────────────────────

  @Operation(summary = "Add Location", description = "Endpoint to add location")
  @PostMapping(value = "/addlocation")
  public ResponseEntity<Location> addLocation(@RequestBody Location location)
          throws LocationAlreadyPresentException {
    Location saved = customerService.addLocation(location);
    auditService.logCreate(AuditModule.LOCATION, String.valueOf(saved.getLocationId()),
            saved.getName(), saved.getCompanyId(),
            Map.of("locationId", String.valueOf(saved.getLocationId()),
                    "name", String.valueOf(saved.getName()),
                    "city", String.valueOf(saved.getCity()),
                    "address", String.valueOf(saved.getAddress())));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Update Location", description = "Endpoint to update location")
  @PutMapping(value = "/addlocation")
  public ResponseEntity<Location> updateLocation(@RequestBody Location location) {
    Location before = locationRepository.findById(location.getId()).orElse(null);
    Location saved = customerService.updateLocation(location);
    if (before != null) {
      auditService.logUpdateWithComparison(AuditModule.LOCATION,
              String.valueOf(saved.getLocationId()), saved.getName(),
              saved.getCompanyId(), before, saved);
    }
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Get All Location", description = "Endpoint to get all location")
  @GetMapping(value = "/getAllLocation/{companyId}")

  public ResponseEntity<List<Location>> getAllLocation(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllLocation(companyId));
  }
  @Operation(summary = "Get All Active Location", description = "Endpoint to get all active location")
  @GetMapping(value = "/getAllActiveLocation/{companyId}")

  public ResponseEntity<List<Location>> getAllActiveLocation(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllActiveLocation(companyId));
  }

  @Operation(summary = "Delete Location", description = "Endpoint to delete location")
  @DeleteMapping(value = "/deleteLocation/{id}")
  public void deleteLocation(@PathVariable String id) throws LocationDeletionException {
    locationRepository.findById(id).ifPresent(loc ->
            auditService.logDelete(AuditModule.LOCATION, String.valueOf(loc.getLocationId()),
                    loc.getName(), loc.getCompanyId(),
                    Map.of("locationId", String.valueOf(loc.getLocationId()),
                            "name", String.valueOf(loc.getName()))));
    customerService.deleteLocation(id);
  }

  @Operation(summary = "Add Bin", description = "Endpoint to add bin")
  @PostMapping(value = "/addbin")
  public ResponseEntity<Bin> addBin(@RequestBody BinDTO bindDto) throws BinAlreadyPresentException {
    Bin saved = customerService.addBin(bindDto);
    auditService.logCreate(AuditModule.BIN, String.valueOf(saved.getBinId()),
            saved.getBinNumber(), saved.getCompanyId(),
            Map.of("binId", String.valueOf(saved.getBinId()),
                    "binNumber", String.valueOf(saved.getBinNumber())));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Update Bin", description = "Endpoint to update bin")
  @PutMapping(value = "/addbin")
  public ResponseEntity<Bin> updateBin(@RequestBody BinDTO bindDto) {
    Bin saved = customerService.updateBin(bindDto);
    auditService.logUpdate(AuditModule.BIN, String.valueOf(saved.getBinId()),
            saved.getBinNumber(), saved.getCompanyId(),
            Map.of("binId", String.valueOf(saved.getBinId()),
                    "binNumber", String.valueOf(saved.getBinNumber()),
                    "status", String.valueOf(saved.getStatus())));
    return ResponseEntity.ok(saved);
  }

  @Operation(summary = "Get All Bin", description = "Endpoint to get all bin")
  @GetMapping(value = "/getAllBin/{companyId}")

  public ResponseEntity<List<BinDTO>> getAllBin(@PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getAllBin(companyId));
  }

  @Operation(summary = "Get Locations With Bins", description = "Endpoint to get locations with bins")
  @GetMapping("/locations-with-bins/{companyId}")

  public ResponseEntity<List<LocationWithBinsDTO>> getLocationsWithBins(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(customerService.getLocationsWithBins(companyId));
  }

  @Operation(summary = "Delete Bin", description = "Endpoint to delete bin")
  @DeleteMapping(value = "/deleteBin/{id}")
  public void deleteBin(@PathVariable String id) throws LocationDeletionException {
    // Fetch before deletion for audit snapshot
    Bin bin = binRepository.findById(id).orElse(null);
    if (bin != null) {
      auditService.logDelete(AuditModule.BIN, String.valueOf(bin.getBinId()),
              bin.getBinNumber(), bin.getCompanyId(),
              Map.of("binId", String.valueOf(bin.getBinId()),
                      "binNumber", String.valueOf(bin.getBinNumber())));
    }
    customerService.deleteBin(id);
  }

  // ─── Import History ───────────────────────────────────────────────────────

  @Operation(summary = "Add Import History", description = "Endpoint to add import history")
  @PostMapping(value = "/addImportHistory")

  public void addImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.addImportHistory(importHistory);
  }

  @Operation(summary = "Get Import History", description = "Endpoint to get import history")
  @PostMapping(value = "/getAllImportHistory/{companyId}")

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

  @Operation(summary = "Update Import History", description = "Endpoint to update import history")
  @PutMapping(value = "/updateImportHistory")

  public void updateImportHistory(@RequestBody ImportHistory importHistory) {
    customerService.updateImportHistory(importHistory);
  }

  @Operation(summary = "Download Import Error Report", description = "Download failed import rows with error messages")
  @GetMapping("/downloadImportErrorReport/{companyId}/{importHistoryId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'imports')")
  public ResponseEntity<byte[]> downloadImportErrorReport(
          @PathVariable Long companyId,
          @PathVariable String importHistoryId) {
    return importHistoryRepository.findById(importHistoryId)
            .filter(history -> companyId.equals(history.getCompanyId()))
            .filter(history -> history.getErrorReportFile() != null && history.getErrorReportFile().length > 0)
            .map(history -> {
              String fileName = history.getErrorReportFileName() != null
                      ? history.getErrorReportFileName()
                      : "ImportErrors.xlsx";
              return ResponseEntity.ok()
                      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                      .contentType(MediaType.APPLICATION_OCTET_STREAM)
                      .body(history.getErrorReportFile());
            })
            .orElse(ResponseEntity.notFound().build());
  }

  // ─── Card & Stripe ────────────────────────────────────────────────────────

  @Operation(summary = "Add Card Details", description = "Endpoint to add card details")
  @PostMapping(value = "/addCardDetails")

  public void addCardDetails(@RequestBody CustomerStripeDetails customerStripeDetails) {
    customerService.addCardDetails(customerStripeDetails);
  }

  @Operation(summary = "Get Card Details", description = "Endpoint to get card details")
  @GetMapping(value = "/getCardDetails/{companyId}")

  public CustomerStripeDetails getCardDetails(@PathVariable Long companyId) {
    return customerService.getCardDetails(companyId);
  }

  @Operation(summary = "Delete Card Details", description = "Endpoint to delete card details")
  @DeleteMapping(value = "/deleteCardDetails/{id}")

  public void deleteCardDetails(@PathVariable String id) {
    customerService.deleteCardDetails(id);
  }

  // ─── Trial ────────────────────────────────────────────────────────────────

  @Operation(summary = "Get Trial Status Details", description = "Endpoint to get trial status details")
  @GetMapping(value = "/trial-status-details/{companyId}")

  public ResponseEntity<TrialStatus> getTrialStatusDetails(@PathVariable Long companyId)
          throws Exception {
    return ResponseEntity.ok(trialService.getTrialDetails(companyId));
  }

  @Operation(summary = "Get Trial Status", description = "Endpoint to get trial status")
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

  @Operation(summary = "Activate Subscription", description = "Endpoint to activate subscription")
  @PostMapping(value = "/activate-subscription/{email}")

  public ResponseEntity<Map<String, String>> activateSubscription(@PathVariable String email) {
    trialService.activatePaidSubscription(email);
    Map<String, String> response = new HashMap<>();
    response.put("message", "Subscription activated successfully");
    response.put("status", "success");
    return ResponseEntity.ok(response);
  }

  /**
   * Check if a user is eligible for a free trial.
   * The response contains "eligible" = true/false. If blacklisted, eligible is set to false.
   */
  @Operation(summary = "Is Trial Eligible", description = "Endpoint to is trial eligible")
  @GetMapping(value = "/trial-eligible/{email}")
  public ResponseEntity<Map<String, Object>> isTrialEligible(@PathVariable String email) {
    Map<String, Object> response = new HashMap<>();
    boolean eligible = trialService.isEligibleForTrial(email);
    response.put("eligible", eligible);
    if (!eligible) {
      response.put("message", "You have previously used a free trial. Please subscribe to a plan to use the application.");
    }
    return ResponseEntity.ok(response);
  }
}