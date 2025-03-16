package com.quantumai.customer.controller;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.ImportFileRowException;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.service.CompanyCustomerService;
import com.quantumai.customer.service.CustomerService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// @CrossOrigin("http://assetyug.com.s3-website-us-east-1.amazonaws.com")
@Slf4j
@CrossOrigin("**")
@RestController
@RequestMapping(value = "/companycustomer")
public class CompanyCustomerAPI {

  @Autowired private CompanyCustomerService companyCustomerService;

  @Autowired CompanyCustomerRepository companyCustomerRepository;

  @Autowired private JavaMailSender emailSender;
  @Autowired private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;

  @Autowired private CompanyCustomerExtraFieldsRepository extraFieldsRepository;

  @Autowired private CustomerService customerService;

  private ModelMapper modelMapper = new ModelMapper();

  @GetMapping("/working")
  public String working() {
    System.out.println("working!!!");
    return "Working!!";
  }

  @DeleteMapping("/deleteCompanyCustomer/{id}")
  public void deleteCompanyCustomer(@PathVariable String id) {
    companyCustomerService.deleteCustomer(id);
  }

  @GetMapping("/allCompanyCustomer/{companyId}")
  public List<CompanyCustomerDTO> getCompanyCustomerList(@PathVariable String companyId) {
    return companyCustomerService.getAllCustomer(companyId);
  }

  @GetMapping("/getCompanyCustomer/{id}")
  public CompanyCustomerDTO getCompanyCustomer(@PathVariable String id) {
    System.out.println(id);
    return companyCustomerService.getCustomer(id);
  }

  @GetMapping("/getCompanyCustomerByLocalId/{id}/{companyId}")
  public CompanyCustomerDTO getCompanyCustomerByLocalId(
      @PathVariable String id, @PathVariable String companyId) {
    System.out.println(id);
    return companyCustomerService.getCompanyCustomerByLocalId(Integer.valueOf(id), companyId);
  }

  //	@GetMapping("/allCompanyCustomerWithExtraFields/{companyId}")
  //	public List<CompanyCustomerDTO> getCompanyCustomerWithExtraFields(@PathVariable String
  // companyId){
  //		return companyCustomerService.getAllCustomer(companyId);
  //	}
  @PostMapping("/addCompanyCustomer")
  public CompanyCustomerDTO addNewFields(@RequestBody CompanyCustomerDTO companyCustomerDTO) {
    return companyCustomerService.addCustomer(companyCustomerDTO);
  }

  @PutMapping("/updateCompanyCustomer")
  public void updateCompanyCustomer(@RequestBody CompanyCustomerDTO companyCustomerDTO) {
    companyCustomerService.updateCustomer(companyCustomerDTO);
  }

  @GetMapping(value = "/searchCompanyCustomerlist/{companyId}")
  public List<String> getCompanyCustomerFromAsset(
      @PathVariable String companyId,
      @RequestParam(name = "data", required = true) String search,
      @RequestParam(name = "category", required = true) String category) {
    System.out.println("----------my CompanyCustomer search------------->" + search);
    return companyCustomerService.searchedCompanyCustomer(companyId, search, category);
  }

  @GetMapping(value = "/sortCompanyCustomerlist/{companyId}")
  public List<String> getCompanyCustomerFromAsset(
      @PathVariable String companyId,
      @RequestParam(name = "category", required = true) String category) {
    return companyCustomerService.sortCompanyCustomer(companyId, category);
  }

  @PostMapping("/addExtraFieldName")
  public void addExtraFieldName(@RequestBody CompanyCustomerExtraFieldNameDTO extraFieldNameDTO)
      throws Exception {
    companyCustomerService.addCompanyCustomerExtraField(extraFieldNameDTO);
  }

  @GetMapping("/getExtraFieldName/{companyId}")
  public List<CompanyCustomerExtraFieldNameDTO> getExtraFieldName(@PathVariable String companyId) {
    //		System.out.println("----------my company------------->"+companyId);
    return companyCustomerService.getCompanyCustomerExtraField(companyId);
  }

  @DeleteMapping("/deleteExtraFieldName/{id}")
  public void deleteExtraFieldName(@PathVariable String id) {
    System.out.println("-----------------------api------------------------>" + id);
    companyCustomerService.deleteCompanyCustomerExtraField(id);
  }

  @PostMapping("/mandatoryFields")
  public void mandatoryFields(@RequestBody CompanyCustomerMandatoryFields mandatoryFields) {
    companyCustomerService.updateMandatoryFields(mandatoryFields);
  }

  @PostMapping("/showFields")
  public void showFields(@RequestBody CompanyCustomerShowFields showFields) {
    companyCustomerService.updateShowFields(showFields);
  }

  @GetMapping("/getMandatoryFields/{name}/{companyId}")
  public ResponseEntity<CompanyCustomerMandatoryFields> getMandatoryFields(
      @PathVariable String name, @PathVariable String companyId) {
    System.out.println("============================>" + name + companyId);
    CompanyCustomerMandatoryFields mandatoryFields =
        companyCustomerService.getMandatoryFields(name, companyId);
    return ResponseEntity.ok(mandatoryFields);
  }

  @GetMapping("/getShowFields/{name}/{companyId}")
  public ResponseEntity<CompanyCustomerShowFields> getShowFields(
      @PathVariable String name, @PathVariable String companyId) {
    CompanyCustomerShowFields showFields = companyCustomerService.getShowFields(name, companyId);
    return ResponseEntity.ok(showFields);
  }

  @GetMapping("/getAllMandatoryFields/{companyId}")
  public ResponseEntity<List<CompanyCustomerMandatoryFields>> getAllMandatoryFields(
      @PathVariable String companyId) {
    List<CompanyCustomerMandatoryFields> mandatoryFieldsList =
        companyCustomerService.getAllMandatoryFields(companyId);
    return ResponseEntity.ok(mandatoryFieldsList);
  }

  @GetMapping("/getAllShowFields/{companyId}")
  public ResponseEntity<List<CompanyCustomerShowFields>> getAllShowFields(
      @PathVariable String companyId) {
    List<CompanyCustomerShowFields> showFieldsList =
        companyCustomerService.getAllShowFields(companyId);
    return ResponseEntity.ok(showFieldsList);
  }

  @DeleteMapping("/deleteShowAndMandatoryField/{name}/{companyId}")
  public void showFields(@PathVariable String name, @PathVariable String companyId) {
    companyCustomerService.deleteShowAndMandatoryFields(companyId, name);
  }

  @GetMapping("/getExtraFieldNameValue/{companyId}")
  public Map<String, Map<String, String>> getExtraFieldNameValue(@PathVariable String companyId) {
    return companyCustomerService.getextraFieldList(companyId);
  }

  @PostMapping("/addfields")
  public void addNewFields(@RequestBody CompanyCustomerExtraFieldsDTO extraFieldsDTO)
      throws Exception {
    companyCustomerService.addExtraFields(extraFieldsDTO);
  }

  @GetMapping("/getExtraFields/{id}")
  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(@PathVariable String id) {
    return companyCustomerService.getExtraFields(id);
  }

  @DeleteMapping("/deleteExtraFields/{id}")
  public void deleteExtraField(@PathVariable String id) throws Exception {
    companyCustomerService.deleteExtraFields(id);
  }

  @DeleteMapping("/deleteCompanyCustomerExtraFields/{id}")
  public void deleteCompanyCustomerExtraFields(@PathVariable String id) throws Exception {
    companyCustomerService.deleteExtraFieldByCompanyCustomer(id);
  }

  @GetMapping("/allCompanyCustomerWithExtraFields/{id}")
  public List<String> allCompanyCustomerWithExtraFields(@PathVariable String id) {
    return companyCustomerService.getAllCustomerWithExtraColumns(id);
  }

  @PostMapping("/addFile/{companyCustomerId}")
  public ResponseEntity<ResponseMessageDTO> addCompanyCustomerFile(
      @RequestParam("file") MultipartFile file, @PathVariable String companyCustomerId) {
    System.out.println("------------------------inside Multifile------------->");
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
  public void deleteFile(@PathVariable String id) {
    companyCustomerService.deleteFile(id);
    //		return new ResponseEntity<>(assetFileDTO.getFile(),HttpStatus.OK);
    //		return new ResponseEntity<>("Successfully Deleted File",HttpStatus.EXPECTATION_FAILED);
  }

  @PostMapping("/advanceFilter/{pageNumber}/{pageSize}")
  public PaginatedResultDTO<String> advanceFilter(
      @RequestBody Object filter,
      @PathVariable(required = false) Integer pageNumber,
      @PathVariable(required = false) Integer pageSize,
      @RequestParam(name = "category", required = false) String category,
      @RequestParam(name = "search", required = false) String searchData,
      @RequestParam(name = "asc", required = false) Boolean asc) {

    // Use a logger instead of System.out.print
    //		Logger logger = LoggerFactory.getLogger(CompanyCustomerAPI.class);

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
      @PathVariable String companyId,
      @PathVariable String email)
      throws CsvValidationException, MessagingException, ImportFileRowException {
    //
    //	//System.out.println("------||---------------------------------------/////////////////////////////////////------->"+columnMappings);

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
            // System.out.println("Start of object");
            break;
          case FIELD_NAME:
            // System.out.println("Field name: " + jsonParser.getCurrentName());
            key = jsonParser.getCurrentName();
            break;
          case VALUE_STRING:
            // System.out.println("Field value: " + jsonParser.getText());
            val = jsonParser.getText();

            break;
          case END_OBJECT:
            // System.out.println("End of object");
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
    // System.out.println("--------------> "+columnMap.size());
    List<CompanyCustomerDTO> assetList = new ArrayList<CompanyCustomerDTO>();
    long totalCount = Integer.MAX_VALUE;
    try (InputStream inputStream = file.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      totalCount = reader.lines().count() - 1;
      System.out.println("Total Count" + totalCount);
      if (reader.lines().count() > 5001) {
        throw new ImportFileRowException("Import File cannot import more than 5000 rows");
      }

    } catch (IOException e) {
      // Handle IOException
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

      // Create a Sheet
      Sheet sheet = workbook.createSheet("Sheet1");
      int excelIndex = 0;
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
        Row myrow = sheet.createRow(excelIndex);
        Cell cell1 = myrow.createCell(0);
        cell1.setCellValue("Line " + (int) (ind + 1));
        CompanyCustomerDTO companyCustomerDTO = new CompanyCustomerDTO();
        companyCustomerDTO.setCompanyId(companyId);
        int errorFlag = 0;
        String errorDesc = "";
        //	            //System.out.println("-------|||||---errorFlag----> "+errorFlag);
        for (int j = 0; j < row.length; j++) {
          String field = headerMap.get(j);
          //	                //System.out.println("--------------> "+j+" " + row[j]);
          //	                //System.out.println("-------|||||------->
          // "+columnMap.get(field).toLowerCase());

          if (errorFlag == 1) {
            break;
          }

          System.out.println(field + "------->" + columnMap.get(field));
          if (columnMap.get(field) != null) {
            switch (columnMap.get(field).toLowerCase()) {
              case "name":
                companyCustomerDTO.setName(row[j]);
                break;

              case "phone":
                companyCustomerDTO.setPhone(row[j]);
                break;
              case "category":
                System.out.println("category//->" + row[j]);
                companyCustomerDTO.setCategory(row[j]);
                break;
              case "email":
                companyCustomerDTO.setEmail(row[j]);
              case "address":
                System.out.println("address//->" + row[j]);
                companyCustomerDTO.setAddress(row[j]);
                break;
              case "apartment":
                System.out.println("Apartment//->" + row[j]);
                companyCustomerDTO.setApartment(row[j]);
                break;
              case "city":
                companyCustomerDTO.setCity(row[j]);
                break;
              case "state":
                companyCustomerDTO.setState(row[j]);
                break;
              case "zipcode":
                System.out.println("zipCode//->" + row[j]);
                companyCustomerDTO.setZipCode(Integer.parseInt(row[j]));
                break;
              case "status":
                if ((row[j].toLowerCase().equals("active"))
                    || (row[j].toLowerCase().equals("inactive"))) {

                  if (row[j].toLowerCase().equals("active")) {
                    companyCustomerDTO.setStatus("active");
                  } else {
                    companyCustomerDTO.setStatus("inActive");
                  }

                  errorFlag = 0;
                  break;

                } else {
                  // System.out.println("ERROR WHILE ADDING IN Status for line->"+ind);
                  if (errorDesc.length() > 0) {
                    errorDesc += ", ";
                  }
                  errorDesc += "ERROR WHILE ADDING IN STATUS";
                  errorFlag = 1;
                  break;
                }
            }
          }
        }
        //	            //System.out.println("///'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/  reached
        // errorFlag" +errorFlag);
        if (errorFlag == 0) {
          //	            	//System.out.println("///'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/'/");
          CompanyCustomerDTO mynewCustomer = companyCustomerService.addCustomer(companyCustomerDTO);

          // -------------------------------------------
          for (int j = 0; j < row.length; j++) {
            String field = headerMap.get(j);
            //		                //System.out.println("-------|||||------->
            // "+columnMap.get(field).toLowerCase());

            String value = row[j];

            List<CompanyCustomerExtraFieldName> listExtraFieldName =
                extraFieldNameRepository.findByCompanyId(companyId);
            if (columnMap.get(field) != null) {
              for (int i = 0; i < listExtraFieldName.size(); i++) {
                if ((columnMap.get(field) != null)
                    && columnMap
                        .get(field)
                        .toLowerCase()
                        .equals(listExtraFieldName.get(i).getName().toLowerCase())) {
                  // System.out.println("-----------------working
                  // ---->"+listExtraFieldName.get(i).getType());
                  CompanyCustomerExtraFields extraFieldsDTO = new CompanyCustomerExtraFields();
                  extraFieldsDTO.setCompanyCustomerId(mynewCustomer.getId());
                  extraFieldsDTO.setName(listExtraFieldName.get(i).getName());
                  extraFieldsDTO.setType(listExtraFieldName.get(i).getType());

                  extraFieldsDTO.setCompanyId(companyId);
                  if (listExtraFieldName.get(i).getType().equals("number")) {
                    try {
                      Integer val = Integer.parseInt(value);
                      // System.out.println("-----------------extra---->"+val+"->"+value);
                      extraFieldsDTO.setValue(val.toString());
                    } catch (Exception e) {
                      // System.out.println("ERROR WHILE ADDING EXTRA IN"+
                      // listExtraFieldName.get(i).getName()+" for row->"+ind+1);
                      errorFlag = 1;
                      if (errorDesc.length() > 0) {
                        errorDesc += ", ";
                      }
                      errorDesc +=
                          "ERROR WHILE ADDING IN "
                              + listExtraFieldName.get(i).getName().toUpperCase();
                    }
                  }
                  if (listExtraFieldName.get(i).getType().equals("date")) {
                    try {

                      DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                      LocalDate date = LocalDate.parse(value, inputFormatter);

                      DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                      String formattedDate = date.format(outputFormatter);

                      // System.out.println("-----------------extra-date--->"+formattedDate+"->"+value);
                      extraFieldsDTO.setValue(formattedDate);
                    } catch (Exception e) {
                      // System.out.println("ERROR WHILE ADDING EXTRA IN"+
                      // listExtraFieldName.get(i).getName()+" for row->"+ind+1);
                      errorFlag = 1;
                      if (errorDesc.length() > 0) {
                        errorDesc += ", ";
                      }
                      errorDesc +=
                          "ERROR WHILE ADDING IN "
                              + listExtraFieldName.get(i).getName().toUpperCase();
                    }
                  } else {
                    extraFieldsDTO.setValue(value);
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

            //		               listExtraFieldName.stream().forEach((x)->{
            //
            //
            //		               });

          }
        }

        Cell cell2 = myrow.createCell(1);
        cell2.setCellValue(errorDesc);
        if (errorFlag == 1) {
          // System.out.println("Inside errorFLag");

          // Close the workbook to release resources
          excelIndex++;
        }
        ind++;
        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now().toString());
        long complete = (currCount * 100) / totalCount;
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
        //				System.out.println(importHistoryDTO);

      }
      if (excelIndex > 0) {
        importHistoryDTO.setMessage("We have sent import result via email");
        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
          workbook.write(fileOut);
        }
        workbook.close();
        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);

          helper.setTo(email);
          helper.setSubject("Import Report from AssetYug");
          helper.setText("Hey, We have attached import result below");
          helper.addAttachment("CustomerAttachment.xlsx", new File("Report.xlsx"));

          emailSender.send(message);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error in sending mail: " + e);
        }
      }
      if (excelIndex == 0) {
        importHistoryDTO.setMessage("Import was Successfully Done");
        try (FileOutputStream fileOut = new FileOutputStream("Report.xlsx")) {
          workbook.write(fileOut);
        }
        workbook.close();
        try {
          MimeMessage message = emailSender.createMimeMessage();
          MimeMessageHelper helper = new MimeMessageHelper(message, true);

          helper.setTo(email);
          helper.setSubject("Import Report from AssetYug");
          helper.setText("Hey, Your Import was Successfully Done");
          //	            helper.addAttachment("ExcelAttachment.xlsx", new File("Report.xlsx"));

          emailSender.send(message);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error in sending mail: " + e);
        }
      }

      //	        //System.out.println("-------|||||-------> "+assetList.size());
      //	        assetsService.importExcel(assetList);
      importHistoryDTO.setStatus("Completed");
    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      e.printStackTrace();
    }
    customerService.addImportHistory(importHistoryDTO);
    //		System.out.println(importHistoryDTO);
    log.info("Import History {}", importHistoryDTO);
  }

  @PostMapping("/importUpdation/{companyId}/{email}")
  public void importUpdation(
      @RequestParam("file") MultipartFile file,
      @RequestParam("columnMappings") String columnMappings,
      @PathVariable String companyId,
      @PathVariable String email)
      throws CsvValidationException,
          JsonParseException,
          IOException,
          MessagingException,
          ImportFileRowException {

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
      if (reader.lines().count() > 5001) {
        throw new ImportFileRowException("Import File cannot import more than 5000 rows");
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
        Row myrow = sheet.createRow(excelIndex);
        Cell cell1 = myrow.createCell(0);
        cell1.setCellValue("Row " + (int) (ind + 1));
        int errorFlag = 0;
        String errorDesc = "";
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
                  errorDesc += "ERROR WHILE UPDATING IN INVENTORYID";
                }
                break;

              case "name":
                companyCustomer.setName(row[j]);
                break;

              case "phone":
                companyCustomer.setPhone(row[j]);
                break;
              case "category":
                System.out.println("category//->" + row[j]);
                companyCustomer.setCategory(row[j]);
                break;
              case "email":
                companyCustomer.setEmail(row[j]);
              case "address":
                System.out.println("address//->" + row[j]);
                companyCustomer.setAddress(row[j]);
                break;
              case "apartment":
                System.out.println("Apartment//->" + row[j]);
                companyCustomer.setApartment(row[j]);
                break;
              case "city":
                companyCustomer.setCity(row[j]);
                break;
              case "state":
                companyCustomer.setState(row[j]);
                break;
              case "zipcode":
                System.out.println("zipCode//->" + row[j]);
                companyCustomer.setZipCode(Integer.parseInt(row[j]));
                break;
              case "status":
                if ((row[j].toLowerCase().equals("active"))
                    || (row[j].toLowerCase().equals("inactive"))) {

                  if (row[j].toLowerCase().equals("active")) {
                    companyCustomer.setStatus("active");
                  } else {
                    companyCustomer.setStatus("inActive");
                  }

                  errorFlag = 0;
                  break;

                } else {
                  // System.out.println("ERROR WHILE ADDING IN Status for line->"+ind);
                  if (errorDesc.length() > 0) {
                    errorDesc += ", ";
                  }
                  errorDesc += "ERROR WHILE ADDING IN STATUS";
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
                    .toLowerCase()
                    .equals(listExtraFieldName.get(i).getName().toLowerCase())) {
                  CompanyCustomerExtraFields extraFieldsDTO = new CompanyCustomerExtraFields();
                  CompanyCustomerExtraFields extraFieldsOptional =
                      extraFieldsRepository.findByNameAndCompanyCustomerId(
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
                        Integer val = Integer.parseInt(value);
                        System.out.println("-----------------extra---->" + val + "->" + value);
                        extraFieldsDTO.setValue(val.toString());
                      } catch (Exception e) {
                        System.out.println(
                            "ERROR WHILE ADDING EXTRA IN"
                                + listExtraFieldName.get(i).getName()
                                + " for row->"
                                + ind);
                        errorFlag = 1;
                        if (errorDesc.length() > 0) {
                          errorDesc += ", ";
                        }
                        errorDesc +=
                            "ERROR WHILE ADDING IN "
                                + listExtraFieldName.get(i).getName().toUpperCase();
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
                        if (errorDesc.length() > 0) {
                          errorDesc += ", ";
                        }
                        errorDesc +=
                            "ERROR WHILE ADDING IN "
                                + listExtraFieldName.get(i).getName().toUpperCase();
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
                        Integer val = Integer.parseInt(value);
                        System.out.println("-----------------extra---->" + val + "->" + value);
                        extraFieldsDTO.setValue(val.toString());
                      } catch (Exception e) {
                        System.out.println(
                            "ERROR WHILE ADDING EXTRA IN"
                                + listExtraFieldName.get(i).getName()
                                + " for row->"
                                + ind);
                        errorFlag = 1;
                        if (errorDesc.length() > 0) {
                          errorDesc += ", ";
                        }
                        errorDesc +=
                            "ERROR WHILE ADDING IN "
                                + listExtraFieldName.get(i).getName().toUpperCase();
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
                        if (errorDesc.length() > 0) {
                          errorDesc += ", ";
                        }
                        errorDesc +=
                            "ERROR WHILE ADDING IN "
                                + listExtraFieldName.get(i).getName().toUpperCase();
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
          //	            InventoryDTO inventoryDTO = modelMapper.map(inventory, InventoryDTO.class);
          companyCustomerRepository.save(companyCustomer);
          // -------------------------------------------
        }

        //	            System.out.println();
        Cell cell2 = myrow.createCell(1);
        cell2.setCellValue(errorDesc);
        if (errorFlag == 1) {
          System.out.println("Inside errorFLag");

          // Close the workbook to release resources
          excelIndex++;
        }
        ind++;
        currCount++;
        importHistoryDTO.setDate(LocalDateTime.now().toString());
        long complete = (currCount * 100) / totalCount;
        importHistoryDTO.setComplete(complete);
        importHistoryDTO = customerService.addImportHistory(importHistoryDTO);
      }
      if (excelIndex > 0) {
        importHistoryDTO.setMessage("We have sent import result via email");
        try (FileOutputStream fileOut = new FileOutputStream("InventoryReport.xlsx")) {
          workbook.write(fileOut);
        }
        workbook.close();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Import Report from AssetYug");
        helper.setText("Hey, We have attached import result below");
        helper.addAttachment("InventoryAttachment.xlsx", new File("InventoryReport.xlsx"));

        emailSender.send(message);
      }
      if (excelIndex == 0) {
        importHistoryDTO.setMessage("Import was Successfully Done");
        try (FileOutputStream fileOut = new FileOutputStream("InventoryReport.xlsx")) {
          workbook.write(fileOut);
        }
        workbook.close();
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Import Report from AssetYug");
        helper.setText("Hey, Your Update was Successfully Done");
        //	            helper.addAttachment("ExcelAttachment.xlsx", new File("Report.xlsx"));

        emailSender.send(message);
      }

      importHistoryDTO.setStatus("Completed");

      //	        assetsService.importExcel(assetList);

    } catch (IOException e) {
      importHistoryDTO.setStatus("Failed");
      importHistoryDTO.setMessage(e.getMessage());
      e.printStackTrace();
    }
    customerService.addImportHistory(importHistoryDTO);
    //		System.out.println(importHistoryDTO);
    log.info("Import History {}", importHistoryDTO);
  }

  @GetMapping(value = "/statelist")
  public ResponseEntity<List<String>> statelist() {
    List<String> stateList =
        List.of(
            "Alaska",
            "Arizona",
            "Arkansas",
            "California",
            "Colorado",
            "Connecticut",
            "Delaware",
            "Florida",
            "Georgia",
            "Hawaii",
            "Idaho",
            "Illinois",
            "Indiana",
            "Iowa",
            "Kansas",
            "Kentucky",
            "Louisiana",
            "Maine",
            "Maryland",
            "Massachusetts",
            "Michigan",
            "Minnesota",
            "Mississippi",
            "Missouri",
            "Montana",
            "Nebraska",
            "Nevada",
            "New Hampshire",
            "New Jersey",
            "New Mexico",
            "New York",
            "North Carolina",
            "North Dakota",
            "Ohio",
            "Oklahoma",
            "Oregon",
            "Pennsylvania",
            "Rhode Island",
            "South Carolina",
            "South Dakota",
            "Tennessee",
            "Texas",
            "Utah",
            "Vermont",
            "Virginia",
            "Washington",
            "West Virginia",
            "Wisconsin",
            "Wyoming");

    return ResponseEntity.ok(stateList);
  }

  @PostMapping(value = "/addCategory")
  public void addCategory(@RequestBody CategoryDTO categoryDTO) throws CategoryException {
    System.out.println("Category===>" + categoryDTO);
    companyCustomerService.addCategory(categoryDTO);
  }
  @GetMapping(value = "/countCompanyCustomerByCategory/{category}")
  public int countCompanyCustomerByCategory(@PathVariable String category) throws CategoryException {
    System.out.println("Category===>" + category);
    return companyCustomerService.countCompanyCustomerByCategory(category);
  }

  @GetMapping(value = "/getCategoryList/{companyId}")
  public List<CompanyCustomerCategory> getCategoryList(@PathVariable String companyId) {
    return companyCustomerService.getCategoryList(companyId);
  }

  @GetMapping(value = "/getCategoryActiveList/{companyId}")
  public List<CompanyCustomerCategory> getCategoryActiveList(@PathVariable String companyId) {
    return companyCustomerService.getActiveCategoryList(companyId);
  }

  @DeleteMapping(value = "/deleteCategory/{id}")
  public void deleteCategory(@PathVariable String id) {
    companyCustomerService.deleteCategory(id);
  }

  @GetMapping(value = "/getCategoryListById/{companyId}/{id}")
  public CompanyCustomerCategory getCategoryById(
      @PathVariable String companyId, @PathVariable String id) {
    return companyCustomerService.getCategoryListById(companyId, id);
  }

  @PutMapping(value = "/updateCategory")
  public void updateCategory(@RequestBody CategoryDTO categoryDTO) {
    companyCustomerService.updateCategory(categoryDTO);
  }
}
