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

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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

  private ModelMapper modelMapper = new ModelMapper();

  @Autowired private CustomerService customerService;

  RestTemplate restTemplate = new RestTemplate();

  @Autowired SubscriptionRepository subscriptionRepository;

  @Autowired AssetCategoryRepository assetCategoryRepository;

  @Autowired AssetCategoryInspectionRepository assetCategoryInspectionRepository;

  @Autowired LocationRepository locationRepository;

  @Autowired BinRepository binRepository;

  @Autowired NotificationService notificationService;

  @Autowired private CompanyCustomerMandatoryFieldsRepository companyCustomerMandatoryFieldsRepository;

  @Autowired private AssetMandatoryFieldsRepository assetMandatoryFieldsRepository;

  @Autowired private UsersRepository usersRepository;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private AssetCheckInOutRepository assetCheckInOutRepository;

  private void checkUserDetailsPermissionFromSpringContext(CustomRoleType customRoleType) throws UserAccessException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    System.out.println("Spring Security"+ authentication.getName());
    Optional<Users> usersOptional=usersRepository.findByEmail(authentication.getName());
    if(usersOptional.isPresent()){
      System.out.println(customRoleType.ordinal()+" "+usersOptional.get().getRole().getAssets().ordinal());
      if(customRoleType.ordinal()>usersOptional.get().getRole().getAssets().ordinal()){
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
    return "Working";
  }

  @GetMapping("/{companyId}")
  public List<AssetsDTO> getAssets(@PathVariable Long companyId) {
    System.out.println("CompanyId--------AssetAPI-------->" + companyId);

    return assetsService.getAssetsDetails(companyId);
  }

  @GetMapping("/getByCutomerId/{customerId}/{pageNumber}")
  public PaginatedResultDTO<String> getAssetsByCustomer(
      @PathVariable String customerId, @PathVariable(required = false) Integer pageNumber) {
    return assetsService.getAssetsDetailsByCustomerId(customerId, pageNumber);
  }

  @PutMapping("/addassets")
  public void addAssets(@RequestBody AssetsDTO assestsDTO) throws NoSubscriptionError, UserAccessException {
    //Optional<Subscription> subscriptionOptional =
    //    subscriptionRepository.findByCompanyIdAndStatus(assestsDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
  //  if (subscriptionOptional.isEmpty()) {
 //     throw new NoSubscriptionError("No Active Subscription");
//    }
    System.out.println("Add AddSets Called");
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    assetsService.addAssets(assestsDTO);
  }

  @PostMapping("/addNewAssets")
  public ResponseEntity<AssetsDTO> addNewAssets(@RequestBody AssetsDTO assestsDTO)
          throws NoSubscriptionError, UserAccessException {
    //Optional<Subscription> subscriptionOptional =
    //    subscriptionRepository.findByCompanyIdAndStatus(assestsDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
  //  if (subscriptionOptional.isEmpty()) {
 //     throw new NoSubscriptionError("No Active Subscription");
//    }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    AssetsDTO assetsDTO = assetsService.addAssets(assestsDTO);
    return ResponseEntity.ok(assetsDTO);
  }

  //    @PostMapping("/import/{companyId}/{email}")
  //    public void  importFile(@RequestParam("file") MultipartFile file,
  // @RequestParam("columnMappings") String columnMappings, @PathVariable Long companyId,
  // @PathVariable String email) throws CsvValidationException,
  // MessagingException,ImportFileRowException {
  //
  //
  //
  //                Map<String, String> columnMap = new HashMap<>();
  //                try {
  //                    // Create a JsonFactory and a JsonParser
  //                    JsonFactory jsonFactory = new JsonFactory();
  //                    JsonParser jsonParser = jsonFactory.createParser(columnMappings);
  //
  //                    // Loop through JSON tokens
  //                    String key = "", val = "";
  //                    while (!jsonParser.isClosed()) {
  //                        // Get the current token
  //                        JsonToken jsonToken = jsonParser.nextToken();
  //                        if (jsonToken == null) {
  //                            break;
  //                        }
  //
  //
  //                        if (key.equals("") == false) {
  //                            columnMap.put(key, val);
  //                        }
  //                        switch (jsonToken) {
  //                            case START_OBJECT:
  //                                //System.out.println("Start of object");
  //                                break;
  //                            case FIELD_NAME:
  //                                //System.out.println("Field name: " +
  // jsonParser.getCurrentName());
  //                                key = jsonParser.getCurrentName();
  //                                break;
  //                            case VALUE_STRING:
  //                                //System.out.println("Field value: " + jsonParser.getText());
  //                                val = jsonParser.getText();
  //
  //                                break;
  //                            case END_OBJECT:
  //                                //System.out.println("End of object");
  //                                break;
  //                            default:
  //                                break;
  //
  //                        }
  //                    }
  //
  //                    // Close the JsonParser
  //                    jsonParser.close();
  //                } catch (Exception e) {
  //                    e.printStackTrace();
  //
  //                }
  //                //System.out.println("--------------> "+columnMap.size());
  //                List<AssetsDTO> assetList = new ArrayList<AssetsDTO>();
  //                long totalCount = 0;
  //                try (InputStream inputStream = file.getInputStream();
  //                     BufferedReader reader = new BufferedReader(new
  // InputStreamReader(inputStream))) {
  //                    totalCount = reader.lines().count();
  //                    if (reader.lines().count() > 5001) {
  //                        throw new ImportFileRowException("Import File cannot import more than
  // 5000 rows");
  //                    }
  //
  //                } catch (IOException e) {
  //                    // Handle IOException
  //                    e.printStackTrace();
  //
  //
  //                }
  //                try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
  //                     CSVReader csvReader = new CSVReader(reader)) {
  //
  //                    String[] headers = csvReader.readNext();
  //                    Map<Integer, String> headerMap = new HashMap<>();
  //
  //                    if (headers != null) {
  //                        for (int i = 0; i < headers.length; i++) {
  //                            headerMap.put(i, headers[i]);
  //                        }
  //                    }
  //
  //
  //                    String[] row;
  //                    long ind = 0;
  //                    Workbook workbook = new XSSFWorkbook();
  //
  //                    // Create a Sheet
  //                    Sheet sheet = workbook.createSheet("Sheet1");
  //                    int excelIndex = 0;
  //                    long processedRow = 0;
  //                    while ((row = csvReader.readNext()) != null) {
  //                        processedRow++;
  //                        Row myrow = sheet.createRow(excelIndex);
  //                        Cell cell1 = myrow.createCell(0);
  //                        cell1.setCellValue("Line " + (int) (ind + 1));
  //                        AssetsDTO assetsDTO = new AssetsDTO();
  //                        assetsDTO.setCompanyId(companyId);
  //                        int errorFlag = 0;
  //                        String errorDesc = "";
  ////	            //System.out.println("-------|||||---errorFlag----> "+errorFlag);
  //                        for (int j = 0; j < row.length; j++) {
  //                            String field = headerMap.get(j);
  ////	                //System.out.println("--------------> "+j+" " + row[j]);
  ////	                //System.out.println("-------|||||------->
  // "+columnMap.get(field).toLowerCase());
  //
  //                            if (errorFlag == 1) {
  //                                break;
  //                            }
  //
  //
  //                            if (columnMap.get(field) != null) {
  //                                switch (columnMap.get(field).toLowerCase()) {
  //
  //
  //                                    case "name":
  //                                        assetsDTO.setName(row[j]);
  //                                        break;
  //
  //                                    case "serialnumber":
  //                                        assetsDTO.setSerialNumber(row[j]);
  //                                        break;
  //                                    case "category":
  //                                        assetsDTO.setCategory(row[j]);
  //                                        break;
  //                                    case "customer":
  //                                        CompanyCustomerDTO myCompanyCustomerDTO =
  // companyCustomerAPI.getCompanyCustomerByLocalId(row[j], companyId);
  //                                        if (myCompanyCustomerDTO == null) {
  //                                            //System.out.println("ERROR WHILE ADDING IN ASSET
  // for row->"+ind);
  //
  //
  //                                            // Create Cells and set values
  //                                            errorDesc += "ERROR WHILE ADDING IN ASSET";
  //
  //
  //                                            errorFlag = 1;
  //                                            break;
  //                                        } else {
  //                                            CompanyCustomerDTO companyCustomerDTO =
  // modelMapper.map(myCompanyCustomerDTO, CompanyCustomerDTO.class);
  //
  //                                            assetsDTO.setCustomerId(companyCustomerDTO.getId());
  //                                            assetsDTO.setCustomer(companyCustomerDTO.getName());
  //
  //                                            break;
  //                                        }
  //                                    case "location":
  //                                        assetsDTO.setLocation(row[j]);
  //                                        break;
  //                                    case "status":
  //
  //                                        if ((row[j].toLowerCase().equals("active")) ||
  // (row[j].toLowerCase().equals("inactive")) || (row[j].toLowerCase().equals("outofservice"))) {
  //
  //                                            assetsDTO.setStatus(row[j]);
  //                                            errorFlag = 0;
  //                                            break;
  //
  //                                        } else {
  //                                            //System.out.println("ERROR WHILE ADDING IN Status
  // for line->"+ind);
  //                                            if (errorDesc.length() > 0) {
  //                                                errorDesc += ", ";
  //                                            }
  //                                            errorDesc += "ERROR WHILE ADDING IN STATUS";
  //                                            errorFlag = 1;
  //                                            break;
  //                                        }
  //
  //
  //                                }
  //                            }
  //
  //
  //                        }
  ////	            //System.out.println("///'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/  reached errorFlag"
  // +errorFlag);
  //                        if (errorFlag == 0) {
  ////	            	//System.out.println("///'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/");
  //                            AssetsDTO mynewAsset = assetsService.addAssets(assetsDTO);
  //
  //                            //-------------------------------------------
  //                            for (int j = 0; j < row.length; j++) {
  //                                String field = headerMap.get(j);
  ////		                //System.out.println("-------|||||------->
  // "+columnMap.get(field).toLowerCase());
  //
  //
  //                                String value = row[j];
  //
  //                                List<AssetExtraFieldName> listExtraFieldName =
  // extraFieldNameRepository.findByCompanyId(companyId);
  //
  //                                for (int i = 0; i < listExtraFieldName.size(); i++) {
  //                                    if
  // (columnMap.get(field).toLowerCase().equals(listExtraFieldName.get(i).getName().toLowerCase()))
  // {
  //                                        //System.out.println("-----------------working
  // ---->"+listExtraFieldName.get(i).getType());
  //                                        AssetExtraFields extraFieldsDTO = new
  // AssetExtraFields();
  //                                        extraFieldsDTO.setAssetId(mynewAsset.getId());
  //
  // extraFieldsDTO.setName(listExtraFieldName.get(i).getName());
  //
  // extraFieldsDTO.setType(listExtraFieldName.get(i).getType());
  //
  //                                        extraFieldsDTO.setCompanyId(companyId);
  //                                        if
  // (listExtraFieldName.get(i).getType().equals("number")) {
  //                                            try {
  //                                                Integer val = Integer.parseInt(value);
  //
  // //System.out.println("-----------------extra---->"+val+"->"+value);
  //                                                extraFieldsDTO.setValue(val.toString());
  //                                            } catch (Exception e) {
  //                                                //System.out.println("ERROR WHILE ADDING EXTRA
  // IN"+ listExtraFieldName.get(i).getName()+" for row->"+ind+1);
  //                                                errorFlag = 1;
  //                                                if (errorDesc.length() > 0) {
  //                                                    errorDesc += ", ";
  //                                                }
  //                                                errorDesc += "ERROR WHILE ADDING IN " +
  // listExtraFieldName.get(i).getName().toUpperCase();
  //                                            }
  //                                        }
  //                                        if (listExtraFieldName.get(i).getType().equals("date"))
  // {
  //                                            try {
  //
  //                                                DateTimeFormatter inputFormatter =
  // DateTimeFormatter.ofPattern("dd-MM-yyyy");
  //                                                LocalDate date = LocalDate.parse(value,
  // inputFormatter);
  //
  //                                                DateTimeFormatter outputFormatter =
  // DateTimeFormatter.ofPattern("yyyy-MM-dd");
  //                                                String formattedDate =
  // date.format(outputFormatter);
  //
  //
  // //System.out.println("-----------------extra-date--->"+formattedDate+"->"+value);
  //                                                extraFieldsDTO.setValue(formattedDate);
  //                                            } catch (Exception e) {
  //                                                //System.out.println("ERROR WHILE ADDING EXTRA
  // IN"+ listExtraFieldName.get(i).getName()+" for row->"+ind+1);
  //                                                errorFlag = 1;
  //                                                if (errorDesc.length() > 0) {
  //                                                    errorDesc += ", ";
  //                                                }
  //                                                errorDesc += "ERROR WHILE ADDING IN " +
  // listExtraFieldName.get(i).getName().toUpperCase();
  //                                            }
  //                                        } else {
  //                                            extraFieldsDTO.setValue(value);
  //                                        }
  //                                        if (errorFlag == 0) {
  //                                            extraFieldsRepository.save(extraFieldsDTO);
  //                                        } else {
  //                                            Assets myAsset = modelMapper.map(mynewAsset,
  // Assets.class);
  //                                            assetsRepository.delete(myAsset);
  //
  //                                        }
  //                                    }
  //                                }
  //
  ////		               listExtraFieldName.stream().forEach((x)->{
  ////
  ////
  ////		               });
  //
  //
  //                            }
  //                        }
  //
  //
  //                        Cell cell2 = myrow.createCell(1);
  //                        cell2.setCellValue(errorDesc);
  //                        if (errorFlag == 1) {
  //                            //System.out.println("Inside errorFLag");
  //
  //                            // Close the workbook to release resources
  //                            excelIndex++;
  //
  //                        }
  //                        ind++;
  //
  //
  //
  //                    }
  //                    if (excelIndex > 0) {
  //                        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
  //                            workbook.write(fileOut);
  //                        }
  //                        workbook.close();
  //                        MimeMessage message = emailSender.createMimeMessage();
  //                        MimeMessageHelper helper = new MimeMessageHelper(message, true);
  //
  //                        helper.setTo(email);
  //                        helper.setSubject("Import Report from AssetYug");
  //                        helper.setText("Hey, We have attached import result below");
  //                        helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
  //
  //                        emailSender.send(message);
  //                    }
  //                    if (excelIndex == 0) {
  //                        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
  //                            workbook.write(fileOut);
  //                        }
  //                        workbook.close();
  //                        MimeMessage message = emailSender.createMimeMessage();
  //                        MimeMessageHelper helper = new MimeMessageHelper(message, true);
  //
  //                        helper.setTo(email);
  //                        helper.setSubject("Import Report from AssetYug");
  //                        helper.setText("Hey, Your Import was Successfully Done");
  //
  //
  ////	            helper.addAttachment("ExcelAttachment.xlsx", new File("Report.xlsx"));
  //
  //                        emailSender.send(message);
  //                    }
  //
  //
  ////	        //System.out.println("-------|||||-------> "+assetList.size());
  ////	        assetsService.importExcel(assetList);
  //
  //                } catch (IOException e) {
  //                    e.printStackTrace();
  //                }
  //
  //
  //
  //    }
//  @PostMapping("/import/{companyId}/{email}")
//  public void importFile(
//      @RequestParam("file") MultipartFile file,
//      @RequestParam("columnMappings") String columnMappings,
//      @PathVariable Long companyId,
//      @PathVariable String email)
//      throws ImportFileRowException, MessagingException, NoSubscriptionError {
//    //    ResponseBodyEmitter emitter = new ResponseBodyEmitter();
//
//    Optional<Subscription> subscriptionOptional =
//        subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
//    List<Location> locationList=locationRepository.findByCompanyId(companyId);
//    List<Bin> binList=binRepository.findByCompanyId(companyId);
//    if (subscriptionOptional.isEmpty()) {
//      throw new NoSubscriptionError("No Active Subscription");
//    }
//    Map<String, String> columnMap = new HashMap<>();
//    try {
//      // Create a JsonFactory and a JsonParser
//      JsonFactory jsonFactory = new JsonFactory();
//      JsonParser jsonParser = jsonFactory.createParser(columnMappings);
//
//      // Loop through JSON tokens
//      String key = "", val = "";
//      while (!jsonParser.isClosed()) {
//        // Get the current token
//        JsonToken jsonToken = jsonParser.nextToken();
//        if (jsonToken == null) {
//          break;
//        }
//
//        if (key.equals("") == false) {
//          columnMap.put(key, val);
//        }
//        switch (jsonToken) {
//          case START_OBJECT:
//            // System.out.println("Start of object");
//            break;
//          case FIELD_NAME:
//            // System.out.println("Field name: " +
//            //     jsonParser.getCurrentName());
//            key = jsonParser.getCurrentName();
//            break;
//          case VALUE_STRING:
//            // System.out.println("Field value: " + jsonParser.getText());
//            val = jsonParser.getText();
//
//            break;
//          case END_OBJECT:
//            // System.out.println("End of object");
//            break;
//          default:
//            break;
//        }
//      }
//
//      // Close the JsonParser
//      jsonParser.close();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//    // System.out.println("--------------> "+columnMap.size());
//    List<AssetsDTO> assetList = new ArrayList<AssetsDTO>();
//    long totalCount = 0;
//    try (InputStream inputStream = file.getInputStream();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//      totalCount = reader.lines().count();
//      if (reader.lines().count() > 5001) {
//        throw new ImportFileRowException("Import File cannot import more than 5000 rows");
//      }
//
//    } catch (IOException e) {
//      // Handle IOException
//      e.printStackTrace();
//    }
//    ImportHistory importHistoryDTO = new ImportHistory();
//    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
//        CSVReader csvReader = new CSVReader(reader)) {
//
//      String[] headers = csvReader.readNext();
//      Map<Integer, String> headerMap = new HashMap<>();
//
//      if (headers != null) {
//        for (int i = 0; i < headers.length; i++) {
//          headerMap.put(i, headers[i]);
//        }
//      }
//
//      String[] row;
//      long ind = 0;
//      Workbook workbook = new XSSFWorkbook();
//      int currCount = 0;
//      importHistoryDTO.setFileName(file.getOriginalFilename());
//      importHistoryDTO.setRecordType("Add Asset Record");
//      importHistoryDTO.setExecutedBy(email);
//      importHistoryDTO.setDate(LocalDateTime.now().toString());
//      importHistoryDTO.setStatus("In-Progress");
//      importHistoryDTO.setCompanyId(companyId);
//      // Create a Sheet
//      Sheet sheet = workbook.createSheet("Sheet1");
//      int excelIndex = 0;
//      long processedRow = 0;
//      while ((row = csvReader.readNext()) != null) {
//
//        processedRow++;
//        boolean isEmpty = Arrays.stream(row)
//                .map(cell -> cell == null ? "" : cell.trim())
//                .allMatch(String::isEmpty);
//
//        if (isEmpty) {
//          continue; // Skip this row
//        }
//        Row myrow = sheet.createRow(excelIndex);
//        Cell cell1 = myrow.createCell(0);
//        cell1.setCellValue("Line " + (int) (ind + 1));
//        AssetsDTO assetsDTO = new AssetsDTO();
//        assetsDTO.setCompanyId(companyId);
//        int errorFlag = 0;
//        StringBuilder errorDesc = new StringBuilder();
//
//        for (int j = 0; j < row.length; j++) {
//
//
//
//          String field = headerMap.get(j);
//
//          if (errorFlag == 1) {
//            break;
//          }
//
//          if (columnMap.get(field) != null) {
//              label:
//              switch (columnMap.get(field).toLowerCase()) {
//                case "name":
//                  assetsDTO.setName(row[j]);
//                  break;
//
//                case "serialnumber":
//
//                  assetsDTO.setSerialNumber(row[j]);
//                  break;
//                case "category":
//                  List<AssetCategory> categoryList=assetCategoryRepository.findByCompanyId(companyId);
//                  String rowValue=row[j];
//                  if(!rowValue.trim().isBlank()){
//                    List<AssetCategory> list=categoryList.stream().filter(x-> x.getName().equalsIgnoreCase(rowValue.trim())).toList();
//                    if(list.isEmpty()){
//                      errorDesc.append("ERROR IN CATEGORY WHILE ADDING IN ASSET");
////                      System.out.println("ERROR IN CATEGORY WHILE ADDING IN ASSET");
//                      errorFlag = 1;
//                      break;
//                    }
//                    else{
//                      assetsDTO.setCategory(list.get(0).getName());
//                    }
//                  }
//
//
//                  break;
//                case "customer":
//                  CompanyCustomerDTO myCompanyCustomerDTO =
//                      companyCustomerAPI.getCompanyCustomerByLocalId(row[j], companyId);
//                  if (myCompanyCustomerDTO == null) {
//                    // System.out.println("ERROR WHILE ADDING IN ASSET
//                    //     for row->"+ind);
//
//                    // Create Cells and set values
//                    errorDesc.append("ERROR IN CUSTOMER ID WHILE ADDING IN ASSET");
//
//                    errorFlag = 1;
//                    break;
//                  } else {
//                    CompanyCustomerDTO companyCustomerDTO =
//                        modelMapper.map(myCompanyCustomerDTO, CompanyCustomerDTO.class);
//
//                    assetsDTO.setCustomerId(companyCustomerDTO.getId());
//                    assetsDTO.setCustomer(companyCustomerDTO.getName());
//
//                    break;
//                  }
//                case "location":
//                    String myLocation=row[j].trim();
//                    List<Location> selectedLocationList=locationList.stream().filter(loc->loc.getName().equalsIgnoreCase(myLocation)).toList();
//                    if(!selectedLocationList.isEmpty()){
//                        assetsDTO.setLocation("location:"+selectedLocationList.get(0).getId());
//                        break;
//                    }
//                    List<Bin> selectedBinList=binList.stream().filter(bin->bin.getBinNumber().equalsIgnoreCase(myLocation)).toList();
//                    if(!selectedBinList.isEmpty()){
//                        assetsDTO.setLocation("bin:"+selectedBinList.get(0).getId());
//                        break;
//                    }
//                    errorDesc.append("ERROR WHILE ADDING IN LOCATION");
//
//                    errorFlag = 1;
//                    break;
//
//                case "status":
//                    switch (row[j].toLowerCase()) {
//                        case "active":
//                            assetsDTO.setStatus("active");
//                            errorFlag = 0;
//                            break label;
//                        case "inactive":
//                            assetsDTO.setStatus("inActive");
//                            errorFlag = 0;
//                            break label;
//                        case "outofservice":
//                            assetsDTO.setStatus("outOfService");
//                            errorFlag = 0;
//                            break label;
//                        default:
//                            // System.out.println("ERROR WHILE ADDING IN Status
//                            //     for line->"+ind);
//                            if (!errorDesc.isEmpty()) {
//                                errorDesc.append(", ");
//                            }
//                            errorDesc.append("ERROR WHILE ADDING IN STATUS");
//                            errorFlag = 1;
//                            break label;
//                    }
//              }
//          }
//        }
//        if (errorFlag == 0) {
//          //	            	//System.out.println("///'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/");
//          AssetsDTO mynewAsset = assetsService.addAssets(assetsDTO);
//
//          // -------------------------------------------
//          for (int j = 0; j < row.length; j++) {
//            String field = headerMap.get(j);
//            //		                //System.out.println("-------|||||------->
//            //     "+columnMap.get(field).toLowerCase());
//
//            String value = row[j];
//
//            List<AssetExtraFieldName> listExtraFieldName =
//                extraFieldNameRepository.findByCompanyId(companyId);
////            System.out.println("626->" + columnMap);
////            System.out.println("627->" + field);
//            for (AssetExtraFieldName assetExtraFieldName : listExtraFieldName) {
//              if (columnMap.containsKey(field)
//                  && columnMap.get(field).equalsIgnoreCase(assetExtraFieldName.getName())) {
//                // System.out.println("-----------------working
//                //     ---->"+listExtraFieldName.get(i).getType());
//                AssetExtraFields extraFieldsDTO = new AssetExtraFields();
//                extraFieldsDTO.setAssetId(mynewAsset.getId());
//
//                extraFieldsDTO.setName(assetExtraFieldName.getName());
//
//                extraFieldsDTO.setType(assetExtraFieldName.getType());
//
//                extraFieldsDTO.setCompanyId(companyId);
//                if (assetExtraFieldName.getType().equals("number")) {
//                  try {
//                    int val = Integer.parseInt(value);
//
//                    // System.out.println("-----------------extra---->"+val+"->"+value);
//                    extraFieldsDTO.setValue(Integer.toString(val));
//                  } catch (Exception e) {
//                    // System.out.println("ERROR WHILE ADDING EXTRA
//                    //     IN"+ listExtraFieldName.get(i).getName()+" for row->"+ind+1);
//                    errorFlag = 1;
//                    if (!errorDesc.isEmpty()) {
//                      errorDesc.append(", ");
//                    }
//                    errorDesc
//                        .append("ERROR WHILE ADDING IN ")
//                        .append(assetExtraFieldName.getName().toUpperCase());
//                  }
//                }
//                if (assetExtraFieldName.getType().equals("date")) {
//                  try {
//
//                    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//                    LocalDate date = LocalDate.parse(value, inputFormatter);
//
//                    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//                    String formattedDate = date.format(outputFormatter);
//
//                    // System.out.println("-----------------extra-date--->"+formattedDate+"->"+value);
//                    extraFieldsDTO.setValue(formattedDate);
//                  } catch (Exception e) {
//                    // System.out.println("ERROR WHILE ADDING EXTRA
//                    //     IN"+ listExtraFieldName.get(i).getName()+" for row->"+ind+1);
//                    errorFlag = 1;
//                    if (!errorDesc.isEmpty()) {
//                      errorDesc.append(", ");
//                    }
//                    errorDesc
//                        .append("ERROR WHILE ADDING IN ")
//                        .append(assetExtraFieldName.getName().toUpperCase());
//                  }
//                } else {
//                  extraFieldsDTO.setValue(value);
//                }
//                if (errorFlag == 0) {
//                  extraFieldsRepository.save(extraFieldsDTO);
//                } else {
//                  Assets myAsset = modelMapper.map(mynewAsset, Assets.class);
//                  assetsRepository.delete(myAsset);
//                }
//              }
//            }
//          }
//        }
//
//        Cell cell2 = myrow.createCell(1);
//        cell2.setCellValue(errorDesc.toString());
//        if (errorFlag == 1) {
//          // System.out.println("Inside errorFLag");
//
//          // Close the workbook to release resources
//          excelIndex++;
//        }
//        ind++;
//        currCount++;
//        importHistoryDTO.setDate(LocalDateTime.now().toString());
//        long complete = (currCount * 100L) / (totalCount-1);
//        importHistoryDTO.setComplete(complete);
//        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
//      }
//      if (excelIndex > 0) {
//        importHistoryDTO.setMessage("We have sent import result via email");
//        Sheet mySheet = workbook.getSheetAt(0);
//
//        // Get last row index (0-based)
//        int lastRowNum = mySheet.getLastRowNum();
//
//        if (lastRowNum >= 0) {
//          Row lastRow = mySheet.getRow(lastRowNum);
//          if (lastRow != null) {
//            mySheet.removeRow(lastRow);
//          }
//        }
//        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
//          workbook.write(fileOut);
//        }
//        workbook.close();
//        MimeMessage message = emailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//        helper.setTo(email);
//        helper.setSubject("Import Report from AssetYug");
//        helper.setText("Hey, We have attached import result below");
//        helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
//
//        emailSender.send(message);
//      }
//      if (excelIndex == 0) {
//        importHistoryDTO.setMessage("Import was Successfully Done");
//        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
//          workbook.write(fileOut);
//        }
//        workbook.close();
//        MimeMessage message = emailSender.createMimeMessage();
//        MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//        helper.setTo(email);
//        helper.setSubject("Import Report from AssetYug");
//        helper.setText("Hey, Your Import was Successfully Done");
//
//        //	            helper.addAttachment("ExcelAttachment.xlsx", new File("Report.xlsx"));
//
//        emailSender.send(message);
//      }
//
//      //	        //System.out.println("-------|||||-------> "+assetList.size());
//      //	        assetsService.importExcel(assetList);
//      importHistoryDTO.setStatus("Completed");
//    } catch (IOException | CsvValidationException e) {
//      importHistoryDTO.setStatus("Failed");
//      importHistoryDTO.setMessage(e.getMessage());
//      e.printStackTrace();
//    }
//    customerService.addImportHistory(importHistoryDTO);
//    Notification notification=new Notification();
//    notification.setNotificationType(NotificationType.COMPANY);
//    notification.setMessage("Assets have been successfully imported from file: " + file.getOriginalFilename());
//    notification.setTitle("Asset Import");
//    notification.setCreatedAt(LocalDateTime.now());
//    notificationService.sendNotificationToCompany(companyId,notification);
//    log.info("Successfully Import");
//  }

@PostMapping("/import/{companyId}/{email}")
public void importFile(
        @RequestParam("file") MultipartFile file,
        @RequestParam("columnMappings") String columnMappings,
        @PathVariable Long companyId,
        @PathVariable String email)
        throws ImportFileRowException, MessagingException, NoSubscriptionError, UserAccessException {

  // Optional<Subscription> subscriptionOptional =
  //         subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
  //         if (subscriptionOptional.isEmpty()) {
  //           throw new NoSubscriptionError("No Active Subscription");
  //         }
  checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
  List<Location> locationList = locationRepository.findByCompanyId(companyId);
  List<Bin> binList = binRepository.findByCompanyId(companyId);

  List<AssetMandatoryFields> assetMandatoryList= assetMandatoryFieldsRepository.findByCompanyIdAndMandatory(companyId,true);
  
  List<String> mandatoryColumnList=assetMandatoryList.stream().map(ele->ele.getName().toLowerCase()).toList();
  System.out.println("=================> MandatoryList----->"+mandatoryColumnList.toString());
  

  // Parse columnMappings JSON into a map
  Map<String, String> columnMap = new HashMap<>();
  try {
    JsonFactory jsonFactory = new JsonFactory();
    JsonParser jsonParser = jsonFactory.createParser(columnMappings);
    String key = "", val = "";
    while (!jsonParser.isClosed()) {
      JsonToken jsonToken = jsonParser.nextToken();
      if (jsonToken == null) break;

      if (!key.equals("")) {
        columnMap.put(key, val);
      }
      switch (jsonToken) {
        case FIELD_NAME:
          key = jsonParser.getCurrentName();
          break;
        case VALUE_STRING:
          val = jsonParser.getText();
          break;
        default:
          break;
      }
    }
    jsonParser.close();
  } catch (Exception e) {
    e.printStackTrace();
  }

  long totalCount = 0;
  try (InputStream inputStream = file.getInputStream();
       BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
    totalCount = reader.lines().count();
    if (totalCount > 1001) {
      throw new ImportFileRowException("Import File cannot import more than 1000 rows");
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

    // Workbook for error rows
    Workbook workbook = new XSSFWorkbook();
    Sheet errorSheet = workbook.createSheet("Errors");
    int excelIndex = 0;

    // Add header row to error sheet
    Row headerRow = errorSheet.createRow(excelIndex++);
    for (int i = 0; i < headers.length; i++) {
      headerRow.createCell(i).setCellValue(headers[i]);
    }
    headerRow.createCell(headers.length).setCellValue("Error");

    long processedRow = 0;
    int currCount = 0;
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType("Add Asset Record");
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now().toString());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);

    String[] row;
    while ((row = csvReader.readNext()) != null) {
      processedRow++;
      boolean isEmpty = Arrays.stream(row)
              .map(cell -> cell == null ? "" : cell.trim())
              .allMatch(String::isEmpty);
      if (isEmpty) continue;

      AssetsDTO assetsDTO = new AssetsDTO();
      assetsDTO.setCompanyId(companyId);
      int errorFlag = 0;
      StringBuilder errorDesc = new StringBuilder();

      // Validate and set fields
      for (int j = 0; j < row.length; j++) {
        String field = headerMap.get(j);
        if (errorFlag == 1) break;

        if (columnMap.get(field) != null) {
          label:
          switch (columnMap.get(field).toLowerCase()) {
            case "name":
              assetsDTO.setName(row[j]);
              break;
            case "serialnumber":
              if(mandatoryColumnList.contains("serialnumber")&&row[j].trim().isEmpty()){
                errorDesc.append("Serial Number Mandatory");
                errorFlag = 1;
                break;
              }
              assetsDTO.setSerialNumber(row[j]);
              break;
            case "category":
              System.out.println("=====================---------------->>>>>"+mandatoryColumnList.contains("category")+" "+row[j].trim()+" "+row[j].trim().isEmpty()+" "+row[j].trim().isBlank());
              if(mandatoryColumnList.contains("category")&&row[j].trim().isEmpty()){
                errorDesc.append("Category is Mandatory");
                errorFlag = 1;
                break;
              }
              List<AssetCategory> categoryList = assetCategoryRepository.findByCompanyId(companyId);
              String rowValue = row[j];
              if (!rowValue.trim().isBlank()) {
                List<AssetCategory> list = categoryList.stream()
                        .filter(x -> x.getName().equalsIgnoreCase(rowValue.trim()))
                        .toList();
                if (list.isEmpty()) {
                  errorDesc.append("CATEGORY");
                  errorFlag = 1;
                  break;
                } else {
                  assetsDTO.setCategory(list.get(0).getName());
                }
              }
              break;
            case "customer":
              if(mandatoryColumnList.contains("customer")&&row[j].trim().isEmpty()){
                errorDesc.append("Customer is Mandatory");
                errorFlag = 1;
                break;
              }
              CompanyCustomerDTO myCompanyCustomerDTO =
                      companyCustomerAPI.getCompanyCustomerByLocalId(row[j], companyId);
              if (myCompanyCustomerDTO == null) {
                errorDesc.append("CUSTOMER ID");
                errorFlag = 1;
                break;
              } else {
                CompanyCustomerDTO companyCustomerDTO =
                        modelMapper.map(myCompanyCustomerDTO, CompanyCustomerDTO.class);
                assetsDTO.setCustomerId(companyCustomerDTO.getId());
                assetsDTO.setCustomer(companyCustomerDTO.getName());
              }
              break;
            case "location":
            if(mandatoryColumnList.contains("location")&&row[j].trim().isEmpty()){
              errorDesc.append("Location is Mandatory");
              errorFlag = 1;
              break;
            }
              String myLocation = row[j].trim();
              List<Location> selectedLocationList = locationList.stream()
                      .filter(loc -> loc.getName().equalsIgnoreCase(myLocation))
                      .toList();
              if (!selectedLocationList.isEmpty()) {
                assetsDTO.setLocation("location:" + selectedLocationList.get(0).getId());
                break;
              }
              List<Bin> selectedBinList = binList.stream()
                      .filter(bin -> bin.getBinNumber().equalsIgnoreCase(myLocation))
                      .toList();
              if (!selectedBinList.isEmpty()) {
                assetsDTO.setLocation("bin:" + selectedBinList.get(0).getId());
                break;
              }
              errorDesc.append("LOCATION");
              errorFlag = 1;
              break;
            case "status":
              switch (row[j].toLowerCase()) {
                case "active":
                  assetsDTO.setStatus("active");
                  break label;
                case "inactive":
                  assetsDTO.setStatus("inActive");
                  break label;
                case "outofservice":
                  assetsDTO.setStatus("outOfService");
                  break label;
                default:
                  errorDesc.append("STATUS");
                  errorFlag = 1;
                  break label;
              }
          }
        }
      }

      if (errorFlag == 1) {
        Row errorRow = errorSheet.createRow(excelIndex++);
        for (int k = 0; k < row.length; k++) {
          errorRow.createCell(k).setCellValue(row[k]);
        }
        errorRow.createCell(row.length).setCellValue(errorDesc.toString());
      } else {
        AssetsDTO mynewAsset = assetsService.addAssets(assetsDTO);
        // Save extra fields if any...
      }

      currCount++;
      importHistoryDTO.setDate(LocalDateTime.now().toString());
      long complete = (currCount * 100L) / (totalCount - 1);
      importHistoryDTO.setComplete(complete);
      importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
    }

    // Save Excel and send email
    try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
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
    MimeMessage message = emailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true);
    helper.setTo(email);
    helper.setSubject("Asset Import Results - AssetYug");
    if (excelIndex > 1) {
      importHistoryDTO.setMessage("We have sent import result via email");
      helper.setText("Hi "+subjectName+",\n" +
              "\n" +
              "Your import has been completed. Please check the attached file for the errors that need correction. Once fixed, please reupload only the data listed in the file.\n" +
              "\n" +
              "If you need any help or additional information, feel free to reach out.\n" +
              "\n" +
              "Best regards,\n" +
              "Asset Yug Team");

      helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
    } else {
      importHistoryDTO.setMessage("Import was Successfully Done");
      helper.setText("Hi "+subjectName+",\n" +
              "\n" +
              "Your import has been completed successfully. All data has been processed and is now available in the system.\n" +
              "\n" +
              "Best regards,\n" +
              "AssetYug Team");
    }
    emailSender.send(message);

    importHistoryDTO.setStatus("Completed");

  } catch (IOException | CsvValidationException e) {
    importHistoryDTO.setStatus("Failed");
    importHistoryDTO.setMessage(e.getMessage());
    e.printStackTrace();
  }

  customerService.addImportHistory(importHistoryDTO);

  Notification notification = new Notification();
  notification.setNotificationType(NotificationType.COMPANY);
  notification.setMessage("Assets have been successfully imported from file: " + file.getOriginalFilename());
  notification.setTitle("Asset Import");
  notification.setCreatedAt(LocalDateTime.now());
  notificationService.sendNotificationToCompany(companyId, notification);

  log.info("Successfully Import");
}

//  @PostMapping("/importUpdation/{companyId}/{email}")
//  public void updateAssetWithFile(
//          @RequestParam("file") MultipartFile file,
//          @RequestParam("columnMappings") String columnMappings,
//          @PathVariable Long companyId,
//          @PathVariable String email)
//          throws CsvValidationException,
//          JsonParseException,
//          IOException,
//          MessagingException,
//          ImportFileRowException,
//          NoSubscriptionError {
//
//    Optional<Subscription> subscriptionOptional =
//            subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
//    if (subscriptionOptional.isEmpty()) {
//      throw new NoSubscriptionError("No Active Subscription");
//    }
//
//    List<Location> locationList = locationRepository.findByCompanyId(companyId);
//    List<Bin> binList = binRepository.findByCompanyId(companyId);
//
//    // Parse JSON column mappings
//    Map<String, String> columnMap = new HashMap<>();
//    try {
//      JsonFactory jsonFactory = new JsonFactory();
//      JsonParser jsonParser = jsonFactory.createParser(columnMappings);
//      String key = "", val = "";
//      while (!jsonParser.isClosed()) {
//        JsonToken jsonToken = jsonParser.nextToken();
//        if (jsonToken == null) break;
//        if (!key.equals("")) {
//          columnMap.put(key, val);
//        }
//        switch (jsonToken) {
//          case FIELD_NAME:
//            key = jsonParser.getCurrentName();
//            break;
//          case VALUE_STRING:
//            val = jsonParser.getText();
//            break;
//          default:
//            break;
//        }
//      }
//      jsonParser.close();
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//
//    // Count file rows
//    long totalCount = 0;
//    try (InputStream inputStream = file.getInputStream();
//         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
//      totalCount = reader.lines().count();
//      if (totalCount > 5001) {
//        throw new ImportFileRowException("Import File cannot import more than 5000 rows");
//      }
//    }
//
//    ImportHistory importHistoryDTO = new ImportHistory();
//    importHistoryDTO.setFileName(file.getOriginalFilename());
//    importHistoryDTO.setRecordType("Updated Asset Record");
//    importHistoryDTO.setExecutedBy(email);
//    importHistoryDTO.setDate(LocalDateTime.now().toString());
//    importHistoryDTO.setStatus("In-Progress");
//    importHistoryDTO.setCompanyId(companyId);
//
//    long currCount = 0;
//
//    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
//         CSVReader csvReader = new CSVReader(reader)) {
//
//      String[] headers = csvReader.readNext();
//      Map<Integer, String> headerMap = new HashMap<>();
//      if (headers != null) {
//        for (int i = 0; i < headers.length; i++) {
//          headerMap.put(i, headers[i]);
//        }
//      }
//
//      Workbook workbook = new XSSFWorkbook();
//
//      // ===== NEW ERROR SHEET LOGIC =====
//      Sheet errorSheet = workbook.createSheet("Errors");
//      Row errorHeaderRow = errorSheet.createRow(0);
//      for (int i = 0; i < headers.length; i++) {
//        errorHeaderRow.createCell(i).setCellValue(headers[i]);
//      }
//      errorHeaderRow.createCell(headers.length).setCellValue("Error");
//      int errorRowIndex = 1; // start from second row in error sheet
//
//      // Main processing loop
//      String[] row;
//      long ind = 0;
//      while ((row = csvReader.readNext()) != null) {
//        boolean isEmpty = Arrays.stream(row)
//                .map(cell -> cell == null ? "" : cell.trim())
//                .allMatch(String::isEmpty);
//        if (isEmpty) continue;
//
//        int errorFlag = 0;
//        StringBuilder errorDesc = new StringBuilder();
//        Assets assets = new Assets();
//
//        // Extract assetId
//        String assetIdValue = null;
//        for (int j = 0; j < row.length; j++) {
//          String field = headerMap.get(j);
//          if ("assetid".equalsIgnoreCase(columnMap.get(field))) {
//            assetIdValue = row[j].trim();
//            break;
//          }
//        }
//
//        if (assetIdValue != null) {
//          Optional<Assets> myAssets = assetsRepository.findByAssetIdAndCompanyId(
//                  Integer.parseInt(assetIdValue), companyId);
//          if (myAssets.isEmpty()) {
//            errorFlag = 1;
//            errorDesc.append("ERROR WHILE UPDATING: ASSETID NOT FOUND");
//          }
//        } else {
//          errorFlag = 1;
//          errorDesc.append("ASSETID COLUMN MISSING OR EMPTY");
//        }
//
//        // Process other mapped fields (category, customer, location, status, etc.)
//        // ... (keep your existing field mapping & validation logic here) ...
//
//        // ===== NEW ERROR HANDLING =====
//        if (errorFlag == 1) {
//          Row errorRow = errorSheet.createRow(errorRowIndex++);
//          for (int k = 0; k < row.length; k++) {
//            errorRow.createCell(k).setCellValue(row[k]);
//          }
//          errorRow.createCell(headers.length).setCellValue(errorDesc.toString());
//        }
//
//        // Progress tracking
//        currCount++;
//        importHistoryDTO.setDate(LocalDateTime.now().toString());
//        long complete = (currCount * 100L) / (totalCount - 1);
//        importHistoryDTO.setComplete(complete);
//        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
//        ind++;
//      }
//
//      // Save workbook with Errors sheet
//      try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
//        workbook.write(fileOut);
//      }
//      workbook.close();
//
//      // Send email with attachment
//      MimeMessage message = emailSender.createMimeMessage();
//      MimeMessageHelper helper = new MimeMessageHelper(message, true);
//      helper.setTo(email);
//      helper.setSubject("Import Report from AssetYug");
//      helper.setText("Hey, We have attached import result below");
//      helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
//      emailSender.send(message);
//
//      importHistoryDTO.setStatus("Completed");
//
//    } catch (IOException e) {
//      importHistoryDTO.setStatus("Failed");
//      importHistoryDTO.setMessage(e.getMessage());
//      e.printStackTrace();
//    }
//
//    customerService.addImportHistory(importHistoryDTO);
//    log.info("Successfully Update Import");
//  }


  @PostMapping("/importUpdation/{companyId}/{email}")
  public void updateAssetWithFile(
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

    // Optional<Subscription> subscriptionOptional =
    //         subscriptionRepository.findByCompanyIdAndStatus(companyId, SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    System.out.println("------||-------->" + columnMappings);

    List<Location> locationList = locationRepository.findByCompanyId(companyId);
    List<Bin> binList = binRepository.findByCompanyId(companyId);

    Map<String, String> columnMap = new HashMap<>();
    try {
      JsonFactory jsonFactory = new JsonFactory();
      JsonParser jsonParser = jsonFactory.createParser(columnMappings);

      String key = "", val = "";
      while (!jsonParser.isClosed()) {
        JsonToken jsonToken = jsonParser.nextToken();
        if (jsonToken == null) break;

        if (!key.equals("")) {
          columnMap.put(key, val);
        }
        switch (jsonToken) {
          case FIELD_NAME:
            key = jsonParser.getCurrentName();
            break;
          case VALUE_STRING:
            val = jsonParser.getText();
            break;
          default:
            break;
        }
      }
      jsonParser.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    long totalCount = 0;
    try (InputStream inputStream = file.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      totalCount = reader.lines().count();
      if (totalCount > 1001) {
        throw new ImportFileRowException("Import File cannot import more than 1000 rows");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    ImportHistory importHistoryDTO = new ImportHistory();
    importHistoryDTO.setFileName(file.getOriginalFilename());
    importHistoryDTO.setRecordType("Updated Asset Record");
    importHistoryDTO.setExecutedBy(email);
    importHistoryDTO.setDate(LocalDateTime.now().toString());
    importHistoryDTO.setStatus("In-Progress");
    importHistoryDTO.setCompanyId(companyId);

    long currCount = 0;

    try (InputStreamReader reader = new InputStreamReader(file.getInputStream());
         CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = csvReader.readNext();
      Map<Integer, String> headerMap = new HashMap<>();

      if (headers != null) {
        for (int i = 0; i < headers.length; i++) {
          headerMap.put(i, headers[i]);
        }
      }

      // ✅ NEW: Create workbook and error sheet
      Workbook workbook = new XSSFWorkbook();
      Sheet errorSheet = workbook.createSheet("Error Report");

      // ✅ NEW: Create header row in error sheet
      if (headers != null) {
        Row errorHeaderRow = errorSheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
          errorHeaderRow.createCell(i).setCellValue(headers[i]);
        }
        errorHeaderRow.createCell(headers.length).setCellValue("Error");
      }

      String[] row;
      long ind = 0;

      while ((row = csvReader.readNext()) != null) {
        boolean isEmpty = Arrays.stream(row)
                .map(cell -> cell == null ? "" : cell.trim())
                .allMatch(String::isEmpty);
        if (isEmpty) {
          continue;
        }

        int errorFlag = 0;
        StringBuilder errorDesc = new StringBuilder();
        Assets assets = new Assets();

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
            errorDesc.append("ERROR WHILE UPDATING: ASSETID NOT FOUND");
          } else {
            assets = myAssets.get();
          }
        } else {
          errorFlag = 1;
          errorDesc.append("ASSETID COLUMN MISSING OR EMPTY");
        }

        for (int j = 0; j < row.length; j++) {
          if (j > 0 && assets == null) {
            break;
          }
          String field = headerMap.get(j);

          if (columnMap.get(field) != null) {
            switch (columnMap.get(field).toLowerCase()) {
              case "name":
                assets.setName(row[j]);
                break;
              case "serialnumber":
                assets.setSerialNumber(row[j]);
                break;
              case "category":
                List<AssetCategory> categoryList = assetCategoryRepository.findByCompanyId(companyId);
                String rowValue = row[j];
                if (!rowValue.trim().isEmpty()) {
                  List<AssetCategory> list = categoryList.stream()
                          .filter(x -> x.getName().equalsIgnoreCase(rowValue))
                          .toList();
                  if (list.isEmpty()) {
                    errorDesc.append("ERROR IN CATEGORY");
                  } else {
                    assets.setCategory(list.get(0).getName());
                  }
                }
                break;
              case "customer":
                CompanyCustomerDTO myCompanyCustomerDTO =
                        companyCustomerAPI.getCompanyCustomerByLocalId(row[j], companyId);
                if (myCompanyCustomerDTO == null) {
                  errorDesc.append("ERROR IN CUSTOMER ID");
                  errorFlag = 1;
                } else {
                  CompanyCustomerDTO companyCustomerDTO =
                          modelMapper.map(myCompanyCustomerDTO, CompanyCustomerDTO.class);
                  assets.setCustomerId(companyCustomerDTO.getId());
                  assets.setCustomer(companyCustomerDTO.getName());
                }
                break;
              case "location":
                String myLocation = row[j].trim();
                List<Location> selectedLocationList = locationList.stream()
                        .filter(loc -> loc.getName().equalsIgnoreCase(myLocation))
                        .toList();
                if (!selectedLocationList.isEmpty()) {
                  assets.setLocation("location:" + selectedLocationList.get(0).getId());
                  break;
                }
                List<Bin> selectedBinList = binList.stream()
                        .filter(bin -> bin.getBinNumber().equalsIgnoreCase(myLocation))
                        .toList();
                if (!selectedBinList.isEmpty()) {
                  assets.setLocation("bin:" + selectedBinList.get(0).getId());
                  break;
                }
                errorDesc.append("ERROR IN LOCATION");
                errorFlag = 1;
                break;
              case "status":
                switch (row[j].toLowerCase()) {
                  case "active":
                    assets.setStatus("active");
                    break;
                  case "inactive":
                    assets.setStatus("inActive");
                    break;
                  case "outofservice":
                    assets.setStatus("outOfService");
                    break;
                  default:
                    errorDesc.append("ERROR IN STATUS");
                    errorFlag = 1;
                    break;
                }
            }

            if (errorFlag == 0) {
              String value = row[j];
              List<AssetExtraFieldName> listExtraFieldName =
                      extraFieldNameRepository.findByCompanyId(companyId);
              String id = assets.getId();

              for (AssetExtraFieldName fieldName : listExtraFieldName) {
                if (columnMap.get(field).equalsIgnoreCase(fieldName.getName())) {
                  AssetExtraFields extraFieldsDTO = new AssetExtraFields();
                  Optional<AssetExtraFields> extraFieldsOptional =
                          extraFieldsRepository.findByNameAndAssetId(fieldName.getName(), id);

                  if (extraFieldsOptional.isPresent()) {
                    extraFieldsDTO.setId(extraFieldsOptional.get().getId());
                    extraFieldsDTO.setAssetId(id);
                    extraFieldsDTO.setName(fieldName.getName());
                    extraFieldsDTO.setType(fieldName.getType());
                    if (fieldName.getType().equals("number")) {
                      try {
                        Integer val = Integer.parseInt(value);
                        extraFieldsDTO.setValue(val.toString());
                      } catch (Exception e) {
                        errorFlag = 1;
                        errorDesc.append("ERROR IN ")
                                .append(fieldName.getName().toUpperCase());
                      }
                    } else if (fieldName.getType().equals("date")) {
                      try {
                        DateTimeFormatter inputFormatter =
                                DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(value, inputFormatter);
                        DateTimeFormatter outputFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        extraFieldsDTO.setValue(date.format(outputFormatter));
                      } catch (Exception e) {
                        errorFlag = 1;
                        errorDesc.append("ERROR IN ")
                                .append(fieldName.getName().toUpperCase());
                      }
                    } else {
                      extraFieldsDTO.setValue(value);
                    }
                  } else {
                    extraFieldsDTO.setAssetId(id);
                    extraFieldsDTO.setName(fieldName.getName());
                    extraFieldsDTO.setType(fieldName.getType());
                    if (fieldName.getType().equals("number")) {
                      try {
                        Integer val = Integer.parseInt(value);
                        extraFieldsDTO.setValue(val.toString());
                      } catch (Exception e) {
                        errorFlag = 1;
                        errorDesc.append("ERROR IN ")
                                .append(fieldName.getName().toUpperCase());
                      }
                    } else if (fieldName.getType().equals("date")) {
                      try {
                        DateTimeFormatter inputFormatter =
                                DateTimeFormatter.ofPattern("dd-MM-yyyy");
                        LocalDate date = LocalDate.parse(value, inputFormatter);
                        DateTimeFormatter outputFormatter =
                                DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        extraFieldsDTO.setValue(date.format(outputFormatter));
                      } catch (Exception e) {
                        errorFlag = 1;
                        errorDesc.append("ERROR IN ")
                                .append(fieldName.getName().toUpperCase());
                      }
                    } else {
                      extraFieldsDTO.setValue(value);
                    }
                    extraFieldsDTO.setCompanyId(companyId);
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
          AssetsDTO assetsDTO = modelMapper.map(assets, AssetsDTO.class);
          assetsService.addAssets(assetsDTO);
        } else {
          // ✅ NEW: Add to error sheet
          Row errorRow = errorSheet.createRow(errorSheet.getLastRowNum() + 1);
          for (int col = 0; col < row.length; col++) {
            errorRow.createCell(col).setCellValue(row[col]);
          }
          errorRow.createCell(row.length).setCellValue(errorDesc.toString());
        }

        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now().toString());
        long complete = (currCount * 100L) / (totalCount - 1);
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
        ind++;
      }

      // ✅ UPDATED: Save and send email
      importHistoryDTO.setMessage(errorSheet.getLastRowNum() > 0
              ? "We have sent import result via email"
              : "Import was Successfully Done");

      try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
        workbook.write(fileOut);
      }
      workbook.close();

      MimeMessage message = emailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setTo(email);
      helper.setSubject("Customer Import Results - AssetYug");
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
      if (errorSheet.getLastRowNum() > 0) {
        helper.setText("Hi "+subjectName+",\n" +
                "\n" +
                "Your import has been completed. Please check the attached file for the errors that need correction. Once fixed, please reupload only the data listed in the file.\n" +
                "\n" +
                "If you need any help or additional information, feel free to reach out.\n" +
                "\n" +
                "Best regards,\n" +
                "Asset Yug Team");
        helper.addAttachment("AssetAttachment.xlsx", new File("Report.xlsx"));
      } else {
        helper.setText("Hi "+subjectName+",\n" +
                "\n" +
                "Your import has been completed successfully. All data has been processed and is now available in the system.\n" +
                "\n" +
                "Best regards,\n" +
                "AssetYug Team");
      }

      emailSender.send(message);

      importHistoryDTO.setStatus("Completed");

    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      e.printStackTrace();
    }

    customerService.addImportHistory(importHistoryDTO);
    log.info("Successfully Update Import");
  }





  @PostMapping("/imageUpload")
  public void importFile(@RequestBody AssetImageDTO assetImageDTO) throws Exception {
    Optional<Assets> assetsOptional = assetsRepository.findById(assetImageDTO.getId());
    if (assetsOptional.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetsOptional.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addImage(assetImageDTO);
  }

  @PostMapping("/removeImage")
  public void removeImage(@RequestBody String id) throws Exception {
    Optional<Assets> assetsOptional = assetsRepository.findById(id);
    if (assetsOptional.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetsOptional.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.removeImage(id);
  }

  @PostMapping("/removeAsset")
  public void removeAsset(@RequestBody String id) throws Exception {
    Optional<Assets> assetsOptional = assetsRepository.findById(id);
    if (assetsOptional.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetsOptional.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    assetsService.removeAsset(id);
  }

  @GetMapping("/getAsset/{id}")
  public AssetsDTO getAsset(@PathVariable String id) throws Exception {
    return assetsService.getAsset(id);
  }

  @PostMapping("/addfields")
  public void addNewFields(@RequestBody AssetExtraFieldsDTO extraFieldsDTO) throws Exception {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(extraFieldsDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addExtraFields(extraFieldsDTO);
  }

  @GetMapping("/getExtraFields/{id}")
  public List<AssetExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return assetsService.getExtraFields(id);
  }

  @DeleteMapping("/deleteExtraFields/{id}")
  public void deleteExtraField(@PathVariable String id) throws Exception {
    Optional<AssetExtraFields> extraFields = extraFieldsRepository.findById(id);
    if (extraFields.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         extraFields.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    assetsService.deleteExtraFields(id);
  }

  @GetMapping("/getExtraFieldName/{companyId}")
  public List<AssetExtraFieldNameDTO> getExtraFieldName(@PathVariable Long companyId) {
    // System.out.println("----------my company------------->"+companyId);
    return assetsService.getAssetExtraField(companyId);
  }

  @PostMapping("/addExtraFieldName")
  public void addExtraFieldName(@RequestBody AssetExtraFieldNameDTO extraFieldNameDTO)
      throws Exception {

    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(extraFieldNameDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addAssetExtraField(extraFieldNameDTO);
  }

  @DeleteMapping("/deleteExtraFieldName/{id}")
  public void deleteExtraFieldName(@PathVariable String id) throws Exception {
    // System.out.println("-----------------------api------------------------>"+id);
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    Optional<AssetExtraFieldName> extraFieldNameOptional = extraFieldNameRepository.findById(id);
    if (extraFieldNameOptional.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         extraFieldNameOptional.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }

    assetsService.deleteAssetExtraField(id);
  }

  @GetMapping("/getExtraFieldNameValue/{companyId}")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable Long companyId) {
    return assetsService.getextraFieldList(companyId);
  }

  @PostMapping("/addCheckInOut")
  public void addCheckInOut(@RequestBody AssetCheckInDTO checkInDTO) throws NoSubscriptionError, UserAccessException {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(checkInDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addCheckInOut(checkInDTO);
  }

  @GetMapping("/getCheckInOutList/{assetId}")
  public ResponseEntity<List<AssetCheckInOutDTO>> getCheckInOutList(@PathVariable String assetId) {

    List<AssetCheckInOutDTO> checkInOutList = assetsService.getCheckOutInList(assetId);
    return new ResponseEntity<>(checkInOutList, HttpStatus.ACCEPTED);
  }

  @PostMapping("/addFile/{assetId}/{username}")
  public ResponseEntity<ResponseMessageDTO> addAssetFile(
          @RequestParam("file") MultipartFile file,
          @PathVariable String assetId,
          @PathVariable String username) throws NoSubscriptionError {

    Optional<Assets> assetsOptional = assetsRepository.findById(assetId);
    if (assetsOptional.isEmpty()) {
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Asset not found");
      return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    try {
      assetsService.addAssetFile(file, assetId,username);

      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Uploaded the file successfully: " + file.getOriginalFilename());
      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IOException e) {
      ResponseMessageDTO response = new ResponseMessageDTO();
      response.setResponseMessage("Could not upload the file: " + file.getOriginalFilename());
      return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/getFile/{assetId}")
  public List<AssetFileDTO> getAssetFile(@PathVariable String assetId) {
    return assetsService.getAssetFile(assetId);
  }

  @GetMapping("/getFile/download/{id}")
  public ResponseEntity<?> downloadFile(@PathVariable String id) {
    AssetFileDTO assetFileDTO = assetsService.downloadFile(id);
    //		return new ResponseEntity<>(assetFileDTO.getFile(),HttpStatus.OK);
    return ResponseEntity.status(HttpStatus.OK)
        .contentType(MediaType.valueOf("json/object"))
        .body(assetFileDTO.getFile());
  }

  @DeleteMapping("deleteFile/{id}")
  public void deleteFile(@PathVariable String id) throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    Optional<Assets> assetsOptional = assetsRepository.findById(id);
    if (assetsOptional.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetsOptional.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }
    assetsService.deleteFile(id);
    //		return new ResponseEntity<>(assetFileDTO.getFile(),HttpStatus.OK);
    //		return new ResponseEntity<>("Successfully Deleted File",HttpStatus.EXPECTATION_FAILED);
  }

  @PostMapping("/mandatoryFields")
  public void mandatoryFields(@RequestBody AssetMandatoryFields mandatoryFields)
          throws NoSubscriptionError, UserAccessException {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(mandatoryFields.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    assetsService.updateMandatoryFields(mandatoryFields);
  }

  @PostMapping("/showFields")
  public void showFields(@RequestBody AssetShowFields showFields) throws NoSubscriptionError, UserAccessException {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(showFields.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    assetsService.updateShowFields(showFields);
  }

  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  public ResponseEntity<AssetMandatoryFields> getMandatoryFields(
      @PathVariable String name, @PathVariable Long companyId) {
    // System.out.println("============================>"+name+companyId);
    AssetMandatoryFields mandatoryFields = assetsService.getMandatoryFields(name, companyId);
    return ResponseEntity.ok(mandatoryFields);
  }

  @GetMapping("/getShowFields/{name}/{companyId}")
  public ResponseEntity<AssetShowFields> getShowFields(
      @PathVariable String name, @PathVariable Long companyId) {
    AssetShowFields showFields = assetsService.getShowFields(name, companyId);
    return ResponseEntity.ok(showFields);
  }

  @GetMapping("/getAllMandatoryFields/{companyId}")
  public ResponseEntity<List<AssetMandatoryFields>> getAllMandatoryFields(
      @PathVariable Long companyId) {
    List<AssetMandatoryFields> mandatoryFieldsList = assetsService.getAllMandatoryFields(companyId);
    return ResponseEntity.ok(mandatoryFieldsList);
  }

  @GetMapping("/getAllShowFields/{companyId}")
  public ResponseEntity<List<AssetShowFields>> getAllShowFields(@PathVariable Long companyId) {
    List<AssetShowFields> showFieldsList = assetsService.getAllShowFields(companyId);
    return ResponseEntity.ok(showFieldsList);
  }

  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  public void showFields(@PathVariable String name, @PathVariable Long companyId) throws Exception {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    assetsService.deleteShowAndMandatoryFields(companyId, name);
  }

  @PostMapping("/saveQRData")
  public void saveQRData(@RequestBody AssetQR qr) throws UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.qrDataUpdation(qr);
  }

  @GetMapping("/getQRData/{companyId}")
  public ResponseEntity<AssetQR> getQRData(@PathVariable Long companyId) {
    AssetQR qr = assetsService.getQRData(companyId);
    return ResponseEntity.ok(qr);
  }

  @GetMapping("/getAllAssetData/{companyId}")
  public PaginatedResultDTO<String> getAllAssetData(@PathVariable Long companyId) {
    return assetsService.getAllAssetDetails(companyId);
  }

  @GetMapping(value = "/searchAssetlist/{companyId}")
  public List<String> getWorkOrderFromAsset(
      @PathVariable Long companyId,
      @RequestParam(name = "data", required = true) String search,
      @RequestParam(name = "category", required = true) String category) {
    // System.out.println("----------my workorder search------------->"+search);
    return assetsService.searchedAssets(companyId, search, category);
  }

  @GetMapping(value = "/sortAssetlist/{companyId}/{pageNumber}/{pageSize}")
  public PaginatedResultDTO<String> getSortedWorkOrderFromAsset(
      @PathVariable Long companyId,
      @PathVariable(required = false) Integer pageNumber,
      @PathVariable(required = false) Integer pageSize,
      @RequestParam(name = "category", required = true) String category) {

    if (pageNumber == null) {
      pageNumber = 0;
    }

    if (pageSize == null) {
      pageNumber = 5;
    }

    return assetsService.sortAssets(companyId, category, pageNumber, pageSize);
  }

  @PostMapping("/updateAssetsWithInActive")
  public void updateAssetsWithInActive(@RequestBody String customerId) throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    List<Assets> assetsList = assetsRepository.findByCustomerId(customerId);
    if (!assetsList.isEmpty()) {

      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetsList.get(0).getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }

    assetsService.updateAssetsWithInActive(customerId);
  }

  @PostMapping("/advanceFilter/{pageNumber}/{pageSize}")
  public PaginatedResultDTO<String> advanceFilter(
      @RequestBody Object filter,
      @PathVariable(required = false) Integer pageNumber,
      @PathVariable(required = false) Integer pageSize,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "search", required = false) String searchData,
      @RequestParam(name = "asc", required = false) Boolean asc)
      throws NoSubscriptionError {
    //        Boolean asc=true;
    //        Boolean asc=true;

    //    System.out.println("CompanyId--------AssetAPI-------->" + filter.toString());

//    try {
//      Thread.sleep(5_000); // 10 seconds (milliseconds)
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//    }

    if (asc == null) {
      asc = true;
    }
    //    System.out.println("Workingggggg");
    //    System.out.println("===> " + pageNumber + "-" + category + "-" + searchData + "-" + asc);
    if (pageNumber == null) {
      pageNumber = 0;
    }

    if (pageSize == null) {
      pageNumber = 5;
    }
    if (category == null || category.equals("")) {
      category = "updatedAt";
      asc = false;
    }
    return assetsService.advanceFilter(filter, pageNumber, pageSize, category, searchData, asc);
  }

  @GetMapping("checkInOutCount/{companyId}")
  public CheckInCheckOutCountDTO checkInCheckOutCountDTO(@PathVariable Long companyId) {
    return assetsService.checkInCheckOut(companyId);
  }

  @GetMapping("/assetBySerialNumber")
  public List<AssetsDTO> assetFromSerialNumber(@RequestBody AssetBySerialDTO assetBySerialDTO) {
    return assetsService.assetListFromSerialNumber(
        assetBySerialDTO.getCompanyId(), assetBySerialDTO.getSerialNumber());
  }

  @GetMapping("/checkInOutAsset/{companyId}/{checkedIn}")
  public List<AssetCheckInOut> checkInOutAsset(
      @PathVariable Long companyId, @PathVariable Boolean checkedIn) {

    return assetsService.filterByCheckedInOut(companyId, checkedIn);
  }
//  @GetMapping("/checkInOutAssetData/{companyId}")
//  public List<AssetCheckInOutData> getCheckInOutData(
//          @PathVariable Long companyId) {
//
//    return assetsService.getAssetCheckInOutData(companyId);
//  }
  @GetMapping("/checkInOutAssetData/{companyId}")
  public PaginatedResultCheckInOutDTO<AssetCheckInOutData> getCheckInOutData(
          @PathVariable Long companyId,
          @RequestParam Long pageNumber,
          @RequestParam Long pageSize) {

    return assetsService.getAssetCheckInOutData(companyId, pageNumber, pageSize);
  }
//  @GetMapping("/latestCheckInOutAssetData/{companyId}")
//  public Objects getLatestCheckInOutAssetData(
//          @PathVariable Long companyId) {
//
//    return assetsService.getAssetCheckInOutData(companyId);
//  }

  @PostMapping(value = "/addCategory")
  public void addCategory(@RequestBody CategoryDTO categoryDTO)
          throws Exception {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(categoryDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addCategory(categoryDTO);
  }

  @GetMapping(value = "/getCategoryList/{companyId}")
  public List<AssetCategory> getCategoryList(@PathVariable Long companyId) {
    return assetsService.getCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  public List<AssetCategory> getCategoryActiveList(@PathVariable Long companyId) {
    return assetsService.getActiveCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  public AssetCategory getCategoryById(@PathVariable Long companyId, @PathVariable String id) {
    return assetsService.getCategoryListById(companyId, id);
  }

  @GetMapping(value = "/countAssetByCategory/{category}")
  public int countAssetByCategory(@PathVariable String category) throws CategoryException {
    System.out.println("Category===>" + category);
    return assetsService.countAssetByCategory(category);
  }

  @DeleteMapping(value = "/deleteCategory/{id}")
  public void deleteCategory(@PathVariable String id) throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    Optional<AssetCategory> assetCategory = assetCategoryRepository.findById(id);
    if (assetCategory.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetCategory.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }

    assetsService.deleteCategory(id);
  }

  @PutMapping(value = "/updateCategory")
  public void updateCategory(@RequestBody CategoryDTO categoryDTO) throws NoSubscriptionError, UserAccessException {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(categoryDTO.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    assetsService.updateCategory(categoryDTO);
  }

  @GetMapping(value = "/getActiveAssets/{companyId}")
  public List<AssetsDTO> getActiveAssets(@PathVariable Long companyId) {
    return assetsService.getActiveAssets(companyId);
  }

  @PostMapping(value = "/addAssetInspection")
  public void addAssetInspection(@RequestBody AssetCategoryInspection assetCategoryInspection)
          throws NoSubscriptionError, UserAccessException {
    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(
    //         assetCategoryInspection.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addAssetInspection(assetCategoryInspection);
  }

  @DeleteMapping(value = "/deleteAssetInspection/{id}")
  public void deleteAssetInspection(@PathVariable String id) throws NoSubscriptionError, UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.full);
    Optional<AssetCategoryInspection> assetCategoryInspection =
        assetCategoryInspectionRepository.findById(id);
    if (assetCategoryInspection.isPresent()) {
      // Optional<Subscription> subscriptionOptional =
      //     subscriptionRepository.findByCompanyIdAndStatus(
      //         assetCategoryInspection.get().getCompanyId(), SubscriptionEnum.ACTIVE);
      // if (subscriptionOptional.isEmpty()) {
      //   throw new NoSubscriptionError("No Active Subscription");
      // }
    }

    assetsService.deleteAssetInspection(id);
  }

  @GetMapping(value = "/getAssetInspection/{id}")
  public AssetCategoryInspection getAssetInspection(@PathVariable String id) throws Exception {
    return assetsService.getAssetInspection(id);
  }

  @GetMapping(value = "/getAllAssetInspectionByCategory/{companyId}")
  public List<AssetCategoryInspection> getAllAssetInspectionByCategory(@PathVariable Long companyId,@RequestParam String category)
      throws Exception {
    System.out.println("API CATEGORY"+category);
    return assetsService.getAllAssetInspectionByCategory(companyId,category);
  }
  @GetMapping(value = "/getAllAssetInspection/{companyId}")
  public List<AssetCategoryInspection> getAllAssetInspection(@PathVariable Long companyId)
          throws Exception {
    return assetsService.getAllAssetInspection(companyId);
  }

  @PostMapping(value = "/addAssetInspectionInstance")
  public void addAssetInspection(
      @RequestBody AssetCategoryInspectionInstance assetCategoryInspection) throws UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.create);
    assetsService.addAssetInspectionInstance(assetCategoryInspection);
  }

  @PutMapping(value = "/addAssetInspectionInstance")
  public void updateAssetInspection(
      @RequestBody AssetCategoryInspectionInstance assetCategoryInspection)
          throws NoSubscriptionError, UserAccessException {

    // Optional<Subscription> subscriptionOptional =
    //     subscriptionRepository.findByCompanyIdAndStatus(
    //         assetCategoryInspection.getCompanyId(), SubscriptionEnum.ACTIVE);
    // if (subscriptionOptional.isEmpty()) {
    //   throw new NoSubscriptionError("No Active Subscription");
    // }
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    assetsService.addAssetInspectionInstance(assetCategoryInspection);
  }

  @GetMapping(value = "/getAllAssetInspectionInstance/{companyId}")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstance(
      @PathVariable Long companyId) {
    return assetsService.getAllAssetCategoryInspectionValues(companyId);
  }
  @GetMapping(value = "/getAllAssetInspectionInstanceByAssetId/{assetId}")
  public List<AssetCategoryInspectionInstance> getAllAssetInspectionInstanceByAssetId(
          @PathVariable String assetId) {
    return assetsService.getAllAssetCategoryInspectionInstanceByAsset(assetId);
  }

  @GetMapping(value = "/getAssetByCategory/{companyId}")
  public Map<String, List<AssetsDTO>> getAssetByCategory(@PathVariable Long companyId) {
    return assetsService.getAssetByCategory(companyId);
  }
  public Location getLocationForBin(Bin bin) {
    if (bin.getLocationId() != null) {
      return locationRepository
              .findById(bin.getLocationId().toString()) // Convert ObjectId → String
              .orElse(null);
    }
    return null;
  }
  @GetMapping(value = "/locationBinDetails/{companyId}/{name}")
  public String getLocationBinDetails(
          @PathVariable Long companyId,@PathVariable String name) {
//    System.out.println("----------------> bin detsild name "+name+" "+companyId);
    Optional<Bin> optBin = binRepository.findByCompanyIdAndBinNumberIgnoreCase(companyId, name);
    if (optBin.isPresent()) {

      Bin bin = optBin.get(); // ✅ Safe here
      Location location = getLocationForBin(bin);
//      System.out.println("----------------> bin detsild "+location.getName().concat("->").concat(bin.getBinNumber()));
      return location.getName().concat("->").concat(bin.getBinNumber());

    }
    else{
      Optional<Location> optLoc=locationRepository.findByCompanyIdAndName(companyId,name);
        return optLoc.map(Location::getName).orElse(null);
    }
//    return assetsService.getAllAssetCategoryInspectionValues(companyId);
  }

  @GetMapping("/export-asset-xlsx/{companyId}")
  public ResponseEntity<byte[]> exportAssetsXlsx(@PathVariable Long companyId) throws IOException {
    List<Assets> assets = assetsRepository.findByCompanyId(companyId);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Assets");
    Row header = sheet.createRow(0);
    int col = 0;
    // Add default headers
    String[] defaultHeaders = {"ID", "Name", "AssetId", "Category", "Customer", "CustomerId", "Location", "Status"};
    for (String h : defaultHeaders) {
      header.createCell(col++).setCellValue(h);
    }
    // Add extra field headers
    for (AssetExtraFieldName extraField : extraFieldNames) {
      header.createCell(col++).setCellValue(extraField.getName());
    }
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
      // row.createCell(c++).setCellValue(asset.getUpdatedAt());
      // Fetch custom fields
      List<AssetExtraFields> extras = extraFieldsRepository.findByAssetId(asset.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (AssetExtraFields ef : extras) {
        extraMap.put(ef.getName(), ef.getValue());
      }
      for (AssetExtraFieldName extraField : extraFieldNames) {
        row.createCell(c++).setCellValue(extraMap.getOrDefault(extraField.getName(), ""));
      }
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    workbook.close();
    byte[] excelBytes = bos.toByteArray();
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=AssetExport.xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excelBytes);
  }

//  @GetMapping(value = "/export-asset/{companyId}")
//  public ResponseEntity<byte[]> downloadAllAssetData(@PathVariable Long companyId)
//      throws IOException {
//
//    //    PaginatedResultDTO result=assetsService.getAllAssetDetails(companyId);
//    PaginatedResultDTO result = assetsService.getAllAssetDetails(companyId);
//    //    System.out.println(result.getData().toString());
//    ObjectMapper objectMapper = new ObjectMapper();
//    JsonNode jsonNode = objectMapper.readTree(result.getData().toString());
//    Workbook workbook = new XSSFWorkbook();
//    Sheet sheet = workbook.createSheet("Data");
//
//    // Add header row
//    Row headerRow = sheet.createRow(0);
//    if (jsonNode.isArray() && jsonNode.size() > 0) {
//      JsonNode firstObject = jsonNode.get(0);
//      Iterator<String> fieldNames = firstObject.fieldNames();
//      int headerCol = 0;
//      while (fieldNames.hasNext()) {
//        String fieldName = fieldNames.next();
//        if ("id".equalsIgnoreCase(fieldName)) {
//          continue;
//        }
//        Cell cell = headerRow.createCell(headerCol++);
//        cell.setCellValue(fieldName);
//        cell.setCellStyle(getHeaderCellStyle(workbook));
//      }
//    }
//
//    // Add data rows
//    int rowIndex = 1;
//    for (JsonNode node : jsonNode) {
//      Row dataRow = sheet.createRow(rowIndex++);
//      Iterator<String> fieldNames = node.fieldNames();
//      int colIndex = 0;
//      while (fieldNames.hasNext()) {
//        String fieldName = fieldNames.next();
//        if ("id".equalsIgnoreCase(fieldName)) {
//          continue;
//        }
//        String cellValue = node.get(fieldName).asText();
//        Cell cell = dataRow.createCell(colIndex++);
//        cell.setCellValue(cellValue);
//      }
//    }
//
//    // Auto-size columns
//    if (jsonNode.isArray() && jsonNode.size() > 0) {
//      JsonNode firstObject = jsonNode.get(0);
//      int columnCount = firstObject.size();
//      for (int i = 0; i < columnCount; i++) {
//        sheet.autoSizeColumn(i);
//      }
//    }
//
//    // Write workbook to a byte array
//    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//    workbook.write(outputStream);
//    workbook.close();
//
//    // Return as an Excel file
//    byte[] excelBytes = outputStream.toByteArray();
//    HttpHeaders headers = new HttpHeaders();
//    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//    headers.setContentDispositionFormData("attachment", "data.xlsx");
//
//    return ResponseEntity.ok().headers(headers).body(excelBytes);
//  }


//  @GetMapping(value = "/getAssetByCompanyCategory/{companyId}")
//  public Map<String, List<AssetsDTO>> getAssetByCompanyCategory(@PathVariable Long companyId) {
//    return assetsService.getAssetByCompanyCategory(companyId);
//  }


  private CellStyle getHeaderCellStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  @PutMapping("/extraFieldName")
  public ResponseEntity<AssetExtraFieldName> updateExtraFieldName(@RequestBody ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) throws UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.edit);
    AssetExtraFieldName assetExtraFieldName=assetsService.updateExtraFieldName(extraFieldNameUpdateDTO);
    return ResponseEntity.ok(assetExtraFieldName);



  }
  @GetMapping("/export-checkinout-xlsx/{companyId}/{assetId}")
  public ResponseEntity<byte[]> exportCheckInOut(@PathVariable Long companyId,@PathVariable String assetId,  HttpServletRequest request) throws UserAccessException {
    checkUserDetailsPermissionFromSpringContext(CustomRoleType.view);

    Optional<AssetCheckInOut> optionalAssetCheckInOut = assetCheckInOutRepository.findByCompanyIdAndAssetId(companyId,assetId);
    Optional<Assets> assetDetails=assetsRepository.findById(assetId);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Check In-Out Report");
    if(optionalAssetCheckInOut.isPresent()){
      Row headerRow = sheet.createRow(0);
      String[] headers = {
              "Asset ID",
              "Asset Name",
              "Customer ID",
              "Customer Name",
              "Action",
              "Action Date",
              "Action Time",
              "Location",
              "Username",
              "Notes",
              "IP Address",
              "GeoLocation"
      };
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(getHeaderCellStyle(workbook));
      }

      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
      DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
      // Add data rows
      int rowIdx = 1;
      for (AssetCheckInOutDetails checkInOut : optionalAssetCheckInOut.get().getDetailsList()) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(assetDetails.get().getAssetId());
        row.createCell(1).setCellValue(assetDetails.get().getName());
        row.createCell(2).setCellValue(assetDetails.get().getCustomerId());
        row.createCell(3).setCellValue(assetDetails.get().getCustomer());
        row.createCell(4).setCellValue(checkInOut.getStatus() != null ? checkInOut.getStatus() : "");
        row.createCell(5).setCellValue(
                checkInOut.getDate() != null
                        ? checkInOut.getDate().format(dateFormatter)
                        : ""
        );

        row.createCell(6).setCellValue(
                checkInOut.getDate() != null
                        ? checkInOut.getDate().format(dateFormatter)
                        : ""
        );
        row.createCell(7).setCellValue(checkInOut.getLocation() != null ? checkInOut.getLocation() : "");
        row.createCell(8).setCellValue(checkInOut.getEmployee() != null ? checkInOut.getEmployee() : "");
        row.createCell(9).setCellValue(checkInOut.getNotes() != null ? checkInOut.getNotes() : "");


        row.createCell(10).setCellValue(checkInOut.getIpAddress());
        row.createCell(11).setCellValue(checkInOut.getUserLocation());




      }

      // Auto-size columns
      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      // Write to byte array
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        workbook.write(bos);
        workbook.close();
      } catch (IOException e) {
        throw new RuntimeException("Error generating Excel file", e);
      }

      byte[] excelBytes = bos.toByteArray();
      return ResponseEntity.ok()
              .header("Content-Disposition", "attachment; filename=CheckInOut_Report_"+assetId+".xlsx")
              .contentType(MediaType.APPLICATION_OCTET_STREAM)
              .body(excelBytes);
    }
    else{
      return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
    // Create header row

  }
  public String getGeoLocation(String lat, String lng) {
    try {
      double latitude = Double.parseDouble(lat);
      double longitude = Double.parseDouble(lng);

      // Using OpenStreetMap Nominatim API (free, no API key needed)
      String url = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
              + latitude + "&lon=" + longitude;

      URL urlObj = new URL(url);
      HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "Mozilla/5.0");

      int responseCode = connection.getResponseCode();

      if (responseCode == 200) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
        );

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          response.append(line);
        }
        reader.close();

        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());

        // Extract address information
        String address = jsonResponse.optString("address", "");
        String city = jsonResponse.optString("city", "");
        String country = jsonResponse.optString("country", "");
        String postcode = jsonResponse.optString("postcode", "");

        // Format the location string
        String location = "";
        if (!address.isEmpty()) location += address + ", ";
        if (!city.isEmpty()) location += city + ", ";
        if (!postcode.isEmpty()) location += postcode + ", ";
        if (!country.isEmpty()) location += country;

        // Clean up trailing commas and spaces
        location = location.replaceAll(", $", "");

        return location.isEmpty() ? "Location not found" : location;

      } else {
        return "Error: " + responseCode;
      }

    } catch (Exception e) {
      e.printStackTrace();
      return "Error: " + e.getMessage();
    }
  }
//  public static String getClientIp(HttpServletRequest request) {
//    String ip = request.getHeader("X-Forwarded-For");
//    if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
//      return ip.split(",")[0].trim();
//    }
//    return request.getRemoteAddr();
//  }
  @GetMapping("/export-asset/{companyId}")
  public ResponseEntity<byte[]> exportCompanyCustomers(@PathVariable Long companyId) throws IOException {
    List<Assets> assets = assetsRepository.findByCompanyId(companyId);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Assets");
    Row header = sheet.createRow(0);
    int col = 0;
    // Add default headers
    String[] defaultHeaders = {"ID", "Serial Number","Customer","Category", "Location", "Status"};
    for (String h : defaultHeaders) {
      header.createCell(col++).setCellValue(h);
    }
    // Add extra field headers
    for (AssetExtraFieldName extraField : extraFieldNames) {
      header.createCell(col++).setCellValue(extraField.getName());
    }
    int rowIdx = 1;
    for (Assets asset : assets) {
      Row row = sheet.createRow(rowIdx++);
      int c = 0;
      row.createCell(c++).setCellValue(asset.getAssetId());
      row.createCell(c++).setCellValue(asset.getSerialNumber());
      row.createCell(c++).setCellValue(asset.getCustomer());
      row.createCell(c++).setCellValue(asset.getCategory());
//      row.createCell(c++).setCellValue(asset.getLocation());
      String locationValue = asset.getLocation();
      String name = "";

      if (locationValue != null && !locationValue.isBlank()) {

        String[] parts = locationValue.split(":", 2);

        if (parts.length == 2) {
          String type = parts[0];
          String id = parts[1];

          if ("location".equalsIgnoreCase(type)) {
            name = locationRepository.findById(id)
                    .map(Location::getName)
                    .orElse("");
          }
          else if ("bin".equalsIgnoreCase(type)) {
            name = binRepository.findById(id)
                    .map(Bin::getBinNumber)
                    .orElse("");
          }
        }
      }

      row.createCell(c++).setCellValue(name);

      String status = asset.getStatus();

      if (status != null && !status.isEmpty()) {
        status = status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
      } else {
        status = "";
      }
      row.createCell(c++).setCellValue(status);
      // Fetch custom fields
      List<AssetExtraFields> extras = extraFieldsRepository.findByAssetId(asset.getId());
      Map<String, String> extraMap = new HashMap<>();
      for (AssetExtraFields ef : extras) {
        extraMap.put(ef.getName(), ef.getValue());
      }
      for (AssetExtraFieldName extraField : extraFieldNames) {
        row.createCell(c++).setCellValue(extraMap.getOrDefault(extraField.getName(), ""));
      }
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    workbook.close();
    byte[] excelBytes = bos.toByteArray();
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=AssetExport.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(excelBytes);
  }

}
