package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.*;
import java.util.List;
import org.springframework.data.domain.Page;

public interface CustomerService {
  public BaseResponseDTO addCustomer(CustomerDTO customerDTO)
      throws UserAlreadyPresentException, Exception;

  public CustomerDTO getCustomer(String email) throws Exception;

  public void sentResetOTP(String email) throws NoEmailFoundException;

  public void updatePassword(String email, String otp, String password)
      throws FirebaseAuthException, OTPException, NoEmailFoundException, SamePasswordException;


  public Boolean checkCustomer(String email) throws Exception;

  public CustomerSubscribedDTO getCustomerSubscription(String email) throws NoSubscriptionError;

  public void addSubscription(String email) throws Exception;

  public AuthenticationResponseDTO authenticate(
      AuthenticationRequestDTO authenticationRequestDTO, String deviceId) throws Exception;

  public AuthenticationResponseDTO getLoginToken(String email, String password, String deviceId)
      throws WrongCredentialException, UserNotFound;

  public void addCompanyInformation(CompanyInformation companyInformation) throws Exception;

  public CompanyInformation getcompanyInformation(Long companyId);

  public CompanyIdDTO getCompanyId(String email);

  public BaseResponseDTO addUsers(CustomerDTO customerDTO);

  public List<String> activeUsers(Long companyId);

  public AccountLockInfoDTO getAccountInfo(String email);

  public void updateAccountInfo(AccountLockInfoDTO accountLockInfo);

  public void deleteUser(Long companyId, String email) throws CustomerException;

  public void addRoleAndPermission(CustomRoleDTO customRoleDTO);

  public void deleteRoleAndPermission(String customRoleId) throws Exception;

  public List<CustomRoleDTO> getRoleAndPermission(Long companyId);

  public Long countByRoleName(String name, Long companyId);

  public CustomRoleDTO roleAndPermissionByName(Long companyId, String name);

  public Location addLocation(Location location) throws LocationAlreadyPresentException;

  public Location updateLocation(Location location);

  public List<Location> getAllLocation(Long companyId);

  public void deleteLocation(String id);

  public Bin addBin(BinDTO bin);

  public Bin updateBin(BinDTO bin);

  public List<BinDTO> getAllBin(Long companyId);

  public List<LocationWithBinsDTO> getLocationsWithBins(Long companyId);

  public void deleteBin(String id);

  public ImportHistory addImportHistory(ImportHistory importHistory);

  public Page<ImportHistoryDTO> getImportHistoryList(Long companyId, int page, int size);

  public void updateImportHistory(ImportHistory importHistory);

  public void addCardDetails(CustomerStripeDetails customerStripeDetails);

  public CustomerStripeDetails getCardDetails(Long companyId);

  public void deleteCardDetails(String id);
}
