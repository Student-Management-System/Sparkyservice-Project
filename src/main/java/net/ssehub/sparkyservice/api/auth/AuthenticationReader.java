package net.ssehub.sparkyservice.api.auth;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;

/**
 * Authentication reader which makes working with JWT token and Authentication objects easier.
 * 
 * @author marcel
 */
public class AuthenticationReader {

    private static Logger log = LoggerFactory.getLogger(AuthenticationReader.class);

    private final JwtSettings jwtConf;

    @Nullable
    private final String authHeader;

    /**
     * Authentication reader for a specific JWT token. 
     * 
     * @param conf
     * @param authorizationHeader - From the request where the JWT Token
     *                              is stored
     */
    public AuthenticationReader(@Nonnull final JwtSettings conf, @Nullable final String authorizationHeader) {
        this.jwtConf = conf;
        this.authHeader = authorizationHeader;
    }

    /**
     * Extracts a full username from a JWT token which can be used as full identifier. <br>
     * Style: <code>user@REALM</code>
     * <br><br>
     * In order to do this, the given authHeader must be a valid token (with Bearer keyword).
     * 
     * @return Optional username with. Optional is empty when no valid token was given
     */
    public @Nonnull Optional<String> getAuthenticatedUserIdent() {
        Optional<String> fullUserNameRealm;
        try {
            fullUserNameRealm = Optional.of(getTokenObject()).map(this::getUserIdentifier);
        } catch (JwtTokenReadException e) {
            log.debug("Could not read JWT token: {}", e.getMessage());
            fullUserNameRealm = Optional.empty();
        }
        if (fullUserNameRealm.isEmpty()) {
            log.debug("No authorization header provided");
        }
        return fullUserNameRealm;
    }

    /**
     * Returns the token object with information of JWT token.
     * 
     * @return information defined by {@link JwtAuth#readJwtToken(String, String)}.
     * @throws JwtTokenReadException
     */
    public UsernamePasswordAuthenticationToken getTokenObject() throws JwtTokenReadException {
        return JwtAuth.readJwtToken(authHeader, jwtConf.getSecret());
    }

    /**
     * Creates a full identifier from authentication. Awaits {@link SparkysAuthPrincipal} as principal object.
     * <br>
     * Style: <code>user@REALM</code>
     * 
     * @param auth - Typically extracted by an JWT token
     * @return fullIdentName
     */
    private @Nonnull String getUserIdentifier(UsernamePasswordAuthenticationToken auth) {
        String name = auth.getName();
        var spPrincipal = (SparkysAuthPrincipal) auth.getPrincipal();
        UserRealm realm = spPrincipal.getRealm();
        return name + "@" + realm.name();
    }
}