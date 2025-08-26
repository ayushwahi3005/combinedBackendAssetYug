package com.quantumai.customer.repository;

import com.quantumai.customer.entity.CompanyCustomer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@TestPropertySource(properties = {
    "spring.mongodb.embedded.version=3.5.5"
})
class CompanyCustomerRepositoryTest {

    @Autowired
    private CompanyCustomerRepository companyCustomerRepository;

    private CompanyCustomer testCustomer1;
    private CompanyCustomer testCustomer2;

    @BeforeEach
    void setUp() {
        companyCustomerRepository.deleteAll();

        testCustomer1 = new CompanyCustomer();
        testCustomer1.setName("Test Customer 1");
        testCustomer1.setEmail("test1@example.com");
        testCustomer1.setCompanyId(1L);
        testCustomer1.setCompanyCustomerId(1);
        testCustomer1.setPhone("1234567890");
        testCustomer1.setAddress("123 Test St");
        testCustomer1.setCity("Test City");
        testCustomer1.setState("Test State");
        testCustomer1.setZipCode(12345);
        testCustomer1.setStatus("active");
        testCustomer1.setCategory("category1");
        testCustomer1.setUpdatedAt(LocalDateTime.now().toString());

        testCustomer2 = new CompanyCustomer();
        testCustomer2.setName("Test Customer 2");
        testCustomer2.setEmail("test2@example.com");
        testCustomer2.setCompanyId(2L);
        testCustomer2.setCompanyCustomerId(1);
        testCustomer2.setPhone("0987654321");
        testCustomer2.setAddress("456 Test Ave");
        testCustomer2.setCity("Another City");
        testCustomer2.setState("Another State");
        testCustomer2.setZipCode(54321);
        testCustomer2.setStatus("inactive");
        testCustomer2.setCategory("category2");
        testCustomer2.setUpdatedAt(LocalDateTime.now().toString());
    }

    @Test
    void testSaveAndFindById() {
        // Save customer
        CompanyCustomer savedCustomer = companyCustomerRepository.save(testCustomer1);
        assertNotNull(savedCustomer.getId());

        // Find by ID
        Optional<CompanyCustomer> foundCustomer = companyCustomerRepository.findById(savedCustomer.getId());
        assertTrue(foundCustomer.isPresent());
        assertEquals(testCustomer1.getName(), foundCustomer.get().getName());
        assertEquals(testCustomer1.getEmail(), foundCustomer.get().getEmail());
    }

    @Test
    void testFindByCompanyId() {
        // Save customers with different company IDs
        companyCustomerRepository.save(testCustomer1);
        companyCustomerRepository.save(testCustomer2);

        // Find by company ID 1
        List<CompanyCustomer> company1Customers = companyCustomerRepository.findByCompanyId(1L);
        assertEquals(1, company1Customers.size());
        assertEquals("Test Customer 1", company1Customers.get(0).getName());

        // Find by company ID 2
        List<CompanyCustomer> company2Customers = companyCustomerRepository.findByCompanyId(2L);
        assertEquals(1, company2Customers.size());
        assertEquals("Test Customer 2", company2Customers.get(0).getName());
    }

    @Test
    void testFindByEmailAndCompanyId() {
        // Save customer
        companyCustomerRepository.save(testCustomer1);

        // Find by email and company ID
        Optional<CompanyCustomer> foundCustomer = companyCustomerRepository
                .findByEmailAndCompanyId("test1@example.com", 1L);
        assertTrue(foundCustomer.isPresent());
        assertEquals(testCustomer1.getName(), foundCustomer.get().getName());

        // Try with wrong company ID
        Optional<CompanyCustomer> notFoundCustomer = companyCustomerRepository
                .findByEmailAndCompanyId("test1@example.com", 2L);
        assertFalse(notFoundCustomer.isPresent());
    }

    @Test
    void testFindByCompanyCustomerIdAndCompanyId() {
        // Save customer
        companyCustomerRepository.save(testCustomer1);

        // Find by company customer ID and company ID
        CompanyCustomer foundCustomer = companyCustomerRepository
                .findByCompanyCustomerIdAndCompanyId(1, 1L);
        assertNotNull(foundCustomer);
        assertEquals(testCustomer1.getName(), foundCustomer.getName());

        // Try with wrong company ID
        CompanyCustomer notFoundCustomer = companyCustomerRepository
                .findByCompanyCustomerIdAndCompanyId(1, 2L);
        assertNull(notFoundCustomer);
    }

    @Test
    void testDeleteCustomer() {
        // Save customer
        CompanyCustomer savedCustomer = companyCustomerRepository.save(testCustomer1);
        String customerId = savedCustomer.getId();

        // Verify customer exists
        assertTrue(companyCustomerRepository.findById(customerId).isPresent());

        // Delete customer
        companyCustomerRepository.deleteById(customerId);

        // Verify customer is deleted
        assertFalse(companyCustomerRepository.findById(customerId).isPresent());
    }

    @Test
    void testUpdateCustomer() {
        // Save customer
        CompanyCustomer savedCustomer = companyCustomerRepository.save(testCustomer1);

        // Update customer
        savedCustomer.setName("Updated Name");
        savedCustomer.setEmail("updated@example.com");
        CompanyCustomer updatedCustomer = companyCustomerRepository.save(savedCustomer);

        // Verify update
        assertEquals("Updated Name", updatedCustomer.getName());
        assertEquals("updated@example.com", updatedCustomer.getEmail());

        // Verify in database
        Optional<CompanyCustomer> foundCustomer = companyCustomerRepository.findById(savedCustomer.getId());
        assertTrue(foundCustomer.isPresent());
        assertEquals("Updated Name", foundCustomer.get().getName());
        assertEquals("updated@example.com", foundCustomer.get().getEmail());
    }

    @Test
    void testFindAll() {
        // Save multiple customers
        companyCustomerRepository.save(testCustomer1);
        companyCustomerRepository.save(testCustomer2);

        // Find all
        List<CompanyCustomer> allCustomers = companyCustomerRepository.findAll();
        assertEquals(2, allCustomers.size());
    }

    @Test
    void testCustomerWithNullValues() {
        CompanyCustomer customerWithNulls = new CompanyCustomer();
        customerWithNulls.setName("Minimal Customer");
        customerWithNulls.setCompanyId(1L);
        customerWithNulls.setCompanyCustomerId(2);

        // Save customer with minimal data
        CompanyCustomer savedCustomer = companyCustomerRepository.save(customerWithNulls);
        assertNotNull(savedCustomer.getId());

        // Verify retrieval
        Optional<CompanyCustomer> foundCustomer = companyCustomerRepository.findById(savedCustomer.getId());
        assertTrue(foundCustomer.isPresent());
        assertEquals("Minimal Customer", foundCustomer.get().getName());
        assertNull(foundCustomer.get().getEmail());
        assertNull(foundCustomer.get().getPhone());
    }

    @Test
    void testUniqueConstraints() {
        // Save first customer
        companyCustomerRepository.save(testCustomer1);

        // Try to save another customer with same email and company ID
        CompanyCustomer duplicateCustomer = new CompanyCustomer();
        duplicateCustomer.setName("Duplicate Customer");
        duplicateCustomer.setEmail("test1@example.com");
        duplicateCustomer.setCompanyId(1L);
        duplicateCustomer.setCompanyCustomerId(2);

        // This should work as there's no unique constraint enforced at DB level
        // The business logic handles this in the service layer
        assertDoesNotThrow(() -> {
            companyCustomerRepository.save(duplicateCustomer);
        });
    }
}
