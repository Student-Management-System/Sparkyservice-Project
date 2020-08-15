package net.ssehub.sparkyservice.api.testconf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ssehub.sparkyservice.api.user.dto.UserDto;

/**
 * Assertion helper for unit tests in SparkyService. 
 * 
 * @author marcel
 */
public class SparkyAssertions {
    public static void assertDtoEquals(UserDto dto1, UserDto dto2) {
        assertAll(
                () -> assertEquals(dto1.username, dto2.username),
                () -> assertEquals(dto1.realm, dto2.realm),
                () -> assertEquals(dto1.settings.email_address, dto2.settings.email_address)
            );
    }

    /**
     * Checks if two dtos have the same values
     * 
     * @param dto1
     * @param dto2
     */
    public static void assertDtoValuesEquals(UserDto dto1, UserDto dto2) {
        assertAll(
                () -> assertEquals(dto1.username, dto2.username),
                () -> assertEquals(dto1.realm, dto2.realm),
                () -> assertEquals(dto1.role, dto2.role, "Role not correctly changed or transformed"), 
                () -> assertEquals(dto1.settings.email_address, dto2.settings.email_address),
                () -> assertEquals(dto1.expirationDate, dto2.expirationDate),
                () -> assertEquals(dto1.fullName, dto2.fullName)
            );
    }

}