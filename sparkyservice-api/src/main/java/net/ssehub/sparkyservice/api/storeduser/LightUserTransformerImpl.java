package net.ssehub.sparkyservice.api.storeduser;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;
import static net.ssehub.sparkyservice.api.conf.ConfigurationValues.REALM_LDAP;
import static net.ssehub.sparkyservice.api.conf.ConfigurationValues.REALM_UNKNOWN;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.storeduser.exceptions.MissingDataException;
import net.ssehub.sparkyservice.db.user.StoredUser;
import net.ssehub.sparkyservice.util.NullHelpers;

public final class LightUserTransformerImpl implements StoredUserTransformer {

    @Autowired
    private IStoredUserService userSerivce;
    private final Logger log = LoggerFactory.getLogger(StoredUserTransformer.class);

    /**
     * Should only be used for springs bean definition. Otherwise this could lead to unexpected behavior.
     */
    public LightUserTransformerImpl() {}
    LightUserTransformerImpl(IStoredUserService userService) {
        this.userSerivce = userService;
    }
    
    /**
     * {@inheritDoc}
     * 
     * This implementation tries to be lightweight as possible and get all necessary information from the
     * provided user details without doing database operations. If too much information are missing, 
     * it tries to load them from the data backend (database). 
     */
    @Override
    public @Nonnull StoredUser extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException {
        if (details != null && details.getUsername() != null && !details.getUsername().isEmpty()) {
            StoredUser storeUser; 
            try {
                storeUser = castFromUserDetails(details);
                if(storeUser.getRole().isEmpty()) {
                    throw new MissingDataException(null);
                }
            } catch (MissingDataException e) {
                /* possible overrides values: The user in details could be enabled, but 
                 * the (newer) user in the database is disabled
                 */
                storeUser = userSerivce.findUserByNameAndRealm(details.getUsername(), REALM_LDAP);
            }
            return storeUser;
        }
        throw new UserNotFoundException("User could not be casted. Username not found");
    }

    /**
     * Cast user details to stored user and identifies the realm through the implementation. 
     * <br>
     * Supported implementations: <br>
     * <ul><li> {@link StoredUserDetails}</li> 
     * <li>{@link LdapUserDetailsImpl}</li></ul>
     * Otherwise the method returns a {@link StoredUser} which may is incomplete and is in an "UNKNOWN" realm.
     * 
     * @param details typically provided by spring security during authentication process
     * @return StoredUser which holds data from the given details
     * @throws Thrown if the provided information are too less in order to create an object and the user is not found
     * in the database
     */
    @Override
    public @Nonnull StoredUser castFromUserDetails(@Nullable UserDetails details) throws MissingDataException {
        StoredUser storeUser = null;
        if (details instanceof LdapUserDetailsImpl) {
            String role = getRoleFromAuthority(details.getAuthorities());
            storeUser = new StoredUser(
                    notNull(details.getUsername()),
                    null, 
                    REALM_LDAP, 
                    details.isEnabled(),
                    role);
        } else if (details instanceof StoredUserDetails || details instanceof StoredUser)  {
            storeUser = (StoredUser) details;
        } else if (details != null) {
            String role = getRoleFromAuthority(details.getAuthorities());
            String username = Optional.ofNullable(details.getUsername()).orElse("");
            storeUser = new StoredUser(notNull(username), null, REALM_UNKNOWN, details.isEnabled(), role);
        }
        var returnVal = Optional.ofNullable(storeUser).orElseThrow(() -> new MissingDataException("User cast was not possible"
                + "with the provided information"));
        return NullHelpers.notNull(returnVal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nonnull StoredUser extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) 
            throws UserNotFoundException {
        // maybe find ligthweight cast
        if (principal == null) {
            throw new UserNotFoundException("User with name null could not be found or casted");
        }
        return userSerivce.findUserByNameAndRealm(principal.getName(), principal.getRealm());
    }

    private @Nonnull String getRoleFromAuthority(@Nullable Collection<? extends GrantedAuthority> authorities) {
        @Nonnull final String defaultRole = "ROLE_" + UserRole.DEFAULT.name();
        @Nonnull final String adminRole = "ROLE_" + UserRole.ADMIN.name();
        if (authorities != null && authorities.size() == 1) {
            for (var authority : authorities) {
                if (authority.getAuthority().equals(defaultRole)) {
                    return defaultRole;
                } else if (authority.getAuthority().equals(adminRole)) {
                    return adminRole;
                } else {
                    // if we want to use remote authorities (ex. from LDAP) set them here.
                    log.warn("Invalid role found:" + authorities.toString() + ". Using default role");
                }
            }
            return defaultRole;
        } else {
            throw new UnsupportedOperationException("Not supported: Can't parse user no role "
                    + "or with more than one role.");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * Supported principal instances: <br>
     * <ul><li> {@link SparkysAuthPrincipal}</li>
     * <li> {@link UserDetails}</li></ul>
     */
    @Override
    public @Nullable StoredUser extendFromAny(@Nullable Object principal) throws UserNotFoundException, MissingDataException {
        StoredUser user = null;
        if (principal instanceof SparkysAuthPrincipal) {
            var userPrincipal = (SparkysAuthPrincipal) principal;
            user = extendFromSparkyPrincipal(userPrincipal);
        } else if (principal instanceof UserDetails) {
            user = castFromUserDetails((UserDetails) principal);
        }
        return user;
    }
}
