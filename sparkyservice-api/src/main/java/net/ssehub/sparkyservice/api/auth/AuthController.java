package net.ssehub.sparkyservice.api.auth;


import javax.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.conf.ControllerPath;

/**
 * Controller for authentication reachable under /auth
 * @author marcel
 */
@RestController
public class AuthController {

    /**
     * This method does nothing. The method header is important to let swagger list this authentication method.
     * The authentication is handled through @link {@link JwtAuthenticationFilter} which listens on the same
     * path than this method.
     * @param username Username of the user
     * @param password Password of the user
     * @return HTTP body and sets JWT Token inside http header
     */
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH) 
    public String authenticate(@RequestParam(value = "username") @NotNull String username, 
                        @RequestParam(value = "password") @NotNull String password) {
        throw new UnsupportedOperationException();
    }
}
