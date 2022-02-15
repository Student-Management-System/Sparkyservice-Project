package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Provides methods for dealing with concrete JWT tokens through {@link JwtCache} only.
 * 
 * @author marcel
 */
@Service
@ParametersAreNonnullByDefault
public class JwtTokenService {

    @Nonnull
    private final JwtSettings jwtConf;

    private final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    /**
     * Service class for dealing with concrete jwt tokens.
     * 
     * @param jwtConf - Contains configuration like secret - not allowed to be null
     */
    @Autowired
    public JwtTokenService(@Nullable JwtSettings jwtConf) {
        if (jwtConf == null) {
            // do this because spring technically would inject null when configuration is not made
            throw new RuntimeException("Try to inject null as jwt configuration into service");
        }
        this.jwtConf = jwtConf;
    }

    /**
     * Lockes jwt tokens for authorization.
     * 
     * @param jit Identifier
     */
    public void disable(UUID... jit) {
        for (var singleJit : jit) {
            Optional<JwtToken> token = JwtCache.getInstance().getCachedToken(singleJit);
            token.ifPresent(cacheToken -> {
                cacheToken.setLocked(true);
                JwtCache.getInstance().storeAndSave(cacheToken);
            });
        }
    }

    /**
     * Disables all stored JWT token from a specific user.
     * 
     * @param user
     */
    public void disableAllFrom(SparkyUser user) {
        Set<JwtToken> tokensFromCache = JwtCache.getInstance().getCachedTokens();
        tokensFromCache.forEach(token -> token.setLocked(true));
        JwtToken[] jwtArray = notNull(tokensFromCache.toArray(JwtToken[]::new));
        JwtCache.getInstance().storeAndSave(jwtArray);
    }

    /**
     * Reads information out of the given JWT token to an authentication object. <br>
     * The returned authentication contains:<br>
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => {@link SparkysAuthPrincipal}</li>
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
                JwtToken tokenObj = JwtAuthTools.decodeAndExtract(jwtString, jwtConf.getSecret());
                if (isJitNonLocked(tokenObj.getJti())) {
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
     * Creates a JWT token for the given user.
     * 
     * @param user Jwt token will hold information from this user
     * @return Signed jwt token
     */
    public String createFor(SparkyUser user) {
        UUID jit = UUID.randomUUID();
        log.trace("Created JWT token with jit {}", jit.toString());
        Date expDate = JwtAuthTools.createJwtExpirationDate(user);
        var tokenObj = new JwtToken(jit, expDate, user.getUsername(), user.getRole());
        tokenObj.setRemainingRefreshes(0 /*TODO*/);
        String tokenString = JwtAuthTools.encode(tokenObj, jwtConf);
        JwtCache.getInstance().storeAndSave(tokenObj);
        return tokenString;
    }

    /**
     * Todo.
     * @param token
     */
    public void refresh(JwtToken token) {
        // TODO implement a refresh system? 
    }

    /**
     * Todo.
     * @param jwtString
     */
    public void refresh(String jwtString) {
        // TODO implement a refresh system ? 
    }

    /**
     * Jwt conf which is used in this service class.
     * 
     * @return jwt settings
     */
    public JwtSettings getJwtConf() {
        return this.jwtConf;
    }

    /**
     * Searches a list of all locked jwt tokens for the given one. 
     * 
     * @param jit The JIT to look for
     * @return <code> true </code> when the JIT is valid an not locked for authorization
     */
    public boolean isJitNonLocked(UUID jit) {
        Optional<JwtToken> token = JwtCache.getInstance().getCachedToken(jit);
        return !token.map(JwtToken::isLocked).orElse(false);
    }
}