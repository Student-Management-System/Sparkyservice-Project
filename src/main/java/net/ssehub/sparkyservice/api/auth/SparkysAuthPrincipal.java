package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;

import net.ssehub.sparkyservice.api.user.UserRealm;

/**
 * Can be used as {@link Authentication} principal object. Holds the necessary information to identify a user.
 * 
 * @author marcel
 */
public interface SparkysAuthPrincipal extends AuthenticatedPrincipal {
    
    @Override
    @Nonnull String getName();
    
    /**
     * The realm where the users is a member of. 
     * 
     * @return Users current realm
     */
    @Nonnull UserRealm getRealm();

    /**
     * Realm and string in a way together to identify them afterwards.
     * 
     * @return Can be used as identifier
     */
    @Nonnull String asString();
}
