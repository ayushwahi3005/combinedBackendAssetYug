package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.exception.UserCannotDeletedException;
import com.quantumai.customer.exception.UserException;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;

public interface UserService {

  public void sendSimpleMessage(String to, String subject, String text);

  public AuthenticationResponseDTO generateToken(Mail mail);

  public Claims decodeDetails(String token);

  public List<UsersDTO> getAllUsers(String companyId);

  public void registerUser(Users user) throws UserException;

  public UsersDTO getUsers(String companyId, String email);

  public void updateUser(UsersDTO usersDTO);

  public List<UsersDTO> getAllUsersByRole(String role, String companyId);

  public void deleteUser(String companyId, String email, String authHeader)
      throws FirebaseAuthException, UserCannotDeletedException, Exception;
  //	public UserDTO resendMail(String email,String companyId);

  public void addExtraFields(UserExtraFieldsDTO extraFieldsDTO) throws Exception;

  public List<UserExtraFieldsDTO> getExtraFields(String id);

  public void deleteExtraFields(String id) throws Exception;

  public List<UserExtraFieldNameDTO> getUserExtraField(String companyId);

  public void addUserExtraField(UserExtraFieldNameDTO extraFieldNameDTO)
          throws ExtraFieldAlreadyPresentException;

  public void deleteUserExtraField(String id);

  public Map<String, Map<String, String>> getextraFieldList(String companyId);

  public void updateShowFields(UserShowFields showFields);

  public void updateMandatoryFields(UserMandatoryFields mandatoryFields);

  public UserShowFields getShowFields(String name, String companyId);

  public UserMandatoryFields getMandatoryFields(String name, String companyId);

  public List<UserShowFields> getAllShowFields(String companyId);

  public List<UserMandatoryFields> getAllMandatoryFields(String companyId);

  public void deleteShowAndMandatoryFields(String companyId, String name);


}
