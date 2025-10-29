package com.quantumai.customer.service;

import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface AssetsService {
  public List<AssetsDTO> getAssetsDetails(Long companyId);

  public PaginatedResultDTO<String> getAssetsDetailsByCustomerId(
      String customerId, Integer pageNumber);

  public AssetsDTO addAssets(AssetsDTO assetsDTO);

  public void importExcel(List<AssetsDTO> assetsDTOList, Map<String, String> columnMap);

  public void addImage(AssetImageDTO assetImageDTO) throws Exception;

  public void removeImage(String id) throws Exception;

  public void removeAsset(String id) throws Exception;

  public AssetsDTO getAsset(String assetId) throws Exception;

  public void addExtraFields(AssetExtraFieldsDTO extraFieldsDTO) throws Exception;

  public List<AssetExtraFieldsDTO> getExtraFields(String id);

  public void deleteExtraFields(String id) throws Exception;

  public List<AssetExtraFieldNameDTO> getAssetExtraField(Long companyId);

  public void addAssetExtraField(AssetExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException;

  public void deleteAssetExtraField(String id);

  public Map<String, Map<String, String>> getextraFieldList(Long companyId);

  public void addCheckInOut(AssetCheckInDTO checkInDTO);

  public List<AssetCheckInOutDTO> getCheckOutInList(String assetId);

  public AssetFile addAssetFile(MultipartFile file, String assetId) throws IOException;

  public List<AssetFileDTO> getAssetFile(String assetId);

  public AssetFileDTO downloadFile(String id);

  public void deleteFile(String id);

  public void updateShowFields(AssetShowFields showFields);

  public void updateMandatoryFields(AssetMandatoryFields mandatoryFields);

  public AssetShowFields getShowFields(String name, Long companyId);

  public AssetMandatoryFields getMandatoryFields(String name, Long companyId);

  public List<AssetShowFields> getAllShowFields(Long companyId);

  public List<AssetMandatoryFields> getAllMandatoryFields(Long companyId);

  public void deleteShowAndMandatoryFields(Long companyId, String name);

  public void updateAssetWithFile(List<AssetsDTO> assetsDTOList, Long companyId);

  public void qrDataUpdation(AssetQR qr);

  public AssetQR getQRData(Long companyId);

  public PaginatedResultDTO<String> getAllAssetDetails(Long companyId);

  public PaginatedResultDTO<String> getAllAssetDetailsWithLocationDetails(Long companyId,Object filter);

  public PaginatedResultDTO<String> sortAssets(
      Long companyId, String field, Integer pageNumber, Integer pageSize);

  public List<String> searchedAssets(Long companyId, String data, String field);

  public void updateAssetsWithInActive(String customerId);

  public PaginatedResultDTO<String> advanceFilter(
      Object filter, int pageNumber, int pageSize, String field, String searchData, Boolean asc);

  public CheckInCheckOutCountDTO checkInCheckOut(Long companyId);

  public List<AssetsDTO> assetListFromSerialNumber(Long companyId, String serialNumber);

  public List<AssetCheckInOut> filterByCheckedInOut(Long companyId, Boolean checkedIn);

  public void addCategory(CategoryDTO categoryDTO) throws Exception;

  public void updateCategory(CategoryDTO categoryDTO);

  public List<AssetCategory> getCategoryList(Long companyId);

  public void deleteCategory(String id);

  public AssetCategory getCategoryListById(Long companyId, String id);

  public int countAssetByCategory(String category);

  public List<AssetCategory> getActiveCategoryList(Long companyId);

  public List<AssetsDTO> getActiveAssets(Long companyId);

  public Map<String, List<AssetsDTO>> getAssetByCategory(Long companyId);

  public void addAssetInspection(AssetCategoryInspection assetCategoryInspection);

  public void deleteAssetInspection(String id);

  public AssetCategoryInspection getAssetInspection(String assetInspectionId) throws Exception;

  public List<AssetCategoryInspection> getAllAssetInspectionByCategory(Long companyId,String category);

  public List<AssetCategoryInspection> getAllAssetInspection(Long companyId);

  public void addAssetInspectionInstance(
      AssetCategoryInspectionInstance assetCategoryInspectionInstance);

  public List<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionValues(Long companyId);

  public List<AssetCategoryInspectionInstance> getAllAssetCategoryInspectionInstanceByAsset(String assetId);

  public AssetExtraFieldName updateExtraFieldName(ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO);

//  public List<Map<String,>> getAssetByCompanyCategory(Long companyId);
}
