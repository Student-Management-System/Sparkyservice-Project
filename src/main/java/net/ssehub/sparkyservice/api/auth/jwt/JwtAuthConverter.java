package net.ssehub.sparkyservice.api.auth.jwt;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;

import net.ssehub.sparkyservice.api.auth.AuthenticationException;

@Component
public class JwtAuthConverter implements AuthenticationConverter {
    @Autowired
    private JwtAuthReader jwtReader;

    @Override
    public Authentication convert(HttpServletRequest request) {
        var jwt = Optional.ofNullable(request.getHeader("Proxy-Authorization"))
                .orElse(request.getHeader("Authorization"));
        try {
            return jwtReader.readToAuthentication(jwt);
        } catch (JwtTokenReadException e) {
            throw new AuthenticationException(e);
        }
    }
}

