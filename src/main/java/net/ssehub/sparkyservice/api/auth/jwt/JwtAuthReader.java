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
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
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
                Optional.of(readToAuthentication(jwt)).map(this::getUsername).map(Identity::of)
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
        SparkyUser user = userFrom(tokenObj, service);
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
    public Authentication readToAuthentication(@Nullable String jwtString) 
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
                throw new IllegalArgumentException("Decode of null token not possible");
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
     * The header name where the JWT can be found in the request.
     * 
     * @return .
     */
    // TODO maybe remove --> try to only use at a point
    public String getJwtRequestHeader() {
        return this.jwtHeader;
    }
    
    private SparkyUser userFrom(JwtToken token, UserStorageService service) {
        var password = new Password("", "UNKWN");
        var ident = Identity.of(token.getSubject());
        SparkyUser user;
        try {
            user = service.findUser(ident);
        } catch (UserNotFoundException e) {
            if (ident.realm() == UserRealm.MEMORY) {
                var perms = token.getTokenPermissionRoles().toArray(UserRole[]::new);
                if (perms.length != 1) {
                    throw new UnsupportedOperationException("Currently only one"
                            + " permission role per token is supported");
                }
                user = UserRealm.MEMORY.getUserFactory()
                    .create(token.getSubject(), password, perms[0], !token.isLocked());
            } else {
                throw e;
            }
        }
        return user;
    }

}