package net.ssehub.sparkyservice.api.user;

import javax.annotation.Nonnull;

import org.springframework.security.authentication.AuthenticationProvider;

/**
 * Authentication realm of a user.
 * 
 * @author marcel
 */
public interface UserRealm {

    @Nonnull
    public String identifierName();

    @Nonnull
    public String publicName();

    /**
     * Returns a factory in order to create a {@link SparkyUser} matching a realm.
     * 
     * @return Abstract Factory for creating users
     */
    @Nonnull
    public SparkyUserFactory<? extends SparkyUser> userFactory();

    @Nonnull
    public AuthenticationProvider authenticationProvider();

    public int authenticationPriorityWeight();
    
    default boolean nameEquals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof UserRealm)) {
            return false;
        }
        var r = (UserRealm) obj;
        return r.identifierName().equals(this.identifierName());
    }
}
