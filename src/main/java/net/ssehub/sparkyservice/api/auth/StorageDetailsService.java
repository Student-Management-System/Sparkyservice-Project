package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.auth.identity.Identity;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.persistence.UserNotFoundException;
import net.ssehub.sparkyservice.api.persistence.UserStorageService;

@Service
public class StorageDetailsService {

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