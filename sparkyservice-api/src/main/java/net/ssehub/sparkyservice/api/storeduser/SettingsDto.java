package net.ssehub.sparkyservice.api.storeduser;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;

public class SettingsDto {
    
    @AssertTrue
    public boolean userWantAI;
    
    public boolean email_subscription;
    
    @Email
    public String email_address;
}
