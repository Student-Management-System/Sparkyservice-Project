package net.ssehub.sparkyservice.api.routing;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.util.Lazy;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;

/**
 * Interpreter for {@link ZuulRoutes}.
 * 
 * @author marcel
 */
public class AccessControlListInterpreter { 

    public static final String NO_ACL = "none";
    public static final String ANY = "any";

    private Lazy<String[]> allowedUsersForPath;
    private final String currentPath;

    /**
     * Provides methods for interpreting paths in zuul configuration.
     * 
     * @author marcel
     */
    class ConfigurationInterpreter {

        @Nonnull
        private final Optional<ZuulRoutes> routes;
        
        /**
         * Takes zuul routes and tries to interpret them in different ways. This tool provides 
         * 
         * @param routes
         */
        public ConfigurationInterpreter(ZuulRoutes routes) {
            this.routes = notNull(Optional.ofNullable(routes));
        }

        /**
         * Tries to identify a configured path from the zuul configuration throug a given path. 
         * 
         * @param currentPath
         * @return Only returns a value if a unique conf-path was found
         */
        Optional<String> identifyConfiguredProxyRoute(String currentPath) {
            var array = currentPath.split("\\/")[0]; // testpath/test/something => testpath
            String confPath = routes.map(ZuulRoutes::getConfiguredPaths)
                                    .orElseGet(ArrayList::new)
                                    .stream()
                                    .filter(array::equals)
                                    .reduce("", String::concat);
            return Optional.of(confPath).filter(p -> !p.isBlank());
        }
        
        /**
         * Returns the ACL extracted from zuul configuration.
         * 
         * @param configuredPath ACL is returned for this path
         * @return Allowed users for configuredPath
         */
        Optional<String[]> getAcl(String configuredPath) {
            return routes.map(ZuulRoutes::getRoutes)
                         .map(map -> map.get(configuredPath + ".acl"))
                         .map(acl -> acl.split(","));
        }
        
        /**
         * Returns the allowed users for a {@link AccessControlListInterpreter#currentPath}. 
         * 
         * @return Allowed users for current path
         */
        @Nonnull
        public String[] getAllowedUsersForCurrentPath() {
            return notNull(
                this.getAcl(currentPath)
                    .or(() -> identifyConfiguredProxyRoute(currentPath).flatMap(this::getAcl))
                    .orElse(new String[] {NO_ACL}) // allow all when no routes are protected
            );
        }
    }

    /**
     * Interpreter for ACL. 
     * 
     * @param zuulRoutes - Configuration for routes containing ACL
     * @param currentPath - The requested resource or path
     */
    public AccessControlListInterpreter(@Nullable ZuulRoutes zuulRoutes, @Nullable String currentPath) {
        this.currentPath = Optional.ofNullable(currentPath)
                                   .map(AccessControlListInterpreter::removeSlash)
                                   .orElse(StringUtils.EMPTY);
        this.allowedUsersForPath = new Lazy<String[]>(
            () -> new ConfigurationInterpreter(zuulRoutes).getAllowedUsersForCurrentPath()
        );
    }

    

    /**
     * Removes a slashes from the beginning and from the end of a given string.
     * 
     * @param path
     * @return Same string without slash at start or at end
     */
    public static String removeSlash(String path) {
        path = removeStartSlash(path);
        return removeTrailingSlash(path);
    }

    /**
     * Removes one or more slashes from the beginning of the given string when present.
     * 
     * @param path
     * @return Same path without slashes at the beginning.
     */
    public static String removeStartSlash(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return path;
    }

    /**
     * Removes one or more trailing slashes from the given path.
     * 
     * @param path
     * @return Substring without trailing slashes
     */
    public static String removeTrailingSlash(String path) {
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Checks if the current username is on the permitted list.
     * 
     * @param currentUser
     * @return true if the current user is configured to pass the zuul path
     */
    public boolean isUsernameAllowed(@Nonnull String currentUser) {
        return !isAclEnabled() || Arrays.stream(allowedUsersForPath.get())
            .anyMatch(currentUser::equalsIgnoreCase);
    }

    /**
     * The path which the current interpreter is configured for.
     * 
     * @return configured path
     */
    public String getConfiguredPath() {
        return currentPath;
    }
    
    /**
     * Indicator if an ACL is present for the current path.
     * 
     * @return <code> true when the ACL is currently enabled </code>
     */
    public boolean isAclEnabled() {
        return !allowedUsersForPath.get()[0].equalsIgnoreCase(NO_ACL);
    }
}