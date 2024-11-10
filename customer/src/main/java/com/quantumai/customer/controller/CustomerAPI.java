package com.quantumai.customer.controller;

import java.lang.System.Logger;
import java.util.ArrayList;
import java.util.List;

import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.Bin;
import com.quantumai.customer.entity.ImportHistory;
import com.quantumai.customer.entity.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import com.quantumai.customer.entity.CompanyInformation;
import com.quantumai.customer.service.CustomerService;

@CrossOrigin(origins = "**")
@RestController
@RequestMapping("/customer")
public class CustomerAPI {
	
	@Autowired
	private CustomerService customerService;

	
	@GetMapping(value="/working")
	public ResponseEntity<String> working() throws Exception {
		
		return ResponseEntity.ok("Working OK");
	}
	
	@PostMapping(value="/addCustomer")
	public ResponseEntity<BaseResponseDTO> addCustomer(@RequestBody CustomerDTO customerDTO) throws Exception{
		
		
		return  ResponseEntity.ok(customerService.addCustomer(customerDTO));
	}
	@PostMapping(value="/authenticate")
	public ResponseEntity<AuthenticationResponseDTO> authenticate(@RequestBody AuthenticationRequestDTO authenticationRequestDTO) throws Exception{
		
		
		
		return ResponseEntity.ok(customerService.authenticate(authenticationRequestDTO));
	}
	@GetMapping(value="/getLoginToken/{email}")
	public ResponseEntity<AuthenticationResponseDTO> getLoginToken(@PathVariable String email) throws Exception {
		
		return ResponseEntity.ok(customerService.getLoginToken(email));
	}

	@GetMapping(value="/get/{email}")
	public CustomerDTO getCustomer(@PathVariable String email) throws Exception {
		
		return customerService.getCustomer(email);
	}
	@GetMapping(value="/getsubscription/{email}")
	public CustomerSubscribedDTO getCustomerSubscribed(@PathVariable String email ) throws Exception {
		return customerService.getCustomerSubscription(email);
	}
	@PostMapping(value="/addCompanyInformation")
	public ResponseEntity<CompanyInformation> addCompanyInformation(@RequestBody CompanyInformation companyInformation) throws Exception{
		customerService.addCompanyInformation(companyInformation);
		
		
		return ResponseEntity.ok(companyInformation);
	}
	@PostMapping(value="/updateCompanyInformation")
	public ResponseEntity<CompanyInformation> updateCompanyInformation(@RequestBody CompanyInformation companyInformation) throws Exception{
		customerService.addCompanyInformation(companyInformation);
		
		
		return ResponseEntity.ok(companyInformation);
	}
	
	@GetMapping(value="/getCompanyInformation/{companyId}")
	public ResponseEntity<CompanyInformation> getCompanyInformation(@PathVariable String companyId ) throws Exception {
		return ResponseEntity.ok(customerService.getcompanyInformation(companyId));
	}
	@GetMapping(value="/getCompanyId/{email}")
	public ResponseEntity<CompanyIdDTO> getCompanyId(@PathVariable String email ) throws Exception {
		return ResponseEntity.ok(customerService.getCompanyId(email));
	}
	@PostMapping(value="/addUser")
	public ResponseEntity<BaseResponseDTO> addUser(@RequestBody CustomerDTO customerDTO) throws Exception{
		
		
		return  ResponseEntity.ok(customerService.addUsers(customerDTO));
	}
	@GetMapping(value="/getRegisteredUsers/{companyId}")
	public ResponseEntity<List<String>> getRegisteredUsers(@PathVariable String companyId ) throws Exception {
		return ResponseEntity.ok(customerService.activeUsers(companyId));
	}
	
	@GetMapping(value="/accountInfo/{customerEmail}")
	public ResponseEntity<AccountLockInfoDTO> getAccountInfo(@PathVariable String customerEmail ) throws Exception {
		return ResponseEntity.ok(customerService.getAccountInfo(customerEmail));
	}
	@PostMapping(value="/accountInfo/update")
	public void addUser(@RequestBody AccountLockInfoDTO AccountLockInfoDTO) throws Exception{
		
		
		customerService.updateAccountInfo(AccountLockInfoDTO);
	}
	@DeleteMapping(value="/deleteAccount/{companyId}/{email}")
	public void deleteUser(@PathVariable String companyId,@PathVariable String email) throws Exception{
		
		
		customerService.deleteUser(companyId, email);
	}
	@PostMapping(value="/roleAndPermission/add")
	public void addRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {

		
		customerService.addRoleAndPermission(customRoleDTO);
		
	}
	@PutMapping(value="/roleAndPermission/update")
	public void updateRoleAndPermission(@RequestBody CustomRoleDTO customRoleDTO) {
		
		
		customerService.addRoleAndPermission(customRoleDTO);
		
	}
	@GetMapping(value="/roleAndPermission/get/{companyId}")
	public ResponseEntity<List<CustomRoleDTO>> getRoleAndPermission(@PathVariable String  companyId) throws Exception {
		
		
		return ResponseEntity.ok(customerService.getRoleAndPermission(companyId)) ;
		
	}
	@DeleteMapping(value="/roleAndPermission/delete/{id}")
	public void deleteRoleAndPermission(@PathVariable String  id) throws Exception {
		
		
		customerService.deleteRoleAndPermission(id);
		
	}
	@GetMapping(value="/countByRole/{companyId}/{roleName}")
	public ResponseEntity<Long> countByRole(@PathVariable String  companyId,@PathVariable String  roleName) throws Exception {
		Long count=customerService.countByRoleName(roleName,companyId);
		System.out.println("count->"+count);
		return ResponseEntity.ok(count) ;
		
	}
	@GetMapping(value="/roleAndPermissionByName/get/{companyId}/{name}")
	public ResponseEntity<CustomRoleDTO> roleAndPermissionByName(@PathVariable String  companyId,@PathVariable String  name) throws Exception {
		
		
		return ResponseEntity.ok(customerService.roleAndPermissionByName(companyId,name)) ;
		
	}
	@PostMapping(value = "/addlocation")
	public ResponseEntity<Location> addLocation(@RequestBody Location location){
		return ResponseEntity.ok(customerService.addLocation(location));
	}

	@GetMapping(value = "/getAllLocation/{companyId}")
	public ResponseEntity<List<Location>> getAllLocation(@PathVariable String companyId){
		return ResponseEntity.ok(customerService.getAllLocation(companyId));
	}
	@DeleteMapping(value = "/deleteLocation/{id}")
	public void deleteLocation(@PathVariable String id){
		 customerService.deleteLocation(id);
	}
//-------------------------------------------------------
	@PostMapping(value = "/addbin")
	public ResponseEntity<Bin> addBin(@RequestBody Bin bin){
		return ResponseEntity.ok(customerService.addBin(bin));
	}

	@GetMapping(value = "/getAllBin/{companyId}")
	public ResponseEntity<List<Bin>> getAllBin(@PathVariable String companyId){
		return ResponseEntity.ok(customerService.getAllBin(companyId));
	}
	@DeleteMapping(value = "/deleteBin/{id}")
	public void deleteBin(@PathVariable String id){
		customerService.deleteBin(id);
	}

	@PostMapping(value = "/addImportHistory")
	public void addImportHistory(@RequestBody ImportHistory importHistory){
		customerService.addImportHistory(importHistory);
	}

	@GetMapping(value = "/getAllImportHistory/{companyId}")
	public Page<ImportHistoryDTO> getImportHistory(
			@PathVariable  String companyId,
			@RequestParam (defaultValue = "0", required = false) int pageNumber,
			@RequestParam (defaultValue = "10",required = false) int pageSize) {

//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			e.printStackTrace(); // Handle the exception if needed
//		}

		return customerService.getImportHistoryList(companyId, pageNumber, pageSize);
	}

	@PutMapping(value = "/updateImportHistory")
	public void updateImportHistory(@RequestBody ImportHistory importHistory){
		customerService.updateImportHistory(importHistory);
	}








}
