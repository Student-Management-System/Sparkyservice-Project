package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Date;
import java.util.List;
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
    public static JwtToken decodeAndExtract(String token, String jwtSecret) {
        var signingKey = jwtSecret.getBytes();
        Jws<Claims> parsedToken = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token.replace("Bearer ", ""));
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
            var tokenObj = new JwtToken(jti, expiration, username, authorities);
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
        return notNull(
            Jwts.builder()
                .signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
                .setHeaderParam("typ", jwtConf.getType())
                .setIssuer(jwtConf.getIssuer())
                .setAudience(jwtConf.getAudience())
                .setSubject(tokenObj.getSubject())
                .setExpiration(tokenObj.getExpirationDate())
                .claim("rol", tokenObj.getTokenPermissionRoles())
                .setId(tokenObj.getJti().toString())
                .compact()
        );
    }
}
