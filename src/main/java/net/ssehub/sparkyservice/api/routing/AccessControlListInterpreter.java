package net.ssehub.sparkyservice.api.routing;

import java.util.Arrays;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues.ZuulRoutes;

/**
 * Interpreter for {@link ZuulRoutes}.
 * 
 * @author marcel
 */
public class AccessControlListInterpreter { 

    public static final String NO_ACL = "none";
    public static final String ANY = "any";

    private final String[] allowedUsersForPath;
    private final String currentPath;

    /**
     * Interpreter for ACL. 
     * 
     * @param zuulRoutes - Configuration for routes containing ACL
     * @param currentPath - The requested resource or path
     */
    public AccessControlListInterpreter(@Nullable ZuulRoutes zuulRoutes, @Nullable String currentPath) {
        this.currentPath = Optional.ofNullable(currentPath).orElse(StringUtils.EMPTY);
        allowedUsersForPath = Optional.ofNullable(zuulRoutes)
            .map(routes -> routes.getRoutes())
            .map(map-> map.get(this.currentPath + ".acl"))
            .map(allowedUsers -> allowedUsers.split(","))
            .orElseGet(() -> new String[] {NO_ACL}); // allow all when no routes are protected
    }

    /**
     * Checks if the current username is on the permitted list.
     * 
     * @param currentUser
     * @return true if the current user is configured to pass the zuul path
     */
    public boolean isUsernameAllowed(@Nonnull String currentUser) {
        return !isAclEnabled() || Arrays.stream(allowedUsersForPath).anyMatch(currentUser::equalsIgnoreCase);
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
        return !allowedUsersForPath[0].equalsIgnoreCase(NO_ACL);
    }
}