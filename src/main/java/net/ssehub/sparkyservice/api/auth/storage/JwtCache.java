package net.ssehub.sparkyservice.api.auth.storage;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Provides a thread safe cache for {@link JwtToken}.
 * 
 * @author marcel
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class JwtCache {

    private static volatile JwtCache instance = new JwtCache(null);
    
    @Nonnull
    private Map<UUID, JwtToken> tokenStore = new ConcurrentHashMap<UUID, JwtToken>();
    @Nonnull
    private final Optional<JwtStorageService> storage;

    
    /**
     * Cache with an empty store.
     * 
     * @param storage The storage service which is used for storage operation
     */
    private JwtCache(@Nullable JwtStorageService storage) {
        this(new ConcurrentHashMap<UUID, JwtToken>(), storage);
    }

    /**
     * Cache with start values.
     * 
     * @param tokenStore should be a thread safe implementation
     * @param storage
     */
    private JwtCache(Map<UUID, JwtToken> tokenStore, @Nullable JwtStorageService storage) {
        this.storage = notNull(Optional.ofNullable(storage));
        this.tokenStore = tokenStore;
    }

    /**
     * Stores a new token object to the cache and stores it in a storage (thread safe).
     * 
     * @param jpaTokens New or updated object
     */
    public synchronized void storeAndSave(JwtToken... jpaTokens) {
        for (final JwtToken singleToken : jpaTokens) {
            tokenStore.put(singleToken.getJti(), singleToken);
        }
        storage.ifPresent(s -> s.commit(jpaTokens));
    }

    /**
     * Refreshed the current cache with values from a storage (when a storage is present).
     */
    public synchronized void refreshFromStorage() {
        tokenStore = notNull(
            storage.map(JwtStorageService::findAll)
                .orElseGet(ArrayList::new)
                .stream()
                .collect(
                    Collectors.toMap(JwtToken::getJti, Function.identity())
                )
        );
    }

    /**
     * Refreshes the cache with a given strategy. 
     * 
     * @param refreshStrategy strategy to refresh cache; should not return null
     */
    public synchronized void refreshFromStorage(Supplier<? extends Map<UUID, JwtToken>> refreshStrategy) {
        Map<UUID, JwtToken> newStore = refreshStrategy.get();
        if (newStore == null) {
            throw new RuntimeException("Store supplier in cache provided null");
        }
        tokenStore = newStore;
    }


    /**
     * A set of disabled JITs. 
     * 
     * @return disbaled jits
     */
    public Set<UUID> getLockedJits() {
        return SparkyUtil.toSet(tokenStore)
            .stream()
            .filter(JwtToken::isLocked)
            .map(JwtToken::getJti)
            .collect(Collectors.toSet());
    }

    /**
     * Returns a JwtToken object from the cache store if present.
     * 
     * @param jit The desired cached token
     * @return Optional token; empty when the token with given jit is not in store
     */
    public synchronized Optional<JwtToken> getCachedToken(@Nullable UUID jit) {
        JwtToken tokenCopy = tokenStore.get(jit);
        if (tokenCopy != null) {
            tokenCopy = tokenCopy.copy();
        } else {
//            storage.ifPresent(s -> s.findBJit(jit.toString()));
        }
        return Optional.ofNullable(tokenCopy);
    }
    
    /**
     * Returns a copy of the currently locked token.
     * 
     * @return copy of currently locked token objects
     */
    public Set<JwtToken> getLockedTokenObjects() {
        return SparkyUtil.toSet(tokenStore)
            .stream()
            .filter(JwtToken::isLocked)
            .map(JwtToken::copy)
            .collect(Collectors.toSet());
    }

    /**
     * Returns a copy of the stored tokens.
     * 
     * @return Set of tokens in the cache
     */
    public Set<JwtToken> getCachedTokens() {
        return SparkyUtil.toSet(tokenStore)
            .stream()
            .map(JwtToken::copy)
            .collect(Collectors.toSet());
    }

// STATIC METHODS 
    
    /**
     * Provides the instance of the cache.
     * 
     * @return Thread safe instance
     */
    public static synchronized JwtCache getInstance() {
        return instance;
    }

    /**
     * Initialize a new Cache. <br>
     * Note: This haven't to be done for the first start. Use this only when a complete new cache is desired.
     */
    public static void initNewCache() {
        instance = new JwtCache(null);
    }

    /**
     * Initialize a new cache with existing values and a storage implementation.
     * 
     * @param cacheStore - Cache entry to start with 
     * @param storage - Is used to sync current cached items to a storage - should be immutable or thread safe
     */
    public static void initNewCache(Collection<JwtToken>  cacheStore, @Nullable JwtStorageService storage) {
        Map<UUID, JwtToken> cacheMap = new ConcurrentHashMap<UUID, JwtToken>();
        cacheStore.forEach(jwt -> cacheMap.put(jwt.getJti(), jwt));
        instance = new JwtCache(cacheMap, storage);
    }
}
