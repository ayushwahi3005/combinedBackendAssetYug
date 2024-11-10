package com.quantumai.customer.service;

import com.quantumai.customer.dto.*;

import com.quantumai.customer.entity.CompanyCustomerCategory;
import com.quantumai.customer.entity.CompanyCustomerFile;
import com.quantumai.customer.entity.CompanyCustomerMandatoryFields;
import com.quantumai.customer.entity.CompanyCustomerShowFields;
import com.quantumai.customer.exception.CategoryException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface CompanyCustomerService {
	
	public CompanyCustomerDTO addCustomer(CompanyCustomerDTO companyCustomerDTO);
	public CompanyCustomerDTO getCustomer(String id);
	public List<CompanyCustomerDTO> getAllCustomer(String companyId);
	public void updateCustomer(CompanyCustomerDTO companyCustomerDTO);
	public void deleteCustomer(String id);
	public List<String> getAllCustomerWithExtraColumns(String companyId);
	public List<String> searchedCompanyCustomer(String companyId,String search,String category);
	public List<String> sortCompanyCustomer(String companyId,String category);
	public void addCompanyCustomerExtraField(CompanyCustomerExtraFieldNameDTO extraFieldNameDTO) throws ExtraFieldAlreadyPresentException;
	public List<CompanyCustomerExtraFieldNameDTO> getCompanyCustomerExtraField(String companyId);
	public void deleteCompanyCustomerExtraField(String id);
	public void updateMandatoryFields(CompanyCustomerMandatoryFields mandatoryFields);
	public void updateShowFields(CompanyCustomerShowFields showFields);
	public CompanyCustomerMandatoryFields getMandatoryFields(String name, String companyId);
	public CompanyCustomerShowFields getShowFields(String name, String companyId);
	public List<CompanyCustomerMandatoryFields> getAllMandatoryFields(String companyId);
	public List<CompanyCustomerShowFields> getAllShowFields(String companyId);
	public void deleteShowAndMandatoryFields(String companyId, String name);
	public Map<String, Map<String,String>> getextraFieldList(String companyId);
	public void addExtraFields(CompanyCustomerExtraFieldsDTO extraFieldsDTO);
	public List<CompanyCustomerExtraFieldsDTO> getExtraFields(String id);
	public void deleteExtraFields( String id) throws Exception;
	public void deleteExtraFieldByCompanyCustomer(String id);
	public CompanyCustomerFile addCompanyCustomerFile(MultipartFile file, String companyCustomerId) throws IOException;
	public List<CompanyCustomerFileDTO> getCompanyCustomerFile(String companyCustomerId);
	public CompanyCustomerFileDTO downloadFile(String id);
	public void deleteFile(String id);
	public CompanyCustomerDTO getCompanyCustomerByLocalId(Integer id,String companyId);

	public PaginatedResultDTO<String> getAllCustomerDetails(String companyId);
	public PaginatedResultDTO<String> sortCustomers(String companyId, String field, Integer pageNumber, Integer pageSize);

//	public void updateCustomersWithInActive(String customerId);
	public PaginatedResultDTO<String> advanceFilter(Object filter, int pageNumber, int pageSize,String field,String searchData,Boolean asc);
	public String working();
	public void addCategory(CategoryDTO categoryDTO) throws CategoryException;
	public List<CompanyCustomerCategory> getCategoryList(String companyId);
	public List<CompanyCustomerCategory> getActiveCategoryList(String companyId);
	public void deleteCategory(String id);
	public void updateCategory(CategoryDTO categoryDTO);
	public CompanyCustomerCategory getCategoryListById(String companyId,String id);
	
	
	

}
