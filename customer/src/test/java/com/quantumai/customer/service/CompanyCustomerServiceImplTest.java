package com.quantumai.customer.service;

import com.quantumai.customer.dto.*;
import com.quantumai.customer.entity.*;
import com.quantumai.customer.entity.IdGenerator.CompanyCustomerIdTable;
import com.quantumai.customer.exception.EmailAlreadyExistsException;
import com.quantumai.customer.exception.ExtraFieldAlreadyPresentException;
import com.quantumai.customer.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyCustomerServiceImplTest {

    @Mock
    private CompanyCustomerRepository companyCustomerRepository;

    @Mock
    private CompanyCustomerCategoryRepository companyCustomerCategoryRepository;

    @Mock
    private CompanyCustomerExtraFieldNameRepository extraFieldNameRepository;

    @Mock
    private CompanyCustomerMandatoryFieldsRepository mandatoryFieldsRepository;

    @Mock
    private CompanyCustomerShowFieldsRepository showFieldsRepository;

    @Mock
    private CompanyCustomerIdTableRepository idTableRepository;

    @Mock
    private CompanyCustomerExtraFieldsRepository extraFieldsRepository;

    @Mock
    private CompanyCustomerFileRepository companyCustomerFileRepository;

    @Mock
    private CompanyCustomerCategoryIdGeneratorRepository companyCustomerCategoryIdGeneratorRepository;

    @InjectMocks
    private CompanyCustomerServiceImpl companyCustomerService;

    private CompanyCustomerDTO testCustomerDTO;
    private CompanyCustomer testCustomer;
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        modelMapper = new ModelMapper();
        
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

        testCustomer = new CompanyCustomer();
        testCustomer.setId("test-id");
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("test@example.com");
        testCustomer.setCompanyId(1L);
        testCustomer.setPhone("1234567890");
        testCustomer.setAddress("123 Test St");
        testCustomer.setCity("Test City");
        testCustomer.setState("Test State");
        testCustomer.setZipCode(12345);
        testCustomer.setStatus("active");
        testCustomer.setCategory("test-category");
        testCustomer.setCompanyCustomerId(1);
        testCustomer.setUpdatedAt(LocalDateTime.now().toString());
    }

    @Test
    void testAddCustomer_Success_NewIdTable() throws EmailAlreadyExistsException {
        // Arrange
        testCustomerDTO.setCompanyCustomerId(null);
        when(companyCustomerRepository.findByEmailAndCompanyId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(idTableRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        when(companyCustomerRepository.save(any(CompanyCustomer.class))).thenReturn(testCustomer);
        when(idTableRepository.save(any(CompanyCustomerIdTable.class))).thenReturn(new CompanyCustomerIdTable());

        // Act
        CompanyCustomerDTO result = companyCustomerService.addCustomer(testCustomerDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testCustomerDTO.getName(), result.getName());
        assertEquals(testCustomerDTO.getEmail(), result.getEmail());
        verify(companyCustomerRepository).save(any(CompanyCustomer.class));
        verify(idTableRepository).save(any(CompanyCustomerIdTable.class));
    }

    @Test
    void testAddCustomer_Success_ExistingIdTable() throws EmailAlreadyExistsException {
        // Arrange
        testCustomerDTO.setCompanyCustomerId(null);
        CompanyCustomerIdTable existingIdTable = new CompanyCustomerIdTable();
        existingIdTable.setTableId(5);
        existingIdTable.setCompanyId(1L);

        when(companyCustomerRepository.findByEmailAndCompanyId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(idTableRepository.findByCompanyId(1L)).thenReturn(Optional.of(existingIdTable));
        when(companyCustomerRepository.save(any(CompanyCustomer.class))).thenReturn(testCustomer);
        when(idTableRepository.save(any(CompanyCustomerIdTable.class))).thenReturn(existingIdTable);

        // Act
        CompanyCustomerDTO result = companyCustomerService.addCustomer(testCustomerDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testCustomerDTO.getName(), result.getName());
        verify(companyCustomerRepository).save(any(CompanyCustomer.class));
        verify(idTableRepository).save(existingIdTable);
    }

    @Test
    void testAddCustomer_EmailAlreadyExists() {
        // Arrange
        when(companyCustomerRepository.findByEmailAndCompanyId(anyString(), anyLong()))
                .thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            companyCustomerService.addCustomer(testCustomerDTO);
        });

        verify(companyCustomerRepository, never()).save(any(CompanyCustomer.class));
    }

    @Test
    void testGetCustomer_Success() {
        // Arrange
        String customerId = "test-id";
        when(companyCustomerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

        // Act
        CompanyCustomerDTO result = companyCustomerService.getCustomer(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getName(), result.getName());
        assertEquals(testCustomer.getEmail(), result.getEmail());
        verify(companyCustomerRepository).findById(customerId);
    }

    @Test
    void testGetAllCustomer_Success() {
        // Arrange
        Long companyId = 1L;
        List<CompanyCustomer> customerList = Arrays.asList(testCustomer);
        when(companyCustomerRepository.findByCompanyId(companyId)).thenReturn(customerList);

        // Act
        List<CompanyCustomerDTO> result = companyCustomerService.getAllCustomer(companyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCustomer.getName(), result.get(0).getName());
        verify(companyCustomerRepository).findByCompanyId(companyId);
    }

    @Test
    void testUpdateCustomer_Success() throws EmailAlreadyExistsException {
        // Arrange
        when(companyCustomerRepository.findById(testCustomerDTO.getId())).thenReturn(Optional.of(testCustomer));
        when(companyCustomerRepository.findByEmailAndCompanyId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(companyCustomerRepository.save(any(CompanyCustomer.class))).thenReturn(testCustomer);

        // Act
        assertDoesNotThrow(() -> companyCustomerService.updateCustomer(testCustomerDTO));

        // Assert
        verify(companyCustomerRepository).save(any(CompanyCustomer.class));
    }

    @Test
    void testUpdateCustomer_CustomerNotFound() {
        // Arrange
        when(companyCustomerRepository.findById(testCustomerDTO.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            companyCustomerService.updateCustomer(testCustomerDTO);
        });
    }

    @Test
    void testUpdateCustomer_EmailAlreadyExists() {
        // Arrange
        CompanyCustomer existingCustomer = new CompanyCustomer();
        existingCustomer.setId("different-id");
        existingCustomer.setEmail(testCustomerDTO.getEmail());
        existingCustomer.setCompanyId(testCustomerDTO.getCompanyId());

        when(companyCustomerRepository.findById(testCustomerDTO.getId())).thenReturn(Optional.of(testCustomer));
        when(companyCustomerRepository.findByEmailAndCompanyId(anyString(), anyLong()))
                .thenReturn(Optional.of(existingCustomer));

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            companyCustomerService.updateCustomer(testCustomerDTO);
        });
    }

    @Test
    void testDeleteCustomer_Success() {
        // Arrange
        String customerId = "test-id";
        when(companyCustomerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));

        // Act
        companyCustomerService.deleteCustomer(customerId);

        // Assert
        verify(companyCustomerRepository).delete(testCustomer);
    }

    @Test
    void testAddCompanyCustomerExtraField_Success() throws ExtraFieldAlreadyPresentException {
        // Arrange
        CompanyCustomerExtraFieldNameDTO extraFieldDTO = new CompanyCustomerExtraFieldNameDTO();
        extraFieldDTO.setName("Extra Field");
        extraFieldDTO.setType("text");
        extraFieldDTO.setCompanyId(1L);

        when(extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(anyString(), anyLong())).thenReturn(null);
        when(extraFieldNameRepository.save(any(CompanyCustomerExtraFieldName.class)))
                .thenReturn(new CompanyCustomerExtraFieldName());

        // Act
        assertDoesNotThrow(() -> companyCustomerService.addCompanyCustomerExtraField(extraFieldDTO));

        // Assert
        verify(extraFieldNameRepository).save(any(CompanyCustomerExtraFieldName.class));
    }

    @Test
    void testAddCompanyCustomerExtraField_AlreadyExists() {
        // Arrange
        CompanyCustomerExtraFieldNameDTO extraFieldDTO = new CompanyCustomerExtraFieldNameDTO();
        extraFieldDTO.setName("Extra Field");
        extraFieldDTO.setCompanyId(1L);

        CompanyCustomerExtraFieldName existingField = new CompanyCustomerExtraFieldName();
        when(extraFieldNameRepository.findByNameIgnoreCaseAndCompanyId(anyString(), anyLong()))
                .thenReturn(existingField);

        // Act & Assert
        assertThrows(ExtraFieldAlreadyPresentException.class, () -> {
            companyCustomerService.addCompanyCustomerExtraField(extraFieldDTO);
        });

        verify(extraFieldNameRepository, never()).save(any(CompanyCustomerExtraFieldName.class));
    }

    @Test
    void testGetCompanyCustomerExtraField_Success() {
        // Arrange
        Long companyId = 1L;
        CompanyCustomerExtraFieldName extraField = new CompanyCustomerExtraFieldName();
        extraField.setName("Extra Field");
        extraField.setType("text");
        List<CompanyCustomerExtraFieldName> extraFields = Arrays.asList(extraField);

        when(extraFieldNameRepository.findByCompanyId(companyId)).thenReturn(extraFields);

        // Act
        List<CompanyCustomerExtraFieldNameDTO> result = companyCustomerService.getCompanyCustomerExtraField(companyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Extra Field", result.get(0).getName());
        verify(extraFieldNameRepository).findByCompanyId(companyId);
    }

    @Test
    void testDeleteCompanyCustomerExtraField_Success() throws Exception {
        // Arrange
        String fieldId = "field-id";
        CompanyCustomerExtraFieldName extraField = new CompanyCustomerExtraFieldName();
        extraField.setName("Extra Field");

        when(extraFieldNameRepository.findById(fieldId)).thenReturn(Optional.of(extraField));
        when(extraFieldsRepository.findByName(anyString())).thenReturn(new ArrayList<>());

        // Act
        companyCustomerService.deleteCompanyCustomerExtraField(fieldId);

        // Assert
        verify(extraFieldNameRepository).deleteById(fieldId);
    }

    @Test
    void testUpdateMandatoryFields_NewField() {
        // Arrange
        CompanyCustomerMandatoryFields mandatoryFields = new CompanyCustomerMandatoryFields();
        mandatoryFields.setName("Test Field");
        mandatoryFields.setCompanyId(1L);

        when(mandatoryFieldsRepository.findByNameAndCompanyId(anyString(), anyLong()))
                .thenReturn(Optional.empty());
        when(mandatoryFieldsRepository.save(any(CompanyCustomerMandatoryFields.class)))
                .thenReturn(mandatoryFields);

        // Act
        companyCustomerService.updateMandatoryFields(mandatoryFields);

        // Assert
        verify(mandatoryFieldsRepository).save(mandatoryFields);
    }

    @Test
    void testUpdateMandatoryFields_ExistingField() {
        // Arrange
        CompanyCustomerMandatoryFields existingField = new CompanyCustomerMandatoryFields();
        existingField.setId("existing-id");
        existingField.setName("Test Field");
        existingField.setCompanyId(1L);

        CompanyCustomerMandatoryFields updatedField = new CompanyCustomerMandatoryFields();
        updatedField.setName("Test Field");
        updatedField.setCompanyId(1L);

        when(mandatoryFieldsRepository.findByNameAndCompanyId(anyString(), anyLong()))
                .thenReturn(Optional.of(existingField));
        when(mandatoryFieldsRepository.save(any(CompanyCustomerMandatoryFields.class)))
                .thenReturn(updatedField);

        // Act
        companyCustomerService.updateMandatoryFields(updatedField);

        // Assert
        assertEquals("existing-id", updatedField.getId());
        verify(mandatoryFieldsRepository).save(updatedField);
    }

    @Test
    void testGetMandatoryFields_Found() {
        // Arrange
        String name = "Test Field";
        Long companyId = 1L;
        CompanyCustomerMandatoryFields mandatoryFields = new CompanyCustomerMandatoryFields();
        mandatoryFields.setName(name);
        mandatoryFields.setCompanyId(companyId);

        when(mandatoryFieldsRepository.findByNameAndCompanyId(name, companyId))
                .thenReturn(Optional.of(mandatoryFields));

        // Act
        CompanyCustomerMandatoryFields result = companyCustomerService.getMandatoryFields(name, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(name, result.getName());
        verify(mandatoryFieldsRepository).findByNameAndCompanyId(name, companyId);
    }

    @Test
    void testGetMandatoryFields_NotFound() {
        // Arrange
        String name = "Test Field";
        Long companyId = 1L;

        when(mandatoryFieldsRepository.findByNameAndCompanyId(name, companyId))
                .thenReturn(Optional.empty());

        // Act
        CompanyCustomerMandatoryFields result = companyCustomerService.getMandatoryFields(name, companyId);

        // Assert
        assertNull(result);
        verify(mandatoryFieldsRepository).findByNameAndCompanyId(name, companyId);
    }

    @Test
    void testAddCompanyCustomerFile_Success() throws IOException {
        // Arrange
        String companyCustomerId = "customer-id";
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getBytes()).thenReturn("test content".getBytes());

        CompanyCustomerFile savedFile = new CompanyCustomerFile();
        savedFile.setId("file-id");
        savedFile.setFileName("test.txt");
        savedFile.setCompanyCustomerId(companyCustomerId);

        when(companyCustomerFileRepository.save(any(CompanyCustomerFile.class))).thenReturn(savedFile);

        // Act
        CompanyCustomerFile result = companyCustomerService.addCompanyCustomerFile(mockFile, companyCustomerId);

        // Assert
        assertNotNull(result);
        assertEquals("file-id", result.getId());
        assertEquals("test.txt", result.getFileName());
        verify(companyCustomerFileRepository).save(any(CompanyCustomerFile.class));
    }

    @Test
    void testAddCompanyCustomerFile_IOException() throws IOException {
        // Arrange
        String companyCustomerId = "customer-id";
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        when(mockFile.getBytes()).thenThrow(new IOException("File read error"));

        // Act & Assert
        assertThrows(IOException.class, () -> {
            companyCustomerService.addCompanyCustomerFile(mockFile, companyCustomerId);
        });
    }

    @Test
    void testGetCompanyCustomerFile_Success() {
        // Arrange
        String companyCustomerId = "customer-id";
        CompanyCustomerFile file = new CompanyCustomerFile();
        file.setFileName("test.txt");
        file.setCompanyCustomerId(companyCustomerId);
        List<CompanyCustomerFile> files = Arrays.asList(file);

        when(companyCustomerFileRepository.findByCompanyCustomerId(companyCustomerId)).thenReturn(files);

        // Act
        List<CompanyCustomerFileDTO> result = companyCustomerService.getCompanyCustomerFile(companyCustomerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.txt", result.get(0).getFileName());
        verify(companyCustomerFileRepository).findByCompanyCustomerId(companyCustomerId);
    }

    @Test
    void testGetCompanyCustomerFile_EmptyList() {
        // Arrange
        String companyCustomerId = "customer-id";
        when(companyCustomerFileRepository.findByCompanyCustomerId(companyCustomerId))
                .thenReturn(new ArrayList<>());

        // Act
        List<CompanyCustomerFileDTO> result = companyCustomerService.getCompanyCustomerFile(companyCustomerId);

        // Assert
        assertNull(result);
        verify(companyCustomerFileRepository).findByCompanyCustomerId(companyCustomerId);
    }

    @Test
    void testDownloadFile_Success() {
        // Arrange
        String fileId = "file-id";
        CompanyCustomerFile file = new CompanyCustomerFile();
        file.setId(fileId);
        file.setFileName("test.txt");
        file.setFile("test content".getBytes());

        when(companyCustomerFileRepository.findById(fileId)).thenReturn(Optional.of(file));

        // Act
        CompanyCustomerFileDTO result = companyCustomerService.downloadFile(fileId);

        // Assert
        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        verify(companyCustomerFileRepository).findById(fileId);
    }

    @Test
    void testDeleteFile_Success() {
        // Arrange
        String fileId = "file-id";

        // Act
        companyCustomerService.deleteFile(fileId);

        // Assert
        verify(companyCustomerFileRepository).deleteById(fileId);
    }

    @Test
    void testGetCompanyCustomerByLocalId_Success() {
        // Arrange
        Integer localId = 1;
        Long companyId = 1L;

        when(companyCustomerRepository.findByCompanyCustomerIdAndCompanyId(localId, companyId))
                .thenReturn(testCustomer);

        // Act
        CompanyCustomerDTO result = companyCustomerService.getCompanyCustomerByLocalId(localId, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(testCustomer.getName(), result.getName());
        verify(companyCustomerRepository).findByCompanyCustomerIdAndCompanyId(localId, companyId);
    }

    @Test
    void testGetCompanyCustomerByLocalId_NotFound() {
        // Arrange
        Integer localId = 1;
        Long companyId = 1L;

        when(companyCustomerRepository.findByCompanyCustomerIdAndCompanyId(localId, companyId))
                .thenReturn(null);

        // Act
        CompanyCustomerDTO result = companyCustomerService.getCompanyCustomerByLocalId(localId, companyId);

        // Assert
        assertNull(result);
        verify(companyCustomerRepository).findByCompanyCustomerIdAndCompanyId(localId, companyId);
    }

    @Test
    void testAddExtraFields_Success() {
        // Arrange
        CompanyCustomerExtraFieldsDTO extraFieldsDTO = new CompanyCustomerExtraFieldsDTO();
        extraFieldsDTO.setName("Extra Field");
        extraFieldsDTO.setValue("Extra Value");
        extraFieldsDTO.setCompanyCustomerId("customer-id");

        CompanyCustomerExtraFields savedField = new CompanyCustomerExtraFields();
        when(extraFieldsRepository.save(any(CompanyCustomerExtraFields.class))).thenReturn(savedField);

        // Act
        assertDoesNotThrow(() -> companyCustomerService.addExtraFields(extraFieldsDTO));

        // Assert
        verify(extraFieldsRepository).save(any(CompanyCustomerExtraFields.class));
    }

    @Test
    void testGetExtraFields_Success() {
        // Arrange
        String customerId = "customer-id";
        CompanyCustomerExtraFields extraField = new CompanyCustomerExtraFields();
        extraField.setName("Extra Field");
        extraField.setValue("Extra Value");
        List<CompanyCustomerExtraFields> extraFields = Arrays.asList(extraField);

        when(extraFieldsRepository.findByCompanyCustomerId(customerId)).thenReturn(extraFields);

        // Act
        List<CompanyCustomerExtraFieldsDTO> result = companyCustomerService.getExtraFields(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Extra Field", result.get(0).getName());
        verify(extraFieldsRepository).findByCompanyCustomerId(customerId);
    }

    @Test
    void testGetExtraFields_EmptyList() {
        // Arrange
        String customerId = "customer-id";
        when(extraFieldsRepository.findByCompanyCustomerId(customerId)).thenReturn(new ArrayList<>());

        // Act
        List<CompanyCustomerExtraFieldsDTO> result = companyCustomerService.getExtraFields(customerId);

        // Assert
        assertNull(result);
        verify(extraFieldsRepository).findByCompanyCustomerId(customerId);
    }

    @Test
    void testDeleteExtraFields_Success() throws Exception {
        // Arrange
        String fieldId = "field-id";
        CompanyCustomerExtraFields extraField = new CompanyCustomerExtraFields();
        when(extraFieldsRepository.findById(fieldId)).thenReturn(Optional.of(extraField));

        // Act
        assertDoesNotThrow(() -> companyCustomerService.deleteExtraFields(fieldId));

        // Assert
        verify(extraFieldsRepository).delete(extraField);
    }

    @Test
    void testDeleteExtraFields_NotFound() {
        // Arrange
        String fieldId = "field-id";
        when(extraFieldsRepository.findById(fieldId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            companyCustomerService.deleteExtraFields(fieldId);
        });
    }
}
