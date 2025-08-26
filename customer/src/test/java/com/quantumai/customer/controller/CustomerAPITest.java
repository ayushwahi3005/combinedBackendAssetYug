package com.quantumai.customer.controller;

import com.quantumai.customer.dto.CustomerDTO;
import com.quantumai.customer.dto.AuthenticationRequestDTO;
import com.quantumai.customer.dto.AuthenticationResponseDTO;
import com.quantumai.customer.dto.BaseResponseDTO;
import com.quantumai.customer.exception.UserAlreadyPresentException;
import com.quantumai.customer.service.CustomerService;
import com.quantumai.customer.service.TrialService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerAPITest {

    @Mock
    private CustomerService customerService;

    @Mock
    private TrialService trialService;

    @InjectMocks
    private CustomerAPI customerAPI;

    private MockMvc mockMvc;
    private CustomerDTO testCustomerDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerAPI).build();
        
        testCustomerDTO = new CustomerDTO();
        testCustomerDTO.setId("test-id");
        testCustomerDTO.setFirstName("Test");
        testCustomerDTO.setLastName("Customer");
        testCustomerDTO.setEmail("test@example.com");
        testCustomerDTO.setCompanyName("Test Company");
        testCustomerDTO.setMobileNumber("1234567890");
        testCustomerDTO.setPassword("password123");
    }

    @Test
    void testWorking() throws Exception {
        mockMvc.perform(get("/customer/working"))
                .andExpect(status().isOk())
                .andExpect(content().string("Working OK"));
    }

    @Test
    void testAddCustomer_Success() throws Exception {
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage("Customer added successfully");
        when(customerService.addCustomer(any(CustomerDTO.class))).thenReturn(baseResponse);

        ResponseEntity<BaseResponseDTO> result = customerAPI.addCustomer(testCustomerDTO);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Customer added successfully", result.getBody().getMessage());
        verify(customerService).addCustomer(testCustomerDTO);
    }

    @Test
    void testAddCustomer_EmailAlreadyExists() throws Exception {
        when(customerService.addCustomer(any(CustomerDTO.class)))
                .thenThrow(new UserAlreadyPresentException("Email already exists"));

        assertThrows(UserAlreadyPresentException.class, () -> {
            customerAPI.addCustomer(testCustomerDTO);
        });
    }

    @Test
    void testGetCustomer_Success() throws Exception {
        String email = "test@example.com";
        when(customerService.getCustomer(email)).thenReturn(testCustomerDTO);

        CustomerDTO result = customerAPI.getCustomer(email);

        assertNotNull(result);
        assertEquals(testCustomerDTO.getFirstName(), result.getFirstName());
        assertEquals(testCustomerDTO.getEmail(), result.getEmail());
        verify(customerService).getCustomer(email);
    }

    @Test
    void testCheckUserName_Success() throws Exception {
        String email = "test@example.com";
        when(customerService.checkCustomer(email)).thenReturn(true);

        ResponseEntity<Boolean> result = customerAPI.checkUserName(email);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody());
        verify(customerService).checkCustomer(email);
    }

    @Test
    void testAddUser_Success() throws Exception {
        BaseResponseDTO baseResponse = new BaseResponseDTO();
        baseResponse.setMessage("User added successfully");
        when(customerService.addUsers(any(CustomerDTO.class))).thenReturn(baseResponse);

        ResponseEntity<BaseResponseDTO> result = customerAPI.addUser(testCustomerDTO);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("User added successfully", result.getBody().getMessage());
        verify(customerService).addUsers(testCustomerDTO);
    }

    @Test
    void testAuthenticate_Success() throws Exception {
        AuthenticationRequestDTO authRequest = new AuthenticationRequestDTO();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password");
        String deviceId = "device123";

        AuthenticationResponseDTO authResponse = AuthenticationResponseDTO.builder()
                .token("jwt-token")
                .role("USER")
                .build();

        when(customerService.authenticate(any(AuthenticationRequestDTO.class), anyString()))
                .thenReturn(authResponse);

        ResponseEntity<AuthenticationResponseDTO> result = customerAPI.authenticate(authRequest, deviceId);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("jwt-token", result.getBody().getToken());
        assertEquals("USER", result.getBody().getRole());
        verify(customerService).authenticate(authRequest, deviceId);
    }

    @Test
    void testSentResetOTP_Success() throws Exception {
        String email = "test@example.com";
        doNothing().when(customerService).sentResetOTP(email);

        assertDoesNotThrow(() -> {
            customerAPI.sentResetOTP(email);
        });

        verify(customerService).sentResetOTP(email);
    }

    @Test
    void testGetTrialStatus_Success() {
        String email = "test@example.com";
        when(trialService.isTrialActive(email)).thenReturn(true);
        when(trialService.isTrialExpired(email)).thenReturn(false);

        ResponseEntity<Map<String, Object>> result = customerAPI.getTrialStatus(email);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertTrue((Boolean) body.get("isTrialActive"));
        assertFalse((Boolean) body.get("isTrialExpired"));
        verify(trialService).isTrialActive(email);
        verify(trialService).isTrialExpired(email);
    }

    @Test
    void testActivateSubscription_Success() {
        String email = "test@example.com";
        doNothing().when(trialService).activatePaidSubscription(email);

        ResponseEntity<Map<String, String>> result = customerAPI.activateSubscription(email);

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        Map<String, String> body = result.getBody();
        assertEquals("Subscription activated successfully", body.get("message"));
        assertEquals("success", body.get("status"));
        verify(trialService).activatePaidSubscription(email);
    }
}
