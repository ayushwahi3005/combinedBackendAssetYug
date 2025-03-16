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
  public List<AssetsDTO> getAssetsDetails(String companyId);

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

  public List<AssetExtraFieldNameDTO> getAssetExtraField(String companyId);

  public void addAssetExtraField(AssetExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException;

  public void deleteAssetExtraField(String id);

  public Map<String, Map<String, String>> getextraFieldList(String companyId);

  public void addCheckInOut(AssetCheckInDTO checkInDTO);

  public List<AssetCheckInOutDTO> getCheckOutInList(String assetId);

  public AssetFile addAssetFile(MultipartFile file, String assetId) throws IOException;

  public List<AssetFileDTO> getAssetFile(String assetId);

  public AssetFileDTO downloadFile(String id);

  public void deleteFile(String id);

  public void updateShowFields(AssetShowFields showFields);

  public void updateMandatoryFields(AssetMandatoryFields mandatoryFields);

  public AssetShowFields getShowFields(String name, String companyId);

  public AssetMandatoryFields getMandatoryFields(String name, String companyId);

  public List<AssetShowFields> getAllShowFields(String companyId);

  public List<AssetMandatoryFields> getAllMandatoryFields(String companyId);

  public void deleteShowAndMandatoryFields(String companyId, String name);

  public void updateAssetWithFile(List<AssetsDTO> assetsDTOList, String companyId);

  public void qrDataUpdation(AssetQR qr);

  public AssetQR getQRData(String companyId);

  public PaginatedResultDTO<String> getAllAssetDetails(String companyId);

  public PaginatedResultDTO<String> sortAssets(
      String companyId, String field, Integer pageNumber, Integer pageSize);

  public List<String> searchedAssets(String companyId, String data, String field);

  public void updateAssetsWithInActive(String customerId);

  public PaginatedResultDTO<String> advanceFilter(
      Object filter, int pageNumber, int pageSize, String field, String searchData, Boolean asc);

  public CheckInCheckOutCountDTO checkInCheckOut(String companyId);

  public List<AssetsDTO> assetListFromSerialNumber(String companyId, String serialNumber);

  public List<AssetCheckInOut> filterByCheckedInOut(String companyId, Boolean checkedIn);

  public void addCategory(CategoryDTO categoryDTO) throws CategoryException;

  public void updateCategory(CategoryDTO categoryDTO);

  public List<AssetCategory> getCategoryList(String companyId);

  public void deleteCategory(String id);

  public AssetCategory getCategoryListById(String companyId, String id);

  public int countAssetByCategory(String category);

  public List<AssetCategory> getActiveCategoryList(String companyId);

  public List<AssetsDTO> getActiveAssets(String companyId);

  public Map<String, List<AssetsDTO>> getAssetByCategory(String companyId);

  public void addAssetInspection(AssetCategoryInspection assetCategoryInspection);

  public AssetCategoryInspection getAssetInspection(String assetInspectionId) throws Exception;

  public List<AssetCategoryInspection> getAllAssetInspection(String companyId);

  public void addAssetInspectionValues(AssetCategoryInspectionValues assetCategoryInspectionValues);

  public List<AssetCategoryInspectionValues> getAllAssetCategoryInspectionValues(String companyId);
}
