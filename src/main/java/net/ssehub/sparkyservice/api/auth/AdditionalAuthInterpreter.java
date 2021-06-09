package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthTools;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides additional methods to get information from a single JWT token.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public class AdditionalAuthInterpreter {

    private Optional<Logger> logger;

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
    public AdditionalAuthInterpreter(final JwtTokenService service, @Nullable final String authorizationHeader) {
        this.jwtService = service;
        this.authHeader = authorizationHeader;
        logger = Optional.empty();
    }

    /**
     * Authentication reader for specific JWT token. 
     * @param service
     * @param jwtString
     * @param logger Used for logging internal processes. Don't set logger when no logs wished.
     */
    public AdditionalAuthInterpreter(final JwtTokenService service, @Nullable final String jwtString, Logger logger) {
        this(service, jwtString);
        this.logger = Optional.of(logger);
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
    public @Nonnull Optional<String> getAuthenticatedUserIdent() {
        Optional<String> fullUserNameRealm;
        try {
            fullUserNameRealm = notNull(
                Optional.of(getAuthentication()).map(this::getUserIdentifier)
            );
        } catch (JwtTokenReadException e) {
            logger.ifPresent(log -> log.debug("Could not read JWT token: {}", e.getMessage()));
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
    private @Nonnull String getUserIdentifier(Authentication auth) {
        var spPrincipal = (SparkysAuthPrincipal) auth.getPrincipal();
        return spPrincipal.asString();
    }

    /**
     * Builds an authentication DTO with the information which can be extracted from the token. 
     * For this, a user extractor is necessary.
     * 
     * @param extractor Service which helps to get user informations
     * @return AuthenticationDTO with information of extracted from the jwt string
     * @throws JwtTokenReadException 
     */
    public @Nonnull AuthenticationInfoDto getAuthenticationInfoDto(UserExtractionService extractor) 
            throws JwtTokenReadException {
        JwtToken tokenObj = jwtService.readJwtToken(authHeader);
        SparkyUser user = extractor.extendAndRefresh(tokenObj.getUserInfo());
        var authDto = new AuthenticationInfoDto();
        authDto.user = user.ownDto();
        authDto.token.expiration = DateUtil.toString(tokenObj.getExpirationDate());
        authDto.token.token = authHeader;
        return authDto;
    }
}