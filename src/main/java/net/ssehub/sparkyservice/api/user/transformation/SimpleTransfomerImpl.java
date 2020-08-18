package net.ssehub.sparkyservice.api.user.transformation;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.nio.channels.UnsupportedAddressTypeException;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.SparkysAuthPrincipal;
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.creation.UserFactoryProvider;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@Service
public class SimpleTransfomerImpl implements UserTransformerService {

    @Autowired
    private UserStorageService storageService;

    private Optional<SparkyUser> transformToSparkyUser(UserDetails details) {
        Optional<SparkyUser> optUser = Optional.empty();
        String passwordString = details.getPassword();
        if (passwordString != null) {
            var pw = new Password(passwordString, "UNKWN");
            optUser = Optional.of(
                UserFactoryProvider.getFactory(UserRealm.MEMORY)
                    .create(details.getUsername(), pw, getRole(details.getAuthorities()) , details.isEnabled())
            );
        }
        return optUser;
    }

    @Nonnull
    private static UserRole getRole(Collection<? extends GrantedAuthority> authorities) {
        String[] authList = authorities.toArray(new String[1]);
        return UserRole.DEFAULT.getEnum(authList[0]);
    }

    @Override
    @Nonnull
    public SparkyUser extendFromUserDetails(@Nullable UserDetails details) throws UserNotFoundException {
        SparkyUser user;
        if (details == null) {
            throw new UserNotFoundException("Cant find null user");
        } else if (details instanceof SparkyUser) {
            user = (SparkyUser) details;
        } else if (details instanceof User) { // mocked Spring user - always memory
            user = notNull(transformToSparkyUser(details).orElseThrow());
        } else {
            throw new UnsupportedOperationException("Unkown user type");
        }
        return user;
    }


    /**
     *  Fullfill information from a storage.
     */
    @Override
    @Nonnull
    public SparkyUser extendFromSparkyPrincipal(@Nullable SparkysAuthPrincipal principal) throws UserNotFoundException {
        if (principal == null) {
            throw new UserNotFoundException("Principal was null");
        }
        return storageService.findUserByNameAndRealm(principal.getName(), principal.getRealm());
    }

    /**
     * Fullfill information from a storage. 
     */
    @Override
    @Nullable
    public SparkyUser extendFromAnyPrincipal(@Nullable Object principal) throws MissingDataException {
        return notNull( 
            Optional.ofNullable(principal)
                .filter(p -> SparkysAuthPrincipal.class.isAssignableFrom(p.getClass()))
                .map(SparkysAuthPrincipal.class::cast)
                .map(this::extendFromSparkyPrincipal)
                .orElseThrow(() -> new MissingDataException(""))
        );
    }

    /**
     * Fullfill information from a storage.
     */
    @Override
    @Nonnull
    public SparkyUser extendFromUserDto(@Nullable UserDto user) throws MissingDataException {
        if(user == null) {
            throw new MissingDataException("UserDto was null");
        }
        return storageService.findUserByNameAndRealm(user.username, user.realm);
    }

    /**
     * Creates a SparkyUser without looking in the database for additional information. Is not suitable 
     * for editing purposes. Get a fresh copy from a storage in that case.
     */
    @Override
    @Nonnull
    public SparkyUser extendFromAuthentication(@Nullable Authentication auth) throws MissingDataException {
        if (auth == null) {
            throw new MissingDataException("Can't extend from null");
        } else if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return extendFromUserDetails((UserDetails) auth.getPrincipal());
        } else if (auth != null && auth.getAuthorities().size() == 1) {
            String username = auth.getName();
            UserRealm realm = Optional.ofNullable(auth.getPrincipal())
                    .filter(p -> SparkysAuthPrincipal.class.isAssignableFrom(p.getClass()))
                    .map(SparkysAuthPrincipal.class::cast)
                    .map(sp -> sp.getRealm())
                    .orElseThrow(() -> new MissingDataException("realm information missing"));
            Password pw = null;
            final var localCred = auth.getCredentials();
            if (localCred != null) {
                pw = new Password(notNull(localCred.toString()), "UNKWN");
            }
            return UserFactoryProvider.getFactory(realm).create(username, pw, getRole(auth.getAuthorities()), false);
        } else {
            throw new UnsupportedOperationException("Can't extend from authentication with unkown user type.");
        }
    }
}
