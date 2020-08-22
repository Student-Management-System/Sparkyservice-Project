package net.ssehub.sparkyservice.api.routing;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import net.ssehub.sparkyservice.api.auth.AuthenticationReader;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.conf.SpringConfig;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

/**
 * Authorization filter for configured zuul routes with zero overhead (and no database operations). 
 * 
 * @author Marcel
 */
public class ZuulAuthorizationFilter extends ZuulFilter {

    public static final String PROXY_AUTH_HEADER = "Proxy-Authorization";
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

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        boolean ctxValid = ctx.get("proxy") != null && ctx.get("proxy") instanceof String;
        if (!ctxValid) {
            log.warn("No proxy attempt but filter is executed. Deny access");
            blockRequest(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (jwtConf == null) {
            ctxValid = false;
            log.error("No jwt conf present in zuul authorization filter");
            blockRequest(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if (zuulRoutes == null) {
            ctxValid = false;
            log.debug("No zuul route configuration");
            
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
        Optional<String> header = Optional.ofNullable(ctx.getRequest().getHeader(PROXY_AUTH_HEADER));
        var aclInterpreter = new AccessControlListInterpreter(zuulRoutes, proxyPath);
        
        /*
         * Wildcards are possible
         */
        // TODO extra class for JWT and account locking
        Predicate<String> jwtNonLocked = currentJwt -> !lockedJwtToken.stream().anyMatch(currentJwt::contains);
        
        if (aclInterpreter.isAclEnabled()) {
            header.filter(jwtNonLocked)
                .map(token -> new AuthenticationReader(notNull(jwtConf), token))
                .flatMap(reader -> reader.getAuthenticatedUserIdent())
                .filter(aclInterpreter::isUsernameAllowed)
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
        String errorJson = new ErrorDtoBuilder().newError(message, returnStatus, (String) ctx.get("proxy"))
                .buildAsJson();
        ctx.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
        ctx.setResponseBody(errorJson);
        ctx.removeRouteHost();
        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(returnStatus.value());
    }
}