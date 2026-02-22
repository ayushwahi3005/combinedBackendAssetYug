package com.quantumai.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.IdGenerator.AssetCategoryIdGenerator;
import com.quantumai.customer.entity.IdGenerator.CompanyCustomerCategoryIdGenerator;
import com.quantumai.customer.entity.IdGenerator.CompanyCustomerIdTable;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.EmailAlreadyExistsException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.exception.ExtraFieldDeletionException;
import com.quantumai.customer.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class CompanyCustomerServiceImpl implements CompanyCustomerService {

  @Autowired
  CompanyCustomerRepository companyCustomerRepository;

  @Autowired
  CompanyCustomerCategoryRepository companyCustomerCategoryRepository;
  @Autowired
  private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;

  @Autowired
  private CompanyCustomerMandatoryFieldsRepository mandatoryFieldsRepository;
  @Autowired
  private CompanyCustomerShowFieldsRepository showFieldsRepository;
  @Autowired
  private CompanyCustomerIdTableRepository idTableRepository;

  @Autowired
  private CompanyCustomerExtraFieldsRepository extraFieldsRepository;

  @Autowired
  CompanyCustomerFileRepository companyCustomerFileRepository;

  @Autowired
  private CompanyCustomerShowFieldsRepository companyCustomerShowFieldsRepository;;

  @Autowired
  private CompanyCustomerCategoryIdGeneratorRepository companyCustomerCategoryIdGeneratorRepository;

  @Autowired
  private com.quantumai.customer.repository.CompanyInformationRepository companyInformationRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

   @Autowired
   private CompanyCustomerRepositoryCustom companyCustomerRepositoryCustom;

  private static final String SEQ_ID = "company_customer_category_sequence";

  private ModelMapper modelMapper = new ModelMapper();

  // private void handleDuplicateKey(CompanyCustomer companyCustomer, Exception e) throws EmailAlreadyExistsException {
  //   System.out.println("Email already exists------- ");
  //   Throwable rootCause = ExceptionUtils.getRootCause(e);
  //   if (rootCause instanceof MongoWriteException ||
  //       rootCause instanceof com.mongodb.MongoWriteConcernException ||
  //       rootCause instanceof com.mongodb.MongoCommandException ||
  //       rootCause instanceof com.mongodb.MongoWriteException ||
  //       rootCause instanceof com.mongodb.MongoWriteException ||
  //       rootCause instanceof com.mongodb.MongoWriteException) {
  //     throw new EmailAlreadyExistsException("Email already exists: " + companyCustomer.getEmail());
  //   }

  //   if (rootCause instanceof DuplicateKeyException) {
  //     throw new EmailAlreadyExistsException("Email already exists: " + companyCustomer.getEmail());
  //   }
  //   throw new RuntimeException("Error saving customer", e);
  // }

  @Override
  public CompanyCustomerDTO addCustomer(CompanyCustomerDTO myCustomerDTO) throws EmailAlreadyExistsException {
    CompanyCustomer companyCustomer = modelMapper.map(myCustomerDTO, CompanyCustomer.class);

    if (companyCustomer.getCompanyCustomerId() == null) {
      Optional<CompanyCustomerIdTable> optionalIdTable = idTableRepository
          .findByCompanyId(myCustomerDTO.getCompanyId());
      if (optionalIdTable.isEmpty()) {
        companyCustomer.setCompanyCustomerId(1);
        CompanyCustomerIdTable myidTable = new CompanyCustomerIdTable();
        myidTable.setTableId(2);
        myidTable.setCompanyId(myCustomerDTO.getCompanyId());
        idTableRepository.save(myidTable);
      } else {
        CompanyCustomerIdTable idTable = optionalIdTable.get();
        companyCustomer.setCompanyCustomerId(idTable.getTableId());
        idTable.updateId();
        idTableRepository.save(idTable);
      }
    }

    companyCustomer.setUpdatedAt(LocalDateTime.now().toString());

      if(companyCustomer.getEmail()!=null&&!companyCustomer.getEmail().trim().isEmpty()&&companyCustomerRepository.findByEmailAndCompanyId(companyCustomer.getEmail(),companyCustomer.getCompanyId()).isPresent()){
        throw new EmailAlreadyExistsException("User With Email Already Present");
      }
      else{
          log.info(companyCustomer.toString());
        CompanyCustomer saved = companyCustomerRepository.save(companyCustomer);
        return modelMapper.map(saved, CompanyCustomerDTO.class);
      }
      
  
  }

  @Override
  public CompanyCustomerDTO getCustomer(String id) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomer> companyCustomerOptional = companyCustomerRepository.findById(id);
    System.out.println(id);
    CompanyCustomerDTO companyCustomerDTO = modelMapper.map(companyCustomerOptional.get(), CompanyCustomerDTO.class);
    return companyCustomerDTO;
  }

  @Override
  public List<CompanyCustomerDTO> getAllCustomer(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomer> companyCustomerList = companyCustomerRepository.findByCompanyId(companyId);
    System.out.println(
        "-----------------------my list---------------->" + companyCustomerList.size());
    List<CompanyCustomerDTO> companyCustomerDTOList = new ArrayList<>();
    companyCustomerList.stream()
        .forEach(
            (x) -> {
              CompanyCustomerDTO companyCustomerDTO = modelMapper.map(x, CompanyCustomerDTO.class);
              System.out.println(companyCustomerDTO.getCompanyCustomerId());
              companyCustomerDTO.getCompanyCustomerId();
              companyCustomerDTOList.add(companyCustomerDTO);
            });
    return companyCustomerDTOList;
  }

  @Override
  public void updateCustomer(CompanyCustomerDTO companyCustomerDTO) throws EmailAlreadyExistsException {
    // Find existing customer
    Optional<CompanyCustomer> companyCustomerOptional = companyCustomerRepository.findById(companyCustomerDTO.getId());

    if (companyCustomerOptional.isEmpty()) {
      throw new RuntimeException("Customer not found with id: " + companyCustomerDTO.getId());
    }

    // Map DTO to entity
    CompanyCustomer companyCustomer = modelMapper.map(companyCustomerDTO, CompanyCustomer.class);

    // Always update timestamp
    companyCustomer.setUpdatedAt(LocalDateTime.now().toString());
    if(companyCustomer.getEmail()!=null&&!companyCustomer.getEmail().isEmpty()){
    Optional<CompanyCustomer> customer=companyCustomerRepository.findByEmailAndCompanyId(companyCustomer.getEmail(),companyCustomer.getCompanyId());
        if(customer.isPresent()&&!customer.get().getId().equals(companyCustomer.getId())){
          throw new EmailAlreadyExistsException("User With Email Aready Present");
        }

    }
      companyCustomerRepository.save(companyCustomer);
   
  }

  @Override
  public void deleteCustomer(String id) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomer> companyCustomerOptional = companyCustomerRepository.findById(id);
    companyCustomerRepository.delete(companyCustomerOptional.get());
  }

  @Override
  public List<String> getAllCustomerWithExtraColumns(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFieldName> extraFieldNameList = extraFieldNameRepository.findByCompanyId(companyId);
    // Optional<WorkOrder>
    // workOrderOptional=workOrderRepository.findById(workOrderId);
    List<CompanyCustomer> workOrderList = companyCustomerRepository.findByCompanyId(companyId);
    // WorkOrder workOrder=workOrderOptional.get();
    // WorkOrderWithExtraFieldsDTO workOrderWithExtraFieldsDTO =
    // modelMapper.map(workOrder,
    // WorkOrderWithExtraFieldsDTO.class);

    List<String> mapList = new ArrayList<>();
    workOrderList.stream()
        .forEach(
            (order) -> {
              List<CompanyCustomerExtraFields> extraFieldsList = extraFieldsRepository
                  .findByCompanyCustomerId(order.getId());
              Map<String, String> m = new HashMap<>();
              extraFieldNameList.stream()
                  .forEach(
                      (x) -> {
                        m.put(x.getName(), "");
                        extraFieldsList.stream()
                            .forEach(
                                (x1) -> {
                                  m.put(x1.getName(), x1.getValue());
                                });
                      });
              m.put("id", order.getId());
              m.put("name", order.getName());
              m.put("companyId", order.getCompanyId().toString());
              m.put("category", order.getCategory());
              m.put("status", order.getStatus());
              m.put("address", order.getAddress());
              m.put("email", order.getEmail());
              m.put("apartment", order.getApartment());
              m.put("city", order.getCity());
              m.put("state", order.getState());
              m.put("country",order.getCountry());
              m.put("companyCustomerId", order.getCompanyCustomerId().toString());
              m.put("updatedAt", order.getUpdatedAt());
              if (order.getPhone() != null)
                m.put("phone", order.getPhone().toString());
              if (order.getZipCode() != null)
                m.put("zipCode", order.getZipCode().toString());

              ObjectMapper objectMapper = new ObjectMapper();
              try {
                // Convert POJO to JSON string
                String json = objectMapper.writeValueAsString(m);

                mapList.add(json);
                // System.out.print(json);
                // System.out.print(m);

              } catch (Exception e) {
                e.printStackTrace();
              }
            });

    return mapList;
  }

  @Override
  public List<String> searchedCompanyCustomer(Long companyId, String search, String category) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> sortCompanyCustomer(Long companyId, String category) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addCompanyCustomerExtraField(CompanyCustomerExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException {
    // TODO Auto-generated method stub
    CompanyCustomerExtraFieldName extraFieldNameNew = extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(
        extraFieldNameDTO.getName(), extraFieldNameDTO.getCompanyId());
    if (extraFieldNameNew != null) {
      throw new ExtraFieldAlreadyPresentException("Extra Field Already Present");
    }
    extraFieldNameDTO.setName(extraFieldNameDTO.getName());

    CompanyCustomerExtraFieldName extraFieldName = modelMapper.map(extraFieldNameDTO,
        CompanyCustomerExtraFieldName.class);
    extraFieldNameRepository.save(extraFieldName);
  }

  @Override
  public List<CompanyCustomerExtraFieldNameDTO> getCompanyCustomerExtraField(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFieldName> extraFieldNameList = extraFieldNameRepository.findByCompanyId(companyId);
    List<CompanyCustomerExtraFieldNameDTO> extraFieldNameListDTO = new ArrayList<>();
    extraFieldNameList.stream()
        .forEach(
            (x) -> {
              CompanyCustomerExtraFieldNameDTO extraFieldNameDTO = modelMapper.map(x,
                  CompanyCustomerExtraFieldNameDTO.class);
              extraFieldNameListDTO.add(extraFieldNameDTO);
            });
    return extraFieldNameListDTO;
  }

  @Override
  public void deleteCompanyCustomerExtraField(String id) throws Exception {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerExtraFieldName> extraFieldNameOptional = extraFieldNameRepository.findById(id);

      if(extraFieldNameOptional.isPresent()){
//          List<CompanyCustomerExtraFields> allDataFields=extraFieldsRepository.findByNameIgnoreCaseAndCompanyId(extraFieldNameOptional.get().getName(),extraFieldNameOptional.get().getCompanyId());
//          allDataFields=allDataFields.stream().filter((data)->!(data.getValue().isEmpty()||data.getValue().isBlank())).toList();
          Optional<CompanyCustomerShowFields> companyShowFieldsOptional=companyCustomerShowFieldsRepository.findByNameAndCompanyId(extraFieldNameOptional.get().getName(),
                          extraFieldNameOptional.get().getCompanyId());

          if(companyShowFieldsOptional.isPresent()){
              CompanyCustomerShowFields showFields=companyShowFieldsOptional.get();


                  long count = companyCustomerRepositoryCustom.countActiveAssetsWithExtraField(extraFieldNameOptional.get().getName(), extraFieldNameOptional.get().getCompanyId());
                  if (count == 0||!showFields.isShow()) {
                      extraFieldNameRepository.deleteById(id);
                      CompanyCustomerExtraFieldName extraFieldName = extraFieldNameOptional.get();
                      List<CompanyCustomerExtraFields> extraFieldsList = extraFieldsRepository
                              .findByName(extraFieldName.getName().toLowerCase());
                      extraFieldsRepository.deleteAll(extraFieldsList);
                      extraFieldsList
                              .forEach(
                                      (x) -> {
                                          log.info("Extra Field {} Deleted Successfully", x.getName());
//                                          extraFieldsRepository.delete(x);
                                      });
                  } else {
                      throw new ExtraFieldDeletionException("Cannot delete extra field as it is in use", count);
                  }



          }
          else{
              throw new Exception("Cannot delete extra field");
          }


      }

  }

  @Override
  public void updateMandatoryFields(CompanyCustomerMandatoryFields mandatoryFields) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerMandatoryFields> mandatoryFieldsOptional = mandatoryFieldsRepository.findByNameAndCompanyId(
        mandatoryFields.getName(), mandatoryFields.getCompanyId());
    CompanyCustomerMandatoryFields myMandatoryFields = new CompanyCustomerMandatoryFields();
    if (mandatoryFieldsOptional.isPresent()) {
      myMandatoryFields = mandatoryFieldsOptional.get();
      mandatoryFields.setId(myMandatoryFields.getId());
    }
    mandatoryFieldsRepository.save(mandatoryFields);
  }

  @Override
  public void updateShowFields(CompanyCustomerShowFields showFields) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerShowFields> showFieldsOptional = showFieldsRepository.findByNameAndCompanyId(
        showFields.getName(), showFields.getCompanyId());
    CompanyCustomerShowFields myShowFields = new CompanyCustomerShowFields();
    if (showFieldsOptional.isPresent()) {
      myShowFields = showFieldsOptional.get();
      showFields.setId(myShowFields.getId());
    }
    showFieldsRepository.save(showFields);
  }

  @Override
  public CompanyCustomerMandatoryFields getMandatoryFields(String name, Long companyId) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerMandatoryFields> mandatoryFieldsOptional = mandatoryFieldsRepository
        .findByNameAndCompanyId(name, companyId);
    if (mandatoryFieldsOptional.isPresent()) {
      return mandatoryFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public CompanyCustomerShowFields getShowFields(String name, Long companyId) {
    // TODO Auto-generated method stub
    // TODO Auto-generated method stub
    System.out.println("Servie====>" + name + " " + companyId);
    Optional<CompanyCustomerShowFields> showFieldsOptional = showFieldsRepository.findByNameAndCompanyId(name,
        companyId);
    // System.out.println("Servie2====>"+showFieldsOptional.get());
    if (showFieldsOptional.isPresent()) {
      return showFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public List<CompanyCustomerMandatoryFields> getAllMandatoryFields(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomerMandatoryFields> mandatoryFieldsList = mandatoryFieldsRepository.findByCompanyId(companyId);
    return mandatoryFieldsList;
  }

  @Override
  public List<CompanyCustomerShowFields> getAllShowFields(Long companyId) {
    // TODO Auto-generated method stub
    System.out.println("getAllShowFields service" + companyId);
    List<CompanyCustomerShowFields> showFieldsList = showFieldsRepository.findByCompanyId(companyId);
    System.out.println("showFieldsList size" + showFieldsList.size());
    return showFieldsList;
  }

  @Override
  public void deleteShowAndMandatoryFields(Long companyId, String name) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerShowFields> showFieldsOptional = showFieldsRepository.findByNameAndCompanyId(name,
        companyId);
    if (showFieldsOptional.isPresent()) {
      showFieldsRepository.delete(showFieldsOptional.get());
    }
    Optional<CompanyCustomerMandatoryFields> mandatoryFieldsOptional = mandatoryFieldsRepository
        .findByNameAndCompanyId(name, companyId);
    if (mandatoryFieldsOptional.isPresent()) {
      mandatoryFieldsRepository.delete(mandatoryFieldsOptional.get());
    }
  }

  @Override
  public Map<String, Map<String, String>> getextraFieldList(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFields> extraFieldNameList = extraFieldsRepository.findByCompanyId(companyId);
    List<CompanyCustomer> assetList = companyCustomerRepository.findByCompanyId(companyId);
    Map<String, Map<String, String>> fieldNameValueMap = new HashMap<>();

    assetList.stream()
        .forEach(
            (workorder) -> {
              Map<String, String> m = new HashMap<>();
              extraFieldNameList.stream()
                  .forEach(
                      (field) -> {
                        if (field.getCompanyCustomerId().endsWith(workorder.getId())) {
                          m.put(field.getName(), field.getValue());
                        }
                      });
              fieldNameValueMap.put(workorder.getId(), m);
            });
    return fieldNameValueMap;
  }

  @Override
  public void addExtraFields(CompanyCustomerExtraFieldsDTO extraFieldsDTO) {
    // TODO Auto-generated method stub
    extraFieldsDTO.setName(extraFieldsDTO.getName());

    // List<CompanyCustomerExtraFields>
    // extraFieldsList=extraFieldsRepository.findByName(extraFieldsDTO.getName().toLowerCase());
    // if(!extraFieldsList.isEmpty()) {
    // throw new Exception("Extra Field Already Present");
    // }
    CompanyCustomerExtraFields extraFields = modelMapper.map(extraFieldsDTO, CompanyCustomerExtraFields.class);
    extraFieldsRepository.save(extraFields);
  }

  @Override
  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(String id) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFields> extraFieldsList = extraFieldsRepository.findByCompanyCustomerId(id);
    if (extraFieldsList.isEmpty()) {
      return null;
    }
    List<CompanyCustomerExtraFieldsDTO> extraFieldsDTOList = new ArrayList<>();
    extraFieldsList.stream()
        .forEach(
            (x) -> {
              CompanyCustomerExtraFieldsDTO extraFieldsDTO = modelMapper.map(x, CompanyCustomerExtraFieldsDTO.class);
              extraFieldsDTOList.add(extraFieldsDTO);
            });
    return extraFieldsDTOList;
  }

  @Override
  public void deleteExtraFields(String id) throws Exception {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerExtraFields> extraFields = extraFieldsRepository.findById(id);
    if (extraFields.isEmpty()) {
      throw new Exception("No such extra Field");
    }
    extraFieldsRepository.delete(extraFields.get());
  }

  @Override
  public void deleteExtraFieldByCompanyCustomer(String id) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFields> extraFieldsList = extraFieldsRepository.findByCompanyCustomerId(id);

    extraFieldsList.stream()
        .forEach(
            (x) -> {
              extraFieldsRepository.delete(x);
            });
  }

  @Override
  public CompanyCustomerFile addCompanyCustomerFile(MultipartFile file, String companyCustomerId)
      throws IOException {
    // TODO Auto-generated method stub
    String fileName = StringUtils.cleanPath(file.getOriginalFilename());
    System.out.println("Add File working-----------------------------------//////");
    CompanyCustomerFile assetFile = new CompanyCustomerFile();
    assetFile.setCompanyCustomerId(companyCustomerId);

    assetFile.setFileName(fileName);
    assetFile.setUploadDateTime(LocalDateTime.now());

    try {
      assetFile.setFile(file.getBytes());
    } catch (IOException e) {
      // Handle file read error
      throw new IOException("Failed to read file: " + fileName, e);
    }

    return companyCustomerFileRepository.save(assetFile);
  }

  @Override
  public List<CompanyCustomerFileDTO> getCompanyCustomerFile(String companyCustomerId) {
    // TODO Auto-generated method stub
    System.out.println("Inside getCompanyCustomerFile" + companyCustomerId);
    List<CompanyCustomerFile> companyCustomerList = companyCustomerFileRepository
        .findByCompanyCustomerId(companyCustomerId);
    System.out.println(companyCustomerList.size());
    if (companyCustomerList.size() == 0) {
      return null;
    } else {
      System.out.println("Else filr");
      List<CompanyCustomerFileDTO> companyCustomerListDTOList = new ArrayList<>();
      companyCustomerList.stream()
          .forEach(
              (x) -> {
                CompanyCustomerFileDTO assetFileDTO = modelMapper.map(x, CompanyCustomerFileDTO.class);

                companyCustomerListDTOList.add(assetFileDTO);
              });

      return companyCustomerListDTOList;
    }
  }

  @Override
  public CompanyCustomerFileDTO downloadFile(String id) {
    // TODO Auto-generated method stub
    Optional<CompanyCustomerFile> companyCustomerFile = companyCustomerFileRepository.findById(id);
    CompanyCustomerFileDTO companyCustomerFileDTO = modelMapper.map(companyCustomerFile.get(),
        CompanyCustomerFileDTO.class);
    return companyCustomerFileDTO;
  }

  @Override
  public void deleteFile(String id) {
    // TODO Auto-generated method stub
    companyCustomerFileRepository.deleteById(id);
  }

  @Override
  public CompanyCustomerDTO getCompanyCustomerByLocalId(Integer id, Long companyId) {
    // TODO Auto-generated method stub
    CompanyCustomer companyCustomerOptional = companyCustomerRepository.findByCompanyCustomerIdAndCompanyId(id,
        companyId);
    if (companyCustomerOptional == null) {
      return null;
    }
    System.out.println(id);
    CompanyCustomerDTO companyCustomerDTO = modelMapper.map(companyCustomerOptional, CompanyCustomerDTO.class);
    return companyCustomerDTO;
  }

  @Override
  public PaginatedResultDTO<String> getAllCustomerDetails(Long companyId) {
    // TODO Auto-generated method stub
    List<CompanyCustomerExtraFieldName> extraFieldNameList = extraFieldNameRepository.findByCompanyId(companyId);

    List<CompanyCustomer> assetList = companyCustomerRepository.findByCompanyId(companyId);

    List<String> mapList = new ArrayList<>();
    assetList.stream()
        .forEach(
            (order) -> {
              List<CompanyCustomerExtraFields> extraFieldsList = extraFieldsRepository
                  .findByCompanyCustomerId(order.getId());
              Map<String, String> m = new HashMap<>();
              extraFieldNameList.stream()
                  .forEach(
                      (x) -> {
                        m.put(x.getName(), "");
                        extraFieldsList.stream()
                            .forEach(
                                (x1) -> {
                                  m.put(x1.getName(), x1.getValue());
                                });
                      });
              m.put("id", order.getId());
              m.put("name", order.getName());
              m.put("email", order.getEmail());
              m.put("phone", order.getPhone() == null ? "" : order.getPhone().toString());
              m.put("companyCustomerId", order.getCompanyCustomerId().toString());
              m.put("companyId", order.getCompanyId().toString());
              m.put("address", order.getAddress());
              m.put("category", order.getCategory());
              m.put("apartment", order.getApartment());
              m.put("city", order.getCity());
              m.put("state", order.getState());
              m.put("country",order.getCountry());
              m.put("status", order.getStatus());
              m.put("updatedAt", order.getUpdatedAt());
              if (order.getZipCode() != null) {
                m.put("zipCode", order.getZipCode().toString());
              }

              ObjectMapper objectMapper = new ObjectMapper();
              try {
                // Convert POJO to JSON string
                String json = objectMapper.writeValueAsString(m);

                mapList.add(json);

              } catch (Exception e) {
                e.printStackTrace();
              }
            });

    return new PaginatedResultDTO<>(mapList, mapList.size());
  }

  @Override
  public PaginatedResultDTO<String> sortCustomers(
      Long companyId, String field, Integer pageNumber, Integer pageSize) {
    System.out.println("--->" + field);
    CompanyCustomerExtraFieldName extraFieldName = extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(field,
        companyId);
    // //System.out.println(extraFieldName);

    List<Map<String, String>> mapList = new ArrayList<>();
    PaginatedResultDTO<String> myList = getAllCustomerDetails(companyId);
    ObjectMapper objectMapper = new ObjectMapper();
    myList.getData().stream()
        .forEach(
            (asset) -> {
              Map<String, String> m = new HashMap<>();
              try {

                m = objectMapper.readValue(asset, new TypeReference<Map<String, String>>() {
                });
                mapList.add(m);

              } catch (Exception e) {

                e.printStackTrace();
              }
            });

    Comparator<Map<String, String>> customComparator = null;

    customComparator = Comparator.comparing(m -> m.get(field));

    List<Map<String, String>> res = mapList.stream().sorted(customComparator).collect(Collectors.toList());
    // //System.out.println(res);
    // //System.out.println("------------------------->"+res.size());
    List<String> resList = new ArrayList<>();
    for (int i = 0; i < res.size(); i++) {
      try {

        String json = objectMapper.writeValueAsString(res.get(i));

        resList.add(json);
        System.out.print(json);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // res.stream().forEach((mydata)->{
    // try {
    //// // Convert POJO to JSON string
    // String json = objectMapper.writeValueAsString(mydata);
    //
    // resList.add(json);
    // System.out.print(json);
    //
    //
    //
    // } catch (Exception e) {
    // e.printStackTrace();
    //
    //
    // }
    // });
    // String jsonString;
    // try {
    // jsonString = objectMapper.writeValueAsString(res);
    // resList = objectMapper.readValue(jsonString, new
    // TypeReference<List<String>>() {});
    // } catch (JsonProcessingException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }

    int startItem = pageNumber * pageSize;
    int endItem = Math.min(startItem + pageSize, resList.size());
    if (startItem > resList.size()) {

      return new PaginatedResultDTO<>(Collections.emptyList(), 0);
    }
    int totalPage = resList.size();
    resList = resList.subList(startItem, endItem);
    return new PaginatedResultDTO<>(resList, totalPage);
  }

  @Override
  public PaginatedResultDTO<String> advanceFilter(
      Object filter,
      int pageNumber,
      int pageSize,
      String sortField,
      String searchData,
      Boolean asc) {
    // TODO Auto-generated method stub
    System.out.println("pageNumber: " + pageNumber + ", pageSize: " + pageSize);
    List<String> filteredAssetsWithAllFields = new ArrayList<>();
    long totalPage = 0;
    System.out.println("----filter--->" + sortField + " " + searchData);
    if (filter instanceof Map) {
      // Cast the filter to a Map
      Map<?, ?> filterMap = (Map<?, ?>) filter;

      // Get all keys
      Set<?> keys = filterMap.keySet();
      Map<String, String> mapping = new HashMap<String, String>();

      // Print keys or do something with them

      for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue();
         System.out.println("Key: " + key + ", Value: " + value);
        if (value != null) {
          mapping.put(key.toString(), value.toString());
        }
      }
      PaginatedResultDTO<String> assetsWithAllFields = getAllCustomerDetails(Long.parseLong(mapping.get("companyId")));
      System.out.println(
          "total1->"
              + assetsWithAllFields.getData().size()
              + " "
              + assetsWithAllFields.getTotalRecords());

      filteredAssetsWithAllFields = assetsWithAllFields.getData().stream()
          .filter(
              data -> {
                ObjectMapper mapper = new ObjectMapper();
                int flag = 1;
                try {
                  Map<String, String> map = mapper.readValue(data, Map.class);
                  for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                      mapping.put(key.toString(), value.toString());
                      String myValue = map.get(key);
                      String expectedValue = value.toString();
                      String keyString = key.toString();
                      if (!keyString.equals("companyId") && value != null && !value.toString().isEmpty()) {
                        myValue = myValue != null ? myValue.toLowerCase() : "";
                        expectedValue = expectedValue.toLowerCase();
                        
                        // For status field, do an exact match
                        if (keyString.equals("status")) {
                            if (!myValue.equals(expectedValue)) {
                                flag = 0;
                            }
                        } 
                        // For other fields, use contains for partial matching
                        else if (!myValue.contains(expectedValue)) {
                            flag = 0;
                        }
                      }
                    }
                  }
                  if (flag == 1) {
                    return true;
                  }
                } catch (JsonMappingException e) {
                  e.printStackTrace();
                } catch (JsonProcessingException e) {
                  e.printStackTrace();
                }
                return false;
              })
          .collect(Collectors.toList());
      System.out.println("total->" + filteredAssetsWithAllFields.size());
      // Sorting if it is enabled

      System.out.println("Sort-" + sortField + " " + sortField.length());
      System.out.println("Search-" + searchData);
      if (sortField != null && (sortField.trim().equals("") == false)) {
        System.out.println("going inside-" + sortField);
        ObjectMapper objectMapper = new ObjectMapper();
        Comparator<String> customComparator = Comparator.comparing(
            data -> {
              System.out.println("inside comparator-" + (String) data);
              Map<String, String> myMap = new HashMap<>();
              try {
                myMap = objectMapper.readValue(
                    (String) data, new TypeReference<Map<String, String>>() {
                    });
              } catch (JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
              System.out.println(" comparator-" + myMap.get(sortField));
              return myMap.get(sortField).toLowerCase();
            },
            String.CASE_INSENSITIVE_ORDER);
        if (asc == true) {

          filteredAssetsWithAllFields = filteredAssetsWithAllFields.stream()
              .sorted(customComparator)
              .collect(Collectors.toList());
        } else {
          filteredAssetsWithAllFields = filteredAssetsWithAllFields.stream()
              .sorted(customComparator.reversed())
              .collect(Collectors.toList());
        }
      }

      // System.out.println("total3->"+filteredAssetsWithAllFields.size()+"
      // "+searchData.length()+" "+searchData.charAt(1));

      if (!searchData.isEmpty() && searchData != "") {
        System.out.println("---------->" + searchData);
        filteredAssetsWithAllFields = filteredAssetsWithAllFields.stream()
            .filter((data) -> data.toLowerCase().contains(searchData.toLowerCase()))
            .collect(Collectors.toList());
      }
      System.out.println("total4->" + filteredAssetsWithAllFields.size());
      int startItem = pageNumber * pageSize;
      int endItem = Math.min(startItem + pageSize, filteredAssetsWithAllFields.size());
      if (startItem > filteredAssetsWithAllFields.size()) {

        return new PaginatedResultDTO<>(Collections.emptyList(), 0);
      }
      totalPage = filteredAssetsWithAllFields.size();
      filteredAssetsWithAllFields = filteredAssetsWithAllFields.subList(startItem, endItem);

    } else {
      System.out.println("The filter is not a Map instance");
    }

    return new PaginatedResultDTO<>(filteredAssetsWithAllFields, totalPage);
  }

  @Override
  public String working() {
    return "Working!!!";
  }

  @Override
  public void addCategory(CategoryDTO categoryDTO) throws Exception {
    CompanyCustomerCategory category = modelMapper.map(categoryDTO, CompanyCustomerCategory.class);

    Optional<CompanyCustomerCategoryIdGenerator> companyCustomerCategoryIdGeneratorOptional = companyCustomerCategoryIdGeneratorRepository
        .findByCompanyId(categoryDTO.getCompanyId());
    if (companyCustomerCategoryIdGeneratorOptional.isEmpty()) {
      throw new Exception("Sequence Database Not Found");
    }
    CompanyCustomerCategoryIdGenerator companyCustomerCategoryIdGenerator = companyCustomerCategoryIdGeneratorOptional
        .get();
    Long id = companyCustomerCategoryIdGenerator.getSeq();
    category.setCompanyCustomerCategoryId(id);
    companyCustomerCategoryIdGenerator.setSeq(id + 1);
    companyCustomerCategoryIdGeneratorRepository.save(companyCustomerCategoryIdGenerator);

    Optional<CompanyCustomerCategory> OptionalCompanyCustomerCategory = companyCustomerCategoryRepository
        .findByNameAndCompanyId(categoryDTO.getName(), categoryDTO.getCompanyId());
    if (OptionalCompanyCustomerCategory.isEmpty()) {
      companyCustomerCategoryRepository.save(category);
    } else {
      throw new CategoryException("Category Already Present");
    }
  }

  @Override
  public List<CompanyCustomerCategory> getCategoryList(Long companyId) {

    List<CompanyCustomerCategory> categoryList = companyCustomerCategoryRepository.findByCompanyId(companyId);
    return categoryList;
  }

  @Override
  public List<CompanyCustomerCategory> getActiveCategoryList(Long companyId) {
    List<CompanyCustomerCategory> categoryList = companyCustomerCategoryRepository.findByCompanyIdAndStatus(companyId,
        "active");
    return categoryList;
  }

  @Override
  public void deleteCategory(String id) {
    Optional<CompanyCustomerCategory> category = companyCustomerCategoryRepository.findById(id);
    if (category.isPresent()) {
      companyCustomerCategoryRepository.delete(category.get());
    }
  }

  @Override
  public void updateCategory(CategoryDTO categoryDTO) {
    CompanyCustomerCategory category = modelMapper.map(categoryDTO, CompanyCustomerCategory.class);

    companyCustomerCategoryRepository.save(category);
  }

  @Override
  public int countCompanyCustomerByCategory(String category) {
    return companyCustomerRepository.countByCategory(category);
  }

  @Override
  public CompanyCustomerCategory getCategoryListById(Long companyId, String id) {
    Optional<CompanyCustomerCategory> categoryOptional = companyCustomerCategoryRepository.findById(id);
    return categoryOptional.orElse(null);
  }
    public void updateNameForCompanyCustomerExtraFields(String oldName,  String newName,Long companyId) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                        .and("companyId").is(companyId)
        );

        Update update = new Update().set("name", newName);
        mongoTemplate.updateMulti(query, update, CompanyCustomerExtraFields.class);
    }
    public void updateNameForMandatoryFields(String oldName,  String newName,Long companyId) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                        .and("companyId").is(companyId)
        );

        Update update = new Update().set("name", newName);
        mongoTemplate.updateMulti(query, update, CompanyCustomerMandatoryFields.class);
    }
    public void updateNameForShowFields(String oldName,  String newName,Long companyId) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                        .and("companyId").is(companyId)
        );

        Update update = new Update().set("name", newName);
        mongoTemplate.updateMulti(query, update, CompanyCustomerShowFields.class);
    }
    @Override
    public CompanyCustomerExtraFieldName updateExtraFieldName(ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
        Optional<CompanyCustomerExtraFieldName> optionalField=extraFieldNameRepository.findById(extraFieldNameUpdateDTO.getId());
        if(optionalField.isPresent()){
      String name=optionalField.get().getName();
      this.updateNameForCompanyCustomerExtraFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());
      this.updateNameForMandatoryFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());
      this.updateNameForShowFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());


      optionalField.get().setName(extraFieldNameUpdateDTO.getName());


      return extraFieldNameRepository.save(optionalField.get());
    }
    else{
      return null;
    }
  }

  @Override
  public com.quantumai.customer.dto.CompanyCustomerTemplateFieldsDTO getTemplateFields(Long companyId) {
    com.quantumai.customer.dto.CompanyCustomerTemplateFieldsDTO dto = new com.quantumai.customer.dto.CompanyCustomerTemplateFieldsDTO();
    // Removed 'Apartment' from standard fields
    List<String> standard = Arrays.asList(
        "Name","Email","Phone","Address","City","State","Country","ZipCode","Category","Status"
    );
    dto.setStandardFields(standard);
    List<CompanyCustomerExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    List<String> extraNames = new ArrayList<>();
    if (extraFieldNames != null) {
      for (CompanyCustomerExtraFieldName ef : extraFieldNames) extraNames.add(ef.getName());
    }
    dto.setExtraFields(extraNames);
    // categories
    List<CompanyCustomerCategory> categories = companyCustomerCategoryRepository.findByCompanyId(companyId);
    List<String> categoryNames = new ArrayList<>();
    if (categories != null) {
      for (CompanyCustomerCategory c : categories) categoryNames.add(c.getName());
    }
    dto.setCategories(categoryNames);
    return dto;
  }

  @Override
  public byte[] generateCompanyCustomerTemplateXlsx(Long companyId) throws IOException {
    // Determine default state/country using CompanyInformation if available
    String defaultCountry = "United States of America";
    String defaultState = "Alabama";
    try {
      Optional<com.quantumai.customer.entity.CompanyInformation> ciOpt = companyInformationRepository.findById(companyId);
      if (ciOpt.isPresent()) {
        com.quantumai.customer.entity.CompanyInformation ci = ciOpt.get();
        if (ci.getCountry() != null && !ci.getCountry().isBlank()) defaultCountry = ci.getCountry();
        if (ci.getState() != null && !ci.getState().isBlank()) defaultState = ci.getState();
      }
    } catch (Exception ex) {
      // fallback to defaults
      log.debug("CompanyInformation lookup failed for companyId {}: {}", companyId, ex.getMessage());
    }
    List<CompanyCustomerExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    List<CompanyCustomerCategory> categories = companyCustomerCategoryRepository.findByCompanyId(companyId);
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("CustomerTemplate");
    Row header = sheet.createRow(0);
    int col = 0;
    // Removed 'Apartment' column as requested
    String[] standardFields = new String[]{"Name","Email","Phone","Address","City","State","Country","Zip Code","Category","Status"};
    for (String h : standardFields) header.createCell(col++).setCellValue(h);
    if (extraFieldNames != null) {
      for (CompanyCustomerExtraFieldName ef : extraFieldNames) header.createCell(col++).setCellValue(ef.getName());
    }

    // Add mock/sample rows (3 rows)
    int sampleRows = 3;
    for (int r = 1; r <= sampleRows; r++) {
      Row row = sheet.createRow(r);
      int c = 0;
      for (String field : standardFields) {
        switch (field.toLowerCase()) {
          case "name":
            row.createCell(c++).setCellValue("Sample Customer " + r);
            break;
          case "email":
            row.createCell(c++).setCellValue("sample" + r + "@example.com");
            break;
          case "phone":
            row.createCell(c++).setCellValue("+1-555-010" + (10 + r));
            break;
          case "address":
            // Address sample without Apartment column
            row.createCell(c++).setCellValue("123 Sample St");
            break;
          case "city":
            row.createCell(c++).setCellValue("Sample City");
            break;
          case "state":
            row.createCell(c++).setCellValue(defaultState);
            break;
          case "country":
            row.createCell(c++).setCellValue(defaultCountry);
            break;
          case "zip code":
            row.createCell(c++).setCellValue("1000" + r);
            break;
          case "category":
            if (categories != null && !categories.isEmpty()) {
              row.createCell(c++).setCellValue(categories.get((r - 1) % categories.size()).getName());
            } else {
              row.createCell(c++).setCellValue("DefaultCategory");
            }
            break;
          case "status":
            row.createCell(c++).setCellValue(r % 2 == 0 ? "inactive" : "active");
            break;
          default:
            row.createCell(c++).setCellValue("");
        }
      }

      // Extra fields
      if (extraFieldNames != null) {
        for (CompanyCustomerExtraFieldName ef : extraFieldNames) {
          String type = ef.getType() == null ? "string" : ef.getType().toLowerCase();
          String cellVal = "";
          try {
            if (type.contains("number") || type.equals("int") || type.equals("integer") || type.equals("double") || type.equals("float")) {
              // numeric sample
              Cell cell = row.createCell(c++);
              cell.setCellValue(r * 10);
              continue;
            } else if (type.contains("date")) {
              cellVal = java.time.LocalDate.now().minusDays(r).toString();
            } else if (type.contains("email")) {
              cellVal = "sample" + r + "@example.com";
            } else if (type.contains("bool") || type.contains("checkbox")) {
              cellVal = (r % 2 == 0) ? "TRUE" : "FALSE";
            } else {
              cellVal = "SampleValue" + r;
            }
          } catch (Exception ex) {
            cellVal = "SampleValue" + r;
          }
          row.createCell(c++).setCellValue(cellVal);
        }
      }
    }

    for (int i = 0; i < Math.min(col, 60); i++) sheet.autoSizeColumn(i);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    workbook.close();
    return bos.toByteArray();
  }

  @Override
  public byte[] generateCompanyCustomerTemplateCsv(Long companyId) throws IOException {
    // Reuse logic used for XLSX template generation: get extra fields, categories and defaults
    List<CompanyCustomerExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    List<CompanyCustomerCategory> categories = companyCustomerCategoryRepository.findByCompanyId(companyId);

    // Determine default state/country using CompanyInformation if available
    String defaultCountry = "United States of America";
    String defaultState = "Alabama";
    try {
      Optional<com.quantumai.customer.entity.CompanyInformation> ciOpt = companyInformationRepository.findById(companyId);
      if (ciOpt.isPresent()) {
        com.quantumai.customer.entity.CompanyInformation ci = ciOpt.get();
        if (ci.getCountry() != null && !ci.getCountry().isBlank()) defaultCountry = ci.getCountry();
        if (ci.getState() != null && !ci.getState().isBlank()) defaultState = ci.getState();
      }
    } catch (Exception ex) {
      log.debug("CompanyInformation lookup failed for companyId {}: {}", companyId, ex.getMessage());
    }

    // Build header list (Apartment removed)
    List<String> headers = new ArrayList<>(Arrays.asList("Name","Email","Phone","Address","City","State","Country","ZipCode","Category","Status"));
    if (extraFieldNames != null) {
      for (CompanyCustomerExtraFieldName ef : extraFieldNames) headers.add(ef.getName());
    }

    StringBuilder sb = new StringBuilder();
    // Header row
    for (int i = 0; i < headers.size(); i++) {
      if (i > 0) sb.append(",");
      sb.append('"').append(headers.get(i).replace("\"", "\"\"")).append('"');
    }
    sb.append('\n');

    // Add sample/mock rows (3)
    int sampleRows = 3;
    for (int r = 1; r <= sampleRows; r++) {
      List<String> row = new ArrayList<>();
      for (String field : Arrays.asList("Name","Email","Phone","Address","City","State","Country","ZipCode","Category","Status")) {
        switch (field.toLowerCase()) {
          case "name":
            row.add("Sample Customer " + r);
            break;
          case "email":
            row.add("sample" + r + "@example.com");
            break;
          case "phone":
            row.add("+1-555-010" + (10 + r));
            break;
          case "address":
            row.add("123 Sample St");
            break;
          case "city":
            row.add("Sample City");
            break;
          case "state":
            row.add(defaultState);
            break;
          case "country":
            row.add(defaultCountry);
            break;
          case "zipcode":
            row.add("1000" + r);
            break;
          case "category":
            if (categories != null && !categories.isEmpty()) row.add(categories.get((r - 1) % categories.size()).getName());
            else row.add("DefaultCategory");
            break;
          case "status":
            row.add(r % 2 == 0 ? "inactive" : "active");
            break;
          default:
            row.add("");
        }
      }

      // extra fields
      if (extraFieldNames != null) {
        for (CompanyCustomerExtraFieldName ef : extraFieldNames) {
          String type = ef.getType() == null ? "string" : ef.getType().toLowerCase();
          if (type.contains("number") || type.equals("int") || type.equals("integer") || type.equals("double") || type.equals("float")) {
            row.add(String.valueOf(r * 10));
          } else if (type.contains("date")) {
            row.add(java.time.LocalDate.now().minusDays(r).toString());
          } else if (type.contains("email")) {
            row.add("sample" + r + "@example.com");
          } else if (type.contains("bool") || type.contains("checkbox")) {
            row.add((r % 2 == 0) ? "TRUE" : "FALSE");
          } else {
            row.add("SampleValue" + r);
          }
        }
      }

      // Append CSV-escaped row
      for (int i = 0; i < row.size(); i++) {
        if (i > 0) sb.append(',');
        sb.append('"').append(row.get(i).replace("\"", "\"\"")).append('"');
      }
      sb.append('\n');
    }

    return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

//    @Override
//    public Integer getCustomFieldCustomerCount(Long companyId, String id) {
//        companyCustomerCategoryRepository.countByCompanyIdAndcompanyCustomerCategoryId(companyId,id);
//        return 0;
//    }
 }
