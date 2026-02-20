package com.quantumai.customer.controller;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.service.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(
        origins = {
                "http://localhost:4200",
                "http://assetyugg.com.s3-website-us-east-1.amazonaws.com"
        },
        allowedHeaders = {"device-id", "Content-Type", "Authorization"}
)
@RestController
@RequestMapping("/assets")
@Slf4j
public class AssetAPI {

  @Autowired private JavaMailSender emailSender;
  @Autowired private AssetsService assetsService;
  @Autowired private AssetExtraFieldNameRepository extraFieldNameRepository;
  @Autowired private AssetExtraFieldsRepository extraFieldsRepository;
  @Autowired private AssetsRepository assetsRepository;
  @Autowired private CompanyCustomerAPI companyCustomerAPI;
  @Autowired private CustomerService customerService;
  @Autowired private SubscriptionRepository subscriptionRepository;
  @Autowired private AssetCategoryRepository assetCategoryRepository;
  @Autowired private AssetCategoryInspectionRepository assetCategoryInspectionRepository;
  @Autowired private LocationRepository locationRepository;
  @Autowired private BinRepository binRepository;
  @Autowired private NotificationService notificationService;
  @Autowired private CompanyCustomerMandatoryFieldsRepository companyCustomerMandatoryFieldsRepository;
  @Autowired private AssetMandatoryFieldsRepository assetMandatoryFieldsRepository;
  @Autowired private UsersRepository usersRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private AssetCheckInOutRepository assetCheckInOutRepository;
  @Autowired private CompanyCustomerRepository companyCustomerRepository;
  @Autowired private MongoTemplate mongoTemplate;

  private final ModelMapper modelMapper = new ModelMapper();

  // ─── Health check (public) ────────────────────────────────────────────────

  @GetMapping("/working")
  public String working() {
    return "Working";
  }

  // ─── Read endpoints ───────────────────────────────────────────────────────

  @GetMapping("/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetsDTO> getAssets(@PathVariable Long companyId) {
    return assetsService.getAssetsDetails(companyId);
  }

  @GetMapping("/getByCutomerId/{customerId}/{pageNumber}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public PaginatedResultDTO<String> getAssetsByCustomer(
          @PathVariable String customerId,
          @PathVariable(required = false) Integer pageNumber) {
    return assetsService.getAssetsDetailsByCustomerId(customerId, pageNumber);
  }

  @GetMapping("/getAsset/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public AssetsDTO getAsset(@PathVariable String id) throws Exception {
    return assetsService.getAsset(id);
  }

  @GetMapping("/getExtraFields/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return assetsService.getExtraFields(id);
  }

  @GetMapping("/getExtraFieldName/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    return assetsService.getAssetExtraField(companyId);
  }

  @GetMapping("/getExtraFieldNameValue/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return assetsService.getextraFieldList(companyId);
  }

  @GetMapping("/getCheckInOutList/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public ResponseEntity<List<AssetCheckInOutDTO>> getCheckInOutList(@PathVariable String assetId) {
    return new ResponseEntity<>(assetsService.getCheckOutInList(assetId), HttpStatus.ACCEPTED);
  }

  @GetMapping("/getFile/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetFileDTO> getAssetFile(@PathVariable String assetId) {
    return assetsService.getAssetFile(assetId);
  }

  @GetMapping("/getFile/download/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    AssetFileDTO assetFileDTO = assetsService.downloadFile(id);
    return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.valueOf("json/object"))
            .body(assetFileDTO.getFile());
  }

  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetMandatoryFields> getMandatoryFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getMandatoryFields(name, companyId));
  }

  @GetMapping("/getShowFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetShowFields> getShowFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getShowFields(name, companyId));
  }

  @GetMapping("/getAllMandatoryFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<List<AssetMandatoryFields>> getAllMandatoryFields(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getAllMandatoryFields(companyId));
  }

  @GetMapping("/getAllShowFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<List<AssetShowFields>> getAllShowFields(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getAllShowFields(companyId));
  }

  @GetMapping("/getQRData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetQR> getQRData(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getQRData(companyId));
  }

  @GetMapping("/getAllAssetData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultDTO<String> getAllAssetData(@PathVariable Long companyId) {
    return assetsService.getAllAssetDetails(companyId);
  }

  @GetMapping(value = "/searchAssetlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<String> getWorkOrderFromAsset(
          @PathVariable Long companyId,
          @RequestParam(name = "data") String search,
          @RequestParam(name = "category") String category) {
    return assetsService.searchedAssets(companyId, search, category);
  }

  @GetMapping(value = "/sortAssetlist/{companyId}/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultDTO<String> getSortedWorkOrderFromAsset(
          @PathVariable Long companyId,
          @PathVariable(required = false) Integer pageNumber,
          @PathVariable(required = false) Integer pageSize,
          @RequestParam(name = "category") String category) {
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 5;
    return assetsService.sortAssets(companyId, category, pageNumber, pageSize);
  }

  @GetMapping("checkInOutCount/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public CheckInCheckOutCountDTO checkInCheckOutCountDTO(@PathVariable Long companyId) {
    return assetsService.checkInCheckOut(companyId);
  }

  @GetMapping("/checkInOutAsset/{companyId}/{checkedIn}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCheckInOut> checkInOutAsset(
          @PathVariable Long companyId, @PathVariable Boolean checkedIn) {
    return assetsService.filterByCheckedInOut(companyId, checkedIn);
  }

  @GetMapping("/checkInOutAssetData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultCheckInOutDTO<AssetCheckInOutData> getCheckInOutData(
          @PathVariable Long companyId,
          @RequestParam Long pageNumber,
          @RequestParam Long pageSize) {
    return assetsService.getAssetCheckInOutData(companyId, pageNumber, pageSize);
  }

  @GetMapping(value = "/getCategoryList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategory> getCategoryList(@PathVariable Long companyId) {
    return assetsService.getCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return assetsService.getActiveCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public AssetCategory getCategoryById(@PathVariable Long companyId, @PathVariable String id) {
    return assetsService.getCategoryListById(companyId, id);
  }

  @GetMapping(value = "/countAssetByCategory/{category}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public int countAssetByCategory(@PathVariable String category) throws CategoryException {
    return assetsService.countAssetByCategory(category);
  }

  @GetMapping(value = "/getActiveAssets/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetsDTO> getActiveAssets(@PathVariable Long companyId) {
    return assetsService.getActiveAssets(companyId);
  }

  @GetMapping(value = "/getAllAssetInspectionByCategory/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspection> getAllAssetInspectionByCategory(
          @PathVariable Long companyId, @RequestParam String category) throws Exception {
    return assetsService.getAllAssetInspectionByCategory(companyId, category);
  }

  @GetMapping(value = "/getAllAssetInspection/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspection> getAllAssetInspection(@PathVariable Long companyId) throws Exception {
    return assetsService.getAllAssetInspection(companyId);
  }

  @GetMapping(value = "/getAllAssetInspectionInstance/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstance(@PathVariable Long companyId) {
    return assetsService.getAllAssetCategoryInspectionValues(companyId);
  }

  @GetMapping(value = "/getAllAssetInspectionInstanceByAssetId/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstanceByAssetId(@PathVariable String assetId) {
    return assetsService.getAllAssetCategoryInspectionInstanceByAsset(assetId);
  }

  @GetMapping(value = "/getAssetByCategory/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public Map<String, List<AssetsDTO>> getAssetByCategory(@PathVariable Long companyId) {
    return assetsService.getAssetByCategory(companyId);
  }

  @GetMapping(value = "/locationBinDetails/{companyId}/{name}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public String getLocationBinDetails(@PathVariable Long companyId, @PathVariable String name) {
    Optional<Bin> optBin = binRepository.findByCompanyIdAndBinNumberIgnoreCase(companyId, name);
    if (optBin.isPresent()) {
      Bin bin = optBin.get();
      Location location = getLocationForBin(bin);
      return location.getName().concat("->").concat(bin.getBinNumber());
    } else {
      return locationRepository.findByCompanyIdAndName(companyId, name)
              .map(Location::getName).orElse(null);
    }
  }

  @GetMapping(value = "/getAssetInspection/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public AssetCategoryInspection getAssetInspection(@PathVariable String id) throws Exception {
    return assetsService.getAssetInspection(id);
  }

  @PostMapping("/advanceFilter/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public PaginatedResultDTO<String> advanceFilter(
          @RequestBody Object filter,
          @PathVariable(required = false) Integer pageNumber,
          @PathVariable(required = false) Integer pageSize,
          @RequestParam(name = "category", required = false) String category,
          @RequestParam(name = "search", required = false) String searchData,
          @RequestParam(name = "asc", required = false) Boolean asc) throws NoSubscriptionError {
    if (asc == null) asc = true;
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 5;
    if (category == null || category.isEmpty()) { category = "updatedAt"; asc = false; }
    return assetsService.advanceFilter(filter, pageNumber, pageSize, category, searchData, asc);
  }

  @PostMapping("/advancedFilter/optimized")
  @PreAuthorize("@appSecurity.canView(authentication, #filter.companyId, 'assets')")
  public PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(@RequestBody AssetAdvancedFilterDTO filter)
          throws NoSubscriptionError {
    log.info("Optimized advanced filter - CompanyId: {}", filter.getCompanyId());
    if (filter.getPageNumber() == null) filter.setPageNumber(0);
    if (filter.getPageSize() == null) filter.setPageSize(10);
    if (filter.getSortDirection() == null || filter.getSortDirection().isEmpty()) filter.setSortDirection("DESC");
    return assetsService.getAssetsWithAdvancedFilter(filter);
  }

  // ─── Create / Write endpoints ─────────────────────────────────────────────

  @PutMapping("/addassets")
  @PreAuthorize("@appSecurity.canEdit(authentication, #assestsDTO.companyId, 'assets')")
  public void addAssets(@RequestBody AssetsDTO assestsDTO) throws NoSubscriptionError {
    assetsService.addAssets(assestsDTO);
  }

  @PostMapping("/addNewAssets")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assestsDTO.companyId, 'assets')")
  public ResponseEntity<AssetsDTO> addNewAssets(@RequestBody AssetsDTO assestsDTO) throws NoSubscriptionError {
    log.info("NEW ASSET DATA: {}", assestsDTO);
    return ResponseEntity.ok(assetsService.addAssets(assestsDTO));
  }

  @PostMapping("/import/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'assets')")
  public void importFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws ImportFileRowException, MessagingException, NoSubscriptionError {

    List<Location> locationList = locationRepository.findByCompanyId(companyId);
    List<Bin> binList = binRepository.findByCompanyId(companyId);
    List<AssetMandatoryFields> assetMandatoryList =
            assetMandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId, true);
    List<String> mandatoryColumnList =
            assetMandatoryList.stream().map(ele -> ele.getName().toLowerCase()).toList();

    Map<String, String> columnMap = parseColumnMappings(columnMappings);

    long totalCount = countFileRows(file);
    if (totalCount > 1001) throw new ImportFileRowException("Import File cannot import more than 1000 rows");

    ImportHistory importHistoryDTO = new ImportHistory();
    try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = buildHeaderMap(headers);

      Workbook workbook = new XSSFWorkbook();
      Sheet errorSheet = workbook.createSheet("Errors");
      int excelIndex = 0;

      Row headerRow = errorSheet.createRow(excelIndex++);
      for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
      headerRow.createCell(headers.length).setCellValue("Error");

      long currCount = 0;
      importHistoryDTO.setFileName(file.getOriginalFilename());
      importHistoryDTO.setRecordType("Add Asset Record");
      importHistoryDTO.setExecutedBy(email);
      importHistoryDTO.setDate(LocalDateTime.now());
      importHistoryDTO.setStatus("In-Progress");
      importHistoryDTO.setCompanyId(companyId);

      String[] row;
      while ((row = csvReader.readNext()) != null) {
        if (isEmptyRow(row)) continue;

        AssetsDTO assetsDTO = new AssetsDTO();
        assetsDTO.setCompanyId(companyId);
        int[] errorFlag = {0};
        StringBuilder errorDesc = new StringBuilder();

        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          if (errorFlag[0] == 1) break;
          if (columnMap.get(field) == null) continue;

          label:
          switch (columnMap.get(field).toLowerCase()) {
            case "name": assetsDTO.setName(row[j]); break;
            case "serialnumber":
              if (mandatoryColumnList.contains("serialnumber") && row[j].trim().isEmpty()) {
                errorDesc.append("Serial Number is Mandatory"); errorFlag[0] = 1;
              } else { assetsDTO.setSerialNumber(row[j]); }
              break;
            case "category":
              if (mandatoryColumnList.contains("category") && row[j].trim().isEmpty()) {
                errorDesc.append("Category is Mandatory"); errorFlag[0] = 1; break;
              }
              final String categoryValue = row[j].trim();  // ✅ effectively final, safe for lambda
              if (!categoryValue.isBlank()) {
                List<AssetCategory> match = assetCategoryRepository.findByCompanyId(companyId)
                        .stream().filter(x -> x.getName().equalsIgnoreCase(categoryValue))
                        .toList();
                if (match.isEmpty()) { errorDesc.append("CATEGORY"); errorFlag[0] = 1; }
                else assetsDTO.setCategory(match.get(0).getName());
              }
              break;
            case "customer":
              if (mandatoryColumnList.contains("customer") && row[j].trim().isEmpty()) {
                errorDesc.append("Customer is Mandatory"); errorFlag[0] = 1; break;
              }
              CompanyCustomerDTO dto = companyCustomerAPI.getCompanyCustomerByLocalId(row[j], companyId);
              if (dto == null) { errorDesc.append("CUSTOMER ID"); errorFlag[0] = 1; }
              else { assetsDTO.setCustomerId(dto.getId()); assetsDTO.setCustomer(dto.getName()); }
              break;
            case "location":
              if (mandatoryColumnList.contains("location") && row[j].trim().isEmpty()) {
                errorDesc.append("Location is Mandatory"); errorFlag[0] = 1; break;
              }
              String loc = row[j].trim();
              List<Location> locs = locationList.stream()
                      .filter(l -> l.getName().equalsIgnoreCase(loc)).toList();
              if (!locs.isEmpty()) { assetsDTO.setLocation("location:" + locs.get(0).getId()); break; }
              List<Bin> bins = binList.stream()
                      .filter(b -> b.getBinNumber().equalsIgnoreCase(loc)).toList();
              if (!bins.isEmpty()) { assetsDTO.setLocation("bin:" + bins.get(0).getId()); break; }
              errorDesc.append("LOCATION"); errorFlag[0] = 1;
              break;
            case "status":
              switch (row[j].toLowerCase()) {
                case "active": assetsDTO.setStatus("active"); break label;
                case "inactive": assetsDTO.setStatus("inActive"); break label;
                case "outofservice": assetsDTO.setStatus("outOfService"); break label;
                default: errorDesc.append("STATUS"); errorFlag[0] = 1; break label;
              }
          }
        }

        if (errorFlag[0] == 1) {
          Row errorRow = errorSheet.createRow(excelIndex++);
          for (int k = 0; k < row.length; k++) errorRow.createCell(k).setCellValue(row[k]);
          errorRow.createCell(row.length).setCellValue(errorDesc.toString());
        } else {
          assetsService.addAssets(assetsDTO);
        }

        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now());
        importHistoryDTO.setComplete((currCount * 100L) / (totalCount - 1));
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
      }

      sendImportEmail(email, workbook, excelIndex > 1, "Asset Import Results - AssetYug");
      importHistoryDTO.setStatus("Completed");
      importHistoryDTO.setMessage(excelIndex > 1 ? "Sent import result via email" : "Import was Successfully Done");

    } catch (IOException | CsvValidationException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      log.error("Import failed", e);
    }

    customerService.addImportHistory(importHistoryDTO);
    sendImportNotification(companyId, file.getOriginalFilename());
    log.info("Import completed for companyId: {}", companyId);
  }

  @PostMapping("/importUpdation/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'assets')")
  public void updateAssetWithFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, JsonParseException, IOException,
          MessagingException, ImportFileRowException, NoSubscriptionError {

    List<Location> locationList = locationRepository.findByCompanyId(companyId);
    List<Bin> binList = binRepository.findByCompanyId(companyId);
    Map<String, String> columnMap = parseColumnMappings(columnMappings);

    long totalCount = countFileRows(file);
    if (totalCount > 1001) throw new ImportFileRowException("Import File cannot import more than 1000 rows");

    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType("Updated Asset Record");
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);

    long currCount = 0;
    try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = buildHeaderMap(headers);

      Workbook workbook = new XSSFWorkbook();
      Sheet errorSheet = workbook.createSheet("Error Report");
      if (headers != null) {
        Row errorHeaderRow = errorSheet.createRow(0);
        for (int i = 0; i < headers.length; i++) errorHeaderRow.createCell(i).setCellValue(headers[i]);
        errorHeaderRow.createCell(headers.length).setCellValue("Error");
      }

      String[] row;
      while ((row = csvReader.readNext()) != null) {
        if (isEmptyRow(row)) continue;

        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        Assets assets = new Assets();

        // Find asset by assetId
        String assetIdValue = null;
        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          if ("assetid".equalsIgnoreCase(columnMap.get(field))) {
            assetIdValue = row[j].trim(); break;
          }
        }

        if (assetIdValue != null) {
          Optional<Assets> myAssets = assetsRepository.findByAssetIdAndCompanyId(
                  Integer.parseInt(assetIdValue), companyId);
          if (myAssets.isEmpty()) {
            errorFlag = 1; errorDesc.append("ASSETID NOT FOUND");
          } else {
            assets = myAssets.get();
          }
        } else {
          errorFlag = 1; errorDesc.append("ASSETID COLUMN MISSING OR EMPTY");
        }

        // Process remaining fields (same logic as before — kept for brevity)
        for (int j = 0; j < row.length && errorFlag == 0; j++) {
          String field = headerMap.get(j);
          if (columnMap.get(field) == null) continue;
          switch (columnMap.get(field).toLowerCase()) {
            case "name": assets.setName(row[j]); break;
            case "serialnumber": assets.setSerialNumber(row[j]); break;
            case "category":
              final String categoryValue = row[j].trim();  // ✅ capture before lambda
              List<AssetCategory> cats = assetCategoryRepository.findByCompanyId(companyId)
                      .stream().filter(x -> x.getName().equalsIgnoreCase(categoryValue)).toList();
              if (cats.isEmpty()) errorDesc.append("ERROR IN CATEGORY");
              else assets.setCategory(cats.get(0).getName());
              break;
            case "status":
              switch (row[j].toLowerCase()) {
                case "active": assets.setStatus("active"); break;
                case "inactive": assets.setStatus("inActive"); break;
                case "outofservice": assets.setStatus("outOfService"); break;
                default: errorDesc.append("ERROR IN STATUS"); errorFlag = 1;
              }
              break;
          }
        }

        if (errorFlag == 0) {
          assetsService.addAssets(modelMapper.map(assets, AssetsDTO.class));
        } else {
          Row errorRow = errorSheet.createRow(errorSheet.getLastRowNum() + 1);
          for (int col = 0; col < row.length; col++) errorRow.createCell(col).setCellValue(row[col]);
          errorRow.createCell(row.length).setCellValue(errorDesc.toString());
        }

        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now());
        importHistoryDTO.setComplete((currCount * 100L) / (totalCount - 1));
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
      }

      sendImportEmail(email, workbook, errorSheet.getLastRowNum() > 0, "Asset Update Results - AssetYug");
      importHistoryDTO.setStatus("Completed");

    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      log.error("Update import failed", e);
    }

    customerService.addImportHistory(importHistoryDTO);
    log.info("Update import completed for companyId: {}", companyId);
  }

  @PostMapping("/imageUpload")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void uploadImage(@RequestBody AssetImageDTO assetImageDTO) throws Exception {
    assetsService.addImage(assetImageDTO);
  }

  @PostMapping("/removeImage")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void removeImage(@RequestBody String id) throws Exception {
    assetsService.removeImage(id);
  }

  @PostMapping("/addfields")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void addNewFields(@RequestBody AssetExtraFieldsDTO extraFieldsDTO) throws Exception {
    assetsService.addExtraFields(extraFieldsDTO);
  }

  @PostMapping("/addExtraFieldName")
  @PreAuthorize("@appSecurity.canCreate(authentication, #extraFieldNameDTO.companyId, 'assets')")
  public void addExtraFieldName(@RequestBody AssetExtraFieldNameDTO extraFieldNameDTO) throws Exception {
    assetsService.addAssetExtraField(extraFieldNameDTO);
  }

  @PostMapping("/addCheckInOut")
  @PreAuthorize("@appSecurity.canCreate(authentication, #checkInDTO.companyId, 'assets')")
  public void addCheckInOut(@RequestBody AssetCheckInDTO checkInDTO) throws NoSubscriptionError {
    assetsService.addCheckInOut(checkInDTO);
  }

  @PostMapping("/addFile/{assetId}/{username}")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public ResponseEntity<ResponseMessageDTO> addAssetFile(
          @RequestParam("file") MultipartFile file,
          @PathVariable String assetId,
          @PathVariable String username) throws NoSubscriptionError {
    if (assetsRepository.findById(assetId).isEmpty()) {
      ResponseMessageDTO r = new ResponseMessageDTO();
      r.setResponseMessage("Asset not found");
      return new ResponseEntity<>(r, HttpStatus.NOT_FOUND);
    }
    try {
      assetsService.addAssetFile(file, assetId, username);
      ResponseMessageDTO r = new ResponseMessageDTO();
      r.setResponseMessage("Uploaded successfully: " + file.getOriginalFilename());
      return new ResponseEntity<>(r, HttpStatus.OK);
    } catch (IOException e) {
      ResponseMessageDTO r = new ResponseMessageDTO();
      r.setResponseMessage("Could not upload: " + file.getOriginalFilename());
      return new ResponseEntity<>(r, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/mandatoryFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #mandatoryFields.companyId, 'assets')")
  public void mandatoryFields(@RequestBody AssetMandatoryFields mandatoryFields) throws NoSubscriptionError {
    assetsService.updateMandatoryFields(mandatoryFields);
  }

  @PostMapping("/showFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #showFields.companyId, 'assets')")
  public void showFields(@RequestBody AssetShowFields showFields) throws NoSubscriptionError {
    assetsService.updateShowFields(showFields);
  }

  @PostMapping("/saveQRData")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void saveQRData(@RequestBody AssetQR qr) {
    assetsService.qrDataUpdation(qr);
  }

  @PostMapping("/updateAssetsWithInActive")
  @PreAuthorize("@appSecurity.canEditAny(authentication, 'assets')")
  public void updateAssetsWithInActive(@RequestBody String customerId) throws NoSubscriptionError {
    assetsService.updateAssetsWithInActive(customerId);
  }

  @PostMapping(value = "/addCategory")
  @PreAuthorize("@appSecurity.canCreate(authentication, #categoryDTO.companyId, 'assets')")
  public void addCategory(@RequestBody CategoryDTO categoryDTO) throws Exception {
    assetsService.addCategory(categoryDTO);
  }

  @PostMapping(value = "/addAssetInspection")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assetCategoryInspection.companyId, 'assets')")
  public void addAssetInspection(@RequestBody AssetCategoryInspection assetCategoryInspection) throws NoSubscriptionError {
    assetsService.addAssetInspection(assetCategoryInspection);
  }

  @PostMapping(value = "/addAssetInspectionInstance")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void addAssetInspectionInstance(@RequestBody AssetCategoryInspectionInstance assetCategoryInspection) {
    assetsService.addAssetInspectionInstance(assetCategoryInspection);
  }

  // ─── Update endpoints ─────────────────────────────────────────────────────

  @PutMapping(value = "/addAssetInspectionInstance")
  @PreAuthorize("@appSecurity.canEdit(authentication, #assetCategoryInspection.companyId, 'assets')")
  public void updateAssetInspection(@RequestBody AssetCategoryInspectionInstance assetCategoryInspection) throws NoSubscriptionError {
    assetsService.updateAssetInspectionInstance(assetCategoryInspection);
  }

  @PutMapping(value = "/updateCategory")
  @PreAuthorize("@appSecurity.canEdit(authentication, #categoryDTO.companyId, 'assets')")
  public void updateCategory(@RequestBody CategoryDTO categoryDTO) throws NoSubscriptionError {
    assetsService.updateCategory(categoryDTO);
  }

  @PutMapping("/extraFieldName")
  @PreAuthorize("@appSecurity.canEditAny(authentication, 'assets')")
  public ResponseEntity<AssetExtraFieldName> updateExtraFieldName(
          @RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
    return ResponseEntity.ok(assetsService.updateExtraFieldName(extraFieldNameUpdateDTO));
  }

  // ─── Delete endpoints ─────────────────────────────────────────────────────

  @PostMapping("/removeAsset")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void removeAsset(@RequestBody String id) throws Exception {
    assetsService.removeAsset(id);
  }

  @DeleteMapping("/deleteExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteExtraField(@PathVariable String id) throws Exception {
    assetsService.deleteExtraFields(id);
  }

  @DeleteMapping("/deleteExtraFieldName/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteExtraFieldName(@PathVariable String id) throws Exception {
    assetsService.deleteAssetExtraField(id);
  }

  @DeleteMapping("deleteFile/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteFile(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteFile(id);
  }

  @DeleteMapping(value = "/deleteCategory/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteCategory(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteCategory(id);
  }

  @DeleteMapping(value = "/deleteAssetInspection/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteAssetInspection(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteAssetInspection(id);
  }

  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'assets')")
  public void deleteShowAndMandatoryField(@PathVariable String name, @PathVariable Long companyId) throws Exception {
    assetsService.deleteShowAndMandatoryFields(companyId, name);
  }

  // ─── Export endpoints ─────────────────────────────────────────────────────

  @GetMapping("/export-asset-xlsx/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> exportAssetsXlsx(@PathVariable Long companyId) throws IOException {
    List<Assets> assets = assetsRepository.findByCompanyId(companyId);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Assets");
    Row header = sheet.createRow(0);
    int col = 0;
    for (String h : new String[]{"ID","Name","AssetId","Category","Customer","CustomerId","Location","Status"})
      header.createCell(col++).setCellValue(h);
    for (AssetExtraFieldName ef : extraFieldNames) header.createCell(col++).setCellValue(ef.getName());
    int rowIdx = 1;
    for (Assets asset : assets) {
      Row row = sheet.createRow(rowIdx++);
      int c = 0;
      row.createCell(c++).setCellValue(asset.getId());
      row.createCell(c++).setCellValue(asset.getName());
      row.createCell(c++).setCellValue(asset.getAssetId() != null ? asset.getAssetId().toString() : "");
      row.createCell(c++).setCellValue(asset.getCategory());
      row.createCell(c++).setCellValue(asset.getCustomer());
      row.createCell(c++).setCellValue(asset.getCustomerId());
      row.createCell(c++).setCellValue(asset.getLocation());
      row.createCell(c++).setCellValue(asset.getStatus());
      List<AssetExtraFields> extras = extraFieldsRepository.findByAssetId(asset.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (AssetExtraFields ef : extras) extraMap.put(ef.getName(), ef.getValue());
      for (AssetExtraFieldName ef : extraFieldNames)
        row.createCell(c++).setCellValue(extraMap.getOrDefault(ef.getName(), ""));
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos); workbook.close();
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=AssetExport.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bos.toByteArray());
  }

  @GetMapping("/export-asset/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> exportCompanyCustomers(@PathVariable Long companyId) throws IOException {
    List<Assets> assets = assetsRepository.findByCompanyId(companyId);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    Query q = new Query(Criteria.where("assetId").in(
            assets.stream().map(Assets::getId).collect(Collectors.toList())));
    List<AssetCheckInOut> checkInOutList = mongoTemplate.find(q, AssetCheckInOut.class);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Assets");
    Row header = sheet.createRow(0);
    int col = 0;
    CellStyle textStyle = workbook.createCellStyle();
    textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));

    for (String h : new String[]{"ID","Serial Number","Customer","Category","Location","Status","Last Handled By","Last Known Location"})
      header.createCell(col++).setCellValue(h);
    for (AssetExtraFieldName ef : extraFieldNames) header.createCell(col++).setCellValue(ef.getName());

    int rowIdx = 1;
    for (Assets asset : assets) {
      Row row = sheet.createRow(rowIdx++);
      int c = 0;
      Cell idCell = row.createCell(c++); idCell.setCellValue(String.valueOf(asset.getAssetId())); idCell.setCellStyle(textStyle);
      Cell snCell = row.createCell(c++); snCell.setCellValue(asset.getSerialNumber()); snCell.setCellStyle(textStyle);
      row.createCell(c++).setCellValue(asset.getCustomer());
      row.createCell(c++).setCellValue(asset.getCategory());

      String locVal = asset.getLocation(); String locName = "";
      if (locVal != null && !locVal.isBlank()) {
        String[] parts = locVal.split(":", 2);
        if (parts.length == 2) {
          if ("location".equalsIgnoreCase(parts[0]))
            locName = locationRepository.findById(parts[1]).map(Location::getName).orElse("");
          else if ("bin".equalsIgnoreCase(parts[0]))
            locName = binRepository.findById(parts[1]).map(Bin::getBinNumber).orElse("");
        }
      }
      row.createCell(c++).setCellValue(locName);

      String status = asset.getStatus();
      if (status != null && !status.isEmpty())
        status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
      row.createCell(c++).setCellValue(status != null ? status : "");

      List<AssetCheckInOut> matched = checkInOutList.stream()
              .filter(a -> a.getAssetId().equals(asset.getId())).toList();
      if (!matched.isEmpty()) {
        int len = matched.get(0).getDetailsList().size();
        row.createCell(c++).setCellValue(matched.get(0).getDetailsList().get(len-1).getEmployee());
        row.createCell(c++).setCellValue(matched.get(0).getDetailsList().get(len-1).getUserLocation());
      } else {
        row.createCell(c++).setCellValue(""); row.createCell(c++).setCellValue("");
      }

      List<AssetExtraFields> extras = extraFieldsRepository.findByAssetId(asset.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (AssetExtraFields ef : extras) extraMap.put(ef.getName(), ef.getValue());
      for (AssetExtraFieldName ef : extraFieldNames) {
        Cell cell = row.createCell(c++);
        cell.setCellValue(extraMap.getOrDefault(ef.getName(), ""));
        cell.setCellStyle(textStyle);
      }
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos); workbook.close();
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=AssetExport.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bos.toByteArray());
  }

  @GetMapping("/export-checkinout-xlsx/{companyId}/{assetId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> exportCheckInOut(
          @PathVariable Long companyId, @PathVariable String assetId) {

    Optional<AssetCheckInOut> opt = assetCheckInOutRepository.findByCompanyIdAndAssetId(companyId, assetId);
    Optional<Assets> assetDetails = assetsRepository.findById(assetId);
    Optional<CompanyCustomer> companyCustomer = companyCustomerRepository.findById(assetDetails.get().getCustomerId());

    if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Check In-Out Report");
    String[] headers = {"Asset ID","Asset Name","Customer ID","Customer Name","Action","Action Date","Action Time","Location","Username","Notes","IP Address","GeoLocation"};
    Row headerRow = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(getHeaderCellStyle(workbook));
    }

    DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    int rowIdx = 1;
    for (AssetCheckInOutDetails d : opt.get().getDetailsList()) {
      Row row = sheet.createRow(rowIdx++);
      row.createCell(0).setCellValue(assetDetails.get().getAssetId());
      row.createCell(1).setCellValue(assetDetails.get().getName());
      row.createCell(2).setCellValue(companyCustomer.get().getCompanyCustomerId());
      row.createCell(3).setCellValue(assetDetails.get().getCustomer());
      row.createCell(4).setCellValue(d.getStatus() != null ? d.getStatus() : "");
      row.createCell(5).setCellValue(d.getDate() != null ? d.getDate().format(dateFmt) : "");
      row.createCell(6).setCellValue(d.getDate() != null ? d.getDate().format(timeFmt) : "");
      row.createCell(7).setCellValue(d.getLocation() != null ? d.getLocation() : "");
      row.createCell(8).setCellValue(d.getEmployee() != null ? d.getEmployee() : "");
      row.createCell(9).setCellValue(d.getNotes() != null ? d.getNotes() : "");
      row.createCell(10).setCellValue(d.getIpAddress());
      row.createCell(11).setCellValue(d.getUserLocation());
    }
    for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try { workbook.write(bos); workbook.close(); }
    catch (IOException e) { throw new RuntimeException("Error generating Excel", e); }

    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=CheckInOut_Report_" + assetId + ".xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(bos.toByteArray());
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private Map<String, String> parseColumnMappings(String columnMappings) {
    Map<String, String> map = new HashMap<>();
    try {
      JsonFactory factory = new JsonFactory();
      JsonParser parser = factory.createParser(columnMappings);
      String key = "", val = "";
      while (!parser.isClosed()) {
        JsonToken token = parser.nextToken();
        if (token == null) break;
        if (!key.isEmpty()) { map.put(key, val); key = ""; }
        if (token == JsonToken.FIELD_NAME) key = parser.getCurrentName();
        else if (token == JsonToken.VALUE_STRING) val = parser.getText();
      }
      parser.close();
    } catch (Exception e) { log.error("Failed to parse column mappings", e); }
    return map;
  }

  private long countFileRows(MultipartFile file) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
      return reader.lines().count();
    } catch (IOException e) { return 0; }
  }

  private Map<Integer, String> buildHeaderMap(String[] headers) {
    Map<Integer, String> map = new HashMap<>();
    if (headers != null) for (int i = 0; i < headers.length; i++) map.put(i, headers[i]);
    return map;
  }

  private boolean isEmptyRow(String[] row) {
    return Arrays.stream(row).map(c -> c == null ? "" : c.trim()).allMatch(String::isEmpty);
  }

  private void sendImportEmail(String email, Workbook workbook, boolean hasErrors, String subject)
          throws MessagingException, IOException {
    String subjectName = resolveUserName(email);
    try (FileOutputStream out = new FileOutputStream("Report.xlsx")) { workbook.write(out); }
    workbook.close();
    MimeMessage message = emailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setTo(email);
    helper.setSubject(subject);
    if (hasErrors) {
      helper.setText("Hi " + subjectName + ",\n\nYour import completed with errors. Please check the attached file, fix the issues, and re-upload only the listed rows.\n\nBest regards,\nAssetYug Team");
      helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
    } else {
      helper.setText("Hi " + subjectName + ",\n\nYour import completed successfully. All data is now in the system.\n\nBest regards,\nAssetYug Team");
    }
    emailSender.send(message);
  }

  private String resolveUserName(String email) {
    return customerRepository.findByEmail(email)
            .filter(c -> c.getFirstName() != null && c.getLastName() != null)
            .map(c -> c.getFirstName() + " " + c.getLastName())
            .orElse("User");
  }

  private void sendImportNotification(Long companyId, String fileName) {
    Notification notification = new Notification();
    notification.setNotificationType(NotificationType.COMPANY);
    notification.setMessage("Assets imported from: " + fileName);
    notification.setTitle("Asset Import");
    notification.setCreatedAt(LocalDateTime.now());
    notificationService.sendNotificationToCompany(companyId, notification);
  }

  private CellStyle getHeaderCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  public Location getLocationForBin(Bin bin) {
    if (bin.getLocationId() != null)
      return locationRepository.findById(bin.getLocationId().toString()).orElse(null);
    return null;
  }
}