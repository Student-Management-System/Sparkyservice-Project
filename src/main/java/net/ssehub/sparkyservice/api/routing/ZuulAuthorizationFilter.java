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
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

import net.ssehub.sparkyservice.api.auth.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.exception.AuthorizationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.user.Identity;

/**
 * Performant Authorization Filter for zuul routes without CRUD-operations. 
 * 
 * @author Marcel
 */
public class ZuulAuthorizationFilter extends ZuulFilter {
    
    public static final String PROXY_AUTH_HEADER = "Proxy-Authorization";

    private static final Logger LOGGER = LoggerFactory.getLogger(ZuulAuthorizationFilter.class);

    @Autowired
    private ZuulRoutes zuulRoutes;

    @Autowired
    @Nonnull
    private JwtTokenService jwtService;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldFilter() {
        LOGGER.trace("Incoming request");
        allowAuthorizationHeader();
        boolean contextValid = getProxyPath() != null;
        if (zuulRoutes == null || zuulRoutes.getRoutes() == null) { 
            zuulRoutes = emergencyConfLoad();
            contextValid = shouldFilter();
            LOGGER.debug("No zuul route configuration but filter is still executed");
        }
        if (!contextValid) {
            LOGGER.warn("Block accesss; missing information - POSSIBLE SERVER FAULT");
            throw new RuntimeException("Missing context informationen. Internal server error");
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
    private ZuulRoutes emergencyConfLoad() {
        LOGGER.debug("Configured routes: " + zuulRoutes.getRoutes());
        var servletContext = RequestContext.getCurrentContext().getRequest().getServletContext();
        var webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        ZuulRoutes routes = webApplicationContext.getBean(ZuulRoutes.class);
        if (routes != null && routes.getRoutes() != null) {
            LOGGER.info("Found zuul route configuration through emergency load");
        } else {
            LOGGER.warn("Routing filter is executed but there is no proxy attempt (no proxy header) - Deny access");
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
    private static String getProxyPath() {
        var ctx = RequestContext.getCurrentContext();
        String proxyPath = (String) ctx.get(FilterConstants.PROXY_KEY);
        if (proxyPath == null) { // (No idea why this is necessary some times)
            LOGGER.debug("Possible error: No proxy field available (trying to extract requested URI from request)");
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
    private void allowAuthorizationHeader() {
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
    private static HttpServletRequest logAndGetRequest() {
        LOGGER.trace("Running filter");
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        LOGGER.debug("{} request to {}", request.getMethod(), request.getRequestURL().toString());
        LOGGER.debug("Forward target: {} or {}", ctx.get(FilterConstants.FORWARD_TO_KEY), ctx.get("routeHost"));
        return request;
    }
    /**
     * {@inheritDoc}
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
            Identity user = notNull(header.map(token -> new JwtAuthReader(jwtService, token))
                .flatMap(JwtAuthReader::getAuthenticatedUserIdent)
                .orElseThrow(AuthenticationException::new));
            if (!aclInterpreter.isAllowed(user)) {
                LOGGER.debug("Denied access to {} with: {}", proxyPath, header.orElseGet(() -> "<no auth token>"));
                throw new AuthorizationException( user);
            }
        } else {
            LOGGER.debug("ACL for {} is disabled - Allow all", proxyPath);
        }
        return null;
    }

}