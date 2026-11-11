package com.bookmuse.app.dto;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ValidationTest {
    @Test void shouldEnforcePasswordLengthAndProgressBounds() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            var v=factory.getValidator();
            for(int length:new int[]{0,7,21}) assertFalse(v.validate(new Requests.Register("r@example.com","x".repeat(length),"Reader")).isEmpty());
            for(int length:new int[]{8,20}) assertTrue(v.validate(new Requests.Register("r@example.com","x".repeat(length),"Reader")).isEmpty());
            assertFalse(v.validate(new Requests.LibraryInput("READING",101)).isEmpty());
            assertFalse(v.validate(new Requests.LibraryInput("INVALID",50)).isEmpty());
        }
    }
}
