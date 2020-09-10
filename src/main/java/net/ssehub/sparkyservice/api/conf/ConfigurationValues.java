package net.ssehub.sparkyservice.api.conf;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Provides configuration bean definition loaded from property files through spring.
 * 
 * @author marcel
 */
@Component
public class ConfigurationValues {
    public static final String AUTH_LOGIN_URL = ControllerPath.AUTHENTICATION_AUTH;

    /**
     * Provides Zuul configuration as bean definition.
     * 
     * @author marcel
     */
    @Configuration
    @ConfigurationProperties(prefix = "zuul")
    public static class ZuulRoutes {
        private Map<String, String> routes;

        /**
         * Defined zuul routes as map. 
         * Keys: acl, url
         * @return configured routes
         */
        public Map<String, String> getRoutes() {
            return routes;
        }

        /**
         * Typically only used by spring or tests.
         * 
         * @param routes
         */
        public void setRoutes(Map<String, String> routes) {
            this.routes = routes;
        }

        /**
         * Returns all configured paths. 
         * 
         * @return Collection of configured paths without configuration values or keys
         */
        public Collection<String> getConfiguredPaths() {
            Set<String> configuredPathes = new HashSet<String>();
            if (routes != null) {
                for (String singleConfiguration: routes.keySet()) { // example value: other.acl or other.url
                    String path = singleConfiguration.split("\\.")[0]; // testpath.acl, testpath.url -> testpath
                    configuredPathes.add(path); // add to a set => no duplicates
                }
            }
            return configuredPathes;
        }
    }

    /**
     * Provides JWT configuration (typically loaded through spring on startup and is provided as bean).
     * 
     * @author marcel
     */
    @Configuration
    @ConfigurationProperties(prefix = "jwt")
    public static class JwtSettings {
        private String secret;
        private String header;
        private String type;
        private String issuer;
        private String prefix;
        private String audience;

        /**
         * .
         * @return Used header like "authorization"
         */
        public String getHeader() {
            return header;
        }

        /**
         * See {@link #getHeader()}.
         * 
         * @param tokenHeader
         */
        public void setHeader(String tokenHeader) {
            this.header = tokenHeader;
        }

        /**
         * The token type. Or "typ" according to specification RFC 7519 5.1
         * 
         * @return The token type - probably always "JWT"
         */
        public String getType() {
            return type;
        }

        /**
         * See {@link #getType()}.
         * 
         * @param tokenType
         */
        public void setType(String tokenType) {
            this.type = tokenType;
        }

        /**
         * JWT Token issuer. Can be used for "iss" according to specification RFC 7519 4.1.1 
         * 
         * @return Token issuer
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * See {@link #getIssuer()}.
         * 
         * @param tokenIssuer
         */
        public void setIssuer(String tokenIssuer) {
            this.issuer = tokenIssuer;
        }

        /**
         * A keyword which should be in front of each token. 
         * 
         * @return Prefix keyword like "bearer"
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * See {@link #getPrefix()}.
         * 
         * @param tokenPrefix
         */
        public void setPrefix(String tokenPrefix) {
            this.prefix = tokenPrefix;
        }

        /**
         * Token audience "aud" according to specification RFC 7519 4.1.3.
         * 
         * @return Audience
         */
        public String getAudience() {
            return audience;
        }

        /**
         * See {@link #getAudience()}.
         * 
         * @param tokenAudience
         */
        public void setAudience(String tokenAudience) {
            this.audience = tokenAudience;
        }

        /**
         * Secret is used to sign a JWT token. Keep this secret!
         * 
         * @return Secret to sign tokens
         */
        public @Nonnull String getSecret() {
            final String jwtSecret2 = secret;
            if (jwtSecret2 != null) {
                return jwtSecret2;
            } else {
                throw new RuntimeException("Spring did not receive the JWT Secret!");
            }
        }

        /**
         * See {@link #getSecret()}.
         * 
         * @param secret
         */
        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

}