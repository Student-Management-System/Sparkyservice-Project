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
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;

public class JwtAuth {
    private JwtAuth() {}
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuth.class);

    public static @Nonnull UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(HttpServletRequest request) {
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

    public static @Nonnull String createJwtToken(String username,  List<UserRole> roles, JwtSettings jwtConf) {
        return createJwtTokenWithRealm(username, roles, jwtConf, UserRealm.UNKNOWN);
    }

    /**
     * Creates a JWT Token.
     * 
     * @param username 
     * @param authorities list of authorities is set 
     * @param jwtConf 
     * @param realm
     * @return
     */
    public static @Nonnull String createJwtTokenWithRealm(@Nullable String username, @Nullable List<UserRole> roles, 
            JwtSettings jwtConf, UserRealm realm) {
//        var roles = authorities
//            .stream()
//            .map(GrantedAuthority::getAuthority)
//            .collect(Collectors.toList());
        var signingKey = jwtConf.getSecret().getBytes();
        var token = Jwts.builder()
            .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
            .setHeaderParam("typ", jwtConf.getType())
            .setIssuer(jwtConf.getIssuer())
            .setAudience(jwtConf.getAudience())
            .setSubject(username)
            .setExpiration(new Date(System.currentTimeMillis() + 864000000))
            .claim("rol", roles)
            .claim("realm", realm)
            .compact();
        return notNull(token);
    }

    /**
     * Extracts information of a given token. The secret must be the same as the secret which was used to encode 
     * the JWT token. <br>
     * The returned authentication contains:<br>
     * <ul><li> {@link Authentication#getPrincipal()} => {@link SparkysAuthPrincipal}
     * </li><li> {@link Authentication#getCredentials()} => {@link TokenDto}
     * </li><li> {@link Authentication#getAuthorities()} => (single) {@link UserRole}
     * </li></ul>
     * 
     * @param token JWT token as string
     * @param jwtSecret Is used to decode the token - must be the same which was used for encoding
     * @return Springs authentication token
     */
    public static @Nullable UsernamePasswordAuthenticationToken readJwtToken(@Nonnull String token, 
            @Nonnull String jwtSecret) {
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
            var tokenDto = new TokenDto();
            tokenDto.expiration = expirationDateAsString(expiration);
            tokenDto.token = token;
            return new UsernamePasswordAuthenticationToken(principal, tokenDto, authorities);
        } else {
            return null;
        }
    }
    
    private static @Nonnull String expirationDateAsString(@Nullable Date expDate) {
        String pattern = "MM/dd/yyyy HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        return notNull(df.format(expDate));
    }
}
