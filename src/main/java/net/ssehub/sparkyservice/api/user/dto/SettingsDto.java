package net.ssehub.sparkyservice.api.user.dto;

import javax.annotation.Nonnull;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import net.ssehub.sparkyservice.api.jpa.user.PersonalSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;

public class SettingsDto {
    
    public static void applyPersonalSettings(@Nonnull User user, @Nonnull SettingsDto settings) {
        PersonalSettings dbSettings = user.getProfileConfiguration();
        dbSettings.setEmail_address(settings.email_address);
        dbSettings.setWantsAi(settings.wantsAi);
        dbSettings.setEmail_receive(settings.email_receive);
    }
    
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
