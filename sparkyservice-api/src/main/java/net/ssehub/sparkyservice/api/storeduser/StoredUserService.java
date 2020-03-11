package net.ssehub.sparkyservice.api.storeduser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.db.user.StoredUser;

@Service
public class StoredUserService implements IStoredUserService {
 
    @Autowired
    private StoredUserRepository repository;

    @Override
    public List<StoredUser> findUserByUsername(String username) throws UserNotFoundException {
        Optional<List<StoredUser>> usersByName = repository.findByuserName(username);
        usersByName.orElseThrow(() -> new UserNotFoundException("No user with this name was found in database"));
        return usersByName.get();
    }
    
    @Override
    public StoredUser findByuserNameAndRealm(String username, String realm) throws UserNotFoundException {
        Optional<StoredUser> user = repository.findByuserNameAndRealm(username, realm);
        user.orElseThrow(() -> new UserNotFoundException("no user with this name in the given realm"));
        return user.get();
    }
    
    @Override
    public Boolean storeNewUser(@NonNull NewUserDto newUser) {
//        var user = new StoredUser(newUser.username, StoredUserDetails.DEFAULT_REALM); // TODO !!
//        repository.save(user);
        return true;
    }
    

    @Override
    public StoredUser findUserByid(int id) throws UserNotFoundException {
        Optional<StoredUser> user = repository.findById(id);
        user.orElseThrow(() -> new UserNotFoundException("Id was not found in database"));
        return user.map(StoredUserDetails::new).get();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            var storedUser = findByuserNameAndRealm(username, StoredUserDetails.DEFAULT_REALM);
            return new StoredUserDetails(storedUser);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }

    private static List<StoredUser> transformToCustomUser(List<StoredUser> users) {
        List<StoredUser> customUsersList = new ArrayList<StoredUser>();
        for(StoredUser singleUser : users) {
            customUsersList.add(new StoredUserDetails(singleUser));
        }
        return customUsersList;
    }
}
