package net.ssehub.sparkyservice.api.routing;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import net.ssehub.sparkyservice.api.auth.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.AuthorizationException;
import net.ssehub.sparkyservice.api.auth.identity.NoSuchRealmException;

/**
 * Exception handling error-Filter for Zuul. Handles authentication and
 * authorization erros and sets the respective HTTP-Status codes.
 * 
 * @author marcel
 *
 */
@Component
public class ZuulAuthErrorFilter extends ZuulFilter {

    @Override
    public String filterType() {
        return FilterConstants.ERROR_TYPE;
    }

    @Override
    public int filterOrder() {
        return -1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext context = RequestContext.getCurrentContext();
        Throwable throwable = context.getThrowable();
        if (throwable instanceof ZuulException) {
            var causeException = throwable.getCause();
            if (causeException instanceof AuthenticationException) {
                String msg = "For this path an authentication is required. Please use the Proxy-Authorization header";
                context.setThrowable(new ZuulException(msg, UNAUTHORIZED.value(), UNAUTHORIZED.getReasonPhrase()));
            } else if (causeException instanceof AuthorizationException) {
                var ex = (AuthorizationException) causeException;
                String msg = ex.getCauseUser().asUsername() + " is not whitelist for this ressource";
                context.setThrowable(new ZuulException(msg, FORBIDDEN.value(), FORBIDDEN.getReasonPhrase()));
            } else if (causeException instanceof NoSuchRealmException) {
                String msg = "Invalid username";
                context.setThrowable(new ZuulException(msg, BAD_REQUEST.value(), msg));
            } else {
                context.setThrowable(new ZuulException(causeException.getMessage(), 
                        INTERNAL_SERVER_ERROR.value(), 
                        INTERNAL_SERVER_ERROR.getReasonPhrase()));
            }
        }
        return null;
    }

}