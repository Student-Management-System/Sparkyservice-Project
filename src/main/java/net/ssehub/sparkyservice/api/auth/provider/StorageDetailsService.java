package net.ssehub.sparkyservice.api.auth.provider;

import javax.annotation.Nonnull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.storage.UserNotFoundException;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

@Service
class StorageDetailsService {

    @Nonnull
    private final UserStorageService storageService;

    public StorageDetailsService(@Nonnull UserStorageService storageService) {
        this.storageService = storageService;
    }

    public UserDetails loadUser(String nickname, @Nonnull UserRealm searchRealm) {
        if (nickname == null) {
            throw new UsernameNotFoundException("null");
        }
        try {
            var ident = new Identity(nickname, searchRealm);
            return storageService.findUser(ident);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage());
        }
    }
}