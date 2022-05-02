package net.ssehub.sparkyservice.api.auth.memory;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.UserNotFoundException;
import net.ssehub.sparkyservice.api.persistence.UserStorageService;
import net.ssehub.sparkyservice.api.util.NullHelpers;

@Service
public class MemoryStorage implements UserStorageService {

    private final Logger log = LoggerFactory.getLogger(MemoryStorage.class);

    @Autowired
    private List<MemoryUser> users;

    @Override
    public <T extends SparkyUser> void commit(@Nonnull T user) {
        log.debug("Ignored memory save");
    }

    @Override
    @Nonnull
    public List<SparkyUser> findUsers(@Nullable String nickname) throws UserNotFoundException {
        if (nickname == null) {
            throw new UserNotFoundException("null");
        }
        return NullHelpers.notNull(
            users.stream().filter(u -> u.getIdentity().nickname().equals(nickname)).collect(Collectors.toList()));
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable Identity ident) throws UserNotFoundException {
        return notNull(users.stream()
            .filter(u -> u.getIdentity().equals(ident)).findFirst()
            .orElseThrow(() -> new UserNotFoundException(ident + " not found in memory realm")));
    }

    @Override
    @Nonnull
    public List<SparkyUser> findAllUsers() {
        return new ArrayList<>(users);
    }

    @Override
    public boolean isUserInStorage(@Nullable SparkyUser user) {
        return false;
    }

    @Override
    public void deleteUser(Identity user) {
        log.debug("Ignored memory deletion attempt");
    }

    @Override
    public List<SparkyUser> findAllUsersInRealm(UserRealm realm) {
        List<SparkyUser> users;
        if (realm.identifierName().equals(MemoryRealm.IDENTIFIER_NAME)) {
            users = findAllUsers();
        } else {
            users = new ArrayList<>();
        }
        return users;
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable String identName) throws UserNotFoundException {
        return this.findUser(Identity.of(identName));
    }

}
