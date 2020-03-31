package net.ssehub.sparkyservice.api.auth;


import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.conf.ControllerPath;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Controller for authentication
 * @author Marcel
 */
@RestController
public class AuthController {

    /**
     * This method does nothing. The method header is important to let swagger list this authentication method.
     * The authentication is handled through {@link JwtAuthenticationFilter} which listens on the same
     * path than this method.
     * 
     * @param username Username of the user
     * @param password Password of the user
     */
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH) 
    public void authenticate(@RequestParam(value = "username") @NotNull String username, 
                        @RequestParam(value = "password") @NotNull String password) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the user is authenticated with a given JWT Token. If the token is valid, the controller is reachable
     * otherwise it would be blocked through spring security and FORBIDDEN is returned. 
     * 
     * @param auth Injected through spring if the user is logged in - holds authentication information
     * @return user information which are stored in the jwt token
     */
    @GetMapping(value = ControllerPath.AUTHENTICATION_CHECK) 
    public String isTokenValid(@ApiIgnore @Nonnull Authentication auth) {
//        if (auth.getPrincipal() instanceof SparkysAuthPrincipal) {
//        } else if ( auth.getPrincipal() instanceof UserDetails) {
//        }
        return "";
    }
}
