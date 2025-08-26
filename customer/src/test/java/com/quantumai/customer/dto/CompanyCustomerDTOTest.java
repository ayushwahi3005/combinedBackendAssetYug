package com.quantumai.customer.dto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class CompanyCustomerDTOTest {

    private CompanyCustomerDTO companyCustomerDTO;

    @BeforeEach
    void setUp() {
        companyCustomerDTO = new CompanyCustomerDTO();
    }

    @Test
    void testDefaultConstructor() {
        CompanyCustomerDTO dto = new CompanyCustomerDTO();
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getName());
        assertNull(dto.getEmail());
        assertNull(dto.getCompanyId());
    }

    @Test
    void testSettersAndGetters() {
        // Test ID
        companyCustomerDTO.setId("test-id");
        assertEquals("test-id", companyCustomerDTO.getId());

        // Test Company Customer ID
        companyCustomerDTO.setCompanyCustomerId(123);
        assertEquals(Integer.valueOf(123), companyCustomerDTO.getCompanyCustomerId());

        // Test Name
        companyCustomerDTO.setName("Test Customer");
        assertEquals("Test Customer", companyCustomerDTO.getName());

        // Test Company ID
        companyCustomerDTO.setCompanyId(1L);
        assertEquals(Long.valueOf(1L), companyCustomerDTO.getCompanyId());

        // Test Category
        companyCustomerDTO.setCategory("test-category");
        assertEquals("test-category", companyCustomerDTO.getCategory());

        // Test Status
        companyCustomerDTO.setStatus("active");
        assertEquals("active", companyCustomerDTO.getStatus());

        // Test Phone
        companyCustomerDTO.setPhone("1234567890");
        assertEquals("1234567890", companyCustomerDTO.getPhone());

        // Test Email
        companyCustomerDTO.setEmail("test@example.com");
        assertEquals("test@example.com", companyCustomerDTO.getEmail());

        // Test Address
        companyCustomerDTO.setAddress("123 Test Street");
        assertEquals("123 Test Street", companyCustomerDTO.getAddress());

        // Test Apartment
        companyCustomerDTO.setApartment("Apt 4B");
        assertEquals("Apt 4B", companyCustomerDTO.getApartment());

        // Test City
        companyCustomerDTO.setCity("Test City");
        assertEquals("Test City", companyCustomerDTO.getCity());

        // Test State
        companyCustomerDTO.setState("Test State");
        assertEquals("Test State", companyCustomerDTO.getState());

        // Test Zip Code
        companyCustomerDTO.setZipCode(12345);
        assertEquals(Integer.valueOf(12345), companyCustomerDTO.getZipCode());

        // Test Updated At
        companyCustomerDTO.setUpdatedAt("2023-01-01T10:00:00");
        assertEquals("2023-01-01T10:00:00", companyCustomerDTO.getUpdatedAt());
    }

    @Test
    void testValidDTO() {
        companyCustomerDTO.setName("Valid Customer");
        companyCustomerDTO.setEmail("valid@example.com");
        companyCustomerDTO.setCompanyId(1L);
        companyCustomerDTO.setPhone("1234567890");
        companyCustomerDTO.setStatus("active");

        // Basic validation - ensure fields are set correctly
        assertEquals("Valid Customer", companyCustomerDTO.getName());
        assertEquals("valid@example.com", companyCustomerDTO.getEmail());
        assertEquals(Long.valueOf(1L), companyCustomerDTO.getCompanyId());
    }

    @Test
    void testNullValues() {
        companyCustomerDTO.setId(null);
        companyCustomerDTO.setName(null);
        companyCustomerDTO.setEmail(null);
        companyCustomerDTO.setPhone(null);
        companyCustomerDTO.setAddress(null);
        companyCustomerDTO.setApartment(null);
        companyCustomerDTO.setCity(null);
        companyCustomerDTO.setState(null);
        companyCustomerDTO.setCategory(null);
        companyCustomerDTO.setStatus(null);
        companyCustomerDTO.setUpdatedAt(null);
        companyCustomerDTO.setCompanyId(null);
        companyCustomerDTO.setCompanyCustomerId(null);
        companyCustomerDTO.setZipCode(null);

        assertNull(companyCustomerDTO.getId());
        assertNull(companyCustomerDTO.getName());
        assertNull(companyCustomerDTO.getEmail());
        assertNull(companyCustomerDTO.getPhone());
        assertNull(companyCustomerDTO.getAddress());
        assertNull(companyCustomerDTO.getApartment());
        assertNull(companyCustomerDTO.getCity());
        assertNull(companyCustomerDTO.getState());
        assertNull(companyCustomerDTO.getCategory());
        assertNull(companyCustomerDTO.getStatus());
        assertNull(companyCustomerDTO.getUpdatedAt());
        assertNull(companyCustomerDTO.getCompanyId());
        assertNull(companyCustomerDTO.getCompanyCustomerId());
        assertNull(companyCustomerDTO.getZipCode());
    }

    @Test
    void testEmptyStrings() {
        companyCustomerDTO.setName("");
        companyCustomerDTO.setEmail("");
        companyCustomerDTO.setPhone("");
        companyCustomerDTO.setAddress("");
        companyCustomerDTO.setApartment("");
        companyCustomerDTO.setCity("");
        companyCustomerDTO.setState("");
        companyCustomerDTO.setCategory("");
        companyCustomerDTO.setStatus("");
        companyCustomerDTO.setUpdatedAt("");

        assertEquals("", companyCustomerDTO.getName());
        assertEquals("", companyCustomerDTO.getEmail());
        assertEquals("", companyCustomerDTO.getPhone());
        assertEquals("", companyCustomerDTO.getAddress());
        assertEquals("", companyCustomerDTO.getApartment());
        assertEquals("", companyCustomerDTO.getCity());
        assertEquals("", companyCustomerDTO.getState());
        assertEquals("", companyCustomerDTO.getCategory());
        assertEquals("", companyCustomerDTO.getStatus());
        assertEquals("", companyCustomerDTO.getUpdatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        CompanyCustomerDTO dto1 = new CompanyCustomerDTO();
        dto1.setId("test-id");
        dto1.setName("Test Customer");
        dto1.setEmail("test@example.com");
        dto1.setCompanyId(1L);

        CompanyCustomerDTO dto2 = new CompanyCustomerDTO();
        dto2.setId("test-id");
        dto2.setName("Test Customer");
        dto2.setEmail("test@example.com");
        dto2.setCompanyId(1L);

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToString() {
        companyCustomerDTO.setId("test-id");
        companyCustomerDTO.setName("Test Customer");
        companyCustomerDTO.setEmail("test@example.com");

        String toString = companyCustomerDTO.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("CompanyCustomerDTO"));
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("Test Customer"));
    }

    @Test
    void testNumericFieldBoundaries() {
        // Test Integer boundaries
        companyCustomerDTO.setCompanyCustomerId(Integer.MAX_VALUE);
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), companyCustomerDTO.getCompanyCustomerId());

        companyCustomerDTO.setCompanyCustomerId(Integer.MIN_VALUE);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), companyCustomerDTO.getCompanyCustomerId());

        // Test Long boundaries
        companyCustomerDTO.setCompanyId(Long.MAX_VALUE);
        assertEquals(Long.valueOf(Long.MAX_VALUE), companyCustomerDTO.getCompanyId());

        companyCustomerDTO.setCompanyId(Long.MIN_VALUE);
        assertEquals(Long.valueOf(Long.MIN_VALUE), companyCustomerDTO.getCompanyId());

        // Test zip code
        companyCustomerDTO.setZipCode(99999);
        assertEquals(Integer.valueOf(99999), companyCustomerDTO.getZipCode());

        companyCustomerDTO.setZipCode(0);
        assertEquals(Integer.valueOf(0), companyCustomerDTO.getZipCode());
    }

    @Test
    void testSpecialCharactersInFields() {
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        companyCustomerDTO.setName("Test " + specialChars);
        assertEquals("Test " + specialChars, companyCustomerDTO.getName());

        companyCustomerDTO.setAddress("123 Main St " + specialChars);
        assertEquals("123 Main St " + specialChars, companyCustomerDTO.getAddress());
    }

    @Test
    void testUnicodeCharacters() {
        companyCustomerDTO.setName("测试客户");
        assertEquals("测试客户", companyCustomerDTO.getName());

        companyCustomerDTO.setCity("São Paulo");
        assertEquals("São Paulo", companyCustomerDTO.getCity());

        companyCustomerDTO.setAddress("Москва, Россия");
        assertEquals("Москва, Россия", companyCustomerDTO.getAddress());
    }

    @Test
    void testLongStringValues() {
        String longString = "a".repeat(1000);
        
        companyCustomerDTO.setName(longString);
        assertEquals(longString, companyCustomerDTO.getName());

        companyCustomerDTO.setEmail(longString);
        assertEquals(longString, companyCustomerDTO.getEmail());

        companyCustomerDTO.setAddress(longString);
        assertEquals(longString, companyCustomerDTO.getAddress());
    }

    @Test
    void testStatusValues() {
        // Test common status values
        companyCustomerDTO.setStatus("active");
        assertEquals("active", companyCustomerDTO.getStatus());

        companyCustomerDTO.setStatus("inactive");
        assertEquals("inactive", companyCustomerDTO.getStatus());

        companyCustomerDTO.setStatus("pending");
        assertEquals("pending", companyCustomerDTO.getStatus());
    }

    @Test
    void testEmailFormats() {
        // Test various email formats
        String[] validEmails = {
            "test@example.com",
            "user.name@domain.co.uk",
            "user+tag@example.org",
            "123@example.com"
        };

        for (String email : validEmails) {
            companyCustomerDTO.setEmail(email);
            assertEquals(email, companyCustomerDTO.getEmail());
        }
    }

    @Test
    void testPhoneFormats() {
        // Test various phone formats
        String[] phoneFormats = {
            "1234567890",
            "+1-234-567-8900",
            "(123) 456-7890",
            "123.456.7890"
        };

        for (String phone : phoneFormats) {
            companyCustomerDTO.setPhone(phone);
            assertEquals(phone, companyCustomerDTO.getPhone());
        }
    }

    @Test
    void testFieldModification() {
        // Test that fields can be modified after initial setting
        companyCustomerDTO.setName("Original Name");
        assertEquals("Original Name", companyCustomerDTO.getName());

        companyCustomerDTO.setName("Modified Name");
        assertEquals("Modified Name", companyCustomerDTO.getName());

        companyCustomerDTO.setCompanyId(1L);
        assertEquals(Long.valueOf(1L), companyCustomerDTO.getCompanyId());

        companyCustomerDTO.setCompanyId(2L);
        assertEquals(Long.valueOf(2L), companyCustomerDTO.getCompanyId());
    }
}
