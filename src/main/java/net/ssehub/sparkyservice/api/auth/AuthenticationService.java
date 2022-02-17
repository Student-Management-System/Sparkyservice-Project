package net.ssehub.sparkyservice.api.auth;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Service for managing authentication infromation from users.
 *
 * @author marcel
 */
@Service
@ParametersAreNonnullByDefault
public class AuthenticationService {

    @Nonnull
    private final UserStorageService userStorage;

    private final JwtAuthReader jwtReader;

    private final UserExtractionService extractionService;
    
    @Autowired
    public AuthenticationService(JwtAuthReader jwtReader, UserStorageService userStorage, UserExtractionService extractionService) {
        this.jwtReader = jwtReader;
        this.extractionService = extractionService;
        this.userStorage = userStorage;
    }

    public AuthenticationInfoDto checkAuthenticationStatus(@Nullable Authentication auth, HttpServletRequest request) 
            throws JwtTokenReadException {
        if (auth == null) {
            checkWrongAuthenticationStatusCause(request);
            throw new AuthenticationException();
        } else {
            return extractAuthenticationInfoDto(auth);
        }
    }

    /**
     * This method will throw something and shows the reason why the authorization through JWT token failed.
     * 
     * @param request
     * @throws JwtTokenReadException
     */
    private void checkWrongAuthenticationStatusCause(HttpServletRequest request) throws JwtTokenReadException {
        var jwtToken = request.getHeader(jwtReader.getJwtRequestHeader());
        jwtReader.readJwtToken(jwtToken);
    }

    public AuthenticationInfoDto extractAuthenticationInfoDto(Authentication auth) {
        // TODO merge this method - it is a possible duplicate
        var user = extractionService.extract(auth);
        var dto = new AuthenticationInfoDto();
        dto.user = user.ownDto();
        if (auth.getCredentials() instanceof TokenDto) {
            dto.token = (TokenDto) auth.getCredentials();
        }
        return dto;
    }
    
    /**
     * Creates an DTO which holds all information of the user the given (authenticated) user and generates an JWT
     * token for this user. The generated token can be used for authorization. 
     * 
     * @param user
     * @return DTO with user information and generated JWT token
     */
    

    /**
     * Creates an authentication token for spring with an JWT token which it extracts from the request.
     * 
     * @param request
     * @return Token object with the values of the JWT token
     */
    @Nonnull
    public  Authentication extractAuthentication(HttpServletRequest request) {
        var jwt = Optional.ofNullable(request.getHeader("Proxy-Authorization"))
                .orElse(request.getHeader("Authorization"));
        try {
            return jwtReader.readToAuthentication(jwt);
        } catch (JwtTokenReadException e) {
            throw new AuthenticationException(e);
        }
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
}