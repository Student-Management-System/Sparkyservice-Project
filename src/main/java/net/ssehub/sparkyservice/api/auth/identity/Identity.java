package net.ssehub.sparkyservice.api.auth.identity;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.persistence.jpa.user.User;

/**
 * Can be used as {@link Authentication} principal object. Holds the necessary information to identify a user.
 * 
 * @author marcel
 */
@ParametersAreNonnullByDefault
public class Identity {
    private static final String SEPARATOR = "@";

    private @Nonnull final String nickname;
    private @Nonnull final UserRealm realm;

    /**
     * Representation of a users identity. It contains necessary information for identifying users across the whole
     * application context.
     * 
     * @param nickname Username without realm information
     * @param realm    The users realm
     */
    public Identity(String nickname, UserRealm realm) {
        if (nickname.isBlank()) {
            throw new IllegalIdentityFormat("Empty nickname is not allowed");
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
        var realm = realmFromRegistry(user.getRealmIdentifier());
        return new Identity(user.getNickname(), realm);
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
            throw new IllegalIdentityFormat("Not a valid username. Missing user or realm.");
        }
        var content = username.split(SEPARATOR);
        String nickname = notNull(content[0]);
        String realmIdentifier = content[1];
        UserRealm realm = realmFromRegistry(realmIdentifier);
        return new Identity(nickname, realm);
    }

    @Nonnull
    private static UserRealm realmFromRegistry(@Nullable String realmIdentifier) {
        Supplier<String> availableRealmsSupp = () -> RealmRegistry.getConfiguredRealms()
            .stream().map(r -> r.identifierName())
            .collect(Collectors.joining(", "));
        UserRealm realm = notNull(RealmRegistry.realmFrom(realmIdentifier)
                .orElseThrow(
                    () -> new NoSuchRealmException("Realm with identifier " + realmIdentifier
                        + " not found. Available realms: " + availableRealmsSupp.get())));
        return realm;
    }

    public static boolean validateFormat(@Nullable String username) {
        return username != null && username.split(SEPARATOR).length == 2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname, realm);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Identity)) {
            return false;
        }
        Identity other = (Identity) obj;
        return Objects.equals(nickname, other.nickname) && Objects.equals(realm, other.realm);
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
        return nickname() + SEPARATOR + realm().identifierName();
    }

    @Nonnull
    public String asPublicIdentity() {
        return nickname() + SEPARATOR + realm.publicName();
    }

    @Override
    public String toString() {
        return asUsername();
    }
}
