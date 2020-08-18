package net.ssehub.sparkyservice.api.auth;

import java.time.LocalDate;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.ppolicy.PasswordPolicyControl;
import org.springframework.security.ldap.ppolicy.PasswordPolicyResponseControl;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LdapUser;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * {@link UserDetails} mapper. It creates a {@link LdapUser} object from a successful LDAP login.
 * 
 * @author marcel
 */
public final class SparkyLdapUserDetailsMapper implements UserDetailsContextMapper {
    
    private UserStorageService storageService;
    
    public SparkyLdapUserDetailsMapper(UserStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * https://ldap.com/ldap-oid-reference-guide/
     */
    public static final String DISPLAY_NAME_OID = "2.16.840.1.113730.3.1.241";

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {

        // we don't want LDAP Authroties here so we don't use them
        var ldapUser = LdapUser.create(username, UserRole.DEFAULT /* maybe change later */, true);
        if (storageService.isUserInStorage(ldapUser)) {
            ldapUser = (LdapUser) storageService.refresh(ldapUser);
        } else {
            storageService.commit(ldapUser);
        }
        ldapUser.setFullname(extractFullname(ctx.getAttributes()));
        ldapUser.setExpireDate(extractExpirationDate(ctx));
        return ldapUser;
    }

    @Nullable
    private static String extractFullname(Attributes contextAttributes) {
        Attribute fullnameAttr = contextAttributes.get("displayname");
        String fullname;
        try {
            if (fullnameAttr != null) {
                fullname = (String) fullnameAttr.get();
            } else {
                fullname = null;
            }
        } catch (NamingException e) {
            e.printStackTrace();
            fullname = null;
        }
        return fullname;
    }

    @Nullable
    private static LocalDate extractExpirationDate(DirContextOperations ctx) {
        PasswordPolicyResponseControl ppolicy = (PasswordPolicyResponseControl) ctx
                .getObjectAttribute(PasswordPolicyControl.OID);
        LocalDate expDate = null;
        if (ppolicy != null) {
            //expDAte
//            ldapuser.setTimeBeforeExpiration(ppolicy.getTimeBeforeExpiration());
        }
        return expDate;
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
