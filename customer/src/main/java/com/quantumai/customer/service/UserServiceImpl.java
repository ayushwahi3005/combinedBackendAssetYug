package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.exception.TheMailException;
import com.quantumai.customer.exception.UserException;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

  @Autowired private JavaMailSender emailSender;
  @Autowired JwtService jwtService;

  @Autowired private PasswordEncoder passwordEncoder;

  RestTemplate restTemplate = new RestTemplate();

  String workOrderAPI = "http://localhost:8083/workorder/getWorkOrderByTechnicianId/";

  private HttpHeaders headers = new HttpHeaders();

  private ModelMapper modelMapper = new ModelMapper();

  @Autowired private UsersRepository usersRepository;

  @Autowired private CustomerRepository customerRepository;

  @Autowired private UserExtraFieldsRepository extraFieldsRepository;

  @Autowired private UserExtraFieldNameRepository extraFieldNameRepository;

  @Autowired private UserMandatoryFieldsRepository mandatoryFieldsRepository;
  @Autowired private UserShowFieldsRepository showFieldsRepository;

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;

  @Override
  public void sendSimpleMessage(String to, String subject, String text) throws TheMailException {
    // TODO Auto-generated method stub
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(to);
      message.setSubject(subject);
      message.setText(text);
      emailSender.send(message);
    } catch (MailException ex) {
      // Log the error or handle it in any appropriate way
      // You can throw a custom exception to provide more context
      System.out.println(
          "--------------------------------Mail Exception------------------------------------------------->");
      throw new TheMailException("Failed to send email: " + ex.getMessage(), ex);
    }
  }

  @Override
  public AuthenticationResponseDTO generateToken(Mail mail) {

    var token = jwtService.generateTokenForInvite(mail);
    System.out.println(
        "--------------------------------token created------------------------------------------------->"
            + token);
    return AuthenticationResponseDTO.builder().token(token).build();
  }

  @Override
  public Claims decodeDetails(String token) {
    // TODO Auto-generated method stub
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Jwts.parser()
        .setSigningKey(Keys.hmacShaKeyFor(keyBytes))
        .parseClaimsJws(token)
        .getBody();
  }

  @Override
  public List<UsersDTO> getAllUsers(Long companyId) {
    // TODO Auto-generated method stub
    List<Users> usersList = usersRepository.findByCompanyId(companyId);
    List<UsersDTO> usersListDTO = new ArrayList<>();
    usersList.stream()
        .forEach(
            (user) -> {
              UsersDTO usersDTO = modelMapper.map(user, UsersDTO.class);
              usersListDTO.add(usersDTO);
            });
    return usersListDTO;
  }

  @Override
  public void registerUser(Users user) throws UserException {
    // TODO Auto-generated method stub
    Optional<Users> OptionalUser =
        usersRepository.findByCompanyIdAndEmail(user.getCompanyId(), user.getEmail());
    Boolean exists = customerRepository.existsByEmail(user.getEmail());

    if ((!exists) && OptionalUser.isEmpty()) {
      usersRepository.save(user);
    } else {
      throw new UserException("User already Invited");
    }
  }

  @Override
  public UsersDTO getUsers(Long companyId, String email) {
    // TODO Auto-generated method stub
    Optional<Users> Optionaluser = usersRepository.findByCompanyIdAndEmail(companyId, email);
    UsersDTO usersDTO = modelMapper.map(Optionaluser.get(), UsersDTO.class);
    return usersDTO;
  }

  @Override
  public void updateUser(UsersDTO usersDTO) {
    Optional<Users> optionaluser =
        usersRepository.findByCompanyIdAndEmail(usersDTO.getCompanyId(), usersDTO.getEmail());
    Users users = modelMapper.map(usersDTO, Users.class);
    optionaluser.ifPresent(
        value -> {
          users.setId(value.getId());
          users.setEmail(value.getEmail());
        });
    usersRepository.save(users);
  }

  @Override
  public List<UsersDTO> getAllUsersByRole(String role, Long companyId) {
    // TODO Auto-generated method stub
    List<Users> usersList = usersRepository.findByCompanyId(companyId);
    List<UsersDTO> usersListDTO = new ArrayList<>();
    usersList.stream()
        .forEach(
            (user) -> {
              UsersDTO usersDTO = modelMapper.map(user, UsersDTO.class);
              usersListDTO.add(usersDTO);
            });
    System.out.println("~~~~~~~~~~~~~~~~>" + usersListDTO);
    return usersListDTO;
  }

  @Override
  public void deleteUser(Long companyId, String email, String authHeader) throws Exception {
    // TODO Auto-generated method stub
    this.headers.set("Authorization", authHeader);
    HttpEntity<String> requestEntity = new HttpEntity<>(headers);
    System.out.println(
        "/----------------------------/////>" + companyId + "  " + email + " " + authHeader);
    Optional<Users> OptionalUser = usersRepository.findByCompanyIdAndEmail(companyId, email);

    System.out.println("/------------new---------------/////>" + workOrderAPI + companyId);
    if (OptionalUser.isPresent()) {
      //			 ResponseEntity<Boolean> response = restTemplate.getForEntity(workOrderAPI,
      // Boolean.class);
      //			 ResponseEntity<Boolean> response =
      // restTemplate.exchange(workOrderAPI+companyId+"/"+OptionalUser.get().getId(),
      // HttpMethod.GET, requestEntity, Boolean.class);

      //			 Boolean b=true;
      //
      // System.out.println("............///////////////.............../////////////////////---->"+response.getBody()+"     "+response.getBody().compareTo(b)+"  "+(response.getBody()==b));
      //			if(response.getBody()==b) {
      //				throw new UserCannotDeletedException("WorkOrder is assigned to this user. Cannot delete
      // the user");
      //			}

      FirebaseAuth.getInstance();

      // Retrieve user record based on email
      try {
        UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
        // Get user ID
        String userId = userRecord.getUid();
        if (!userId.isEmpty()) {
          FirebaseAuth.getInstance().deleteUser(userId);
        }
      } catch (Exception e) {
        log.info("User Not Found in Firebase...");
      }

      usersRepository.delete(OptionalUser.get());

    } else {
      throw new Exception("Deletion failed. No such user");
    }
  }

  @Override
  public void addExtraFields(UserExtraFieldsDTO extraFieldsDTO) throws Exception {
    extraFieldsDTO.setName(extraFieldsDTO.getName());

    UserExtraFields extraFields = modelMapper.map(extraFieldsDTO, UserExtraFields.class);

    extraFieldsRepository.save(extraFields);
  }

  @Override
  public List<UserExtraFieldsDTO> getExtraFields(String id) {
    List<UserExtraFields> extraFieldsList = extraFieldsRepository.findByUserId(id);
    if (extraFieldsList.isEmpty()) {
      return null;
    }
    List<UserExtraFieldsDTO> extraFieldsDTOList = new ArrayList<>();
    extraFieldsList.stream()
        .forEach(
            (x) -> {
              UserExtraFieldsDTO extraFieldsDTO = modelMapper.map(x, UserExtraFieldsDTO.class);
              extraFieldsDTOList.add(extraFieldsDTO);
            });
    return extraFieldsDTOList;
  }

  @Override
  public void deleteExtraFields(String id) throws Exception {
    Optional<UserExtraFields> extraFields = extraFieldsRepository.findById(id);
    if (extraFields.isEmpty()) {
      throw new Exception("No such extra Field");
    }
    extraFieldsRepository.delete(extraFields.get());
  }

  @Override
  public List<UserExtraFieldNameDTO> getUserExtraField(Long companyId) {
    List<UserExtraFieldName> extraFieldNameList =
        extraFieldNameRepository.findByCompanyId(companyId);
    List<UserExtraFieldNameDTO> extraFieldNameListDTO = new ArrayList<>();
    extraFieldNameList.stream()
        .forEach(
            (x) -> {
              UserExtraFieldNameDTO extraFieldNameDTO =
                  modelMapper.map(x, UserExtraFieldNameDTO.class);
              extraFieldNameListDTO.add(extraFieldNameDTO);
            });
    return extraFieldNameListDTO;
  }

  @Override
  public void addUserExtraField(UserExtraFieldNameDTO extraFieldNameDTO)
      throws ExtraFieldAlreadyPresentException {
    UserExtraFieldName extraFieldNameNew =
        extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(
            extraFieldNameDTO.getName(), extraFieldNameDTO.getCompanyId());
    if (extraFieldNameNew != null) {
      throw new ExtraFieldAlreadyPresentException("Extra Field Already Present");
    }
    extraFieldNameDTO.setName(extraFieldNameDTO.getName());

    UserExtraFieldName extraFieldName =
        modelMapper.map(extraFieldNameDTO, UserExtraFieldName.class);
    extraFieldNameRepository.save(extraFieldName);
  }

  @Override
  public void deleteUserExtraField(String id) {
    Optional<UserExtraFieldName> extraFieldNameOptional = extraFieldNameRepository.findById(id);
    extraFieldNameRepository.deleteById(id);
    UserExtraFieldName extraFieldName = extraFieldNameOptional.get();
    List<UserExtraFields> extraFieldsList =
        extraFieldsRepository.findByNameIgnoreCase(extraFieldName.getName());
    extraFieldsList.stream()
        .forEach(
            (x) -> {
              // System.out.println("-------------------------------------->"+x.getName());
              extraFieldsRepository.delete(x);
            });
  }

  @Override
  public Map<String, Map<String, String>> getextraFieldList(Long companyId) {
    List<UserExtraFields> extraFieldNameList = extraFieldsRepository.findByCompanyId(companyId);
    List<Users> userList = usersRepository.findByCompanyId(companyId);
    Map<String, Map<String, String>> fieldNameValueMap = new HashMap<>();

    userList.stream()
        .forEach(
            (user) -> {
              Map<String, String> m = new HashMap<>();
              extraFieldNameList.stream()
                  .forEach(
                      (field) -> {
                        if (field.getUserId().endsWith(user.getId())) {
                          m.put(field.getName(), field.getValue());
                        }
                      });
              fieldNameValueMap.put(user.getId(), m);
            });
    return fieldNameValueMap;
  }

  @Override
  public void updateShowFields(UserShowFields showFields) {
    Optional<UserShowFields> showFieldsOptional =
        showFieldsRepository.findByNameIgnoreCaseAndCompanyId(
            showFields.getName(), showFields.getCompanyId());
    UserShowFields myShowFields = new UserShowFields();
    if (showFieldsOptional.isPresent()) {
      myShowFields = showFieldsOptional.get();
      showFields.setId(myShowFields.getId());
    }
    showFieldsRepository.save(showFields);
  }

  @Override
  public void updateMandatoryFields(UserMandatoryFields mandatoryFields) {
    Optional<UserMandatoryFields> mandatoryFieldsOptional =
        mandatoryFieldsRepository.findByNameIgnoreCaseAndCompanyId(
            mandatoryFields.getName(), mandatoryFields.getCompanyId());
    UserMandatoryFields myMandatoryFields = new UserMandatoryFields();
    if (mandatoryFieldsOptional.isPresent()) {
      myMandatoryFields = mandatoryFieldsOptional.get();
      mandatoryFields.setId(myMandatoryFields.getId());
    }
    mandatoryFieldsRepository.save(mandatoryFields);
  }

  @Override
  public UserShowFields getShowFields(String name, Long companyId) {
    Optional<UserShowFields> showFieldsOptional =
        showFieldsRepository.findByNameIgnoreCaseAndCompanyId(name, companyId);
    if (showFieldsOptional.isPresent()) {
      return showFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public UserMandatoryFields getMandatoryFields(String name, Long companyId) {
    Optional<UserMandatoryFields> mandatoryFieldsOptional =
        mandatoryFieldsRepository.findByNameIgnoreCaseAndCompanyId(name, companyId);
    if (mandatoryFieldsOptional.isPresent()) {
      return mandatoryFieldsOptional.get();
    } else {
      return null;
    }
  }

  @Override
  public List<UserShowFields> getAllShowFields(Long companyId) {
    List<UserShowFields> showFieldsList = showFieldsRepository.findByCompanyId(companyId);
    return showFieldsList;
  }

  @Override
  public List<UserMandatoryFields> getAllMandatoryFields(Long companyId) {
    List<UserMandatoryFields> mandatoryFieldsList =
        mandatoryFieldsRepository.findByCompanyId(companyId);
    return mandatoryFieldsList;
  }

  @Override
  public void deleteShowAndMandatoryFields(Long companyId, String name) {
    Optional<UserShowFields> showFieldsOptional =
        showFieldsRepository.findByNameIgnoreCaseAndCompanyId(name, companyId);
    showFieldsRepository.delete(showFieldsOptional.get());
    Optional<UserMandatoryFields> mandatoryFieldsOptional =
        mandatoryFieldsRepository.findByNameIgnoreCaseAndCompanyId(name, companyId);
    if (mandatoryFieldsOptional.isPresent()) {
      mandatoryFieldsRepository.delete(mandatoryFieldsOptional.get());
    }
  }
}
