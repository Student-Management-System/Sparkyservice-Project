package net.ssehub.sparkyservice.api.testconf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.ssehub.sparkyservice.api.user.dto.UserDto;

public class SparkyAssertions {
    public static void assertDtoEquals(UserDto dto1, UserDto dto2) {
        assertAll(
                () -> assertEquals(dto1.username, dto2.username),
                () -> assertEquals(dto1.realm, dto2.realm),
                () -> assertEquals(dto1.settings.email_address, dto2.settings.email_address)
            );
    }
}
