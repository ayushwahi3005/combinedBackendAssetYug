package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.IdGenerator.UserIdGenerator;
import com.quantumai.customer.exception.*;
import com.quantumai.customer.repository.*;
import com.quantumai.customer.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.quantumai.customer.entity.StatusEnum;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserActivationService userActivationService;


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

    @Autowired private UserIdGeneratorRepository userIdGeneratorRepository;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Override
    public void resendFirebaseVerificationEmail(String email, Long companyId) throws UserException, TheMailException, FirebaseAuthException, UserEmailAlreadyVerifiedException {
        log.info("Resending Firebase verification email to: {} for company: {}", email, companyId);
        
        // Find user by email and company ID
        Optional<Users> userOpt = usersRepository.findByCompanyIdAndEmail(companyId, email);
        if (userOpt.isEmpty()) {
            log.warn("User not found with email: {} and companyId: {}", email, companyId);
            throw new UserException("User not found with the provided email and company");
        }
        
        Users user = userOpt.get();
        UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
        // Check if user is already active
        if (userRecord.isEmailVerified()) {
            log.warn("User {} is already active, no need to resend verification", email);
            throw new UserEmailAlreadyVerifiedException("This email is already verified and active");
        }
        
        try {
            // Generate Firebase email verification link
            String verificationLink = FirebaseAuth.getInstance().generateEmailVerificationLink(email);
            
            // Customize the email content
            String subject = "Verify Your Email - Action Required";
            String message = String.format(
                "Hello %s,\n\n" +
                "Please verify your email address by clicking the link below to complete your registration.\n\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\nThe Team",
                email.split("@")[0], // Use the part before @ as the name
                verificationLink
            );
            
            // Send the verification email
            sendSimpleMessage(email, subject, message);
            log.info("Firebase verification email sent successfully to: {}", email);

        } catch (FirebaseAuthException e) {
            log.error("Failed to generate verification link", e);
            throw new RuntimeException("Failed to generate verification link", e);

        } catch (MailException e) {
            log.error("Failed to send email", e);
            throw new TheMailException(
                    "Failed to send verification email.",
                    e
            );}
    }
    
    @Override
    public void resendVerificationEmail(String email, Long companyId) throws UserException, TheMailException {
        Optional<Users> userOpt = usersRepository.findByCompanyIdAndEmail(companyId, email);
        if (userOpt.isEmpty()) {
            throw new UserException("User not found");
        }
        Users user = userOpt.get();
        if (user.getStatus() != UserStatusEnum.inActive) {
            throw new UserException("User is already verified or active");
        }
        // Trigger Firebase to send the verification email
        try {
            // Firebase user lookup (optional, can be used for validation)
            String link = FirebaseAuth.getInstance().generateEmailVerificationLink(user.getEmail());
            // Optionally, send this link via your backend email for customization, but Firebase will send if configured
            // For now, send using backend for audit/logging
            sendSimpleMessage(user.getEmail(), "Verify your email", "Please verify your email using this link: " + link);
        } catch (Exception e) {
            throw new UserException("Failed to trigger Firebase verification email: " + e.getMessage());
        }
    }


    private static final String INVITATION_EMAIL_SUBJECT = "You're invited to join AssetYug";
    private static final int INVITATION_EXPIRY_DAYS = 7;

    @Override
    public void sendInvitationEmail(Mail mail, String invitationLink, String organizationName)
            throws TheMailException {
        String firstName = resolveDisplayFirstName(mail.getFirstName(), mail.getEmail());
        String invitedBy = mail.getFrom() != null && !mail.getFrom().trim().isEmpty()
                ? mail.getFrom().trim()
                : "your organization administrator";
        String roleName = resolveRoleName(mail);
        String orgName = organizationName != null && !organizationName.trim().isEmpty()
                ? organizationName.trim()
                : "your organization";
        String expirationDate = LocalDateTime.now().plusDays(INVITATION_EXPIRY_DAYS)
                .format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(mail.getEmail());
            helper.setSubject(INVITATION_EMAIL_SUBJECT);
            helper.setText(
                    buildInvitationEmailPlainText(
                            firstName, invitedBy, orgName, roleName, mail.getEmail(), invitationLink, expirationDate),
                    buildInvitationEmailHtml(
                            firstName, invitedBy, orgName, roleName, mail.getEmail(), invitationLink, expirationDate));
            emailSender.send(message);
        } catch (Exception ex) {
            MailException mailException = ex instanceof MailException
                    ? (MailException) ex
                    : new org.springframework.mail.MailSendException("Failed to send email: " + ex.getMessage(), ex);
            throw new TheMailException("Failed to send email: " + ex.getMessage(), mailException);
        }
    }

    private String resolveRoleName(Mail mail) {
        if (mail.getRole() != null && mail.getRole().getName() != null && !mail.getRole().getName().isBlank()) {
            return mail.getRole().getName();
        }
        return "Member";
    }

    private String resolveDisplayFirstName(String firstName, String email) {
        if (firstName != null && !firstName.trim().isEmpty()) {
            return firstName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "there";
    }

    private String buildInvitationEmailPlainText(
            String firstName,
            String invitedBy,
            String organizationName,
            String roleName,
            String email,
            String invitationLink,
            String expirationDate) {
        return String.format(
                "Hello %s,%n%n"
                        + "You've been invited by %s to join %s on AssetYug.%n%n"
                        + "AssetYug is an intelligent asset management platform that helps organizations manage assets, "
                        + "streamline inspections, monitor asset status, and maintain complete audit visibility from a single platform.%n%n"
                        + "Your Account Details%n"
                        + "Organization: %s%n"
                        + "Role: %s%n"
                        + "Email: %s%n%n"
                        + "To activate your account and create your password, click the link below.%n%n"
                        + "Accept Invitation:%n"
                        + "%s%n%n"
                        + "This invitation will expire on %s.%n%n"
                        + "If the link above doesn't work, copy and paste it into your browser:%n"
                        + "%s%n%n"
                        + "If you weren't expecting this invitation, you can safely ignore this email. "
                        + "No account will be created unless you accept the invitation.%n%n"
                        + "If you need assistance, contact your organization administrator or reply to this email.%n%n"
                        + "Welcome to AssetYug!%n%n"
                        + "Best regards,%n"
                        + "The AssetYug Team",
                firstName,
                invitedBy,
                organizationName,
                organizationName,
                roleName,
                email,
                invitationLink,
                expirationDate,
                invitationLink);
    }

    private String buildInvitationEmailHtml(
            String firstName,
            String invitedBy,
            String organizationName,
            String roleName,
            String email,
            String invitationLink,
            String expirationDate) {
        return String.format(
                """
                <html>
                <body style="font-family: Arial, sans-serif; color: #333333; line-height: 1.6;">
                  <p>Hello %s,</p>
                  <p>You've been invited by <strong>%s</strong> to join <strong>%s</strong> on <strong>AssetYug</strong>.</p>
                  <p>AssetYug is an intelligent asset management platform that helps organizations manage assets, streamline inspections, monitor asset status, and maintain complete audit visibility from a single platform.</p>
                  <h3 style="margin-bottom: 8px;">Your Account Details</h3>
                  <ul style="padding-left: 20px;">
                    <li><strong>Organization:</strong> %s</li>
                    <li><strong>Role:</strong> %s</li>
                    <li><strong>Email:</strong> %s</li>
                  </ul>
                  <p>To activate your account and create your password, click the button below.</p>
                  <p style="margin: 24px 0;">
                    <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; display: inline-block; font-weight: bold;">Accept Invitation</a>
                  </p>
                  <p>This invitation will expire on <strong>%s</strong>.</p>
                  <p>If the button above doesn't work, copy and paste the following link into your browser:</p>
                  <p><a href="%s">%s</a></p>
                  <p>If you weren't expecting this invitation, you can safely ignore this email. No account will be created unless you accept the invitation.</p>
                  <p>If you need assistance, contact your organization administrator or reply to this email.</p>
                  <p>Welcome to AssetYug!</p>
                  <p>Best regards,<br>The AssetYug Team</p>
                </body>
                </html>
                """,
                firstName,
                invitedBy,
                organizationName,
                organizationName,
                roleName,
                email,
                invitationLink,
                expirationDate,
                invitationLink,
                invitationLink);
    }

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
//      System.out.println(
//          "--------------------------------Mail Exception------------------------------------------------->");
      throw new TheMailException("Failed to send email: " + ex.getMessage(), ex);
    }
  }

  @Override
  public AuthenticationResponseDTO generateToken(Mail mail) {

    var token = jwtService.generateTokenForInvite(mail);
//    System.out.println(
//        "--------------------------------token created------------------------------------------------->"
//            + token);
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
    public List<UsersDTO> getAllActiveUsers(Long companyId) {
        List<Users> usersList = usersRepository.findByCompanyId(companyId);
        List<UsersDTO> usersListDTO = new ArrayList<>();
        usersList.stream().filter(users -> users.getStatus() == UserStatusEnum.active)
                .forEach(
                        (user) -> {
                            UsersDTO usersDTO = modelMapper.map(user, UsersDTO.class);
                            usersListDTO.add(usersDTO);
                        });
        return usersListDTO;
    }

    @Override
  @Transactional
  public Users registerUser(Users user) throws UserException {
    // Check if user exists and is not already active
    Optional<Users> OptionalUser = usersRepository.findByCompanyIdAndEmail(user.getCompanyId(), user.getEmail());
    if (OptionalUser.isPresent() && OptionalUser.get().getStatus() == UserStatusEnum.active) {
      throw new UserException("User with this email is already registered and active");
    }
    Boolean exists = customerRepository.existsByEmail(user.getEmail());

    if ((!exists) && OptionalUser.isEmpty()) {
        userIdGeneratorRepository.findByCompanyId(user.getCompanyId()).ifPresentOrElse((data)->{
            Long seq= data.getSeq();
            user.setUserId(seq);
            data.setSeq(seq+1);
            userIdGeneratorRepository.save(data);
        }, ()->{
            user.setUserId(1L);
            UserIdGenerator userIdGenerator=new UserIdGenerator();
            userIdGenerator.setSeq(2L);
            userIdGenerator.setCompanyId(user.getCompanyId());
            userIdGeneratorRepository.save(userIdGenerator);
                });
      return usersRepository.save(user);
    } else {
      throw new UserException("User already Invited");
    }
  }

  @Override
  public UsersDTO getUsers(Long companyId, String email) throws UserException {
    // TODO Auto-generated method stub
    Optional<Users> Optionaluser = usersRepository.findByCompanyIdAndEmail(companyId, email);
    if (Optionaluser.isEmpty()) {
      throw new UserException("User not found");
    }
    Users user = Optionaluser.get();
    // If user is already active, the token should be considered used
//    if (user.getStatus() == StatusEnum.active) {
//      throw new UserException("This invitation link has already been used");
//    }
    UsersDTO usersDTO = modelMapper.map(user, UsersDTO.class);
    return usersDTO;
  }

  @Override
  public UsersDTO getUserForInvite(Long companyId, String email) throws UserException {
    Optional<Users> Optionaluser = usersRepository.findByCompanyIdAndEmail(companyId, email);
    if (Optionaluser.isEmpty()) {
      throw new UserException("User not found");
    }
    Users user = Optionaluser.get();
    // If user is already active, the token should be considered used
    if (user.getStatus() == UserStatusEnum.active) {
      throw new UserException("This invitation link has already been used");
    }
    UsersDTO usersDTO = modelMapper.map(user, UsersDTO.class);
    return usersDTO;
  }

  @Override
  @Transactional
  public void updateUser(UsersDTO usersDTO) throws UserException {
    Optional<Users> optionaluser =
        usersRepository.findByCompanyIdAndEmail(usersDTO.getCompanyId(), usersDTO.getEmail());
    
    if (optionaluser.isEmpty()) {
      throw new UserException("User not found");
    }
    
    Users existingUser = optionaluser.get();
    Long userId=existingUser.getUserId();
    
    // If this is a password update (activating the account)
    if (usersDTO.getPassword() != null && !usersDTO.getPassword().isEmpty()) {
      // Encode the new password
      usersDTO.setPassword(passwordEncoder.encode(usersDTO.getPassword()));
      // Set status to active when password is set
      usersDTO.setStatus(UserStatusEnum.active);
      // The token used for signup is now invalidated by the status change
    }

    // Map non-null fields from DTO to existing user

    modelMapper.map(usersDTO, existingUser);
    existingUser.setUserId(userId);
    usersRepository.save(existingUser);
  }

    @Override
    public void updateUserStatus(UsersDTO usersDTO) throws UserCannotActivateException,UserException {
        Optional<Users> optionaluser =
                usersRepository.findByCompanyIdAndEmail(usersDTO.getCompanyId(), usersDTO.getEmail());

        if (optionaluser.isEmpty()) {
            throw new UserException("User not found");
        }

        Users existingUser = optionaluser.get();
        Long userId=existingUser.getUserId();
        log.info("Existing user details {},New Details {}",existingUser.toString(),usersDTO.toString());
        // If this is a password update (activating the account)
        if ((( usersDTO.getStatus().equals(UserStatusEnum.inActive)))||(existingUser.getStatus() .equals( UserStatusEnum.inActive) && usersDTO.getStatus().equals(UserStatusEnum.active)&& userActivationService.canActivateNewUser(existingUser.getCompanyId()))) {
            // Encode the new password
//            usersDTO.setPassword(passwordEncoder.encode(usersDTO.getPassword()));
            // Set status to active when password is set
//            usersDTO.setStatus(StatusEnum.active);
            modelMapper.map(usersDTO, existingUser);
            existingUser.setUserId(userId);
            usersRepository.save(existingUser);
            // The token used for signup is now invalidated by the status change
        }
        else{
            throw new UserCannotActivateException("User Cannot be Activated");
        }

        // Map non-null fields from DTO to existing user

    }

    @Override
  public List<UsersDTO> getAllUsersByRole(String role, Long companyId) {
    // TODO Auto-generated method stub
    List<Users> usersList = usersRepository.findByCompanyId(companyId);
    List<UsersDTO> usersListDTO = new ArrayList<>();
    usersList.stream().filter((user)->user.getStatus()==UserStatusEnum.active)
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

    @Override
    public void updateLastLogin(String email,Long companyId) {
        Optional<Users> optionalUser=usersRepository.findByCompanyIdAndEmail(companyId,email);
        optionalUser.ifPresent((data)->{
            data.setLastLogin(LocalDateTime.now());
            usersRepository.save(data);
        });

    }
}
