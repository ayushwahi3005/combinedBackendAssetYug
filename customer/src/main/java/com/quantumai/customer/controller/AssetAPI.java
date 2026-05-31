package com.quantumai.customer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.Notification;
import com.quantumai.customer.entity.enums.ImportHistoryRecordType;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.service.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
@Tag(name = "Asset", description = "Asset Management API")
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
  @Autowired private ImportHistoryRepository importHistoryRepository;
  @Autowired private AssetUniqueFieldConfigurationRepository assetUniqueFieldConfigurationRepository;

  private final ModelMapper modelMapper = new ModelMapper();

  // ─── Health check (public) ────────────────────────────────────────────────

  @Operation(summary = "Working", description = "Endpoint to working")
  @GetMapping("/working")
  public String working() {
    return "Working";
  }

  // ─── Read endpoints ───────────────────────────────────────────────────────

  @Operation(summary = "Get Assets", description = "Endpoint to get assets")
  @GetMapping("/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetsDTO> getAssets(@PathVariable Long companyId) {
    return assetsService.getAssetsDetails(companyId);
  }

  @Operation(summary = "Get Assets By Customer", description = "Endpoint to get assets by customer")
  @GetMapping("/getByCutomerId/{customerId}/{pageNumber}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public PaginatedResultDTO<String> getAssetsByCustomer(
          @PathVariable String customerId,
          @PathVariable(required = false) Integer pageNumber) {
    return assetsService.getAssetsDetailsByCustomerId(customerId, pageNumber);
  }

  @Operation(summary = "Get Asset", description = "Endpoint to get asset")
  @GetMapping("/getAsset/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public AssetsDTO getAsset(@PathVariable String id) throws Exception {
    return assetsService.getAsset(id);
  }

  @Operation(summary = "Get Asset Details", description = "Endpoint to get asset details")
  @GetMapping("/getAssetDetails/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public AssetsDTO getAssetDetails(@PathVariable String id) throws Exception {
    return assetsService.getAssetSpecific(id);
  }

  @Operation(summary = "Get Extra Fields", description = "Endpoint to get extra fields")
  @GetMapping("/getExtraFields/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return assetsService.getExtraFields(id);
  }

  @Operation(summary = "Get Extra Field Name", description = "Endpoint to get extra field name")
  @GetMapping("/getExtraFieldName/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    return assetsService.getAssetExtraField(companyId);
  }

  @Operation(summary = "Get Extra Field Name Value", description = "Endpoint to get extra field name value")
  @GetMapping("/getExtraFieldNameValue/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return assetsService.getextraFieldList(companyId);
  }

  @Operation(summary = "Get Check In Out List", description = "Endpoint to get check in out list")
  @GetMapping("/getCheckInOutList/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public ResponseEntity<List<AssetCheckInOutDTO>> getCheckInOutList(@PathVariable String assetId) {
    return new ResponseEntity<>(assetsService.getCheckOutInList(assetId), HttpStatus.ACCEPTED);
  }

  @Operation(summary = "Get Asset File", description = "Endpoint to get asset file")
  @GetMapping("/getFile/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetFileDTO> getAssetFile(@PathVariable String assetId) {
    return assetsService.getAssetFile(assetId);
  }

  @Operation(summary = "Download File", description = "Endpoint to download file")
  @GetMapping("/getFile/download/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    AssetFileDTO assetFileDTO = assetsService.downloadFile(id);
    return ResponseEntity.status(HttpStatus.OK)
            .contentType(MediaType.valueOf("json/object"))
            .body(assetFileDTO.getFile());
  }

  @Operation(summary = "Get Mandatory Fields", description = "Endpoint to get mandatory fields")
  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetMandatoryFields> getMandatoryFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getMandatoryFields(name, companyId));
  }

  @Operation(summary = "Get Show Fields", description = "Endpoint to get show fields")
  @GetMapping("/getShowFields/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetShowFields> getShowFields(
          @PathVariable String name, @PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getShowFields(name, companyId));
  }

  @Operation(summary = "Get All Mandatory Fields", description = "Endpoint to get all mandatory fields")
  @GetMapping("/getAllMandatoryFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<List<AssetMandatoryFields>> getAllMandatoryFields(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getAllMandatoryFields(companyId));
  }

  @Operation(summary = "Get All Show Fields", description = "Endpoint to get all show fields")
  @GetMapping("/getAllShowFields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<List<AssetShowFields>> getAllShowFields(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getAllShowFields(companyId));
  }

  @Operation(summary = "Get Qrdata", description = "Endpoint to get qrdata")
  @GetMapping("/getQRData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetQR> getQRData(@PathVariable Long companyId) {
    return ResponseEntity.ok(assetsService.getQRData(companyId));
  }

  @Operation(summary = "Get All Asset Data", description = "Endpoint to get all asset data")
  @GetMapping("/getAllAssetData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultDTO<String> getAllAssetData(@PathVariable Long companyId) {
    return assetsService.getAllAssetDetails(companyId);
  }

  @Operation(summary = "Get Work Order From Asset", description = "Endpoint to get work order from asset")
  @GetMapping(value = "/searchAssetlist/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<String> getWorkOrderFromAsset(
          @PathVariable Long companyId,
          @RequestParam(name = "data") String search,
          @RequestParam(name = "category") String category) {
    return assetsService.searchedAssets(companyId, search, category);
  }

  @Operation(summary = "Get Sorted Work Order From Asset", description = "Endpoint to get sorted work order from asset")
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

  @Operation(summary = "Check In Check Out Count Dto", description = "Endpoint to check in check out count dto")
  @GetMapping("checkInOutCount/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public CheckInCheckOutCountDTO checkInCheckOutCountDTO(@PathVariable Long companyId) {
    return assetsService.checkInCheckOut(companyId);
  }

  @Operation(summary = "Check In Out Asset", description = "Endpoint to check in out asset")
  @GetMapping("/checkInOutAsset/{companyId}/{checkedIn}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCheckInOut> checkInOutAsset(
          @PathVariable Long companyId, @PathVariable Boolean checkedIn) {
    return assetsService.filterByCheckedInOut(companyId, checkedIn);
  }

  @Operation(summary = "Get Check In Out Data", description = "Endpoint to get check in out data")
  @GetMapping("/checkInOutAssetData/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultCheckInOutDTO<AssetCheckInOutData> getCheckInOutData(
          @PathVariable Long companyId,
          @RequestParam Long pageNumber,
          @RequestParam Long pageSize) {
    return assetsService.getAssetCheckInOutData(companyId, pageNumber, pageSize);
  }

  @Operation(summary = "Get Category List", description = "Endpoint to get category list")
  @GetMapping(value = "/getCategoryList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategory> getCategoryList(@PathVariable Long companyId) {
    return assetsService.getCategoryList(companyId);
  }



  @Operation(summary = "Get Category Active List", description = "Endpoint to get category active list")
  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return assetsService.getActiveCategoryList(companyId);
  }

  @Operation(summary = "Get Category By Id", description = "Endpoint to get category by id")
  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public AssetCategory getCategoryById(@PathVariable Long companyId, @PathVariable String id) {
    return assetsService.getCategoryListById(companyId, id);
  }

  @Operation(summary = "Count Asset By Category", description = "Endpoint to count asset by category")
  @GetMapping(value = "/countAssetByCategory/{category}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public int countAssetByCategory(@PathVariable String category) throws CategoryException {
    return assetsService.countAssetByCategory(category);
  }

  @Operation(summary = "Count Asset By Categories", description = "Endpoint to count asset by categories")
  @GetMapping(value = "/countAssetByCategories/{companyId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetCountByCategoryDTO> countAssetByCategories(@PathVariable Long companyId) throws CategoryException {
    return assetsService.countAssetByCategories(companyId);
  }

  @Operation(summary = "Get Active Assets", description = "Endpoint to get active assets")
  @GetMapping(value = "/getActiveAssets/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetsDTO> getActiveAssets(@PathVariable Long companyId) {
    return assetsService.getActiveAssets(companyId);
  }

  @Operation(summary = "Get All Asset Inspection By Category", description = "Endpoint to get all asset inspection by category")
  @GetMapping(value = "/getAllAssetInspectionByCategory/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspection> getAllAssetInspectionByCategory(
          @PathVariable Long companyId, @RequestParam String category) throws Exception {
    return assetsService.getAllAssetInspectionByCategory(companyId, category);
  }

  @Operation(summary = "Get All Asset Inspection", description = "Endpoint to get all asset inspection")
  @GetMapping(value = "/getAllAssetInspection/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspection> getAllAssetInspection(@PathVariable Long companyId) throws Exception {
    return assetsService.getAllAssetInspection(companyId);
  }

  @Operation(summary = "Get All Asset Inspection Instance", description = "Endpoint to get all asset inspection instance")
  @GetMapping(value = "/getAllAssetInspectionInstance/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstance(@PathVariable Long companyId) {
    return assetsService.getAllAssetCategoryInspectionValues(companyId);
  }

  @Operation(summary = "Get All Asset Inspection Instance Paginated", description = "Endpoint to get all asset inspection instance paginated")
  @GetMapping(value = "/getAllAssetInspectionInstance/{companyId}/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public PaginatedResultDTO<AssetCategoryInspectionInstance> getAllAssetInspectionInstancePaginated(
          @PathVariable Long companyId,
          @PathVariable Integer pageNumber,
          @PathVariable Integer pageSize) {
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 10;
    return assetsService.getAllAssetCategoryInspectionValuesPaginated(companyId, pageNumber, pageSize);
  }

  @Operation(summary = "Get All Asset Inspection Instance By Asset Id", description = "Endpoint to get all asset inspection instance by asset id")
  @GetMapping(value = "/getAllAssetInspectionInstanceByAssetId/{assetId}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstanceByAssetId(@PathVariable String assetId) {
    return assetsService.getAllAssetCategoryInspectionInstanceByAsset(assetId);
  }

  @Operation(summary = "Get All Asset Inspection Instance By Asset Id Paginated", description = "Endpoint to get all asset inspection instance by asset id paginated")
  @GetMapping(value = "/getAllAssetInspectionInstanceByAssetId/{assetId}/{pageNumber}/{pageSize}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public PaginatedResultDTO<AssetCategoryInspectionInstance> getAllAssetInspectionInstanceByAssetIdPaginated(
          @PathVariable String assetId,
          @PathVariable Integer pageNumber,
          @PathVariable Integer pageSize) {
    if (pageNumber == null) pageNumber = 0;
    if (pageSize == null) pageSize = 10;
    log.info("Fetching paginated inspection instances for assetId: {}, pageNumber: {}, pageSize: {}", assetId, pageNumber, pageSize);
    return assetsService.getAllAssetCategoryInspectionInstanceByAssetPaginated(assetId, pageNumber, pageSize);
  }

  @Operation(summary = "Get Asset By Category", description = "Endpoint to get asset by category")
  @GetMapping(value = "/getAssetByCategory/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public Map<String, List<AssetsDTO>> getAssetByCategory(@PathVariable Long companyId) {
    return assetsService.getAssetByCategory(companyId);
  }

  @Operation(summary = "Get Location Bin Details", description = "Endpoint to get location bin details")
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

  @Operation(summary = "Get Asset Inspection", description = "Endpoint to get asset inspection")
  @GetMapping(value = "/getAssetInspection/{id}")
  @PreAuthorize("@appSecurity.canViewAny(authentication, 'assets')")
  public AssetCategoryInspection getAssetInspection(@PathVariable String id) throws Exception {
    return assetsService.getAssetInspection(id);
  }

  @Operation(summary = "Get Unique Field Config", description = "Endpoint to get unique field config")
  @GetMapping("/uniqueFieldConfig/{companyId}")
//  @PreAuthorize("@appSecurity.canViewAny(authentication, #companyId, 'assets')")
  public ResponseEntity<List<AssetUniqueFieldConfigurationDTO>> getUniqueFieldConfig(@PathVariable Long companyId) throws Exception {

    return ResponseEntity.ok(assetsService.getUniqueFieldConfigurations(companyId));
  }

  @Operation(summary = "Advance Filter", description = "Endpoint to advance filter")
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

  @Operation(summary = "Get Assets With Advanced Filter", description = "Endpoint to get assets with advanced filter")
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

  @Operation(summary = "Add Assets", description = "Endpoint to add assets")
  @PutMapping("/addassets")
  @PreAuthorize("@appSecurity.canEdit(authentication, #assestsDTO.companyId, 'assets')")
  public void addAssets(@RequestBody AssetsDTO assestsDTO) throws Exception {
    assetsService.addAssets(assestsDTO);
  }

  @Operation(summary = "Add New Assets", description = "Endpoint to add new assets")
  @PostMapping("/addNewAssets")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assestsDTO.companyId, 'assets')")
  public ResponseEntity<AssetsDTO> addNewAssets(@RequestBody AssetsDTO assestsDTO) throws Exception {
    log.info("NEW ASSET DATA: {}", assestsDTO);
    return ResponseEntity.ok(assetsService.addAssets(assestsDTO));
  }

  @Operation(summary = "Add Unique Field Config", description = "Endpoint to add unique field config")
  @PostMapping("/uniqueFieldConfig")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assetUniqueFieldConfigurationDTO.companyId, 'assets')")
  public ResponseEntity<AssetUniqueFieldConfigurationDTO> addUniqueFieldConfig(@RequestBody AssetUniqueFieldConfigurationDTO assetUniqueFieldConfigurationDTO) throws Exception {

    return ResponseEntity.ok(assetsService.saveUniqueFieldConfiguration(assetUniqueFieldConfigurationDTO));
  }

  @Operation(summary = "Import File", description = "Endpoint to import file")
  @PostMapping("/import/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canCreate(authentication, #companyId, 'assets')")
  public void importFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws ImportFileRowException, MessagingException, ImportInProgressException {

    // ✅ STEP 1: Check if an import is already running for this company
    boolean isInProgress = importHistoryRepository
            .findTopByCompanyIdAndStatusAndRecordTypeOrderByDateDesc(companyId, "In-Progress", ImportHistoryRecordType.ADDASSET)
            .map(h -> h.getDate().isAfter(LocalDateTime.now().minusMinutes(30))) // stale-lock guard
            .orElse(false);

    if (isInProgress) {
      throw new ImportInProgressException(
              "An import is already in progress for this company. Please wait until it completes. You can check in the Import History for details."
      );
    }

    List<Location> locationList = locationRepository.findByCompanyId(companyId);
    List<Bin> binList = binRepository.findByCompanyId(companyId);
    List<AssetMandatoryFields> assetMandatoryList =
            assetMandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId, true);
    List<String> mandatoryColumnList =
            assetMandatoryList.stream().map(ele -> ele.getName().toLowerCase()).toList();

//    log.info("📌 Columns for companyId {}: {}", companyId, columnMappings.toString());
    Map<String, String> columnMap = parseColumnMappings(columnMappings);
//    log.info("📋 Column Mappings Parsed: {}", columnMap);

    long totalCount = countFileRows(file);
    if (totalCount > 1001) throw new ImportFileRowException("Import File cannot import more than 1000 rows");

    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType(ImportHistoryRecordType.ADDASSET);
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setComplete(0L);
    importHistoryDTO = customerService.addImportHistory(importHistoryDTO);


//    try {
//      Thread.sleep(50000);
//    } catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }




    try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = buildHeaderMap(headers);

      Workbook workbook = new XSSFWorkbook();
      Sheet errorSheet = workbook.createSheet("Errors");
      CellStyle errorCellStyle = createErrorCellStyle(workbook);
      int excelIndex = 0;

      Row headerRow = errorSheet.createRow(excelIndex++);
      for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
      headerRow.createCell(headers.length).setCellValue("Error");

      long currCount = 0;


      String[] row;
      while ((row = csvReader.readNext()) != null) {
        if (isEmptyRow(row)) continue;

        AssetsDTO assetsDTO = new AssetsDTO();
        assetsDTO.setCompanyId(companyId);
        int errorFlag =0;
        StringBuilder errorDesc = new StringBuilder();
        Map<Integer, Boolean> errorCellMap = new HashMap<>();

        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          log.info("Column Mapping for field '{}': '{}'", field, columnMap.get(field));
          if (errorFlag == 1) break;
          if (columnMap.get(field) == null) continue;

          label:
          switch (columnMap.get(field).toLowerCase()) {
            case "name":
              log.info("NAME FIELD VALUE: '{}'", row[j]);
              if(row[j].trim().isEmpty()){

                  errorDesc.append("Name is Mandatory");
                  errorFlag = 1;
                  errorCellMap.put(j, true);

              } else{
              assetsDTO.setName(row[j]);}
              break;
            case "serialnumber":
              if (mandatoryColumnList.contains("serialnumber") && row[j].trim().isEmpty()) {
                errorDesc.append("Serial Number is Mandatory");
                errorFlag = 1;
                errorCellMap.put(j, true);
              } else { assetsDTO.setSerialNumber(row[j]); }
              break;
            case "category":
              if (mandatoryColumnList.contains("category") && row[j].trim().isEmpty()) {
                errorDesc.append("Category is Mandatory");
                errorFlag = 1;
                errorCellMap.put(j, true);
                break;
              }
              final String categoryValue = row[j].trim();  // ✅ effectively final, safe for lambda
              if (!categoryValue.isBlank()) {
                List<AssetCategory> match = assetCategoryRepository.findByCompanyId(companyId)
                        .stream().filter(x -> x.getName().equalsIgnoreCase(categoryValue))
                        .toList();
                if (match.isEmpty()) {
                  errorDesc.append("CATEGORY");
                  errorFlag = 1;
                  errorCellMap.put(j, true);
                }
                else assetsDTO.setCategory(match.get(0).getName());
              }
              break;
            case "customer":
              if (mandatoryColumnList.contains("customer") && row[j].trim().isEmpty()) {
                errorDesc.append("Customer is Mandatory");
                errorFlag = 1;
                errorCellMap.put(j, true);
                break;
              }
              String customerName = row[j].trim();
              if (!customerName.isBlank()) {
                CompanyCustomerDTO customerDTO = companyCustomerAPI.getCompanyCustomerByLocalId(customerName, companyId);
                if (customerDTO == null) {
                  errorDesc.append("CUSTOMER NOT FOUND");
                  errorFlag = 1;
                  errorCellMap.put(j, true);
                } else {
                  assetsDTO.setCustomerId(customerDTO.getId());
                  assetsDTO.setCustomer(customerDTO.getName());
                }
              }
              break;
            case "location":
              if (mandatoryColumnList.contains("location") && row[j].trim().isEmpty()) {
                errorDesc.append("Location is Mandatory");
                errorFlag = 1;
                errorCellMap.put(j, true);
                break;
              }
              else if(row[j].trim().isEmpty()) { break; }
              String loc = row[j].trim();
              List<Location> locs = locationList.stream()
                      .filter(l -> l.getName().equalsIgnoreCase(loc)).toList();
              if (!locs.isEmpty()) { assetsDTO.setLocation("location:" + locs.get(0).getId()); break; }
              List<Bin> bins = binList.stream()
                      .filter(b -> b.getBinNumber().equalsIgnoreCase(loc)).toList();
              if (!bins.isEmpty()) { assetsDTO.setLocation("bin:" + bins.get(0).getId()); break; }
              errorDesc.append("LOCATION");
              errorFlag = 1;
              errorCellMap.put(j, true);
              break;
            case "status":
              switch (row[j].toLowerCase()) {
                case "active": assetsDTO.setStatus("active"); break label;
                case "inactive": assetsDTO.setStatus("inactive"); break label;
                case "outofservice": assetsDTO.setStatus("outofservice"); break label;
                default:  assetsDTO.setStatus("active"); break label;
              }
          }
        }
        log.info("Error Flag : {}",errorFlag);
        log.info("Asset DTO : {}",assetsDTO.toString());
        if (errorFlag == 1) {
          Row errorRow = errorSheet.createRow(excelIndex++);
          for (int k = 0; k < row.length; k++) {
            Cell cell = errorRow.createCell(k);
            cell.setCellValue(row[k]);
          }

          // Apply red background to error cells
          for (Map.Entry<Integer, Boolean> entry : errorCellMap.entrySet()) {
            if (entry.getValue()) {
              Cell errorCell = errorRow.getCell(entry.getKey());
              if (errorCell != null) errorCell.setCellStyle(errorCellStyle);
            }
          }

          // Create error description cell with red background
          Cell errorDescriptionCell = errorRow.createCell(row.length);
          errorDescriptionCell.setCellValue(errorDesc.toString());
          errorDescriptionCell.setCellStyle(errorCellStyle);
        } else {
          log.info("Attempting to save asset: {}", assetsDTO.toString());

          // ✅ STEP 1: Collect all extra field values and validate format/mandatory (don't stop on first error)
          List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
          Map<String, String> extraFieldValues = new HashMap<>();
          List<String> formatErrorMessages = new ArrayList<>();

          // Process extra fields from CSV to collect values and validate format
          for (int j = 0; j < row.length; j++) {
            String field = headerMap.get(j);
            String value = row[j] != null ? row[j].trim() : "";

            if (columnMap.get(field) == null) continue;

            // Check if this column maps to an extra field
            for (AssetExtraFieldName extraFieldName : extraFieldNames) {
              if (columnMap.get(field).equalsIgnoreCase(extraFieldName.getName())) {
                String formattedValue = value;

                // ✅ Check mandatory extra fields
                if (mandatoryColumnList.contains(extraFieldName.getName().toLowerCase())) {
                  if (value.trim().isEmpty()) {
                    formatErrorMessages.add(extraFieldName.getName().toUpperCase() + " is MANDATORY");
                    errorCellMap.put(j, true);
                    errorFlag = 1;
                    break;
                  }
                }

                // ✅ Validate format based on type (don't break, collect all errors)
                if ("number".equals(extraFieldName.getType())) {
                  if (!value.trim().isEmpty()) {
                    try {
                      int val = Integer.parseInt(value);
                      formattedValue = Integer.toString(val);
                    } catch (NumberFormatException e) {
                      formatErrorMessages.add(extraFieldName.getName().toUpperCase() + " - Invalid number format");
                      errorCellMap.put(j, true);
                      errorFlag = 1;
                      break;
                    }
                  }
                } else if ("date".equals(extraFieldName.getType())) {
                  if (!value.trim().isEmpty()) {
                    try {
                      DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                      LocalDate date = LocalDate.parse(value, inputFormatter);
                      DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                      formattedValue = date.format(outputFormatter);
                    } catch (Exception e) {
                      formatErrorMessages.add(extraFieldName.getName().toUpperCase() + " - Invalid date format (use dd-MM-yyyy)");
                      errorCellMap.put(j, true);
                      errorFlag = 1;
                      break;
                    }
                  }
                }

                // Only add to map if format validation passed for this field
                if (errorFlag == 0) {
                  extraFieldValues.put(extraFieldName.getName().toLowerCase(), formattedValue);
                }
                break;
              }
            }
          }

          // ✅ STEP 2: Validate unique fields (even if there are format errors, so we can highlight all problems)
          if (!validateAllUniqueFieldsForImport(assetsDTO, extraFieldValues, companyId, errorDesc,
                                                errorCellMap, headerMap, columnMap)) {
            errorFlag = 1;
            log.warn("Unique field validation failed: {}", errorDesc.toString());
          }

          // ✅ Combine all error messages
          if (errorFlag == 1) {
            if (!formatErrorMessages.isEmpty()) {
              for (int i = 0; i < formatErrorMessages.size(); i++) {
                if (i == 0 && errorDesc.length() == 0) {
                  errorDesc.append("ERROR: ").append(formatErrorMessages.get(i));
                } else {
                  if (errorDesc.length() > 0) {
                    errorDesc.append(" | ");
                  }
                  errorDesc.append(formatErrorMessages.get(i));
                }
              }
            }
          }

          // ✅ STEP 3: Save asset and extra fields ONLY if all validations pass
          if (errorFlag == 0) {
            AssetsDTO savedAssetDTO = assetsService.addAssets(assetsDTO);

            // Save extra fields
            for (Map.Entry<String, String> entry : extraFieldValues.entrySet()) {
              AssetExtraFields extraFields = new AssetExtraFields();
              extraFields.setAssetId(savedAssetDTO.getId());
              extraFields.setName(entry.getKey());
              extraFields.setValue(entry.getValue());
              extraFields.setCompanyId(companyId);

              // Find type from extraFieldNames
              for (AssetExtraFieldName extraFieldName : extraFieldNames) {
                if (extraFieldName.getName().equalsIgnoreCase(entry.getKey())) {
                  extraFields.setType(extraFieldName.getType());
                  break;
                }
              }

              extraFieldsRepository.save(extraFields);
              log.info("Extra field saved: {} = {}", entry.getKey(), entry.getValue());
            }
          }

          if (errorFlag != 0) {
            Row errorRow = errorSheet.createRow(excelIndex++);
            for (int col = 0; col < row.length; col++) {
              Cell cell = errorRow.createCell(col);
              cell.setCellValue(row[col]);
            }

            // Apply red background to error cells
            for (Map.Entry<Integer, Boolean> entry : errorCellMap.entrySet()) {
              if (entry.getValue()) {
                Cell errorCell = errorRow.getCell(entry.getKey());
                if (errorCell != null) errorCell.setCellStyle(errorCellStyle);
              }
            }

            // Create error description cell with red background
            Cell errorDescriptionCell = errorRow.createCell(row.length);
            errorDescriptionCell.setCellValue(errorDesc.toString());
            errorDescriptionCell.setCellStyle(errorCellStyle);
            log.warn("📋 Extra field error recorded at row {}: {}", excelIndex - 1, errorDesc.toString());
          }
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
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

      customerService.addImportHistory(importHistoryDTO);
    sendImportNotification(companyId, file.getOriginalFilename());
    log.info("Import completed for companyId: {}", companyId);
  }

  @Operation(summary = "Update Asset With File", description = "Endpoint to update asset with file")
  @PostMapping("/importUpdation/{companyId}/{email}")
  @PreAuthorize("@appSecurity.canEdit(authentication, #companyId, 'assets')")
  public void updateAssetWithFile(
          @RequestParam("file") MultipartFile file,
          @RequestParam("columnMappings") String columnMappings,
          @PathVariable Long companyId,
          @PathVariable String email)
          throws CsvValidationException, JsonParseException, IOException,
          MessagingException, ImportFileRowException, NoSubscriptionError, ImportInProgressException {


    boolean isInProgress = importHistoryRepository
            .findTopByCompanyIdAndStatusAndRecordTypeOrderByDateDesc(companyId, "In-Progress", ImportHistoryRecordType.ADDASSET)
            .map(h -> h.getDate().isAfter(LocalDateTime.now().minusMinutes(30))) // stale-lock guard
            .orElse(false);

    if (isInProgress) {
      throw new ImportInProgressException(
              "An import is already in progress for this company. Please wait until it completes. You can check in the Import History for details."
      );
    }



    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType(ImportHistoryRecordType.UPDATEASSET);
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);
    importHistoryDTO.setDate(LocalDateTime.now());
    importHistoryDTO.setComplete(0L);
    importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
    List<Location> locationList = locationRepository.findByCompanyId(companyId);
    List<Bin> binList = binRepository.findByCompanyId(companyId);
    Map<String, String> columnMap = parseColumnMappings(columnMappings);

    long totalCount = countFileRows(file);
    if (totalCount > 1001) throw new ImportFileRowException("Import File cannot import more than 1000 rows");



    long currCount = 0;
    try (CSVReader csvReader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = buildHeaderMap(headers);

      Workbook workbook = new XSSFWorkbook();
      Sheet errorSheet = workbook.createSheet("Error Report");
      int errorRowIndex = 0;
      if (headers != null) {
        Row errorHeaderRow = errorSheet.createRow(errorRowIndex++);
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
            assetIdValue = row[j].trim();
            break;
          }
        }

        if (assetIdValue != null) {
          Optional<Assets> myAssets = assetsRepository.findByAssetIdAndCompanyId(
                  Integer.parseInt(assetIdValue), companyId);
          if (myAssets.isEmpty()) {
            errorFlag = 1;
            errorDesc.append("ASSETID NOT FOUND");
          } else {
            assets = myAssets.get();
          }
        } else {
          errorFlag = 1;
          errorDesc.append("ASSETID COLUMN MISSING OR EMPTY");
        }

        // Process remaining fields
        for (int j = 0; j < row.length && errorFlag == 0; j++) {
          String field = headerMap.get(j);
          if (columnMap.get(field) == null) continue;

          switch (columnMap.get(field).toLowerCase()) {
            case "name":
              String nameValue = row[j] != null ? row[j].trim() : "";
              if (nameValue.isEmpty()) {
                errorDesc.append("Name is mandatory and cannot be empty");
                errorFlag = 1;
              } else {
                assets.setName(nameValue);
              }
              break;
            case "serialnumber":
              assets.setSerialNumber(row[j]);
              break;
            case "category":
              final String categoryValue = row[j].trim();
              List<AssetCategory> cats = assetCategoryRepository.findByCompanyId(companyId)
                      .stream().filter(x -> x.getName().equalsIgnoreCase(categoryValue)).toList();
              if (cats.isEmpty()) {
                errorDesc.append("ERROR IN CATEGORY");
                errorFlag = 1;
              } else {
                assets.setCategory(cats.get(0).getName());
              }
              break;
            case "location":

             if(row[j].trim().isEmpty()) { break; }
              String loc = row[j].trim();
              List<Location> locs = locationList.stream()
                      .filter(l -> l.getName().equalsIgnoreCase(loc)).toList();
              if (!locs.isEmpty()) { assets.setLocation("location:" + locs.get(0).getId()); break; }
              List<Bin> bins = binList.stream()
                      .filter(b -> b.getBinNumber().equalsIgnoreCase(loc)).toList();
              if (!bins.isEmpty()) { assets.setLocation("bin:" + bins.get(0).getId()); break; }
              errorDesc.append("LOCATION"); errorFlag = 1;
              break;
            case "status":
              switch (row[j].toLowerCase()) {
                case "active":
                  assets.setStatus("active");
                  break;
                case "inactive":
                  assets.setStatus("inactive");
                  break;
                case "outofservice":
                  assets.setStatus("outofservice");
                  break;
                default:
                  assets.setStatus("active");
              }
              break;
          }
        }

        if (errorFlag == 0) {
          AssetsDTO updatedAsset = assetsService.addAssets(modelMapper.map(assets, AssetsDTO.class));

          // Handle extra fields for update
          List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
          List<AssetMandatoryFields> mandatoryFieldsList =
              assetMandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId, true);
          Map<String, Boolean> mandatoryExtraFieldsMap = new HashMap<>();
          for (AssetMandatoryFields mf : mandatoryFieldsList) {
            mandatoryExtraFieldsMap.put(mf.getName().toLowerCase(), true);
          }

          // Process extra fields from CSV
          for (int j = 0; j < row.length; j++) {
            String field = headerMap.get(j);
            String value = row[j] != null ? row[j].trim() : "";

            if (columnMap.get(field) == null) continue;

            // Check if this column maps to an extra field
            for (AssetExtraFieldName extraFieldName : extraFieldNames) {
              if (columnMap.get(field).equalsIgnoreCase(extraFieldName.getName())) {
                // Find or create extra field
                Optional<AssetExtraFields> existingExtra =
                    extraFieldsRepository.findByNameAndAssetId(
                        extraFieldName.getName(), updatedAsset.getId());

                AssetExtraFields extraFields = existingExtra.orElse(new AssetExtraFields());
                extraFields.setAssetId(updatedAsset.getId());
                extraFields.setName(extraFieldName.getName());
                extraFields.setType(extraFieldName.getType());

                // Check mandatory constraint
                if (mandatoryExtraFieldsMap.getOrDefault(extraFieldName.getName().toLowerCase(), false)) {
                  if (value.isEmpty()) {
                    errorDesc.append("ERROR WITH ").append(extraFieldName.getName().toUpperCase())
                            .append(" MANDATORY WHILE UPDATING ASSET");
                    errorFlag = 1;
                    break;
                  }
                }

                if (errorFlag == 0) {
                  // Validate and format based on type
                  String formattedValue = value;
                  if ("number".equals(extraFieldName.getType())) {
                    if(value.trim().isEmpty()){
                      extraFields.setValue("");
                      continue;
                    }
                    try {
                      int val = Integer.parseInt(value);
                      formattedValue = Integer.toString(val);
                    } catch (NumberFormatException e) {
                      errorDesc.append("ERROR WHILE UPDATING IN ").append(extraFieldName.getName().toUpperCase())
                              .append(" - Invalid number format");
                      errorFlag = 1;
                      break;
                    }
                  } else if ("date".equals(extraFieldName.getType())) {
                    if(value.trim().isEmpty()){
                      extraFields.setValue("");
                      continue;
                    }
                    try {
                      DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                      LocalDate date = LocalDate.parse(value, inputFormatter);
                      DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                      formattedValue = date.format(outputFormatter);
                    } catch (Exception e) {
                      errorDesc.append("ERROR WHILE UPDATING IN ").append(extraFieldName.getName().toUpperCase())
                              .append(" - Invalid date format (use dd-MM-yyyy)");
                      errorFlag = 1;
                      break;
                    }
                  }

                  extraFields.setValue(formattedValue);
                  extraFields.setCompanyId(companyId);
                  extraFieldsRepository.save(extraFields);
                }
                break;
              }
            }
          }
        }

        if (errorFlag != 0) {
          Row errorRow = errorSheet.createRow(errorRowIndex++);
          for (int col = 0; col < row.length; col++) errorRow.createCell(col).setCellValue(row[col]);
          errorRow.createCell(row.length).setCellValue(errorDesc.toString());
          log.warn("📋 Update error recorded at row {}: {}", errorRowIndex - 1, errorDesc.toString());
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
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

      customerService.addImportHistory(importHistoryDTO);
    log.info("Update import completed for companyId: {}", companyId);
  }

  @Operation(summary = "Upload Image", description = "Endpoint to upload image")
  @PostMapping("/imageUpload")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void uploadImage(@RequestBody AssetImageDTO assetImageDTO) throws Exception {
    assetsService.addImage(assetImageDTO);
  }

  @Operation(summary = "Remove Image", description = "Endpoint to remove image")
  @PostMapping("/removeImage")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void removeImage(@RequestBody String id) throws Exception {
    assetsService.removeImage(id);
  }

  @Operation(summary = "Add New Fields", description = "Endpoint to add new fields")
  @PostMapping("/addfields")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void addNewFields(@RequestBody AssetExtraFieldsDTO extraFieldsDTO) throws Exception {
    assetsService.addExtraFields(extraFieldsDTO);
  }

  @Operation(summary = "Add Extra Field Name", description = "Endpoint to add extra field name")
  @PostMapping("/addExtraFieldName")
  @PreAuthorize("@appSecurity.canCreate(authentication, #extraFieldNameDTO.companyId, 'assets')")
  public void addExtraFieldName(@RequestBody AssetExtraFieldNameDTO extraFieldNameDTO) throws Exception {
    assetsService.addAssetExtraField(extraFieldNameDTO);
  }

  @Operation(summary = "Add Check In Out", description = "Endpoint to add check in out")
  @PostMapping("/addCheckInOut")
  @PreAuthorize("@appSecurity.canCreate(authentication, #checkInDTO.companyId, 'assets')")
  public void addCheckInOut(@RequestBody AssetCheckInDTO checkInDTO) throws NoSubscriptionError {
    assetsService.addCheckInOut(checkInDTO);
  }

  @Operation(summary = "Add Asset File", description = "Endpoint to add asset file")
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

  @Operation(summary = "Mandatory Fields", description = "Endpoint to mandatory fields")
  @PostMapping("/mandatoryFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #mandatoryFields.companyId, 'assets')")
  public void mandatoryFields(@RequestBody AssetMandatoryFields mandatoryFields) throws NoSubscriptionError {
    assetsService.updateMandatoryFields(mandatoryFields);
  }

  @Operation(summary = "Show Fields", description = "Endpoint to show fields")
  @PostMapping("/showFields")
  @PreAuthorize("@appSecurity.canEdit(authentication, #showFields.companyId, 'assets')")
  public void showFields(@RequestBody AssetShowFields showFields) throws NoSubscriptionError {
    assetsService.updateShowFields(showFields);
  }

  @Operation(summary = "Save Qrdata", description = "Endpoint to save qrdata")
  @PostMapping("/saveQRData")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void saveQRData(@RequestBody AssetQR qr) {
    assetsService.qrDataUpdation(qr);
  }

  @Operation(summary = "Update Assets With In Active", description = "Endpoint to update assets with in active")
  @PostMapping("/updateAssetsWithInActive")
  @PreAuthorize("@appSecurity.canEditAny(authentication, 'assets')")
  public void updateAssetsWithInActive(@RequestBody String customerId) throws NoSubscriptionError {
    assetsService.updateAssetsWithInActive(customerId);
  }

  @Operation(summary = "Add Category", description = "Endpoint to add category")
  @PostMapping(value = "/addCategory")
  @PreAuthorize("@appSecurity.canCreate(authentication, #categoryDTO.companyId, 'assets')")
  public void addCategory(@RequestBody CategoryDTO categoryDTO) throws Exception {
    assetsService.addCategory(categoryDTO);
  }

  @Operation(summary = "Add Asset Inspection", description = "Endpoint to add asset inspection")
  @PostMapping(value = "/addAssetInspection")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assetCategoryInspection.companyId, 'assets')")
  public void addAssetInspection(@RequestBody AssetCategoryInspection assetCategoryInspection) throws NoSubscriptionError {
    assetsService.addAssetInspection(assetCategoryInspection);
  }

  @Operation(summary = "Add Asset Inspection Instance", description = "Endpoint to add asset inspection instance")
  @PostMapping(value = "/addAssetInspectionInstance")
  @PreAuthorize("@appSecurity.canCreateAny(authentication, 'assets')")
  public void addAssetInspectionInstance(@RequestBody AssetCategoryInspectionInstance assetCategoryInspection) {
    assetsService.addAssetInspectionInstance(assetCategoryInspection);
  }

  // ─── Update endpoints ─────────────────────────────────────────────────────

  @Operation(summary = "Add Asset Inspection", description = "Endpoint to add asset inspection")
  @PutMapping(value = "/addAssetInspectionInstance")
  @PreAuthorize("@appSecurity.canEdit(authentication, #assetCategoryInspection.companyId, 'assets')")
  public void addAssetInspection(@RequestBody AssetCategoryInspectionInstance assetCategoryInspection) throws NoSubscriptionError {
    assetsService.updateAssetInspectionInstance(assetCategoryInspection);
  }

  @Operation(summary = "Update Asset Inspection", description = "Endpoint to update asset inspection")
  @PutMapping(value = "/updateAssetInspection")
  @PreAuthorize("@appSecurity.canCreate(authentication, #assetCategoryInspection.companyId, 'assets')")
  public void updateAssetInspection(@RequestBody AssetCategoryInspection assetCategoryInspection) throws NoSubscriptionError {
    assetsService.updateAssetInspection(assetCategoryInspection);
    log.info("Update Asset Inspection");
  }

  @Operation(summary = "Update Category", description = "Endpoint to update category")
  @PutMapping(value = "/updateCategory")
  @PreAuthorize("@appSecurity.canEdit(authentication, #categoryDTO.companyId, 'assets')")
  public void updateCategory(@RequestBody CategoryDTO categoryDTO) throws NoSubscriptionError {
    assetsService.updateCategory(categoryDTO);
  }

  @Operation(summary = "Update Extra Field Name", description = "Endpoint to update extra field name")
  @PutMapping("/extraFieldName")
  @PreAuthorize("@appSecurity.canEditAny(authentication, 'assets')")
  public ResponseEntity<AssetExtraFieldName> updateExtraFieldName(
          @RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
    return ResponseEntity.ok(assetsService.updateExtraFieldName(extraFieldNameUpdateDTO));
  }

  // ─── Delete endpoints ─────────────────────────────────────────────────────

  @Operation(summary = "Remove Asset", description = "Endpoint to remove asset")
  @PostMapping("/removeAsset")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void removeAsset(@RequestBody String id) throws Exception {
    assetsService.removeAsset(id);
  }

  @Operation(summary = "Delete Extra Field", description = "Endpoint to delete extra field")
  @DeleteMapping("/deleteExtraFields/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteExtraField(@PathVariable String id) throws Exception {
    assetsService.deleteExtraFields(id);
  }

  @Operation(summary = "Delete Extra Field Name", description = "Endpoint to delete extra field name")
  @DeleteMapping("/deleteExtraFieldName/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteExtraFieldName(@PathVariable String id) throws Exception {
    assetsService.deleteAssetExtraField(id);
  }

  @Operation(summary = "Delete File", description = "Endpoint to delete file")
  @DeleteMapping("deleteFile/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteFile(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteFile(id);
  }

  @Operation(summary = "Delete Category", description = "Endpoint to delete category")
  @DeleteMapping(value = "/deleteCategory/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteCategory(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteCategory(id);
  }

  @Operation(summary = "Delete Asset Inspection", description = "Endpoint to delete asset inspection")
  @DeleteMapping(value = "/deleteAssetInspection/{id}")
  @PreAuthorize("@appSecurity.canDeleteAny(authentication, 'assets')")
  public void deleteAssetInspection(@PathVariable String id) throws NoSubscriptionError {
    assetsService.deleteAssetInspection(id);
  }

  @Operation(summary = "Delete Show And Mandatory Field", description = "Endpoint to delete show and mandatory field")
  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  @PreAuthorize("@appSecurity.canDelete(authentication, #companyId, 'assets')")
  public void deleteShowAndMandatoryField(@PathVariable String name, @PathVariable Long companyId) throws Exception {
    assetsService.deleteShowAndMandatoryFields(companyId, name);
  }

  // ─── Export endpoints ─────────────────────────────────────────────────────

  @Operation(summary = "Export Assets Xlsx", description = "Endpoint to export assets xlsx")
  @GetMapping("/export-asset-xlsx/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> exportAssetsXlsx(@PathVariable Long companyId) throws IOException {
    List<Assets> assets = assetsRepository.findByCompanyId(companyId);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);

    // Fetch all check-in/out records for all assets
    Query q = new Query(Criteria.where("assetId").in(
            assets.stream().map(Assets::getId).collect(Collectors.toList())));
    List<AssetCheckInOut> checkInOutList = mongoTemplate.find(q, AssetCheckInOut.class);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Assets");
    Row header = sheet.createRow(0);
    int col = 0;
    for (String h : new String[]{"ID","Name","AssetId","Category","Customer","CustomerId","Location","Status","Checked In/Out"})
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

      // Add check-in/out status
      List<AssetCheckInOut> matched = checkInOutList.stream()
              .filter(a -> a.getAssetId().equals(asset.getId())).toList();
      String checkedInOutStatus = !matched.isEmpty() ? matched.get(0).getStatus() : "";
      row.createCell(c++).setCellValue(checkedInOutStatus);

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

  @Operation(summary = "Export Assets", description = "Endpoint to export assets")
  @GetMapping("/export-asset/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> exportAssets(@PathVariable Long companyId) throws IOException {
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

    for (String h : new String[]{"ID","Serial Number","Customer","Category","Location","Status","Checked In/Out","Last Handled By","Last Known Location"})
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

      // Add check-in/out status
      String checkedInOutStatus = !matched.isEmpty() ? matched.get(0).getStatus() : "Checked In";
      Cell checkedCell = row.createCell(c++);
      checkedCell.setCellValue(checkedInOutStatus);
      checkedCell.setCellStyle(textStyle);

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

  @Operation(summary = "Export Check In Out", description = "Endpoint to export check in out")
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
      row.createCell(2).setCellValue(
              companyCustomer
                      .map(CompanyCustomer::getCompanyCustomerId)
                      .map(String::valueOf)
                      .orElse("")
      );
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

  // New endpoint: download template containing headers (standard + extra fields)
  @Operation(summary = "Download Asset Template", description = "Endpoint to download asset template")
  @GetMapping("/template-download/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<byte[]> downloadAssetTemplate(@PathVariable Long companyId) throws IOException {
    byte[] content = assetsService.generateAssetTemplateXlsx(companyId);
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=AssetTemplate.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
  }

  // New endpoint: return template fields (standard + extra) as JSON for UI preview
  @Operation(summary = "Get Asset Template Fields", description = "Endpoint to get asset template fields")
  @GetMapping("/template-fields/{companyId}")
  @PreAuthorize("@appSecurity.canView(authentication, #companyId, 'assets')")
  public ResponseEntity<AssetTemplateFieldsDTO> getAssetTemplateFields(@PathVariable Long companyId) {
    AssetTemplateFieldsDTO fields = assetsService.getTemplateFields(companyId);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(fields);
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private Map<String, String> parseColumnMappings(String columnMappings) {
    Map<String, String> map = new HashMap<>();
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      Map<String, String> parsed = objectMapper.readValue(
              columnMappings,
              new TypeReference<Map<String, String>>() {}
      );
      if (parsed != null) {
        parsed.forEach((key, value) -> {
          if (key != null && value != null) {
            map.put(key.trim(), value.trim()); // ✅ trim both key and value
          }
        });
      }
      log.debug("✅ Successfully parsed columnMappings: {}", map);
    } catch (Exception e) {
      log.error("❌ Failed to parse column mappings: {}", columnMappings, e);
    }
    return map;
  }

  /**
   * ✅ Validates if a field value is unique for the given company
   * Checks both standard fields and extra fields
   */
  private boolean isUniqueFieldValid(Long companyId, String fieldName, String fieldValue, String fieldType) {
    if (fieldValue == null || fieldValue.trim().isEmpty()) {
      return true; // Empty values are allowed
    }

    try {
      if ("STANDARD".equalsIgnoreCase(fieldType)) {
        // Check standard fields
        Query query = new Query();
        query.addCriteria(Criteria.where("companyId").is(companyId));

        String fieldNameLower = fieldName.toLowerCase();
        switch (fieldNameLower) {
          case "name":
            query.addCriteria(Criteria.where("name").regex("^" + Pattern.quote(fieldValue) + "$", "i"));
            break;
          case "serialnumber":
            query.addCriteria(Criteria.where("serialNumber").regex("^" + Pattern.quote(fieldValue) + "$", "i"));
            break;
          case "customer":
            query.addCriteria(Criteria.where("customer").regex("^" + Pattern.quote(fieldValue) + "$", "i"));
            break;
          case "category":
            query.addCriteria(Criteria.where("category").regex("^" + Pattern.quote(fieldValue) + "$", "i"));
            break;
          case "location":
            query.addCriteria(Criteria.where("location").regex("^" + Pattern.quote(fieldValue) + "$", "i"));
            break;
          default:
            return true;
        }

        List<Assets> existingAssets = mongoTemplate.find(query, Assets.class);
        return existingAssets.isEmpty();
      } else if ("EXTRA".equalsIgnoreCase(fieldType)) {
        // Check extra fields
        Query extraQuery = new Query();
        extraQuery.addCriteria(Criteria.where("companyId").is(companyId));
        extraQuery.addCriteria(Criteria.where("name").is(fieldName));
        extraQuery.addCriteria(Criteria.where("value").regex("^" + Pattern.quote(fieldValue) + "$", "i"));

        List<AssetExtraFields> existingExtraFields = mongoTemplate.find(extraQuery, AssetExtraFields.class);
        return existingExtraFields.isEmpty();
      }
    } catch (Exception e) {
      log.error("Error validating unique field: {}", e.getMessage(), e);
      return false;
    }

    return true;
  }

  /**
   * ✅ Validates all unique fields for the asset before saving
   * Collects ALL violations (not just the first one) and returns:
   * - true if all valid
   * - false if any duplicates found
   * Highlights all problematic fields and provides comprehensive error message
   */
  private boolean validateAllUniqueFieldsForImport(AssetsDTO assetsDTO, Map<String, String> extraFieldValues,
                                                   Long companyId, StringBuilder errorDesc,
                                                   Map<Integer, Boolean> errorCellMap, Map<Integer, String> headerMap,
                                                   Map<String, String> columnMap) {
    try {
      List<AssetUniqueFieldConfiguration> uniqueConfigs = assetUniqueFieldConfigurationRepository.findByCompanyIdAndIsUniqueTrue(companyId);

      if (uniqueConfigs == null || uniqueConfigs.isEmpty()) {
        log.debug("No unique field configurations found for company: {}", companyId);
        return true;
      }

      boolean hasViolations = false;
      List<String> violationMessages = new ArrayList<>();

      // ✅ Check ALL fields and collect all violations
      for (AssetUniqueFieldConfiguration config : uniqueConfigs) {
        String fieldName = config.getFieldName();
        String fieldType = config.getType();
        String fieldValue = null;
        int cellIndex = -1;

        // Get field value from standard fields or extra fields
        if ("STANDARD".equalsIgnoreCase(fieldType)) {
          switch (fieldName.toLowerCase()) {
            case "name":
              fieldValue = assetsDTO.getName();
              break;
            case "serialnumber":
              fieldValue = assetsDTO.getSerialNumber();
              break;
            case "customer":
              fieldValue = assetsDTO.getCustomer();
              break;
            case "category":
              fieldValue = assetsDTO.getCategory();
              break;
            case "location":
              fieldValue = assetsDTO.getLocation();
              break;
          }
        } else if ("EXTRA".equalsIgnoreCase(fieldType)) {
          fieldValue = extraFieldValues.get(fieldName.toLowerCase());
        }

        // Find cell index for highlighting error
        for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
          String header = entry.getValue();
          if (columnMap.get(header) != null && columnMap.get(header).equalsIgnoreCase(fieldName)) {
            cellIndex = entry.getKey();
            break;
          }
        }

        // Validate uniqueness
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
          boolean isValid = isUniqueFieldValid(companyId, fieldName, fieldValue, fieldType);
          if (!isValid) {
            hasViolations = true;
            String violation = fieldName.toUpperCase() + " (value: '" + fieldValue + "' already exists)";
            violationMessages.add(violation);

            // ✅ Highlight ALL problematic cells
            if (cellIndex >= 0) {
              errorCellMap.put(cellIndex, true);
            }

            log.warn("Unique field validation failed for {}: {}", fieldName, fieldValue);
          }
        }
      }

      // ✅ Build comprehensive error message with all violations
      if (hasViolations) {
        errorDesc.append("DUPLICATE VALUES FOUND: ");
        for (int i = 0; i < violationMessages.size(); i++) {
          errorDesc.append(violationMessages.get(i));
          if (i < violationMessages.size() - 1) {
            errorDesc.append(" | ");
          }
        }
        return false;
      }

    } catch (Exception e) {
      log.error("Error validating unique fields during import: {}", e.getMessage(), e);
      return false;
    }

    return true;
  }

  private long countFileRows(MultipartFile file) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
      return reader.lines().count();
    } catch (IOException e) { return 0; }
  }

  private Map<Integer, String> buildHeaderMap(String[] headers) {
    Map<Integer, String> headerMap = new HashMap<>();
    for (int i = 0; i < headers.length; i++) {
      String header = headers[i]
              .trim()
              .replace("\uFEFF", ""); // ✅ strip BOM wherever it appears
      headerMap.put(i, header);
    }
    return headerMap;
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

  private CellStyle createErrorCellStyle(Workbook workbook) {
    CellStyle errorStyle = workbook.createCellStyle();
    errorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
    errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return errorStyle;
  }
}

