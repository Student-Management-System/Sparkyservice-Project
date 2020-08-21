package net.ssehub.sparkyservice.api.user.creation;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.user.LdapUserFactory;
import net.ssehub.sparkyservice.api.user.LocalUserFactory;
import net.ssehub.sparkyservice.api.user.MemoryUserFactory;
import net.ssehub.sparkyservice.api.user.SparkyUser;

/**
 * Provides Factory methods for {@link AbstractSparkyUserFactory} depending on a given {@link UserRealm}.
 * 
 * @author marcel
 */
public class UserFactoryProvider {
    
    /**
     * Creates a user factory depending on the given realm. 
     * 
     * @param realm
     * @return Factory class for a concrete type
     */
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
