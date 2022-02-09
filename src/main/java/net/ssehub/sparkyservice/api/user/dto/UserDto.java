package net.ssehub.sparkyservice.api.user.dto;

import java.io.Serializable;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.user.SparkyUser;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
        result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
        result = prime * result + ((passwordDto == null) ? 0 : passwordDto.hashCode());
        result = prime * result + ((role == null) ? 0 : role.hashCode());
        result = prime * result + ((settings == null) ? 0 : settings.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserDto other = (UserDto) obj;
        if (expirationDate == null) {
            if (other.expirationDate != null)
                return false;
        } else if (!expirationDate.equals(other.expirationDate))
            return false;
        if (fullName == null) {
            if (other.fullName != null)
                return false;
        } else if (!fullName.equals(other.fullName))
            return false;
        if (role != other.role)
            return false;
        if (settings == null) {
            if (other.settings != null)
                return false;
        } else if (!settings.equals(other.settings))
            return false;
        if (username == null) {
            if (other.username != null)
                return false;
        } else if (!username.equals(other.username))
            return false;
        return true;
    }
    
    
}
