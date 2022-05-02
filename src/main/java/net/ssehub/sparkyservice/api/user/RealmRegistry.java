package net.ssehub.sparkyservice.api.user;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides static methods for all configured user realm beans.
 * 
 * @author marcel
 */
@Component
public class RealmRegistry {

    @Nullable
    private static List<UserRealm> configuredRealms;

    @Autowired
    public RealmRegistry(List<UserRealm> configuredRealms) {
        RealmRegistry.configuredRealms = configuredRealms;
    }

    @Nonnull
    public static List<UserRealm> getConfiguredRealms() {
        var realms = configuredRealms;
        if (realms == null) {
            throw new BeanInitializationException("Application context was not loaded. Can't extract all realm beans.");
        }
        return realms;
    }
    
    public static Optional<UserRealm> realmFromIdentifier(@Nullable String identifier) {
        return RealmRegistry.getConfiguredRealms().stream().filter(r -> r.identifierName().equalsIgnoreCase(identifier))
            .findFirst();
    }

    public static Optional<UserRealm> realmFromPublicName(@Nullable String identifier) {
        return RealmRegistry.getConfiguredRealms().stream().filter(r -> r.publicName().equalsIgnoreCase(identifier))
            .findFirst();
    }
    
    public static Optional<UserRealm> realmFrom(@Nullable String identifier) {
        return realmFromIdentifier(identifier).or(() -> realmFromPublicName(identifier));
    }
}
