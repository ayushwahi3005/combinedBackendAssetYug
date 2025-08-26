package com.quantumai.customer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.CompanyCustomerDTO;
import com.quantumai.customer.entity.CompanyCustomer;
import com.quantumai.customer.repository.CompanyCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=3.5.5",
    "spring.data.mongodb.database=test_db"
})
class CompanyCustomerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CompanyCustomerRepository companyCustomerRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CompanyCustomerDTO testCustomerDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        
        // Clean database
        companyCustomerRepository.deleteAll();
        
        testCustomerDTO = new CompanyCustomerDTO();
        testCustomerDTO.setName("Integration Test Customer");
        testCustomerDTO.setEmail("integration@example.com");
        testCustomerDTO.setCompanyId(1L);
        testCustomerDTO.setPhone("1234567890");
        testCustomerDTO.setAddress("123 Integration St");
        testCustomerDTO.setCity("Test City");
        testCustomerDTO.setState("Test State");
        testCustomerDTO.setZipCode(12345);
        testCustomerDTO.setStatus("active");
        testCustomerDTO.setCategory("test-category");
    }

    @Test
    void testWorkingEndpoint() throws Exception {
        mockMvc.perform(get("/companycustomer/working"))
                .andExpect(status().isOk())
                .andExpect(content().string("Working!!"));
    }

    @Test
    void testAddAndGetCustomer() throws Exception {
        // Add customer
        String customerJson = objectMapper.writeValueAsString(testCustomerDTO);
        
        mockMvc.perform(post("/companycustomer/addCompanyCustomer")
                        .header("companyId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Customer"))
                .andExpect(jsonPath("$.email").value("integration@example.com"));

        // Get all customers
        mockMvc.perform(get("/companycustomer/allCompanyCustomer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Integration Test Customer"));
    }

    @Test
    void testUpdateCustomer() throws Exception {
        // First add a customer
        CompanyCustomer savedCustomer = new CompanyCustomer();
        savedCustomer.setName("Original Name");
        savedCustomer.setEmail("original@example.com");
        savedCustomer.setCompanyId(1L);
        savedCustomer.setCompanyCustomerId(1);
        savedCustomer = companyCustomerRepository.save(savedCustomer);

        // Update the customer
        testCustomerDTO.setId(savedCustomer.getId());
        testCustomerDTO.setName("Updated Name");
        testCustomerDTO.setEmail("updated@example.com");
        
        String customerJson = objectMapper.writeValueAsString(testCustomerDTO);
        
        mockMvc.perform(put("/companycustomer/updateCompanyCustomer")
                        .header("companyId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isOk());

        // Verify update
        mockMvc.perform(get("/companycustomer/getCompanyCustomer/" + savedCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void testDeleteCustomer() throws Exception {
        // First add a customer
        CompanyCustomer savedCustomer = new CompanyCustomer();
        savedCustomer.setName("To Delete");
        savedCustomer.setEmail("delete@example.com");
        savedCustomer.setCompanyId(1L);
        savedCustomer.setCompanyCustomerId(1);
        savedCustomer = companyCustomerRepository.save(savedCustomer);

        // Delete the customer
        mockMvc.perform(delete("/companycustomer/deleteCompanyCustomer/" + savedCustomer.getId())
                        .header("companyId", 1L))
                .andExpect(status().isOk());

        // Verify deletion - should return empty list
        mockMvc.perform(get("/companycustomer/allCompanyCustomer/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testEmailAlreadyExistsValidation() throws Exception {
        // Add first customer
        String customerJson = objectMapper.writeValueAsString(testCustomerDTO);
        
        mockMvc.perform(post("/companycustomer/addCompanyCustomer")
                        .header("companyId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerJson))
                .andExpect(status().isOk());

        // Try to add another customer with same email
        CompanyCustomerDTO duplicateCustomer = new CompanyCustomerDTO();
        duplicateCustomer.setName("Duplicate Customer");
        duplicateCustomer.setEmail("integration@example.com"); // Same email
        duplicateCustomer.setCompanyId(1L);
        
        String duplicateJson = objectMapper.writeValueAsString(duplicateCustomer);
        
        mockMvc.perform(post("/companycustomer/addCompanyCustomer")
                        .header("companyId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateJson))
                .andExpect(status().isBadRequest());
    }
}
