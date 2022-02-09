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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((emailAddress == null) ? 0 : emailAddress.hashCode());
        result = prime * result + (emailReceive ? 1231 : 1237);
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        result = prime * result + (wantsAi ? 1231 : 1237);
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
        SettingsDto other = (SettingsDto) obj;
        if (emailAddress == null) {
            if (other.emailAddress != null)
                return false;
        } else if (!emailAddress.equals(other.emailAddress))
            return false;
        if (emailReceive != other.emailReceive)
            return false;
        if (payload == null) {
            if (other.payload != null)
                return false;
        } else if (!payload.equals(other.payload))
            return false;
        if (wantsAi != other.wantsAi)
            return false;
        return true;
    }
    
    
}
