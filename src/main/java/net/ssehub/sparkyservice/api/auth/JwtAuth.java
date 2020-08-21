package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.modification.UserModificationService;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Helper class for working with JWT Tokens during Authentication.
 * 
 * @author marcel
 */
public class JwtAuth {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuth.class);

    /**
     * Disabled.
     */
    private JwtAuth() {
    }

    /**
     * Method reads the {@link CredentialsDto} from a given request and transform
     * them into a AuthenticationToken.
     * 
     * @param request
     * @return contains the username and password used for authentication
     */
    public static @Nonnull UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(
            HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean passwordAvailable = password != null && !password.isBlank();
        LOG.debug("[HTTP Parameter] Username: " + username + " | Password available: " + passwordAvailable);
        if (username == null && password == null) {
            try {
                CredentialsDto cred = new ObjectMapper().readValue(request.getInputStream(), CredentialsDto.class);
                username = cred.username;
                password = cred.password;
                boolean avail = password != null && !password.isBlank();
                LOG.debug("[HTTP Body] Username: " + username + " | Password available: " + avail);
            } catch (MismatchedInputException e) {
                LOG.debug("Credentials not avaiable in requests input stream");
                // do nothing - is thrown on invalid values like null
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new UsernamePasswordAuthenticationToken(username, password);
    }

    /**
     * Creates a JWT token which is encodes user data like username, roles, realm and sets a custom expiration time.
     * 
     * @param user - Provides the data
     * @param jwtConf - Essential settings like secrets and the used issuer and audience
     * @return plain encoded JWT token as string (without bearer keyword)
     */
    public static @Nonnull String createJwtToken(SparkyUser user, JwtSettings jwtConf) {
        List<UserRole> roles = Stream.of(user.getRole()).map(e -> e).collect(Collectors.toList());
        java.util.Date expDate = UserModificationService.from(user.getRole()).createJwtExpirationDate(user);
        var signingKey = jwtConf.getSecret().getBytes();
        var token = Jwts.builder().signWith(Keys.hmacShaKeyFor(signingKey), SignatureAlgorithm.HS512)
                .setHeaderParam("typ", jwtConf.getType()).setIssuer(jwtConf.getIssuer())
                .setAudience(jwtConf.getAudience()).setSubject(user.getUsername())
                .setExpiration(expDate).claim("rol", roles)
                .claim("realm", user.getRealm()).compact();
        return notNull(token);
    }

    /**
     * Decodes the content of a JWT Token. Content of the returned object: 
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => {@link SparkysAuthPrincipal}</li>
     * <li>{@link Authentication#getCredentials()} => {@link TokenDto}</li>
     * <li>{@link Authentication#getAuthorities()} => (single) {@link UserRole}</li>
     * </ul>
     * 
     * @param token
     * @param jwtSecret
     * @return Can be used for authorization
     */
    private static @Nonnull Optional<UsernamePasswordAuthenticationToken> decodeToken(@Nonnull String token,
            @Nonnull String jwtSecret) {
        var signingKey = jwtSecret.getBytes();
        var parsedToken = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token.replace("Bearer ", ""));
        var username = parsedToken.getBody().getSubject();
        List<UserRole> authorities = ((List<?>) parsedToken.getBody().get("rol"))
                .stream()
                .map(role -> UserRole.DEFAULT.getEnum((String) role))
                .collect(Collectors.toList());
        Date expiration = parsedToken.getBody().getExpiration();
        var realm = (String) parsedToken.getBody().get("realm");
        
        @Nonnull Optional<UsernamePasswordAuthenticationToken> tokenObject = notNull(Optional.empty());
        if (!StringUtils.isEmpty(username)) {
            SparkysAuthPrincipal principal = new AuthPrincipalImpl(realm, username);
            var tokenDto = new TokenDto();
            tokenDto.expiration = SparkyUtil.expirationDateAsString(expiration);
            tokenDto.token = token;
            tokenObject = notNull(
                    Optional.of(new UsernamePasswordAuthenticationToken(principal, tokenDto, authorities))
                );
        }
        return tokenObject;
    }

    /**
     * Extracts information of a given token. The secret must be the same as the secret which was used 
     * to encode the JWT token. <br>
     * The returned authentication contains:<br>
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => {@link SparkysAuthPrincipal}</li>
     * <li>{@link Authentication#getCredentials()} => {@link TokenDto}</li>
     * <li>{@link Authentication#getAuthorities()} => (single) {@link UserRole}</li>
     * </ul>
     * 
     * @param token - JWT token as string
     * @param jwtSecret - Is used to decode the token; must be the same which was used for encoding
     * @throws JwtTokenReadException
     * @return Springs authentication token
     */
    @Nonnull
    public static UsernamePasswordAuthenticationToken readJwtToken(String token, String jwtSecret) 
            throws JwtTokenReadException {
        try {
            /*
             * Token and jwtSecret are not allowed to be null. In this lambda statement we can't be sure this is the 
             * case. But because this is a lazy init, this is executed when get() is called. This is done
             * after necessary null-checks.
             */
            @SuppressWarnings("null") var lazyTokenObj = Lazy.of(() -> decodeToken(token, jwtSecret));
            if (token == null || jwtSecret == null || lazyTokenObj.get().isEmpty() ) {
                throw new IllegalArgumentException("Couldn't decode JWT Token with given information");
            }
            return notNull(lazyTokenObj.get().get());
        } catch (ExpiredJwtException exception) {
            LOG.debug("Request to parse expired JWT : {} failed : {}", token, exception.getMessage());
            throw new JwtTokenReadException("Expired token");
        } catch (UnsupportedJwtException exception) {
            LOG.debug("Request to parse unsupported JWT : {} failed : {}", token, exception.getMessage());
            throw new JwtTokenReadException("Unsupported JWT");
        } catch (MalformedJwtException exception) {
            LOG.debug("Request to parse invalid JWT : {} failed : {}", token, exception.getMessage());
            throw new JwtTokenReadException("Invalid JWT");
        } catch (SignatureException exception) {
            LOG.debug("Request to parse JWT with invalid signature : {} failed : {}", token, exception.getMessage());
            throw new JwtTokenReadException("Invalid signature");
        } catch (IllegalArgumentException exception) {
            LOG.debug("Request to parse empty or null JWT : {} failed : {}", token, exception.getMessage());
            throw new JwtTokenReadException(exception.getMessage());
        }
    }

    /**
     * Extracts information of a given token and verify/extends them with information of a storage. 
     * The secret must be the same as the secret which was used to encode the JWT token. <br>
     * The returned authentication contains:<br>
     * 
     * <ul>
     * <li>{@link Authentication#getPrincipal()} => {@link SparkyUser}</li>
     * <li>{@link Authentication#getCredentials()} => {@link TokenDto}</li>
     * <li>{@link Authentication#getAuthorities()} => (single) {@link UserRole}</li>
     * </ul>
     * 
     * @param token - JWT token as string
     * @param jwtSecret - Is used to decode the token; must be the same which was used for encoding
     * @param service - Transformer which should be used for completing information from a storage
     * @throws JwtTokenReadException
     * @return Springs authentication token
     */
    public static UsernamePasswordAuthenticationToken readJwtToken(String token, String jwtSecret,
            UserExtractionService service) throws JwtTokenReadException {
        var tokenObj = readJwtToken(token, jwtSecret);
        try {
            var user = service.extractAndRefresh(tokenObj);
            tokenObj = new UsernamePasswordAuthenticationToken(user, tokenObj.getCredentials(), user.getAuthorities());
        } catch (UserNotFoundException e) {
            
        }
        return tokenObj;
    }
}
