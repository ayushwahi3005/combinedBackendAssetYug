package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.*;
import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.Map;

public interface UserService {
    /**
     * Resend verification email to user if not yet verified
     */
   /**
   * Resend the Firebase email verification link to a user
   * @param email The email of the user
   * @param companyId The company ID of the user
   * @throws UserException If user is not found or already verified
   * @throws TheMailException If there's an error sending the email
   */
  void resendFirebaseVerificationEmail(String email, Long companyId) throws UserException, TheMailException, FirebaseAuthException, UserEmailAlreadyVerifiedException;
  
  void resendVerificationEmail(String email, Long companyId) throws UserException, TheMailException;

  public void sendSimpleMessage(String to, String subject, String text);

  public AuthenticationResponseDTO generateToken(Mail mail);

  public Claims decodeDetails(String token);

  public List<UsersDTO> getAllUsers(Long companyId);

  public Users registerUser(Users user) throws UserException;

  public UsersDTO getUsers(Long companyId, String email) throws UserException;

  public UsersDTO getUserForInvite(Long companyId, String email) throws UserException;

  public void updateUser(UsersDTO usersDTO) throws UserException;

  public void updateUserStatus(UsersDTO usersDTO) throws UserException, UserCannotActivateException;

  public List<UsersDTO> getAllUsersByRole(String role, Long companyId);

  public void deleteUser(Long companyId, String email, String authHeader)
      throws FirebaseAuthException, UserCannotDeletedException, Exception;

  //	public UserDTO resendMail(String email,Long companyId);

  public void addExtraFields(UserExtraFieldsDTO extraFieldsDTO) throws Exception;

  public List<UserExtraFieldsDTO> getExtraFields(String id);

  public void deleteExtraFields(String id) throws Exception;

  public List<UserExtraFieldNameDTO> getUserExtraField(Long companyId);

  public void addUserExtraField(UserExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException;

  public void deleteUserExtraField(String id);

  public Map<String, Map<String, String>> getextraFieldList(Long companyId);

  public void updateShowFields(UserShowFields showFields);

  public void updateMandatoryFields(UserMandatoryFields mandatoryFields);

  public UserShowFields getShowFields(String name, Long companyId);

  public UserMandatoryFields getMandatoryFields(String name, Long companyId);

  public List<UserShowFields> getAllShowFields(Long companyId);

  public List<UserMandatoryFields> getAllMandatoryFields(Long companyId);

  public void deleteShowAndMandatoryFields(Long companyId, String name);

  public void updateLastLogin(String email,Long companyId);
}
