package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthConverter;
import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.dto.JwtDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Service for managing authentication infromation from users.
 *
 * @author marcel
 */
@Service
@ParametersAreNonnullByDefault
public class AuthenticationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);

    @Autowired
    private @Nonnull UserStorageService userStorage;
    
    @Autowired
    private JwtAuthReader jwtReader;
    
    @Autowired
    private AuthenticationManagerResolver<CredentialsDto> contextManagerResolver;
    
    @Autowired
    private JwtTokenService jwtService;
    
    @Autowired
    private JwtAuthConverter converter;

    public AuthenticationInfoDto checkAuthenticationStatus(HttpServletRequest request) {
        Authentication auth = converter.convert(request);
        var jwt = (JwtDto) auth.getCredentials();
        var dto = new AuthenticationInfoDto();
        dto.jwt = jwt;
        if (Identity.of(auth.getName()).realm() == UserRealm.RECOVERY) {
            dto.user = userFromAuthenticationOnly(auth).ownDto();
        } else {
            dto.user = userStorage.findUser(auth.getName()).ownDto();            
        }
        return dto;
    }
    
    private static SparkyUser userFromAuthenticationOnly(Authentication auth) {
        var ident = Identity.of(auth.getName());
        var role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(UserRole::getEnum).findFirst()
                .orElse(UserRole.DEFAULT);
        return ident.realm().getUserFactory().create(ident.nickname(),null, role, true);
    }

    /**
     * Verifies the status of a token.
     * 
     * @param jwtString Token to verify
     * @return Authentications extracted from the token when valid
     * @throws JwtTokenReadException When the token isn't valid
     */
    public AuthenticationInfoDto verifyJwtToken(String jwtString) throws JwtTokenReadException {
        return jwtReader.createAuthenticationInfoDto(jwtString, userStorage);
    }
    
    /**
     * Creates an DTO which holds all information (authenticated) user and generates an JWT
     * token for this user. The generated token can be used for authorization. 
     * 
     * @param user
     * @return DTO with user information and generated JWT token
     */
    @Nonnull
    private AuthenticationInfoDto createToken(SparkyUser user) {
        String jwt = jwtService.createFor(user);
        try {
            return jwtReader.createAuthenticationInfoDto(jwt, user);
        } catch (JwtTokenReadException e) {
            throw new RuntimeException("Could not create Token. Maybe the server is misconfigured");
        }
    }
    
    public Authentication authenticate(CredentialsDto credentials) {
        if (credentials.username == null || credentials.password == null) {
            LOG.debug("Username null not allowed");
            throw new AuthenticationException();
        }
        AuthenticationManager manager = contextManagerResolver.resolve(credentials);
        LOG.debug("Attempt authentication: {}", credentials.username);
        var authAttempt = new UsernamePasswordAuthenticationToken(credentials.username.trim(), credentials.password);
        var authentication = manager.authenticate(authAttempt);
        assertSparkyUser(authentication);
        return authentication;
    }

    public AuthenticationInfoDto authenticateAndGenerateJwt(CredentialsDto credentials) {
        var authentication = authenticate(credentials);
        if (authentication.isAuthenticated()) {
            LOG.debug("Successful authentication: {}", authentication.getName());
            try {
                var user = notNull(Optional.of((SparkyUser) authentication.getPrincipal()).orElseThrow());
                return createToken(user);
            } catch (Exception e) {
                LOG.error("Could not complete authentication request but attempt was successful)", e);
                throw e;
            }
        } else {
            throw new AuthenticationException();
        }
    }
    
   /**
    * Technically it could happen that an administrator configures spring to not return a supported implementation.
    * This is a check at runtime for this.
    * 
    * @param auth - Current authentication object where the principal is checked
    */
   private static void assertSparkyUser(@Nullable Authentication auth) {
       Optional.of(auth)
           .map(a -> a.getPrincipal())
           .filter(SparkyUser.class::isInstance)
           .map(SparkyUser.class::cast)
           .orElseThrow(() -> new RuntimeException("Spring authentication didn't provide a "
                   + "valid authentication object after authentication."));
   }
   
   /**
    * Refreshed a JWT. This must be done before the expiration time is reached. Only succeed when the maximum refresh
    * amount of the token is not reached.
    * 
    * @param request
    * @return A new refreshed token
    */
   public JwtDto refreshJwt(HttpServletRequest request) {
       var auth = converter.convert(request); 
       var jwt = (JwtToken) auth.getCredentials();
       jwtService.refresh(jwt); //TODO
       return null;
   }
}