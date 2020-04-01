package net.ssehub.sparkyservice.api.user.dto;

import javax.validation.constraints.NotNull;

public class SettingsDto {
    
    @NotNull // TODO Marcel: Test if necessary
    public boolean wantsAi;
    
    /**
     * User can decide if he wants receive email from the system.
     */
    @NotNull // TODO Marcel: Test if necessary (boolean)
    public boolean email_receive;
    
    //@Email // TODO Marcel: fix exception
    public String email_address;
}
