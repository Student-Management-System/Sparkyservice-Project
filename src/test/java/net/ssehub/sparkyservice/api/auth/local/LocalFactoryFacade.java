package net.ssehub.sparkyservice.api.auth.local;


import net.ssehub.sparkyservice.api.auth.identity.UserRealm;

public class LocalFactoryFacade extends LocalUserFactory {

    @SuppressWarnings("null")
    public LocalFactoryFacade(UserRealm realm) {
        super(realm);
    }
}
