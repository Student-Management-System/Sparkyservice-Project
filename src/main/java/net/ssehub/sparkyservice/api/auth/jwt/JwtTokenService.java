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
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.SparkyUser;

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
     * Creates a JWT token for the given user.
     * 
     * @param user Jwt token will hold information from this user
     * @return Signed jwt token
     */
    @Nonnull
    public String createFor(SparkyUser user) {
        UUID jit = UUID.randomUUID();
        log.trace("Created JWT token with jit {}", jit.toString());
        Date expDate = JwtUtils.createJwtExpirationDate(user);
        var tokenObj = new JwtToken(jit, expDate, user.getUsername(), user.getRole());
        tokenObj.setRemainingRefreshes(0 /*TODO*/);
        String tokenString = JwtUtils.encode(tokenObj, jwtConf);
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