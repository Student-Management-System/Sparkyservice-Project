package net.ssehub.sparkyservice.api.routing;

import javax.servlet.http.HttpServletRequest;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.ZuulFilter;

import net.ssehub.sparkyservice.api.auth.JwtAuth;
import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;

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
        return 10;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        boolean ctxValid = ctx.get("proxy") != null && ctx.get("proxy") instanceof String;
        if (ctxValid) {
            HttpServletRequest request = ctx.getRequest();
            String proxyPath = (String) ctx.get("proxy");
            String header = ctx.getRequest().getHeader(jwtConf.getHeader());
            log.debug(String.format("%s request to %s", request.getMethod(), request.getRequestURL().toString()));
            if (header == null) {
                return true;
            }
            var authentication = JwtAuth.readJwtToken(header, jwtConf.getSecret());
            if (authentication != null) {
                String name = authentication.getName();
                var spPrincipal = (SparkysAuthPrincipal) authentication.getPrincipal();
                UserRealm realm = spPrincipal.getRealm();
                String fullAuthenticatedUsername = name + "@" + realm.name();
                var routesConfig = zuulRoutes.getRoutes();

                String users[] = routesConfig.get(proxyPath + ".protectedBy").split(",");
                for (String user : users) {
                    if (fullAuthenticatedUsername.equalsIgnoreCase(user) || (user.equalsIgnoreCase("none") || user.equals("")) ) {
                        return false;
                    }
                }
                log.info("{} is not allowed to access {}", fullAuthenticatedUsername, proxyPath);
            }
        }
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        String proxyPath = (String) ctx.get("proxy");
        log.info("Denied access to {}", proxyPath);
        ctx.setSendZuulResponse(false);
        ctx.setResponseBody("API key not authorized");
        ctx.getResponse().setHeader("Content-Type", "text/plain;charset=UTF-8");
        ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
        return null;
    }
}