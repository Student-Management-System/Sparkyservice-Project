package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

@Service
public class HeavyUserTransformerImpl implements UserTransformer {

    @Autowired
    private IUserService userSerivce;
    private final Logger log = LoggerFactory.getLogger(HeavyUserTransformerImpl.class);

    @Override
    public @Nonnull User extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException {
        if (details instanceof LdapUserDetails) {
            try {
                return userSerivce.findUserByNameAndRealm(details.getUsername(), UserRealm.LDAP);
            } catch (UserNotFoundException e){
                // probably the first time this user is authenticated?
                return new User(notNull(details.getUsername()), null, UserRealm.LDAP, true, UserRole.DEFAULT);
            }
        } else if (details instanceof org.springframework.security.core.userdetails.User) {
            return createMemoryUser((org.springframework.security.core.userdetails.User) details);
        } else if (details instanceof LocalUserDetails || details instanceof User) {
            var user = (User) details;
            return notNull(user);
        } else if (details != null) {
            var userList = userSerivce.findUsersByUsername(details.getUsername());
            if (userList.size() == 1) {
                return notNull(userList.get(0));
            } else {
                log.warn("More user than one with the same username found. Can't choose any. Provide realm "
                        + "information to complete request.");
                throw new UserNotFoundException("Arbitrary user found");
            }
        }
        throw new UserNotFoundException("");
    }

    public @Nonnull User createMemoryUser(org.springframework.security.core.userdetails.User memoryUser) {
        String username = Optional.ofNullable(memoryUser.getUsername()).orElse("");
        String password = Optional.ofNullable(memoryUser.getPassword()).orElse("");
        UserRole role = getRoleFromAuthority(memoryUser.getAuthorities());
        return new User(notNull(username), new Password(notNull(password)), UserRealm.MEMORY, memoryUser.isEnabled(), 
                role);
    }

    private @Nonnull UserRole getRoleFromAuthority(@Nullable Collection<? extends GrantedAuthority> authorities) {
        if (authorities != null && authorities.size() == 1) {
            var authority = (GrantedAuthority) authorities.toArray()[0];
            try {
                return UserRole.DEFAULT.getEnum(authority.getAuthority());
            } catch (IllegalArgumentException e){
                log.warn("Invalid role found:" + authorities.toString() + ". Using default role");
            }
            return UserRole.DEFAULT;
        } else {
            throw new UnsupportedOperationException(
                    "Not supported: Can't parse user no role or with more than one role.");
        }
    }

    @Override
    public @Nonnull User extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException {
        if (principal != null) {
            userSerivce.findUserByNameAndRealm(principal.getName(), principal.getRealm());
        }
        throw new UserNotFoundException("Can't find user: null");
    }

    @Override
    public @Nullable User extendFromAny(@Nullable Object principal) throws MissingDataException, UserNotFoundException {
        if (principal instanceof SparkysAuthPrincipal) {
            return extendFromSparkyPrincipal((SparkysAuthPrincipal) principal);
        } else if (principal instanceof UserDetails ){
            return extendFromUserDetails((UserDetails) principal);
        } else if (principal instanceof UsernamePasswordAuthenticationToken) {
            var auth = (Authentication) principal;
            var authPrincipal = (SparkysAuthPrincipal) auth.getPrincipal();
            var role = getRoleFromAuthority(auth.getAuthorities());
            return new User(authPrincipal.getName(), null, UserRealm.MEMORY, true, role);
        }
        throw new MissingDataException("Principal implementation not known.");
    }

    @Override
    public @Nonnull User extendFromUserDto(@Nullable UserDto userDto) throws MissingDataException, UserNotFoundException {
        if (userDto != null && userDto.username != null && userDto.realm != null) {
            return userSerivce.findUserByNameAndRealm(userDto.username, userDto.realm);
        }
        throw new MissingDataException("Identifier not provided");
    }
}