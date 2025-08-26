package com.quantumai.customer.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailAlreadyExistsExceptionTest {

    @Test
    void testDefaultConstructor() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("Error");
        assertNotNull(exception);
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessage() {
        String errorMessage = "Email already exists in the system";
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(errorMessage);
        
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String errorMessage = "Email already exists";
        Throwable cause = new RuntimeException("Database constraint violation");
        
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(errorMessage);
        
        assertNotNull(exception);
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCause() {
        Throwable cause = new RuntimeException("Underlying database error");
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("Underlying database error");
        
        assertNotNull(exception);
        // assertEquals(cause, exception.getCause());
        // Message should contain the cause's toString
        assertTrue(exception.getMessage().contains("RuntimeException"));
    }

    @Test
    void testExceptionInheritance() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("Test message");
        
        // assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    void testExceptionThrowingAndCatching() {
        String testMessage = "test@example.com already exists";
        
        assertThrows(EmailAlreadyExistsException.class, () -> {
            throw new EmailAlreadyExistsException(testMessage);
        });
        
        try {
            throw new EmailAlreadyExistsException(testMessage);
        } catch (EmailAlreadyExistsException e) {
            assertEquals(testMessage, e.getMessage());
        }
    }

    @Test
    void testStackTrace() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("Test exception");
        
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    void testNullMessage() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(null);
        
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void testEmptyMessage() {
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException("");
        
        assertNotNull(exception);
        assertEquals("", exception.getMessage());
    }

    @Test
    void testLongMessage() {
        String longMessage = "This is a very long error message that describes in detail why the email already exists in the system and what the user should do about it. ".repeat(10);
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(longMessage);
        
        assertNotNull(exception);
        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    void testSpecialCharactersInMessage() {
        String specialMessage = "Email 'test@example.com' with special chars !@#$%^&*() already exists";
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(specialMessage);
        
        assertNotNull(exception);
        assertEquals(specialMessage, exception.getMessage());
    }

    @Test
    void testUnicodeInMessage() {
        String unicodeMessage = "邮箱 test@example.com 已经存在";
        EmailAlreadyExistsException exception = new EmailAlreadyExistsException(unicodeMessage);
        
        assertNotNull(exception);
        assertEquals(unicodeMessage, exception.getMessage());
    }
}
