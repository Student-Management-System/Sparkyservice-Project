package net.ssehub.sparkyservice.api.user.extraction;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRole;

/**
 * Simple user transformer which does the necessary work. Tries to make a compromise between performance
 * and simplicity.
 * 
 * @author marcel
 */
@Service
public class SimpleExtractionImpl implements UserExtractionService {

    /**
     * Get a role from a GrantedAuthority. Only a single authority is used (the first one). 
     * 
     * @param authorities
     * @return UserRole which was casted from an authority
     */
    @Nonnull
    private static UserRole getRole(Collection<? extends GrantedAuthority> authorities) {
        Object[] objList = authorities.toArray();
        String[] authList = Arrays.stream(objList).map(String::valueOf).toArray(String[]::new);
        return UserRole.getEnum(authList[0]);
    }

    /*
     * Creates a SparkyUser without looking in the database for additional information. Is not suitable 
     * for editing purposes. Get a fresh copy from a storage in that case.
     */
    @Override
    @Nonnull
    public SparkyUser extract(@Nullable Authentication auth) {
        try {
            if (auth == null) {
                throw new NoSuchElementException();
            }
            return notNull(
                    tryExtractInformation(auth)
                    .orElseThrow()
            );
        } catch (NoSuchElementException e) {
            throw new MissingDataException("Not enough information to extract from");
        }
    }

    /**
     * Tries to extract information of the authentication object an build a user. 
     * 
     * @param auth
     * @return User with information present from the authentication object
     * @throws MissingDataException When the principal is not an {@link SparkysAuthPrincipal}
     */
    private static Optional<SparkyUser> tryExtractInformation(@Nonnull Authentication auth) {
        String username = auth.getName();
        Password pw = extractPassword(auth);
        // TODO check the content of principal and username
        return Optional.ofNullable(auth.getPrincipal())
            .map(String.class::cast).map(Identity::of)
            .map(id -> id.realm().getUserFactory().create(username, pw, getRole(auth.getAuthorities()), false));
    }

    @Nullable
    private static Password extractPassword(@Nonnull Authentication auth) {
        Password pw = null;
        final var localCred = auth.getCredentials();
        if (localCred != null) {
            pw = new Password(notNull(localCred.toString()), "UNKWN");
        }
        return pw;
    }
}
