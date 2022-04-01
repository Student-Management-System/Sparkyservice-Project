package net.ssehub.sparkyservice.api.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.jpa.user.User;

/**
 * Can be used as {@link Authentication} principal object. Holds the necessary
 * information to identify a user.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public class Identity {
    private static final String SEPERATOR = "@";

    private @Nonnull final String nickname;
    private @Nonnull final UserRealm realm;

    /**
     * Representation of a users identity. It contains necessary information for identifying 
     * users across the whole application context. 
     * 
     * @param nickname Username without realm information 
     * @param realm The users realm
     */
    public Identity(String nickname, UserRealm realm) {
        if (nickname.isBlank()) {
            throw new IllegalArgumentException("Empty nickname is not allowed");
        }
        this.nickname = notNull(nickname.toLowerCase().trim());
        this.realm = realm;
    }

    /**
     * Creates an identity object based on a JPA user.
     * 
     * @param user
     * @return Identity for unique identification
     */
    @Nonnull
    public static Identity of(User user) {
        return new Identity(user.getNickname(), user.getRealm());
    }

    /**
     * Creates an identitiy object based on a username string. 
     * 
     * @param username Username in the following format <code> user@realm </code>
     * @return Identity representationof the given username
     */
    @Nonnull
    public static Identity of(@Nullable String username) {
        if (username == null || !validateFormat(username)) {
            throw new IllegalArgumentException("Not a valid username. Missing user or realm.");
        }
        var content = username.split(SEPERATOR);
        String nickname = notNull(content[0]);
        UserRealm realm;
        try {
            realm = UserRealm.valueOf(content[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            realm = UserRealm.UNKNOWN;
        }
        return new Identity(nickname, realm);
    }
    
    public static boolean validateFormat(@Nullable String username) {
        return username != null && username.split(SEPERATOR).length == 2;
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Identity other = (Identity) obj;
        if (!nickname.equals(other.nickname)) {
            return false;
        }
        if (realm != other.realm) {
            return false;
        }
        return true;
    }

    /**
     * Nickname only without realm information. Can't be used for application wide context.
     * 
     * @return Nickname only 
     */
    @Nonnull
    public String nickname() {
        return nickname;
    }

    
    /**
     * The source realm where the {@link #nickname} of this user belongs to.
     * 
     * @return The realm
     */
    @Nonnull
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

    @Override
    public String toString() {
        return asUsername();
    }
}
