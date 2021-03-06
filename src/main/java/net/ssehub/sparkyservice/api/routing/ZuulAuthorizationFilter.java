package net.ssehub.sparkyservice.api.routing;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import net.ssehub.sparkyservice.api.auth.AdditionalAuthInterpreter;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

/**
 * Authorization filter for configured zuul routes with zero overhead (and no database operations). 
 * 
 * @author Marcel
 */
public class ZuulAuthorizationFilter extends ZuulFilter {

    public static final String PROXY_AUTH_HEADER = "Proxy-Authorization";

    @Nonnull
    private static Logger log = notNull(LoggerFactory.getLogger(ZuulAuthorizationFilter.class));

    @Autowired
    private ZuulRoutes zuulRoutes;

    @Autowired
    @Nonnull
    private JwtTokenService jwtService;

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldFilter() {
        log.trace("Incoming request");
        allowAuthorizationHeader();
        boolean contextValid = getProxyPath() != null;
        if (zuulRoutes == null || zuulRoutes.getRoutes() == null) { 
            zuulRoutes = emergencyConfLoad();
            contextValid = shouldFilter();
            log.debug("No zuul route configuration but filter is executed");
        }
        if (!contextValid) {
            log.warn("Block access during missing information - POSSIBLE SERVER FAULT");
            blockRequest(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return contextValid;
    }

    /**
     * Especially during system and integration tests, sometimes the routes aren't correctly loaded when this filter
     * is started. This method loads the bean defnition afterwards. <br>
     * This method does not load them from the file!
     * 
     * @return Configured zuul routes from a configuration file
     */
    // (No idea why this is necessary some times)
    public ZuulRoutes emergencyConfLoad() {
        log.debug("Configured routes: " + zuulRoutes.getRoutes());
        var servletContext = RequestContext.getCurrentContext().getRequest().getServletContext();
        var webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        ZuulRoutes routes = webApplicationContext.getBean(ZuulRoutes.class);
        if (routes != null && routes.getRoutes() != null) {
            log.info("Found zuul route configuration through emergency load");
        } else {
            log.warn("Routing filter is executed but there is no proxy attempt (no proxy header) - Deny access");
        }
        return routes;
    }

    /**
     * A proxy path is the location which the client requested. This path is always a configured one in zuul and 
     * is forwarded to another path. 
     * Sometimes it is necessary to load the path directly from the request. Through that, maybe the whole 
     * path is returned instead of just the configured location. <br>
     * <br>
     * <code>
     * Conf: testpath = forward => google.com <br>
     * Request: testpath/search/something = forward => google.com/search/something<br>
     * </code>
     * "Testpath" is the proxy path. Sometimes it is possible that "testpath/search/something" is returned.
     * 
     * @return Request path of the user
     */
    public String getProxyPath() {
        var ctx = RequestContext.getCurrentContext();
        String proxyPath = (String) ctx.get(FilterConstants.PROXY_KEY);
        if (proxyPath == null) { // (No idea why this is necessary some times)
            log.debug("Possible error: No proxy field available (trying to extract requested URI from request)");
            proxyPath = RequestContext.getCurrentContext().getRequest().getPathInfo();
            proxyPath = AccessControlListInterpreter.removeStartSlash(proxyPath);
        }
        return proxyPath;
    }

    /**
     * Allow authorization header while proxying a request.
     * This is necessary because the zuul config "sensitiveHeaders" is not working properly.
     * 
     * @see https://stackoverflow.com/questions/36359915/
     *  authorization-header-not-passed-by-zuulproxy-starting-with-brixton-rc1
     */
    public void allowAuthorizationHeader() {
        var ctx = RequestContext.getCurrentContext();
        // Alter ignored headers as per: https://gitter.im/spring-cloud/spring-cloud?at=56fea31f11ea211749c3ed22
        @SuppressWarnings("unchecked") Set<String> headers = (Set<String>) ctx.get("ignoredHeaders");
        if (headers != null) {
            headers.remove("authorization");
        }
    }

    /**
     * Does the logging and extracting the request.
     * 
     * @return Request context of the request which leads to this filter invocation
     */
    private HttpServletRequest logAndGetRequest() {
        log.trace("Running filter");
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.debug("{} request to {}", request.getMethod(), request.getRequestURL().toString());
        log.debug("Forward target: {} or {}", ctx.get(FilterConstants.FORWARD_TO_KEY), ctx.get("routeHost"));
        return request;
    }
    /**
     * Checks if the user is authorized to access the desired path. If not, the
     * request wont be forwarded.
     */
    @Override
    public Object run() {
        HttpServletRequest request = logAndGetRequest();
        String proxyPath = getProxyPath();
        Optional<String> header = Optional.ofNullable(request.getHeader(PROXY_AUTH_HEADER));
        var aclInterpreter = new AccessControlListInterpreter(zuulRoutes, proxyPath);
        if (aclInterpreter.isAclEnabled()) {
            header.map(token -> new AdditionalAuthInterpreter(jwtService, token, log))
                .flatMap(AdditionalAuthInterpreter::getAuthenticatedUserIdent)
                .filter(aclInterpreter::isUsernameAllowed)
                .ifPresentOrElse(ident -> log.debug("Access granted to {}, user: {}", proxyPath, ident), 
                    () ->  {
                        log.info("Denied access to {} with: {}", proxyPath, header.orElseGet(() -> "<no auth token>"));
                        blockRequest(HttpStatus.FORBIDDEN);
                    }
                );
        } else {
            log.debug("ACL for {} is disabled - Allow all", proxyPath);
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
        log.debug("Blocking access with status {}", returnStatus);
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