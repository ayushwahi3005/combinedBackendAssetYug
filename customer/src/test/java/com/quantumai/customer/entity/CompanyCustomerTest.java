package com.quantumai.customer.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CompanyCustomerTest {

    private CompanyCustomer companyCustomer;

    @BeforeEach
    void setUp() {
        companyCustomer = new CompanyCustomer();
    }

    @Test
    void testDefaultConstructor() {
        CompanyCustomer customer = new CompanyCustomer();
        assertNotNull(customer);
        assertNull(customer.getId());
        assertNull(customer.getName());
        assertNull(customer.getEmail());
        assertNull(customer.getCompanyId());
    }

    @Test
    void testSettersAndGetters() {
        // Test ID
        companyCustomer.setId("test-id");
        assertEquals("test-id", companyCustomer.getId());

        // Test Company Customer ID
        companyCustomer.setCompanyCustomerId(123);
        assertEquals(Integer.valueOf(123), companyCustomer.getCompanyCustomerId());

        // Test Name
        companyCustomer.setName("Test Customer");
        assertEquals("Test Customer", companyCustomer.getName());

        // Test Company ID
        companyCustomer.setCompanyId(1L);
        assertEquals(Long.valueOf(1L), companyCustomer.getCompanyId());

        // Test Category
        companyCustomer.setCategory("test-category");
        assertEquals("test-category", companyCustomer.getCategory());

        // Test Status
        companyCustomer.setStatus("active");
        assertEquals("active", companyCustomer.getStatus());

        // Test Phone
        companyCustomer.setPhone("1234567890");
        assertEquals("1234567890", companyCustomer.getPhone());

        // Test Email
        companyCustomer.setEmail("test@example.com");
        assertEquals("test@example.com", companyCustomer.getEmail());

        // Test Address
        companyCustomer.setAddress("123 Test Street");
        assertEquals("123 Test Street", companyCustomer.getAddress());

        // Test Apartment
        companyCustomer.setApartment("Apt 4B");
        assertEquals("Apt 4B", companyCustomer.getApartment());

        // Test City
        companyCustomer.setCity("Test City");
        assertEquals("Test City", companyCustomer.getCity());

        // Test State
        companyCustomer.setState("Test State");
        assertEquals("Test State", companyCustomer.getState());

        // Test Zip Code
        companyCustomer.setZipCode(12345);
        assertEquals(Integer.valueOf(12345), companyCustomer.getZipCode());

        // Test Updated At
        String timestamp = LocalDateTime.now().toString();
        companyCustomer.setUpdatedAt(timestamp);
        assertEquals(timestamp, companyCustomer.getUpdatedAt());
    }

    @Test
    void testNullValues() {
        // Test that null values are handled properly
        companyCustomer.setId(null);
        companyCustomer.setName(null);
        companyCustomer.setEmail(null);
        companyCustomer.setPhone(null);
        companyCustomer.setAddress(null);
        companyCustomer.setApartment(null);
        companyCustomer.setCity(null);
        companyCustomer.setState(null);
        companyCustomer.setCategory(null);
        companyCustomer.setStatus(null);
        companyCustomer.setUpdatedAt(null);
        companyCustomer.setCompanyId(null);
        companyCustomer.setCompanyCustomerId(null);
        companyCustomer.setZipCode(null);

        assertNull(companyCustomer.getId());
        assertNull(companyCustomer.getName());
        assertNull(companyCustomer.getEmail());
        assertNull(companyCustomer.getPhone());
        assertNull(companyCustomer.getAddress());
        assertNull(companyCustomer.getApartment());
        assertNull(companyCustomer.getCity());
        assertNull(companyCustomer.getState());
        assertNull(companyCustomer.getCategory());
        assertNull(companyCustomer.getStatus());
        assertNull(companyCustomer.getUpdatedAt());
        assertNull(companyCustomer.getCompanyId());
        assertNull(companyCustomer.getCompanyCustomerId());
        assertNull(companyCustomer.getZipCode());
    }

    @Test
    void testEmptyStrings() {
        // Test that empty strings are handled properly
        companyCustomer.setName("");
        companyCustomer.setEmail("");
        companyCustomer.setPhone("");
        companyCustomer.setAddress("");
        companyCustomer.setApartment("");
        companyCustomer.setCity("");
        companyCustomer.setState("");
        companyCustomer.setCategory("");
        companyCustomer.setStatus("");
        companyCustomer.setUpdatedAt("");

        assertEquals("", companyCustomer.getName());
        assertEquals("", companyCustomer.getEmail());
        assertEquals("", companyCustomer.getPhone());
        assertEquals("", companyCustomer.getAddress());
        assertEquals("", companyCustomer.getApartment());
        assertEquals("", companyCustomer.getCity());
        assertEquals("", companyCustomer.getState());
        assertEquals("", companyCustomer.getCategory());
        assertEquals("", companyCustomer.getStatus());
        assertEquals("", companyCustomer.getUpdatedAt());
    }

    @Test
    void testLombokDataAnnotation() {
        // Test that Lombok @Data annotation works correctly
        CompanyCustomer customer1 = new CompanyCustomer();
        customer1.setId("test-id");
        customer1.setName("Test Customer");
        customer1.setEmail("test@example.com");
        customer1.setCompanyId(1L);

        CompanyCustomer customer2 = new CompanyCustomer();
        customer2.setId("test-id");
        customer2.setName("Test Customer");
        customer2.setEmail("test@example.com");
        customer2.setCompanyId(1L);

        // Test equals method (generated by Lombok)
        assertEquals(customer1, customer2);

        // Test hashCode method (generated by Lombok)
        assertEquals(customer1.hashCode(), customer2.hashCode());

        // Test toString method (generated by Lombok)
        assertNotNull(customer1.toString());
        assertTrue(customer1.toString().contains("CompanyCustomer"));
        assertTrue(customer1.toString().contains("test-id"));
        assertTrue(customer1.toString().contains("Test Customer"));
    }

    @Test
    void testFieldModification() {
        // Test that fields can be modified after creation
        companyCustomer.setName("Original Name");
        assertEquals("Original Name", companyCustomer.getName());

        companyCustomer.setName("Modified Name");
        assertEquals("Modified Name", companyCustomer.getName());

        companyCustomer.setCompanyId(1L);
        assertEquals(Long.valueOf(1L), companyCustomer.getCompanyId());

        companyCustomer.setCompanyId(2L);
        assertEquals(Long.valueOf(2L), companyCustomer.getCompanyId());
    }

    @Test
    void testNumericFields() {
        // Test numeric field boundaries
        companyCustomer.setCompanyCustomerId(Integer.MAX_VALUE);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), companyCustomer.getCompanyCustomerId());

        companyCustomer.setCompanyCustomerId(Integer.MIN_VALUE);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), companyCustomer.getCompanyCustomerId());

        companyCustomer.setCompanyId(Long.MAX_VALUE);
        assertEquals(Long.valueOf(Long.MAX_VALUE), companyCustomer.getCompanyId());

        companyCustomer.setCompanyId(Long.MIN_VALUE);
        assertEquals(Long.valueOf(Long.MIN_VALUE), companyCustomer.getCompanyId());

        companyCustomer.setZipCode(99999);
        assertEquals(Integer.valueOf(99999), companyCustomer.getZipCode());

        companyCustomer.setZipCode(0);
        assertEquals(Integer.valueOf(0), companyCustomer.getZipCode());
    }

    @Test
    void testStringFieldLengths() {
        // Test with long strings
        String longString = "a".repeat(1000);
        
        companyCustomer.setName(longString);
        assertEquals(longString, companyCustomer.getName());

        companyCustomer.setEmail(longString);
        assertEquals(longString, companyCustomer.getEmail());

        companyCustomer.setAddress(longString);
        assertEquals(longString, companyCustomer.getAddress());
    }

    @Test
    void testSpecialCharacters() {
        // Test with special characters
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        companyCustomer.setName("Test " + specialChars);
        assertEquals("Test " + specialChars, companyCustomer.getName());

        companyCustomer.setAddress("123 Main St " + specialChars);
        assertEquals("123 Main St " + specialChars, companyCustomer.getAddress());
    }

    @Test
    void testUnicodeCharacters() {
        // Test with Unicode characters
        companyCustomer.setName("测试客户");
        assertEquals("测试客户", companyCustomer.getName());

        companyCustomer.setCity("São Paulo");
        assertEquals("São Paulo", companyCustomer.getCity());

        companyCustomer.setAddress("Москва, Россия");
        assertEquals("Москва, Россия", companyCustomer.getAddress());
    }
}
