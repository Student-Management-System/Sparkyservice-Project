package net.ssehub.sparkyservice.api.auth.ldap;

import java.util.Collection;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Service;

import net.ssehub.sparkyservice.api.user.LdapUser;
import net.ssehub.sparkyservice.api.user.LdapUserFactory;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * {@link UserDetails} mapper. It creates a {@link LdapUser} object from a successful LDAP login.
 * 
 * @author marcel
 */
@Service
public final class LdapContextMapper implements UserDetailsContextMapper {

    /**
     * OID List.
     * https://ldap.com/ldap-oid-reference-guide/
     */
    public static final String DISPLAY_NAME_OID = "2.16.840.1.113730.3.1.241";
    
    private UserStorageService storageService;
    
    /**
     * Maps user information from an LDAP login to a correct {@link UserDetails} which is an {@link LdapUser}.
     * 
     * @param storageService - The used storage service used to store information after login
     */
    public LdapContextMapper(UserStorageService storageService) {
        this.storageService = storageService;
    }


    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {
        /*
         * we don't want LDAP Authorities here
         * The following line specifies the default role all new users will have. This does not affect already existing
         * ones.
         */
        var ldapUser = new LdapUserFactory().create(username, null, UserRole.DEFAULT, true);
        if (ctx != null) {
            var ldapInfoExtractor = new LdapInformationReader(ctx);
            ldapUser.setExpireDate(ldapInfoExtractor.getExpirationDate());
            ldapUser.setFullname(ldapInfoExtractor.getFullname());
            ldapUser.getSettings().setEmailAddress(ldapInfoExtractor.getEmail());
        }
        if (storageService.isUserInStorage(ldapUser)) {
            ldapUser = (LdapUser) storageService.refresh(ldapUser);
        } else {
            storageService.commit(ldapUser);
        } 
        return ldapUser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException(
                "SparkyLdapUserDetailsMapper only supports reading from a context. Please"
                        + " use a subclass if mapUserToContext() is required.");
    }
}
