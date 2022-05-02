package net.ssehub.sparkyservice.api.useraccess.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Token identification DTO. 
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class JwtDto implements Serializable {
    private static final long serialVersionUID = 6425173137954338569L;
    public String key;
    public String token;
    public LocalDateTime expiration;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
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
        JwtDto other = (JwtDto) obj;
        if (expiration == null) {
            if (other.expiration != null)
                return false;
        } else if (!expiration.equals(other.expiration))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (token == null) {
            if (other.token != null)
                return false;
        } else if (!token.equals(other.token))
            return false;
        return true;
    }

    
    
}