package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.auth.AuthenticationInfoDto;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides additional methods to get information from a single JWT token.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public class JwtAuthReader {

    private static Logger logger = LoggerFactory.getLogger(JwtAuthReader.class);

    @Nonnull
    private final JwtTokenService jwtService;

    @Nullable
    private final String authHeader;

    /**
     * Authentication reader for a specific JWT token. 
     * 
     * @param authorizationHeader - From the request where the JWT Token
     *                              is stored
     * @param service
     */
    public JwtAuthReader(final JwtTokenService service, @Nullable final String authorizationHeader) {
        this.jwtService = service;
        this.authHeader = authorizationHeader;
    }

    /**
     * Extracts a full username from a JWT token which can be used as full identifier. <br>
     * Style: <code>user@REALM</code>
     * <br><br>
     * In order to do this, the given authHeader must be a valid token (with Bearer keyword).
     * 
     * @return Optional username with. Optional is empty when no valid token was given (error is written to debug logger
     *         when present)
     */
    public @Nonnull Optional<Identity> getAuthenticatedUserIdent() {
        Optional<Identity> fullUserNameRealm;
        try {
            fullUserNameRealm = notNull(
                Optional.of(getAuthentication()).map(this::getUsername).map(Identity::of)
            );
        } catch (JwtTokenReadException e) {
            logger.debug("Could not read JWT token: {}", e.getMessage());
            fullUserNameRealm = notNull(Optional.empty());
        }
        return fullUserNameRealm;
    }

    /**
     * Returns the token object with information of JWT token.
     * 
     * @return information defined by {@link JwtAuthTools#readJwtToken(String, String)}.
     * @throws JwtTokenReadException
     */
    public Authentication getAuthentication() throws JwtTokenReadException {
        return jwtService.readToAuthentication(authHeader);
    }

    /**
     * Creates a full identifier from authentication. Awaits {@link SparkysAuthPrincipal} as principal object.
     * <br>
     * Style: <code>user@REALM</code>
     * 
     * @param auth - Typically extracted by an JWT token
     * @return fullIdentName
     */
    private String getUsername(Authentication auth) {
        // TODO check if this is possible never null and annotate this method
        return (String) auth.getPrincipal();
    }

    /**
     * Builds an authentication DTO with the information which can be extracted from the token. 
     * For this, a user extractor is necessary.
     * 
     * @param service Service which helps to get user informations
     * @return AuthenticationDTO with information of extracted from the jwt string
     * @throws JwtTokenReadException 
     */
    public @Nonnull AuthenticationInfoDto getAuthenticationInfoDto(UserStorageService service) 
            throws JwtTokenReadException {
        JwtToken tokenObj = jwtService.readJwtToken(authHeader);
        SparkyUser user = service.findUser(tokenObj.getSubject());
        var authDto = new AuthenticationInfoDto();
        authDto.user = user.ownDto();
        authDto.token.expiration = DateUtil.toString(tokenObj.getExpirationDate());
        authDto.token.token = authHeader;
        return authDto;
    }
}