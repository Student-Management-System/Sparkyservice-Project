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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        boolean ctxValid = ctx.get("proxy") != null && ctx.get("proxy") instanceof String;
        if (!ctxValid) {
            ctx.setSendZuulResponse(false);
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
        String users[] = routesConfig.get(proxyPath + ".protectedBy").split(",");
        String header = ctx.getRequest().getHeader(jwtConf.getHeader());
        String username = getFullUserNameWithRealm(header);
        boolean userAllowed = Arrays.stream(users).anyMatch(username::equalsIgnoreCase);
        if (!userAllowed) {
            if (header == null) {
                log.info("Denied access to {}", proxyPath);
                return blockRequest(HttpStatus.UNAUTHORIZED);
            }
            log.info("{} is not allowed to access {}", username, proxyPath);
            return blockRequest(HttpStatus.FORBIDDEN);
        }
        return null;
    }

    /**
     * Configure the zuul toolchain to not sending a response to the client which is
     * equivalent to blocking the request. While doing this, it sets a proper HTTP
     * status.
     * 
     * @param returnStatus The status to set
     */
    private Object blockRequest(HttpStatus returnStatus) {
        RequestContext ctx = RequestContext.getCurrentContext();
        switch (returnStatus) {
        case FORBIDDEN:
            ctx.setResponseBody("API key not authorized");
            break;
        case UNAUTHORIZED:
            ctx.setResponseBody("Not authenticated. Use authentication controller");
            ctx.getResponse().setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"TODO\"");
        default:
            break;
        }
        ctx.getResponse().setHeader("Content-Type", "text/plain;charset=UTF-8");
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
}