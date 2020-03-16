package net.ssehub.sparkyservice.api.storeduser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.db.user.StoredUser;

@Service
public class StoredUserService implements IStoredUserService {
    
    @Autowired
    private StoredUserRepository repository;
    
    public <T extends StoredUser> void storeUser(@Nonnull T user) {
        StoredUser stUser = new StoredUser((StoredUser) user);
        repository.save(stUser);
    }
        
    @Override
    public @Nonnull List<StoredUserDetails> findUsersByUsername(@Nullable String username) throws UserNotFoundException {
        Optional<List<StoredUser>> usersByName = repository.findByuserName(username);
        usersByName.orElseThrow(() -> new UserNotFoundException("No user with this name was found in database"));
        usersByName.get().forEach(x -> new StoredUserDetails(x));
        var list = usersByName.get();
        List<StoredUserDetails> userDetailsList = new ArrayList<StoredUserDetails>();
        for (StoredUser transformUser : list) {
            userDetailsList.add(new StoredUserDetails(transformUser));
        }
        return userDetailsList;
    }
    
    @Override
    public @Nonnull StoredUserDetails findUserByNameAndRealm(@Nullable String username, @Nullable String realm) throws UserNotFoundException {
        Optional<StoredUser> user = repository.findByuserNameAndRealm(username, realm);
        user.orElseThrow(() -> new UserNotFoundException("no user with this name in the given realm"));
        return user.map(StoredUserDetails::new).get();
    }
     
    @Override
    public @Nonnull StoredUserDetails findUserByid(int id) throws UserNotFoundException {
        Optional<StoredUser> user = repository.findById(id);
        user.orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return user.map(StoredUserDetails::new).get();
    }

    /**
     * Used by spring security for loading users (springs {@link UserDetails} service) from the local database.
     * Because this method is only used by spring for local user lookups, 
     * it will search only in {@link StoredUserDetails.DEDEFAULT_REALM}.
     * 
     * @param username name to look for
     * @return userDetails from the database - never null
     * @throws When a the given username is not found in the database with the default realm - spring will continue 
     * with the next configured AuthProvider
     */
    @Override
    public @Nonnull UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            if (username == null) {
                throw new UsernameNotFoundException("User with name \"null\" not found");
            } 
            final var storedUser = findUserByNameAndRealm(username, StoredUserDetails.DEFAULT_REALM);
            return new StoredUserDetails(storedUser);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }
}
