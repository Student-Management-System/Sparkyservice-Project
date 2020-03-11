package net.ssehub.sparkyservice.api.auth;

import java.util.Date;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;

 
public class JwtAuthentication {
    private JwtAuthentication() {}
    
    public static UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(HttpServletRequest request) {
        var username = request.getParameter("username");
        var password = request.getParameter("password");
        return new UsernamePasswordAuthenticationToken(username, password);
    }
    
    public static String createJwtToken(Authentication authentication, ConfigurationValues jwtConf) {
        var user = (UserDetails) authentication.getPrincipal();
        var roles = user.getAuthorities()
            .stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
        var signingKey = jwtConf.getJwtSecret().getBytes();
        var token = Jwts.builder()
            .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
            .setHeaderParam("typ", jwtConf.getJwtTokenType())
            .setIssuer(jwtConf.getJwtTokenIssuer())
            .setAudience(jwtConf.getJwtTokenAudience())
            .setSubject(user.getUsername())
            .setExpiration(new Date(System.currentTimeMillis() + 864000000))
            .claim("rol", roles)
            .compact();
        return token;
    }
}
