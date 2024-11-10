package com.quantumai.customer.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
@Document
public class Customer implements UserDetails {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5030992221287361876L;
	
	@Id
	private String id;
	private String firstName;
	private String lastName;
	private String email;
	private String companyName;
	private String companyId;
	private String mobileNumber;
	private String password;
	
	
//	@Enumerated
	private String role;
	

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// TODO Auto-generated method stub
		return List.of(new SimpleGrantedAuthority(role));
	
	}
	@Override
	public String getPassword() {
		// TODO Auto-generated method stub
		return password;
	}
	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return email;
	}
	@Override
	public boolean isAccountNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isAccountNonLocked() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isCredentialsNonExpired() {
		// TODO Auto-generated method stub
		return true;
	}
	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}
	
	
}
