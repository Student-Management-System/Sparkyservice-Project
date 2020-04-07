package net.ssehub.sparkyservice.api.conf;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Singleton class for configuration values loaded from spring properties.
 * @author marcel
 */
@Component
public class ConfigurationValues {
    
    public static final String AUTH_LOGIN_URL = ControllerPath.AUTHENTICATION_AUTH;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.token.header}")
    private String jwtTokenHeader;
    
    @Value("${jwt.token.prefix}")
    private String jwtTokenPrefix;

    @Value("${jwt.token.type}")
    private String jwtTokenType;
    
    @Value("${jwt.token.issuer}")
    private String jwtTokenIssuer;
    
    @Value("${jwt.token.audience}")
    private String jwtTokenAudience;
    
    public @Nonnull String getJwtSecret() {
        final String jwtSecret2 = jwtSecret;
        if (jwtSecret2 != null) {
            return jwtSecret2;
        } else {
            throw new RuntimeException("Spring did not receive the JWT Secret!");
        }
    }

    public String getJwtTokenHeader() {
        return jwtTokenHeader;
    }

    public String getJwtTokenPrefix() {
        return jwtTokenPrefix;
    }

    public String getJwtTokenType() {
        return jwtTokenType;
    }

    public String getJwtTokenIssuer() {
        return jwtTokenIssuer;
    }

    public String getJwtTokenAudience() {
        return jwtTokenAudience;
    }

}