package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.IdGenerator.AssetIdTable;
import com.quantumai.customer.entity.IdGenerator.CompanyCustomerIdTable;
import com.quantumai.customer.entity.IdGenerator.CompanyPrimaryKeyTable;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.security.JwtService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

  @Autowired private CustomerRepository customerRepository;
  @Autowired private CustomerSubscribedRepository customerSubscribedRepository;

  @Autowired private CompanyInformationRepository companyInformationRepository;

  @Autowired private CustomRoleRepository customRoleRepository;

  @Autowired private AccountLockInfoRepository accountLockInfoRepository;

  @Autowired private LocationRepository locationRepository;
  @Autowired private ImportHistoryRepository importHistoryRepository;

  @Autowired private BinRepository binRepository;

  @Autowired JwtService jwtService;

  @Autowired private JavaMailSender mailSender;

  @Autowired private CustomerStripeDetailsRepository customerStripeDetailsRepository;

  @Autowired private AdminRepository adminRepository;

  @Autowired private UsersRepository usersRepository;

  @Autowired private MongoTemplate mongoTemplate;

  private final Map<String, OTPEntry> customerOtpStorage = new HashMap<>();

  @Autowired
  //	@Qualifier("customerAuthenticationProvider")
  private AuthenticationManager authenticationManager;

  private ModelMapper modelMapper = new ModelMapper();

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private UserService userService;

  @Autowired CompanyPrimaryKeyTableRepository companyPrimaryKeyTableRepository;

  @Autowired CompanyCustomerRepository companyCustomerRepository;

  @Autowired CompanyCustomerIdTableRepository companyCustomerIdTableRepository;

  @Autowired AssetsRepository assetsRepository;

  @Autowired AssetIdTableRepository assetIdTableRepository;

  @Autowired AssetQRRepository assetQRRepository;

  private static final String SEQ_ID = "company_sequence";

  @Override
  public BaseResponseDTO addCustomer(CustomerDTO customerDTO) throws Exception {
    // TODO Auto-generated method stub
    //		System.out.print("---------------->Called");
    if (customerDTO == null) {
      throw new Exception("Empty User");
    }
    if (customerRepository.existsByEmail(customerDTO.getEmail())) {
      throw new UserAlreadyPresentException("User Already Present");
    }
    Customer customer = modelMapper.map(customerDTO, Customer.class);
    customer.setRole("ADMIN");
    customer.setPassword(passwordEncoder.encode(customerDTO.getPassword()));
    customerRepository.save(customer);
    BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
    baseResponseDTO.setSucess(true);
    baseResponseDTO.setMessage("User Successfully Created");

    Admin admin = new Admin();
    admin.setEmail(customer.getEmail());
    admin.setRole("ADMIN");
    admin.setPassword(passwordEncoder.encode("admin"));
    adminRepository.save(admin);

    // Add User In Customer as ADMIN

    return baseResponseDTO;
  }

  @Override
  public CustomerDTO getCustomer(String email) throws Exception {
    // TODO Auto-generated method stub
    if (email.isEmpty()) {
      throw new Exception("Empty email");
    }
    Optional<Customer> customer = customerRepository.findByEmail(email);
    CustomerDTO customerDTO = modelMapper.map(customer.get(), CustomerDTO.class);
    return customerDTO;
  }

  @Override
  public void sentResetOTP(String email) throws NoEmailFoundException {

    Optional<Customer> customer = customerRepository.findByEmail(email);
    if (customer.isPresent()) {
      String otp = String.valueOf((int) (Math.random() * 900000) + 100000); // Generate 6-digit OTP
      OTPEntry otpEntry = new OTPEntry();
      otpEntry.setOtp(otp);
      otpEntry.setCreateTime(LocalDateTime.now());
      customerOtpStorage.put(email, otpEntry);
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("OTP for Password Reset");
      message.setText("Your OTP for password reset is: " + otp);
      mailSender.send(message);
    } else {
      throw new NoEmailFoundException("No Such Email");
    }
  }

  @Override
  public void updatePassword(String email, String otp, String password)
      throws FirebaseAuthException, OTPException, NoEmailFoundException {
    if (otp.equals(customerOtpStorage.get(email).getOtp())
        && customerOtpStorage
            .get(email)
            .getCreateTime()
            .isAfter(LocalDateTime.now().minusMinutes(1))) {
      Optional<Customer> customer = customerRepository.findByEmail(email);
      if (customer.isPresent()) {
        Customer myCustomer = customer.get();

        myCustomer.setPassword(passwordEncoder.encode(password));
        customerRepository.save(myCustomer);
        //        log.info("Updated Password: {}",password);
        FirebaseAuth.getInstance();

        // Retrieve user record based on email
        UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);

        // Get user ID
        String userId = userRecord.getUid();
        UpdateRequest request = new UpdateRequest(userId).setPassword(password);

        // Update the user in Firebase
        UserRecord updatedUser = FirebaseAuth.getInstance().updateUser(request);
        customerOtpStorage.remove(email);
      } else {
        throw new NoEmailFoundException("No such Email");
      }

    } else {
      customerOtpStorage.remove(email);
      throw new OTPException("OTP is wrong");
    }
  }

  @Override
  public Boolean checkCustomer(String email) throws Exception {
    if (email.isEmpty()) {
      throw new Exception("Empty email");
    }
    Optional<Customer> customer = customerRepository.findByEmail(email);

    return customer.isPresent();
  }

  @Override
  public CustomerSubscribedDTO getCustomerSubscription(String email) throws NoSubscriptionError {
    // TODO Auto-generated method stub
    if (email.isEmpty()) {
      throw new NoSubscriptionError("Empty email");
    }
    Optional<CustomerSubscribed> customerSubscribed = customerSubscribedRepository.findById(email);

    if (customerSubscribed.isEmpty()
        || customerSubscribed.get().getLastDate().isBefore(LocalDate.now())) {
      throw new NoSubscriptionError("No subscription");
    }

    CustomerSubscribedDTO customerSubscribedDTO =
        modelMapper.map(customerSubscribed.get(), CustomerSubscribedDTO.class);
    return customerSubscribedDTO;
  }

  @Override
  public void addSubscription(String email) throws Exception {
    // TODO Auto-generated method stub
    if (email.isEmpty()) {
      throw new Exception("Empty email");
    }
    Optional<CustomerSubscribed> optionalCustomerSubscribed =
        customerSubscribedRepository.findById(email);
    CustomerSubscribed customerSubscribed = new CustomerSubscribed();

    if (optionalCustomerSubscribed.isEmpty()) {
      customerSubscribed.setEmail(email);
      customerSubscribed.setLastDate(LocalDate.now().plusMonths(1));
      customerSubscribed.setSubscription(true);
    } else {
      customerSubscribed = optionalCustomerSubscribed.get();
      customerSubscribed.setLastDate(LocalDate.now().plusMonths(1));
      customerSubscribed.setSubscription(true);
    }

    customerSubscribedRepository.save(customerSubscribed);
  }

  @Override
  public AuthenticationResponseDTO authenticate(
      AuthenticationRequestDTO authenticationRequestDTO, String deviceId) throws Exception {
    // TODO Auto-generated method stub
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            authenticationRequestDTO.getEmail(), authenticationRequestDTO.getPassword()));
    var user =
        customerRepository
            .findByEmail(authenticationRequestDTO.getEmail())
            .orElseThrow(() -> new Exception("Not Present"));
    var jwtToken = jwtService.generateToken(user, deviceId);
    return AuthenticationResponseDTO.builder().token(jwtToken).build();
  }

  @Override
  public AuthenticationResponseDTO getLoginToken(String email, String password, String deviceId)
      throws WrongCredentialException, UserNotFound {
    // TODO Auto-generated method stub
    Optional<Customer> customer = customerRepository.findByEmail(email);
    System.out.println("////" + customer);
    //    log.info("Pswd {} {}",passwordEncoder.encode(password),customer.get().getPassword()
    //    );
    if (customer.isEmpty()) {
      throw new UserNotFound("User Not Associated to any company");
    } else if (passwordEncoder.matches(password, customer.get().getPassword())) {

      var jwtToken = jwtService.generateToken(customer.get(), deviceId);

      return AuthenticationResponseDTO.builder()
          .token(jwtToken)
          .role(customer.get().getRole())
          .build();

    } else {
      throw new WrongCredentialException("Wrong Credential");
    }
    //		System.out.print(customer);

  }

  private void addDummyData(Long companyId) {

    // Create Dummy CompanyCustomer
    Optional<CompanyCustomerIdTable> companyCustomerIdTableOptional =
        companyCustomerIdTableRepository.findByCompanyId(companyId);
    CompanyCustomer companyCustomer = new CompanyCustomer();
    companyCustomer.setCompanyId(companyId);
    companyCustomer.setName("Alex");
    //    companyCustomer.setCategory("Retail");
    companyCustomer.setStatus("active");
    companyCustomer.setPhone("+1-555-123-4567");
    companyCustomer.setEmail("johndoe@example.com");
    companyCustomer.setAddress("123 Main Street");
    companyCustomer.setApartment("Apt 4B");
    companyCustomer.setCity("Buffalo");
    companyCustomer.setState("New York");
    companyCustomer.setZipCode(14201);
    companyCustomer.setUpdatedAt(LocalDateTime.now().toString());

    if (companyCustomerIdTableOptional.isEmpty()) {
      companyCustomer.setCompanyCustomerId(1);
      CompanyCustomerIdTable myidTable = new CompanyCustomerIdTable();
      myidTable.setTableId(2);
      myidTable.setCompanyId(companyId);
      companyCustomerIdTableRepository.save(myidTable);

    } else {

      CompanyCustomerIdTable idTable = companyCustomerIdTableOptional.get();
      companyCustomer.setCompanyCustomerId(idTable.getTableId());
      idTable.updateId();
      companyCustomerIdTableRepository.save(idTable);
    }

    CompanyCustomer savedCustomer = companyCustomerRepository.save(companyCustomer);

    // Create Dummy Asset
    Assets assets = new Assets();
    Optional<AssetIdTable> optionalIdTable = assetIdTableRepository.findByCompanyId(companyId);
    if (optionalIdTable.isEmpty()) {
      assets.setAssetId(1);
      AssetIdTable myidTable = new AssetIdTable();
      myidTable.setTableId(2);
      myidTable.setCompanyId(companyId);
      assetIdTableRepository.save(myidTable);
    } else {

      AssetIdTable idTable = optionalIdTable.get();
      assets.setAssetId(idTable.getTableId());
      idTable.updateId();
      assetIdTableRepository.save(idTable);
    }
    assets.setCustomerId(savedCustomer.getId());
    assets.setCustomer(savedCustomer.getName());
    assets.setStatus("active");
    assets.setName("Asset1");
    assets.setSerialNumber("10000");
    assets.setLocation("Buffalo");
    assets.setUpdatedAt(LocalDateTime.now().toString());
    assets.setCompanyId(companyId);
    assetsRepository.save(assets);

    // AssetQr
    AssetQR assetQR = new AssetQR();
    assetQR.setType("1");
    assetQR.setCustom("Custom Text");
    assetQR.setOptional("Optional Text");
    assetQR.setCompanyId(companyId);

    assetQRRepository.save(assetQR);
  }

  @Override
  public void addCompanyInformation(CompanyInformation companyInformation) throws Exception {
    // TODO Auto-generated method stub
    String email = companyInformation.getCustomerEmail();
    Optional<CompanyInformation> myCompanyInformation =
        companyInformationRepository.findByCustomerEmail(email);
    if (myCompanyInformation.isEmpty()) {
      //			customRoleRepository
      Optional<CompanyPrimaryKeyTable> companyPrimaryKeyTableOptional =
          companyPrimaryKeyTableRepository.findById(SEQ_ID);
      if (companyPrimaryKeyTableOptional.isEmpty()) {
        throw new Exception("Sequence Database Not Found");
      }
      CompanyPrimaryKeyTable companyPrimaryKeyTable = companyPrimaryKeyTableOptional.get();
      Long id = companyPrimaryKeyTable.getSeq();
      companyInformation.setId(id);
      companyPrimaryKeyTable.setSeq(id + 1);
      companyPrimaryKeyTableRepository.save(companyPrimaryKeyTable);

      CompanyInformation ci = companyInformationRepository.save(companyInformation);
      addDummyData(ci.getId());

      CustomRole customRole = new CustomRole();
      customRole.setCompanyId(ci.getId());
      customRole.setName("ADMIN");
      customRole.setStatus("active");
      customRole.setType(RoleType.STANDARD);
      customRole.setAssets(CustomRoleType.full);
      customRole.setCustomers(CustomRoleType.full);
      customRole.setWorkOrders(CustomRoleType.full);
      customRole.setUsers(CustomRoleType.full);
      customRole.setRoleAndPermissions(CustomRoleType.full);
      customRole.setImports(CustomRoleType.full);
      customRole.setCategory(CustomRoleType.full);
      customRole.setInventory(CustomRoleType.full);

      customRoleRepository.save(customRole);

    } else {
      companyInformation.setId(myCompanyInformation.get().getId());
      companyInformationRepository.save(companyInformation);
    }
  }

  @Override
  public CompanyInformation getcompanyInformation(Long companyId) {
    // TODO Auto-generated method stub

    Optional<CompanyInformation> myCompanyInformation =
        companyInformationRepository.findById(companyId);
    if (myCompanyInformation.isEmpty()) {
      return null;
    } else {

      return myCompanyInformation.get();
    }
  }

  @Override
  public CompanyIdDTO getCompanyId(String email) {
    // TODO Auto-generated method stub
    Optional<Customer> myCompanyInformation = customerRepository.findByEmail(email);
    if (myCompanyInformation.isEmpty()) {
      return null;
    } else {
      Customer companyInformation = myCompanyInformation.get();
      CompanyIdDTO companyIdDTO = new CompanyIdDTO();
      companyIdDTO.setCompanyName(companyInformation.getCompanyName());
      companyIdDTO.setId(companyInformation.getCompanyId());
      return companyIdDTO;
    }
  }

  @Override
  public BaseResponseDTO addUsers(CustomerDTO customerDTO) {
    // TODO Auto-generated method stub
    Customer customer = modelMapper.map(customerDTO, Customer.class);

    customer.setPassword(passwordEncoder.encode(customerDTO.getPassword()));
    customerRepository.save(customer);
    BaseResponseDTO baseResponseDTO = new BaseResponseDTO();
    baseResponseDTO.setSucess(true);
    baseResponseDTO.setMessage("User Successfully Created");
    Optional<Users> optionalUser = usersRepository.findByEmail(customerDTO.getEmail());
    if (optionalUser.isPresent()) {
      optionalUser.get().setStatus(StatusEnum.active);
      usersRepository.save(optionalUser.get());
    }

    return baseResponseDTO;
  }

  @Override
  public List<String> activeUsers(Long companyId) {
    // TODO Auto-generated method stub
    List<Customer> customerList = customerRepository.findByCompanyId(companyId);
    List<String> myCustomerList = new ArrayList<>();
    customerList.stream()
        .forEach(
            (x) -> {
              myCustomerList.add(x.getEmail());
            });
    return myCustomerList;
  }

  @Override
  public AccountLockInfoDTO getAccountInfo(String email) {
    // TODO Auto-generated method stub
    AccountLockInfoDTO accountLockInfoDTO = null;
    Optional<AccountLockInfo> accountLockInfoOptional =
        accountLockInfoRepository.findByCustomerEmail(email);
    if (accountLockInfoOptional.isPresent()) {
      AccountLockInfo accountLockInfo = accountLockInfoOptional.get();
      accountLockInfoDTO = modelMapper.map(accountLockInfo, AccountLockInfoDTO.class);
    }
    return accountLockInfoDTO;
  }

  @Override
  public void updateAccountInfo(AccountLockInfoDTO accountLockInfoDTO) {
    // TODO Auto-generated method stub
    AccountLockInfo accountLockInfo = modelMapper.map(accountLockInfoDTO, AccountLockInfo.class);
    accountLockInfoRepository.save(accountLockInfo);
  }

  @Override
  public void deleteUser(Long companyId, String email) throws CustomerException {
    // TODO Auto-generated method stub

    Optional<Customer> customerOptional =
        customerRepository.findByEmailAndCompanyId(email, companyId);
    if (customerOptional.isPresent()) {
      System.out.println("Deleting cutomer :" + email);
      customerRepository.delete(customerOptional.get());
    } else {
      throw new CustomerException("Customer Not Present");
    }
  }

  @Override
  public void addRoleAndPermission(CustomRoleDTO customRoleDTO) {
    // TODO Auto-generated method stub
    //		customRoleRepository
    CustomRole customRole = modelMapper.map(customRoleDTO, CustomRole.class);
    System.out.print("-------> permission" + customRole.getAssets());
    customRoleRepository.save(customRole);
  }

  @Override
  public void deleteRoleAndPermission(String customRoleId) throws RoleDeletionException {
    // TODO Auto-generated method stub
    Optional<CustomRole> customRoleOptional = customRoleRepository.findById(customRoleId);
    if (customRoleOptional.isPresent()) {
      Long count =
          countByRoleName(
              customRoleOptional.get().getName(), customRoleOptional.get().getCompanyId());
      if (count == 0) {
        customRoleRepository.delete(customRoleOptional.get());
      } else {
        throw new RoleDeletionException("Error in deleting role");
      }
    }
  }

  @Override
  public List<CustomRoleDTO> getRoleAndPermission(Long companyId) {
    // TODO Auto-generated method stub
    List<CustomRole> customRoleList = customRoleRepository.findByCompanyId(companyId);
    List<CustomRoleDTO> customRoleDTOList = new ArrayList<>();
    customRoleList.stream()
        .forEach(
            (x) -> {
              CustomRoleDTO customRoleDTO = modelMapper.map(x, CustomRoleDTO.class);
              customRoleDTOList.add(customRoleDTO);
            });
    return customRoleDTOList;
  }

  @Override
  public Long countByRoleName(String name, Long companyId) {
    // TODO Auto-generated method stub
    Long count = customerRepository.countByRoleAndCompanyId(name, companyId);
    return count;
  }

  @Override
  public CustomRoleDTO roleAndPermissionByName(Long companyId, String name) {
    // TODO Auto-generated method stub
    Optional<CustomRole> customRoleOptional =
        customRoleRepository.findByNameAndCompanyId(name, companyId);
    if (customRoleOptional.isEmpty()) {
      return null;
    }

    CustomRoleDTO customRoleDTO = modelMapper.map(customRoleOptional.get(), CustomRoleDTO.class);

    return customRoleDTO;
  }

  @Override
  public Location addLocation(Location location) {
    location.setStatus(StatusEnum.active);
    return locationRepository.save(location);
  }

  @Override
  public List<Location> getAllLocation(Long companyId) {
    List<Location> myLocations = new ArrayList<>();

    myLocations = locationRepository.findByCompanyId(companyId);
    return myLocations;
  }

  @Override
  public void deleteLocation(String id) {
    locationRepository.deleteById(id);
  }

  @Override
  public Bin addBin(Bin bin) {
    bin.setStatus(StatusEnum.active);
    return binRepository.save(bin);
  }

  @Override
  public List<BinDTO> getAllBin(Long companyId) {
    //    List<Bin> myBins = new ArrayList<>();
    //
    //    myBins = binRepository.findByCompanyId(companyId);
    //    return myBins;
    Aggregation agg =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("companyId").is(companyId)),
            Aggregation.lookup("location", "locationId", "_id", "locationDetails"),
            Aggregation.unwind("locationDetails", true),
            Aggregation.project("id", "locationId", "binNumber", "status", "companyId")
                .and("locationDetails.name")
                .as("locationName"));

    AggregationResults<BinDTO> results = mongoTemplate.aggregate(agg, "bin", BinDTO.class);
    return results.getMappedResults();
  }

  @Override
  public List<LocationWithBinsDTO> getLocationsWithBins(Long companyId) {
    Aggregation agg =
        Aggregation.newAggregation(
            Aggregation.match(Criteria.where("companyId").is(companyId)),
            Aggregation.lookup(
                "bin", "_id", "locationId", "bins"), // join from location._id to bin.locationId
            Aggregation.project("name")
                .and(ConvertOperators.ToString.toString("_id"))
                .as("id")
                .and("bins")
                .as("bins"));

    AggregationResults<LocationWithBinsDTO> results =
        mongoTemplate.aggregate(agg, "location", LocationWithBinsDTO.class);

    return results.getMappedResults();
  }

  @Override
  public void deleteBin(String id) {
    binRepository.deleteById(id);
  }

  @Override
  public ImportHistory addImportHistory(ImportHistory importHistory) {
    return importHistoryRepository.save(importHistory);
  }

  @Override
  public Page<ImportHistoryDTO> getImportHistoryList(Long companyId, int page, int size) {
    //		Pageable pageable = PageRequest.of(page, size);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
    return importHistoryRepository.findByCompanyId(companyId, pageable);
  }

  @Override
  public void updateImportHistory(ImportHistory importHistory) {
    importHistoryRepository.save(importHistory);
  }

  @Override
  public void addCardDetails(CustomerStripeDetails customerStripeDetails) {
    Optional<CustomerStripeDetails> optionalCustomerCardDetails =
        customerStripeDetailsRepository.findByCompanyId(customerStripeDetails.getCompanyId());
    optionalCustomerCardDetails.ifPresent(
        cardDetails -> customerStripeDetails.setId(cardDetails.getId()));
    customerStripeDetailsRepository.save(customerStripeDetails);
  }

  @Override
  public CustomerStripeDetails getCardDetails(Long companyId) {
    Optional<CustomerStripeDetails> optionalCustomerCardDetails =
        customerStripeDetailsRepository.findByCompanyId(companyId);
    return optionalCustomerCardDetails.orElse(null);
  }

  @Override
  public void deleteCardDetails(String id) {
    customerStripeDetailsRepository.deleteById(id);
  }
}
