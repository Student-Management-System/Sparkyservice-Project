package net.ssehub.sparkyservice.api.routing;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import com.netflix.zuul.context.RequestContext;

import io.jsonwebtoken.io.IOException;

import com.netflix.zuul.ZuulFilter;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;
import net.ssehub.sparkyservice.api.util.NullHelpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class ZuulAuthorizationFilter extends ZuulFilter {

    @Autowired
    private ZuulRoutes zuulRoutes;

    @Autowired
    private JwtSettings jwtConf;

    private static Logger log = LoggerFactory.getLogger(ZuulAuthorizationFilter.class);

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
        Map<String, String> routesConfig = zuulRoutes.getRoutes();
        String users[] = NullHelpers.notNull(routesConfig.get(proxyPath + ".protectedBy").split(","));
        String header = ctx.getRequest().getHeader(jwtConf.getHeader());
        String username = getFullUserNameWithRealm(header);
        if (!isUsernameAllowed(users, username)) {
            if (header == null) {
                log.info("Denied access to {}", proxyPath);
                return blockRequest(HttpStatus.UNAUTHORIZED);
            }
            log.info("{} is not allowed to access {}", username, proxyPath);
            return blockRequest(HttpStatus.FORBIDDEN);
        }
        log.debug("Granted access {} to {}", username, proxyPath);
        return null;
    }

    /**
     * Configure the zuul toolchain to not sending a response to the client which is
     * equivalent to blocking the request. While doing this, it sets a proper HTTP
     * status.
     * 
     * @param returnStatus The status to set
     * @return always null
     */
    private @Nullable Object blockRequest(@Nonnull HttpStatus returnStatus) {
        RequestContext ctx = RequestContext.getCurrentContext();
        String message = null;
        if (returnStatus == HttpStatus.UNAUTHORIZED) {
            message = "API key not authorized for this location";
        }
        String errorJson = new ErrorDtoBuilder().newError(message, returnStatus, ctx.getRequest().getContextPath())
                .buildAsJson();
        ctx.getResponse().setHeader("Content-Type", "application/json;charset=UTF-8");
        ctx.setResponseBody(errorJson);
        ctx.removeRouteHost();
        ctx.setSendZuulResponse(false);
        ctx.setResponseStatusCode(returnStatus.value());
        return null;
    }

    /**
     * Extracts a full username which can be used as full identifier. <br>
     * Example Style: <code>user@REALM</code>
     * 
     * @param authHeader Authorization header from the request where the JWT Token
     *                   is stored
     * @return Username with realm - or "none" when the user is not authenticated
     */
    public @Nonnull String getFullUserNameWithRealm(@Nullable String authHeader) {
        if (authHeader != null) {
            try {
                var authentication = JwtAuth.readJwtToken(authHeader, jwtConf.getSecret());
                if (authentication != null) {
                    String name = authentication.getName();
                    var spPrincipal = (SparkysAuthPrincipal) authentication.getPrincipal();
                    UserRealm realm = spPrincipal.getRealm();
                    return name + "@" + realm.name();
                }
            } catch (IOException e) {
                log.debug("Exception thrown: {}", e.getMessage());
            }
        }
        return "none";
    }

    /**
     * Checks if the current username is on the permitted list.
     * 
     * @param allowedUsers List of username or just "none"
     * @param currentUser
     * @return true if the current user is configured to pass the zuul path
     */
    private boolean isUsernameAllowed(@Nonnull String[] allowedUsers, @Nonnull String currentUser) {
        boolean userAllowed = Arrays.stream(allowedUsers).anyMatch(currentUser::equalsIgnoreCase);
        if (!userAllowed) {
            return allowedUsers[0].equalsIgnoreCase("none");
        }
        return userAllowed;
    }
}