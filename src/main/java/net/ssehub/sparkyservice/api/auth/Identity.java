package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;

/**
 * Can be used as {@link Authentication} principal object. Holds the necessary
 * information to identify a user.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
//public record Identity(String nickname, UserRealm realm) {
public class Identity {
    private final String nickname;
    private final UserRealm realm;

    private static final String SEPERATOR = "@";

    @Nonnull
    public static final Identity of(SparkyUser user) {
        return user.getIdentity();
    }

    @Nonnull
    public static Identity of(User user) {
        return new Identity(user.getNickname(), user.getRealm());
    }

    @Nonnull
    public static Identity of(String username) {
        var content = username.split(SEPERATOR);
        if (content.length != 2) {
            throw new IllegalArgumentException("Not a valid username. Missing user or realm.");
        }
        String nickname = notNull(content[0]);
        UserRealm realm;
        try {
            realm = UserRealm.valueOf(content[1]);
        } catch (IllegalArgumentException e) {
            realm = UserRealm.UNKNOWN;
        }
        return new Identity(nickname, realm);
    }

    @Deprecated
    // TODO delete
    public static Identity of(@Nullable String name, @Nullable String realmString) {
        if (realmString == null || name == null) {
            throw new IllegalArgumentException("Arguments must have a value");
        }
        UserRealm realm;
        try {
            realm = UserRealm.valueOf(realmString);
        } catch (IllegalArgumentException e) {
            realm = UserRealm.UNKNOWN;
        }
        return new Identity(name, realm);
    }

//    public Identity {
    public Identity(String nickname, UserRealm realm) {
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("Empty nickname is not allowed");
        }
        this.nickname = nickname;
        this.realm = realm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((nickname == null) ? 0 : nickname.hashCode());
        result = prime * result + ((realm == null) ? 0 : realm.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Identity other = (Identity) obj;
        if (nickname == null) {
            if (other.nickname != null)
                return false;
        } else if (!nickname.equals(other.nickname))
            return false;
        if (realm != other.realm)
            return false;
        return true;
    }

    @Nonnull
    public String nickname() {
        return notNull(nickname.toLowerCase().trim());
    }

    @SuppressWarnings("null") // field is final
    @Nonnull
    // Override for non null annotation
    public UserRealm realm() {
        return this.realm;
    }

    /**
     * The identity representation as single String.
     * 
     * @return Can be used as identifier
     */
    @Nonnull
    public String asUsername() {
        return nickname() + SEPERATOR + realm();
    }
}
