package com.quantumai.customer.service;

import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.EmailAlreadyExistsException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyCustomerService {

  public CompanyCustomerDTO addCustomer(CompanyCustomerDTO companyCustomerDTO) throws EmailAlreadyExistsException;

  public CompanyCustomerDTO getCustomer(String id);

  public List<CompanyCustomerDTO> getAllCustomer(Long companyId);

  public void updateCustomer(CompanyCustomerDTO companyCustomerDTO) throws EmailAlreadyExistsException;

  public void deleteCustomer(String id);

  public List<String> getAllCustomerWithExtraColumns(Long companyId);

  public List<String> searchedCompanyCustomer(Long companyId, String search, String category);

  public List<String> sortCompanyCustomer(Long companyId, String category);

  public void addCompanyCustomerExtraField(CompanyCustomerExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException;

  public List<CompanyCustomerExtraFieldNameDTO> getCompanyCustomerExtraField(Long companyId);

  public void deleteCompanyCustomerExtraField(String id) throws Exception;

  public void updateMandatoryFields(CompanyCustomerMandatoryFields mandatoryFields);

  public void updateShowFields(CompanyCustomerShowFields showFields);

  public CompanyCustomerMandatoryFields getMandatoryFields(String name, Long companyId);

  public CompanyCustomerShowFields getShowFields(String name, Long companyId);

  public List<CompanyCustomerMandatoryFields> getAllMandatoryFields(Long companyId);

  public List<CompanyCustomerShowFields> getAllShowFields(Long companyId);

  public void deleteShowAndMandatoryFields(Long companyId, String name);

  public Map<String, Map<String, String>> getextraFieldList(Long companyId);

  public void addExtraFields(CompanyCustomerExtraFieldsDTO extraFieldsDTO);

  public List<CompanyCustomerExtraFieldsDTO> getExtraFields(String id);

  public void deleteExtraFields(String id) throws Exception;

  public void deleteExtraFieldByCompanyCustomer(String id);

  public CompanyCustomerFile addCompanyCustomerFile(MultipartFile file, String companyCustomerId)
      throws IOException;

  public List<CompanyCustomerFileDTO> getCompanyCustomerFile(String companyCustomerId);

  public CompanyCustomerFileDTO downloadFile(String id);

  public void deleteFile(String id);

  public CompanyCustomerDTO getCompanyCustomerByLocalId(Integer id, Long companyId);

  public PaginatedResultDTO<String> getAllCustomerDetails(Long companyId);

  public PaginatedResultDTO<String> sortCustomers(
      Long companyId, String field, Integer pageNumber, Integer pageSize);

  //	public void updateCustomersWithInActive(String customerId);
  public PaginatedResultDTO<String> advanceFilter(
      Object filter, int pageNumber, int pageSize, String field, String searchData, Boolean asc);

  public String working();

  public void addCategory(CategoryDTO categoryDTO) throws Exception;

  public List<CompanyCustomerCategory> getCategoryList(Long companyId);

  public List<CompanyCustomerCategory> getActiveCategoryList(Long companyId);

  public void deleteCategory(String id);

  public void updateCategory(CategoryDTO categoryDTO);

  public int countCompanyCustomerByCategory(String category);

  public CompanyCustomerCategory getCategoryListById(Long companyId, String id);

  public CompanyCustomerExtraFieldName updateExtraFieldName(ExtraFieldNameUpdateDTO extraFieldNameUpdateDTO);

  // Template helpers
  public CompanyCustomerTemplateFieldsDTO getTemplateFields(Long companyId);

  public byte[] generateCompanyCustomerTemplateXlsx(Long companyId) throws IOException;

  public byte[] generateCompanyCustomerTemplateCsv(Long companyId) throws IOException;

//  public Integer getCustomFieldCustomerCount(Long companyId, String id);
}
