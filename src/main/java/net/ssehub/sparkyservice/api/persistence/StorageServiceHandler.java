package net.ssehub.sparkyservice.api.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.auth.memory.MemoryStorage;

// TODO write generic storage handler
@Service
@Primary
public class StorageServiceHandler implements UserStorageService {

    @Autowired
    private UserDatabaseStorageService dbService;

    @Autowired
    private MemoryStorage memoryService;

    @Override
    public <T extends SparkyUser> void commit(@Nonnull T user) {
        dbService.commit(user);
    }

    @Override
    @Nonnull
    public List<SparkyUser> findUsers(@Nullable String nickname) throws UserNotFoundException {
        return extendList(() -> memoryService.findUsers(nickname) , () -> dbService.findUsers(nickname));
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable String identName) throws UserNotFoundException {
        return findUser(Identity.of(identName));
    }

    @Override
    @Nonnull
    public SparkyUser findUser(@Nullable Identity ident) throws UserNotFoundException {
        SparkyUser u;
        try {
            u = memoryService.findUser(ident);
        } catch (UserNotFoundException e) {
            u = dbService.findUser(ident);
        }
        return u;
    }

    @Override
    @Nonnull
    public List<SparkyUser> findAllUsers() {
        return extendList(() -> memoryService.findAllUsers(), () -> dbService.findAllUsers());
    }

    @Override
    public boolean isUserInStorage(@Nullable SparkyUser user) {
        return memoryService.isUserInStorage(user) || dbService.isUserInStorage(user);
    }

    @Override
    public void deleteUser(Identity user) {
        memoryService.deleteUser(user);
        dbService.deleteUser(user);
    }

    @Override
    public List<SparkyUser> findAllUsersInRealm(UserRealm realm) {
        return extendList(() -> memoryService.findAllUsersInRealm(realm), () -> dbService.findAllUsersInRealm(realm));
    }

    @Nonnull
    private static List<SparkyUser> extendList(Supplier<List<SparkyUser>> searchMethod,
        Supplier<List<SparkyUser>> searchMethod2) {
        var us = new ArrayList<SparkyUser>(searchMethod.get());
        us.addAll(searchMethod2.get());
        return us;
    }

}
