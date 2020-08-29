package net.ssehub.sparkyservice.api.auth.storage;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.jpa.token.JpaJwtToken;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.storage.NoTransactionUnitException;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.util.SparkyUtil;

/**
 * Provides service methods for querying a storage and deal with {@link JwtToken}.
 * 
 * @author marcel
 */
@Service
public class JwtStorageService {

    private final Logger log = LoggerFactory.getLogger(JwtStorageService.class);

    @Nonnull
    private final JwtRepository repo;

    @Nonnull
    private final UserStorageService userStorageService;
    
    /**
     * Constructor for dependency injection. 
     * 
     * @param repo
     * @param userStorageService
     */
    @Autowired 
    public JwtStorageService(@Nonnull JwtRepository repo, @Nonnull UserStorageService userStorageService) {
        super();
        this.repo = repo;
        this.userStorageService = userStorageService;
    }

    /**
     * All token from a storage.
     * 
     * @return list of all stored tokens
     */
    public List<JwtToken> findAll() {
        List<JpaJwtToken> jpaList = SparkyUtil.toList(notNull(repo.findAll()));
        return jpaList.stream().map(JwtToken::new).collect(Collectors.toList());
    }

    /**
     * Saves a JwtToken to the database. This creates a new entry if the token isn't in the storage yet. Otherwise 
     * it will edit them.
     * 
     * @param jwt - Desired tokens to saved. Each token will be saved successively
     */
    public void commit(JwtToken... jwt) {
        try {
            List<JpaJwtToken> list = Arrays.stream(jwt)
                    .map(obj -> obj.getJpa(userStorageService))
                    .collect(Collectors.toList());
            repo.saveAll(list);
        } catch (UserNotFoundException e) {
           log.debug("Don't safe JWT token to storage for user.");
        }
    }

    /**
     * Finds all JWT token from a storage which the given user owns. 
     * 
     * @param user
     * @return JWT token of a user
     */
    public List<JwtToken> findAllByUser(SparkyUser user) {
        List<JpaJwtToken> list;
        try {
            list = repo.findByUser(user.getJpa());  
        } catch (NoTransactionUnitException e) {
            list = new ArrayList<JpaJwtToken>();
        }
        return list.stream().map(JwtToken::new).collect(Collectors.toList());
    }

    /**
     * Finds all locked token from the database.
     * 
     * @return locked Token from a storage 
     */
    public Set<JwtToken> findAllLocked() {
        var tokenSet = repo.findByLocked(true);
        return tokenSet.stream().map(JwtToken::new).collect(Collectors.toSet());
    }
}
