package com.quantumai.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.CompanyCustomer;
import com.quantumai.customer.entity.CompanyCustomerMandatoryFields;
import com.quantumai.customer.entity.CompanyCustomerShowFields;
import com.quantumai.customer.exception.EmailAlreadyExistsException;
import com.quantumai.customer.exception.NameColumnMissingException;
import com.quantumai.customer.exception.NoSubscriptionError;
import com.quantumai.customer.repository.CompanyCustomerRepository;
import com.quantumai.customer.repository.SubscriptionRepository;
import com.quantumai.customer.service.CompanyCustomerService;
import com.quantumai.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompanyCustomerAPITest {

    @Mock
    private CompanyCustomerService companyCustomerService;

    @Mock
    private CompanyCustomerRepository companyCustomerRepository;

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private CustomerService customerService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private CompanyCustomerAPI companyCustomerAPI;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CompanyCustomerDTO testCustomerDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(companyCustomerAPI).build();
        objectMapper = new ObjectMapper();
        
        testCustomerDTO = new CompanyCustomerDTO();
        testCustomerDTO.setId("test-id");
        testCustomerDTO.setName("Test Customer");
        testCustomerDTO.setEmail("test@example.com");
        testCustomerDTO.setCompanyId(1L);
        testCustomerDTO.setPhone("1234567890");
        testCustomerDTO.setAddress("123 Test St");
        testCustomerDTO.setCity("Test City");
        testCustomerDTO.setState("Test State");
        testCustomerDTO.setZipCode(12345);
        testCustomerDTO.setStatus("active");
        testCustomerDTO.setCategory("test-category");
    }

    @Test
    void testWorking() throws Exception {
        mockMvc.perform(get("/companycustomer/working"))
                .andExpect(status().isOk())
                .andExpect(content().string("Working!!"));
    }

    @Test
    void testDeleteCompanyCustomer_Success() throws Exception {
        String customerId = "test-id";
        Long companyId = 1L;

        doNothing().when(companyCustomerService).deleteCustomer(customerId);

        mockMvc.perform(delete("/companycustomer/deleteCompanyCustomer/{id}", customerId)
                        .header("companyId", companyId))
                .andExpect(status().isOk());

        verify(companyCustomerService).deleteCustomer(customerId);
    }

    @Test
    void testGetCompanyCustomerList_Success() {
        Long companyId = 1L;
        List<CompanyCustomerDTO> expectedList = Arrays.asList(testCustomerDTO);

        when(companyCustomerService.getAllCustomer(companyId)).thenReturn(expectedList);

        List<CompanyCustomerDTO> result = companyCustomerAPI.getCompanyCustomerList(companyId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomerDTO.getName(), result.get(0).getName());
        verify(companyCustomerService).getAllCustomer(companyId);
    }

    @Test
    void testGetCompanyCustomer_Success() {
        String customerId = "test-id";

        when(companyCustomerService.getCustomer(customerId)).thenReturn(testCustomerDTO);

        CompanyCustomerDTO result = companyCustomerAPI.getCompanyCustomer(customerId);

        assertNotNull(result);
        assertEquals(testCustomerDTO.getName(), result.getName());
        assertEquals(testCustomerDTO.getEmail(), result.getEmail());
        verify(companyCustomerService).getCustomer(customerId);
    }

    @Test
    void testGetCompanyCustomerByLocalId_Success() {
        String localId = "1";
        Long companyId = 1L;

        when(companyCustomerService.getCompanyCustomerByLocalId(1, companyId)).thenReturn(testCustomerDTO);

        CompanyCustomerDTO result = companyCustomerAPI.getCompanyCustomerByLocalId(localId, companyId);

        assertNotNull(result);
        assertEquals(testCustomerDTO.getName(), result.getName());
        verify(companyCustomerService).getCompanyCustomerByLocalId(1, companyId);
    }

    @Test
    void testAddNewFields_Success() throws Exception {
        Long companyId = 1L;

        when(companyCustomerService.addCustomer(any(CompanyCustomerDTO.class))).thenReturn(testCustomerDTO);

        CompanyCustomerDTO result = companyCustomerAPI.addNewFields(testCustomerDTO, companyId);

        assertNotNull(result);
        assertEquals(testCustomerDTO.getName(), result.getName());
        verify(companyCustomerService).addCustomer(testCustomerDTO);
    }

    @Test
    void testAddNewFields_EmailAlreadyExists() throws EmailAlreadyExistsException {
        Long companyId = 1L;

        when(companyCustomerService.addCustomer(any(CompanyCustomerDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        assertThrows(EmailAlreadyExistsException.class, () -> {
            companyCustomerAPI.addNewFields(testCustomerDTO, companyId);
        });
    }

    @Test
    void testUpdateCompanyCustomer_Success() throws Exception {
        Long companyId = 1L;

        doNothing().when(companyCustomerService).updateCustomer(any(CompanyCustomerDTO.class));

        assertDoesNotThrow(() -> {
            companyCustomerAPI.updateCompanyCustomer(testCustomerDTO, companyId);
        });

        verify(companyCustomerService).updateCustomer(testCustomerDTO);
    }

    @Test
    void testUpdateCompanyCustomer_EmailAlreadyExists() throws EmailAlreadyExistsException {
        Long companyId = 1L;

        doThrow(new EmailAlreadyExistsException("Email already exists"))
                .when(companyCustomerService).updateCustomer(any(CompanyCustomerDTO.class));

        assertThrows(EmailAlreadyExistsException.class, () -> {
            companyCustomerAPI.updateCompanyCustomer(testCustomerDTO, companyId);
        });
    }

    @Test
    void testGetCompanyCustomerFromAsset_Search() {
        Long companyId = 1L;
        String search = "test";
        String category = "name";
        List<String> expectedResults = Arrays.asList("Test Customer 1", "Test Customer 2");

        when(companyCustomerService.searchedCompanyCustomer(companyId, search, category))
                .thenReturn(expectedResults);

        List<String> result = companyCustomerAPI.getCompanyCustomerFromAsset(companyId, search, category);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(companyCustomerService).searchedCompanyCustomer(companyId, search, category);
    }

    @Test
    void testGetCompanyCustomerFromAsset_Sort() {
        Long companyId = 1L;
        String category = "name";
        List<String> expectedResults = Arrays.asList("Customer A", "Customer B");

        when(companyCustomerService.sortCompanyCustomer(companyId, category))
                .thenReturn(expectedResults);

        List<String> result = companyCustomerAPI.getCompanyCustomerFromAsset(companyId, category);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(companyCustomerService).sortCompanyCustomer(companyId, category);
    }

    @Test
    void testAddExtraFieldName_Success() throws Exception {
        CompanyCustomerExtraFieldNameDTO extraFieldDTO = new CompanyCustomerExtraFieldNameDTO();
        extraFieldDTO.setName("Extra Field");
        extraFieldDTO.setType("text");
        extraFieldDTO.setCompanyId(1L);
        Long companyId = 1L;

        doNothing().when(companyCustomerService).addCompanyCustomerExtraField(any(CompanyCustomerExtraFieldNameDTO.class));

        assertDoesNotThrow(() -> {
            companyCustomerAPI.addExtraFieldName(extraFieldDTO, companyId);
        });

        verify(companyCustomerService).addCompanyCustomerExtraField(extraFieldDTO);
    }

    @Test
    void testGetExtraFieldName_Success() {
        Long companyId = 1L;
        CompanyCustomerExtraFieldNameDTO extraFieldDTO = new CompanyCustomerExtraFieldNameDTO();
        extraFieldDTO.setName("Extra Field");
        List<CompanyCustomerExtraFieldNameDTO> expectedList = Arrays.asList(extraFieldDTO);

        when(companyCustomerService.getCompanyCustomerExtraField(companyId)).thenReturn(expectedList);

        List<CompanyCustomerExtraFieldNameDTO> result = companyCustomerAPI.getExtraFieldName(companyId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Extra Field", result.get(0).getName());
        verify(companyCustomerService).getCompanyCustomerExtraField(companyId);
    }

    @Test
    void testDeleteExtraFieldName_Success() throws Exception {
        String fieldId = "field-id";
        Long companyId = 1L;

        doNothing().when(companyCustomerService).deleteCompanyCustomerExtraField(fieldId);

        assertDoesNotThrow(() -> {
            companyCustomerAPI.deleteExtraFieldName(fieldId, companyId);
        });

        verify(companyCustomerService).deleteCompanyCustomerExtraField(fieldId);
    }

    @Test
    void testAddCompanyCustomerFile_Success() throws Exception {
        String companyCustomerId = "customer-id";
        Long companyId = 1L;
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test content".getBytes());

        doNothing().when(companyCustomerService).addCompanyCustomerFile(any(), eq(companyCustomerId));

        ResponseEntity<ResponseMessageDTO> result = companyCustomerAPI.addCompanyCustomerFile(file, companyCustomerId, companyId);

        assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().getResponseMessage().contains("Uploaded the file successfully"));
        verify(companyCustomerService).addCompanyCustomerFile(file, companyCustomerId);
    }

    @Test
    void testGetCompanyCustomerFile_Success() {
        String companyCustomerId = "customer-id";
        CompanyCustomerFileDTO fileDTO = new CompanyCustomerFileDTO();
        fileDTO.setFileName("test.txt");
        List<CompanyCustomerFileDTO> expectedList = Arrays.asList(fileDTO);

        when(companyCustomerService.getCompanyCustomerFile(companyCustomerId)).thenReturn(expectedList);

        List<CompanyCustomerFileDTO> result = companyCustomerAPI.getCompanyCustomerFile(companyCustomerId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getFileName());
        verify(companyCustomerService).getCompanyCustomerFile(companyCustomerId);
    }

    @Test
    void testDownloadFile_Success() {
        String fileId = "file-id";
        CompanyCustomerFileDTO fileDTO = new CompanyCustomerFileDTO();
        fileDTO.setFile("test content".getBytes());

        when(companyCustomerService.downloadFile(fileId)).thenReturn(fileDTO);

        ResponseEntity<?> result = companyCustomerAPI.downloadFile(fileId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(MediaType.valueOf("json/object"), result.getHeaders().getContentType());
        verify(companyCustomerService).downloadFile(fileId);
    }

    @Test
    void testDeleteFile_Success() throws Exception {
        String fileId = "file-id";
        Long companyId = 1L;

        doNothing().when(companyCustomerService).deleteFile(fileId);

        assertDoesNotThrow(() -> {
            companyCustomerAPI.deleteFile(fileId, companyId);
        });

        verify(companyCustomerService).deleteFile(fileId);
    }

    @Test
    void testAdvanceFilter_Success() throws Exception {
        Object filter = Map.of("name", "test", "companyId", "1");
        Integer pageNumber = 0;
        Integer pageSize = 10;
        String category = "name";
        String searchData = "test";
        Boolean asc = true;
        Long companyId = 1L;

        PaginatedResultDTO<String> expectedResult = new PaginatedResultDTO<>(Arrays.asList("result1", "result2"), 2);

        when(companyCustomerService.advanceFilter(filter, pageNumber, pageSize, category, searchData, asc))
                .thenReturn(expectedResult);

        PaginatedResultDTO<String> result = companyCustomerAPI.advanceFilter(
                filter, pageNumber, pageSize, category, searchData, asc, companyId);

        assertNotNull(result);
        assertEquals(2, result.getTotalRecords());
        assertEquals(2, result.getData().size());
        verify(companyCustomerService).advanceFilter(filter, pageNumber, pageSize, category, searchData, asc);
    }

    @Test
    void testAdvanceFilter_WithDefaults() throws Exception {
        Object filter = Map.of("companyId", "1");
        Long companyId = 1L;

        PaginatedResultDTO<String> expectedResult = new PaginatedResultDTO<>(Arrays.asList("result1"), 1);

        when(companyCustomerService.advanceFilter(any(), eq(0), eq(5), eq("updatedAt"), eq(""), eq(false)))
                .thenReturn(expectedResult);

        PaginatedResultDTO<String> result = companyCustomerAPI.advanceFilter(
                filter, null, null, null, null, null, companyId);

        assertNotNull(result);
        assertEquals(1, result.getTotalRecords());
        verify(companyCustomerService).advanceFilter(filter, 0, 5, "updatedAt", "", false);
    }

    @Test
    void testImportFile_NameColumnMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "header1,header2\nvalue1,value2".getBytes());
        String columnMappings = "{\"header1\":\"Email\",\"header2\":\"Phone\"}";
        Long companyId = 1L;
        String email = "test@example.com";

        assertThrows(NameColumnMissingException.class, () -> {
            companyCustomerAPI.importFile(file, columnMappings, companyId, email);
        });
    }

    @Test
    void testGetMandatoryFields_Success() {
        String name = "test-field";
        Long companyId = 1L;
        CompanyCustomerMandatoryFields mandatoryFields = new CompanyCustomerMandatoryFields();
        mandatoryFields.setName(name);
        mandatoryFields.setCompanyId(companyId);

        when(companyCustomerService.getMandatoryFields(name, companyId)).thenReturn(mandatoryFields);

        ResponseEntity<CompanyCustomerMandatoryFields> result = companyCustomerAPI.getMandatoryFields(name, companyId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(name, result.getBody().getName());
        verify(companyCustomerService).getMandatoryFields(name, companyId);
    }

    @Test
    void testGetShowFields_Success() {
        String name = "test-field";
        Long companyId = 1L;
        CompanyCustomerShowFields showFields = new CompanyCustomerShowFields();
        showFields.setName(name);
        showFields.setCompanyId(companyId);

        when(companyCustomerService.getShowFields(name, companyId)).thenReturn(showFields);

        ResponseEntity<CompanyCustomerShowFields> result = companyCustomerAPI.getShowFields(name, companyId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(name, result.getBody().getName());
        verify(companyCustomerService).getShowFields(name, companyId);
    }
}
