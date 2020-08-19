package net.ssehub.sparkyservice.api.routing;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.auth.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.conf.SpringConfig;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

/**
 * Authorization filter for configured zuul routes with zero overhead (and no database operations). 
 * 
 * @author Marcel
 */
public class ZuulAuthorizationFilter extends ZuulFilter {

    public static final String PROXY_AUTH_HEADER = "Proxy-Authorization";
    public static final String NO_ACL = "none";
    private static Logger log = LoggerFactory.getLogger(ZuulAuthorizationFilter.class);
    
    @Autowired
    private ZuulRoutes zuulRoutes;
    
    @Autowired
    private JwtSettings jwtConf;

    @Autowired
    @Qualifier(SpringConfig.LOCKED_JWT_BEAN)
    private Set<String> lockedJwtToken;


    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 999;
    }

    static <T> Consumer<T> consumer(Consumer<T> c) {
        return arg -> {
            try {
                c.accept(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, R> Function<T, R> function(Function<T, R> f) {
        return arg -> {
            try {
                return f.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Creates a full identifier.
     * <br>Style: <code>user@REALM</code>
     * 
     * @param auth - Typically extracted by an JWT token
     * @return fullIdentName
     */
    private static @Nonnull String getUserIdentifier(UsernamePasswordAuthenticationToken auth) {
        String name = auth.getName();
        var spPrincipal = (SparkysAuthPrincipal) auth.getPrincipal();
        UserRealm realm = spPrincipal.getRealm();
        return name + "@" + realm.name();
    }

    /**
     * Checks if the current username is on the permitted list.
     * 
     * @param allowedUsers List of username
     * @param currentUser
     * @return true if the current user is configured to pass the zuul path
     */
    private static boolean isUsernameAllowed(@Nonnull String[] allowedUsers, @Nonnull String currentUser) {
        return Arrays.stream(allowedUsers).anyMatch(currentUser::equalsIgnoreCase);
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        boolean ctxValid = ctx.get("proxy") != null && ctx.get("proxy") instanceof String;
        if (!ctxValid) {
            log.warn("No proxy attempt but filter is executed. Deny access");
            blockRequest(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ctxValid;
    }

    /**
     * Checks if the user is authorized to access the desired path. If not, the
     * request wont be forwarded.
     */
    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.debug(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
        
        String proxyPath = (String) ctx.get("proxy");
        String[] aclList = zuulRoutes.getRoutes().get(proxyPath + ".protectedBy").split(",");
        Optional<String> header = Optional.ofNullable(ctx.getRequest().getHeader(PROXY_AUTH_HEADER));
        
        /*
         * Wildcards are possible
         */
        Predicate<String> jwtNonLocked = currentJwt -> !lockedJwtToken.stream().anyMatch(currentJwt::contains);

        /* 
         * Will throw ArrayOutOfBounds when administrator didn't configure any ACL. If not wished, change it here
         * when the config is set to "none" everyone is  allowed to access => return true
         */
        boolean aclDisabled = aclList[0].equalsIgnoreCase(NO_ACL);
        
        if (!aclDisabled) {
            header.filter(jwtNonLocked)
                .flatMap(this::getAuthenticatedUser)
                .map(name -> isUsernameAllowed(aclList, notNull(name)))
                .filter(allow -> allow.booleanValue())
                .ifPresentOrElse(
                    e -> log.debug("Access granted to {}", proxyPath), 
                    () ->  {
                        log.info("Denied access to {} with: {}", proxyPath, header.orElseGet(() -> "<no auth token>"));
                        blockRequest(HttpStatus.FORBIDDEN);
                    }
                );
        }
        return null;
    }

    /**
     * Extracts a full username from a JWT token which can be used as full identifier. <br>
     * Style: <code>user@REALM</code>
     * <br><br>
     * In order to do this, the given authHeader must be a valid token (with Bearer keyword).
     * 
     * @param authHeader Authorization header from the request where the JWT Token
     *                   is stored
     * @return Optional Username with. Optional is empty when no valid token was given
     */
    private @Nonnull Optional<String> getAuthenticatedUser(@Nullable String authHeader) {
        Optional<String> fullUserNameRealm;
        try {
            fullUserNameRealm = Optional.of(getTokenObject(authHeader))
                    .map(ZuulAuthorizationFilter::getUserIdentifier);
        } catch (JwtTokenReadException e) {
            log.debug("Could not read JWT token: {}", e.getMessage());
            fullUserNameRealm = Optional.empty();
        }
        if (fullUserNameRealm.isEmpty()) {
            log.debug("No authorization header provided");
        }
        return fullUserNameRealm;
    }
    
    private UsernamePasswordAuthenticationToken getTokenObject(@Nullable String token) throws JwtTokenReadException {
        return JwtAuth.readJwtToken(token, jwtConf.getSecret());
    }
    
    /**
     * Configure the zuul tool chain to not sending a response to the client which is
     * equivalent to blocking the request. While doing this, it sets a proper HTTP
     * status.
     * 
     * @param returnStatus - The HTTP status to set in the response
     */
    private void blockRequest(@Nonnull HttpStatus returnStatus) {
        RequestContext ctx = RequestContext.getCurrentContext();
        String message = null;
        if (returnStatus == HttpStatus.FORBIDDEN) {
            message = "API key not authorized for this location";
        } else if (returnStatus == HttpStatus.UNAUTHORIZED) {
            message = "Not authorized. Please use Proxy-Authorization header for authorization";
        }
        String errorJson = new ErrorDtoBuilder().newError(message, returnStatus, ctx.getRequest().getContextPath())
                .buildAsJson();
        ctx.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
        ctx.setResponseBody(errorJson);
        ctx.removeRouteHost();
        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(returnStatus.value());
    }
}