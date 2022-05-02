package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.config.ControllerPath;
import net.ssehub.sparkyservice.api.persistence.UserNotFoundException;
import net.ssehub.sparkyservice.api.useraccess.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.useraccess.dto.ErrorDto;
import net.ssehub.sparkyservice.api.useraccess.dto.JwtDto;

/**
 * Provides controller methods for authentication mechanisms.
 * 
 * @author Marcel
 */
@RestController
@Tag(name = "auth-controller", description = "Controller for realm authentication with JWT")
public class AuthController {
    
    @Autowired
    private AuthenticationService authService;

    /**
     * Authenticates a user.
     * 
     * @param credentials used for authentication
     * @return
     */
    @Operation(summary = "Authentication / Login", 
            description = "Authenticates the a user with a given nickname. Can be used without realm information")
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH)
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Authentication success"),
            
        @ApiResponse(responseCode = "401", description = "Authentication failed", 
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = ErrorDto.class)))
    })
    @PreAuthorize("permitAll()")
    public AuthenticationInfoDto login(@Valid @Nonnull @NotNull @RequestBody CredentialsDto credentials) {
        return authService.authenticateAndGenerateJwt(credentials);
    }

    /**
     * Checks if the user is authenticated with a given JWT Token. This controller
     * is not protected through spring security in order to provide better
     * information about what went wrong.
     * 
     * @param auth - Injected through spring if the user is logged in - holds
     *               authentication information
     * @param request - Provided by Spring
     * @return user information which are stored in the JWT token
     * @throws UserNotFoundException
     * @throws MissingDataException
     * @throws JwtTokenReadException 
     */
    @Operation(description = "Checks the authentication state of the users authorization header and returns all "
            + " user informations which belongs to the user",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = ControllerPath.AUTHENTICATION_CHECK)
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "Authentication status is good"),
        @ApiResponse(responseCode = "401", description = "Not authenticated",
                content = @Content(mediaType = "application/json",
                       schema = @Schema(implementation = ErrorDto.class))) })
    public AuthenticationInfoDto whoAmI(@Nonnull HttpServletRequest request) {
        return authService.checkAuthenticationStatus(request);      
    }

    /*
     * Only supports jwtTokens which was created through the projects own
     * authentication filter. Mocking users with spring during integration tests are
     * not supported here.
     */
    /**
     * Checks if a given token is valid. Validity means a non expired token which contains all information
     * which are necessary to proceed with it in the application. When the token is valid, it can be used as 
     * authentication token for the whole application.
     * 
     * @param jwtToken - The token which should be verified
     * @return The stored information in the token
     * @throws MissingDataException - Is thrown when the data of the token is not complete
     * @throws JwtTokenReadException 
     */
    @Operation(description = "Prints the validity status of a given token")
    @GetMapping(value = ControllerPath.AUTHENTICATION_VERIFY)
    public AuthenticationInfoDto verifyTokenValidity(@NotNull @Nonnull String jwtToken) throws JwtTokenReadException {
        try {
            return authService.verifyJwtToken(jwtToken);
        } catch (JwtTokenReadException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JWT token", e);
        }
    }
    
    /**
     * REST Controller for JWT refreshing. 
     * 
     * @param request
     * @return new JWT with ne expiration time
     * @throws JwtTokenReadException
     */
    @Operation(description = "Refreshed/Renews the current used JWT without the need of re-authentication. "
            + "This must be done before expiration time of the token is reached.",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = ControllerPath.RENEW_JWT)
    @ApiResponses(value = { 
            @ApiResponse(responseCode = "200", description = "The new JWT"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "423", description = "JWT is locked and/or maximum renew amount is reached")})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public JwtDto renew(@Nonnull HttpServletRequest request) throws JwtTokenReadException {
        return authService.refreshJwt(request);
    }
    
}

