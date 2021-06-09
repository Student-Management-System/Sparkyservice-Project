package net.ssehub.sparkyservice.api.user.dto;

import java.io.Serializable;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.validation.ValidPassword;

/**
 * DTO for {@link SparkyUser}.
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class UserDto implements Serializable {

    private static final long serialVersionUID = 4775708076296820879L;

    /**
     * DTO for passwords or changing Passwords.
     * 
     * @author marcel
     *
     */
    public static class ChangePasswordDto {

        public String oldPassword;

        @NotBlank
        @ValidPassword
        public String newPassword; 
    }

    @NotBlank
    public String username;

    public String fullName;

    @NotNull
    public UserRealm realm;

    /**
     * Not necessary. Won't have any effect if the user is in any other realm than the local one (like LDAP). 
     */
    @Valid
    public ChangePasswordDto passwordDto;

    @Valid
    @NotNull
    public SettingsDto settings;

    public UserRole role;

    public LocalDate expirationDate;
}
