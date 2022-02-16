package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import net.ssehub.sparkyservice.api.auth.AuthenticationInfoDto;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides additional methods to get information from a single JWT token.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
@Service
public class JwtAuthReader {

    private static Logger log = LoggerFactory.getLogger(JwtAuthReader.class);

    @Nonnull
    private final JwtTokenService jwtService;

    @Nonnull
    private final String jwtSecret;
    
    private final String jwtHeader;

    /**
     * Authentication reader for a specific JWT token. 
     * 
     * @param conf
     * @param service
     */
    public JwtAuthReader(final JwtTokenService service, final JwtSettings conf) {
        this.jwtService = service;
        this.jwtSecret = conf.getSecret();
        this.jwtHeader = conf.getHeader();
    }

    /**
     * Extracts a full username from a JWT token which can be used as full identifier. <br>
     * Style: <code>user@REALM</code>
     * <br><br>
     * In order to do this, the given authHeader must be a valid token (with Bearer keyword).
     * 
     * @param jwt
     * @return Optional username with. Optional is empty when no valid token was given (error is written to debug logger
     *         when present)
     */
    public @Nonnull Optional<Identity> getAuthenticatedUserIdent(@Nullable String jwt) {
        Optional<Identity> fullUserNameRealm;
        try {
            fullUserNameRealm = notNull(
                Optional.of(getAuthentication(jwt)).map(this::getUsername).map(Identity::of)
            );
        } catch (JwtTokenReadException e) {
            log.debug("Could not read JWT token: {}", e.getMessage());
            fullUserNameRealm = notNull(Optional.empty());
        }
        return fullUserNameRealm;
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
     * Builds an authentication DTO with from the token.  
     * 
     * @param jwt
     * @param service Service which helps to get user informations
     * @return DTO with information from the user which the jwt belongs to 
     * @throws JwtTokenReadException 
     */
    public @Nonnull AuthenticationInfoDto createAuthenticationInfoDto(String jwt, UserStorageService service) 
            throws JwtTokenReadException {
        JwtToken tokenObj = readJwtToken(jwt);
        SparkyUser user = service.findUser(tokenObj.getSubject());
        var authDto = new AuthenticationInfoDto();
        authDto.user = user.ownDto();
        authDto.token.expiration = DateUtil.toString(tokenObj.getExpirationDate());
        authDto.token.token = jwt;
        return authDto;
    }
    
    /**
     * Builds an authentication DTO with from the token and the given user. 
     * 
     * @param jwt
     * @param user
     * @return DTO with information from the user which the jwt belongs to
     * @throws JwtTokenReadException
     */
    public @Nonnull AuthenticationInfoDto createAuthenticationInfoDto(String jwt, SparkyUser user) 
            throws JwtTokenReadException {
        JwtToken tokenObj = readJwtToken(jwt);
        Identity tokenUser = Identity.of(tokenObj.getSubject());
        if (tokenUser.equals(user.getIdentity())) {
            var authDto = new AuthenticationInfoDto();
            authDto.user = user.ownDto();
            authDto.token.expiration = DateUtil.toString(tokenObj.getExpirationDate());
            authDto.token.token = jwt;
            return authDto;
        } 
        throw new JwtTokenReadException("Illegal access. User does not match with JWT subject"); // TODO write tests
    }
    

    /**
     * Returns the token object with information of JWT token.
     * 
     * @param jwt
     * @return information defined by {@link JwtUtils#readJwtToken(String, String)}.
     * @throws JwtTokenReadException
     */
    public Authentication getAuthentication(@Nullable String jwt) throws JwtTokenReadException {
        return readToAuthentication(jwt);
    }
    
    /**
     * Reads information out of the given JWT token to an authentication object. <br>
     * The returned authentication contains:<br>
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => String which can be used for {@link Identity} creation
     * <li>{@link Authentication#getCredentials()} => {@link TokenDto}</li>
     * <li>{@link Authentication#getAuthorities()} => (single) {@link UserRole}</li>
     * </ul>
     * 
     * @param jwtString - JWT token as string
     * @throws JwtTokenReadException
     * @return Springs authentication token
     */
    @Nonnull
    public UsernamePasswordAuthenticationToken readToAuthentication(@Nullable String jwtString) 
            throws JwtTokenReadException {
        JwtToken tokenObj = readJwtToken(jwtString);
        var tokenDto = new TokenDto();
        tokenDto.expiration = DateUtil.toString(tokenObj.getExpirationDate());
        tokenDto.token = jwtString;
        return new UsernamePasswordAuthenticationToken(
                tokenObj.getSubject(), tokenDto, tokenObj.getTokenPermissionRoles()
        );
    }


    /**
     * Reads information from a JWT token.
     * 
     * @param jwtString - JWT token as string
     * @throws JwtTokenReadException
     * @return Springs authentication token
     */
    @Nonnull
    public JwtToken readJwtToken(@Nullable String jwtString) throws JwtTokenReadException {
        try {
            if (jwtString == null) {
                throw new IllegalArgumentException("Couldn't decode JWT Token with given information");
            } else {
                JwtToken tokenObj = JwtUtils.decodeAndExtract(jwtString, jwtSecret);
                if (jwtService.isJitNonLocked(tokenObj.getJti())) {
                    return tokenObj;
                } else {
                    log.debug("Token {} is locked. User: {}" + tokenObj.getJti(), tokenObj.getSubject());
                    throw new JwtTokenReadException("The token with jit " + tokenObj.getJti() + " is locked");
                }
            }
        } catch (ExpiredJwtException exception) {
            log.debug("Request to parse expired JWT : {} failed : {}", jwtString, exception.getMessage());
            throw new JwtTokenReadException("Expired token");
        } catch (UnsupportedJwtException exception) {
            log.debug("Request to parse unsupported JWT : {} failed : {}", jwtString, exception.getMessage());
            throw new JwtTokenReadException("Unsupported JWT");
        } catch (MalformedJwtException exception) {
            log.debug("Request to parse invalid JWT : {} failed : {}", jwtString, exception.getMessage());
            throw new JwtTokenReadException("Invalid JWT");
        } catch (SignatureException exception) {
            log.debug("Request to parse JWT with invalid signature: {} failed : {}", jwtString, exception.getMessage());
            throw new JwtTokenReadException("Invalid signature");
        } catch (IllegalArgumentException exception) {
            log.debug("Request to parse empty or null JWT : {} failed : {}", jwtString, exception.getMessage());
            throw new JwtTokenReadException(exception.getMessage());
        }
    }
    
    /**
     * Reads information out of the given JWT token and tries to refreshes them with information from a storage
     * to make sure the given information are valid.
     * The returned authentication contains:<br>
     * 
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => {@link SparkyUser}</li>
     * <li>{@link Authentication#getCredentials()} => {@link TokenDto}</li>
     * <li>{@link Authentication#getAuthorities()} => (single) {@link UserRole}</li>
     * </ul>
     * 
     * @param jwtString - JWT token as string
     * @param service - Transformer which should be used for completing information from a storage
     * @throws JwtTokenReadException
     * @return Springs authentication token
     */
    @Nonnull
    public Authentication readRefreshToAuthentication(@Nullable String jwtString, UserExtractionService service) 
            throws JwtTokenReadException {
        var authenticationObject = readToAuthentication(jwtString);
        try {
            var user = service.extractAndRefresh(authenticationObject);
            authenticationObject = new UsernamePasswordAuthenticationToken(
                user, authenticationObject.getCredentials(), user.getAuthorities()
            );
        } catch (UserNotFoundException e) {
            log.debug("No storage refresh operation after reading a JWT token: {}", e.getMessage()); 
        }
        return authenticationObject;
    }
    
    /**
     * The header name where the JWT can be found in the request.
     * 
     * @return .
     */
    public String getJwtRequestHeader() {
        return this.jwtHeader;
    }

}