package com.quantumai.customer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.IdGenerator.*;
import com.quantumai.customer.entity.IdGenerator.AssetIdTable;
import com.quantumai.customer.exception.AssetExtraFieldDeletionException;
import com.quantumai.customer.exception.AssetUniqueFieldViolationException;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.util.AdvanceFilterUtils;
import com.quantumai.customer.util.LocationHierarchyUtil;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;

import com.quantumai.customer.dto.AssetTemplateFieldsDTO;

@Service
@Slf4j
public class AssetsServiceImpl implements AssetsService {

  @Autowired private AssetFileRepository assetFileRepository;

  @Autowired private AssetCategoryRepository assetCategoryRepository;

  @Autowired private AssetsRepository assetsRepository;
  @Autowired private AssetExtraFieldsRepository extraFieldsRepository;

  @Autowired private AssetExtraFieldNameRepository extraFieldNameRepository;

  @Autowired private AssetCheckInOutRepository checkInOutRepository;

  @Autowired private AssetMandatoryFieldsRepository mandatoryFieldsRepository;
  @Autowired private AssetShowFieldsRepository showFieldsRepository;

  @Autowired private AssetIdTableRepository idTableRepository;

  @Autowired private AssetQRRepository qrRepository;

  @Autowired private AssetCategoryInspectionRepository assetCategoryInspectionRepository;

  @Autowired
  private AssetCategoryInspectionInstanceRepository assetCategoryInspectionInstanceRepository;

  @Autowired private LocationRepository locationRepository;
  @Autowired private BinRepository binRepository;

  @Autowired private AssetCategoryIdGeneratorRepository assetCategoryIdGeneratorRepository;

  @Autowired
  private AssetShowFieldsRepository assetShowFieldsRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private AssetRepositoryCustom assetRepositoryCustom;

  @Autowired InspectionInstanceIdGeneratorRepository inspectionInstanceIdGeneratorRepository;

  @Autowired private LocationHierarchyUtil locationHierarchyUtil;

  @Autowired
  private  InspectionTemplateIdGeneratorRepository inspectionTemplateIdGeneratorRepository;

  @Autowired
  private AssetCustomFieldIdGeneratorRepository assetCustomFieldIdGeneratorRepository;

  @Autowired
  private AssetCountByCategoriesRepository assetCountByCategoriesRepository;;

  @Autowired
  private AssetUniqueFieldConfigurationRepository assetUniqueFieldConfigurationRepository;

  private static final String SEQ_ID = "asset_category_sequence";
  LocalDateTime localDateTime;

  private ModelMapper modelMapper = new ModelMapper();

  @Override
  public List<AssetsDTO> getAssetsDetails(Long companyId) {
    // TODO Auto-generated method stub
    List<Assets> assetsList = assetsRepository.findByCompanyId(companyId);
    //    locationRepository.findByCompanyId(companyId).stream().;
    List<AssetsDTO> assetsDTOList = new ArrayList<AssetsDTO>();
    assetsList.stream()
        .forEach(
            x -> {
              AssetsDTO assetsDTO = modelMapper.map(x, AssetsDTO.class);
              assetsDTOList.add(assetsDTO);
            });
    return assetsDTOList;
  }

  @Override
  public AssetsDTO addAssets(AssetsDTO assetsDTO) throws Exception {
    // Step 1: Validate standard unique fields before adding asset
    AssetUniqueFieldValidationDTO validation = validateUniqueFields(assetsDTO, assetsDTO.getCompanyId());
    if (!validation.isValid()) {
      log.warn("Unique field validation failed: {}", validation.getMessage());
      throw new AssetUniqueFieldViolationException(
        "Unique field constraint violated: " + validation.getMessage(),
        validation
      );
    }
    log.info("AssetTO-->:{}",assetsDTO.toString());

    // Step 2: VALIDATE ALL EXTRA FIELDS BEFORE SAVING ASSET
    if (assetsDTO.getExtraFields() != null && !assetsDTO.getExtraFields().isEmpty()) {
      for (Map.Entry<String, String> entry : assetsDTO.getExtraFields().entrySet()) {
        String fieldName = entry.getKey();
        String fieldValue = entry.getValue();

        if (fieldValue == null || fieldValue.trim().isEmpty()) {
          log.debug("Extra field {} has empty value, skipping validation", fieldName);
          continue;
        }

        if (isFieldMarkedUnique(assetsDTO.getCompanyId(), fieldName, "EXTRA")) {
          List<AssetExtraFields> existingFields = checkExtraFieldUniqueness(
            assetsDTO.getCompanyId(),
            fieldName,
            fieldValue,
            null
          );

          if (!existingFields.isEmpty()) {
            log.warn("Unique extra field {} already has value: {}", fieldName, fieldValue);
            throw new AssetUniqueFieldViolationException(
              "Unique constraint violated for extra field '" + fieldName + "' with value '" + fieldValue + "'",
              fieldName,
              Map.of(fieldName,
                existingFields.stream().map(AssetExtraFields::getAssetId).collect(Collectors.toList()))
            );
          }
        }
      }
    }

    // Step 3: All validations passed - save the asset
    Assets assets = modelMapper.map(assetsDTO, Assets.class);
    if (assets.getAssetId() == null) {
      Optional<AssetIdTable> optionalIdTable =
          idTableRepository.findByCompanyId(assetsDTO.getCompanyId());
      if (optionalIdTable.isEmpty()) {
        assets.setAssetId(1);
        AssetIdTable myidTable = new AssetIdTable();
        myidTable.setTableId(2);
        myidTable.setCompanyId(assetsDTO.getCompanyId());
        idTableRepository.save(myidTable);
      } else {
        AssetIdTable idTable = optionalIdTable.get();
        assets.setAssetId(idTable.getTableId());
        idTable.updateId();
        idTableRepository.save(idTable);
      }
    }

    assets.setUpdatedAt(LocalDateTime.now().toString());
    Assets savedAsset = assetsRepository.save(assets);
    log.info("Asset Saved with ID: {}", savedAsset.getId());

    // Step 4: Save extra fields (create new or update existing — audit only actual changes)
    if (assetsDTO.getExtraFields() != null && !assetsDTO.getExtraFields().isEmpty()) {
      for (Map.Entry<String, String> entry : assetsDTO.getExtraFields().entrySet()) {
        upsertAssetExtraField(
                savedAsset.getId(),
                assetsDTO.getCompanyId(),
                assetsDTO.getEmail(),
                entry.getKey(),
                entry.getValue(),
                null);
      }
    }

    AssetsDTO myAssetsDTO = modelMapper.map(savedAsset, AssetsDTO.class);
    log.info("Asset with extra fields saved successfully: {}", myAssetsDTO.getId());
    return myAssetsDTO;
  }

  @Override
  public void importExcel(List<AssetsDTO> assetsDTOList, Map<String, String> columnMap) {
    // TODO Auto-generated method stub

    if (assetsDTOList.isEmpty()) {
      return;
    }
    Optional<AssetIdTable> optionalIdTable =
        idTableRepository.findByCompanyId(assetsDTOList.get(0).getCompanyId());
    assetsDTOList.stream()
        .forEach(
            x -> {
              Assets assets = modelMapper.map(x, Assets.class);
              if (optionalIdTable.isEmpty()) {
                assets.setAssetId(1);
                AssetIdTable myidTable = new AssetIdTable();
                myidTable.setTableId(2);
                myidTable.setCompanyId(assetsDTOList.get(0).getCompanyId());
                //
                //	//System.out.println("---------------------new---------->"+idTable.getTableId());
                idTableRepository.save(myidTable);
              } else {

                AssetIdTable myidTable = optionalIdTable.get();
                assets.setAssetId(myidTable.getTableId());
                myidTable.updateId();
                idTableRepository.save(myidTable);
              }
              assets.setUpdatedAt(LocalDateTime.now().toString());
              assetsRepository.save(assets);
            });
  }

  @Override
  public void addImage(AssetImageDTO assetImageDTO) throws Exception {
    // TODO Auto-generated method stub
    Optional<Assets> optionalAssets = assetsRepository.findById(assetImageDTO.getId());
    Assets asset = optionalAssets.orElseThrow(() -> new Exception("No Such Asset"));
    asset.setImage(assetImageDTO.getImage());

    assetsRepository.save(asset);
  }

  @Override
  public void removeImage(String id) throws Exception {
    // TODO Auto-generated method stub
    Optional<Assets> optionalAssets = assetsRepository.findById(id);
    Assets asset = optionalAssets.orElseThrow(() -> new Exception("No Such Asset"));
    asset.setImage("");
    assetsRepository.save(asset);
  }

  @Override
  public void removeAsset(String id) throws Exception {
    // TODO Auto-generated method stub

    assetsRepository.deleteById(id);
  }

  @Override
  public AssetsDTO getAsset(String assetId) throws Exception {
    // TODO Auto-generated method stub
    Optional<Assets> optionalasset = assetsRepository.findById(assetId);

    Assets asset = optionalasset.orElseThrow(() -> new Exception("No Such Asset"));
    AssetsDTO assetDTO = modelMapper.map(asset, AssetsDTO.class);
    if (asset.getLocation()!=null&&asset.getLocation().startsWith("bin")) {
      Optional<Bin> binOptional = binRepository.findById(asset.getLocation().substring(4));
      binOptional.ifPresent(bin -> assetDTO.setLocation(bin.getBinNumber()));

    } else if (asset.getLocation()!=null&&asset.getLocation().startsWith("location")) {
      Optional<Location> locationOptional =
          locationRepository.findById(asset.getLocation().substring(9));
      locationOptional.ifPresent(loc -> assetDTO.setLocation(loc.getName()));
    }

    return assetDTO;
  }

  @Override
  public AssetsDTO getAssetSpecific(String assetId) throws Exception {
    Optional<Assets> optionalasset = assetsRepository.findById(assetId);

    Assets asset = optionalasset.orElseThrow(() -> new Exception("No Such Asset"));
    AssetsDTO assetDTO = modelMapper.map(asset, AssetsDTO.class);
    if (asset.getLocation()!=null&&asset.getLocation().startsWith("bin")) {
      Optional<Bin> binOptional = binRepository.findById(asset.getLocation().substring(4));
      binOptional.ifPresent(bin -> {
        String fullPath = locationHierarchyUtil.resolveAssetLocationName(asset);
        assetDTO.setLocation(fullPath);
        assetDTO.setLocationName(fullPath);
        log.info("Bin found with full hierarchy path: {}", fullPath);
      });

    } else if (asset.getLocation()!=null&&asset.getLocation().startsWith("location")) {
      String hierarchyPath = locationHierarchyUtil.resolveAssetLocationName(asset);
      assetDTO.setLocation(hierarchyPath);
      assetDTO.setLocationName(hierarchyPath);
      log.info("Location found with full hierarchy path: {}", hierarchyPath);
    }

    return assetDTO;

  }

  private static final Object idGeneratorLock = new Object();

  @Override
  public void addExtraFields(AssetExtraFieldsDTO extraFieldsDTO) throws Exception {
    extraFieldsDTO.setName(extraFieldsDTO.getName());
    log.info("Saving extra field with name: {} for companyId: {}", extraFieldsDTO.getName(), extraFieldsDTO.getCompanyId());

    if (extraFieldsDTO.getId() != null && !extraFieldsDTO.getId().isBlank()) {
      Optional<AssetExtraFields> byId = extraFieldsRepository.findById(extraFieldsDTO.getId());
      if (byId.isPresent()) {
        updateAssetExtraFieldIfChanged(byId.get(), extraFieldsDTO.getValue(), extraFieldsDTO.getType());
        return;
      }
    }

    upsertAssetExtraField(
            extraFieldsDTO.getAssetId(),
            extraFieldsDTO.getCompanyId(),
            extraFieldsDTO.getEmail(),
            extraFieldsDTO.getName(),
            extraFieldsDTO.getValue(),
            extraFieldsDTO.getType());
  }

  private void upsertAssetExtraField(
          String assetId,
          Long companyId,
          String email,
          String fieldName,
          String fieldValue,
          String type) throws Exception {
    if (fieldValue == null || fieldValue.trim().isEmpty()) {
      return;
    }

    Optional<AssetExtraFields> existingOpt = extraFieldsRepository.findByNameAndAssetId(fieldName, assetId);
    if (existingOpt.isPresent()) {
      updateAssetExtraFieldIfChanged(existingOpt.get(), fieldValue, type);
      return;
    }

    long assetExtraFieldId = getAndIncrementSequence(companyId);
    AssetExtraFields extraFields = new AssetExtraFields();
    extraFields.setName(fieldName);
    extraFields.setValue(fieldValue);
    extraFields.setAssetId(assetId);
    extraFields.setCompanyId(companyId);
    extraFields.setEmail(email);
    extraFields.setType(type);
    extraFields.setAssetExtraFieldId(assetExtraFieldId);
    extraFieldsRepository.save(extraFields);
    log.info("Extra field {} created for asset {}", fieldName, assetId);
  }

  private void updateAssetExtraFieldIfChanged(AssetExtraFields existing, String newValue, String type) {
    if (Objects.equals(existing.getValue(), newValue)) {
      return;
    }
    existing.setValue(newValue);
    if (type != null) {
      existing.setType(type);
    }
    extraFieldsRepository.save(existing);
    log.info("Extra field {} updated for asset {}", existing.getName(), existing.getAssetId());
  }

  /**
   * Atomically gets current sequence and increments it for the company
   * Ensures no two calls get the same ID - entire operation is in synchronized block
   */
  private long getAndIncrementSequence(Long companyId) throws Exception {
    synchronized (idGeneratorLock) {
      // Initialize if needed
      if (!assetCustomFieldIdGeneratorRepository.existsByCompanyId(companyId)) {
        try {
          log.info("Creating new id generator for companyId: " + companyId);
          AssetCustomFieldIdGenerator idGenerator = new AssetCustomFieldIdGenerator();
          idGenerator.setCompanyId(companyId);
          idGenerator.setSeq(1L);
          assetCustomFieldIdGeneratorRepository.save(idGenerator);
          log.info("Successfully created id generator for companyId: " + companyId);
        } catch (Exception e) {
          log.debug("ID generator creation failed (likely concurrent creation), verifying existence: ", e);
          if (!assetCustomFieldIdGeneratorRepository.existsByCompanyId(companyId)) {
            log.error("Failed to create or find ID generator for companyId: " + companyId, e);
            throw new RuntimeException("Failed to initialize ID generator for companyId: " + companyId, e);
          }
          log.info("ID generator exists after concurrent attempt for companyId: " + companyId);
        }
      }

      // Fetch, get current seq, and increment - ALL INSIDE SYNCHRONIZED BLOCK
      AssetCustomFieldIdGenerator idGenerator = assetCustomFieldIdGeneratorRepository.findByCompanyId(companyId)
          .orElseThrow(() -> new Exception("ID Generator not found for companyId: " + companyId));

      long currentSeq = idGenerator.getSeq();
      long nextSeq = currentSeq + 1;
      idGenerator.setSeq(nextSeq);
      assetCustomFieldIdGeneratorRepository.save(idGenerator);

      log.debug("Sequence incremented from "+currentSeq+" to "+nextSeq+" for companyId: "+companyId);
      return currentSeq;
    }
  }

  /**
   * Thread-safe initialization of ID generator per company
   * Uses synchronized block to ensure only one thread creates the generator
   */
  private void initializeIdGeneratorIfNeeded(Long companyId) {
    synchronized (idGeneratorLock) {
      // Double-check pattern: check again inside synchronized block
      if (!assetCustomFieldIdGeneratorRepository.existsByCompanyId(companyId)) {
        try {
          log.info("Creating new id generator for companyId: " + companyId);
          AssetCustomFieldIdGenerator idGenerator = new AssetCustomFieldIdGenerator();
          idGenerator.setCompanyId(companyId);
          idGenerator.setSeq(1L);
          assetCustomFieldIdGeneratorRepository.save(idGenerator);
          log.info("Successfully created id generator for companyId: " + companyId);
        } catch (Exception e) {
          // Handle case where another thread created it while we were in the synchronized block
          log.debug("ID generator creation failed (likely concurrent creation), verifying existence: ", e);
          if (!assetCustomFieldIdGeneratorRepository.existsByCompanyId(companyId)) {
            log.error("Failed to create or find ID generator for companyId: " + companyId, e);
            throw new RuntimeException("Failed to initialize ID generator for companyId: " + companyId, e);
          }
          log.info("ID generator exists after concurrent attempt for companyId: " + companyId);
        }
      }
    }
  }

  @Override
  public List<AssetExtraFieldsDTO> getExtraFields(String id) {
    // TODO Auto-generated method stub
    List<AssetExtraFields> extraFieldsList = extraFieldsRepository.findByAssetId(id);
    if (extraFieldsList.isEmpty()) {
      return null;
    }
    List<AssetExtraFieldsDTO> extraFieldsDTOList = new ArrayList<>();
    extraFieldsList.stream()
        .forEach(
            (x) -> {
              AssetExtraFieldsDTO extraFieldsDTO = modelMapper.map(x, AssetExtraFieldsDTO.class);
              extraFieldsDTOList.add(extraFieldsDTO);
            });
    return extraFieldsDTOList;
  }

  @Override
  public void deleteExtraFields(String id) throws Exception {
    Optional<AssetExtraFields> extraFields = extraFieldsRepository.findById(id);
    if (extraFields.isEmpty()) {
      throw new Exception("No such extra Field");
    }
    extraFieldsRepository.delete(extraFields.get());
  }

  @Override
  public List<AssetExtraFieldNameDTO> getAssetExtraField(Long companyId) {
    // TODO Auto-generated method stub
    List<AssetExtraFieldName> extraFieldNameList =
        extraFieldNameRepository.findByCompanyId(companyId);
    List<AssetExtraFieldNameDTO> extraFieldNameListDTO = new ArrayList<>();
    extraFieldNameList.stream()
        .forEach(
            (x) -> {
              AssetExtraFieldNameDTO extraFieldNameDTO =
                  modelMapper.map(x, AssetExtraFieldNameDTO.class);
              extraFieldNameListDTO.add(extraFieldNameDTO);
            });
    return extraFieldNameListDTO;
  }

  @Override
  public void addAssetExtraField(AssetExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException {
    // TODO Auto-generated method stub
    AssetExtraFieldName extraFieldNameNew =
        extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(
            extraFieldNameDTO.getName(), extraFieldNameDTO.getCompanyId());
    if (extraFieldNameNew != null) {
      throw new ExtraFieldAlreadyPresentException("Extra Field Already Present");
    }
    extraFieldNameDTO.setName(extraFieldNameDTO.getName());

    AssetExtraFieldName extraFieldName =
        modelMapper.map(extraFieldNameDTO, AssetExtraFieldName.class);
    extraFieldNameRepository.save(extraFieldName);
  }

  @Override
  public void deleteAssetExtraField(String id) throws Exception {
    // TODO Auto-generated method stub

    Optional<AssetExtraFieldName> extraFieldNameOptional = extraFieldNameRepository.findById(id);
//      List<CompanyCustomerExtraFields> allDataFields=extraFieldsRepository.findByNameIgnoreCaseAndCompanyId(extraFieldNameOptional.get().getName(),extraFieldNameOptional.get().getCompanyId());
//      allDataFields=allDataFields.stream().filter((data)->!(data.getValue().isEmpty()||data.getValue().isBlank())).toList();
      log.info(extraFieldNameOptional.get().getName()+" "+extraFieldNameOptional.get().getCompanyId());
      Optional<AssetShowFields> assetShowFieldsOptional=assetShowFieldsRepository.findByNameAndCompanyId(extraFieldNameOptional.get().getName(),
              extraFieldNameOptional.get().getCompanyId());
      log.info("AssetShowFields Optional: "+assetShowFieldsOptional.isPresent());
      if(assetShowFieldsOptional.isPresent()){
          AssetShowFields showFields=assetShowFieldsOptional.get();
          long count=assetRepositoryCustom.countActiveAssetsWithExtraField(extraFieldNameOptional.get().getName(),extraFieldNameOptional.get().getCompanyId());
          log.info("Count of assets using the extra field: "+count);
          if(count==0||!showFields.isShow()){
              extraFieldNameRepository.deleteById(id);

              AssetExtraFieldName extraFieldName = extraFieldNameOptional.get();
              List<AssetExtraFields> extraFieldsList =
                      extraFieldsRepository.findByName(extraFieldName.getName());
              extraFieldsList.stream()
                      .forEach(
                              (x) -> {
                                  // System.out.println("-------------------------------------->"+x.getName());
                                  extraFieldsRepository.delete(x);
                              });
          }
          else{
              throw new AssetExtraFieldDeletionException("Cannot delete extra field as it is in use",count );
          }
      }
      else{
          throw new Exception("Cannot delete extra field");
      }


  }

  @Override
  public Map<String, Map<String, String>> getextraFieldList(Long companyId) {
    // TODO Auto-generated method stub
    List<AssetExtraFields> extraFieldNameList = extraFieldsRepository.findByCompanyId(companyId);
    List<Assets> assetList = assetsRepository.findByCompanyId(companyId);
    Map<String, Map<String, String>> fieldNameValueMap = new HashMap<>();

    assetList.stream()
        .forEach(
            (asset) -> {
              Map<String, String> m = new HashMap<>();
              extraFieldNameList.stream()
                  .forEach(
                      (field) -> {
                        if (field.getAssetId().endsWith(asset.getId())) {
                          m.put(field.getName(), field.getValue());
                        }
                      });
              fieldNameValueMap.put(asset.getId(), m);
            });
    return fieldNameValueMap;
  }

  @Override
  public void addCheckInOut(AssetCheckInDTO checkInDTO) {
    // TODO Auto-generated method stub

    //		checkInOutDetailsDTO.stream().forEach((x)->{
    //			//System.out.println("--------------------------------------------"+x.getDate());
    //		});
    //
    //	//System.out.println("--------------------------------------------"+checkInOutDTO.getDetailsList().size());
    Optional<AssetCheckInOut> checkInOutList =
        checkInOutRepository.findByAssetId(checkInDTO.getAssetId());

    if (checkInOutList.isEmpty()) {
      AssetCheckInOut checkInOut = new AssetCheckInOut();
      checkInOut.setAssetId(checkInDTO.getAssetId());
      checkInOut.setStatus(checkInDTO.getStatus());
      checkInOut.setCompanyId(checkInDTO.getCompanyId());
      AssetCheckInOutDetails checkInOutDetails = new AssetCheckInOutDetails();
      checkInOutDetails.setStatus(checkInDTO.getStatus());
      checkInOutDetails.setDate(checkInDTO.getDate());
      checkInOutDetails.setEmployee(checkInDTO.getEmployee());
      checkInOutDetails.setLocation(checkInDTO.getLocation());
      checkInOutDetails.setNotes(checkInDTO.getNotes());
      checkInOutDetails.setUpdateTime(LocalDateTime.now());
      checkInOutDetails.setUserLatitude(checkInDTO.getUserLatitude());
      checkInOutDetails.setUserLongitude(checkInDTO.getUserLongitude());
      checkInOutDetails.setIpAddress(checkInDTO.getIpAddress());
        checkInOutDetails.setUserLocation(checkInDTO.getUserLocation());
      List<AssetCheckInOutDetails> checkInOutDetailsList = new ArrayList<>();
      checkInOutDetailsList.add(checkInOutDetails);
      checkInOut.setDetailsList(checkInOutDetailsList);
      checkInOutRepository.save(checkInOut);

    } else {
      checkInOutList.stream()
          .forEach(
              (x) -> {
                x.setAssetId(checkInDTO.getAssetId());
                x.setStatus(checkInDTO.getStatus());
                x.setCompanyId(checkInDTO.getCompanyId());

                List<AssetCheckInOutDetails> checkInOutDetailsList = x.getDetailsList();
                AssetCheckInOutDetails checkInOutDetails = new AssetCheckInOutDetails();
                checkInOutDetails.setStatus(checkInDTO.getStatus());
                checkInOutDetails.setDate(checkInDTO.getDate());
                checkInOutDetails.setEmployee(checkInDTO.getEmployee());
                checkInOutDetails.setLocation(checkInDTO.getLocation());
                checkInOutDetails.setNotes(checkInDTO.getNotes());
                checkInOutDetails.setUserLatitude(checkInDTO.getUserLatitude());
                checkInOutDetails.setUserLongitude(checkInDTO.getUserLongitude());
                  checkInOutDetails.setIpAddress(checkInDTO.getIpAddress());
                  checkInOutDetails.setUserLocation(checkInDTO.getUserLocation());
                checkInOutDetails.setUpdateTime(LocalDateTime.now());
                checkInOutDetailsList.add(checkInOutDetails);
                x.setDetailsList(checkInOutDetailsList);
                checkInOutRepository.save(x);
              });
    }
  }

  @Override
  public List<AssetCheckInOutDTO> getCheckOutInList(String assetId) {
    // TODO Auto-generated method stub
    Optional<AssetCheckInOut> checkInOutList = checkInOutRepository.findByAssetId(assetId);
    List<AssetCheckInOutDTO> checkInOutDTOList = new ArrayList<>();
    if (checkInOutList.isPresent()) {

      checkInOutList.ifPresent(
          (x) -> {
            AssetCheckInOutDTO checkInOutDTO = modelMapper.map(x, AssetCheckInOutDTO.class);
            checkInOutDTOList.add(checkInOutDTO);
          });
    }
    if (checkInOutDTOList.isEmpty()) return List.of();
    List<AssetCheckInOutDetailsDTO> myList = checkInOutDTOList.get(0).getDetailsList();

    //		Collections.reverse(myList);
    myList.sort((d1, d2) -> d2.getUpdateTime().compareTo(d1.getUpdateTime()));

    checkInOutDTOList.get(0).setDetailsList(myList);
    //		Collections.reverse(checkInOutDTOList);
    return checkInOutDTOList;
  }

  @Override
  public AssetFile addAssetFile(MultipartFile file, String assetId,String username) throws IOException {
    // TODO Auto-generated method stub
    String fileName = StringUtils.cleanPath(file.getOriginalFilename());
    AssetFile assetFile = new AssetFile();
    assetFile.setAssetId(assetId);
    assetFile.setFile(file.getBytes());
    assetFile.setFileName(fileName);
    assetFile.setUploadedBy(username);
    assetFile.setUploadDateTime(LocalDateTime.now());

    return assetFileRepository.save(assetFile);
    //		AssetFile assetFile=modelMapper.map(assetFileDTO, AssetFile.class);
    //		assetFileRepository.save(assetFile);

  }

  @Override
  public List<AssetFileDTO> getAssetFile(String assetId) {
    // TODO Auto-generated method stub

    List<AssetFile> assetFileList = assetFileRepository.findByAssetId(assetId);
    if (assetFileList.size() == 0) {
      return null;
    } else {
      List<AssetFileDTO> assetFileDTOList = new ArrayList<>();
      assetFileList.stream()
          .forEach(
              (x) -> {
                AssetFileDTO assetFileDTO = modelMapper.map(x, AssetFileDTO.class);

                assetFileDTOList.add(assetFileDTO);
              });

      return assetFileDTOList;
    }
  }

  @Override
  public PaginatedResultDTO<AssetFileDTO> getAssetFilePaginated(String assetId, int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "uploadDateTime"));
    Page<AssetFile> page = assetFileRepository.findByAssetId(assetId, pageable);

    List<AssetFileDTO> assetFileDTOList = page.getContent().stream()
        .map(assetFile -> {
          AssetFileDTO assetFileDTO = modelMapper.map(assetFile, AssetFileDTO.class);
          assetFileDTO.setFile(null);
          return assetFileDTO;
        })
        .toList();

    return new PaginatedResultDTO<>(assetFileDTOList, page.getTotalElements());
  }

  @Override
  public AssetFileDTO downloadFile(String id) {
    // TODO Auto-generated method stub
    Optional<AssetFile> assetFile = assetFileRepository.findById(id);
    AssetFileDTO assetFileDTO = modelMapper.map(assetFile.get(), AssetFileDTO.class);
    return assetFileDTO;
  }

  @Override
  public void deleteFile(String id) {
    // TODO Auto-generated method stub
    assetFileRepository.deleteById(id);
  }

  @Override
  public void updateShowFields(AssetShowFields showFields) {
    // TODO Auto-generated method stub
    Optional<AssetShowFields> showFieldsOptional =
        showFieldsRepository.findByNameAndCompanyId(
            showFields.getName(), showFields.getCompanyId());
    AssetShowFields myShowFields = new AssetShowFields();
    if (showFieldsOptional.isPresent()) {
      myShowFields = showFieldsOptional.get();
      showFields.setId(myShowFields.getId());
    }
    showFieldsRepository.save(showFields);
  }

  @Override
  public void updateMandatoryFields(AssetMandatoryFields mandatoryFields) {
    // TODO Auto-generated method stub
    Optional<AssetMandatoryFields> mandatoryFieldsOptional =
        mandatoryFieldsRepository.findByNameAndCompanyId(
            mandatoryFields.getName(), mandatoryFields.getCompanyId());
    AssetMandatoryFields myMandatoryFields = new AssetMandatoryFields();
    if (mandatoryFieldsOptional.isPresent()) {
      myMandatoryFields = mandatoryFieldsOptional.get();
      mandatoryFields.setId(myMandatoryFields.getId());
    }
    mandatoryFieldsRepository.save(mandatoryFields);
  }

  @Override
  public AssetShowFields getShowFields(String name, Long companyId) {
    // TODO Auto-generated method stub
    Optional<AssetShowFields> showFieldsOptional =
        showFieldsRepository.findByNameAndCompanyId(name, companyId);
    if (showFieldsOptional.isPresent()) {
      return showFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public AssetMandatoryFields getMandatoryFields(String name, Long companyId) {
    // TODO Auto-generated method stub
    Optional<AssetMandatoryFields> mandatoryFieldsOptional =
        mandatoryFieldsRepository.findByNameAndCompanyId(name, companyId);
    if (mandatoryFieldsOptional.isPresent()) {
      return mandatoryFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public List<AssetShowFields> getAllShowFields(Long companyId) {
    // TODO Auto-generated method stub
    List<AssetShowFields> showFieldsList = showFieldsRepository.findByCompanyId(companyId);
    return showFieldsList;
  }

  @Override
  public List<AssetMandatoryFields> getAllMandatoryFields(Long companyId) {
    // TODO Auto-generated method stub
    List<AssetMandatoryFields> mandatoryFieldsList =
        mandatoryFieldsRepository.findByCompanyId(companyId);
    return mandatoryFieldsList;
  }

  @Override
  public void deleteShowAndMandatoryFields(Long companyId, String name) throws Exception {
    // TODO Auto-generated method stub
    Optional<AssetShowFields> showFieldsOptional =
        showFieldsRepository.findByNameAndCompanyId(name, companyId);

      List<CompanyCustomerExtraFields> allDataFields=extraFieldsRepository.findByNameIgnoreCaseAndCompanyId(name,companyId);
      allDataFields=allDataFields.stream().filter((data)->!(data.getValue().isEmpty()||data.getValue().isBlank())).toList();
      Long count=assetRepositoryCustom.countActiveAssetsWithExtraField(name,companyId);
      if(count==0){
          showFieldsRepository.delete(showFieldsOptional.get());
          Optional<AssetMandatoryFields> mandatoryFieldsOptional =
                  mandatoryFieldsRepository.findByNameAndCompanyId(name, companyId);
          if (mandatoryFieldsOptional.isPresent()) {
              mandatoryFieldsRepository.delete(mandatoryFieldsOptional.get());
          }
      }
      else{
          throw new Exception("Cannot delete extra field as it is in use" );
      }

  }

  @Override
  public void updateAssetWithFile(List<AssetsDTO> assetsDTOList, Long companyId) {
    // TODO Auto-generated method stub

    assetsDTOList.stream()
        .forEach(
            (x) -> {
              // System.out.println("------------------------->>>"+x.getAssetId());
              Optional<Assets> assets = assetsRepository.findByAssetIdAndCompanyId(x.getAssetId(), companyId);
              if(assets.isPresent()) {
                  x.setId(assets.get().getId());
                  x.setImage(assets.get().getImage());

                  Assets myasset = modelMapper.map(x, Assets.class);
                  assetsRepository.save(myasset);
              }
            });
  }

  @Override
  public void qrDataUpdation(AssetQR qr) {
    // TODO Auto-generated method stub
    Optional<AssetQR> optionalQr = qrRepository.findByCompanyId(qr.getCompanyId());
    if (optionalQr.isEmpty()) {
      qrRepository.save(qr);
    } else {
      qr.setId(optionalQr.get().getId());
      qrRepository.save(qr);
    }
  }

  @Override
  public AssetQR getQRData(Long companyId) {
    // TODO Auto-generated method stub
    Optional<AssetQR> optionalQr = qrRepository.findByCompanyId(companyId);
    if (optionalQr.isPresent()) {
      return optionalQr.get();
    }
    return null;
  }

  @Override
  public PaginatedResultDTO<String> getAllAssetDetails(Long companyId) {
    // TODO Auto-generated method stub
    List<AssetExtraFieldName> extraFieldNameList =
        extraFieldNameRepository.findByCompanyId(companyId);

    List<Assets> assetList = assetsRepository.findByCompanyId(companyId);
    Map<String, String> binIdToNameMap =
        binRepository.findByCompanyId(companyId).stream()
            .collect(Collectors.toMap(Bin::getId, Bin::getBinNumber));
    Map<String, String> locationIdToNameMap =
        locationRepository.findByCompanyId(companyId).stream()
            .collect(Collectors.toMap(Location::getId, Location::getName));

    List<String> mapList = new ArrayList<>();
    assetList.stream()
        .forEach(
            (order) -> {
              if (order.getLocation()!=null&&order.getLocation().startsWith("bin")) {
                order.setLocation(binIdToNameMap.get(order.getLocation().substring(4)));
              } else if (order.getLocation()!=null&&order.getLocation().startsWith("location")) {
                order.setLocation(locationIdToNameMap.get(order.getLocation().substring(9)));
              }
              List<AssetExtraFields> extraFieldsList =
                  extraFieldsRepository.findByAssetId(order.getId());
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
              m.put("image", order.getImage());
              m.put("email", order.getEmail());
              m.put("name", order.getName());
              m.put("assetId", order.getAssetId().toString());
              m.put("companyId", order.getCompanyId().toString());
              m.put("serialNumber", order.getSerialNumber());
              m.put("category", order.getCategory());
              m.put("customer", order.getCustomer());
              m.put("customerId", order.getCustomerId());
              m.put("location", order.getLocation());
              m.put("status", order.getStatus());
              m.put("updatedAt", order.getUpdatedAt());

              //			if(order.getDueDate()!=null) m.put("dueDate",order.getDueDate().toString());
              //			if(order.getLastUpdate()!=null)
              // m.put("lastUpdate",order.getLastUpdate().toString());
              //			if(order.getPriority()!=null) m.put("priority",order.getPriority().toString());
              //			if(order.getStatus()!=null) m.put("status",order.getStatus().toString());
              ObjectMapper objectMapper = new ObjectMapper();
              try {
                // Convert POJO to JSON string
                String json = objectMapper.writeValueAsString(m);

                mapList.add(json);
                //	            System.out.print(json);
                //	            System.out.print(m);

              } catch (Exception e) {
                e.printStackTrace();
              }
            });

    return new PaginatedResultDTO<>(mapList, mapList.size());
  }

  @Override
  public PaginatedResultDTO<String> getAllAssetDetailsWithLocationDetails(Long companyId,Object filter) {
    Map<?, ?> filterMap = (Map<?, ?>) filter;

      // Get all keys
      Set<?> keys = filterMap.keySet();
      Map<String, String> mapping = new HashMap<String, String>();

      // Print keys or do something with them

      for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue();
        //	                //System.out.println("Key: " + key + ", Value: " + value);
        if (value != null) {
          mapping.put(key.toString(), value.toString());
        }
      }
      String location=mapping.get("location");
    List<AssetExtraFieldName> extraFieldNameList =
        extraFieldNameRepository.findByCompanyId(companyId);



    List<Assets> assetList = assetsRepository.findByCompanyId(companyId);
    Map<String, String> binIdToNameMap =
        binRepository.findByCompanyId(companyId).stream()
            .collect(Collectors.toMap(Bin::getId, Bin::getBinNumber));
    Map<String, String> locationIdToNameMap =
        locationRepository.findByCompanyId(companyId).stream()
            .collect(Collectors.toMap(Location::getId, Location::getName));


      List<AssetExtraFields> allExtraFieldsList =
              extraFieldsRepository.findByCompanyId(companyId);

    List<String> mapList = new ArrayList<>();
    // System.out.println("++++++++++++++++++++++++++>"+location);
    if(location!=null&&!location.isEmpty()){
      assetList=assetList.stream().filter((data)->{
        return data.getLocation().equals(location);
      }).collect(Collectors.toList());
    }
    assetList.stream()
        .forEach(
            (order) -> {
              if (order.getLocation()!=null&&order.getLocation().startsWith("bin")) {
                order.setLocation(binIdToNameMap.get(order.getLocation().substring(4)));
              } else if (order.getLocation()!=null&&order.getLocation().startsWith("location")) {
                order.setLocation(locationIdToNameMap.get(order.getLocation().substring(9)));
              }
              List<AssetExtraFields> extraFieldsList =
                  extraFieldsRepository.findByAssetId(order.getId());
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
              m.put("image", order.getImage());
              m.put("email", order.getEmail());
              m.put("name", order.getName());
              m.put("assetId", order.getAssetId().toString());
              m.put("companyId", order.getCompanyId().toString());
              m.put("serialNumber", order.getSerialNumber());
              m.put("category", order.getCategory());
              m.put("customer", order.getCustomer());
              m.put("customerId", order.getCustomerId());
              m.put("location", order.getLocation());
              m.put("status", order.getStatus());
              m.put("updatedAt", order.getUpdatedAt());

              //			if(order.getDueDate()!=null) m.put("dueDate",order.getDueDate().toString());
              //			if(order.getLastUpdate()!=null)
              // m.put("lastUpdate",order.getLastUpdate().toString());
              //			if(order.getPriority()!=null) m.put("priority",order.getPriority().toString());
              //			if(order.getStatus()!=null) m.put("status",order.getStatus().toString());
              ObjectMapper objectMapper = new ObjectMapper();
              try {
                // Convert POJO to JSON string
                String json = objectMapper.writeValueAsString(m);

                mapList.add(json);
                //	            System.out.print(json);
                //	            System.out.print(m);

              } catch (Exception e) {
                e.printStackTrace();
              }
            });

    return new PaginatedResultDTO<>(mapList, mapList.size());
  }
  @Override
  public PaginatedResultDTO<String> sortAssets(
      Long companyId, String field, Integer pageNumber, Integer pageSize) {
    System.out.println("--->" + field);
    AssetExtraFieldName extraFieldName =
        extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(field, companyId);
    //		//System.out.println(extraFieldName);

    List<Map<String, String>> mapList = new ArrayList<>();
    PaginatedResultDTO<String> myList = getAllAssetDetails(companyId);
    ObjectMapper objectMapper = new ObjectMapper();
    myList.getData().stream()
        .forEach(
            (asset) -> {
              Map<String, String> m = new HashMap<>();
              try {

                m = objectMapper.readValue(asset, new TypeReference<Map<String, String>>() {});
                mapList.add(m);

              } catch (Exception e) {

                e.printStackTrace();
              }
            });

    Comparator<Map<String, String>> customComparator = null;

    customComparator = Comparator.comparing(m -> m.get(field));

    List<Map<String, String>> res =
        mapList.stream().sorted(customComparator).collect(Collectors.toList());
    //		 //System.out.println(res);
    //		 //System.out.println("------------------------->"+res.size());
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
    //		 res.stream().forEach((mydata)->{
    //			 try {
    ////	            // Convert POJO to JSON string
    //	          String json = objectMapper.writeValueAsString(mydata);
    //
    //	          resList.add(json);
    //	           System.out.print(json);
    //
    //
    //
    //	       } catch (Exception e) {
    //	            e.printStackTrace();
    //
    //
    //	        }
    //		 });
    //		 String jsonString;
    //		try {
    //			jsonString = objectMapper.writeValueAsString(res);
    //			resList = objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
    //		} catch (JsonProcessingException e) {
    //			// TODO Auto-generated catch block
    //			e.printStackTrace();
    //		}

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
  public List<String> searchedAssets(Long companyId, String data, String field) {
    // TODO Auto-generated method stub
    List<Map<String, String>> mapList = new ArrayList<>();
    PaginatedResultDTO<String> myList = getAllAssetDetails(companyId);
    ObjectMapper objectMapper = new ObjectMapper();
    myList.getData().stream()
        .forEach(
            (asset) -> {
              Map<String, String> m = new HashMap<>();
              try {
                // Convert JSON string to Map<String, String>
                m = objectMapper.readValue(asset, new TypeReference<Map<String, String>>() {});
                mapList.add(m);

              } catch (Exception e) {
                // Handle exception
                e.printStackTrace();
              }
            });
    //		//System.out.println(field);
    List<Map<String, String>> res =
        mapList.stream()
            .filter(
                m -> {
                  // System.out.println("---->"+m.get(field));
                  String s = m.get(field);
                  //			return s.contains(data);
                  return s.toLowerCase().contains(data.toLowerCase());
                })
            .collect(Collectors.toList());
    // System.out.println(res);
    // System.out.println(res.size());
    List<String> resList = new ArrayList<>();
    res.stream()
        .forEach(
            (mydata) -> {
              try {
                //	            // Convert POJO to JSON string
                String json = objectMapper.writeValueAsString(mydata);

                resList.add(json);
                System.out.print(json);

              } catch (Exception e) {
                e.printStackTrace();
              }
            });
    return resList;
  }

  @Override
  public PaginatedResultDTO<String> getAssetsDetailsByCustomerId(
      String customerId, Integer pageNumber) {
    // TODO Auto-generated method stub

    List<Assets> assetsList = assetsRepository.findByCustomerId(customerId);
    List<String> assetsDTOList = new ArrayList<String>();
    //		assetsList.stream().forEach(x->{
    //			AssetsDTO assetsDTO=modelMapper.map(x, AssetsDTO.class);
    //			assetsDTOList.add(assetsDTO.toString());
    //
    //		});
    Map<String, String> m = new HashMap<>();
    for (int i = 0; i < assetsList.size(); i++) {
      AssetsDTO assetsDTO = modelMapper.map(assetsList.get(i), AssetsDTO.class);
      m.put("id", assetsDTO.getId());
      m.put("image", assetsDTO.getImage());
      m.put("email", assetsDTO.getEmail());
      m.put("name", assetsDTO.getName());
      m.put("assetId", assetsDTO.getAssetId().toString());
      m.put("companyId", assetsDTO.getCompanyId().toString());
      m.put("serialNumber", assetsDTO.getSerialNumber());
      m.put("category", assetsDTO.getCategory());
      m.put("customer", assetsDTO.getCustomer());
      m.put("customerId", assetsDTO.getCustomerId());
      m.put("location", locationHierarchyUtil.resolveAssetLocationName(assetsList.get(i)));
      m.put("status", assetsDTO.getStatus());
      m.put("updatedAt", assetsDTO.getUpdatedAt());
      ObjectMapper objectMapper = new ObjectMapper();
      try {
        String json = objectMapper.writeValueAsString(m);
        assetsDTOList.add(json);
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    int startItem = pageNumber * 5;
    int endItem = Math.min(startItem + 5, assetsDTOList.size());
    if (startItem > assetsDTOList.size()) {

      return new PaginatedResultDTO<>(Collections.emptyList(), 0);
    }
    int totalPage = assetsDTOList.size();
    assetsDTOList = assetsDTOList.subList(startItem, endItem);
    // System.out.println("Asset by cutomer"+assetsDTOList.size());
    return new PaginatedResultDTO<>(assetsDTOList, totalPage);
  }

  @Override
  public void updateAssetsWithInActive(String customerId) {
    // TODO Auto-generated method stub
    List<Assets> assetsList = assetsRepository.findByCustomerId(customerId);

    assetsList.stream()
        .forEach(
            x -> {
              x.setStatus("inactive");
              assetsRepository.save(x);
            });
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
    List<String> filteredAssetsWithAllFields = new ArrayList<>();
    long totalPage = 0;
    log.info("Search Data: {}",searchData);
      log.info("Filter Data: {}",filter.toString());


    if (filter instanceof Map) {
      // Cast the filter to a Map
      Map<?, ?> filterMap = (Map<?, ?>) filter;
      String effectiveSearch = AdvanceFilterUtils.resolveSearchTerm(searchData, filterMap);

      // Get all keys
      Set<?> keys = filterMap.keySet();
      Map<String, String> mapping = new HashMap<String, String>();

      // Print keys or do something with them

      for (Map.Entry<?, ?> entry : filterMap.entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue();
        //	                //System.out.println("Key: " + key + ", Value: " + value);
        if (value != null) {
          mapping.put(key.toString(), value.toString());
        }
      }
      PaginatedResultDTO<String> assetsWithAllFields =
      getAllAssetDetailsWithLocationDetails(Long.parseLong(mapping.get("companyId")),filter);
        // System.out.println("=====================//////////////=====================>"+filterMap);
        log.info("FilterMap : {}",filterMap);
        // System.out.println("=====================//////////////=====================>"+assetsWithAllFields.getData());
        log.info("Assets With All Fields : {}",assetsWithAllFields.getData());
      filteredAssetsWithAllFields =
          assetsWithAllFields.getData().stream()
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
                          if (!AdvanceFilterUtils.isFilterMetadataKey(keyString)
                          && !keyString.equals("location")
                              && myValue != null
                              && value != null
                              && !value.toString().isEmpty()) {

                              myValue = myValue.toLowerCase();
                              expectedValue = expectedValue.toLowerCase();

                              if (keyString.equals("status")) {
                                  if (!myValue.equals(expectedValue)) {
                                      flag = 0;
                                  }
                              } else {
                                  if (!myValue.contains(expectedValue)
                                          && !myValue.equals(expectedValue)) {
                                      flag = 0;
                                  }
                              }

                          }
//                          if(keyString.equals("location")){
//                              myValue = myValue.toLowerCase();
//                              if (!myValue.contains(expectedValue.toLowerCase())
//                                      && !myValue.equals(expectedValue.toLowerCase())) {
//                                  flag = 0;
//                              }
//                          }
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

      //	            Sorting if it is enabled
             
  //     Map<String, String> binIdToNameMap =
  //     binRepository.findByCompanyId(Long.parseLong(mapping.get("companyId"))).stream()
  //         .collect(Collectors.toMap(Bin::getId, Bin::getBinNumber));
  // Map<String, String> locationIdToNameMap =
  //     locationRepository.findByCompanyId(Long.parseLong(mapping.get("companyId"))).stream()
  //         .collect(Collectors.toMap(Location::getId, Location::getName));

          // filteredAssetsWithAllFields.stream()
          //   .forEach(
          //     (order) -> {
          //       if (order.getLocation()!=null&&order.getLocation().startsWith("bin")) {
          //         order.setLocation(binIdToNameMap.get(order.getLocation().substring(4)));
          //       } else if (order.getLocation()!=null&&order.getLocation().startsWith("location")) {
          //         order.setLocation(locationIdToNameMap.get(order.getLocation().substring(9)));
          //       }
          // });

      System.out.println("Sort-" + sortField);
      System.out.println("Search-" + searchData);

      // Use updatedAt as default sort field if sortField is null or empty
      String effectiveSortField = (sortField == null || sortField.isEmpty()) ? "updatedAt" : sortField;
      System.out.println("Effective Sort Field: " + effectiveSortField);

      if (effectiveSortField != null && (effectiveSortField.equals("") == false)) {
        System.out.println("going inside-" + effectiveSortField);
        Comparator<String> customComparator = null;
        ObjectMapper objectMapper = new ObjectMapper();
        customComparator =
            Comparator.comparing(
                data -> {
                  String jsonString = (String) data;

                  Map<String, String> myMap = new HashMap<>();
                  try {
                    myMap =
                        objectMapper.readValue(
                            jsonString, new TypeReference<Map<String, String>>() {});
                  } catch (JsonMappingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  } catch (JsonProcessingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                  }
                  return myMap.get(effectiveSortField);
                },
                String.CASE_INSENSITIVE_ORDER);
        if (asc == true) {
          filteredAssetsWithAllFields =
              filteredAssetsWithAllFields.stream()
                  .sorted(customComparator)
                  .collect(Collectors.toList());
        } else {
          filteredAssetsWithAllFields =
              filteredAssetsWithAllFields.stream()
                  .sorted(customComparator.reversed())
                  .collect(Collectors.toList());
        }
      }

      System.out.println("TOTAL__LENGTH___" + filteredAssetsWithAllFields.size());
      log.info("Effective search term: {}", effectiveSearch);
      if (!effectiveSearch.isEmpty()) {
        filteredAssetsWithAllFields =
            filteredAssetsWithAllFields.stream()
                .filter(data -> AdvanceFilterUtils.matchesSearch(data, effectiveSearch))
                .collect(Collectors.toList());
      }
//        ObjectMapper objectMapperNew = new ObjectMapper();

//        if (mySearchData != null && !mySearchData.trim().isEmpty()) {
//            filteredAssetsWithAllFields = filteredAssetsWithAllFields.stream()
//                    .map(json -> {
//                        try {
//                            return objectMapperNew.readValue(json, new TypeReference<Map<String, Object>>() {});
//                        } catch (Exception e) {
//                            return null;
//                        }
//                    })
//                    .filter(Objects::nonNull)
//                    .filter(map -> {
//                        Object status = map.get("status");
//                        return status != null && mySearchData.equalsIgnoreCase(status.toString());
//                    })
//                    .map(map -> {
//                        try {
//                            return objectMapperNew.writeValueAsString(map);
//                        } catch (JsonProcessingException e) {
//                            return null;
//                        }
//                    })
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//        }

        System.out.println("==========/////////////////////////================>"+filteredAssetsWithAllFields);

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

    //		//System.out.println(filteredAssetsWithAllFields.size());
    // System.out.println("----------"+filteredAssetsWithAllFields.size());
    return new PaginatedResultDTO<>(filteredAssetsWithAllFields, totalPage);
  }

  @Override
  public CheckInCheckOutCountDTO checkInCheckOut(Long companyId) {
    List<Assets> assetsList = assetsRepository.findByCompanyId(companyId);
    AtomicReference<Integer> checkIn = new AtomicReference<>(0);
    AtomicReference<Integer> checkOut = new AtomicReference<>(0);
    List<AssetCheckInOutDTO> assetCheckInOutDTOList = new ArrayList<>();
    assetsList.stream()
        .forEach(
            (data) -> {
              Optional<AssetCheckInOut> assetCheckInOutDTO =
                  checkInOutRepository.findByAssetId(data.getId());
              if (assetCheckInOutDTO.isPresent()
                  && assetCheckInOutDTO.get().getStatus().equals("Checked Out")) {
                checkOut.getAndSet(checkOut.get() + 1);
              } else {
                checkIn.getAndSet(checkIn.get() + 1);
              }
              //			assetCheckInOutDTOmyList.forEach((checkList)->{
              //
              //				checkList.getDetailsList().forEach((ele)->{
              //					if(ele.getStatus().equals("Checked Out")){
              //						checkIn.getAndSet(checkIn.get() + 1);
              //					}
              //					else{
              //						checkOut.getAndSet(checkOut.get() + 1);
              //					}
              //
              //				});
              //			});

            });
    System.out.println("===>" + checkIn.get() + " " + checkOut.get());
    CheckInCheckOutCountDTO checkInCheckOutCountDTO = new CheckInCheckOutCountDTO();
    checkInCheckOutCountDTO.setCheckIn(checkIn.get());
    checkInCheckOutCountDTO.setCheckOut(checkOut.get());
    return checkInCheckOutCountDTO;
  }

  @Override
  public List<AssetsDTO> assetListFromSerialNumber(Long companyId, String serialNumber) {
    List<AssetsDTO> assetList =
        assetsRepository.findByCompanyIdAndSerialNumber(companyId, serialNumber);
    return assetList;
  }

  @Override
  public List<AssetCheckInOut> filterByCheckedInOut(Long companyId, Boolean checkedIn) {

    List<AssetCheckInOut> assetCheckInOutDTOmyList =
        checkInOutRepository.findByCompanyId(companyId);
    if (checkedIn == true) {

      assetCheckInOutDTOmyList =
          assetCheckInOutDTOmyList.stream()
              .filter(data -> data.getStatus().toLowerCase().equals("checked in"))
              .toList();
    } else {

      assetCheckInOutDTOmyList =
          assetCheckInOutDTOmyList.stream()
              .filter(data -> data.getStatus().toLowerCase().equals("checked out"))
              .toList();
    }

    return assetCheckInOutDTOmyList;
  }



  @Override
  public void addCategory(CategoryDTO categoryDTO) throws Exception {
    AssetCategory category = modelMapper.map(categoryDTO, AssetCategory.class);


      Optional<AssetCategoryIdGenerator> AssetCategoryIdGeneratorOptional =
              assetCategoryIdGeneratorRepository.findByCompanyId(categoryDTO.getCompanyId());
      if (AssetCategoryIdGeneratorOptional.isEmpty()) {
          throw new Exception("Sequence Database Not Found");
      }
      AssetCategoryIdGenerator assetCategoryIdGenerator = AssetCategoryIdGeneratorOptional.get();
      Long id = assetCategoryIdGenerator.getSeq();
      category.setAssetCategoryId(id);
      assetCategoryIdGenerator.setSeq(id + 1);
      assetCategoryIdGeneratorRepository.save(assetCategoryIdGenerator);



    Optional<AssetCategory> optionalAssetCategory =
        assetCategoryRepository.findByNameIgnoreCaseAndCompanyId(categoryDTO.getName(),categoryDTO.getCompanyId());

    if (optionalAssetCategory.isEmpty()) {
      assetCategoryRepository.save(category);
    } else {
      throw new CategoryException("Category Already Present");
    }
  }

  @Override
  public void updateCategory(CategoryDTO categoryDTO) {
    AssetCategory category = modelMapper.map(categoryDTO, AssetCategory.class);

    assetCategoryRepository.save(category);
  }

  @Override
  public List<AssetCategory> getCategoryList(Long companyId) {

    List<AssetCategory> categoryList = assetCategoryRepository.findByCompanyId(companyId);

    return categoryList;
  }

  @Override
  public void deleteCategory(String id) {
    Optional<AssetCategory> category = assetCategoryRepository.findById(id);
    if (category.isPresent()) {
      assetCategoryRepository.delete(category.get());
    }
  }

  @Override
  public AssetCategory getCategoryListById(Long companyId, String id) {
    Optional<AssetCategory> categoryOptional = assetCategoryRepository.findById(id);
    return categoryOptional.orElse(null);
  }

  @Override
  public int countAssetByCategory(String category) {
    return assetsRepository.countByCategoryIgnoreCase(category);
  }

  @Override
  public List<AssetCountByCategoryDTO> countAssetByCategories(Long companyId) {
    return assetCountByCategoriesRepository.getAssetCountByCategories(companyId,"ASC");
  }

  public List<AssetCategory> getActiveCategoryList(Long companyId) {
    List<AssetCategory> categoryList =
        assetCategoryRepository.findByCompanyIdAndStatus(companyId, "active");
    return categoryList;
  }

  @Override
  public List<AssetsDTO> getActiveAssets(Long companyId) {
    List<Assets> assetList = assetsRepository.findByCompanyId(companyId);
    List<AssetsDTO> filteredList = new ArrayList<>();
    assetList.stream()
        .forEach(
            (ele) -> {
              if (ele.getStatus()!=null&&ele.getStatus().equals("active")) {
                AssetsDTO assetsDTO = modelMapper.map(ele, AssetsDTO.class);

                filteredList.add(assetsDTO);
              }
            });
    return filteredList;
  }

  @Override
  public Map<String, List<AssetsDTO>> getAssetByCategory(Long companyId) {
    List<Assets> assetList = assetsRepository.findByCompanyId(companyId);
    List<AssetCategory> assetCategoryList = assetCategoryRepository.findByCompanyId(companyId);
    Map<String, List<AssetsDTO>> categoryAssetMap = new HashMap<>();
    List<AssetsDTO> filteredList = new ArrayList<>();
    assetCategoryList.forEach(
        (category) -> {
          assetList.forEach(
              (asset) -> {
                if (asset.getCategory().equals(category.getName())) {
                  if (categoryAssetMap.containsKey(category.getName())) {
                    List<AssetsDTO> assetsDTOList = categoryAssetMap.get(category.getName());
                    AssetsDTO assetsDTO = modelMapper.map(asset, AssetsDTO.class);
                    assetsDTOList.add(assetsDTO);
                    categoryAssetMap.put(category.getName(), assetsDTOList);
                  } else {
                    AssetsDTO assetsDTO = modelMapper.map(asset, AssetsDTO.class);
                    List<AssetsDTO> assetsDTOList = new ArrayList<>();
                    assetsDTOList.add(assetsDTO);
                    categoryAssetMap.put(category.getName(), assetsDTOList);
                  }
                }
              });
        });
    return categoryAssetMap;
  }

  @Override
  public void addAssetInspection(AssetCategoryInspection assetCategoryInspection) {

      inspectionTemplateIdGeneratorRepository.findByCompanyId(assetCategoryInspection.getCompanyId()).ifPresentOrElse((data)->{
        Long seqId=data.getSeq();
        data.setSeq(seqId+1);
          assetCategoryInspection.setAssetCategoryInspectionId(seqId);
          inspectionTemplateIdGeneratorRepository.save(data);
      },()->{
          assetCategoryInspection.setAssetCategoryInspectionId(1L);
          InspectionTemplateIdGenerator inspectionTemplateIdGenerator=new InspectionTemplateIdGenerator();
          inspectionTemplateIdGenerator.setSeq(2L);
          inspectionTemplateIdGenerator.setCompanyId(assetCategoryInspection.getCompanyId());
          inspectionTemplateIdGeneratorRepository.save(inspectionTemplateIdGenerator);
      });
    assetCategoryInspectionRepository.save(assetCategoryInspection);
  }

  @Override
  public void deleteAssetInspection(String id) {
    assetCategoryInspectionRepository.deleteById(id);
  }

  @Override
  public AssetCategoryInspection getAssetInspection(String assetInspectionId) throws Exception {
    return assetCategoryInspectionRepository
        .findById(assetInspectionId)
        .orElseThrow(() -> new Exception("No such Inspection Step"));
  }

  @Override
  public List<AssetCategoryInspection> getAllAssetInspectionByCategory(Long companyId, String category) {
      System.out.println("===================>>>" + category);
      if (category.trim().isEmpty()) {
          return new ArrayList<>();
      }

      return assetCategoryInspectionRepository.findByCompanyId(companyId)
          .stream()
          .filter(data -> {
              System.out.println("Category Name : " + data.getCategoryName());
              if (data.getCategoryName() == null || data.getCategoryName().isEmpty()) {
                  return false;
              }
              // Check if any category in the list matches the provided category (case-insensitive)
              return data.getCategoryName().stream()
                  .filter(Objects::nonNull)
                  .anyMatch(categoryObj -> {
                      try {
                          // Handle the case where categoryObj is a Map (like {id: 1, categoryName: 'abc'})
                          if (categoryObj instanceof Map) {
                              Map<?, ?> categoryMap = (Map<?, ?>) categoryObj;
                              Object categoryNameValue = categoryMap.get("categoryName");
                              return categoryNameValue != null &&
                                     categoryNameValue.toString().equalsIgnoreCase(category);
                          }
                          // Fallback to toString() for other object types
                          return categoryObj.toString().equalsIgnoreCase(category);
                      } catch (Exception e) {
                          System.out.println("Error processing category object: " + e.getMessage());
                          return false;
                      }
                  });
          })
          .collect(Collectors.toList());
  }

  @Override
  public List<AssetCategoryInspection> getAllActiveAssetInspectionByCategory(Long companyId, String category)  {
    System.out.println("===================>>>" + category+" "+companyId);
//    if (category.trim().isEmpty()) {
////      return new ArrayList<>();
//
//      return assetCategoryInspectionRepository.findByCompanyId(companyId)
//              .stream()
//              .filter(data -> {
//
//                if (data.getStatus() == null || !data.getStatus().equals(StatusEnum.active)) {
//                  return false;
//                }
//
//                // Include inspections having no category
//                if (data.getCategoryName() == null|| data.getCategoryName().isEmpty()|| data.getCategoryName().toString().trim().isEmpty()) {
//                  return true;
//                }
//
//                return data.getCategoryName().stream()
//                        .anyMatch(categoryObj -> {
//                          try {
//                            if (categoryObj instanceof Map) {
//                              Map<?, ?> categoryMap = (Map<?, ?>) categoryObj;
//                              Object categoryNameValue = categoryMap.get("categoryName");
//                              return categoryNameValue != null &&
//                                      categoryNameValue.toString().equalsIgnoreCase(category);
//                            }
//
//                            return categoryObj != null &&
//                                    categoryObj.toString().equalsIgnoreCase(category);
//                          } catch (Exception e) {
//                            return false;
//                          }
//                        });
//              })
//              .collect(Collectors.toList());
//    }

    return assetCategoryInspectionRepository.findByCompanyId(companyId)
            .stream()
            .filter(data -> {

              if (data.getStatus() == null || !data.getStatus().equals(StatusEnum.active)) {
                return false;
              }

              // Include inspections having no category
              if (data.getCategoryName() == null|| data.getCategoryName().isEmpty()|| data.getCategoryName().toString().trim().isEmpty()) {
                return true;
              }

              return data.getCategoryName().stream()
                      .anyMatch(categoryObj -> {
                        try {
                          if (categoryObj instanceof Map) {
                            Map<?, ?> categoryMap = (Map<?, ?>) categoryObj;
                            Object categoryNameValue = categoryMap.get("categoryName");
                            return categoryNameValue != null &&
                                    categoryNameValue.toString().equalsIgnoreCase(category);
                          }

                          return categoryObj != null &&
                                  categoryObj.toString().equalsIgnoreCase(category);
                        } catch (Exception e) {
                          return false;
                        }
                      });
            })
            .collect(Collectors.toList());
  }

  @Override
    public List<AssetCategoryInspection> getAllAssetInspection(Long companyId) {
        return assetCategoryInspectionRepository.findByCompanyId(companyId);
    }

    @Override
  public void addAssetInspectionInstance(
      AssetCategoryInspectionInstance assetCategoryInspectionInstance) {
      LocalDateTime now = LocalDateTime.now();
      boolean isNew = assetCategoryInspectionInstance.getId() == null
              || assetCategoryInspectionInstance.getId().isEmpty();

      if (isNew) {
        log.info("New AssetCategoryInspectionInstance : {}",assetCategoryInspectionInstance.toString());
        assetCategoryInspectionInstance.setCreatedAt(now);
        assetCategoryInspectionInstance.setUpdatedAt(now);
        inspectionInstanceIdGeneratorRepository.findByCompanyId(assetCategoryInspectionInstance.getCompanyId()).ifPresentOrElse((data) -> {
          Long seqId = data.getSeq();
          assetCategoryInspectionInstance.setAssetCategoryInspectionInstanceId(seqId);
          data.setSeq(seqId + 1);
          inspectionInstanceIdGeneratorRepository.save(data);

        }, () -> {
          InspectionInstanceIdGenerator inspectionInstanceIdGenerator = new InspectionInstanceIdGenerator();
          assetCategoryInspectionInstance.setAssetCategoryInspectionInstanceId(1L);
          inspectionInstanceIdGenerator.setSeq(2L);
          inspectionInstanceIdGenerator.setCompanyId(assetCategoryInspectionInstance.getCompanyId());
          inspectionInstanceIdGeneratorRepository.save(inspectionInstanceIdGenerator);
        });
      }
      else{
        log.info("Update AssetCategoryInspectionInstance : {}",assetCategoryInspectionInstance.toString());
        assetCategoryInspectionInstanceRepository.findById(assetCategoryInspectionInstance.getId()).ifPresent((data)->{
          assetCategoryInspectionInstance.setAssetCategoryInspectionInstanceId(data.getAssetCategoryInspectionInstanceId());
          assetCategoryInspectionInstance.setCreatedAt(
                  data.getCreatedAt() != null ? data.getCreatedAt() : now);
        });
        assetCategoryInspectionInstance.setUpdatedAt(now);
      }

    assetCategoryInspectionInstanceRepository.save(assetCategoryInspectionInstance);
  }

  @Override
  public void updateAssetInspectionInstance(AssetCategoryInspectionInstance assetCategoryInspection) {
    LocalDateTime now = LocalDateTime.now();
    assetCategoryInspectionInstanceRepository.findById(assetCategoryInspection.getId()).ifPresent((data)->{
      assetCategoryInspection.setAssetCategoryInspectionInstanceId(data.getAssetCategoryInspectionInstanceId());
      assetCategoryInspection.setCreatedAt(data.getCreatedAt() != null ? data.getCreatedAt() : now);
    });
    assetCategoryInspection.setUpdatedAt(now);
      assetCategoryInspectionInstanceRepository.save(assetCategoryInspection);
  }

  @Override
  public List<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionValues(Long companyId) {
    return assetCategoryInspectionInstanceRepository.findByCompanyId(companyId);
  }

  @Override
  public PaginatedResultDTO<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionValuesPaginated(Long companyId, int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
    Page<AssetCategoryInspectionInstance> page = assetCategoryInspectionInstanceRepository.findByCompanyId(companyId, pageable);
    return new PaginatedResultDTO<>(page.getContent(), page.getTotalElements());
  }

    @Override
    public List<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionInstanceByAsset(String assetId) {
        return assetCategoryInspectionInstanceRepository.findByAssetId(assetId);
    }

  @Override
  public PaginatedResultDTO<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionInstanceByAssetPaginated(String assetId, int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt"));
    Page<AssetCategoryInspectionInstance> page = assetCategoryInspectionInstanceRepository.findByAssetId(assetId, pageable);
    log.info("Total Instances for Asset {} : {}",assetId,page.getTotalElements());
    return new PaginatedResultDTO<>(page.getContent(), page.getTotalElements());
  }
  public void updateNameForAssetExtraFields(String oldName,  String newName,Long companyId) {
    Query query = new Query();
    query.addCriteria(
            Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                    .and("companyId").is(companyId)
    );

    Update update = new Update().set("name", newName);
    mongoTemplate.updateMulti(query, update, AssetExtraFields.class);
  }
  public void updateNameForMandatoryFields(String oldName,  String newName,Long companyId) {
    Query query = new Query();
    query.addCriteria(
            Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                    .and("companyId").is(companyId)
    );

    Update update = new Update().set("name", newName);
    mongoTemplate.updateMulti(query, update, AssetMandatoryFields.class);
  }
  public void updateNameForShowFields(String oldName,  String newName,Long companyId) {
    Query query = new Query();
    query.addCriteria(
            Criteria.where("name").regex("^" + Pattern.quote(oldName) + "$", "i") // case-insensitive exact match
                    .and("companyId").is(companyId)
    );

    Update update = new Update().set("name", newName);
    mongoTemplate.updateMulti(query, update, AssetShowFields.class);
  }
  @Override
  public AssetExtraFieldName updateExtraFieldName(ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO) {
    Optional<AssetExtraFieldName> optionalField=extraFieldNameRepository.findById(extraFieldNameUpdateDTO.getId());
    if(optionalField.isPresent()){
      String name=optionalField.get().getName();
      this.updateNameForAssetExtraFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());
      this.updateNameForMandatoryFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());
      this.updateNameForShowFields(name,extraFieldNameUpdateDTO.getName(),optionalField.get().getCompanyId());


      optionalField.get().setName(extraFieldNameUpdateDTO.getName());


      return extraFieldNameRepository.save(optionalField.get());
    }
    else{
      return null;
    }
  }
  public List<AssetCheckInOutData> findAllCheckInOutData(Long companyId) {
    return buildAssetCheckInOutDataList(companyId);
  }

  @Override
  public PaginatedResultCheckInOutDTO<AssetCheckInOutData> getAssetCheckInOutData(Long companyId, Long pageNumber, Long pageSize) {
    List<AssetCheckInOutData> assetCheckInOutDataList = buildAssetCheckInOutDataList(companyId);

    long totalCheckedIn = 0;
    long totalCheckedOut = 0;
    List<Assets> assetsList = assetsRepository.findByCompanyId(companyId);
    for (Assets asset : assetsList) {
      if (asset.getStatus() == null || !"active".equalsIgnoreCase(asset.getStatus())) {
        continue;
      }
      Optional<AssetCheckInOut> checkInOutOptional = checkInOutRepository.findByAssetId(asset.getId());
      if (checkInOutOptional.isPresent()) {
        String status = checkInOutOptional.get().getStatus();
        if ("Checked In".equalsIgnoreCase(status)) {
          totalCheckedIn++;
        } else if ("Checked Out".equalsIgnoreCase(status)) {
          totalCheckedOut++;
        }
      } else {
        totalCheckedIn++;
      }
    }
    log.info("Total Checked In: {}, Total Checked Out: {}", totalCheckedIn, totalCheckedOut);
    int totalRecords = assetCheckInOutDataList.size();
    int startIndex = (int) (pageNumber * pageSize);
    int endIndex = (int) Math.min(startIndex + pageSize, totalRecords);

    if (startIndex >= totalRecords) {
      return new PaginatedResultCheckInOutDTO<>(
              new ArrayList<>(),
              totalRecords,
              totalCheckedIn,
              totalCheckedOut
      );
    }

    return new PaginatedResultCheckInOutDTO<>(
            assetCheckInOutDataList.subList(startIndex, endIndex),
            totalRecords,
            totalCheckedIn,
            totalCheckedOut
    );
  }

  /**
   * Build check-in/out history rows from assets + details, with batch customer lookup.
   */
  private List<AssetCheckInOutData> buildAssetCheckInOutDataList(Long companyId) {
    List<Assets> assetsList = assetsRepository.findByCompanyId(companyId);
    List<AssetCheckInOutData> assetCheckInOutDataList = new ArrayList<>();

    Set<String> customerIds = assetsList.stream()
            .map(Assets::getCustomerId)
            .filter(Objects::nonNull)
            .filter(id -> !id.isEmpty())
            .collect(Collectors.toSet());

    final Map<String, CompanyCustomer> customerMap;
    if (!customerIds.isEmpty()) {
      Query customerQuery = new Query(Criteria.where("_id").in(new ArrayList<>(customerIds)));
      List<CompanyCustomer> customers = mongoTemplate.find(customerQuery, CompanyCustomer.class);
      customerMap = customers.stream()
              .collect(Collectors.toMap(CompanyCustomer::getId, c -> c, (c1, c2) -> c1));
    } else {
      customerMap = Collections.emptyMap();
    }

    for (Assets asset : assetsList) {
      Optional<AssetCheckInOut> assetCheckInOutOptional = checkInOutRepository.findByAssetId(asset.getId());
      if (assetCheckInOutOptional.isPresent()) {
        AssetCheckInOut assetCheckInOut = assetCheckInOutOptional.get();
        assetCheckInOut.getDetailsList().stream()
                .sorted(Comparator.comparing(AssetCheckInOutDetails::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(detail -> assetCheckInOutDataList.add(
                        toAssetCheckInOutData(asset, detail, customerMap)));
      } else {
        AssetCheckInOutData assetCheckInOutData = new AssetCheckInOutData();
        assetCheckInOutData.setAssetId(asset.getAssetId());
        assetCheckInOutData.setAssetName(asset.getName());
        assetCheckInOutData.setAssetStatus(asset.getStatus());
        assetCheckInOutData.setAction("Checked In");
        assetCheckInOutData.setUsername(asset.getCreatedBy());
        if (asset.getUpdatedAt() != null) {
          LocalDateTime date = LocalDateTime.parse(asset.getUpdatedAt());
          assetCheckInOutData.setDate(date);
          assetCheckInOutData.setTime(date.toLocalTime().withNano(0));
        }
        assetCheckInOutData.setLocation("NA");
//        assetCheckInOutData.setUsername("NA");
        assetCheckInOutData.setCustomerName(asset.getCustomer());
        populateCustomerId(asset, assetCheckInOutData, customerMap);
        assetCheckInOutDataList.add(assetCheckInOutData);
      }
    }

    assetCheckInOutDataList.sort(
            Comparator.comparing(AssetCheckInOutData::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AssetCheckInOutData::getTime, Comparator.nullsLast(Comparator.reverseOrder()))
    );

    return assetCheckInOutDataList;
  }

  private AssetCheckInOutData toAssetCheckInOutData(
          Assets asset, AssetCheckInOutDetails detail, Map<String, CompanyCustomer> customerMap) {
    AssetCheckInOutData data = new AssetCheckInOutData();
    data.setAssetId(asset.getAssetId());
    data.setAssetName(asset.getName());
    data.setAssetStatus(asset.getStatus());
    data.setAction(detail.getStatus());
    data.setDate(detail.getDate());
    if (detail.getUpdateTime() != null) {
      data.setTime(detail.getUpdateTime().toLocalTime().withNano(0));
    } else if (detail.getDate() != null) {
      data.setTime(detail.getDate().toLocalTime().withNano(0));
    }
    data.setLocation(detail.getLocation());
    data.setUsername(detail.getEmployee());
    data.setCustomerName(asset.getCustomer());
    populateCustomerId(asset, data, customerMap);
    return data;
  }

  private void populateCustomerId(
          Assets asset, AssetCheckInOutData data, Map<String, CompanyCustomer> customerMap) {
    if (asset.getCustomerId() != null && !asset.getCustomerId().isEmpty()) {
      CompanyCustomer companyCustomer = customerMap.get(asset.getCustomerId());
      if (companyCustomer != null) {
        data.setCustomerId(String.valueOf(companyCustomer.getCompanyCustomerId()));
      } else {
        data.setCustomerId(asset.getCustomerId());
      }
    }
  }

//    @Override
//    public PaginatedResultCheckInOutDTO<AssetCheckInOutData> getAssetCheckInOutData(Long companyId,Long pageNumber,Long pageSize) {
//        List<Assets> assetsList = assetsRepository.findByCompanyId(companyId);
//
//        List<AssetCheckInOutData> assetCheckInOutDataList = new ArrayList<>();
//        AtomicLong totalCheckedIn = new AtomicLong();
//        AtomicLong totalCheckedOut = new AtomicLong();
//
//        // Batch fetch all unique customerIds to avoid N+1 problem
//        Set<String> customerIds = assetsList.stream()
//                .map(Assets::getCustomerId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        // Fetch all CompanyCustomers in one query
//        Query customerQuery = new Query(Criteria.where("id").in(new ArrayList<>(customerIds)));
//        List<CompanyCustomer> customers = mongoTemplate.find(customerQuery, CompanyCustomer.class);
//        Map<String, CompanyCustomer> customerMap = customers.stream()
//                .collect(Collectors.toMap(CompanyCustomer::getId, c -> c, (c1, c2) -> c1));
//
//        assetsList.stream().forEach((asset) -> {
////            AssetCheckInOutData assetCheckInOutData = new AssetCheckInOutData();
////            assetCheckInOutData.setAssetId(asset.getAssetId());
////            assetCheckInOutData.setAssetName(asset.getName());
//            Optional<AssetCheckInOut> assetCheckInOutOptional = checkInOutRepository.findByAssetId(asset.getId());
//            if(assetCheckInOutOptional.isPresent()){
//                AssetCheckInOut assetCheckInOut = assetCheckInOutOptional.get();
//                if(assetCheckInOut.getStatus().equalsIgnoreCase("Checked In")){
//                    totalCheckedIn.getAndIncrement();
//                }
//                else if(assetCheckInOut.getStatus().equalsIgnoreCase("Checked Out")){
//                    totalCheckedOut.getAndIncrement();
//                }
//                assetCheckInOut.getDetailsList().stream()
//                        .sorted(Comparator.comparing(AssetCheckInOutDetails::getDate).reversed())
//                        .forEach(detail -> {
//                            // process each detail
//                            AssetCheckInOutData assetCheckInOutData = new AssetCheckInOutData();
//                            assetCheckInOutData.setAssetId(asset.getAssetId());
//                            assetCheckInOutData.setAssetName(asset.getName());
//                            assetCheckInOutData.setAction(detail.getStatus());
//                            assetCheckInOutData.setDate(detail.getDate());
//                            assetCheckInOutData.setTime(detail.getUpdateTime().toLocalTime().withNano(0));
//                            assetCheckInOutData.setLocation(detail.getLocation());
//                            assetCheckInOutData.setUsername(detail.getEmployee());
//                            assetCheckInOutData.setCustomerName(asset.getCustomer());
//                            // Use pre-fetched customer map instead of individual queries
//                            if (asset.getCustomerId() != null && !asset.getCustomerId().isEmpty()) {
//                              CompanyCustomer companyCustomer = customerMap.get(asset.getCustomerId());
//                              if (companyCustomer != null) {
//                                assetCheckInOutData.setCustomerId(String.valueOf(companyCustomer.getCompanyCustomerId()));
//                              } else {
//                                assetCheckInOutData.setCustomerId(asset.getCustomerId());
//                              }
//                            }
//                            assetCheckInOutDataList.add(assetCheckInOutData);
//
//                        });
//            }
//            else{
//                totalCheckedIn.getAndIncrement();
//                AssetCheckInOutData assetCheckInOutData = new AssetCheckInOutData();
//                assetCheckInOutData.setAssetId(asset.getAssetId());
//                assetCheckInOutData.setAssetName(asset.getName());
//                assetCheckInOutData.setAction("Checked In");
//                LocalDateTime date = LocalDateTime.parse(asset.getUpdatedAt());
//
//              LocalTime timeOnly = LocalDateTime.parse(asset.getUpdatedAt()).toLocalTime().withNano(0);
//                assetCheckInOutData.setTime(timeOnly);
//                assetCheckInOutData.setDate(date);
////                assetCheckInOutData.setTime(dateTime);
//                assetCheckInOutData.setLocation("NA");
//                assetCheckInOutData.setUsername("NA");
//                assetCheckInOutData.setCustomerName(asset.getCustomer());
//                // Use pre-fetched customer map instead of individual queries
//               if (asset.getCustomerId() != null && !asset.getCustomerId().isEmpty()) {
//                 CompanyCustomer companyCustomer = customerMap.get(asset.getCustomerId());
//                 if (companyCustomer != null) {
//                   assetCheckInOutData.setCustomerId(String.valueOf(companyCustomer.getCompanyCustomerId()));
//                 } else {
//                   assetCheckInOutData.setCustomerId(asset.getCustomerId());
//                 }
//               }
//                assetCheckInOutDataList.add(assetCheckInOutData);
//            }
//
//
//
//        });
//
//
//        // Sort by date (descending) and then by time (descending)
//        assetCheckInOutDataList.sort(
//                Comparator.comparing(AssetCheckInOutData::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
//                        .thenComparing(AssetCheckInOutData::getTime, Comparator.nullsLast(Comparator.reverseOrder()))
//        );
//
//
//
//
////        for (AssetCheckInOutData data : assetCheckInOutDataList) {
////            if (data.getAction().equalsIgnoreCase("Checked In")) {
////                totalCheckedIn++;
////            } else if (data.getAction().equalsIgnoreCase("Checked Out")) {
////                totalCheckedOut++;
////            }
////        }
//
//        // Pagination logic
//
//        int totalRecords = assetCheckInOutDataList.size();
//        int startIndex = (int) (pageNumber * pageSize);
//        int endIndex = (int) Math.min(startIndex + pageSize, totalRecords);
//
//        // Handle edge cases
//
//        if (startIndex >= totalRecords) {
//            return new PaginatedResultCheckInOutDTO<>(new ArrayList<>(),totalRecords, totalCheckedIn.get(), totalCheckedOut.get()); // Return empty list if pageNumber is out of bounds
//        }
//        return new PaginatedResultCheckInOutDTO<>(assetCheckInOutDataList.subList(startIndex, endIndex),totalRecords, totalCheckedIn.get(), totalCheckedOut.get());
////        return assetCheckInOutDataList;
//    }

    /**
     * Fetch assets with advanced filtering on predefined and custom fields with pagination
     * Uses MongoDB aggregation for efficient querying
     */
    @Override
    public PaginatedAssetResponseDTO getAssetsWithAdvancedFilter(AssetAdvancedFilterDTO filter) {
        log.info("Fetching assets with advanced filter - PageNumber: {}, PageSize: {}, CompanyId: {}",
                filter.getPageNumber(), filter.getPageSize(), filter.getCompanyId());

        try {
            String searchTerm = AdvanceFilterUtils.normalizeSearch(filter.getSearch());
            if (!searchTerm.isEmpty()) {
                filter.setSearch(searchTerm);
                List<AssetWithCustomFieldsDTO> allFiltered = assetsRepository.findAllWithAdvancedFilter(filter);
                enrichLocationNames(allFiltered);
                List<AssetWithCustomFieldsDTO> searched = allFiltered.stream()
                        .filter(asset -> AdvanceFilterUtils.matchesAssetSearch(asset, searchTerm))
                        .collect(Collectors.toList());
                PaginatedAssetResponseDTO response = paginateAssetResults(searched, filter);
                log.info("Search '{}' matched {} of {} filtered assets", searchTerm, response.getTotalElements(), allFiltered.size());
                return response;
            }

            PaginatedAssetResponseDTO response = assetsRepository.getAssetsWithAdvancedFilter(filter);
            enrichLocationNames(response.getAssets());
            log.info("Successfully fetched {} assets", response.getAssets().size());
            return response;

        } catch (Exception e) {
            log.error("Error fetching assets with advanced filter", e);
            return new PaginatedAssetResponseDTO(
                    new ArrayList<>(),
                    0,
                    0,
                    filter.getPageNumber(),
                    filter.getPageSize(),
                    false,
                    false
            );
        }
    }

    private PaginatedAssetResponseDTO paginateAssetResults(
            List<AssetWithCustomFieldsDTO> assets, AssetAdvancedFilterDTO filter) {
        int pageNumber = filter.getPageNumber() != null ? filter.getPageNumber() : 0;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        long totalCount = assets.size();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
        int start = pageNumber * pageSize;

        if (start >= assets.size()) {
            return new PaginatedAssetResponseDTO(
                    new ArrayList<>(),
                    totalCount,
                    totalPages,
                    pageNumber,
                    pageSize,
                    false,
                    pageNumber > 0
            );
        }

        int end = Math.min(start + pageSize, assets.size());
        return new PaginatedAssetResponseDTO(
                new ArrayList<>(assets.subList(start, end)),
                totalCount,
                totalPages,
                pageNumber,
                pageSize,
                pageNumber < totalPages - 1,
                pageNumber > 0
        );
    }

    /**
     * Enrich location information by resolving location/bin IDs to names
     */
    private void enrichLocationNames(List<AssetWithCustomFieldsDTO> assets) {
        Map<String, String> binIdToNameMap = new HashMap<>();
        Map<String, String> locationIdToNameMap = new HashMap<>();

        // Cache location and bin names
        binRepository.findAll().forEach(bin -> binIdToNameMap.put(bin.getId(), bin.getBinNumber()));
        locationRepository.findAll().forEach(loc -> locationIdToNameMap.put(loc.getId(), loc.getName()));

        assets.forEach(asset -> {
            if (asset.getLocation() != null) {
                if (asset.getLocation().startsWith("bin:")) {
                    String binId = asset.getLocation().substring(4);
                    asset.setLocationName(binIdToNameMap.getOrDefault(binId, asset.getLocation()));
                } else if (asset.getLocation().startsWith("location:")) {
                    String locationId = asset.getLocation().substring(9);
                    asset.setLocationName(locationIdToNameMap.getOrDefault(locationId, asset.getLocation()));
                }
            }
        });
    }


    @Override
  public byte[] generateAssetTemplateXlsx(Long companyId) throws IOException {
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("AssetTemplate");
    Row header = sheet.createRow(0);
    int col = 0;
    // Standard fields recommended for template
    String[] standardFields = new String[]{"Name","AssetId","SerialNumber","Category","Customer","CustomerId","Location","Status"};
    for (String h : standardFields) header.createCell(col++).setCellValue(h);
    if (extraFieldNames != null) {
      for (AssetExtraFieldName ef : extraFieldNames) header.createCell(col++).setCellValue(ef.getName());
    }

    // Add mock/sample rows (3 rows) using datatype hints from extraFieldNames
    int sampleRows = 3;
    for (int r = 1; r <= sampleRows; r++) {
      Row row = sheet.createRow(r);
      int c = 0;
      for (String field : standardFields) {
        switch (field.toLowerCase()) {
          case "name":
            row.createCell(c++).setCellValue("Sample Asset " + r);
            break;
          case "assetid":
            // numeric asset id
            row.createCell(c++).setCellValue(1000 + r);
            break;
          case "serialnumber":
            row.createCell(c++).setCellValue("SN" + (1000 + r));
            break;
          case "category":
            row.createCell(c++).setCellValue("DefaultCategory");
            break;
          case "customer":
            row.createCell(c++).setCellValue("Sample Customer");
            break;
          case "customerid":
            row.createCell(c++).setCellValue("cust-" + r);
            break;
          case "location":
            row.createCell(c++).setCellValue("Location " + r);
            break;
          case "status":
            row.createCell(c++).setCellValue(r % 2 == 0 ? "inActive" : "active");
            break;
          default:
            row.createCell(c++).setCellValue("");
        }
      }

      // Extra fields (ordered same as header creation)
      if (extraFieldNames != null) {
        for (AssetExtraFieldName ef : extraFieldNames) {
          String type = ef.getType() == null ? "string" : ef.getType().toLowerCase();
          String cellVal = "";
          try {
            if (type.contains("number") || type.equals("int") || type.equals("integer") || type.equals("double") || type.equals("float")) {
              // put numeric sample
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

    // autosize a few columns
    for (int i = 0; i < Math.min(col, 50); i++) sheet.autoSizeColumn(i);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    workbook.write(bos);
    workbook.close();
    return bos.toByteArray();
  }

  @Override
  public AssetTemplateFieldsDTO getTemplateFields(Long companyId) {
    AssetTemplateFieldsDTO dto = new AssetTemplateFieldsDTO();
    List<String> standard = Arrays.asList(
        "Name","AssetId","SerialNumber","Category","Customer","CustomerId","Location","Status"
    );
    dto.setStandardFields(standard);
    List<AssetExtraFieldName> extraFieldNames = extraFieldNameRepository.findByCompanyId(companyId);
    List<String> extraNames = new ArrayList<>();
    if (extraFieldNames != null) {
      for (AssetExtraFieldName ef : extraFieldNames) extraNames.add(ef.getName());
    }
    dto.setExtraFields(extraNames);
    return dto;
  }

  @Override
  public void updateAssetInspection(AssetCategoryInspection assetCategoryInspection) {
    log.info("Updating AssetCategoryInspection: {}", assetCategoryInspection.toString());
    // Check if the inspection template exists
    assetCategoryInspectionRepository.findById(assetCategoryInspection.getId()).ifPresent((existingInspection) -> {
      // Preserve the original ID and inspection ID
      assetCategoryInspection.setAssetCategoryInspectionId(existingInspection.getAssetCategoryInspectionId());
      log.info("Updated inspection with ID: {} and inspection ID: {}",
              assetCategoryInspection.getId(),
              assetCategoryInspection.getAssetCategoryInspectionId());
    });
    assetCategoryInspectionRepository.save(assetCategoryInspection);
    log.info("AssetCategoryInspection updated successfully");
  }

  @Override
  public AssetUniqueFieldConfigurationDTO saveUniqueFieldConfiguration(AssetUniqueFieldConfigurationDTO configDTO) {
    try {
      log.info("Saving unique field configuration for company: {} field: {}", configDTO.getCompanyId(), configDTO.getFieldName());

      Optional<AssetUniqueFieldConfiguration> existingOpt =
              assetUniqueFieldConfigurationRepository.findByCompanyIdAndFieldName(
                      configDTO.getCompanyId(), configDTO.getFieldName());

      AssetUniqueFieldConfiguration config = modelMapper.map(configDTO, AssetUniqueFieldConfiguration.class);
      String now = LocalDateTime.now().toString();
      if (existingOpt.isPresent()) {
        AssetUniqueFieldConfiguration existing = existingOpt.get();
        config.setId(existing.getId());
        config.setCreatedAt(existing.getCreatedAt());
      } else {
        config.setCreatedAt(now);
      }
      config.setUpdatedAt(now);

      AssetUniqueFieldConfiguration savedConfig = assetUniqueFieldConfigurationRepository.save(config);
      log.info("Unique field configuration saved successfully with ID: {}", savedConfig.getId());

      return modelMapper.map(savedConfig, AssetUniqueFieldConfigurationDTO.class);
    } catch (Exception e) {
      log.error("Error saving unique field configuration: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to save unique field configuration: " + e.getMessage());
    }
  }

  @Override
  public List<AssetUniqueFieldConfigurationDTO> getUniqueFieldConfigurations(Long companyId) {
    try {
      log.info("Fetching unique field configurations for company: {}", companyId);

      List<AssetUniqueFieldConfiguration> configs = assetUniqueFieldConfigurationRepository.findByCompanyId(companyId);
      List<AssetUniqueFieldConfigurationDTO> dtos = new ArrayList<>();

      configs.forEach(config -> dtos.add(modelMapper.map(config, AssetUniqueFieldConfigurationDTO.class)));

      log.info("Found {} unique field configurations", dtos.size());
      return dtos;
    } catch (Exception e) {
      log.error("Error fetching unique field configurations: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  @Override
  public AssetUniqueFieldValidationDTO validateUniqueFields(AssetsDTO assetsDTO, Long companyId) {
    try {
      log.info("Validating unique fields for asset in company: {}", companyId);
      log.info("Extra fields in DTO: {}", assetsDTO.getExtraFields());

      AssetUniqueFieldValidationDTO validationResult = new AssetUniqueFieldValidationDTO();
      Map<String, List<String>> conflicts = new HashMap<>();

      List<AssetUniqueFieldConfiguration> uniqueConfigs = assetUniqueFieldConfigurationRepository.findByCompanyIdAndIsUniqueTrue(companyId);

      log.info("Found {} unique field configurations", uniqueConfigs == null ? 0 : uniqueConfigs.size());

      if (uniqueConfigs == null || uniqueConfigs.isEmpty()) {
        validationResult.setValid(true);
        validationResult.setMessage("No unique field configurations found");
        return validationResult;
      }

      for (AssetUniqueFieldConfiguration config : uniqueConfigs) {
        String fieldName = config.getFieldName();
        String fieldType = config.getType();
        Object fieldValue = null;

        log.info("Checking unique config - fieldName: {}, type: {}, isUnique: {}", fieldName, fieldType, config.getIsUnique());

        if ("STANDARD".equalsIgnoreCase(fieldType)) {
          fieldValue = getStandardFieldValue(assetsDTO, fieldName);

          if (fieldValue == null || "".equals(fieldValue.toString().trim())) {
            log.debug("Field {} has empty value, skipping validation", fieldName);
            continue;
          }

          List<Assets> existingAssets = checkStandardFieldUniqueness(companyId, fieldName, fieldValue, assetsDTO.getId());
          if (!existingAssets.isEmpty()) {
            List<String> conflictingIds = existingAssets.stream()
              .map(a -> a.getId() != null ? a.getId() : (a.getAssetId() != null ? a.getAssetId().toString() : ""))
              .collect(Collectors.toList());
            conflicts.put(fieldName, conflictingIds);
            log.warn("Unique field {} violation found with values: {}", fieldName, conflictingIds);
          }
        } else if ("EXTRA".equalsIgnoreCase(fieldType)) {
          log.info("Validating EXTRA field: {}", fieldName);

          if (assetsDTO.getExtraFields() != null && assetsDTO.getExtraFields().containsKey(fieldName)) {
            fieldValue = assetsDTO.getExtraFields().get(fieldName);
            log.info("Extra field {} value from DTO: {}", fieldName, fieldValue);

            if (fieldValue == null || "".equals(fieldValue.toString().trim())) {
              log.debug("Extra field {} has empty value, skipping validation", fieldName);
              continue;
            }

            log.info("Checking uniqueness for extra field {} with value: {}", fieldName, fieldValue);
            List<AssetExtraFields> existingExtraFields = checkExtraFieldUniqueness(companyId, fieldName, fieldValue.toString(), assetsDTO.getId());
            log.info("Found {} existing extra fields with same name and value", existingExtraFields.size());

            if (!existingExtraFields.isEmpty()) {
              List<String> conflictingIds = existingExtraFields.stream()
                .map(e -> e.getAssetId() != null ? e.getAssetId() : "")
                .collect(Collectors.toList());
              conflicts.put(fieldName, conflictingIds);
              log.warn("Unique extra field {} violation found with values: {}", fieldName, conflictingIds);
            }
          } else {
            log.info("Extra field {} not provided in request or extraFields is null", fieldName);
            log.debug("ExtraFields: {}", assetsDTO.getExtraFields());
          }
        }
      }

      if (!conflicts.isEmpty()) {
        validationResult.setValid(false);
        validationResult.setConflicts(conflicts);

        String conflictFields = conflicts.keySet().stream()
                .map(key -> key.equals("serialNumber") ? "Serial Number" : key)
                .collect(Collectors.joining(", "));

        validationResult.setMessage("Unique field constraint violated for fields: " + conflictFields);
      }else {
        validationResult.setValid(true);
        validationResult.setMessage("All unique field constraints satisfied");
      }

      return validationResult;
    } catch (Exception e) {
      log.error("Error validating unique fields: {}", e.getMessage(), e);
      AssetUniqueFieldValidationDTO result = new AssetUniqueFieldValidationDTO();
      result.setValid(false);
      result.setMessage("Error during validation: " + e.getMessage());
      return result;
    }
  }

  @Override
  public void deleteUniqueFieldConfiguration(String configId) {
    try {
      log.info("Deleting unique field configuration: {}", configId);
      assetUniqueFieldConfigurationRepository.deleteById(configId);
      log.info("Unique field configuration deleted successfully");
    } catch (Exception e) {
      log.error("Error deleting unique field configuration: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to delete unique field configuration: " + e.getMessage());
    }
  }

  @Override
  public void deleteUniqueFieldConfigurationByCompanyAndField(Long companyId, String fieldName) {
    try {
      log.info("Deleting unique field configuration for company: {} field: {}", companyId, fieldName);
      assetUniqueFieldConfigurationRepository.deleteByCompanyIdAndFieldName(companyId, fieldName);
      log.info("Unique field configuration deleted successfully");
    } catch (Exception e) {
      log.error("Error deleting unique field configuration: {}", e.getMessage(), e);
    }
  }

  private Object getStandardFieldValue(AssetsDTO assetsDTO, String fieldName) {
    switch (fieldName.toLowerCase()) {
      case "name":
        return assetsDTO.getName();
      case "serialnumber":
        return assetsDTO.getSerialNumber();
      case "category":
        return assetsDTO.getCategory();
      case "customer":
        return assetsDTO.getCustomer();
      case "customerid":
        return assetsDTO.getCustomerId();
      case "location":
        return assetsDTO.getLocation();
      case "status":
        return assetsDTO.getStatus();
      case "assetid":
        return assetsDTO.getAssetId();
      default:
        return null;
    }
  }

  private List<Assets> checkStandardFieldUniqueness(Long companyId, String fieldName, Object fieldValue, String excludeAssetId) {
    try {
      Query query = new Query();
      query.addCriteria(Criteria.where("companyId").is(companyId));

      switch (fieldName.toLowerCase()) {
        case "name":
          query.addCriteria(Criteria.where("name").is(fieldValue));
          break;
        case "serialnumber":
          query.addCriteria(Criteria.where("serialNumber").is(fieldValue));
          break;
        case "category":
          query.addCriteria(Criteria.where("category").is(fieldValue));
          break;
        case "customer":
          query.addCriteria(Criteria.where("customer").is(fieldValue));
          break;
        case "customerid":
          query.addCriteria(Criteria.where("customerId").is(fieldValue));
          break;
        case "location":
          query.addCriteria(Criteria.where("location").is(fieldValue));
          break;
        case "status":
          query.addCriteria(Criteria.where("status").is(fieldValue));
          break;
        case "assetid":
          query.addCriteria(Criteria.where("assetId").is(fieldValue));
          break;
      }

      if (excludeAssetId != null && !excludeAssetId.isEmpty()) {
        query.addCriteria(Criteria.where("_id").ne(excludeAssetId));
      }

      return mongoTemplate.find(query, Assets.class);
    } catch (Exception e) {
      log.error("Error checking standard field uniqueness: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  public List<AssetExtraFields> checkExtraFieldUniqueness(Long companyId, String fieldName, String fieldValue, String excludeAssetId) {
    try {
      log.info("Checking extra field uniqueness - companyId: {}, fieldName: {}, fieldValue: {}, excludeAssetId: {}",
        companyId, fieldName, fieldValue, excludeAssetId);

      Query query = new Query();
      query.addCriteria(Criteria.where("companyId").is(companyId));
      query.addCriteria(Criteria.where("name").is(fieldName));
      query.addCriteria(Criteria.where("value").is(fieldValue));

      if (excludeAssetId != null && !excludeAssetId.isEmpty()) {
        query.addCriteria(Criteria.where("assetId").ne(excludeAssetId));
      }

      List<AssetExtraFields> results = mongoTemplate.find(query, AssetExtraFields.class);
      log.info("Query results for extra field uniqueness: {} records found", results.size());
      results.forEach(r -> log.info("  - AssetId: {}, Name: {}, Value: {}", r.getAssetId(), r.getName(), r.getValue()));

      return results;
    } catch (Exception e) {
      log.error("Error checking extra field uniqueness: {}", e.getMessage(), e);
      return new ArrayList<>();
    }
  }

  public boolean isFieldMarkedUnique(Long companyId, String fieldName, String fieldType) {
    try {
      Optional<AssetUniqueFieldConfiguration> config = assetUniqueFieldConfigurationRepository.findByCompanyIdAndFieldName(companyId, fieldName);
      return config.isPresent() && config.get().getIsUnique() != null && config.get().getIsUnique();
    } catch (Exception e) {
      log.error("Error checking if field is marked unique: {}", e.getMessage(), e);
      return false;
    }
  }
}
