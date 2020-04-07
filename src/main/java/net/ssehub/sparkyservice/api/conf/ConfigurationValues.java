package net.ssehub.sparkyservice.api.conf;

import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Singleton class for configuration values loaded from spring properties.
 * @author marcel
 */
@Component
public class ConfigurationValues {

    @Configuration
    @ConfigurationProperties(prefix = "zuul")
    public class ZuulRoutes {
        private Map<String, String> routes;

        public Map<String, String> getRoutes() {
            return routes;
        }

        public void setRoutes(Map<String, String> routes) {
            this.routes = routes;
        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "jwt")
    public class JwtSettings {
        private String secret;
        private String tokenHeader;
        private String tokenType;
        private String tokenIssuer;
        private String tokenPrefix;
        private String tokenAudience;

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
    public static final String AUTH_LOGIN_URL = ControllerPath.AUTHENTICATION_AUTH;
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.token.header}")
    private String jwtTokenHeader;
    
    @Value("${jwt.token.prefix}")
    private String jwtTokenPrefix;

    @Value("${jwt.token.type}")
    private String jwtTokenType;
    
    @Value("${jwt.token.issuer:}")
    private String jwtTokenIssuer;
    
    @Value("${jwt.token.audience:}")
    private String jwtTokenAudience;

    @Autowired
    private ZuulRoutes zuulRoutes;

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

    public Map<String, String> getZuulRoutes() {
        return zuulRoutes.getRoutes();
    }

    public void setZuulRoutes(ZuulRoutes routes) {
        this.zuulRoutes = routes;
    }

}