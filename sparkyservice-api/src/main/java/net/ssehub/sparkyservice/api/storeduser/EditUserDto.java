package net.ssehub.sparkyservice.api.storeduser;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class EditUserDto {
    public class ChangePasswordDto {
        @NotBlank
        public String oldPassword;
        
        @NotBlank
        public String newPassword; 
    }
    
    @NotBlank
    public String username;
    
    @NotBlank
    public String realm;
    
    /**
     * Not necessary. Won't have any effect if the user is in any other realm than the local one (like LDAP). 
     */
    @Valid
    public ChangePasswordDto passwordDto;
    
    @Valid
    @NotNull
    public SettingsDto settings;
}
