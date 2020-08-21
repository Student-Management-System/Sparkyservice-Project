package net.ssehub.sparkyservice.api.user.dto;

import javax.validation.constraints.NotNull;

/**
 * DTO for settings of one user.
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class SettingsDto {
    
    @NotNull // TODO Marcel: Test if necessary
    public boolean wantsAi;
    
    /**
     * User can decide if he wants receive email from the system.
     */
    @NotNull // TODO Marcel: Test if necessary (boolean)
    public boolean emailReceive;
    
    //@Email // TODO Marcel: fix exception
    public String emailAddress;

    public String payload;
}
