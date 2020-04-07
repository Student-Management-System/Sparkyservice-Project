package net.ssehub.sparkyservice.api.conf;

import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationValues {
    public static final String AUTH_LOGIN_URL = ControllerPath.AUTHENTICATION_AUTH;

    @Configuration
    @ConfigurationProperties(prefix = "zuul")
    public static class ZuulRoutes {
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
    public static class JwtSettings {
        private String secret;
        private String header;
        private String type;
        private String issuer;
        private String prefix;
        private String audience;

        public String getHeader() {
            return header;
        }

        public void setHeader(String tokenHeader) {
            this.header = tokenHeader;
        }

        public String getType() {
            return type;
        }

        public void setType(String tokenType) {
            this.type = tokenType;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String tokenIssuer) {
            this.issuer = tokenIssuer;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String tokenPrefix) {
            this.prefix = tokenPrefix;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String tokenAudience) {
            this.audience = tokenAudience;
        }

        public @Nonnull String getSecret() {
            final String jwtSecret2 = secret;
            if (jwtSecret2 != null) {
                return jwtSecret2;
            } else {
                throw new RuntimeException("Spring did not receive the JWT Secret!");
            }
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

}