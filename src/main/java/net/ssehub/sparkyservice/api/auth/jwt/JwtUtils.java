package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Helper class for working with JWT Tokens during Authentication seperated in an extra class for testing purposes.
 * 
 * @author marcel
 */
class JwtUtils {

    /**
     * Disabled.
     */
    private JwtUtils() {
    }

    /**
     * Decodes information present in a JWT token. 
     * 
     * @param token
     * @param jwtSecret
     * @return Object with fields from the decoded JWT token.
     */
    @Nonnull
    public static JwtToken decodeAndExtract(String token, JwtSettings jwtConf) {
        var signingKey = jwtConf.getSecret().getBytes();
        Jws<Claims> parsedToken = Jwts.parser()
                .setSigningKey(signingKey)
                .requireAudience(jwtConf.getAudience())
                .parseClaimsJws(token.replace("Bearer ", ""));
        String username = parsedToken.getBody().getSubject();
        var rolList = (List<?>) parsedToken.getBody().get("rol");
        List<UserRole> authorities = rolList.stream()
                .map(String.class::cast)
                .map(UserRole::getEnum)
                .collect(Collectors.toList());
        Date expiration = parsedToken.getBody().getExpiration();
        var jtiString = (String) parsedToken.getBody().get("jti");
        var jti = UUID.fromString(jtiString);
        
        if (jti != null && expiration != null && authorities != null && username != null) {
            var expDate = toLocalDateTime(expiration);
            var tokenObj = new JwtToken(jti, expDate, username, authorities);
            tokenObj.setTokenPermissionRoles(authorities);
            return tokenObj;
        } else {
            throw new RuntimeException("The JWT token has invalid fields but it has the right signature. Probably the"
                    + " encoding method is wrong");
        }
    }

    /**
     * Creates a JWT token which is encodes user data like username, roles, realm and sets a custom expiration time.
     * 
     * @param tokenObj 
     * @param jwtConf - Essential settings like secrets and the used issuer and audience
     * @return plain encoded JWT token as string (without bearer keyword)
     */
    @Nonnull
    public static String encode(JwtToken tokenObj, JwtSettings jwtConf) {
        byte[] signingKey = jwtConf.getSecret().getBytes();
        Date expiration = toUtilDate(tokenObj.getExpirationDate());
        Date iat = toUtilDate(LocalDateTime.now());
        return notNull(
            Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
                .setHeaderParam("typ", jwtConf.getType())
                .setIssuer(jwtConf.getIssuer())
                .setAudience(jwtConf.getAudience())
                .setSubject(tokenObj.getSubject())
                .setIssuedAt(iat)
                .setExpiration(expiration)
                .claim("rol", tokenObj.getTokenPermissionRoles())
                .setId(tokenObj.getJti().toString())
                .compact()
        );
    }
    
    /**
     * Method transforms a LocalDate to {@link Date}.
     * 
     * @param date - Date which is requested to be in the java.util.Date format
     * @return Same date as the provided LocalDate
     */
    private @Nonnull static java.util.Date toUtilDate(LocalDateTime date) {
        Instant instant = date.atZone(ZoneId.systemDefault()).toInstant();
        return notNull(Date.from(instant));
    }

    /**
     * Converts date.
     * 
     * @param date - Date which will be transformed to an LocalDate.
     * @return util.Date with values from the given one
     */
    private @Nonnull static LocalDateTime toLocalDateTime(java.util.Date date) {
        return notNull(
            Optional.of(date)
                .map(java.util.Date::toInstant)
                .map(instant -> instant.atZone(ZoneId.systemDefault()))
                .map(ZonedDateTime::toLocalDateTime)
                .get()
        );
    }
}
