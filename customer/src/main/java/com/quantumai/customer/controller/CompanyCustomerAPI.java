package com.quantumai.customer.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.service.CompanyCustomerService;
import com.quantumai.customer.service.CustomerService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// @CrossOrigin("http://assetyug.com.s3-website-us-east-1.amazonaws.com")
@Slf4j
@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@RestController
@RequestMapping(value = "/companycustomer")
public class CompanyCustomerAPI {



  // Services
  @Autowired private CompanyCustomerService companyCustomerService;
  @Autowired private CustomerService customerService;

  // Repositories
  @Autowired private CompanyCustomerRepository companyCustomerRepository;
  @Autowired private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;
  @Autowired private CompanyCustomerExtraFieldsRepository extraFieldsRepository;
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;
  @Autowired private CompanyCustomerMandatoryFieldsRepository mandatoryFieldsRepository;

  // External Services
  @Autowired private JavaMailSender emailSender;

  @Autowired private UsersRepository usersRepository;

  // Utilities
  private final ModelMapper modelMapper = new ModelMapper();

  // Constants
  private static final List<String> US_STATES = List.of(
          "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
          "Delaware", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois",
          "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine",
          "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
          "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire",
          "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota",
          "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island",
          "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah",
          "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"
  );

  private static final int MAX_IMPORT_ROWS = 1000;
  private static final String IMPORT_SUBJECT = "Import Report from AssetYug";
  private static final String EXCEL_FILENAME = "CustomerReport.xlsx";

  private Map<String, List<String>> data;

  @PostConstruct
  public void loadJson() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      InputStream is = getClass().getResourceAsStream("/Country-States.json");

      data = mapper.readValue(
              is,
              mapper.getTypeFactory().constructMapType(Map.class, String.class, List.class)
      );

    } catch (Exception e) {
      throw new RuntimeException("Failed to load country-state JSON", e);
    }
  }

  private void checkUserDetailsPermissionFromSpringContext(CustomRoleType customRoleType) throws UserAccessException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("Spring Security"+ authentication.getName());
    Optional<Users> usersOptional=usersRepository.findByEmail(authentication.getName());
    if(usersOptional.isPresent()){
      System.out.println(customRoleType.ordinal()+" "+usersOptional.get().getRole().getCustomers().ordinal());
      if(customRoleType.ordinal()>usersOptional.get().getRole().getCustomers().ordinal()){
        GenricErrorMessage genricErrorMessage=new GenricErrorMessage("User Dont Have access", HttpStatus.FORBIDDEN);
        throw new UserAccessException(genricErrorMessage.getMessage());
      }
    }
    else{
      GenricErrorMessage genricErrorMessage=new GenricErrorMessage("User Dont Have access", HttpStatus.FORBIDDEN);
      throw new UserAccessException(genricErrorMessage.getMessage());
    }




  }

  @GetMapping("/working")
  public String working() {
    System.out.println("working!!!");
    return "Working!!";
  }

  @DeleteMapping("/deleteCompanyCustomer/{id}")
  public void deleteCompanyCustomer(@PathVariable String id, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteCustomer(id);
  }

  @GetMapping("/allCompanyCustomer/{companyId}")
  public List<CompanyCustomerDTO> getCompanyCustomerList(@PathVariable Long companyId) {
    return companyCustomerService.getAllCustomer(companyId);
  }

  @GetMapping("/getCompanyCustomer/{id}")
  public CompanyCustomerDTO getCompanyCustomer(@PathVariable String id) {
    System.out.println(id);
    return companyCustomerService.getCustomer(id);
  }

  @GetMapping("/getCompanyCustomerByLocalId/{id}/{companyId}")
  public CompanyCustomerDTO getCompanyCustomerByLocalId(
          @PathVariable String id, @PathVariable Long companyId) {
    System.out.println(id);
    return companyCustomerService.getCompanyCustomerByLocalId(Integer.valueOf(id), companyId);
  }

  //	@GetMapping("/allCompanyCustomerWithExtraFields/{companyId}")
  //	public List<CompanyCustomerDTO> getCompanyCustomerWithExtraFields(@PathVariable String
  // companyId){
  //		return companyCustomerService.getAllCustomer(companyId);
  //	}
  @PostMapping("/addCompanyCustomer")
  public CompanyCustomerDTO addNewFields(
          @RequestBody CompanyCustomerDTO companyCustomerDTO, @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    System.out.println("CompanyId-->" + companyId);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    return companyCustomerService.addCustomer(companyCustomerDTO);
  }

  @PutMapping("/updateCompanyCustomer")
  public void updateCompanyCustomer(
          @RequestBody CompanyCustomerDTO companyCustomerDTO, @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.updateCustomer(companyCustomerDTO);
  }

  @GetMapping(value = "/searchCompanyCustomerlist/{companyId}")
  public List<String> getCompanyCustomerFromAsset(
          @PathVariable Long companyId,
          @RequestParam(name = "data", required = true) String search,
          @RequestParam(name = "category", required = true) String category) {
    System.out.println("----------my CompanyCustomer search------------->" + search);
    return companyCustomerService.searchedCompanyCustomer(companyId, search, category);
  }

  @GetMapping(value = "/sortCompanyCustomerlist/{companyId}")
  public List<String> getCompanyCustomerFromAsset(
          @PathVariable Long companyId,
          @RequestParam(name = "category", required = true) String category) {
    return companyCustomerService.sortCompanyCustomer(companyId, category);
  }

  @PostMapping("/addExtraFieldName")
  public void addExtraFieldName(
          @RequestBody CompanyCustomerExtraFieldNameDTO extraFieldNameDTO,
          @RequestHeader Long companyId)
          throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.addCompanyCustomerExtraField(extraFieldNameDTO);
  }

  @GetMapping("/getExtraFieldName/{companyId}")
  public List<CompanyCustomerExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    //		System.out.println("----------my company------------->"+companyId);
    return companyCustomerService.getCompanyCustomerExtraField(companyId);
  }

  @DeleteMapping("/deleteExtraFieldName/{id}")
  public void deleteExtraFieldName(@PathVariable String id, @RequestHeader Long companyId)
          throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteCompanyCustomerExtraField(id);
  }

  @PostMapping("/mandatoryFields")
  public void mandatoryFields(
          @RequestBody CompanyCustomerMandatoryFields mandatoryFields, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.updateMandatoryFields(mandatoryFields);
  }

  @PostMapping("/showFields")
  public void showFields(
          @RequestBody CompanyCustomerShowFields showFields, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.updateShowFields(showFields);
  }

  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  public ResponseEntity<CompanyCustomerMandatoryFields> getMandatoryFields(
          @PathVariable String name, @PathVariable Long companyId) {
    System.out.println("============================>" + name + companyId);
    CompanyCustomerMandatoryFields mandatoryFields =
            companyCustomerService.getMandatoryFields(name, companyId);
    return ResponseEntity.ok(mandatoryFields);
  }

  @GetMapping("/getShowFields/{name}/{companyId}")
  public ResponseEntity<CompanyCustomerShowFields> getShowFields(
          @PathVariable String name, @PathVariable Long companyId) {
    CompanyCustomerShowFields showFields = companyCustomerService.getShowFields(name, companyId);
    return ResponseEntity.ok(showFields);
  }

  @GetMapping("/getAllMandatoryFields/{companyId}")
  public ResponseEntity<List<CompanyCustomerMandatoryFields>> getAllMandatoryFields(
          @PathVariable Long companyId) {
    List<CompanyCustomerMandatoryFields> mandatoryFieldsList =
            companyCustomerService.getAllMandatoryFields(companyId);
    return ResponseEntity.ok(mandatoryFieldsList);
  }

  @GetMapping("/getAllShowFields/{companyId}")
  public ResponseEntity<List<CompanyCustomerShowFields>> getAllShowFields(
          @PathVariable Long companyId) {
    List<CompanyCustomerShowFields> showFieldsList =
            companyCustomerService.getAllShowFields(companyId);
    return ResponseEntity.ok(showFieldsList);
  }

  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  public void showFields(@PathVariable String name, @PathVariable Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteShowAndMandatoryFields(companyId, name);
  }

  @GetMapping("/getExtraFieldNameValue/{companyId}")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {

    return companyCustomerService.getextraFieldList(companyId);
  }

  @PostMapping("/addfields")
  public void addNewFields(
          @RequestBody CompanyCustomerExtraFieldsDTO extraFieldsDTO, @RequestHeader Long companyId)
          throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.addExtraFields(extraFieldsDTO);
  }

  @GetMapping("/getExtraFields/{id}")
  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return companyCustomerService.getExtraFields(id);
  }

  @DeleteMapping("/deleteExtraFields/{id}")
  public void deleteExtraField(@PathVariable String id, Long companyId) throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteExtraFields(id);
  }

  @DeleteMapping("/deleteCompanyCustomerExtraFields/{id}")
  public void deleteCompanyCustomerExtraFields(
          @PathVariable String id, @RequestHeader Long companyId) throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteExtraFieldByCompanyCustomer(id);
  }

  @GetMapping("/allCompanyCustomerWithExtraFields/{id}")
  public List<String> allCompanyCustomerWithExtraFields(@PathVariable Long id) {
    return companyCustomerService.getAllCustomerWithExtraColumns(id);
  }

  @PostMapping("/addFile/{companyCustomerId}")
  public ResponseEntity<ResponseMessageDTO> addCompanyCustomerFile(
          @RequestParam("file") MultipartFile file,
          @PathVariable String companyCustomerId,
          @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    String message = "";
    try {
      companyCustomerService.addCompanyCustomerFile(file, companyCustomerId);
      message = "Uploaded the file successfully: " + file.getOriginalFilename();
      ResponseMessageDTO responseMessageDTO = new ResponseMessageDTO();
      responseMessageDTO.setResponseMessage(message);
      return new ResponseEntity<>(responseMessageDTO, HttpStatus.ACCEPTED);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      message = "Could not upload the file: " + file.getOriginalFilename() + "!";
      ResponseMessageDTO responseMessageDTO = new ResponseMessageDTO();
      responseMessageDTO.setResponseMessage(message);
      return new ResponseEntity<>(responseMessageDTO, HttpStatus.EXPECTATION_FAILED);
    }
  }

  @GetMapping("/getFile/{companyCustomerId}")
  public List<CompanyCustomerFileDTO> getCompanyCustomerFile(
          @PathVariable String companyCustomerId) {
    return companyCustomerService.getCompanyCustomerFile(companyCustomerId);
  }

  @GetMapping("/getFile/download/{id}")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    CompanyCustomerFileDTO companyCustomerFileDTO = companyCustomerService.downloadFile(id);
    //		return new ResponseEntity<>(assetFileDTO.getFile(),HttpStatus.OK);
    return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.valueOf("json/object"))
            .body(companyCustomerFileDTO.getFile());
  }

  @DeleteMapping("deleteFile/{id}")
  public void deleteFile(@PathVariable String id, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.deleteFile(id);
  }

  @PostMapping("/advanceFilter/{pageNumber}/{pageSize}")
  public PaginatedResultDTO<String> advanceFilter(
          @RequestBody Object filter,
          @PathVariable(required = false) Integer pageNumber,
          @PathVariable(required = false) Integer pageSize,
          @RequestParam(name = "category", required = false) String category,
          @RequestParam(name = "search", required = false) String searchData,
          @RequestParam(name = "asc", required = false) Boolean asc,
          @RequestHeader Long companyId)
          throws NoSubscriptionError {

    //    Optional<Subscription> subscriptionOptional=
    // subscriptionRepository.findByCompanyIdAndStatus(companyId,SubscriptionEnum.ACTIVE);
    //    if(subscriptionOptional.isEmpty()){
    //      throw new NoSubscriptionError("No Active Subscription");
    //    }

    if (asc == null) {
      asc = true;
    }
    System.out.println("advanceSearch");
    if (searchData != null) {
      //			logger.info("Search Data: {} -- Length: {}", searchData, searchData.length());
    }

    // Handle default values for pageNumber and pageSize
    if (pageNumber == null) {
      pageNumber = 0; // Default value for pageNumber
    }

    if (pageSize == null) {
      pageSize = 5; // Default value for pageSize
    }
    if (category == null || category.equals("")) {
      category = "updatedAt";
      asc = false;
    }
    //	return  null;

    return companyCustomerService.advanceFilter(
            filter, pageNumber, pageSize, category, searchData, asc);
  }

  @PostMapping("/import/{companyId}/{email}")
  public void importFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException,
          MessagingException,
          ImportFileRowException,
          NoSubscriptionError, EmailAlreadyExistsException, NameColumnMissingException, UserAccessException {

    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    System.out.println("------||---------------------------------------/////////////////////////////////////------->"+columnMappings);
    List<CompanyCustomerMandatoryFields> mandatoryFieldsList=mandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId,true);
    Map<String,Boolean> mandatoryFieldsMap=new HashMap<>();
    for(CompanyCustomerMandatoryFields mandatoryFields:mandatoryFieldsList){
      mandatoryFieldsMap.put(mandatoryFields.getName().toLowerCase(),true);
    }
    log.info("Mandatory Fields Map: {}", mandatoryFieldsMap);

    Map<String, String> columnMap = new HashMap<>();

    try {
      JsonFactory jsonFactory = new JsonFactory();
      JsonParser jsonParser = jsonFactory.createParser(columnMappings);

      ObjectMapper objectMapper = new ObjectMapper();
      Map<String, String> map= objectMapper.readValue(
              columnMappings,
              new TypeReference<Map<String, String>>() {}
      );

      if(!map.containsValue("Name")){
        throw new NameColumnMissingException("Name Column Missing");
      }

      String key = "", val = "";
      while (!jsonParser.isClosed()) {
        JsonToken jsonToken = jsonParser.nextToken();
        if (jsonToken == null) {
          break;
        }

        if (!key.isEmpty()) {
          columnMap.put(key, val);
        }
        switch (jsonToken) {
          case START_OBJECT:
            break;
          case FIELD_NAME:
            key = jsonParser.getCurrentName();
            break;
          case VALUE_STRING:
            val = jsonParser.getText();
            break;
          case END_OBJECT:
            break;
          default:
            break;
        }
      }

      jsonParser.close();
    }
    catch (NameColumnMissingException e) {
      throw e;
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    List<CompanyCustomerDTO> assetList = new ArrayList<CompanyCustomerDTO>();
    long totalCount = Integer.MAX_VALUE;
    try (InputStream inputStream = file.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      totalCount = reader.lines().count() - 1;
      System.out.println("Total Count" + totalCount);
      if (reader.lines().count() > MAX_IMPORT_ROWS + 1) {
        throw new ImportFileRowException("Import File cannot import more than " + MAX_IMPORT_ROWS + " rows");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    ImportHistory importHistoryDTO = new ImportHistory();
    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
         CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = new HashMap<>();

      if (headers != null) {
        for (int i = 0; i < headers.length; i++) {
          headerMap.put(i, headers[i]);
        }
      }

      String[] row;
      long ind = 0;
      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Sheet1");
      CellStyle errorCellStyle = createErrorCellStyle(workbook);

      Row headerRow = sheet.createRow(0);
      int headerColIndex = 0;
      headerRow.createCell(headerColIndex++).setCellValue("Line#");
      for (int k = 0; k < Objects.requireNonNull(headers).length; k++) {
        String field = headerMap.get(k);
        if (field != null) {
          headerRow.createCell(headerColIndex++).setCellValue(field);
        }
      }
      headerRow.createCell(headerColIndex).setCellValue("Error Description");

      int excelIndex = 1;
      int currCount = 0;

      importHistoryDTO.setFileName(file.getOriginalFilename());
      importHistoryDTO.setRecordType("Customer Record");
      importHistoryDTO.setExecutedBy(email);
      importHistoryDTO.setDate(LocalDateTime.now().toString());
      importHistoryDTO.setStatus("In-Progress");
      importHistoryDTO.setCompanyId(companyId);
      System.out.println("===========>");
      System.out.println(importHistoryDTO);

      while ((row = csvReader.readNext()) != null) {

        boolean isEmpty = Arrays.stream(row)
                .allMatch(cell -> cell == null || cell.trim().isEmpty());
        if (isEmpty) {
          continue;
        }

        System.out.println("CSV Row Raw: " + Arrays.toString(row));

        String[] trimmedRow = Arrays.stream(row)
                .map(cell -> cell == null ? "" : cell.trim())
                .toArray(String[]::new);
        System.out.println("CSV Row Trimmed: " + Arrays.toString(trimmedRow));

        Row myrow = sheet.createRow(excelIndex);
        Cell lineNumberCell = myrow.createCell(0);
        lineNumberCell.setCellValue("Line " + (int) (ind + 1));

        CompanyCustomerDTO companyCustomerDTO = new CompanyCustomerDTO();
        companyCustomerDTO.setCompanyId(companyId);
        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        Map<Integer, Boolean> errorCellMap = new HashMap<>();

        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          Cell dataCell = myrow.createCell(j + 1);
          dataCell.setCellValue(row[j]);

          if (errorFlag == 1) {
            break;
          }

          System.out.println(field + "------->" + columnMap.get(field));
          if (columnMap.get(field) != null) {
            switch (columnMap.get(field).toLowerCase()) {
              case "name":
                System.out.println("name//->" + row[j]);
                if(row[j].trim().equals("")){
                  errorDesc.append("ERROR WITH NO NAME WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                companyCustomerDTO.setName(row[j]);
                break;

              case "phone":
                companyCustomerDTO.setPhone(row[j]);
                log.info("Phone to be set: {}", row[j]);
                if(mandatoryFieldsMap.containsKey("phone")){
                  log.info("Phone before inside empty: {}", row[j]);
                  if(row[j].trim().isEmpty()){
                    log.info("Phone inside empty: {}", row[j]);
                    errorDesc.append("ERROR WITH PHONE MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                }
                break;

              case "category":
                System.out.println("category//->" + row[j]);
                List<CompanyCustomerCategory> categoryList=companyCustomerCategoryRepository.findByCompanyId(companyId);
                String rowValue=row[j];
                if(!rowValue.trim().isBlank()){
                  List<CompanyCustomerCategory> list=categoryList.stream().filter(x-> x.getName().equalsIgnoreCase(rowValue)).toList();
                  if(list.isEmpty()){
                    errorDesc.append("ERROR IN CATEGORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                  else{
                    if(mandatoryFieldsMap.containsKey("category")){
                      if(row[j].trim().isEmpty()){
                        errorDesc.append("ERROR WITH CATEGORY MANDATORY WHILE ADDING IN CUSTOMER");
                        errorFlag = 1;
                        errorCellMap.put(j + 1, true);
                        break;
                      }
                    }
                    companyCustomerDTO.setCategory(list.get(0).getName());
                  }
                }
                break;

              case "email":
                String emailValue = row[j] != null ? row[j].trim() : "";
                if (StringUtils.isBlank(emailValue)) {
                  // Email is optional, set empty string instead of null
                  companyCustomerDTO.setEmail("");
                } else {
                  Optional<CompanyCustomer> myCustomer = companyCustomerRepository
                          .findByEmailAndCompanyId(emailValue, companyCustomerDTO.getCompanyId());
                  if (myCustomer.isPresent()) {
                    errorFlag = 1;
                    errorDesc.append("Email already exists. ");
                    log.info("Email already: {}", emailValue);
                    errorCellMap.put(j + 1, true);
                  } else {
                    log.info("Email to be set: {}", emailValue);
                    if(mandatoryFieldsMap.containsKey("email")){
                      if(row[j].trim().isEmpty()){
                        errorDesc.append("ERROR WITH EMAIL MANDATORY WHILE ADDING IN CUSTOMER");
                        errorFlag = 1;
                        errorCellMap.put(j + 1, true);
                        break;
                      }
                    }
                    companyCustomerDTO.setEmail(emailValue);
                  }
                }
                break;



              case "address":
                System.out.println("address//->" + row[j]);
                if(mandatoryFieldsMap.containsKey("address")){
                  if(row[j].trim().isEmpty()){
                    errorDesc.append("ERROR WITH ADDRESS MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }
                companyCustomerDTO.setAddress(row[j]);
                break;

              case "city":
                companyCustomerDTO.setCity(row[j]);
                if(mandatoryFieldsMap.containsKey("city")){
                  if(row[j].trim().isEmpty()){
                    errorDesc.append("ERROR WITH CITY MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }
                break;

              case "state":
                String myState=row[j];
                if(!row[j].equals("")){
                  List<String> selectedStateList=US_STATES.stream().filter(myState::equalsIgnoreCase).toList();

                  if(!selectedStateList.isEmpty()){
                    companyCustomerDTO.setState(selectedStateList.get(0));
                    boolean isStateMatched=false;
                    if(mandatoryFieldsMap.containsKey("state")){
                      if(row[j].trim().isEmpty()){
                        errorDesc.append("ERROR WITH STATE MANDATORY WHILE ADDING IN CUSTOMER");
                        errorFlag = 1;
                        errorCellMap.put(j + 1, true);
                        break;
                      }
                    }
                    else{
                      for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                        for (String state:entry.getValue()){
                          if(state.equalsIgnoreCase(myState)){
                            companyCustomerDTO.setCountry(entry.getKey());
                            isStateMatched=true;
                            break;
                          }
                        }
                        if (isStateMatched) break;
                      }
                    }

                  }
                  else{
                    errorDesc.append("ERROR WHILE ADDING IN STATE");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                }
                break;

              case "zipcode":
                System.out.println("zipCode//->" + row[j]);
                if(mandatoryFieldsMap.containsKey("zipcode")){
                  if(row[j].trim().isEmpty()){
                    errorDesc.append("ERROR WITH ZIPCODE MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }
                else{
                  try {
                    companyCustomerDTO.setZipCode(Integer.parseInt(row[j]));
                  } catch (NumberFormatException e) {
                    errorDesc.append("ERROR IN ZIPCODE FORMAT");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                }

                break;

              case "status":
                if(mandatoryFieldsMap.containsKey("status")){
                  if(row[j].trim().isEmpty()){
                    errorDesc.append("ERROR WITH STATUS MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }
                else {
                  if ((row[j].equalsIgnoreCase("active"))
                          || (row[j].equalsIgnoreCase("inactive"))) {

                    if (row[j].toLowerCase().equals("active")) {
                      companyCustomerDTO.setStatus("active");
                    } else {
                      companyCustomerDTO.setStatus("inActive");
                    }

                    errorFlag = 0;
                    break;

                  } else {
                    if (errorDesc.length() > 0) {
                      errorDesc.append(", ");
                    }
                    errorDesc.append("ERROR WHILE ADDING IN STATUS");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }


            }
          }
        }

        // Apply red background to error cells
        for (Map.Entry<Integer, Boolean> entry : errorCellMap.entrySet()) {
          if (entry.getValue()) {
            Cell errorCell = myrow.getCell(entry.getKey());
            if (errorCell != null) {
              errorCell.setCellStyle(errorCellStyle);
            }
          }
        }
        String [] defaultNameCheck= {"Name","Phone","Email","Address","City","State","Zipcode","Status","Category"};
        for(String defaultName:defaultNameCheck){
          log.info("Checking mandatory for field: {}", defaultName);
          // check companyCustomerDTO fields value if mandatory is not present then error flag=1 with meesage
          if(mandatoryFieldsMap.containsKey(defaultName.toLowerCase())){
            log.info("Field {} is mandatory", defaultName);
            String value="";
            switch (defaultName.toLowerCase()){
              case "name":
                value=companyCustomerDTO.getName()!=null?companyCustomerDTO.getName().trim():"";
                break;
              case "phone":
                value=companyCustomerDTO.getPhone()!=null?companyCustomerDTO.getPhone().trim():"";
                break;
              case "email":
                value=companyCustomerDTO.getEmail()!=null?companyCustomerDTO.getEmail().trim():"";
                break;
              case "address":
                value=companyCustomerDTO.getAddress()!=null?companyCustomerDTO.getAddress().trim():"";
                break;
              case "city":
                value=companyCustomerDTO.getCity()!=null?companyCustomerDTO.getCity().trim():"";
                break;
              case "state":
                value=companyCustomerDTO.getState()!=null?companyCustomerDTO.getState().trim():"";
                break;
              case "zipcode":
                value=companyCustomerDTO.getZipCode()!=null?companyCustomerDTO.getZipCode().toString().trim():"";
                break;
              case "status":
                value=companyCustomerDTO.getStatus()!=null?companyCustomerDTO.getStatus().trim():"";
                break;
              case "category":
                value=companyCustomerDTO.getCategory()!=null?companyCustomerDTO.getCategory().trim():"";
                break;
            }
            log.info("Field value {}", value);
            if (value.isEmpty()) {
              log.info("Value is empty");
              errorFlag = 1;
              errorDesc.append("Mandatory field ").append(defaultName.toUpperCase()).append(" is not mapped.");
            }
          }
        }

        if (errorFlag == 0) {

          CompanyCustomerDTO mynewCustomer = companyCustomerService.addCustomer(companyCustomerDTO);

          for (int j = 0; j < row.length; j++) {
            String field = headerMap.get(j);
            String value = row[j];

            List<CompanyCustomerExtraFieldName> listExtraFieldName =
                    extraFieldNameRepository.findByCompanyId(companyId);
            if (columnMap.get(field) != null) {
              for (CompanyCustomerExtraFieldName companyCustomerExtraFieldName : listExtraFieldName) {

                if ((columnMap.get(field) != null)
                        && columnMap
                        .get(field)
                        .equalsIgnoreCase(companyCustomerExtraFieldName.getName())) {
                  CompanyCustomerExtraFields extraFieldsDTO = new CompanyCustomerExtraFields();
                  extraFieldsDTO.setCompanyCustomerId(mynewCustomer.getId());
                  extraFieldsDTO.setName(companyCustomerExtraFieldName.getName());
                  extraFieldsDTO.setType(companyCustomerExtraFieldName.getType());

                  extraFieldsDTO.setCompanyId(companyId);
                  log.info("Mandatory Fields "+mandatoryFieldsMap.toString());
                  log.info("Mandatory Fields Map Check for {}: {}", companyCustomerExtraFieldName.getName(), mandatoryFieldsMap.containsKey(companyCustomerExtraFieldName.getName().toLowerCase()));
                  if(mandatoryFieldsMap.containsKey(companyCustomerExtraFieldName.getName().toLowerCase())){
                    if(value.trim().isEmpty()){
                      errorDesc.append("ERROR WITH ").append(companyCustomerExtraFieldName.getName().toUpperCase()).append(" MANDATORY WHILE ADDING IN CUSTOMER");
                      errorFlag = 1;
                      break;
                    }
                  }
                  else{
                    if (companyCustomerExtraFieldName.getType().equals("number")) {
                      try {
                        int val = Integer.parseInt(value);
                        extraFieldsDTO.setValue(Integer.toString(val));
                      } catch (Exception e) {
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(companyCustomerExtraFieldName.getName().toUpperCase());
                      }
                    }
                    if (companyCustomerExtraFieldName.getType().equals("date")) {
                      try {

                        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(value, inputFormatter);

                        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String formattedDate = date.format(outputFormatter);

                        extraFieldsDTO.setValue(formattedDate);
                      } catch (Exception e) {
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(companyCustomerExtraFieldName.getName().toUpperCase());
                      }
                    } else {
                      extraFieldsDTO.setValue(value);
                    }
                  }

                  if (errorFlag == 0) {
                    extraFieldsRepository.save(extraFieldsDTO);
                  } else {
                    CompanyCustomer myCustomer =
                            modelMapper.map(mynewCustomer, CompanyCustomer.class);
                    companyCustomerRepository.delete(myCustomer);
                  }
                }
              }
            }

          }
        }

        Cell errorDescriptionCell = myrow.createCell(row.length + 1);
        errorDescriptionCell.setCellValue(errorDesc.toString());
        if (errorFlag == 1) {
          errorDescriptionCell.setCellStyle(errorCellStyle);
          excelIndex++;
        }

        ind++;
        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now().toString());
        long complete = (currCount * 100L) / (totalCount);
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);

      }
      if (excelIndex > 1) {
        importHistoryDTO.setMessage("We have sent import result via email");
        Sheet mySheet = workbook.getSheetAt(0);

        int lastRowNum = mySheet.getLastRowNum();

        if (lastRowNum >= 0) {
          Row lastRow = mySheet.getRow(lastRowNum);
          if (lastRow != null) {
            mySheet.removeRow(lastRow);
          }
        }

        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();
        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);

          helper.setTo(email);
          helper.setSubject(IMPORT_SUBJECT);
          helper.setText("Hey, We have attached import result below");
          helper.addAttachment("CustomerAttachment.xlsx", new File(EXCEL_FILENAME));

          emailSender.send(message);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error in sending mail: " + e);
        }
      }
      if (excelIndex == 1) {
        importHistoryDTO.setMessage("Import was Successfully Done");
        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();
        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);

          helper.setTo(email);
          helper.setSubject(IMPORT_SUBJECT);
          helper.setText("Hey, Your Import was Successfully Done");

          emailSender.send(message);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error in sending mail: " + e);
        }
      }

      importHistoryDTO.setStatus("Completed");
    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      e.printStackTrace();
    }
    customerService.addImportHistory(importHistoryDTO);
    log.info("Import History {}", importHistoryDTO);
  }

  private CellStyle createErrorCellStyle(Workbook workbook) {
    CellStyle errorStyle = workbook.createCellStyle();
    errorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
    errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return errorStyle;
  }


  @PostMapping("/importUpdation/{companyId}/{email}")
  public void importUpdation(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException,
          JsonParseException,
          IOException,
          MessagingException,
          ImportFileRowException,
          NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    System.out.println("------||-------->" + columnMappings);
    Map<String, String> columnMap = new HashMap<>();
    try {
      // Create a JsonFactory and a JsonParser
      JsonFactory jsonFactory = new JsonFactory();
      JsonParser jsonParser = jsonFactory.createParser(columnMappings);

      // Loop through JSON tokens
      String key = "", val = "";
      while (!jsonParser.isClosed()) {
        // Get the current token
        JsonToken jsonToken = jsonParser.nextToken();
        if (jsonToken == null) {
          break;
        }

        if (key.equals("") == false) {
          columnMap.put(key, val);
        }
        switch (jsonToken) {
          case START_OBJECT:
            System.out.println("Start of object");
            break;
          case FIELD_NAME:
            System.out.println("Field name: " + jsonParser.getCurrentName());
            key = jsonParser.getCurrentName();
            break;
          case VALUE_STRING:
            System.out.println("Field value: " + jsonParser.getText());
            val = jsonParser.getText();

            break;
          case END_OBJECT:
            System.out.println("End of object");
            break;
          default:
            break;
        }
      }

      // Close the JsonParser
      jsonParser.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    long totalCount = Integer.MAX_VALUE;
    try (InputStream inputStream = file.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      totalCount = reader.lines().count() - 1;
      if (reader.lines().count() > MAX_IMPORT_ROWS + 1) {
        throw new ImportFileRowException("Import File cannot import more than " + MAX_IMPORT_ROWS + " rows");
      }

    } catch (IOException e) {
      // Handle IOException
      e.printStackTrace();
    }
    ImportHistory importHistoryDTO = new ImportHistory();
    List<CompanyCustomerDTO> inventoryList = new ArrayList<CompanyCustomerDTO>();
    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
         CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = new HashMap<>();

      if (headers != null) {
        for (int i = 0; i < headers.length; i++) {
          headerMap.put(i, headers[i]);
        }
      }

      String[] row;
      long ind = 0;
      Workbook workbook = new XSSFWorkbook();
      int currCount = 0;

      importHistoryDTO.setFileName(file.getOriginalFilename());
      importHistoryDTO.setRecordType("Update Customer Record");
      importHistoryDTO.setExecutedBy(email);
      importHistoryDTO.setDate(LocalDateTime.now().toString());
      importHistoryDTO.setStatus("In-Progress");
      importHistoryDTO.setCompanyId(companyId);
      // Create a Sheet
      Sheet sheet = workbook.createSheet("Sheet1");
      int excelIndex = 0;
      while ((row = csvReader.readNext()) != null) {
        boolean isEmpty = Arrays.stream(row)
                .allMatch(cell -> cell == null || cell.trim().isEmpty());
        if (isEmpty) {
          continue; // Don't create the Excel row or process this CSV line
        }

        System.out.println("CSV Row Raw: " + Arrays.toString(row));

        // Print the trimmed row values
        String[] trimmedRow = Arrays.stream(row)
                .map(cell -> cell == null ? "" : cell.trim())
                .toArray(String[]::new);
        System.out.println("CSV Row Trimmed: " + Arrays.toString(trimmedRow));

        Row myrow = sheet.createRow(excelIndex);
        Cell cell1 = myrow.createCell(0);
        cell1.setCellValue("Row " + (int) (ind + 1));
        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        //	            AssetsDTO assetsDTO = new AssetsDTO();
        //	            assetsDTO.setCompanyId(companyId);
        CompanyCustomer companyCustomer = new CompanyCustomer();
        for (int j = 0; j < row.length; j++) {


          if (j > 0 && companyCustomer == null) {
            break;
          }
          String field = headerMap.get(j);
          //	                System.out.println("--------------> "+j+" " + row[j]);
          //					System.out.print("-------|||||-------> "+columnMap.get(field).toLowerCase());

          if (columnMap.get(field) != null) {
            switch (columnMap.get(field).toLowerCase()) {
              case "companycustomerid":
                String companyCustomerIdValue = row[j].trim();
                //	                    assetIdValue = assetIdValue.substring(0,
                // assetIdValue.length() - 2);
                //	                    try {
                //	                        if (!assetIdValue.isEmpty()) {
                //	                            assetsDTO.setAssetId(Integer.parseInt(assetIdValue));
                //	                        } else {
                //	                            // Handle empty cell case, set a default value, or
                // take appropriate action
                //	                        }
                //	                    } catch (NumberFormatException e) {
                //	                        // Handle the exception or log an error message
                //	                        System.err.println("Error parsing integer: " +
                // e.getMessage());
                //	                        // Set a default value or take appropriate action
                //	                    }
                companyCustomer =
                        companyCustomerRepository.findByCompanyCustomerIdAndCompanyId(
                                Integer.parseInt(companyCustomerIdValue), companyId);
                //
                // System.out.println("------------------/////////----"+assets.getId());
                //	                    assetsDTO.setId(assets.getId());
                //
                // System.out.println("------------------/////////----"+assetsDTO.getId());
                if (companyCustomer == null) {
                  errorFlag = 1;
                  errorDesc.append("ERROR WHILE UPDATING IN INVENTORY ID");
                }
                break;

              case "name":
                companyCustomer.setName(row[j]);
                if(row[j].trim().equals("")){
                  errorDesc.append("ERROR WITH NO NAME WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  break;
                }
                break;

              case "phone":
                companyCustomer.setPhone(row[j]);
                break;
              case "category":
                System.out.println("category//->" + row[j]);
                List<CompanyCustomerCategory> categoryList=companyCustomerCategoryRepository.findByCompanyId(companyId);
                String rowValue=row[j];
                if(!rowValue.trim().isBlank()){
                  List<CompanyCustomerCategory> list=categoryList.stream().filter(x-> x.getName().equalsIgnoreCase(rowValue)).toList();
                  if(list.isEmpty()){
                    errorDesc.append("ERROR IN CATEGORY WHILE ADDING IN CUSTOMER");
                  }
                  else{
                    companyCustomer.setCategory(list.get(0).getName());
                  }
                }

                break;
              case "email":
                Optional<CompanyCustomer> myCustomer=companyCustomerRepository.findByEmailAndCompanyId(companyCustomer.getEmail(),companyCustomer.getCompanyId());
                if(myCustomer.isPresent()&&!myCustomer.get().getId().equals(companyCustomer.getId())){
                  // throw new EmailAlreadyExistsException("User With Email Aready Present");
                  errorDesc.append("User With Email Aready Present");
                  errorFlag = 1;
                  break;
                }
                companyCustomer.setEmail(row[j]);
                break;
              case "address":
                System.out.println("address//->" + row[j]);
                companyCustomer.setAddress(row[j]);
                break;
              case "city":
                companyCustomer.setCity(row[j]);
                break;
              case "state":

                String myState=row[j];
                List<String> selectedStateList=US_STATES.stream().filter(myState::equalsIgnoreCase).toList();
                if(!selectedStateList.isEmpty()){
                  companyCustomer.setState(selectedStateList.get(0));
                  boolean isStateMatched=false;
                  for (Map.Entry<String, List<String>> entry : data.entrySet()) {
                    for (String state:entry.getValue()){
                      log.info(state+" "+myState);
                      if(state.equalsIgnoreCase(myState)){
                        companyCustomer.setCountry(entry.getKey());
                        isStateMatched=true;
                        break;
                      }
                    }
                    if (isStateMatched) break;
                  }

                }
                else{
                  errorDesc.append("ERROR WHILE ADDING IN STATE");
                  errorFlag = 1;
                }

                break;
              case "zipcode":
                System.out.println("zipCode//->" + row[j]);
                companyCustomer.setZipCode(Integer.parseInt(row[j]));
                break;
              case "status":
                if ((row[j].equalsIgnoreCase("active"))
                        || (row[j].equalsIgnoreCase("inactive"))) {

                  if (row[j].equalsIgnoreCase("active")) {
                    companyCustomer.setStatus("active");
                  } else {
                    companyCustomer.setStatus("inActive");
                  }

                  errorFlag = 0;
                  break;

                } else {
                  // System.out.println("ERROR WHILE ADDING IN Status for line->"+ind);
                  if (!errorDesc.isEmpty()) {
                    errorDesc.append(", ");
                  }
                  errorDesc.append("ERROR WHILE ADDING IN STATUS");
                  errorFlag = 1;
                  break;
                }
            }

            if (errorFlag == 0) {
              String value = row[j];

              List<CompanyCustomerExtraFieldName> listExtraFieldName =
                      extraFieldNameRepository.findByCompanyId(companyId);
              String id = companyCustomer.getId();
              //		               System.out.println("--------id------>"+id);
              for (int i = 0; i < listExtraFieldName.size(); i++) {
                if (columnMap
                        .get(field)
                        .equalsIgnoreCase(listExtraFieldName.get(i).getName())) {
                  CompanyCustomerExtraFields extraFieldsDTO = new CompanyCustomerExtraFields();
                  CompanyCustomerExtraFields extraFieldsOptional =
                          extraFieldsRepository.findByNameIgnoreCaseAndCompanyCustomerId(
                                  listExtraFieldName.get(i).getName(), id);
                  System.out.println(
                          "-----------------working ---->" + listExtraFieldName.get(i).getType());
                  if (extraFieldsOptional != null) {
                    extraFieldsDTO.setId(extraFieldsOptional.getId());
                    extraFieldsDTO.setCompanyCustomerId(id);
                    extraFieldsDTO.setName(listExtraFieldName.get(i).getName());
                    extraFieldsDTO.setType(listExtraFieldName.get(i).getType());
                    if (listExtraFieldName.get(i).getType().equals("number")) {
                      try {
                        int val = Integer.parseInt(value);
                        System.out.println("-----------------extra---->" + val + "->" + value);
                        extraFieldsDTO.setValue(Integer.toString(val));
                      } catch (Exception e) {
                        System.out.println(
                                "ERROR WHILE ADDING EXTRA IN"
                                        + listExtraFieldName.get(i).getName()
                                        + " for row->"
                                        + ind);
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(listExtraFieldName.get(i).getName().toUpperCase());
                      }
                    } else if (listExtraFieldName.get(i).getType().equals("date")) {
                      try {

                        DateTimeFormatter inputFormatter =
                                DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(value, inputFormatter);

                        DateTimeFormatter outputFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String formattedDate = date.format(outputFormatter);

                        System.out.println(
                                "-----------------extra-date--->" + formattedDate + "->" + value);
                        extraFieldsDTO.setValue(formattedDate);
                      } catch (Exception e) {
                        System.out.println(
                                "ERROR WHILE ADDING EXTRA IN"
                                        + listExtraFieldName.get(i).getName()
                                        + " for row->"
                                        + ind);
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(listExtraFieldName.get(i).getName().toUpperCase());
                      }
                    } else {
                      extraFieldsDTO.setValue(value);
                    }
                    extraFieldsDTO.setCompanyId(companyId);
                    extraFieldsRepository.save(extraFieldsDTO);
                  } else {
                    extraFieldsDTO.setCompanyCustomerId(id);
                    extraFieldsDTO.setName(listExtraFieldName.get(i).getName());
                    extraFieldsDTO.setType(listExtraFieldName.get(i).getType());
                    if (listExtraFieldName.get(i).getType().equals("number")) {
                      try {
                        int val = Integer.parseInt(value);
                        System.out.println("-----------------extra---->" + val + "->" + value);
                        extraFieldsDTO.setValue(Integer.toString(val));
                      } catch (Exception e) {
                        System.out.println(
                                "ERROR WHILE ADDING EXTRA IN"
                                        + listExtraFieldName.get(i).getName()
                                        + " for row->"
                                        + ind);
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(listExtraFieldName.get(i).getName().toUpperCase());
                      }
                    } else if (listExtraFieldName.get(i).getType().equals("date")) {
                      try {

                        DateTimeFormatter inputFormatter =
                                DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(value, inputFormatter);

                        DateTimeFormatter outputFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String formattedDate = date.format(outputFormatter);

                        System.out.println(
                                "-----------------extra-date--->" + formattedDate + "->" + value);
                        extraFieldsDTO.setValue(formattedDate);
                      } catch (Exception e) {
                        System.out.println(
                                "ERROR WHILE ADDING EXTRA IN"
                                        + listExtraFieldName.get(i).getName()
                                        + " for row->"
                                        + ind);
                        errorFlag = 1;
                        if (!errorDesc.isEmpty()) {
                          errorDesc.append(", ");
                        }
                        errorDesc.append("ERROR WHILE ADDING IN ").append(listExtraFieldName.get(i).getName().toUpperCase());
                      }
                    } else {
                      extraFieldsDTO.setValue(value);
                    }
                    extraFieldsDTO.setCompanyId(companyId);
                    extraFieldsRepository.save(extraFieldsDTO);
                  }

                  if (errorFlag == 0) {
                    extraFieldsRepository.save(extraFieldsDTO);
                  }
                }
              }
            }
          }
        }

        if (errorFlag == 0) {
          System.out.println("saving inventory" + companyCustomer.getId());
          //	            CustomerDTO inventoryDTO = modelMapper.map(inventory, CustomerDTO.class);
          companyCustomerRepository.save(companyCustomer);
          // -------------------------------------------
        }

        //	            System.out.println();
        Cell cell2 = myrow.createCell(1);
        cell2.setCellValue(errorDesc.toString());
        if (errorFlag == 1) {
          System.out.println("Inside errorFLag");

          // Close the workbook to release resources
          excelIndex++;
        }
        ind++;
        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now().toString());
        long complete = (currCount * 100L) / (totalCount);
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
      }
      if (excelIndex > 0) {
        importHistoryDTO.setMessage("We have sent import result via email");
        Sheet mySheet = workbook.getSheetAt(0);

        // Get last row index (0-based)
        int lastRowNum = mySheet.getLastRowNum();

        if (lastRowNum >= 0) {
          Row lastRow = mySheet.getRow(lastRowNum);
          if (lastRow != null) {
            mySheet.removeRow(lastRow);
          }
        }
        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject(IMPORT_SUBJECT);
        helper.setText("Hey, We have attached import result below");
        helper.addAttachment("CustomerAttachment.xlsx", new File(EXCEL_FILENAME));

        emailSender.send(message);
      }
      if (excelIndex == 0) {
        importHistoryDTO.setMessage("Import was Successfully Done");
        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject(IMPORT_SUBJECT);
        helper.setText("Hey, Your Update was Successfully Done");
        //	            helper.addAttachment("ExcelAttachment.xlsx", new File("Report.xlsx"));

        emailSender.send(message);
      }

      importHistoryDTO.setStatus("Completed");

      //	        assetsService.importExcel(assetList);

    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());

    }
    customerService.addImportHistory(importHistoryDTO);
    //		System.out.println(importHistoryDTO);
    log.info("Import History {}", importHistoryDTO);
  }

  @GetMapping(value = "/statelist")
  public ResponseEntity<List<String>> statelist() {
    return ResponseEntity.ok(US_STATES);
  }



  @PostMapping(value = "/addCategory")
  public void addCategory(@RequestBody CategoryDTO categoryDTO, @RequestHeader Long companyId)
          throws Exception {
    System.out.println("Category===>" + categoryDTO);
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.addCategory(categoryDTO);
  }

  @GetMapping(value = "/countCompanyCustomerByCategory/{category}")
  public int countCompanyCustomerByCategory(@PathVariable String category)
          throws CategoryException {
    System.out.println("Category===>" + category);
    return companyCustomerService.countCompanyCustomerByCategory(category);
  }

  @GetMapping(value = "/getCategoryList/{companyId}")
  public List<CompanyCustomerCategory> getCategoryList(@PathVariable Long companyId) {
    return companyCustomerService.getCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  public List<CompanyCustomerCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return companyCustomerService.getActiveCategoryList(companyId);
  }

  @DeleteMapping(value = "/deleteCategory/{id}")
  public void deleteCategory(@PathVariable String id, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    companyCustomerService.deleteCategory(id);
  }

  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  public CompanyCustomerCategory getCategoryById(
          @PathVariable Long companyId, @PathVariable String id) {
    return companyCustomerService.getCategoryListById(companyId, id);
  }

  @PutMapping(value = "/updateCategory")
  public void updateCategory(@RequestBody CategoryDTO categoryDTO, @RequestHeader Long companyId)
          throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    //Optional<Subscription> subscriptionOptional =
    //  subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    //  if (subscriptionOptional.isEmpty()) {
    //  throw new NoSubscriptionError("No Active Subscription");
//    }
    companyCustomerService.updateCategory(categoryDTO);
  }

  //  @GetMapping(value = "/CustomFieldCustomerCount/{companyId}/{id}")
//  public Integer getCustomFieldCustomerCount(
//          @PathVariable Long companyId, @PathVariable String id) {
//    return companyCustomerService.getCustomFieldCustomerCount(companyId, id);
//  }
  @GetMapping("/export-company-customer/{companyId}")
  public ResponseEntity<byte[]> exportCompanyCustomers(@PathVariable Long companyId) throws IOException {
    List<CompanyCustomer> customers = companyCustomerRepository.findByCompanyId(companyId);
    List<CompanyCustomerExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Customers");
    Row header = sheet.createRow(0);
    int col = 0;
    // Add default headers
    String[] defaultHeaders = {"ID", "Name", "Email", "Phone", "Status"};
    for (String h : defaultHeaders) {
      header.createCell(col++).setCellValue(h);
    }
    // Add extra field headers
    for (CompanyCustomerExtraFieldName extraField : extraFieldNames) {
      header.createCell(col++).setCellValue(extraField.getName());
    }
    int rowIdx = 1;
    for (CompanyCustomer customer : customers) {
      Row row = sheet.createRow(rowIdx++);
      int c = 0;
      row.createCell(c++).setCellValue(customer.getId());
      row.createCell(c++).setCellValue(customer.getName());
      row.createCell(c++).setCellValue(customer.getEmail());
      row.createCell(c++).setCellValue(customer.getPhone());
      row.createCell(c++).setCellValue(customer.getStatus());
      // Fetch custom fields
      List<CompanyCustomerExtraFields> extras = extraFieldsRepository.findByCompanyCustomerId(customer.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (CompanyCustomerExtraFields ef : extras) {
        extraMap.put(ef.getName(), ef.getValue());
      }
      for (CompanyCustomerExtraFieldName extraField : extraFieldNames) {
        row.createCell(c++).setCellValue(extraMap.getOrDefault(extraField.getName(), ""));
      }
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    workbook.close();
    byte[] excelBytes = bos.toByteArray();
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=CompanyCustomerExport.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excelBytes);
  }

  @PutMapping("/extraFieldName")
  public ResponseEntity<CompanyCustomerExtraFieldName> updateExtraFieldName(@RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) throws UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    CompanyCustomerExtraFieldName companyCustomerExtraFieldName=companyCustomerService.updateExtraFieldName(extraFieldNameUpdateDTO);
    return ResponseEntity.ok(companyCustomerExtraFieldName);



  }
}
