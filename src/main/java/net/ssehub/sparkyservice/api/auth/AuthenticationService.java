package net.ssehub.sparkyservice.api.auth;

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

/**
 * Service for managing authentication infromation from users.
 *
 * @author marcel
 */
@Service
@ParametersAreNonnullByDefault
public class AuthenticationService {

    @Nonnull
    private final UserExtractionService userExtractor;

    private final JwtAuthReader jwtReader;

    @Autowired
    public AuthenticationService(JwtAuthReader jwtReader, UserExtractionService userExtractor) {
        this.jwtReader = jwtReader;
        this.userExtractor = userExtractor;
    }

    public AuthenticationInfoDto checkAuthenticationStatus(@Nullable Authentication auth, HttpServletRequest request) 
            throws JwtTokenReadException {
        if (auth == null) {
            checkWrongAuthenticationStatusCause(request);
            throw new AuthenticationException();
        } else {
            return createAuthenticationInfoDto(auth);
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

    private AuthenticationInfoDto createAuthenticationInfoDto(Authentication auth) {
        // TODO merge this method - it is a possible duplicate
        var user = userExtractor.extract(auth);
        var dto = new AuthenticationInfoDto();
        dto.user = user.ownDto();
        if (auth.getCredentials() instanceof TokenDto) {
            dto.token = (TokenDto) auth.getCredentials();
        }
        return dto;
    }

    /**
     * Verifies the status of a token.
     * 
     * @param jwtString Token to verify
     * @return Authentications extracted from the token when valid
     * @throws JwtTokenReadException When the token isn't valid
     */
    public AuthenticationInfoDto verifyJwtToken(String jwtString) throws JwtTokenReadException {
        var auth = jwtReader.readRefreshToAuthentication(jwtString, userExtractor);
        return createAuthenticationInfoDto(auth);
    }
}