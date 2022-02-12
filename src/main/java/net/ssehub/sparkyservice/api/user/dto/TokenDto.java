package net.ssehub.sparkyservice.api.user.dto;

import java.io.Serializable;

/**
 * Token identification DTO. 
 * 
 * @author marcel
 */
//checkstyle: stop visibility modifier check
public class TokenDto implements Serializable {
    private static final long serialVersionUID = 6425173137954338569L;
    public String token;
    public String expiration;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((expiration == null) ? 0 : expiration.hashCode());
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
            
        if (getClass() != obj.getClass()) {
            return false;
        }
            
        TokenDto other = (TokenDto) obj;
        if (expiration == null) {
            if (other.expiration != null) {
                return false;
            }
        } else if (!expiration.equals(other.expiration)) {
            return false;
        }
        if (token == null) {
            if (other.token != null) {
                return false;
            }
        } else if (!token.equals(other.token)) {
            return false;
        }
        return true;
    }
    
    
}