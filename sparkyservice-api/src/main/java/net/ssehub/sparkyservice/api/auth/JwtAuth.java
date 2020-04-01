package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;

public class JwtAuth {
    private JwtAuth() {}

    public static UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(HttpServletRequest request) {
        var username = request.getParameter("username");
        var password = request.getParameter("password");
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    public static @Nullable String createJwtFromAuthentication(Authentication authentication, ConfigurationValues jwtConf) {
        if (authentication.getPrincipal() instanceof UserDetails) {
            return createJwtToken((UserDetails) authentication, jwtConf);
        } else {
            return null;
        }
    }

    public static String createJwtToken(UserDetails user, ConfigurationValues jwtConf) {
        return createJwtTokenWithRealm(user, jwtConf, "");
    }

    public static @Nonnull String createJwtTokenWithRealm(UserDetails user, ConfigurationValues jwtConf, String realm) {
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
                .claim("realm", realm)
                .compact();
            return notNull(token);
    }

    public static @Nullable UsernamePasswordAuthenticationToken readJwtToken(String token, String jwtSecret) {
        var signingKey = jwtSecret.getBytes();
        var parsedToken = Jwts.parser()
            .setSigningKey(signingKey)
            .parseClaimsJws(token.replace("Bearer ", ""));
        var username = parsedToken
            .getBody()
            .getSubject();
        var authorities = ((List<?>) parsedToken.getBody()
            .get("rol")).stream()
            .map(authority -> new SimpleGrantedAuthority((String) authority))
            .collect(Collectors.toList());
        Date expiration = parsedToken.getBody().getExpiration();
        var realm = (String) parsedToken.getBody().get("realm");
        if (!StringUtils.isEmpty(username)) {
            SparkysAuthPrincipal principal = new AuthPrincipalImplementation(realm, username);
            return new UsernamePasswordAuthenticationToken(principal, expirationDateAsString(expiration), authorities);
        } else {
            return null;
        }
    }
    
    private static String expirationDateAsString(Date expDate) {
        String pattern = "MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        return df.format(expDate);
    }
}
