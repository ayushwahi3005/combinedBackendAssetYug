package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.dto.UsersDTO;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.TheMailException;
import com.quantumai.customer.exception.UserCannotDeletedException;
import com.quantumai.customer.exception.UserException;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Service
public class UserServiceImpl implements UserService{
	
	@Autowired
	private JavaMailSender emailSender;
	@Autowired
	JwtService jwtService;
	
	@Autowired
	private PasswordEncoder passwordEncoder ;
	
	RestTemplate restTemplate=new RestTemplate();
	
	String workOrderAPI = "http://localhost:8083/workorder/getWorkOrderByTechnicianId/";
	
	 private HttpHeaders headers = new HttpHeaders();
	
	
	private ModelMapper modelMapper=new ModelMapper();

	@Autowired private UsersRepository usersRepository;

	@Autowired private CustomerRepository customerRepository;
	
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
	    	System.out.println("--------------------------------Mail Exception------------------------------------------------->");
	        throw new TheMailException("Failed to send email: " + ex.getMessage(), ex);
	    }
	}


	@Override
	public AuthenticationResponseDTO generateToken(Mail mail) {


		
		var token=jwtService.generateTokenForInvite(mail);
		System.out.println("--------------------------------token created------------------------------------------------->"+token);
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
	public List<UsersDTO> getAllUsers(String companyId) {
		// TODO Auto-generated method stub
		List<Users> usersList=usersRepository.findByCompanyId(companyId);
		List<UsersDTO> usersListDTO=new ArrayList<>();
		usersList.stream().forEach((user)->{
			UsersDTO usersDTO=modelMapper.map(user, UsersDTO.class);
			usersListDTO.add(usersDTO);
		});
		return usersListDTO;
	}


	@Override
	public void registerUser(Users user) throws UserException {
		// TODO Auto-generated method stub
		Optional<Users> OptionalUser=usersRepository.findByCompanyIdAndEmail(user.getCompanyId(), user.getEmail());
		Boolean exists=customerRepository.existsByEmail(user.getEmail());
		

		
		if((!exists)&&OptionalUser.isEmpty()) {
			usersRepository.save(user);
		}
		else {
			throw  new UserException("User already Invited");
		}
			
	
		
		
	}


	@Override
	public UsersDTO getUsers(String companyId, String email) {
		// TODO Auto-generated method stub
		Optional<Users> Optionaluser=usersRepository.findByCompanyIdAndEmail(companyId, email);
		UsersDTO usersDTO=modelMapper.map(Optionaluser.get(), UsersDTO.class);
		return usersDTO;
	}


	@Override
	public List<UsersDTO> getAllUsersByRole(String role,String companyId) {
		// TODO Auto-generated method stub
		List<Users> usersList=usersRepository.findByCompanyId(companyId);
		List<UsersDTO> usersListDTO=new ArrayList<>();
		usersList.stream().forEach((user)->{
			UsersDTO usersDTO=modelMapper.map(user, UsersDTO.class);
			usersListDTO.add(usersDTO);
		});
		return usersListDTO;
	}


	@Override
	public void deleteUser(String companyId, String email,String authHeader) throws Exception {
		// TODO Auto-generated method stub
		this.headers.set("Authorization", authHeader);
		HttpEntity<String> requestEntity = new HttpEntity<>(headers);
		System.out.println("/----------------------------/////>"+companyId+"  "+email+" "+authHeader);
		Optional<Users> OptionalUser=usersRepository.findByCompanyIdAndEmail(companyId, email);
		
		System.out.println("/------------new---------------/////>"+workOrderAPI+companyId);
		if(OptionalUser.isPresent()) {
//			 ResponseEntity<Boolean> response = restTemplate.getForEntity(workOrderAPI, Boolean.class);
//			 ResponseEntity<Boolean> response = restTemplate.exchange(workOrderAPI+companyId+"/"+OptionalUser.get().getId(), HttpMethod.GET, requestEntity, Boolean.class);
			
//			 Boolean b=true;
//			 System.out.println("............///////////////.............../////////////////////---->"+response.getBody()+"     "+response.getBody().compareTo(b)+"  "+(response.getBody()==b));
//			if(response.getBody()==b) {
//				throw new UserCannotDeletedException("WorkOrder is assigned to this user. Cannot delete the user");
//			}

				FirebaseAuth.getInstance();

		        // Retrieve user record based on email
		        UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
		        
		        // Get user ID
		        String userId = userRecord.getUid();
				 FirebaseAuth.getInstance().deleteUser(userId);	
			usersRepository.delete(OptionalUser.get());
			
			

			
		}
		else {
			throw new Exception("Deletion failed. No such user");
		}
		
		
	}






	



}
