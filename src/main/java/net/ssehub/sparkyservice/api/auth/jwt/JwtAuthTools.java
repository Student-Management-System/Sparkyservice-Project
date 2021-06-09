package net.ssehub.sparkyservice.api.auth.jwt;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.util.DateUtil;

/**
 * Helper class for working with JWT Tokens during Authentication seperated in an extra class for testing purposes.
 * 
 * @author marcel
 */
public class JwtAuthTools {

    public static final int TOKEN_EXPIRE_TIME_MS = 86_400_000; // 24 hours

    /**
     * Disabled.
     */
    private JwtAuthTools() {
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
        String realmName = (String) parsedToken.getBody().get("realm");
        var jtiString = (String) parsedToken.getBody().get("jti");
        var jti = UUID.fromString(jtiString);
        SparkysAuthPrincipal sparkyPrincipal = new AuthPrincipalImpl(realmName, username);
        
        if (jti != null && expiration != null && authorities != null) {
            var tokenObj = new JwtToken(jti, expiration, sparkyPrincipal, authorities);
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
                .setSubject(tokenObj.getUserInfo().getName())
                .setExpiration(tokenObj.getExpirationDate())
                .claim("rol", tokenObj.getTokenPermissionRoles())
                .claim("realm", tokenObj.getUserInfo().getRealm())
                .setId(tokenObj.getJti().toString())
                .compact()
        );
    }

    /**
     * Returns a date where a JWT token of user should expire. 
     * 
     * @param user
     * @return Date where the validity of a JWT token should end for the given user
     */
    @Nonnull
    public static java.util.Date createJwtExpirationDate(SparkyUser user) {
        @Nonnull java.util.Date expirationDate;
        @Nonnull Supplier<LocalDate> defaultServiceExpirationDate = () -> LocalDate.now().plusYears(10);

        if (user.getRole() == UserRole.SERVICE) {
            expirationDate = notNull(
                user.getExpireDate()
                    .map(DateUtil::toUtilDate)
                    .orElse(DateUtil.toUtilDate(defaultServiceExpirationDate.get()))
            );
        } else {
            expirationDate = new java.util.Date(System.currentTimeMillis() + TOKEN_EXPIRE_TIME_MS);
        }
        return expirationDate;
    }
}
