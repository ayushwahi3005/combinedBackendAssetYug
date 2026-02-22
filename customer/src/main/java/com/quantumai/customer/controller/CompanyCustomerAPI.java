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
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.security.access.prepost.PreAuthorize;
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

  @Autowired private CompanyCustomerService companyCustomerService;
  @Autowired private CustomerService customerService;
  @Autowired private CompanyCustomerRepository companyCustomerRepository;
  @Autowired private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;
  @Autowired private CompanyCustomerExtraFieldsRepository extraFieldsRepository;
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;
  @Autowired private CompanyCustomerMandatoryFieldsRepository mandatoryFieldsRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private JavaMailSender emailSender;
  @Autowired private UsersRepository usersRepository;

  private final ModelMapper modelMapper = new ModelMapper();

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
  private static final String IMPORT_SUBJECT = "Customer Import Result  - AssetYug";
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

  @GetMapping("/working")
  public String working() {
    System.out.println("working!!!");
    return "Working!!";
  }

  // ─── Customer CRUD ────────────────────────────────────────────────────────

  @GetMapping("/allCompanyCustomer/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerDTO> getCompanyCustomerList(@PathVariable Long companyId) {
    return companyCustomerService.getAllCustomer(companyId);
  }

  @GetMapping("/getCompanyCustomer/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public CompanyCustomerDTO getCompanyCustomer(@PathVariable String id) {
    return companyCustomerService.getCustomer(id);
  }

  @GetMapping("/getCompanyCustomerByLocalId/{id}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerDTO getCompanyCustomerByLocalId(
          @PathVariable String id, @PathVariable Long companyId) {
    return companyCustomerService.getCompanyCustomerByLocalId(Integer.valueOf(id), companyId);
  }

  @PostMapping("/addCompanyCustomer")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public CompanyCustomerDTO addNewFields(
          @RequestBody CompanyCustomerDTO companyCustomerDTO,
          @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException {
    return companyCustomerService.addCustomer(companyCustomerDTO);
  }

  @PutMapping("/updateCompanyCustomer")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void updateCompanyCustomer(
          @RequestBody CompanyCustomerDTO companyCustomerDTO,
          @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException {
    companyCustomerService.updateCustomer(companyCustomerDTO);
  }

  @DeleteMapping("/deleteCompanyCustomer/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteCompanyCustomer(
          @PathVariable String id,
          @RequestHeader Long companyId)
          throws NoSubscriptionError {
    companyCustomerService.deleteCustomer(id);
  }

  // ─── Search & Sort ────────────────────────────────────────────────────────

  @GetMapping(value = "/searchCompanyCustomerlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<String> searchCompanyCustomer(
          @PathVariable Long companyId,
          @RequestParam(name = "data", required = true) String search,
          @RequestParam(name = "category", required = true) String category) {
    return companyCustomerService.searchedCompanyCustomer(companyId, search, category);
  }

  @GetMapping(value = "/sortCompanyCustomerlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<String> sortCompanyCustomer(
          @PathVariable Long companyId,
          @RequestParam(name = "category", required = true) String category) {
    return companyCustomerService.sortCompanyCustomer(companyId, category);
  }

  // ─── Extra Fields ─────────────────────────────────────────────────────────

  @PostMapping("/addExtraFieldName")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addExtraFieldName(
          @RequestBody CompanyCustomerExtraFieldNameDTO extraFieldNameDTO,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.addCompanyCustomerExtraField(extraFieldNameDTO);
  }

  @GetMapping("/getExtraFieldName/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    return companyCustomerService.getCompanyCustomerExtraField(companyId);
  }

  @DeleteMapping("/deleteExtraFieldName/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteExtraFieldName(
          @PathVariable String id,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.deleteCompanyCustomerExtraField(id);
  }

  @PostMapping("/addfields")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addNewFields(
          @RequestBody CompanyCustomerExtraFieldsDTO extraFieldsDTO,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.addExtraFields(extraFieldsDTO);
  }

  @GetMapping("/getExtraFields/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return companyCustomerService.getExtraFields(id);
  }

  @DeleteMapping("/deleteExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'customers')")
  public void deleteExtraField(@PathVariable String id, Long companyId) throws Exception {
    companyCustomerService.deleteExtraFields(id);
  }

  @DeleteMapping("/deleteCompanyCustomerExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteCompanyCustomerExtraFields(
          @PathVariable String id,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.deleteExtraFieldByCompanyCustomer(id);
  }

  @GetMapping("/getExtraFieldNameValue/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return companyCustomerService.getextraFieldList(companyId);
  }

  @PutMapping("/extraFieldName")
  @PreAuthorize("@appSecurity.canEdit(authentication, #extraFieldNameUpdateDTO.companyId, 'customers')")
  public ResponseEntity<CompanyCustomerExtraFieldName> updateExtraFieldName(
          @RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
    CompanyCustomerExtraFieldName result = companyCustomerService.updateExtraFieldName(extraFieldNameUpdateDTO);
    return ResponseEntity.ok(result);
  }

  // ─── Mandatory & Show Fields ──────────────────────────────────────────────

  @PostMapping("/mandatoryFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void mandatoryFields(
          @RequestBody CompanyCustomerMandatoryFields mandatoryFields,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    companyCustomerService.updateMandatoryFields(mandatoryFields);
  }

  @PostMapping("/showFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void showFields(
          @RequestBody CompanyCustomerShowFields showFields,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    companyCustomerService.updateShowFields(showFields);
  }

  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<CompanyCustomerMandatoryFields> getMandatoryFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getMandatoryFields(name, companyId));
  }

  @GetMapping("/getShowFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<CompanyCustomerShowFields> getShowFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getShowFields(name, companyId));
  }

  @GetMapping("/getAllMandatoryFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<List<CompanyCustomerMandatoryFields>> getAllMandatoryFields(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getAllMandatoryFields(companyId));
  }

  @GetMapping("/getAllShowFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<List<CompanyCustomerShowFields>> getAllShowFields(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getAllShowFields(companyId));
  }

  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteShowAndMandatoryField(
          @PathVariable String name,
          @PathVariable Long companyId) throws NoSubscriptionError {
    companyCustomerService.deleteShowAndMandatoryFields(companyId, name);
  }

  // ─── All Customers with Extra Fields ─────────────────────────────────────

  @GetMapping("/allCompanyCustomerWithExtraFields/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #id, 'customers')")
  public List<String> allCompanyCustomerWithExtraFields(@PathVariable Long id) {
    return companyCustomerService.getAllCustomerWithExtraColumns(id);
  }

  // ─── File Management ──────────────────────────────────────────────────────

  @PostMapping("/addFile/{companyCustomerId}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public ResponseEntity<ResponseMessageDTO> addCompanyCustomerFile(
          @RequestParam("file") MultipartFile file,
          @PathVariable String companyCustomerId,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    try {
      companyCustomerService.addCompanyCustomerFile(file, companyCustomerId);
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Uploaded the file successfully: " + file.getOriginalFilename());
      return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    } catch (IOException e) {
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Could not upload the file: " + file.getOriginalFilename() + "!");
      return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
    }
  }

  @GetMapping("/getFile/{companyCustomerId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public List<CompanyCustomerFileDTO> getCompanyCustomerFile(@PathVariable String companyCustomerId) {
    return companyCustomerService.getCompanyCustomerFile(companyCustomerId);
  }

  @GetMapping("/getFile/download/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    CompanyCustomerFileDTO companyCustomerFileDTO = companyCustomerService.downloadFile(id);
    return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.valueOf("json/object"))
            .body(companyCustomerFileDTO.getFile());
  }

  @DeleteMapping("deleteFile/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteFile(
          @PathVariable String id,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    companyCustomerService.deleteFile(id);
  }

  // ─── Advance Filter ───────────────────────────────────────────────────────

  @PostMapping("/advanceFilter/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public PaginatedResultDTO<String> advanceFilter(
          @RequestBody Object filter,
          @PathVariable(required = false) Integer pageNumber,
          @PathVariable(required = false) Integer pageSize,
          @RequestParam(name = "category", required = false) String category,
          @RequestParam(name = "search", required = false) String searchData,
          @RequestParam(name = "asc", required = false) Boolean asc,
          @RequestHeader Long companyId) throws NoSubscriptionError {

    if (asc == null) asc = true;
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 5;
    if (category == null || category.equals("")) {
      category = "updatedAt";
      asc = false;
    }

    return companyCustomerService.advanceFilter(filter, pageNumber, pageSize, category, searchData, asc);
  }

  // ─── Category ─────────────────────────────────────────────────────────────

  @PostMapping(value = "/addCategory")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addCategory(
          @RequestBody CategoryDTO categoryDTO,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.addCategory(categoryDTO);
  }

  @GetMapping(value = "/getCategoryList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerCategory> getCategoryList(@PathVariable Long companyId) {
    return companyCustomerService.getCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return companyCustomerService.getActiveCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerCategory getCategoryById(
          @PathVariable Long companyId, @PathVariable String id) {
    return companyCustomerService.getCategoryListById(companyId, id);
  }

  // ─── Template / Download ─────────────────────────────────────────────────

  @GetMapping("/template-fields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerTemplateFieldsDTO getTemplateFields(@PathVariable Long companyId) {
    return companyCustomerService.getTemplateFields(companyId);
  }

  @GetMapping(value = "/template-download/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long companyId) {
    try {
      byte[] data = companyCustomerService.generateCompanyCustomerTemplateXlsx(companyId);
      return ResponseEntity.ok()
              .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
              .header("Content-Disposition", "attachment; filename=CompanyCustomerTemplate.xlsx")
              .body(data);
    } catch (IOException e) {
      log.error("Error generating template", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  // ─── Import ───────────────────────────────────────────────────────────────

  @PostMapping("/import/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void importFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, MessagingException, ImportFileRowException,
          NoSubscriptionError, EmailAlreadyExistsException, NameColumnMissingException {


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
      importHistoryDTO.setDate(LocalDateTime.now());
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
                    companyCustomerDTO.setZipCode(row[j]);
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
              if(!errorDesc.isEmpty()){
                errorDesc.append(". Mandatory field ").append(defaultName.toUpperCase()).append(" is not mapped.");
              }

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
        importHistoryDTO.setDate(LocalDateTime.now());
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
        String subjectName = "User";
        Optional<Customer> customerOptional=
                customerRepository.findByEmail(email);
        if(customerOptional.isPresent()){
          Customer customer=customerOptional.get();
          String fullName=customer.getFirstName() + " " + customer.getLastName();
          if(customer.getFirstName()!=null&&customer.getLastName()!=null){
            subjectName=fullName;
          }
          else {
            subjectName="User";
          }
        }
        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);


          helper.setTo(email);
          helper.setSubject(IMPORT_SUBJECT);
          helper.setText("Hi,\n" +
                  "\n" +
                  "Your import has been completed. Please check the attached file for the errors that need correction. Once fixed, please reupload only the data listed in the file.\n" +
                  "\n" +
                  "If you need any help or additional information, feel free to reach out.\n" +
                  "\n" +
                  "Best regards,\n" +
                  "Asset Yug Team");
          helper.addAttachment("CustomerAttachment.xlsx", new File(EXCEL_FILENAME));

          emailSender.send(message);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error in sending mail: " + e);
        }
      }
      if (excelIndex == 1) {
        String subjectName = "User";
        Optional<Customer> customerOptional=
                customerRepository.findByEmail(email);
        if(customerOptional.isPresent()){
          Customer customer=customerOptional.get();
          String fullName=customer.getFirstName() + " " + customer.getLastName();
          if(customer.getFirstName()!=null&&customer.getLastName()!=null){
            subjectName=fullName;
          }
          else {
            subjectName="User";
          }
        }
        importHistoryDTO.setMessage("Hi "+ subjectName+",\n" +
                "\n" +
                "Your import has been completed successfully. All data has been processed and is now available in the system.\n" +
                "\n" +
                "Best regards,\n" +
                "AssetYug Team");
        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();

        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);

          helper.setTo(email);
          helper.setSubject(IMPORT_SUBJECT);
          helper.setText("Hi,\n" +
                  "\n" +
                  "Your import has been completed successfully. All data has been processed and is now available in the system.\n" +
                  "\n" +
                  "Best regards,\n" +
                  "AssetYug Team");

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

  @PostMapping("/importUpdation/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void importUpdation(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, JsonParseException, IOException,
          MessagingException, ImportFileRowException, NoSubscriptionError {

    // Parse column mappings (expects a JSON object mapping CSV header -> field name)
    Map<String, String> columnMap = new HashMap<>();
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      Map<String, String> parsed = objectMapper.readValue(columnMappings, new TypeReference<Map<String, String>>() {});
      if (parsed != null) columnMap.putAll(parsed);
    } catch (Exception ex) {
      log.warn("Failed to parse columnMappings JSON, proceeding with empty map", ex);
    }

    // Prepare import history
    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType("Update Customer Record");
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);

    long totalCount = 0L;
    try (InputStream inputStream = file.getInputStream();
         BufferedReader countReader = new BufferedReader(new InputStreamReader(inputStream))) {
      totalCount = Math.max(0, countReader.lines().count() - 1);
      if (totalCount > MAX_IMPORT_ROWS) {
        throw new ImportFileRowException("Import File cannot import more than " + MAX_IMPORT_ROWS + " rows");
      }
    } catch (IOException e) {
      log.warn("Error counting CSV rows", e);
    }

    // Build mandatory fields map (same logic as importFile)
    List<CompanyCustomerMandatoryFields> mandatoryFieldsList = mandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId, true);
    Map<String, Boolean> mandatoryFieldsMap = new HashMap<>();
    for (CompanyCustomerMandatoryFields mf : mandatoryFieldsList) {
      mandatoryFieldsMap.put(mf.getName().toLowerCase(), true);
    }

    // Process CSV and build error workbook (if any)
    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
         CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = new HashMap<>();
      if (headers != null) {
        for (int i = 0; i < headers.length; i++) headerMap.put(i, headers[i]);
      }

      Workbook workbook = new XSSFWorkbook();
      Sheet sheet = workbook.createSheet("Sheet1");
      CellStyle errorCellStyle = createErrorCellStyle(workbook);

      int excelIndex = 1; // Excel row index for error workbook (skip header)
      long ind = 0L;
      int currCount = 0;
      String[] row;

      // Create header row for error sheet
      Row headerRow = sheet.createRow(0);
      int headerColIndex = 0;
      headerRow.createCell(headerColIndex++).setCellValue("Line#");
      if (headers != null) {
        for (String h : headers) headerRow.createCell(headerColIndex++).setCellValue(h);
      }
      headerRow.createCell(headerColIndex).setCellValue("Error Description");

      while ((row = csvReader.readNext()) != null) {
        boolean isEmpty = Arrays.stream(row).allMatch(cell -> cell == null || cell.trim().isEmpty());
        if (isEmpty) { ind++; continue; }

        Row myrow = sheet.createRow(excelIndex);
        myrow.createCell(0).setCellValue("Line " + (ind + 1));

        CompanyCustomer companyCustomer = null;
        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        Map<Integer, Boolean> errorCellMap = new HashMap<>();

        // iterate columns and update fields
        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          String mapped = columnMap.get(field) != null ? columnMap.get(field).toLowerCase() : null;

          // create a visible cell in error workbook copying raw value
          Cell dataCell = myrow.createCell(j + 1);
          dataCell.setCellValue(row[j] == null ? "" : row[j]);

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
                if (companyCustomer == null) companyCustomer = new CompanyCustomer();
                companyCustomer.setName(row[j]);
                break;

              case "phone":
                if (companyCustomer == null) companyCustomer = new CompanyCustomer();
                companyCustomer.setPhone(row[j]);
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
                    companyCustomer.setCategory(list.get(0).getName());
                  }
                }
                break;

              case "email":
                String emailValue = row[j] != null ? row[j].trim() : "";
                if (StringUtils.isBlank(emailValue)) {
                  // Email is optional, set empty string instead of null
                  companyCustomer.setEmail("");
                } else {
                  Optional<CompanyCustomer> myCustomer = companyCustomerRepository
                          .findByEmailAndCompanyId(emailValue, companyCustomer.getCompanyId());
                  if (myCustomer.isPresent() && (companyCustomer.getId() == null || !myCustomer.get().getId().equals(companyCustomer.getId()))) {
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
                    companyCustomer.setEmail(emailValue);
                  }
                }
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
                if(!row[j].equals("")){
                  List<String> selectedStateList=US_STATES.stream().filter(myState::equalsIgnoreCase).toList();

                  if(!selectedStateList.isEmpty()){
                    companyCustomer.setState(selectedStateList.get(0));
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
                            companyCustomer.setCountry(entry.getKey());
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
                companyCustomer.setZipCode(row[j]);
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
                      companyCustomer.setStatus("active");
                    } else {
                      companyCustomer.setStatus("inActive");
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

          // If there are extra fields mappings, handle them now (simple implementation)
          if (errorFlag == 0 && companyCustomer != null) {
            // ensure we set companyId
            companyCustomer.setCompanyId(companyId);
            // Save or update main customer
            companyCustomerRepository.save(companyCustomer);

            // Now iterate again to save extra fields if mapped
            for (int k = 0; k < row.length; k++) {
              String innerField = headerMap.get(k);
              String innerMapped = columnMap.get(innerField) != null ? columnMap.get(innerField) : null;
              if (innerMapped == null) continue;
              // If mapped matches an extra field name for this company, save it
              List<CompanyCustomerExtraFieldName> extraNames = extraFieldNameRepository.findByCompanyId(companyId);
              for (CompanyCustomerExtraFieldName ef : extraNames) {
                if (ef.getName().equalsIgnoreCase(innerMapped)) {
                  CompanyCustomerExtraFields extra = extraFieldsRepository.findByNameIgnoreCaseAndCompanyCustomerId(ef.getName(), companyCustomer.getId());
                  if (extra == null) extra = new CompanyCustomerExtraFields();
                  extra.setCompanyCustomerId(companyCustomer.getId());
                  extra.setCompanyId(companyId);
                  extra.setName(ef.getName());
                  extra.setType(ef.getType());
                  String val = row[k] == null ? "" : row[k].trim();
                  extra.setValue(val);
                  extraFieldsRepository.save(extra);
                }
              }
            }
          }
        }

        // Apply red background to error cells
        for (Map.Entry<Integer, Boolean> entry : errorCellMap.entrySet()) {
          if (entry.getValue()) {
            Cell errorCell = myrow.getCell(entry.getKey());
            if (errorCell != null) errorCell.setCellStyle(errorCellStyle);
          }
        }

        // Write error description column
        Cell errorDescriptionCell = myrow.createCell((headers == null ? 0 : headers.length) + 1);
        errorDescriptionCell.setCellValue(errorDesc.toString());
        if (errorFlag == 1) {
          errorDescriptionCell.setCellStyle(errorCellStyle);
          excelIndex++;
        }

        ind++;
        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now());
        long complete = (totalCount == 0) ? 100 : (currCount * 100L) / totalCount;
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
      }

      // If we have errors (rows in workbook), write and email the file
      if (excelIndex > 1) {
        try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
          workbook.write(fileOut);
        }
        workbook.close();

        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject(IMPORT_SUBJECT);
        helper.setText("Your import has been completed. Please check the attached file for errors.");
        helper.addAttachment("CustomerAttachment.xlsx", new File(EXCEL_FILENAME));
        emailSender.send(message);
        importHistoryDTO.setMessage("We have sent import result via email");
      } else {
        importHistoryDTO.setMessage("Import completed successfully");
      }

      importHistoryDTO.setStatus("Completed");

    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      log.error("Import update failed", e);
    }

    customerService.addImportHistory(importHistoryDTO);
    log.info("Import History {}", importHistoryDTO);
  }

  // ─── Export ───────────────────────────────────────────────────────────────

  @GetMapping("/export-company-customer/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<byte[]> exportCompanyCustomers(@PathVariable Long companyId) throws IOException {
    List<CompanyCustomer> customers = companyCustomerRepository.findByCompanyId(companyId);
    List<CompanyCustomerExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Customers");
    Row header = sheet.createRow(0);
    int col = 0;

    // Create text format style
    CellStyle textStyle = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    textStyle.setDataFormat(format.getFormat("@")); // @ = text format

    // Add default headers
    String[] defaultHeaders = {"ID", "Name","Address", "Email","Category", "Phone", "Status"};
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

      // ID - apply text style
      Cell idCell = row.createCell(c++);
      idCell.setCellValue(String.valueOf(customer.getCompanyCustomerId()));
      idCell.setCellStyle(textStyle);

      // Name
      row.createCell(c++).setCellValue(customer.getName());

      // Address
      StringBuilder sb = new StringBuilder();
      if (customer.getAddress() != null && !customer.getAddress().isEmpty())
        sb.append(customer.getAddress());
      if (customer.getCity() != null && !customer.getCity().isEmpty()) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(customer.getCity());
      }
      if (customer.getState() != null && !customer.getState().isEmpty()) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(customer.getState());
      }
      if (customer.getCountry() != null && !customer.getCountry().isEmpty()) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(customer.getCountry());
      }
      if (customer.getZipCode() != null && !customer.getZipCode().isEmpty()) {
        sb.append(" ").append(customer.getZipCode());
      }
      row.createCell(c++).setCellValue(sb.toString());

      // Email
      row.createCell(c++).setCellValue(customer.getEmail());

      // Category
      row.createCell(c++).setCellValue(customer.getCategory());

      // Phone - apply text style to preserve leading zeros
      Cell phoneCell = row.createCell(c++);
      phoneCell.setCellValue(customer.getPhone());
      phoneCell.setCellStyle(textStyle);

      // Status
      String status = customer.getStatus();
      if (status != null && !status.isEmpty()) {
        status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
      } else {
        status = "";
      }
      row.createCell(c++).setCellValue(status);

      // Fetch custom fields
      List<CompanyCustomerExtraFields> extras = extraFieldsRepository.findByCompanyCustomerId(customer.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (CompanyCustomerExtraFields ef : extras) {
        extraMap.put(ef.getName(), ef.getValue());
      }
      for (CompanyCustomerExtraFieldName extraField : extraFieldNames) {
        Cell extraCell = row.createCell(c++);
        extraCell.setCellValue(extraMap.getOrDefault(extraField.getName(), ""));
        extraCell.setCellStyle(textStyle); // Apply text style to all extra fields
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

  // ─── State List (public - no auth needed) ────────────────────────────────

  @GetMapping(value = "/statelist")
  public ResponseEntity<List<String>> statelist() {
    return ResponseEntity.ok(US_STATES);
  }

  // ─── Utility ──────────────────────────────────────────────────────────────

  private CellStyle createErrorCellStyle(Workbook workbook) {
    CellStyle errorStyle = workbook.createCellStyle();
    errorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
    errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return errorStyle;
  }
}

