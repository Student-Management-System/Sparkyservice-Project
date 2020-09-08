package net.ssehub.sparkyservice.api.auth.ldap;

import java.util.Collection;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LdapUser;
import net.ssehub.sparkyservice.api.user.LdapUserFactory;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * {@link UserDetails} mapper. It creates a {@link LdapUser} object from a successful LDAP login.
 * 
 * @author marcel
 */
public final class SparkyLdapUserDetailsMapper implements UserDetailsContextMapper {

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
    public SparkyLdapUserDetailsMapper(UserStorageService storageService) {
        this.storageService = storageService;
    }


    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {
        // we don't want LDAP Authroties here so we don't use them
        var ldapUser = new LdapUserFactory().create(username, null, UserRole.DEFAULT /* maybe change later */, true);
        if (ctx != null) {
            var ldapInfoExtractor = new LdapInformationExtractor(ctx);
            ldapUser.setExpireDate(ldapInfoExtractor.getExpirationDate());
            ldapUser.setFullname(ldapInfoExtractor.getFullname());
            ldapUser.getSettings().setEmail_address(ldapInfoExtractor.getEmail());
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
