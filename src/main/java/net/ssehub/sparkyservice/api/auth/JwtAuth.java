package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;

public class JwtAuth {
    private JwtAuth() {}
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuth.class);

    public static UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean passwordAvailable = password != null && !password.isBlank();
        LOG.debug("[HTTP Parameter] Username: " + username + " | Password available: " + passwordAvailable );
        if (username == null && password == null) {
            try {
                CredentialsDto cred = new ObjectMapper().readValue(request.getInputStream(), CredentialsDto.class); 
                username = cred.username;
                password = cred.password;
                boolean avail = password != null && !password.isBlank();
                LOG.debug("[HTTP Body] Username: " + username + " | Password available: " + avail );
            } catch (MismatchedInputException e) {
                LOG.debug("Credentials not avaiable in requests input stream");
                // do nothing - is thrown on invalid values like null
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
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
        return createJwtTokenWithRealm(user, jwtConf, UserRealm.UNKNOWN);
    }

    public static @Nonnull String createJwtTokenWithRealm(UserDetails user, ConfigurationValues jwtConf, 
            UserRealm realm) {
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
            .map(authority -> UserRole.DEFAULT.getEnum((String) authority))
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
