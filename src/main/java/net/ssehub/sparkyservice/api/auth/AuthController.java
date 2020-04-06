package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import net.ssehub.sparkyservice.api.auth.exceptions.AccessViolationException;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.user.IUserService;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

/**
 * Controller for authentication
 * 
 * @author Marcel
 */
@RestController
public class AuthController {
    public class AuthenticationInfoDto {
        public UserDto user;
        public TokenDto token;
    }

    @Autowired
    private IUserService userService;

    @Autowired
    private ConfigurationValues confValues;

    /**
     * This method does nothing. The method header is important to let swagger list
     * this authentication method. The authentication is handled through
     * {@link JwtAuthenticationFilter} which listens on the same path than this
     * method.
     * 
     * @param username Username of the user
     * @param password Password of the user
     */
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH)
    public AuthenticationInfoDto authenticate(@Nonnull @NotNull @Valid CredentialsDto credentials) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the user is authenticated with a given JWT Token. This controller is not protected through spring
     * secrurity in order to provide better information about what went wrong. 
     * 
     * @param auth Injected through spring if the user is logged in - holds
     *             authentication information
     * @return user information which are stored in the jwt token
     * @throws UserNotFoundException
     * @throws MissingDataException
     */
    @Operation(security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = ControllerPath.AUTHENTICATION_CHECK)
    public AuthenticationInfoDto isTokenValid(@Nullable Authentication auth, HttpServletRequest request)
            throws AccessViolationException, MissingDataException {
        if (auth == null) { // check what went wrong
            var jwtToken = request.getHeader(confValues.getJwtTokenHeader());
            if (!StringUtils.isEmpty(jwtToken) && jwtToken.startsWith(confValues.getJwtTokenPrefix())) {
                JwtAuth.readJwtToken(jwtToken, confValues.getJwtSecret()); // should throw something
            }
            throw new AccessViolationException("Not authenticated");
        }
        var user = userService.getDefaultTransformer().extendFromAny(auth);
        var dto = new AuthenticationInfoDto();
        if (user != null) {
            dto.user = user.asDto();
            //dto.token = (TokenDto) auth.getCredentials();
            return dto;
        } else {
            throw new AccessViolationException("Wrong authentication object");
        }
    }

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler({ AccessViolationException.class, MissingDataException.class, UserNotFoundException.class })
    public String handleUserEditException(AccessViolationException ex) {
        return handleException(ex);
    }

    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({ Exception.class })
    public String handleException(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isEmpty()) {
            return "There was a problem with the user data.";
        } else {
            return ex.getMessage();
        }
    }
}
