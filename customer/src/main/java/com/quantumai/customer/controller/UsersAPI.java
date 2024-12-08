package com.quantumai.customer.controller;

;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.dto.CustomerDTO;
import com.quantumai.customer.dto.UsersDTO;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.exception.UserException;
import com.quantumai.customer.repository.CustomRoleRepository;
import com.quantumai.customer.repository.CustomerRepository;
import com.quantumai.customer.repository.UsersRepository;
import com.quantumai.customer.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "**")
public class UsersAPI {
private RestTemplate restTemplate=new RestTemplate();
	
//	String customerApi="http://localhost:8080/customer/get/";

	@Value("${application.security.jwt.secret-key}")
	private String secretKey;
	private ModelMapper modelMapper=new ModelMapper();
	
	@Autowired
    private UserService userService;
	
//	@Autowired
//	 private PasswordEncoder passwordEncoder;
	
	@Autowired private UsersRepository usersRepository;
	@Autowired private CustomerRepository customerRepository;

	@Autowired private CustomRoleRepository customRoleRepository;
	
	@PostMapping("/registerUser")
	public void register(@RequestBody Users users) throws UserException {
		
	
		
		userService.registerUser(users);
		
	}
	

	@PostMapping("/send/{companyId}")
    public void sendEmail(@PathVariable String companyId,@RequestBody Mail mail) {
		AuthenticationResponseDTO response=userService.generateToken(mail);
		System.out.print("---------------------///////--------------------////-----------------"+response.getToken());
		userService.sendSimpleMessage(mail.getEmail(), "Invitation mail", mail.getMessage()+" http://localhost:4200/invitation/"+companyId+"/"+response.getToken());
    }
//	@GetMapping(value="/getCustomer/{companyId}")
//	public ResponseEntity<Customer> getCustomerDetails(@PathVariable String companyId, @RequestHeader("Authorization") String token){
//
//		HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", token);
//        HttpEntity<Customer> entity = new HttpEntity<>(headers);
//
//		ResponseEntity<CustomerDTO> response = restTemplate.exchange(
//				customerApi+companyId,
//                HttpMethod.GET,
//                entity,
//                CustomerDTO.class
//        );
//		Customer customer=modelMapper.map(response.getBody(), Customer.class);
//        return ResponseEntity.ok(customer);
//
//	}
	@PostMapping(value="/invite")
	public void getInviteDetails(@RequestBody UsersDTO userDTO) throws UserException{
//		Claims myDetails=userService.decodeDetails(details);
		System.out.println(userDTO);
		Users user=modelMapper.map(userDTO, Users.class);
//		user.setPassword(passwordEncoder.encode(user.getPassword()));
//		user.setCompanyId(companyId);
//		if(myDetails.get("role").equals("ADMIN")) {
//			user.setRole(Role.ADMIN);
//		}s
//		else if(myDetails.get("role").equals("TECHNICAL")) {
//			user.setRole(Role.TECHNICAL);
//		}
		userService.registerUser(user);
	
	}
	
	@GetMapping(value="/getUsers/{companyId}")
	public ResponseEntity<List<UsersDTO>> getAllUsers(@PathVariable String companyId){
		List<UsersDTO> userList=userService.getAllUsers(companyId);
		return ResponseEntity.ok(userList);
	
	}
	@GetMapping(value="/invite/getUser/{companyId}/{details}")
	public ResponseEntity<UsersDTO> getUsers(@PathVariable String companyId,@PathVariable String details){
		Claims myDetails=userService.decodeDetails(details);
		UsersDTO user=userService.getUsers(companyId,myDetails.get("email").toString());
		return ResponseEntity.ok(user);
	
	}
	@GetMapping(value="/getTechnicalUser/{companyId}")
	public ResponseEntity<List<UsersDTO>> getTechnicalUser(@PathVariable String companyId){
		
		List<UsersDTO> userDTOList=userService.getAllUsersByRole("TECHNICAL", companyId);
		return ResponseEntity.ok(userDTOList);
	
	}
	@GetMapping(value="/getUserDetails/{companyId}/{email}")
	public ResponseEntity<UsersDTO> getUserDetails(@PathVariable String companyId,@PathVariable String email){
		
		UsersDTO UserDTO=userService.getUsers(companyId, email);
		return ResponseEntity.ok(UserDTO);
	
	}
	@PostMapping(value="/updateUserDetails")
	public void updateUserDetails(@RequestBody UsersDTO usersDTO, HttpServletRequest request) throws Exception {
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Invalid or missing Authorization header");
		}

		// Extract token (remove "Bearer " prefix)
		String token = authorizationHeader.substring(7);

		// Decode token to extract claims
		Claims claims = Jwts.parser()
				.setSigningKey(secretKey) // Replace with your actual secret key
				.parseClaimsJws(token)
				.getBody();

		// Extract email from claims
		String email = claims.getSubject(); // Assuming email is set as the 'sub' (subject) claim

		// Log or use the email
		System.out.println("Extracted Email: " + email);
		Optional<Customer> myUsers=customerRepository.findByEmail(email);
		if(myUsers.isPresent()){
			String role=myUsers.get().getRole();
			System.out.println("Role->"+role);
			if(role.toLowerCase().equals("admin")){
				userService.updateUser(usersDTO);
			}
			else {
				Optional<CustomRole> optionalCustomRole = customRoleRepository.findByNameAndCompanyId(role, myUsers.get().getCompanyId());
				if (optionalCustomRole.isPresent()) {
					CustomRole customRole = optionalCustomRole.get();
					if (customRole.getUsers().equals(CustomRoleType.full) || customRole.getUsers().equals(CustomRoleType.edit)) {
						System.out.println(usersDTO);
						userService.updateUser(usersDTO);
					} else {
						throw new Exception("Not Authorized");
					}

				} else {
					throw new Exception("Unkown Role");
				}
			}

		}
		else{
			throw new Exception("Unkown Email");
		}



	}
	@DeleteMapping(value="/deleteUserDetails/{companyId}/{email}")
	public void deleteUserDetails(@PathVariable String companyId,@PathVariable String email,@RequestHeader("Authorization") String authHeader) throws Exception{
//		System.out.print("-,,,,---,,,,---"+authHeader);
		userService.deleteUser(companyId, email, authHeader);
		
	
	}
	
	
}
