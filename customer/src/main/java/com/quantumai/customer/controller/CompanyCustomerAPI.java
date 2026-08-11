package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


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
import com.quantumai.customer.entity.enums.AuditAction;
import com.quantumai.customer.entity.enums.AuditModule;
import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import com.quantumai.customer.service.AuditService;
import com.quantumai.customer.service.AuditChangeCalculator;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.service.CompanyCustomerService;
import com.quantumai.customer.service.CustomerService;
import com.quantumai.customer.util.CustomerImportUtils;
import com.quantumai.customer.util.PhoneUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.math.BigDecimal;
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
@Tag(name = "CompanyCustomer", description = "CompanyCustomer Management API")
public class CompanyCustomerAPI {

  @Autowired private CompanyCustomerService companyCustomerService;
  @Autowired private CustomerService customerService;
  @Autowired private CompanyCustomerRepository companyCustomerRepository;
  @Autowired private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;
  @Autowired private CompanyCustomerExtraFieldsRepository extraFieldsRepository;
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;
  @Autowired private CompanyCustomerMandatoryFieldsRepository mandatoryFieldsRepository;
  @Autowired private CompanyCustomerShowFieldsRepository companyCustomerShowFieldsRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private JavaMailSender emailSender;
  @Autowired private UsersRepository usersRepository;
  @Autowired private ImportHistoryRepository importHistoryRepository;
  @Autowired private CustomerImportColumnMappingRepository customerImportColumnMappingRepository;
  @Autowired private AuditService auditService;
  @Autowired private CompanyCustomerExtraFieldNameRepository companyCustomerExtraFieldNameRepository;
  @Autowired private CompanyCustomerFileRepository companyCustomerFileRepository;

  private static final String DEFAULT_COUNTRY_CODE = "1"; // India

  private final ModelMapper modelMapper = new ModelMapper();



  private static final List<String> countryList = List.of(
    "Canada",
    "Mexico",
    "United States of America",
    "Antigua and Barbuda",
    "The Bahamas",
    "Barbados",
    "Cuba",
    "Dominica",
    "Dominican Republic",
    "Grenada",
    "Haiti",
    "Jamaica",
    "Saint Kitts and Nevis",
    "Saint Lucia",
    "Saint Vincent and the Grenadines",
    "Trinidad and Tobago",
    "Belize",
    "Costa Rica",
    "El Salvador",
    "Guatemala",
    "Honduras",
    "Nicaragua",
    "Panama"
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

  @Operation(summary = "Working", description = "Endpoint to working")
  @GetMapping("/working")
  public String working() {
    System.out.println("working!!!");
    return "Working!!";
  }

  // ─── Customer CRUD ────────────────────────────────────────────────────────

  @Operation(summary = "Get Company Customer List", description = "Endpoint to get company customer list")
  @GetMapping("/allCompanyCustomer/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerDTO> getCompanyCustomerList(@PathVariable Long companyId) {
    return companyCustomerService.getAllCustomer(companyId);
  }

  @Operation(summary = "Get Active Company Customer List", description = "Endpoint to get company customer list")
  @GetMapping("/allActiveCompanyCustomer/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerDTO> getActiveCompanyCustomerList(@PathVariable Long companyId) {
    return companyCustomerService.getActiveAllCustomer(companyId);
  }


  @Operation(summary = "Get Company Customer", description = "Endpoint to get company customer")
  @GetMapping("/getCompanyCustomer/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public CompanyCustomerDTO getCompanyCustomer(@PathVariable String id) {
    return companyCustomerService.getCustomer(id);
  }

  @Operation(summary = "Get Company Customer By Local Id", description = "Endpoint to get company customer by local id")
  @GetMapping("/getCompanyCustomerByLocalId/{id}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerDTO getCompanyCustomerByLocalId(
          @PathVariable String id, @PathVariable Long companyId) {
    return companyCustomerService.getCompanyCustomerByLocalId(Integer.valueOf(id), companyId);
  }

//  @GetMapping("/getCompanyCustomerByLocalId/{id}/{companyId}")
//  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
//  public CompanyCustomerDTO getCompanyCustomerByName(
//          @PathVariable String name, @PathVariable Long companyId) {
//    return companyCustomerService.getCompanyCustomerByName(name, companyId);
//  }

  @Operation(summary = "Add New Fields", description = "Endpoint to add new fields")
  @PostMapping("/addCompanyCustomer")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public CompanyCustomerDTO addNewFields(
          @RequestBody CompanyCustomerDTO companyCustomerDTO,
          @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException {
    CompanyCustomerDTO saved = companyCustomerService.addCustomer(companyCustomerDTO);
    auditService.logCreate(AuditModule.CUSTOMER,
            String.valueOf(saved.getCompanyCustomerId()),
            saved.getName(), companyId,
            Map.of("companyCustomerId", String.valueOf(saved.getCompanyCustomerId()),
                    "name", String.valueOf(saved.getName()),
                    "email", String.valueOf(saved.getEmail())));
    return saved;
  }

  @Operation(summary = "Update Company Customer", description = "Endpoint to update company customer")
  @PutMapping("/updateCompanyCustomer")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void updateCompanyCustomer(
          @RequestBody CompanyCustomerDTO companyCustomerDTO,
          @RequestHeader Long companyId)
          throws NoSubscriptionError, EmailAlreadyExistsException {
    // Fetch BEFORE state first — must happen BEFORE the update
    Optional<CompanyCustomer> beforeStateOpt = companyCustomerRepository.findById(companyCustomerDTO.getId());

    companyCustomerService.updateCustomer(companyCustomerDTO);

    // Fetch AFTER state — now get the updated version
    if (beforeStateOpt.isPresent()) {
      CompanyCustomer afterState = companyCustomerRepository.findById(companyCustomerDTO.getId()).orElse(null);
      if (afterState != null) {
      auditService.logUpdateWithComparison(AuditModule.CUSTOMER,
              String.valueOf(afterState.getCompanyCustomerId()),
              afterState.getName(), companyId,
              beforeStateOpt.get(), afterState);
      }
    }
  }

  @Operation(summary = "Delete Company Customer", description = "Endpoint to delete company customer")
  @DeleteMapping("/deleteCompanyCustomer/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteCompanyCustomer(
          @PathVariable String id,
          @RequestHeader Long companyId)
          throws NoSubscriptionError {
    companyCustomerRepository.findById(id).ifPresent(cc ->
            auditService.logDelete(AuditModule.CUSTOMER,
                    String.valueOf(cc.getCompanyCustomerId()), cc.getName(), companyId,
                    Map.of("companyCustomerId", String.valueOf(cc.getCompanyCustomerId()),
                            "name", String.valueOf(cc.getName()))));
    companyCustomerService.deleteCustomer(id);
  }

  // ─── Search & Sort ────────────────────────────────────────────────────────

  @Operation(summary = "Search Company Customer", description = "Endpoint to search company customer")
  @GetMapping(value = "/searchCompanyCustomerlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<String> searchCompanyCustomer(
          @PathVariable Long companyId,
          @RequestParam(name = "data", required = true) String search,
          @RequestParam(name = "category", required = true) String category) {
    return companyCustomerService.searchedCompanyCustomer(companyId, search, category);
  }

  @Operation(summary = "Sort Company Customer", description = "Endpoint to sort company customer")
  @GetMapping(value = "/sortCompanyCustomerlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerDTO> sortCompanyCustomer(
          @PathVariable Long companyId,
          @RequestParam(name = "sortField", required = false, defaultValue = "companyCustomerId") String sortField,
          @RequestParam(name = "sortDirection", required = false, defaultValue = "ASC") String sortDirection) {
    return companyCustomerService.sortCompanyCustomer(companyId, sortField, sortDirection);
  }

  // ─── Extra Fields ─────────────────────────────────────────────────────────

  @Operation(summary = "Add Extra Field Name", description = "Endpoint to add extra field name")
  @PostMapping("/addExtraFieldName")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addExtraFieldName(
          @RequestBody CompanyCustomerExtraFieldNameDTO extraFieldNameDTO,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.addCompanyCustomerExtraField(extraFieldNameDTO);
    auditService.logCreate(AuditModule.CUSTOMER_CUSTOM_FIELD,
            extraFieldNameDTO.getId(), extraFieldNameDTO.getName(), companyId,
            Map.of("name", String.valueOf(extraFieldNameDTO.getName()),
                    "type", String.valueOf(extraFieldNameDTO.getType())));
  }

  @Operation(summary = "Get Extra Field Name", description = "Endpoint to get extra field name")
  @GetMapping("/getExtraFieldName/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    return companyCustomerService.getCompanyCustomerExtraField(companyId);
  }

  @Operation(summary = "Delete Extra Field Name", description = "Endpoint to delete extra field name")
  @DeleteMapping("/deleteExtraFieldName/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteExtraFieldName(
          @PathVariable String id,
          @RequestHeader Long companyId) throws Exception {
    auditService.logDelete(AuditModule.CUSTOMER_CUSTOM_FIELD, id, id, companyId,
            Map.of("id", id));
    companyCustomerService.deleteCompanyCustomerExtraField(id);
  }

  @Operation(summary = "Add New Fields", description = "Endpoint to add new fields")
  @PostMapping("/addfields")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addNewFields(
          @RequestBody CompanyCustomerExtraFieldsDTO extraFieldsDTO,
          @RequestHeader Long companyId) throws Exception {
    String oldValue = resolveCompanyCustomerExtraFieldOldValue(extraFieldsDTO);
    companyCustomerService.addExtraFields(extraFieldsDTO);
    auditCompanyCustomerExtraFieldValueChange(extraFieldsDTO, oldValue, companyId);
  }

  @Operation(summary = "Get Extra Fields", description = "Endpoint to get extra fields")
  @GetMapping("/getExtraFields/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return companyCustomerService.getExtraFields(id);
  }

  @Operation(summary = "Get Count of Category", description = "Endpoint to get count of category in customer")
  @GetMapping("/countCompanyCustomerByCategory/{categoryName}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public int countCompanyCustomerByCategory(@PathVariable String categoryName) {
    return companyCustomerService.countCompanyCustomerByCategory(categoryName);
  }

  @Operation(summary = "Delete Extra Field", description = "Endpoint to delete extra field")
  @DeleteMapping("/deleteExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'customers')")
  public void deleteExtraField(@PathVariable String id, Long companyId) throws Exception {
    Optional<CompanyCustomerExtraFields> fieldOpt = extraFieldsRepository.findById(id);
    if (fieldOpt.isEmpty()) {
      companyCustomerService.deleteExtraFields(id);
      return;
    }
    CompanyCustomerExtraFields field = fieldOpt.get();
    String oldValue = field.getValue();
    String fieldName = field.getName();
    String companyCustomerMongoId = field.getCompanyCustomerId();
    Long fieldCompanyId = field.getCompanyId();
    companyCustomerService.deleteExtraFields(id);
    companyCustomerRepository.findById(companyCustomerMongoId).ifPresent(customer -> {
      if (fieldName != null) {
        auditService.logUpdate(AuditModule.CUSTOMER,
                String.valueOf(customer.getCompanyCustomerId()), customer.getName(), fieldCompanyId,
                Map.of(fieldName, Map.of("old", oldValue != null ? oldValue : "", "new", "")));
      }
    });
  }

  @Operation(summary = "Delete Company Customer Extra Fields", description = "Endpoint to delete company customer extra fields")
  @DeleteMapping("/deleteCompanyCustomerExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteCompanyCustomerExtraFields(
          @PathVariable String id,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.deleteExtraFieldByCompanyCustomer(id);
  }

  @Operation(summary = "Get Extra Field Name Value", description = "Endpoint to get extra field name value")
  @GetMapping("/getExtraFieldNameValue/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return companyCustomerService.getextraFieldList(companyId);
  }

  @Operation(summary = "Update Extra Field Name", description = "Endpoint to update extra field name")
  @PutMapping("/extraFieldName")
//  @PreAuthorize("@appSecurity.canEdit(authentication, #extraFieldNameUpdateDTO.companyId, 'customers')")
  public ResponseEntity<CompanyCustomerExtraFieldName> updateExtraFieldName(
          @RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
    CompanyCustomerExtraFieldName before = companyCustomerExtraFieldNameRepository.findById(extraFieldNameUpdateDTO.getId()).orElse(null);
    CompanyCustomerExtraFieldName result = companyCustomerService.updateExtraFieldName(extraFieldNameUpdateDTO);
    if (before != null) {
      auditService.logUpdateWithComparison(AuditModule.CUSTOMER_CUSTOM_FIELD,
              result.getId(), result.getName(), result.getCompanyId(), before, result);
    }
    return ResponseEntity.ok(result);
  }

  // ─── Mandatory & Show Fields ──────────────────────────────────────────────

  @Operation(summary = "Mandatory Fields", description = "Endpoint to mandatory fields")
  @PostMapping("/mandatoryFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void mandatoryFields(
          @RequestBody CompanyCustomerMandatoryFields mandatoryFields,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    CompanyCustomerMandatoryFields beforeState = mandatoryFieldsRepository
            .findByNameAndCompanyId(mandatoryFields.getName(), companyId)
            .orElse(null);
    companyCustomerService.updateMandatoryFields(mandatoryFields);
    CompanyCustomerMandatoryFields afterState = mandatoryFieldsRepository
            .findByNameAndCompanyId(mandatoryFields.getName(), companyId)
            .orElse(mandatoryFields);
    if (beforeState != null) {
      Map<String, Object> changes = AuditChangeCalculator.computeMandatoryShowChanges(beforeState, afterState, "mandatory");
      if (!changes.isEmpty()) {
        auditService.logUpdate(AuditModule.CUSTOMER_CUSTOM_FIELD, afterState.getId(), afterState.getName(),
                companyId, changes);
      }
    } else {
      auditService.logCreate(AuditModule.CUSTOMER_CUSTOM_FIELD, afterState.getId(), afterState.getName(), companyId,
              Map.of("name", afterState.getName(), "mandatory", String.valueOf(afterState.isMandatory())));
    }
  }

  @Operation(summary = "Show Fields", description = "Endpoint to show fields")
  @PostMapping("/showFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void showFields(
          @RequestBody CompanyCustomerShowFields showFields,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    CompanyCustomerShowFields beforeState = companyCustomerShowFieldsRepository
            .findByNameAndCompanyId(showFields.getName(), companyId)
            .orElse(null);
    companyCustomerService.updateShowFields(showFields);
    CompanyCustomerShowFields afterState = companyCustomerShowFieldsRepository
            .findByNameAndCompanyId(showFields.getName(), companyId)
            .orElse(showFields);
    if (beforeState != null) {
      Map<String, Object> changes = AuditChangeCalculator.computeMandatoryShowChanges(beforeState, afterState, "show");
      if (!changes.isEmpty()) {
        auditService.logUpdate(AuditModule.CUSTOMER_CUSTOM_FIELD, afterState.getId(), afterState.getName(),
                companyId, changes);
      }
    } else {
      auditService.logCreate(AuditModule.CUSTOMER_CUSTOM_FIELD, afterState.getId(), afterState.getName(), companyId,
              Map.of("name", afterState.getName(), "show", String.valueOf(afterState.isShow())));
    }
  }

  @Operation(summary = "Get Mandatory Fields", description = "Endpoint to get mandatory fields")
  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<CompanyCustomerMandatoryFields> getMandatoryFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getMandatoryFields(name, companyId));
  }

  @Operation(summary = "Get Show Fields", description = "Endpoint to get show fields")
  @GetMapping("/getShowFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<CompanyCustomerShowFields> getShowFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getShowFields(name, companyId));
  }

  @Operation(summary = "Get All Mandatory Fields", description = "Endpoint to get all mandatory fields")
  @GetMapping("/getAllMandatoryFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<List<CompanyCustomerMandatoryFields>> getAllMandatoryFields(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getAllMandatoryFields(companyId));
  }

  @Operation(summary = "Get All Show Fields", description = "Endpoint to get all show fields")
  @GetMapping("/getAllShowFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<List<CompanyCustomerShowFields>> getAllShowFields(
          @PathVariable Long companyId) {
    return ResponseEntity.ok(companyCustomerService.getAllShowFields(companyId));
  }

  @Operation(summary = "Delete Show And Mandatory Field", description = "Endpoint to delete show and mandatory field")
  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteShowAndMandatoryField(
          @PathVariable String name,
          @PathVariable Long companyId) throws NoSubscriptionError {
    companyCustomerService.deleteShowAndMandatoryFields(companyId, name);
  }

  // ─── All Customers with Extra Fields ─────────────────────────────────────

  @Operation(summary = "All Company Customer With Extra Fields", description = "Endpoint to all company customer with extra fields")
  @GetMapping("/allCompanyCustomerWithExtraFields/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #id, 'customers')")
  public List<String> allCompanyCustomerWithExtraFields(@PathVariable Long id) {
    return companyCustomerService.getAllCustomerWithExtraColumns(id);
  }

  // ─── File Management ──────────────────────────────────────────────────────

  @Operation(summary = "Add Company Customer File", description = "Endpoint to add company customer file")
  @PostMapping("/addFile/{companyCustomerId}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public ResponseEntity<ResponseMessageDTO> addCompanyCustomerFile(
          @RequestParam("file") MultipartFile file,
          @PathVariable String companyCustomerId,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    try {
      CompanyCustomerFile savedFile = companyCustomerService.addCompanyCustomerFile(file, companyCustomerId);
      companyCustomerRepository.findById(companyCustomerId).ifPresent(customer ->
              auditService.log(AuditModule.CUSTOMER, AuditAction.CREATE,
                      String.valueOf(customer.getCompanyCustomerId()), customer.getName(), companyId,
                      "Uploaded file: " + file.getOriginalFilename(),
                      Map.of("fileName", file.getOriginalFilename(),
                              "fileId", savedFile.getId(),
                              "action", "file_upload")));
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Uploaded the file successfully: " + file.getOriginalFilename());
      return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    } catch (IOException e) {
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Could not upload the file: " + file.getOriginalFilename() + "!");
      return new ResponseEntity<>(response, HttpStatus.EXPECTATION_FAILED);
    }
  }

  @Operation(summary = "Get Company Customer File", description = "Endpoint to get company customer file")
  @GetMapping("/getFile/{companyCustomerId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public List<CompanyCustomerFileDTO> getCompanyCustomerFile(@PathVariable String companyCustomerId) {
    return companyCustomerService.getCompanyCustomerFile(companyCustomerId);
  }

  @Operation(summary = "Get Company Customer File Paginated", description = "Endpoint to get company customer files with pagination")
  @GetMapping("/getFile/{companyCustomerId}/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public PaginatedResultDTO<CompanyCustomerFileDTO> getCompanyCustomerFilePaginated(
          @PathVariable String companyCustomerId,
          @PathVariable Integer pageNumber,
          @PathVariable Integer pageSize) {
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 10;
    return companyCustomerService.getCompanyCustomerFilePaginated(companyCustomerId, pageNumber, pageSize);
  }

  @Operation(summary = "Download File", description = "Endpoint to download file")
  @GetMapping("/getFile/download/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'customers')")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    CompanyCustomerFileDTO companyCustomerFileDTO = companyCustomerService.downloadFile(id);
    return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.valueOf("json/object"))
            .body(companyCustomerFileDTO.getFile());
  }

  @Operation(summary = "Delete File", description = "Endpoint to delete file")
  @DeleteMapping("deleteFile/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteFile(
          @PathVariable String id,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    companyCustomerFileRepository.findById(id).ifPresent(file -> {
      companyCustomerRepository.findById(file.getCompanyCustomerId()).ifPresent(customer ->
              auditService.logDelete(AuditModule.CUSTOMER,
                      String.valueOf(customer.getCompanyCustomerId()), customer.getName(), companyId,
                      Map.of("fileName", file.getFileName(),
                              "fileId", file.getId(),
                              "action", "file_delete")));
    });
    companyCustomerService.deleteFile(id);
  }

  // ─── Advance Filter ───────────────────────────────────────────────────────

  @Operation(summary = "Advance Filter", description = "Endpoint to advance filter")
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

  @Operation(summary = "Add Category", description = "Endpoint to add category")
  @PostMapping(value = "/addCategory")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void addCategory(
          @RequestBody CategoryDTO categoryDTO,
          @RequestHeader Long companyId) throws Exception {
    companyCustomerService.addCategory(categoryDTO);
    companyCustomerCategoryRepository.findByNameAndCompanyId(categoryDTO.getName(), companyId)
            .ifPresent(cat -> auditService.logCreate(AuditModule.CUSTOMER_CATEGORY,
                    String.valueOf(cat.getCompanyCustomerCategoryId()), cat.getName(), companyId,
                    Map.of("categoryId", String.valueOf(cat.getCompanyCustomerCategoryId()),
                            "name", cat.getName(),
                            "status", cat.getStatus() != null ? cat.getStatus() : "")));
  }

  @Operation(summary = "Update Category", description = "Endpoint to update category")
  @PutMapping(value = "/updateCategory")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void updateCategory(
          @RequestBody CategoryDTO categoryDTO,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    Optional<CompanyCustomerCategory> beforeStateOpt =
            companyCustomerCategoryRepository.findById(categoryDTO.getId());
    companyCustomerService.updateCategory(categoryDTO);
    if (beforeStateOpt.isPresent()) {
      CompanyCustomerCategory afterState =
              companyCustomerCategoryRepository.findById(categoryDTO.getId()).orElse(null);
      if (afterState != null) {
        auditService.logUpdateWithComparison(AuditModule.CUSTOMER_CATEGORY,
                String.valueOf(afterState.getCompanyCustomerCategoryId()), afterState.getName(),
                companyId, beforeStateOpt.get(), afterState);
      }
    }
  }

  @Operation(summary = "Delete Category", description = "Endpoint to delete category")
  @DeleteMapping(value = "/deleteCategory/{id}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'customers')")
  public void deleteCategory(
          @PathVariable String id,
          @RequestHeader Long companyId) throws NoSubscriptionError {
    companyCustomerCategoryRepository.findById(id).ifPresent(cat ->
            auditService.logDelete(AuditModule.CUSTOMER_CATEGORY,
                    String.valueOf(cat.getCompanyCustomerCategoryId()), cat.getName(),
                    companyId,
                    Map.of("categoryId", String.valueOf(cat.getCompanyCustomerCategoryId()),
                            "name", cat.getName())));
    companyCustomerService.deleteCategory(id);
  }

  @Operation(summary = "Get Category List", description = "Endpoint to get category list")
  @GetMapping(value = "/getCategoryList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerCategory> getCategoryList(@PathVariable Long companyId) {
    return companyCustomerService.getCategoryList(companyId);
  }

  @Operation(summary = "Get Category Active List", description = "Endpoint to get category active list")
  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public List<CompanyCustomerCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return companyCustomerService.getActiveCategoryList(companyId);
  }

  @Operation(summary = "Get Category By Id", description = "Endpoint to get category by id")
  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerCategory getCategoryById(
          @PathVariable Long companyId, @PathVariable String id) {
    return companyCustomerService.getCategoryListById(companyId, id);
  }

  // ─── Template / Download ─────────────────────────────────────────────────

  @Operation(summary = "Get Template Fields", description = "Endpoint to get template fields")
  @GetMapping("/template-fields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public CompanyCustomerTemplateFieldsDTO getTemplateFields(@PathVariable Long companyId) {
    return companyCustomerService.getTemplateFields(companyId);
  }

  @Operation(summary = "Download Template", description = "Endpoint to download template")
  @GetMapping(value = "/template-download/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'customers')")
  public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long companyId) {
    try {
      // Generate CSV template
      byte[] data = companyCustomerService.generateCompanyCustomerTemplateCsv(companyId);
      return ResponseEntity.ok()
              .contentType(MediaType.parseMediaType("text/csv"))
              .header("Content-Disposition", "attachment; filename=CompanyCustomerTemplate.csv")
              .body(data);
    } catch (IOException e) {
      log.error("Error generating template", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @Operation(summary = "Save Import Column Mapping", description = "Persist customer import column mapping for reuse")
  @PostMapping("/importMapping/{companyId}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'imports')")
  public CustomerImportColumnMapping saveImportMapping(
          @PathVariable Long companyId,
          @RequestBody CustomerImportColumnMapping mapping) {
    mapping.setCompanyId(companyId);
    mapping.setUpdatedAt(LocalDateTime.now());
    if (mapping.getCreatedAt() == null) {
      mapping.setCreatedAt(LocalDateTime.now());
    }
    if (mapping.getId() != null) {
      return customerImportColumnMappingRepository.findByIdAndCompanyId(mapping.getId(), companyId)
              .map(existing -> {
                existing.setName(mapping.getName());
                existing.setRecordType(mapping.getRecordType());
                existing.setColumnMappings(mapping.getColumnMappings());
                existing.setUpdatedAt(LocalDateTime.now());
                return customerImportColumnMappingRepository.save(existing);
              })
              .orElseGet(() -> customerImportColumnMappingRepository.save(mapping));
    }
    return customerImportColumnMappingRepository.save(mapping);
  }

  @Operation(summary = "Get Import Column Mappings", description = "List saved customer import column mappings")
  @GetMapping("/importMappings/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'imports')")
  public List<CustomerImportColumnMapping> getImportMappings(
          @PathVariable Long companyId,
          @RequestParam ImportHistoryRecordType recordType) {
    return customerImportColumnMappingRepository.findByCompanyIdAndRecordTypeOrderByUpdatedAtDesc(
            companyId, recordType);
  }

  @Operation(summary = "Delete Import Column Mapping", description = "Delete a saved customer import column mapping")
  @DeleteMapping("/importMapping/{companyId}/{mappingId}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'imports')")
  public void deleteImportMapping(
          @PathVariable Long companyId, @PathVariable String mappingId) {
    customerImportColumnMappingRepository
            .findByIdAndCompanyId(mappingId, companyId)
            .ifPresent(customerImportColumnMappingRepository::delete);
  }

  // ─── Import ───────────────────────────────────────────────────────────────

  @Operation(summary = "Import File", description = "Endpoint to import file")
  @PostMapping("/import/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'customers')")
  public void importFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, MessagingException, ImportFileRowException,
          NoSubscriptionError, EmailAlreadyExistsException, NameColumnMissingException, ImportInProgressException {


    boolean isInProgress = importHistoryRepository
            .findTopByCompanyIdAndStatusAndRecordTypeOrderByDateDesc(companyId, "In-Progress", ImportHistoryRecordType.ADDCUSTOMER)
            .map(h -> h.getDate().isAfter(LocalDateTime.now().minusMinutes(30))) // stale-lock guard
            .orElse(false);

    if (isInProgress) {
      throw new ImportInProgressException(
              "Only one import can be processed at a time. Please wait for the current import to complete before starting another. Check Import History for status and details.Only one import can be processed at a time. Please wait for the current import to complete before starting another. Check Import History for status and details.."
      );
    }

    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType(ImportHistoryRecordType.ADDCUSTOMER);
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);
    System.out.println("===========>");
    System.out.println(importHistoryDTO);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setComplete(0L);
    importHistoryDTO = customerService.addImportHistory(importHistoryDTO);

//    System.out.println("------||---------------------------------------/////////////////////////////////////------->"+columnMappings);
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
      if (totalCount > MAX_IMPORT_ROWS ) {
        throw new ImportFileRowException("Import File cannot import more than " + MAX_IMPORT_ROWS + " rows");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }


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

        CompanyCustomerDTO companyCustomerDTO = new CompanyCustomerDTO();
        companyCustomerDTO.setCompanyId(companyId);
        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        Map<Integer, Boolean> errorCellMap = new HashMap<>();

        for (int j = 0; j < headers.length; j++) {
          String field = headerMap.get(j);
          String cellValue = CustomerImportUtils.getImportCellValue(row, j);

          System.out.println(field + "------->" + columnMap.get(field));
          if (columnMap.get(field) != null) {
            switch (columnMap.get(field).toLowerCase()) {
              case "name":
                System.out.println("name//->" + cellValue);
                if (CustomerImportUtils.isBlank(cellValue)) {
                  errorDesc.append("ERROR WITH NO NAME WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                companyCustomerDTO.setName(cellValue);
                break;

              case "phone":
                log.info("Phone to be set: {}", cellValue);
                if (mandatoryFieldsMap.containsKey("phone")) {
                  log.info("Phone before inside empty: {}", cellValue);
                  if (CustomerImportUtils.isBlank(cellValue)) {
                    log.info("Phone inside empty: {}", cellValue);
                    errorDesc.append("ERROR WITH PHONE MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                }

                if (!CustomerImportUtils.isBlank(cellValue)) {
                  if (!PhoneUtils.isValidImportPhone(cellValue)) {
                    errorDesc.append("INVALID PHONE NUMBER: Must be 3-15 characters, no letters allowed. Example: +11876543210");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                  String formattedPhone = PhoneUtils.formatToUSStyle(cellValue);
                  companyCustomerDTO.setPhone(formattedPhone);

                }
                break;

              case "category":
                System.out.println("category//->" + cellValue);
                List<CompanyCustomerCategory> categoryList=companyCustomerCategoryRepository.findByCompanyId(companyId);
                if(!CustomerImportUtils.isBlank(cellValue)){
                  List<CompanyCustomerCategory> list=categoryList.stream().filter(x-> x.getName().equalsIgnoreCase(cellValue)).toList();
                  if(list.isEmpty()){
                    errorDesc.append("ERROR IN CATEGORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                  else{
                    if(mandatoryFieldsMap.containsKey("category") && CustomerImportUtils.isBlank(cellValue)){
                      errorDesc.append("ERROR WITH CATEGORY MANDATORY WHILE ADDING IN CUSTOMER");
                      errorFlag = 1;
                      errorCellMap.put(j + 1, true);
                      break;
                    }
                    companyCustomerDTO.setCategory(list.get(0).getName());
                  }
                }
                break;

              case "email":
                if (StringUtils.isBlank(cellValue)) {
                  companyCustomerDTO.setEmail("");
                } else {
                  Optional<CompanyCustomer> myCustomer = companyCustomerRepository
                          .findByEmailAndCompanyId(cellValue, companyCustomerDTO.getCompanyId());
                  if (myCustomer.isPresent()) {
                    errorFlag = 1;
                    errorDesc.append("Email already exists. ");
                    log.info("Email already: {}", cellValue);
                    errorCellMap.put(j + 1, true);
                  } else {
                    log.info("Email to be set: {}", cellValue);
                    if(mandatoryFieldsMap.containsKey("email") && CustomerImportUtils.isBlank(cellValue)){
                      errorDesc.append("ERROR WITH EMAIL MANDATORY WHILE ADDING IN CUSTOMER");
                      errorFlag = 1;
                      errorCellMap.put(j + 1, true);
                      break;
                    }
                    companyCustomerDTO.setEmail(cellValue);
                  }
                }
                break;

              case "address":
                System.out.println("address//->" + cellValue);
                if(mandatoryFieldsMap.containsKey("address") && CustomerImportUtils.isBlank(cellValue)){
                  errorDesc.append("ERROR WITH ADDRESS MANDATORY WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                companyCustomerDTO.setAddress(cellValue);
                break;

              case "city":
                companyCustomerDTO.setCity(cellValue);
                if(mandatoryFieldsMap.containsKey("city") && CustomerImportUtils.isBlank(cellValue)){
                  errorDesc.append("ERROR WITH CITY MANDATORY WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                break;

              case "state":
                if (CustomerImportUtils.isBlank(cellValue)) {
                  if (mandatoryFieldsMap.containsKey("state")) {
                    errorDesc.append("ERROR WITH STATE MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                  break;
                }
                Optional<Map.Entry<String, String>> matchedState =
                        findStateInCountryStatesData(cellValue);
                if (matchedState.isEmpty()) {
                  errorDesc.append("ERROR WHILE ADDING IN STATE");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                } else {
                  companyCustomerDTO.setState(matchedState.get().getValue());
                  if (CustomerImportUtils.isBlank(companyCustomerDTO.getCountry())) {
                    companyCustomerDTO.setCountry(matchedState.get().getKey());
                  }
                }
                break;

              case "country":
                if (mandatoryFieldsMap.containsKey("country") && CustomerImportUtils.isBlank(cellValue)) {
                  errorDesc.append("ERROR WITH COUNTRY MANDATORY WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                if (!CustomerImportUtils.isBlank(cellValue)
                        && !CustomerImportUtils.isValidCountry(cellValue, countryList)) {
                  errorDesc.append("ERROR WITH COUNTRY INVALID WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                } else if (!CustomerImportUtils.isBlank(cellValue)) {
                  companyCustomerDTO.setCountry(CustomerImportUtils.normalizeCountryName(cellValue));
                }
                break;

              case "zip code":
              case "zipcode":
                System.out.println("zipCode//->" + cellValue);
                if (CustomerImportUtils.isBlank(cellValue)) {
                  if (mandatoryFieldsMap.containsKey("zipcode")) {
                    errorDesc.append("ERROR WITH ZIPCODE MANDATORY WHILE ADDING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                  }
                  break;
                }

                String zipValue = CustomerImportUtils.normalizeZipValue(cellValue);
                System.out.println("zipCode//-> " + zipValue + " " + zipValue.length());

                if (!CustomerImportUtils.isValidOptionalZip(zipValue, mandatoryFieldsMap.containsKey("zipcode"))) {
                  log.info("ERROR IN ZIPCODE FORMAT {}", zipValue);
                  errorDesc.append("ERROR IN ZIPCODE FORMAT");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                } else {
                  companyCustomerDTO.setZipCode(zipValue);
                }

                break;

              case "status":
                if(mandatoryFieldsMap.containsKey("status") && CustomerImportUtils.isBlank(cellValue)){
                  errorDesc.append("ERROR WITH STATUS MANDATORY WHILE ADDING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                else {
                  if(CustomerImportUtils.isBlank(cellValue)){
                    companyCustomerDTO.setStatus("active");
                    break;
                  }
                  if ((cellValue.equalsIgnoreCase("active"))
                          || (cellValue.equalsIgnoreCase("inactive"))) {

                    if (cellValue.equalsIgnoreCase("active")) {
                      companyCustomerDTO.setStatus("active");
                    } else {
                      companyCustomerDTO.setStatus("inActive");
                    }

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

        if (CustomerImportUtils.isBlank(companyCustomerDTO.getName())) {
          errorFlag = 1;
          if (errorDesc.length() == 0) {
            errorDesc.append("ERROR WITH NO NAME WHILE ADDING IN CUSTOMER");
          }
          if (!CustomerImportUtils.isNameMapped(columnMap.values())) {
            if (errorDesc.length() > 0) {
              errorDesc.append(". ");
            }
            errorDesc.append("Name column is not mapped.");
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

        if (errorFlag == 0
            && (companyCustomerDTO.getStatus() == null || companyCustomerDTO.getStatus().isBlank())
            && !CustomerImportUtils.isStatusMapped(columnMap.values())) {
          companyCustomerDTO.setStatus("active");
        }

        if (errorFlag == 0) {
          List<CompanyCustomerExtraFieldName> listExtraFieldName =
                  extraFieldNameRepository.findByCompanyId(companyId);
          Map<String, String> extraFieldValues = new HashMap<>();
          int[] errorFlagHolder = new int[] {errorFlag};
          collectImportExtraFields(row, headerMap, columnMap, mandatoryFieldsMap, listExtraFieldName,
                  errorDesc, errorFlagHolder, errorCellMap, extraFieldValues, "ADDING");
          errorFlag = errorFlagHolder[0];

          if (errorFlag == 0) {
            CompanyCustomerDTO mynewCustomer = companyCustomerService.addCustomer(companyCustomerDTO);
            saveImportExtraFields(mynewCustomer.getId(), companyId, extraFieldValues, listExtraFieldName);
          }
        }

        if (errorFlag == 1) {
          writeImportErrorRow(sheet, excelIndex++, ind, headers, row, errorDesc.toString(), errorCellMap, errorCellStyle);
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
        importHistoryDTO.setHasErrorReport(true);
        importHistoryDTO.setErrorReportFileName("CustomerImportErrors.xlsx");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          workbook.write(baos);
          importHistoryDTO.setErrorReportFile(baos.toByteArray());
          try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
            workbook.write(fileOut);
          }
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

  @Operation(summary = "Import Updation", description = "Endpoint to import updation")
  @PostMapping("/importUpdation/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'customers')")
  public void importUpdation(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, JsonParseException, IOException,
          MessagingException, ImportFileRowException, NoSubscriptionError, ImportInProgressException {


    boolean isInProgress = importHistoryRepository
            .findTopByCompanyIdAndStatusAndRecordTypeOrderByDateDesc(companyId, "In-Progress", ImportHistoryRecordType.UPDATECUSTOMER)
            .map(h -> h.getDate().isAfter(LocalDateTime.now().minusMinutes(30))) // stale-lock guard
            .orElse(false);

    if (isInProgress) {
      throw new ImportInProgressException(
              "An import is already in progress for this company. Please wait until it completes. You can check in the Import History for details."
      );
    }

    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType(ImportHistoryRecordType.UPDATECUSTOMER);
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);
    System.out.println("===========>");
    System.out.println(importHistoryDTO);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setComplete(0L);
    importHistoryDTO = customerService.addImportHistory(importHistoryDTO);

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
        if (isEmpty) {
          ind++;
          continue;
        }

        int errorFlag = 0;
        int[] errorFlagHolder = new int[] {errorFlag};
        StringBuilder errorDesc = new StringBuilder();
        Map<Integer, Boolean> errorCellMap = new HashMap<>();
        CompanyCustomerDTO companyCustomerDTO = null;

        Integer customerLocalId = resolveImportCustomerLocalId(row, headerMap, columnMap, errorCellMap, errorDesc, errorFlagHolder);
        errorFlag = errorFlagHolder[0];

        if (customerLocalId != null && errorFlag == 0) {
          CompanyCustomer existingCustomer =
                  companyCustomerRepository.findByCompanyCustomerIdAndCompanyId(customerLocalId, companyId);
          if (existingCustomer == null) {
            CustomerImportUtils.appendImportError(errorDesc, "ERROR CUSTOMER ID NOT FOUND WHILE UPDATING IN CUSTOMER");
            errorFlag = 1;
            highlightMappedImportField(errorCellMap, headerMap, columnMap, "id");
          } else {
            companyCustomerDTO = modelMapper.map(existingCustomer, CompanyCustomerDTO.class);
          }
        }

        if (companyCustomerDTO == null) {
          companyCustomerDTO = new CompanyCustomerDTO();
          companyCustomerDTO.setCompanyId(companyId);
        }

        for (int j = 0; j < headers.length; j++) {
          String field = headerMap.get(j);
          String cellValue = CustomerImportUtils.getImportCellValue(row, j);
          if (columnMap.get(field) == null) {
            continue;
          }

          String mappedField = CustomerImportUtils.normalizeMappedField(columnMap.get(field));
          if (mappedField == null || "id".equals(mappedField) || "companycustomerid".equals(mappedField)) {
            continue;
          }

          switch (mappedField) {
            case "name":
              if (CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH NO NAME WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
              } else {
                companyCustomerDTO.setName(cellValue);
              }
              break;

            case "phone":
              if (mandatoryFieldsMap.containsKey("phone") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH PHONE MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
                break;
              }
              if (!CustomerImportUtils.isBlank(cellValue)) {
                if (!PhoneUtils.isValidImportPhone(cellValue)) {
                  CustomerImportUtils.appendImportError(errorDesc,
                          "INVALID PHONE NUMBER: Must be 3-15 characters, no letters allowed. Example: +11876543210");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                  break;
                }
                companyCustomerDTO.setPhone(PhoneUtils.formatToUSStyle(cellValue));
              }
              break;

            case "category":
              if (mandatoryFieldsMap.containsKey("category") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH CATEGORY MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
                break;
              }
              if (!CustomerImportUtils.isBlank(cellValue)) {
                List<CompanyCustomerCategory> categoryList =
                        companyCustomerCategoryRepository.findByCompanyId(companyId);
                List<CompanyCustomerCategory> match = categoryList.stream()
                        .filter(x -> x.getName() != null && x.getName().trim().equalsIgnoreCase(cellValue.trim()))
                        .toList();
                if (match.isEmpty()) {
                  CustomerImportUtils.appendImportError(errorDesc, "ERROR IN CATEGORY WHILE UPDATING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                } else {
                  companyCustomerDTO.setCategory(match.get(0).getName());
                }
              }
              break;

            case "email":
              if (CustomerImportUtils.isBlank(cellValue)) {
                companyCustomerDTO.setEmail("");
              } else {
                Optional<CompanyCustomer> duplicateCustomer = companyCustomerRepository
                        .findByEmailAndCompanyId(cellValue, companyId);
                if (duplicateCustomer.isPresent()
                        && (companyCustomerDTO.getId() == null
                        || !duplicateCustomer.get().getId().equals(companyCustomerDTO.getId()))) {
                  CustomerImportUtils.appendImportError(errorDesc, "Email already exists.");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                } else {
                  if (mandatoryFieldsMap.containsKey("email") && CustomerImportUtils.isBlank(cellValue)) {
                    CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH EMAIL MANDATORY WHILE UPDATING IN CUSTOMER");
                    errorFlag = 1;
                    errorCellMap.put(j + 1, true);
                    break;
                  }
                  companyCustomerDTO.setEmail(cellValue);
                }
              }
              break;

            case "address":
              if (mandatoryFieldsMap.containsKey("address") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH ADDRESS MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
                break;
              }
              companyCustomerDTO.setAddress(cellValue);
              break;

            case "city":
              companyCustomerDTO.setCity(cellValue);
              if (mandatoryFieldsMap.containsKey("city") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH CITY MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
              }
              break;

            case "state":
              if (CustomerImportUtils.isBlank(cellValue)) {
                if (mandatoryFieldsMap.containsKey("state")) {
                  CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH STATE MANDATORY WHILE UPDATING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                }
                break;
              }
              Optional<Map.Entry<String, String>> matchedState = findStateInCountryStatesData(cellValue);
              if (matchedState.isEmpty()) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WHILE UPDATING IN STATE");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
              } else {
                companyCustomerDTO.setState(matchedState.get().getValue());
                if (CustomerImportUtils.isBlank(companyCustomerDTO.getCountry())) {
                  companyCustomerDTO.setCountry(matchedState.get().getKey());
                }
              }
              break;

            case "country":
              if (mandatoryFieldsMap.containsKey("country") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH COUNTRY MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
                break;
              }
              if (!CustomerImportUtils.isBlank(cellValue)) {
                if (!CustomerImportUtils.isValidCountry(cellValue, countryList)) {
                  CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH COUNTRY INVALID WHILE UPDATING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                } else {
                  companyCustomerDTO.setCountry(CustomerImportUtils.normalizeCountryName(cellValue));
                }
              }
              break;

            case "zip code":
            case "zipcode":
              if (CustomerImportUtils.isBlank(cellValue)) {
                if (mandatoryFieldsMap.containsKey("zipcode")) {
                  CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH ZIPCODE MANDATORY WHILE UPDATING IN CUSTOMER");
                  errorFlag = 1;
                  errorCellMap.put(j + 1, true);
                }
                break;
              }
              String zipValue = CustomerImportUtils.normalizeZipValue(cellValue);
              if (!CustomerImportUtils.isValidOptionalZip(zipValue, mandatoryFieldsMap.containsKey("zipcode"))) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR IN ZIPCODE FORMAT");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
              } else {
                companyCustomerDTO.setZipCode(zipValue);
              }
              break;

            case "status":
              if (mandatoryFieldsMap.containsKey("status") && CustomerImportUtils.isBlank(cellValue)) {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH STATUS MANDATORY WHILE UPDATING IN CUSTOMER");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
                break;
              }
              if (CustomerImportUtils.isBlank(cellValue)) {
                break;
              }
              if (cellValue.equalsIgnoreCase("active") || cellValue.equalsIgnoreCase("inactive")) {
                companyCustomerDTO.setStatus(cellValue.equalsIgnoreCase("active") ? "active" : "inActive");
              } else {
                CustomerImportUtils.appendImportError(errorDesc, "ERROR WHILE UPDATING IN STATUS");
                errorFlag = 1;
                errorCellMap.put(j + 1, true);
              }
              break;
            default:
              break;
          }
        }

        List<CompanyCustomerExtraFieldName> listExtraFieldName =
                extraFieldNameRepository.findByCompanyId(companyId);
        Map<String, String> extraFieldValues = new HashMap<>();
        errorFlagHolder[0] = errorFlag;
        collectImportExtraFields(row, headerMap, columnMap, mandatoryFieldsMap, listExtraFieldName,
                errorDesc, errorFlagHolder, errorCellMap, extraFieldValues, "UPDATING");
        errorFlag = errorFlagHolder[0];

        if (errorFlag == 0 && companyCustomerDTO.getId() != null) {
          try {
            companyCustomerService.updateCustomer(companyCustomerDTO);
            saveImportExtraFields(companyCustomerDTO.getId(), companyId, extraFieldValues, listExtraFieldName);
          } catch (Exception e) {
            CustomerImportUtils.appendImportError(errorDesc, "ERROR WHILE UPDATING IN CUSTOMER");
            errorFlag = 1;
          }
        }

        if (errorFlag == 1) {
          writeImportErrorRow(sheet, excelIndex++, ind, headers, row, errorDesc.toString(), errorCellMap, errorCellStyle);
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
        importHistoryDTO.setHasErrorReport(true);
        importHistoryDTO.setErrorReportFileName("CustomerImportErrors.xlsx");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
          workbook.write(baos);
          importHistoryDTO.setErrorReportFile(baos.toByteArray());
          try (FileOutputStream fileOut = new FileOutputStream(EXCEL_FILENAME)) {
            workbook.write(fileOut);
          }
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

  @Operation(summary = "Export Company Customers", description = "Endpoint to export company customers")
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

  @Operation(summary = "Statelist", description = "Endpoint to statelist")
  @GetMapping(value = "/statelist")
  public ResponseEntity<List<String>> statelist() {
    return ResponseEntity.ok(getAllStatesFromCountryStatesData());
  }

  // ─── Utility ──────────────────────────────────────────────────────────────

  /**
   * Looks up a state/province in {@code Country-States.json}.
   * Returns country key and canonical state name when found.
   */
  private Optional<Map.Entry<String, String>> findStateInCountryStatesData(String stateValue) {
    if (CustomerImportUtils.isBlank(stateValue) || data == null) {
      return Optional.empty();
    }
    String trimmedState = stateValue.trim();
    for (Map.Entry<String, List<String>> countryEntry : data.entrySet()) {
      List<String> states = countryEntry.getValue();
      if (states == null) {
        continue;
      }
      for (String state : states) {
        if (state != null && state.trim().equalsIgnoreCase(trimmedState)) {
          return Optional.of(Map.entry(countryEntry.getKey(), state.trim()));
        }
      }
    }
    return Optional.empty();
  }

  private List<String> getAllStatesFromCountryStatesData() {
    if (data == null) {
      return List.of();
    }
    return data.values().stream()
            .filter(Objects::nonNull)
            .flatMap(states -> states.stream().filter(Objects::nonNull))
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
  }

  private void collectImportExtraFields(
          String[] row,
          Map<Integer, String> headerMap,
          Map<String, String> columnMap,
          Map<String, Boolean> mandatoryFieldsMap,
          List<CompanyCustomerExtraFieldName> extraFieldNames,
          StringBuilder errorDesc,
          int[] errorFlag,
          Map<Integer, Boolean> errorCellMap,
          Map<String, String> extraFieldValues,
          String importAction) {
    for (int j = 0; j < headerMap.size(); j++) {
      String field = headerMap.get(j);
      if (field == null || columnMap.get(field) == null) {
        continue;
      }

      for (CompanyCustomerExtraFieldName extraFieldName : extraFieldNames) {
        if (!columnMap.get(field).equalsIgnoreCase(extraFieldName.getName())) {
          continue;
        }

        String value = CustomerImportUtils.getImportCellValue(row, j);
        String extraFieldLabel = extraFieldName.getName().toUpperCase();

        if (mandatoryFieldsMap.containsKey(extraFieldName.getName().toLowerCase())
                && CustomerImportUtils.isBlank(value)) {
          CustomerImportUtils.appendImportError(errorDesc,
                  "ERROR WITH " + extraFieldLabel + " MANDATORY WHILE " + importAction + " IN CUSTOMER");
          errorFlag[0] = 1;
          errorCellMap.put(j + 1, true);
          break;
        }

        if (CustomerImportUtils.isBlank(value)) {
          break;
        }

        if ("number".equals(extraFieldName.getType())) {
          try {
            int val = Integer.parseInt(value.trim());
            extraFieldValues.put(extraFieldName.getName().toLowerCase(), Integer.toString(val));
          } catch (Exception e) {
            CustomerImportUtils.appendImportError(errorDesc,
                    "ERROR WHILE " + importAction + " IN " + extraFieldLabel);
            errorFlag[0] = 1;
            errorCellMap.put(j + 1, true);
          }
        } else if ("date".equals(extraFieldName.getType())) {
          try {
            extraFieldValues.put(extraFieldName.getName().toLowerCase(),
                    CustomerImportUtils.parseImportDate(value));
          } catch (Exception e) {
            CustomerImportUtils.appendImportError(errorDesc,
                    "ERROR WHILE " + importAction + " IN " + extraFieldLabel);
            errorFlag[0] = 1;
            errorCellMap.put(j + 1, true);
          }
        } else {
          extraFieldValues.put(extraFieldName.getName().toLowerCase(), value);
        }
        break;
      }
    }
  }

  private void saveImportExtraFields(
          String customerMongoId,
          Long companyId,
          Map<String, String> extraFieldValues,
          List<CompanyCustomerExtraFieldName> extraFieldNames) {
    for (CompanyCustomerExtraFieldName extraFieldName : extraFieldNames) {
      String lowerName = extraFieldName.getName().toLowerCase();
      if (!extraFieldValues.containsKey(lowerName)) {
        continue;
      }
      CompanyCustomerExtraFields extraFields = extraFieldsRepository
              .findByNameIgnoreCaseAndCompanyCustomerId(extraFieldName.getName(), customerMongoId);
      if (extraFields == null) {
        extraFields = new CompanyCustomerExtraFields();
      }
      extraFields.setCompanyCustomerId(customerMongoId);
      extraFields.setCompanyId(companyId);
      extraFields.setName(extraFieldName.getName());
      extraFields.setType(extraFieldName.getType());
      extraFields.setValue(extraFieldValues.get(lowerName));
      extraFieldsRepository.save(extraFields);
    }
  }

  private Integer resolveImportCustomerLocalId(
          String[] row,
          Map<Integer, String> headerMap,
          Map<String, String> columnMap,
          Map<Integer, Boolean> errorCellMap,
          StringBuilder errorDesc,
          int[] errorFlag) {
    for (int j = 0; j < headerMap.size(); j++) {
      String field = headerMap.get(j);
      if (field == null || columnMap.get(field) == null) {
        continue;
      }
      String mappedField = CustomerImportUtils.normalizeMappedField(columnMap.get(field));
      if (!"id".equals(mappedField) && !"companycustomerid".equals(mappedField)) {
        continue;
      }
      String value = CustomerImportUtils.getImportCellValue(row, j);
      if (CustomerImportUtils.isBlank(value)) {
        CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH NO CUSTOMER ID WHILE UPDATING IN CUSTOMER");
        errorFlag[0] = 1;
        errorCellMap.put(j + 1, true);
        return null;
      }
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH INVALID CUSTOMER ID WHILE UPDATING IN CUSTOMER");
        errorFlag[0] = 1;
        errorCellMap.put(j + 1, true);
        return null;
      }
    }
    CustomerImportUtils.appendImportError(errorDesc, "ERROR WITH NO CUSTOMER ID WHILE UPDATING IN CUSTOMER");
    errorFlag[0] = 1;
    return null;
  }

  private CellStyle createErrorCellStyle(Workbook workbook) {
    CellStyle errorStyle = workbook.createCellStyle();
    errorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
    errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return errorStyle;
  }

  private void highlightMappedImportField(
          Map<Integer, Boolean> errorCellMap,
          Map<Integer, String> headerMap,
          Map<String, String> columnMap,
          String targetField) {
    for (int j = 0; j < headerMap.size(); j++) {
      String field = headerMap.get(j);
      if (field == null || columnMap.get(field) == null) {
        continue;
      }
      String mappedField = CustomerImportUtils.normalizeMappedField(columnMap.get(field));
      if (targetField.equals(mappedField)
              || ("companycustomerid".equals(targetField) && "id".equals(mappedField))
              || ("id".equals(targetField) && "companycustomerid".equals(mappedField))) {
        errorCellMap.put(j + 1, true);
      }
    }
  }

  private void writeImportErrorRow(
          Sheet sheet,
          int excelIndex,
          long rowIndex,
          String[] headers,
          String[] row,
          String errorDescription,
          Map<Integer, Boolean> errorCellMap,
          CellStyle errorCellStyle) {
    Row myrow = sheet.createRow(excelIndex);
    myrow.createCell(0).setCellValue("Line " + (rowIndex + 1));
    for (int j = 0; j < headers.length; j++) {
      Cell dataCell = myrow.createCell(j + 1);
      dataCell.setCellValue(CustomerImportUtils.getImportCellValue(row, j));
      if (Boolean.TRUE.equals(errorCellMap.get(j + 1))) {
        dataCell.setCellStyle(errorCellStyle);
      }
    }
    Cell errorDescriptionCell = myrow.createCell(headers.length + 1);
    errorDescriptionCell.setCellValue(errorDescription);
    errorDescriptionCell.setCellStyle(errorCellStyle);
  }

  private String normalizeToE164(String phone) {
    if (phone == null || phone.trim().isEmpty()) return null;


    // Strip all formatting characters (spaces, dashes, dots, parentheses)
    String cleaned = phone.trim()
            .replaceAll("[\\s\\-().+]", "");

    // After stripping, must be all digits
    if (!cleaned.matches("\\d+")) {
      log.warn("Phone contains non-numeric characters after cleaning: {}", phone);
      return null;
    }

    // Re-attach + if original started with +
    String e164;
    if (phone.trim().startsWith("+")) {
      e164 = "+" + cleaned;
    } else {
      // No + prefix — if number is long enough to have a country code, add +
      // But we can't safely assume country code, so reject ambiguous numbers
      log.warn("Phone number missing + country code prefix: {}", phone);
      return null;
    }

    // E.164 max length is 15 digits (excluding +)
    if (cleaned.length() > 15) {
      log.warn("Phone number exceeds 15 digits (E.164 max): {}", phone);
      return null;
    }

    // E.164 min length — shortest valid numbers are 7 digits total
    if (cleaned.length() < 7) {
      log.warn("Phone number too short to be valid: {}", phone);
      return null;
    }

    // Country code cannot start with 0
    if (cleaned.startsWith("0")) {
      log.warn("Phone number has invalid country code starting with 0: {}", phone);
      return null;
    }
    else {
      // Assume default country code if no + prefix
      log.info("No country code prefix, assuming default +{}: {}", DEFAULT_COUNTRY_CODE, phone);
      e164 = "+" + DEFAULT_COUNTRY_CODE + cleaned;
    }

    return e164;
  }

  /**
   * Get asset count by company customer with sorting capability
   * @param sortOrder Optional sort order (ASC or DESC). Default is DESC
   * @return List of company customers with their asset count, sorted by count
   */
  @Operation(summary = "Get Asset Count By Company Customer", description = "Endpoint to get asset count by company customer")
  @GetMapping("/assetCountByCustomer")
//  @PreAuthorize("hasAnyRole('ADMIN', 'COMPANY_ADMIN')")
  public ResponseEntity<?> getAssetCountByCompanyCustomer(
          @RequestParam(value = "sortOrder", required = false, defaultValue = "DESC") String sortOrder) {
    try {
      log.info("API: Received request to get asset count by company customer with sortOrder: {}", sortOrder);

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      String username = authentication.getName();
      log.info("User: {} requesting asset count", username);

      Long companyId = null;
      Optional<Users> userOptional = usersRepository.findByEmail(username);
      if (userOptional.isPresent()) {
        companyId = userOptional.get().getCompanyId();
      }

      if (companyId == null) {
        log.error("Company ID not found for user: {}", username);
        ResponseMessageDTO response = new ResponseMessageDTO();
        response.setResponseMessage("Company ID not found");
        return ResponseEntity.badRequest().body(response);
      }

      List<AssetCountByCompanyCustomerDTO> results = companyCustomerService.getAssetCountByCompanyCustomer(companyId, sortOrder);

      log.info("Successfully retrieved asset count for {} company customers", results.size());
      return ResponseEntity.ok(results);

    } catch (Exception e) {
      log.error("Error getting asset count by company customer: {}", e.getMessage(), e);
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Failed to retrieve asset count: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
  }

  private String resolveCompanyCustomerExtraFieldOldValue(CompanyCustomerExtraFieldsDTO extraFieldsDTO) {
    if (extraFieldsDTO.getId() != null && !extraFieldsDTO.getId().isBlank()) {
      return extraFieldsRepository.findById(extraFieldsDTO.getId())
              .map(CompanyCustomerExtraFields::getValue)
              .orElse(null);
    }
    if (extraFieldsDTO.getCompanyCustomerId() != null && extraFieldsDTO.getName() != null) {
      CompanyCustomerExtraFields existing = extraFieldsRepository.findByNameIgnoreCaseAndCompanyCustomerId(
              extraFieldsDTO.getName(), extraFieldsDTO.getCompanyCustomerId());
      return existing != null ? existing.getValue() : null;
    }
    return null;
  }

  private void auditCompanyCustomerExtraFieldValueChange(
          CompanyCustomerExtraFieldsDTO extraFieldsDTO, String oldValue, Long companyId) {
    if (extraFieldsDTO.getCompanyCustomerId() == null || extraFieldsDTO.getName() == null) {
      return;
    }
    if (Objects.equals(oldValue, extraFieldsDTO.getValue())) {
      return;
    }
    companyCustomerRepository.findById(extraFieldsDTO.getCompanyCustomerId()).ifPresent(customer -> {
      Map<String, Object> fieldChange = Map.of(
              extraFieldsDTO.getName(),
              Map.of(
                      "old", oldValue != null ? oldValue : "",
                      "new", extraFieldsDTO.getValue() != null ? extraFieldsDTO.getValue() : ""));
      auditService.logUpdate(AuditModule.CUSTOMER,
              String.valueOf(customer.getCompanyCustomerId()), customer.getName(),
              companyId != null ? companyId : customer.getCompanyId(), fieldChange);
    });
  }
}

