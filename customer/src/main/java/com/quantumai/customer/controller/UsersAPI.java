package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.service.AuditService;
import com.quantumai.customer.repository.CustomRoleRepository;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.security.JwtService;
import com.quantumai.customer.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/users")
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@Tag(name = "Users", description = "Users Management API")
public class UsersAPI {
  private RestTemplate restTemplate = new RestTemplate();

  //	String customerApi="http://localhost:8080/customer/get/";

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;

  private ModelMapper modelMapper = new ModelMapper();

  @Autowired private UserService userService;

  //	@Autowired
  //	 private PasswordEncoder passwordEncoder;

  @Autowired private UsersRepository usersRepository;
  @Autowired private CustomerRepository customerRepository;

  @Autowired private CustomRoleRepository customRoleRepository;

  @Autowired private JwtService jwtService;

  @Autowired SubscriptionRepository subscriptionRepository;
  @Autowired private AuditService auditService;


  private void checkUserDetailsPermissionFromSpringContext(CustomRoleType customRoleType) throws UserAccessException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("Spring Security"+ authentication.getName());
    Optional<Users> usersOptional=usersRepository.findByEmail(authentication.getName());
    if(usersOptional.isPresent()){
      System.out.println(customRoleType.ordinal()+" "+usersOptional.get().getRole().getUsers().ordinal());
      if(customRoleType.ordinal()>usersOptional.get().getRole().getUsers().ordinal()){
        GenricErrorMessage genricErrorMessage=new GenricErrorMessage("User Dont Have access", HttpStatus.FORBIDDEN);
        throw new UserAccessException(genricErrorMessage.getMessage());
      }
    }
    else{
      GenricErrorMessage genricErrorMessage=new GenricErrorMessage("User Dont Have access", HttpStatus.FORBIDDEN);
      throw new UserAccessException(genricErrorMessage.getMessage());
    }




  }

  @Operation(summary = "Register", description = "Endpoint to register")
  @PostMapping("/registerUser")
  public Users register(@RequestBody Users users, @RequestHeader String Authorization)
          throws UserException, NoSubscriptionError, UserAccessException {
    //      users.setStatus(StatusEnum.active);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    Users registered = userService.registerUser(users);
    auditService.logCreate(AuditModule.USER,
            String.valueOf(registered.getUserId()),
            registered.getFirstName() + " " + registered.getLastName(),
            registered.getCompanyId(),
            Map.of("userId", String.valueOf(registered.getUserId()),
                    "email", String.valueOf(registered.getEmail()),
                    "companyId", String.valueOf(registered.getCompanyId())));
    return registered;
  }

  @Operation(summary = "Send Email", description = "Endpoint to send email")
  @PostMapping("/send/{companyId}")
  public void sendEmail(@PathVariable Long companyId, @RequestBody Mail mail)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    Optional<Subscription> subscriptionOptional =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    if (subscriptionOptional.isEmpty()) {
      throw new NoSubscriptionError("No Active Subscription");
    }

    AuthenticationResponseDTO response = userService.generateToken(mail);
    System.out.print(
        "---------------------///////--------------------////-----------------"
            + response.getToken());
    //    String emailId = "http://localhost:4200/invitation/";
    String emailId = "http://assetyugg.com.s3-website-us-east-1.amazonaws.com/invitation/";
    userService.sendSimpleMessage(
        mail.getEmail(),
        "Invitation mail: ",
        mail.getMessage() + emailId + companyId + "/" + response.getToken());
  }

  //	@GetMapping(value="/getCustomer/{companyId}")
  //	public ResponseEntity<Customer> getCustomerDetails(@PathVariable Long companyId,
  // @RequestHeader("Authorization") String token){
  //
  //		HttpHeaders headers = new HttpHeaders();
  //        headers.set("Authorization", token);
  //        HttpEntity<Customer> entity = new HttpEntity<>(headers);
  //
  //		ResponseEntity<CustomerDTO> response = restTemplate.exchange(
  //				customerApi+companyId,
  //                HttpMethod.GET,
  //                entity,
  //                CustomerDTO.class
  //        );
  //		Customer customer=modelMapper.map(response.getBody(), Customer.class);
  //        return ResponseEntity.ok(customer);
  //
  //	}
  @Operation(summary = "Get Invite Details", description = "Endpoint to get invite details")
  @PostMapping(value = "/invite")
  public void getInviteDetails(@RequestBody UsersDTO userDTO, @RequestHeader String Authorization)
      throws UserException, NoSubscriptionError {
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    System.out.println(userDTO);
    Users user = modelMapper.map(userDTO, Users.class);
    //		user.setPassword(passwordEncoder.encode(user.getPassword()));
    //		user.setCompanyId(companyId);
    //		if(myDetails.get("role").equals("ADMIN")) {
    //			user.setRole(Role.ADMIN);
    //		}s
    //		else if(myDetails.get("role").equals("TECHNICAL")) {
    //			user.setRole(Role.TECHNICAL);
    //		}
    user.setStatus(UserStatusEnum.inActive);
    userService.registerUser(user);
  }

  @Operation(summary = "Get All Users", description = "Endpoint to get all users")
  @GetMapping(value = "/getUsers/{companyId}")
  public ResponseEntity<List<UsersDTO>> getAllUsers(@PathVariable Long companyId) {
    List<UsersDTO> userList = userService.getAllUsers(companyId);
    return ResponseEntity.ok(userList);
  }

  @Operation(summary = "Get Users", description = "Endpoint to get users")
  @GetMapping(value = "/invite/getUser/{companyId}/{details}")
  public ResponseEntity<UsersDTO> getUsers(
      @PathVariable Long companyId, @PathVariable String details) throws UserException {
    Claims myDetails = userService.decodeDetails(details);
    UsersDTO user = userService.getUsers(companyId, myDetails.get("email").toString());
    return ResponseEntity.ok(user);
  }

  @Operation(summary = "Get Technical User", description = "Endpoint to get technical user")
  @GetMapping(value = "/getTechnicalUser/{companyId}")
  public ResponseEntity<List<UsersDTO>> getTechnicalUser(@PathVariable Long companyId) {

    List<UsersDTO> userDTOList = userService.getAllUsersByRole("TECHNICAL", companyId);
    return ResponseEntity.ok(userDTOList);
  }

  @Operation(summary = "Resend Firebase Verification Email", description = "Endpoint to resend firebase verification email")
  @PostMapping(value = "/resend-email-firebase-verification/{companyId}/{email}")
  public ResponseEntity<Map<String, String>> resendFirebaseVerificationEmail(
      @PathVariable Long companyId,
      @PathVariable String email) throws UserException, TheMailException, FirebaseAuthException, UserEmailAlreadyVerifiedException, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    userService.resendFirebaseVerificationEmail(email, companyId);
    
    return ResponseEntity.ok(Map.of(
        "status", "success",
        "message", "Firebase verification email has been resent successfully"
    ));
  }

  @Operation(summary = "Get User Details", description = "Endpoint to get user details")
  @GetMapping(value = "/getUserDetails/{companyId}/{email}")
  public ResponseEntity<UsersDTO> getUserDetails(
      @PathVariable Long companyId, @PathVariable String email) throws UserException {

    UsersDTO UserDTO = userService.getUsers(companyId, email);
    return ResponseEntity.ok(UserDTO);
  }

  @Operation(summary = "Update User Details", description = "Endpoint to update user details")
  @PutMapping(value = "/userDetails")
  public ResponseEntity<String> updateUserDetails(
      @RequestBody UsersDTO usersDTO, @RequestHeader String Authorization)
          throws NoSubscriptionError, UserException, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }

    // Fetch current state before update
    Optional<Users> beforeStateOpt = usersRepository.findById(usersDTO.getId());
    
    userService.updateUser(usersDTO);
    
    if (beforeStateOpt.isPresent()) {
      // Fetch updated state for comparison
      Users afterState = usersRepository.findById(usersDTO.getId()).orElse(null);
      if (afterState != null) {
        auditService.logUpdateWithComparison(AuditModule.USER,
                String.valueOf(usersDTO.getUserId()),
                usersDTO.getFirstName() + " " + usersDTO.getLastName(),
                usersDTO.getCompanyId(), beforeStateOpt.get(), afterState);
      }
    }
    return ResponseEntity.ok("Status Updated Successfully");
  }
  @Operation(summary = "User Status Update", description = "Endpoint to user status update")
  @PutMapping(value = "/userStatusUpdate")
  public ResponseEntity<String> userStatusUpdate(
          @RequestBody UsersDTO usersDTO, @RequestHeader String Authorization)
          throws NoSubscriptionError, UserException, UserCannotActivateException, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
              subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.updateUserStatus(usersDTO);
    
    // Fetch updated state for audit comparison
    Optional<Users> afterStateOpt = usersRepository.findById(usersDTO.getId());
    if (afterStateOpt.isPresent()) {
      // For status update, capture the before state if available via email lookup
      Optional<Users> beforeStateOpt = usersRepository.findByEmail(usersDTO.getEmail());
      if (beforeStateOpt.isPresent()) {
        auditService.logUpdateWithComparison(AuditModule.USER,
                String.valueOf(usersDTO.getUserId()),
                usersDTO.getEmail(), usersDTO.getCompanyId(),
                beforeStateOpt.get(), afterStateOpt.get());
      }
    }
    return ResponseEntity.ok("Status Updated Successfully");
  }

  //    @Transactional
  @Operation(summary = "Update User Details", description = "Endpoint to update user details")
  @PostMapping(value = "/updateUserDetails")
  public void updateUserDetails(@RequestBody UsersDTO usersDTO, HttpServletRequest request)
      throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    String authorizationHeader = request.getHeader("Authorization");
    String jwt = authorizationHeader.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new RuntimeException("Invalid or missing Authorization header");
    }

    // Fetch before state for audit
    Optional<Users> beforeStateOpt = usersRepository.findByEmail(usersDTO.getEmail());

    // Extract token (remove "Bearer " prefix)
    String token = authorizationHeader.substring(7);

    // Decode token to extract claims
    Claims claims =
        Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody();

    // Extract email from claims
    String email = claims.getSubject();

    // Log or use the email
    System.out.println("Extracted Email: " + email);
    Optional<Customer> myUsers = customerRepository.findByEmail(email);
    if (myUsers.isPresent()) {
      String role = myUsers.get().getRole();
      System.out.println("Role->" + role);
      if (role.toLowerCase().equals("admin")) {
        userService.updateUser(usersDTO);
      } else {
        Optional<CustomRole> optionalCustomRole =
            customRoleRepository.findByNameAndCompanyId(role, myUsers.get().getCompanyId());
        if (optionalCustomRole.isPresent()) {
          CustomRole customRole = optionalCustomRole.get();
          if (customRole.getUsers().equals(CustomRoleType.full)
              || customRole.getUsers().equals(CustomRoleType.edit)) {
            System.out.println(usersDTO);
            userService.updateUser(usersDTO);
          } else {
            throw new Exception("Not Authorized");
          }

        } else {
          throw new Exception("Unkown Role");
        }
      }
      Optional<Customer> customerOptional = customerRepository.findByEmail(usersDTO.getEmail());
      if (customerOptional.isPresent()) {
        if (usersDTO.getRole() != null) {
          customerOptional.get().setRole(usersDTO.getRole().getName());
        }
        customerOptional.get().setFirstName(usersDTO.getFirstName());
        customerOptional.get().setLastName(usersDTO.getLastName());
        customerOptional.get().setMobileNumber(usersDTO.getMobileNumber());
        customerRepository.save(customerOptional.get());
      }

      // Log audit after update
      if (beforeStateOpt.isPresent()) {
        Optional<Users> afterStateOpt = usersRepository.findByEmail(usersDTO.getEmail());
        if (afterStateOpt.isPresent()) {
          auditService.logUpdateWithComparison(AuditModule.USER,
                  String.valueOf(afterStateOpt.get().getUserId()),
                  usersDTO.getEmail(), myUsers.get().getCompanyId(),
                  beforeStateOpt.get(), afterStateOpt.get());
        }
      }

    } else {
      throw new Exception("Unkown Email");
    }
  }

  @Operation(summary = "Delete User Details", description = "Endpoint to delete user details")
  @DeleteMapping(value = "/deleteUserDetails/{companyId}/{email}")
  public void deleteUserDetails(
      @PathVariable Long companyId,
      @PathVariable String email,
      @RequestHeader("Authorization") String authHeader)
      throws Exception {
    //		System.out.print("-,,,,---,,,,---"+authHeader);
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    String jwt = authHeader.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    auditService.logDelete(AuditModule.USER, null, email, companyId,
            Map.of("email", email, "companyId", String.valueOf(companyId)));
    userService.deleteUser(companyId, email, authHeader);
  }

  @Operation(summary = "Add New Fields", description = "Endpoint to add new fields")
  @PostMapping("/addfields")
  public void addNewFields(
      @RequestBody UserExtraFieldsDTO extraFieldsDTO, @RequestHeader String Authorization)
      throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.addExtraFields(extraFieldsDTO);
  }

  @Operation(summary = "Get Extra Fields", description = "Endpoint to get extra fields")
  @GetMapping("/getExtraFields/{id}")
  public List<UserExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return userService.getExtraFields(id);
  }

  @Operation(summary = "Delete Extra Field", description = "Endpoint to delete extra field")
  @DeleteMapping("/deleteExtraFields/{id}")
  public void deleteExtraField(@PathVariable String id, @RequestHeader String Authorization)
      throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.deleteExtraFields(id);
  }

  @Operation(summary = "Get Extra Field Name", description = "Endpoint to get extra field name")
  @GetMapping("/getExtraFieldName/{companyId}")
  public List<UserExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    // System.out.println("----------my company------------->"+companyId);
    return userService.getUserExtraField(companyId);
  }

  @Operation(summary = "Add Extra Field Name", description = "Endpoint to add extra field name")
  @PostMapping("/addExtraFieldName")
  public void addExtraFieldName(
      @RequestBody UserExtraFieldNameDTO extraFieldNameDTO, @RequestHeader String Authorization)
      throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);

    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.addUserExtraField(extraFieldNameDTO);
  }

  @Operation(summary = "Delete Extra Field Name", description = "Endpoint to delete extra field name")
  @DeleteMapping("/deleteExtraFieldName/{id}")
  public void deleteExtraFieldName(@PathVariable String id, @RequestHeader String Authorization)
          throws NoSubscriptionError, UserAccessException {
    // System.out.println("-----------------------api------------------------>"+id);
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.deleteUserExtraField(id);
  }

  @Operation(summary = "Get Extra Field Name Value", description = "Endpoint to get extra field name value")
  @GetMapping("/getExtraFieldNameValue/{companyId}")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return userService.getextraFieldList(companyId);
  }

  @Operation(summary = "Mandatory Fields", description = "Endpoint to mandatory fields")
  @PostMapping("/mandatoryFields")
  public void mandatoryFields(
      @RequestBody UserMandatoryFields mandatoryFields, @RequestHeader String Authorization)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.updateMandatoryFields(mandatoryFields);
  }

  @Operation(summary = "Show Fields", description = "Endpoint to show fields")
  @PostMapping("/showFields")
  public void showFields(
      @RequestBody UserShowFields showFields, @RequestHeader String Authorization)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    String jwt = Authorization.substring(7);
    String userEmail = jwtService.extractUserEmail(jwt);
    Optional<Customer> customer = customerRepository.findByEmail(userEmail);
    if (customer.isPresent()) {

      Optional<Subscription> subscriptionOptional =
          subscriptionRepository.findByCompanyIdAndStatus(customer.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      if (subscriptionOptional.isEmpty()) {
        throw new NoSubscriptionError("No Active Subscription");
      }
    }
    userService.updateShowFields(showFields);
  }

  @Operation(summary = "Get Mandatory Fields", description = "Endpoint to get mandatory fields")
  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  public ResponseEntity<UserMandatoryFields> getMandatoryFields(
      @PathVariable String name, @PathVariable Long companyId) {
    // System.out.println("============================>"+name+companyId);
    UserMandatoryFields mandatoryFields = userService.getMandatoryFields(name, companyId);
    return ResponseEntity.ok(mandatoryFields);
  }

  @Operation(summary = "Get Show Fields", description = "Endpoint to get show fields")
  @GetMapping("/getShowFields/{name}/{companyId}")
  public ResponseEntity<UserShowFields> getShowFields(
      @PathVariable String name, @PathVariable Long companyId) {
    UserShowFields showFields = userService.getShowFields(name, companyId);
    return ResponseEntity.ok(showFields);
  }

  @Operation(summary = "Get All Mandatory Fields", description = "Endpoint to get all mandatory fields")
  @GetMapping("/getAllMandatoryFields/{companyId}")
  public ResponseEntity<List<UserMandatoryFields>> getAllMandatoryFields(
      @PathVariable Long companyId) {
    List<UserMandatoryFields> mandatoryFieldsList = userService.getAllMandatoryFields(companyId);
    return ResponseEntity.ok(mandatoryFieldsList);
  }

  @Operation(summary = "Get All Show Fields", description = "Endpoint to get all show fields")
  @GetMapping("/getAllShowFields/{companyId}")
  public ResponseEntity<List<UserShowFields>> getAllShowFields(@PathVariable Long companyId) {
    List<UserShowFields> showFieldsList = userService.getAllShowFields(companyId);
    return ResponseEntity.ok(showFieldsList);
  }

  @Operation(summary = "Show Fields", description = "Endpoint to show fields")
  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  public void showFields(@PathVariable String name, @PathVariable Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    Optional<Subscription> subscriptionOptional =
        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    if (subscriptionOptional.isEmpty()) {
      throw new NoSubscriptionError("No Active Subscription");
    }

    userService.deleteShowAndMandatoryFields(companyId, name);
  }
}
