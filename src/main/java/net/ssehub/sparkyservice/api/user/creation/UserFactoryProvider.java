package net.ssehub.sparkyservice.api.user.creation;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.user.LdapUserFactory;
import net.ssehub.sparkyservice.api.user.LocalUserFactory;
import net.ssehub.sparkyservice.api.user.MemoryUserFactory;
import net.ssehub.sparkyservice.api.user.SparkyUser;

public class UserFactoryProvider {
    public static AbstractSparkyUserFactory<? extends SparkyUser> getFactory(UserRealm realm) {
        AbstractSparkyUserFactory<? extends SparkyUser> fac = new LdapUserFactory();
        switch (realm) {
        case LOCAL:
            fac = new LocalUserFactory();
            break;
        case LDAP:
            fac = new LdapUserFactory();
            break;
        case MEMORY:
            fac = new MemoryUserFactory();
            break;
        default:
            throw new UnsupportedOperationException();
        }
        return fac;
    }
}
