package com.quantumai.customer.service;

import com.google.firebase.auth.FirebaseAuthException;

import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.dto.UsersDTO;
import com.quantumai.customer.entity.Mail;
import com.quantumai.customer.entity.Users;
import com.quantumai.customer.exception.UserCannotDeletedException;
import com.quantumai.customer.exception.UserException;
import io.jsonwebtoken.Claims;

import java.util.List;

public interface UserService {
	
	
	public void sendSimpleMessage(String to, String subject, String text);
	public AuthenticationResponseDTO generateToken(Mail mail);
	public Claims  decodeDetails(String token);
	public List<UsersDTO> getAllUsers(String companyId);
	public void registerUser(Users user) throws UserException;
	public UsersDTO getUsers(String companyId,String email);
	public List<UsersDTO> getAllUsersByRole(String role,String companyId);
	public void deleteUser(String companyId,String email,String authHeader) throws  FirebaseAuthException, UserCannotDeletedException, Exception;
//	public UserDTO resendMail(String email,String companyId);

}
